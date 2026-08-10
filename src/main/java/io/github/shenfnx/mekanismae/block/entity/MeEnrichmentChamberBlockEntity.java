package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import io.github.shenfnx.mekanismae.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.Action;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.core.NonNullList;

public final class MeEnrichmentChamberBlockEntity extends AENetworkedBlockEntity implements ICraftingProvider, Container, net.minecraft.world.MenuProvider {
    private static final int BASE_ENERGY_CAPACITY = 1_000_000;
    private static final int BASE_ENERGY_RECEIVE = 200_000;
    private static final int ENERGY_CAPACITY_PER_UPGRADE = 500_000;
    private static final int ENERGY_RECEIVE_PER_UPGRADE = 200_000;
    private static final int ENERGY_PER_OPERATION = 10_000;
    private static final int BASE_PROCESSING_TICKS = 20;
    public static final int PATTERN_SLOT_COUNT = 9;
    public static final int UPGRADE_SLOT_COUNT = 8;
    public static final int MAX_UPGRADES_PER_TYPE = 8;
    private static final long MAX_ACCEPTED_OPERATIONS = 1_048_576;
    private static final int TASK_DATA_VERSION = 2;
    private static final int[] PARALLEL_MULTIPLIER = {1, 2, 3, 4, 6, 8, 10, 12, 16};

    private final MachineEnergyStorage energyStorage = new MachineEnergyStorage(BASE_ENERGY_CAPACITY, BASE_ENERGY_RECEIVE);
    private final IStrictEnergyHandler strictEnergyHandler = new StrictEnergyHandler();
    private final NonNullList<ItemStack> patternSlots = NonNullList.withSize(PATTERN_SLOT_COUNT, ItemStack.EMPTY);
    private AEItemKey activeInputKey;
    private long activeInputCount;
    private AEItemKey pendingOutputKey;
    private long pendingOutputCount;
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private int inputPerOperation = 1;
    private int progress;
    private boolean processingFaulted;

    // These are intentionally data-driven entry points for the later upgrade system.
    private int speedUpgrades;
    private int parallelUpgrades;
    private boolean networkEnabled = true;
    private final NonNullList<ItemStack> upgradeSlots = NonNullList.withSize(UPGRADE_SLOT_COUNT, ItemStack.EMPTY);

