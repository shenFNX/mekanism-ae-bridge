package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.shenfnx.mekanismae.registry.ModItems;
import io.github.shenfnx.mekanismae.config.MachineType;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

public abstract class AbstractItemToItemMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 2;
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
    private final RecipeType<ItemStackToItemStackRecipe> recipeType;

    protected AbstractItemToItemMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, RecipeType<ItemStackToItemStackRecipe> recipeType,
            MachineType machineType) {
        super(type, pos, state, visualRepresentation, machineType);
        this.recipeType = recipeType;
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

    public ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored();
                    case 1 -> progress;
                    case 2 -> (int) Math.min(Integer.MAX_VALUE, getTotalQueuedOperations());
                    case 3 -> getProcessingTicks();
                    case 4 -> getMainNode().isOnline() ? 1 : 0;
                    case 5 -> energyStorage.getMaxEnergyStored();
                    case 6 -> networkEnabled ? 1 : 0;
                    case 7 -> speedUpgrades;
                    case 8 -> parallelUpgrades;
                    case 9 -> getUpgradeCount(ModItems.ENERGY_CARD.get());
                    case 10 -> energyStorage.getReceiveLimit();
                    case 11 -> (int) Math.min(Integer.MAX_VALUE, getTotalQueuedOperations());
                    case 12 -> (int) Math.min(Integer.MAX_VALUE, getBufferOperationLimit());
                    case 13 -> getParallelBatchSize();
                    case 14 -> (int) Math.min(Integer.MAX_VALUE, pendingOutputCount);
                    case 15 -> processingFaulted ? 1 : 0;
                    case 16 -> getSpeedMultiplier();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // All values are server-owned; clients only receive them through DataSlots.
            }

            @Override
            public int getCount() {
                return 17;
            }
        };
    }

    @Override
    public boolean isProcessingFaulted() {
        return processingFaulted;
    }

    public long getBufferedOperationCount() {
        return getTotalQueuedOperations();
    }

    public long getCurrentOperationCount() {
        return Math.max(0, pendingOperations);
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
        long bufferLimit = getBufferOperationLimit();
        if (current > bufferLimit || operations > bufferLimit - current) {
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
        return getTotalQueuedOperations() >= getBufferOperationLimit();
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

    @Override
    protected boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0 || (activeInputKey != null && activeInputCount > 0)
                || (pendingOutputKey != null && pendingOutputCount > 0) || !queuedJobs.isEmpty();
    }

    public boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
    }

    public static void serverTick(
            Level level, BlockPos pos, BlockState state, AbstractItemToItemMeMachineBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    private void tickServer() {
        if (level == null || level.isClientSide()) {
            return;
        }

        flushOutput();
        finishActiveJobIfDrained();
        activateNextJobIfIdle();
        updateVisualState();
        if (processingFaulted) {
            return;
        }
        if (shouldPauseForRedstone()) {
            return;
        }
        if ((pendingOutputKey != null && pendingOutputCount > 0) || pendingOperations <= 0) {
            return;
        }

        if (activeInputKey == null || activeInputCount < inputPerOperation) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        ItemStack oneInput = activeInputKey.toStack(inputPerOperation);
        ItemStack result = getRecipeOutput(oneInput);
        if (result.isEmpty()) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        AEItemKey resultKey = AEItemKey.of(result);
        int resultCount = result.getCount();

        int speed = getSpeedMultiplier();
        int processingTicks = getProcessingTicks();
        progress = (int) Math.min(processingTicks, (long) Math.max(0, progress) + speed);
        updateVisualState();
        int energyPerOperation = getEnergyPerOperation();
        if (progress < processingTicks || energyStorage.getEnergyStored() < energyPerOperation) {
            return;
        }

        long availableOperations = Math.min(getParallelBatchSize(), pendingOperations);
        availableOperations = Math.min(availableOperations, energyStorage.getEnergyStored() / energyPerOperation);
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
            updateVisualState();
            setChanged();
            return;
        }
        for (int i = 0; i < availableOperations; i++) {
            energyStorage.consumeEnergy(energyPerOperation);
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
        activateNextJobIfIdle();
        updateVisualState();
    }

    /**
     * True only while the machine can genuinely advance its current recipe. Queued
     * work by itself is not enough: a redstone pause, blocked output, fault, missing
     * input, or insufficient energy all switch the working lamp off.
     */
    protected final boolean isVisuallyWorking() {
        return !processingFaulted
                && !shouldPauseForRedstone()
                && pendingOperations > 0
                && activeInputKey != null
                && activeInputCount >= inputPerOperation
                && (pendingOutputKey == null || pendingOutputCount <= 0)
                && energyStorage.getEnergyStored() >= getEnergyPerOperation();
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
                .getRecipeFor(recipeType, new SingleRecipeInput(input), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    private boolean supportsPattern(IPatternDetails details) {
        if (details.getInputs().length != 1 || details.getOutputs().size() != 1) {
            return false;
        }
        if (!isPatternForThisMachine(details)) {
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

    @Override
    protected boolean isPatternForThisMachine(IPatternDetails details) {
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
        tag.putLong("PendingOperations", pendingOperations);
        tag.putInt("InputPerOperation", inputPerOperation);
        tag.putInt("Progress", progress);
        tag.putBoolean("ProcessingFaulted", processingFaulted);
    }

    private static void saveItemKey(AEItemKey key, CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Item", key.toStack(1).save(registries));
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
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

}