    public MeEnrichmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(), pos, state);
        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(2.0)
                .setVisualRepresentation(ModBlocks.ME_ENRICHMENT_CHAMBER_ITEM.get())
                .addService(ICraftingProvider.class, this);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State state) {
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public IStrictEnergyHandler getStrictEnergyHandler() {
        return strictEnergyHandler;
    }

    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        if (activeInputKey == null || activeInputCount <= 0) {
            return ItemStack.EMPTY;
        }
        return createDisplayStack(activeInputKey, Math.min(activeInputCount, inputPerOperation));
    }

    public ItemStack getProcessingOutputDisplay() {
        activateNextJobIfIdle();
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            return createDisplayStack(pendingOutputKey, pendingOutputCount);
        }
        if (activeInputKey == null || activeInputCount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack probe = activeInputKey.toStack(Math.max(1, inputPerOperation));
        return getRecipeOutput(probe);
    }

    public boolean setPattern(ItemStack stack) {
        if (!PatternDetailsHelper.isEncodedPattern(stack)) {
            return false;
        }
        for (int slot = 0; slot < PATTERN_SLOT_COUNT; slot++) {
            if (patternSlots.get(slot).isEmpty()) {
                setItem(slot, stack.copyWithCount(1));
                return true;
            }
        }
        return false;
    }

    public ItemStack takePattern() {
        if (hasProcessingWork()) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < PATTERN_SLOT_COUNT; slot++) {
            if (!patternSlots.get(slot).isEmpty()) {
                ItemStack result = patternSlots.get(slot);
                patternSlots.set(slot, ItemStack.EMPTY);
                ICraftingProvider.requestUpdate(getMainNode());
                setChanged();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean canTakePattern() {
        return !hasProcessingWork();
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_enrichment_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
            Player player) {
        return new io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu(containerId, inventory, worldPosition);
    }

    public ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored();
                    case 1 -> progress;
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, getTotalQueuedOperations());
                    case 3 -> BASE_PROCESSING_TICKS;
                    case 4 -> getMainNode().isOnline() ? 1 : 0;
                    case 5 -> energyStorage.getMaxEnergyStored();
                    case 6 -> networkEnabled ? 1 : 0;
                    case 7 -> speedUpgrades;
                    case 8 -> parallelUpgrades;
                    case 9 -> getUpgradeCount(ModItems.ENERGY_CARD.get());
                    case 10 -> energyStorage.getReceiveLimit();
                    case 11 -> (int) Math.min(Integer.MAX_VALUE, getTotalQueuedOperations());
                    case 12 -> (int) Math.min(Integer.MAX_VALUE, MAX_ACCEPTED_OPERATIONS);
                    case 13 -> getParallelBatchSize();
                    case 14 -> (int) Math.min(Integer.MAX_VALUE, pendingOutputCount);
                    case 15 -> processingFaulted ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // All values are server-owned; clients only receive them through DataSlots.
            }

            @Override
            public int getCount() {
                return 16;
            }
        };
    }

    public boolean isNetworkEnabled() {
        return networkEnabled;
    }

    /**
     * Read-only status used by overlays such as Jade. These accessors must not
     * advance the work queue because tooltip requests can arrive independently
     * from the server tick.
     */
    public boolean isNetworkOnline() {
        return getMainNode().isOnline();
    }

    public boolean isProcessingFaulted() {
        return processingFaulted;
    }

    public long getBufferedOperationCount() {
        return getTotalQueuedOperations();
    }

    public long getBufferOperationLimit() {
        return MAX_ACCEPTED_OPERATIONS;
    }

    public long getCurrentOperationCount() {
        return Math.max(0, pendingOperations);
    }

    public int getParallelMultiplier() {
        return getParallelBatchSize();
    }

    public ItemStack getBufferedInputDisplay() {
        return activeInputKey == null || activeInputCount <= 0 ? ItemStack.EMPTY : activeInputKey.toStack(1);
    }

    public ItemStack getBufferedOutputDisplay() {
        return pendingOutputKey == null || pendingOutputCount <= 0 ? ItemStack.EMPTY : pendingOutputKey.toStack(1);
    }

    public long getBufferedOutputCount() {
        return Math.max(0, pendingOutputCount);
    }

    public void toggleNetworkEnabled() {
        if (!networkEnabled && processingFaulted && hasProcessingWork()) {
            return;
        }
        networkEnabled = !networkEnabled;
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        if (level == null || !networkEnabled) {
            return List.of();
        }
        List<IPatternDetails> result = new ArrayList<>(PATTERN_SLOT_COUNT);
        for (ItemStack pattern : patternSlots) {
            if (!pattern.isEmpty()) {
                IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
                if (details != null && isEnrichmentPattern(details)) {
                    result.add(details);
                }
            }
        }
        return result;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy() || !supportsPattern(details)) {
            return false;
        }

        AEItemKey inputKey = null;
        long inputCount = 0;
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<appeng.api.stacks.AEKey> entry : counter) {
                if (!(entry.getKey() instanceof AEItemKey itemKey)) {
                    return false;
                }
                long amount = entry.getLongValue();
                if (amount <= 0 || inputCount > Long.MAX_VALUE - amount) {
                    return false;
                }
                if (inputKey != null && !inputKey.equals(itemKey)) {
                    return false;
                }
                inputKey = itemKey;
                inputCount += amount;
            }
        }

        if (inputKey == null || inputCount <= 0) {
            return false;
        }

        ItemStack probe = inputKey.toStack(1);
        var recipe = findRecipe(probe);
        if (recipe == null || !matchesPatternOutput(recipe, details)) {
            return false;
        }

        long neededAmount = recipe.getInput().getNeededAmount(probe);
        if (neededAmount <= 0 || neededAmount > Integer.MAX_VALUE || inputCount % neededAmount != 0) {
            return false;
        }

        int needed = (int) neededAmount;
        long operations = inputCount / neededAmount;
        long current = getTotalQueuedOperations();
        if (current > MAX_ACCEPTED_OPERATIONS || operations > MAX_ACCEPTED_OPERATIONS - current) {
            return false;
        }

        enqueueJob(details.getDefinition().toStack(), inputKey, inputCount, operations, needed);
        setChanged();
        return true;
    }

    @Override
    public boolean isBusy() {
        if (!networkEnabled || processingFaulted) {
            return true;
        }
        return getTotalQueuedOperations() >= MAX_ACCEPTED_OPERATIONS;
    }

    private void enqueueJob(ItemStack patternDefinition, AEItemKey inputKey, long inputCount, long operations, int neededPerOperation) {
        if (operations <= 0 || inputKey == null || inputCount <= 0) {
            return;
        }

        ProcessingJob newJob = new ProcessingJob(patternDefinition, inputKey, inputCount, operations, neededPerOperation);

        if (pendingOperations <= 0 && activeInputKey == null && pendingOutputKey == null && queuedJobs.isEmpty()) {
            activateJob(newJob);
            return;
        }

        if (samePatternJob(activePatternDefinition, inputPerOperation, patternDefinition, neededPerOperation)
                && (activeInputKey == null || activeInputKey.equals(inputKey))
                && canAdd(activeInputCount, inputCount) && canAdd(pendingOperations, operations)) {
            if (activeInputKey == null) {
                activeInputKey = inputKey;
            }
            activeInputCount += inputCount;
            pendingOperations += operations;
            return;
        }

        for (ProcessingJob job : queuedJobs) {
            if (job.matches(patternDefinition, inputKey, neededPerOperation) && job.canGrow(inputCount, operations)) {
                job.inputCount += inputCount;
                job.operations += operations;
                return;
            }
        }
        queuedJobs.add(newJob);
    }

    private boolean samePatternJob(ItemStack firstPattern, int firstNeeded,
            ItemStack secondPattern, int secondNeeded) {
        return firstNeeded == secondNeeded
                && ItemStack.isSameItemSameComponents(firstPattern, secondPattern);
    }

    private int getParallelBatchSize() {
        return PARALLEL_MULTIPLIER[Math.min(Math.max(0, parallelUpgrades), PARALLEL_MULTIPLIER.length - 1)];
    }

    private static boolean canAdd(long first, long second) {
        return first >= 0 && second >= 0 && first <= Long.MAX_VALUE - second;
    }

    private long getTotalQueuedOperations() {
        long total = Math.max(0, pendingOperations);
        for (ProcessingJob job : queuedJobs) {
            if (Long.MAX_VALUE - total < job.operations) {
                return Long.MAX_VALUE;
            }
            total += Math.max(0, job.operations);
        }
        return total;
    }

    private boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0 || (activeInputKey != null && activeInputCount > 0)
                || (pendingOutputKey != null && pendingOutputCount > 0) || !queuedJobs.isEmpty();
    }

    public boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, MeEnrichmentChamberBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide()) {
            return;
        }

        flushOutput();
        finishActiveJobIfDrained();
        activateNextJobIfIdle();
        if (processingFaulted) {
            return;
        }
        if (level.hasNeighborSignal(worldPosition)) {
            return;
        }
        if ((pendingOutputKey != null && pendingOutputCount > 0) || pendingOperations <= 0) {
            return;
        }

        if (activeInputKey == null || activeInputCount < inputPerOperation) {
            markProcessingFault();
            setChanged();
            return;
        }
        ItemStack oneInput = activeInputKey.toStack(inputPerOperation);
        ItemStack result = getRecipeOutput(oneInput);
        if (result.isEmpty()) {
            markProcessingFault();
            setChanged();
            return;
        }
        AEItemKey resultKey = AEItemKey.of(result);
        int resultCount = result.getCount();

        int speed = Math.max(1, 1 + speedUpgrades);
        progress += speed;
        if (progress < BASE_PROCESSING_TICKS || energyStorage.getEnergyStored() < ENERGY_PER_OPERATION) {
            return;
        }

        long availableOperations = Math.min(getParallelBatchSize(), pendingOperations);
        availableOperations = Math.min(availableOperations, energyStorage.getEnergyStored() / ENERGY_PER_OPERATION);
        long consumedInput = availableOperations * inputPerOperation;
        if (consumedInput > activeInputCount) {
            availableOperations = activeInputCount / inputPerOperation;
            consumedInput = availableOperations * inputPerOperation;
        }
        if (availableOperations <= 0) {
            return;
        }

        long produced = availableOperations * resultCount;
        if (pendingOutputKey != null
                && (!pendingOutputKey.equals(resultKey) || !canAdd(pendingOutputCount, produced))) {
            markProcessingFault();
            setChanged();
            return;
        }
        for (int i = 0; i < availableOperations; i++) {
            energyStorage.consumeEnergy(ENERGY_PER_OPERATION);
        }
        activeInputCount -= consumedInput;
        pendingOperations -= availableOperations;
        if (pendingOutputKey == null) {
            pendingOutputKey = resultKey;
            pendingOutputCount = produced;
        } else {
            pendingOutputCount += produced;
        }
        progress = 0;
        setChanged();
        flushOutput();
        finishActiveJobIfDrained();
    }

    private void markProcessingFault() {
        processingFaulted = true;
        progress = 0;
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0 && activeInputCount <= 0 && activeInputKey != null) {
            activeInputKey = null;
        }
        if (pendingOperations <= 0 && activeInputKey == null && pendingOutputKey == null) {
            activePatternDefinition = ItemStack.EMPTY;
            inputPerOperation = 1;
            progress = 0;
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
    }

    private void activateNextJobIfIdle() {
        if (activeInputKey != null || pendingOperations > 0 || (pendingOutputKey != null && pendingOutputCount > 0)
                || queuedJobs.isEmpty()) {
            return;
        }
        ProcessingJob next = queuedJobs.removeFirst();
        activateJob(next);
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        activeInputKey = job.inputKey;
        activeInputCount = job.inputCount;
        pendingOperations = job.operations;
        inputPerOperation = Math.max(1, job.inputPerOperation);
        progress = 0;
    }

    private void flushOutput() {
        if (pendingOutputKey == null || pendingOutputCount <= 0 || !getMainNode().isOnline()) {
            return;
        }

        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        long inserted = grid.getStorageService().getInventory().insert(
                pendingOutputKey,
                pendingOutputCount,
                Actionable.MODULATE,
                IActionSource.ofMachine(this));
        if (inserted > 0) {
            pendingOutputCount = Math.max(0, pendingOutputCount - inserted);
            if (pendingOutputCount <= 0) {
                pendingOutputKey = null;
                pendingOutputCount = 0;
            }
            setChanged();
        }
    }

    public boolean returnAllResourcesToNetwork() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        networkEnabled = false;
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
        if (!getMainNode().isOnline()) {
            return false;
        }
        flushOutput();
        if (activeInputKey != null && activeInputCount > 0) {
            long inserted = insertWholeOperationsToNetwork(activeInputKey, activeInputCount, inputPerOperation);
            if (inserted > 0) {
                activeInputCount -= inserted;
                pendingOperations = activeInputCount / Math.max(1, inputPerOperation);
                if (activeInputCount % Math.max(1, inputPerOperation) != 0) {
                    processingFaulted = true;
                }
                if (activeInputCount <= 0) {
                    activeInputKey = null;
                }
            }
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            long inserted = insertWholeOperationsToNetwork(job.inputKey, job.inputCount, job.inputPerOperation);
            if (inserted > 0) {
                job.inputCount -= inserted;
                job.operations = job.inputCount / Math.max(1, job.inputPerOperation);
                if (job.inputCount % Math.max(1, job.inputPerOperation) != 0) {
                    processingFaulted = true;
                }
            }
            if (job.inputCount <= 0) {
                queuedJobs.remove(index);
            }
        }
        if (activeInputKey == null) {
            pendingOperations = 0;
            progress = 0;
            activePatternDefinition = ItemStack.EMPTY;
        }
        if (activeInputKey == null && pendingOutputKey == null && queuedJobs.isEmpty()) {
            processingFaulted = false;
        }
        setChanged();
        return activeInputKey == null && pendingOutputKey == null && queuedJobs.isEmpty();
    }

    private long insertWholeOperationsToNetwork(AEItemKey key, long count, int inputPerOperation) {
        var grid = getMainNode().getGrid();
        if (grid == null || key == null || count <= 0) {
            return 0;
        }
        long unit = Math.max(1, inputPerOperation);
        var inventory = grid.getStorageService().getInventory();
        long accepted = inventory.insert(key, count, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long wholeAmount = Math.min(count, Math.max(0, accepted));
        wholeAmount -= wholeAmount % unit;
        if (wholeAmount <= 0) {
            return 0;
        }
        long inserted = inventory.insert(key, wholeAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (inserted % unit != 0) {
            processingFaulted = true;
        }
        return Math.min(wholeAmount, Math.max(0, inserted));
    }

    private static ItemStack createDisplayStack(AEItemKey key, long count) {
        ItemStack display = key.toStack(1);
        display.setCount((int) Math.max(1, Math.min(count, display.getMaxStackSize())));
        return display;
    }

    private ItemStack getRecipeOutput(ItemStack input) {
        var recipe = findRecipe(input);
        return recipe == null ? ItemStack.EMPTY : recipe.getOutput(input);
    }

    private ItemStackToItemStackRecipe findRecipe(ItemStack input) {
        if (level == null) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_ENRICHING.get(), new SingleRecipeInput(input), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    private boolean supportsPattern(IPatternDetails details) {
        if (details.getInputs().length != 1 || details.getOutputs().size() != 1) {
            return false;
        }
        if (!isEnrichmentPattern(details)) {
            return false;
        }
        for (ItemStack pattern : patternSlots) {
            if (!pattern.isEmpty()) {
                IPatternDetails stored = PatternDetailsHelper.decodePattern(pattern, level);
                if (stored != null && stored.getDefinition().equals(details.getDefinition())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isEnrichmentPattern(IPatternDetails details) {
        if (level == null || details.getInputs().length != 1 || details.getOutputs().size() != 1) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        if (!(output.what() instanceof AEItemKey outputKey)) {
            return false;
        }
        for (GenericStack possibleInput : details.getInputs()[0].getPossibleInputs()) {
            if (possibleInput.what() instanceof AEItemKey inputKey) {
                int sampleCount = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, possibleInput.amount()));
                ItemStack result = getRecipeOutput(inputKey.toStack(sampleCount));
                if (!result.isEmpty() && outputKey.matches(result)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesPatternOutput(ItemStackToItemStackRecipe recipe, IPatternDetails details) {
        GenericStack output = details.getOutputs().getFirst();
        return output.what() instanceof AEItemKey outputKey
                && outputKey.matches(recipe.getResultItem(level.registryAccess()));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TaskDataVersion", TASK_DATA_VERSION);
        net.minecraft.nbt.ListTag patterns = new net.minecraft.nbt.ListTag();
        for (int index = 0; index < patternSlots.size(); index++) {
            ItemStack stack = patternSlots.get(index);
            if (!stack.isEmpty()) {
                CompoundTag pattern = (CompoundTag) stack.save(registries);
                pattern.putByte("Slot", (byte) index);
                patterns.add(pattern);
            }
        }
        tag.put("Patterns", patterns);
        if (activeInputKey != null && activeInputCount > 0) {
            CompoundTag inputTag = new CompoundTag();
            saveItemKey(activeInputKey, inputTag, registries);
            tag.put("PendingInputItem", inputTag);
            tag.putLong("PendingInputCount", activeInputCount);
        }
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            CompoundTag outputTag = new CompoundTag();
            saveItemKey(pendingOutputKey, outputTag, registries);
            tag.put("PendingOutputItem", outputTag);
            tag.putLong("PendingOutputCount", pendingOutputCount);
        }
        if (!activePatternDefinition.isEmpty()) {
            tag.put("ActivePattern", activePatternDefinition.save(registries));
        }
        net.minecraft.nbt.ListTag jobs = new net.minecraft.nbt.ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag savedJob = new CompoundTag();
            savedJob.put("Pattern", job.patternDefinition.save(registries));
            CompoundTag inputTag = new CompoundTag();
            saveItemKey(job.inputKey, inputTag, registries);
            savedJob.put("InputItem", inputTag);
            savedJob.putLong("InputCount", job.inputCount);
            savedJob.putLong("Operations", job.operations);
            savedJob.putInt("InputPerOperation", job.inputPerOperation);
            jobs.add(savedJob);
        }
        tag.put("ProcessingQueue", jobs);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putLong("PendingOperations", pendingOperations);
        tag.putInt("InputPerOperation", inputPerOperation);
        tag.putInt("Progress", progress);
        tag.putBoolean("ProcessingFaulted", processingFaulted);
        tag.putInt("SpeedUpgrades", speedUpgrades);
        tag.putInt("ParallelUpgrades", parallelUpgrades);
        tag.putBoolean("NetworkEnabled", networkEnabled);
        net.minecraft.nbt.ListTag upgrades = new net.minecraft.nbt.ListTag();
        for (int index = 0; index < upgradeSlots.size(); index++) {
            ItemStack stack = upgradeSlots.get(index);
            if (!stack.isEmpty()) {
                CompoundTag upgrade = (CompoundTag) stack.save(registries);
                upgrade.putByte("Slot", (byte) index);
                upgrades.add(upgrade);
            }
        }
        tag.put("Upgrades", upgrades);
    }

    private static void saveItemKey(AEItemKey key, CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Item", key.toStack(1).save(registries));
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        patternSlots.replaceAll(stack -> ItemStack.EMPTY);
        if (tag.contains("Patterns", net.minecraft.nbt.Tag.TAG_LIST)) {
            var patterns = tag.getList("Patterns", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < patterns.size(); index++) {
                CompoundTag pattern = patterns.getCompound(index);
                int slot = pattern.contains("Slot") ? pattern.getByte("Slot") : index;
                if (slot >= 0 && slot < patternSlots.size()) {
                    patternSlots.set(slot, ItemStack.parseOptional(registries, pattern));
                }
            }
        } else if (tag.contains("Pattern")) {
            patternSlots.set(0, ItemStack.parseOptional(registries, tag.getCompound("Pattern")));
        }

        int dataVersion = tag.contains("TaskDataVersion")
                ? tag.getInt("TaskDataVersion")
                : tag.contains("PendingInputItem") || tag.contains("PendingOutputItem") ? 2 : 1;
        activeInputKey = null;
        activeInputCount = 0;
        pendingOutputKey = null;
        pendingOutputCount = 0;

        if (dataVersion >= 2) {
            if (tag.contains("PendingInputItem")) {
                ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("PendingInputItem").getCompound("Item"));
                if (!template.isEmpty()) {
                    activeInputKey = AEItemKey.of(template);
                    activeInputCount = Math.max(0, tag.getLong("PendingInputCount"));
                }
            }
            if (tag.contains("PendingOutputItem")) {
                ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("PendingOutputItem").getCompound("Item"));
                if (!template.isEmpty()) {
                    pendingOutputKey = AEItemKey.of(template);
                    pendingOutputCount = Math.max(0, tag.getLong("PendingOutputCount"));
                    if (pendingOutputCount == 0) {
                        pendingOutputKey = null;
                    }
                }
            }
        } else {
            // Migrate from v1 NBT (ItemStack-based pending input/output).
            if (tag.contains("PendingInput")) {
                ItemStack input = ItemStack.parseOptional(registries, tag.getCompound("PendingInput"));
                if (!input.isEmpty()) {
                    activeInputKey = AEItemKey.of(input);
                    activeInputCount = input.getCount();
                }
            }
            if (tag.contains("PendingOutput")) {
                ItemStack output = ItemStack.parseOptional(registries, tag.getCompound("PendingOutput"));
                if (!output.isEmpty()) {
                    pendingOutputKey = AEItemKey.of(output);
                    pendingOutputCount = output.getCount();
                }
            }
        }

        activePatternDefinition = tag.contains("ActivePattern")
                ? ItemStack.parseOptional(registries, tag.getCompound("ActivePattern"))
                : ItemStack.EMPTY;
        queuedJobs.clear();
        if (tag.contains("ProcessingQueue", net.minecraft.nbt.Tag.TAG_LIST)) {
            var jobs = tag.getList("ProcessingQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < jobs.size(); index++) {
                CompoundTag savedJob = jobs.getCompound(index);
                ItemStack pattern = ItemStack.parseOptional(registries, savedJob.getCompound("Pattern"));
                AEItemKey inputKey;
                long inputCount;
                if (dataVersion >= 2 && savedJob.contains("InputItem")) {
                    ItemStack template = ItemStack.parseOptional(registries, savedJob.getCompound("InputItem").getCompound("Item"));
                    inputKey = template.isEmpty() ? null : AEItemKey.of(template);
                    inputCount = Math.max(0, savedJob.getLong("InputCount"));
                } else if (savedJob.contains("Input")) {
                    // Migrate from v1.
                    ItemStack input = ItemStack.parseOptional(registries, savedJob.getCompound("Input"));
                    inputKey = input.isEmpty() ? null : AEItemKey.of(input);
                    inputCount = input.getCount();
                } else {
                    inputKey = null;
                    inputCount = 0;
                }
                long operations = Math.max(0, savedJob.getLong("Operations"));
                int needed = Math.max(1, savedJob.getInt("InputPerOperation"));
                if (inputKey != null && inputCount > 0) {
                    queuedJobs.add(new ProcessingJob(pattern, inputKey, inputCount, operations, needed));
                }
            }
        }
        int savedEnergy = tag.getInt("Energy");
        pendingOperations = Math.max(0, tag.getLong("PendingOperations"));
        inputPerOperation = Math.max(1, tag.getInt("InputPerOperation"));
        progress = Math.max(0, tag.getInt("Progress"));
        processingFaulted = tag.getBoolean("ProcessingFaulted");
        if (activeInputKey == null || activeInputCount <= 0) {
            activeInputKey = null;
            activeInputCount = 0;
            pendingOperations = 0;
        } else if (activePatternDefinition.isEmpty()
                || activeInputCount % inputPerOperation != 0
                || pendingOperations != activeInputCount / inputPerOperation) {
            processingFaulted = true;
        }
        for (ProcessingJob job : queuedJobs) {
            if (job.patternDefinition.isEmpty()
                    || job.inputCount % job.inputPerOperation != 0
                    || job.operations != job.inputCount / job.inputPerOperation) {
                processingFaulted = true;
            }
        }
        speedUpgrades = Math.max(0, tag.getInt("SpeedUpgrades"));
        parallelUpgrades = Math.max(0, tag.getInt("ParallelUpgrades"));
        networkEnabled = !tag.contains("NetworkEnabled") || tag.getBoolean("NetworkEnabled");
        upgradeSlots.replaceAll(stack -> ItemStack.EMPTY);
        if (tag.contains("Upgrades", net.minecraft.nbt.Tag.TAG_LIST)) {
            var upgrades = tag.getList("Upgrades", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < upgrades.size(); index++) {
                CompoundTag upgrade = upgrades.getCompound(index);
                int slot = upgrade.contains("Slot") ? upgrade.getByte("Slot") : index;
                if (slot >= 0 && slot < upgradeSlots.size()) {
                    upgradeSlots.set(slot, ItemStack.parseOptional(registries, upgrade));
                }
            }
        }
        recalculateUpgrades();
        energyStorage.loadEnergy(savedEnergy);
    }

    @Override
    public int getContainerSize() {
        return PATTERN_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        if (patternSlots.stream().anyMatch(stack -> !stack.isEmpty())) {
            return false;
        }
        return upgradeSlots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < PATTERN_SLOT_COUNT ? patternSlots.get(slot) : upgradeSlots.get(slot - PATTERN_SLOT_COUNT);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot < PATTERN_SLOT_COUNT && !canTakePattern()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = current.split(amount);
        if (current.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        } else {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot < PATTERN_SLOT_COUNT && !canTakePattern()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = getItem(slot);
        if (slot < PATTERN_SLOT_COUNT) {
            patternSlots.set(slot, ItemStack.EMPTY);
        } else {
            upgradeSlots.set(slot - PATTERN_SLOT_COUNT, ItemStack.EMPTY);
            recalculateUpgrades();
        }
        setChanged();
        return result;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < PATTERN_SLOT_COUNT) {
            if (!stack.isEmpty() && !PatternDetailsHelper.isEncodedPattern(stack)) {
                return;
            }
            patternSlots.set(slot, stack.copyWithCount(Math.min(1, stack.getCount())));
            ICraftingProvider.requestUpdate(getMainNode());
        } else if (slot < getContainerSize()) {
            ItemStack accepted = stack.copy();
            accepted.setCount(Math.min(accepted.getCount(), getUpgradeLimitForSlot(slot, accepted)));
            upgradeSlots.set(slot - PATTERN_SLOT_COUNT, accepted);
            recalculateUpgrades();
        }
        setChanged();
    }

    private void recalculateUpgrades() {
        speedUpgrades = upgradeSlots.stream().filter(stack -> stack.is(io.github.shenfnx.mekanismae.registry.ModItems.SPEED_CARD.get()))
                .mapToInt(ItemStack::getCount).sum();
        parallelUpgrades = upgradeSlots.stream().filter(stack -> stack.is(io.github.shenfnx.mekanismae.registry.ModItems.PARALLEL_CARD.get()))
                .mapToInt(ItemStack::getCount).sum();
        int energyUpgrades = upgradeSlots.stream().filter(stack -> stack.is(io.github.shenfnx.mekanismae.registry.ModItems.ENERGY_CARD.get()))
                .mapToInt(ItemStack::getCount).sum();
        energyStorage.updateUpgrades(energyUpgrades);
    }

    public int getUpgradeCount(net.minecraft.world.item.Item item) {
        return upgradeSlots.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    public int getUpgradeLimitForSlot(int slot, ItemStack stack) {
        if (slot < PATTERN_SLOT_COUNT || slot >= getContainerSize() || !isSupportedUpgrade(stack)) {
            return 0;
        }
        ItemStack current = upgradeSlots.get(slot - PATTERN_SLOT_COUNT);
        int currentInSlot = ItemStack.isSameItemSameComponents(current, stack) ? current.getCount() : 0;
        return Math.max(0, MAX_UPGRADES_PER_TYPE - getUpgradeCount(stack.getItem()) + currentInSlot);
    }

    private boolean isSupportedUpgrade(ItemStack stack) {
        return stack.is(ModItems.SPEED_CARD.get()) || stack.is(ModItems.PARALLEL_CARD.get())
                || stack.is(ModItems.ENERGY_CARD.get());
    }

    @Override
    public void clearContent() {
        patternSlots.replaceAll(stack -> ItemStack.EMPTY);
        upgradeSlots.replaceAll(stack -> ItemStack.EMPTY);
        recalculateUpgrades();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < PATTERN_SLOT_COUNT ? PatternDetailsHelper.isEncodedPattern(stack)
                : isSupportedUpgrade(stack) && getUpgradeLimitForSlot(slot, stack) > 0;
    }

    /**
     * One isolated AE crafting submission. The encoded pattern identity stays
     * attached to its own input buffer so future multi-input/chemical machines
     * can extend this job without allowing resources from another pattern to mix.
     * Uses AEItemKey plus long counts so Int32 overflow is never a concern.
     */
    private static final class ProcessingJob {
        private final ItemStack patternDefinition;
        private final AEItemKey inputKey;
        private long inputCount;
        private long operations;
        private final int inputPerOperation;

        private ProcessingJob(ItemStack patternDefinition, AEItemKey inputKey, long inputCount, long operations, int inputPerOperation) {
            this.patternDefinition = patternDefinition.copyWithCount(1);
            this.inputKey = inputKey;
            this.inputCount = Math.max(0, inputCount);
            this.operations = Math.max(0, operations);
            this.inputPerOperation = Math.max(1, inputPerOperation);
        }

        private boolean matches(ItemStack pattern, AEItemKey otherInput, int neededPerOperation) {
            return inputPerOperation == neededPerOperation
                    && ItemStack.isSameItemSameComponents(patternDefinition, pattern)
                    && inputKey.equals(otherInput);
        }

        private boolean canGrow(long inputAddition, long operationAddition) {
            return canAdd(inputCount, inputAddition) && canAdd(operations, operationAddition);
        }
    }

    private final class MachineEnergyStorage extends EnergyStorage {
        private MachineEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        private void loadEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        private void updateUpgrades(int upgrades) {
            int safeUpgrades = Math.max(0, upgrades);
            capacity = BASE_ENERGY_CAPACITY + safeUpgrades * ENERGY_CAPACITY_PER_UPGRADE;
            maxReceive = BASE_ENERGY_RECEIVE + safeUpgrades * ENERGY_RECEIVE_PER_UPGRADE;
            energy = Math.min(energy, capacity);
        }

        private int consumeEnergy(int amount) {
            int consumed = Math.min(energy, Math.max(0, amount));
            if (consumed > 0) {
                energy -= consumed;
                setChanged();
            }
            return consumed;
        }

        private int getReceiveLimit() {
            return maxReceive;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    }

    private final class StrictEnergyHandler implements IStrictEnergyHandler {
        @Override
        public int getEnergyContainerCount() {
            return 1;
        }

        @Override
        public long getEnergy(int container) {
            return container == 0 ? energyStorage.getEnergyStored() : 0;
        }

        @Override
        public void setEnergy(int container, long energy) {
            if (container == 0) {
                energyStorage.loadEnergy((int) Math.min(Integer.MAX_VALUE, Math.max(0, energy)));
                setChanged();
            }
        }

        @Override
        public long getMaxEnergy(int container) {
            return container == 0 ? energyStorage.getMaxEnergyStored() : 0;
        }

        @Override
        public long getNeededEnergy(int container) {
            return container == 0 ? energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored() : 0;
        }

        @Override
        public long insertEnergy(int container, long amount, Action action) {
            if (container != 0 || amount <= 0) {
                return amount;
            }
            int value = (int) Math.min(Integer.MAX_VALUE, amount);
            int accepted = energyStorage.receiveEnergy(value, action.simulate());
            // Mekanism's insert contract returns the unaccepted remainder, not the amount inserted.
            return amount - accepted;
        }

        @Override
        public long extractEnergy(int container, long amount, Action action) {
            if (container != 0 || amount <= 0) {
                return 0;
            }
            int value = (int) Math.min(Integer.MAX_VALUE, amount);
            return energyStorage.extractEnergy(value, action.simulate());
        }
    }
}
