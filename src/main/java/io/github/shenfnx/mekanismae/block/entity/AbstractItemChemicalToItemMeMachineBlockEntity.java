package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackChemicalToItemStackRecipe;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

/**
 * Shared GTNH-style task ledger for Mekanism item + chemical -> item recipes.
 * Every queued submission retains its encoded pattern identity and its own item
 * and chemical balances, so compatible resources from different patterns never mix.
 */
public abstract class AbstractItemChemicalToItemMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 3;
    private final RecipeType<? extends ItemStackChemicalToItemStackRecipe> recipeType;
    private final String rejectionLogName;
    private AEItemKey activeItemKey;
    private long activeItemCount;
    private MekanismKey activeChemicalKey;
    private long activeChemicalCount;
    private AEItemKey pendingOutputKey;
    private long pendingOutputCount;
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private int itemPerOperation = 1;
    private long chemicalPerOperation = 1;
    private int progress;
    private boolean processingFaulted;
    private long nextPatternRejectLogTime;

    protected AbstractItemChemicalToItemMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType,
            RecipeType<? extends ItemStackChemicalToItemStackRecipe> recipeType, String rejectionLogName) {
        super(type, pos, state, visualRepresentation, machineType);
        this.recipeType = recipeType;
        this.rejectionLogName = rejectionLogName;
    }

    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        if (activeItemKey == null || activeItemCount <= 0) {
            return ItemStack.EMPTY;
        }
        return createDisplayStack(activeItemKey, Math.min(activeItemCount, itemPerOperation));
    }

    public ItemStack getProcessingOutputDisplay() {
        activateNextJobIfIdle();
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            return createDisplayStack(pendingOutputKey, pendingOutputCount);
        }
        if (activeItemKey == null || activeItemCount <= 0 || activeChemicalKey == null) {
            return ItemStack.EMPTY;
        }
        return getRecipeOutput(activeItemKey.toStack(Math.max(1, itemPerOperation)),
                activeChemicalKey.withAmount(Math.max(1, chemicalPerOperation)));
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
                    case 16 -> activeChemicalKey == null ? -1
                            : MekanismAPI.CHEMICAL_REGISTRY.getId(activeChemicalKey.getStack().getChemical());
                    case 17 -> (int) Math.min(Integer.MAX_VALUE, activeChemicalCount);
                    case 18 -> getSpeedMultiplier();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // All values are server-owned; clients only receive them through DataSlots.
            }

            @Override
            public int getCount() {
                return 19;
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
        return activeItemKey == null || activeItemCount <= 0 ? ItemStack.EMPTY : activeItemKey.toStack(1);
    }

    public ItemStack getBufferedOutputDisplay() {
        return pendingOutputKey == null || pendingOutputCount <= 0 ? ItemStack.EMPTY : pendingOutputKey.toStack(1);
    }

    public long getBufferedOutputCount() {
        return Math.max(0, pendingOutputCount);
    }

    public MekanismKey getBufferedChemicalKey() {
        return activeChemicalCount > 0 ? activeChemicalKey : null;
    }

    public long getBufferedChemicalCount() {
        return Math.max(0, activeChemicalCount);
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide()) {
            return rejectPattern("missing server level", details, inputs);
        }
        if (!networkEnabled) {
            return rejectPattern("task intake disabled", details, inputs);
        }
        if (isBusy()) {
            return rejectPattern("machine busy or faulted", details, inputs);
        }
        if (!supportsPattern(details)) {
            return rejectPattern("pattern definition is not installed or structurally supported", details, inputs);
        }

        AEItemKey itemKey = null;
        long itemCount = 0;
        MekanismKey chemicalKey = null;
        long chemicalCount = 0;
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<appeng.api.stacks.AEKey> entry : counter) {
                long amount = entry.getLongValue();
                if (amount <= 0) {
                    return rejectPattern("non-positive input amount", details, inputs);
                }
                if (entry.getKey() instanceof AEItemKey foundItem) {
                    if (itemKey != null && !itemKey.equals(foundItem) || !canAdd(itemCount, amount)) {
                        return rejectPattern("multiple item keys or item count overflow", details, inputs);
                    }
                    itemKey = foundItem;
                    itemCount += amount;
                } else if (entry.getKey() instanceof MekanismKey foundChemical) {
                    if (chemicalKey != null && !chemicalKey.equals(foundChemical) || !canAdd(chemicalCount, amount)) {
                        return rejectPattern("multiple chemical keys or chemical count overflow", details, inputs);
                    }
                    chemicalKey = foundChemical;
                    chemicalCount += amount;
                } else {
                    return rejectPattern("unsupported AE input key type", details, inputs);
                }
            }
        }

        if (itemKey == null || itemCount <= 0 || chemicalKey == null || chemicalCount <= 0) {
            return rejectPattern("missing delivered item or chemical", details, inputs);
        }

        ItemStack itemProbe = itemKey.toStack(1);
        ChemicalStack chemicalProbe = chemicalKey.withAmount(chemicalCount);
        var recipe = findRecipe(itemProbe, chemicalProbe);
        if (recipe == null) {
            return rejectPattern("Mekanism found no matching recipe", details, inputs);
        }
        if (!matchesPatternOutput(recipe, details, itemProbe, chemicalProbe)) {
            GenericStack declaredOutput = details.getOutputs().getFirst();
            ItemStack recipeOutput = recipe.getOutput(itemProbe, chemicalProbe);
            return rejectPattern("recipe output differs from pattern (declared=" + declaredOutput.what().getId()
                    + " x" + declaredOutput.amount() + ", recipe=" + recipeOutput.getItem()
                    + " x" + recipeOutput.getCount() + ")", details, inputs);
        }

        long itemNeeded = recipe.getItemInput().getNeededAmount(itemProbe);
        long chemicalNeeded = getChemicalNeededPerOperation(recipe, chemicalProbe);
        if (itemNeeded <= 0 || itemNeeded > Integer.MAX_VALUE || chemicalNeeded <= 0
                || itemCount % itemNeeded != 0 || chemicalCount % chemicalNeeded != 0) {
            return rejectPattern("delivered amounts are not whole recipe operations (item=" + itemCount
                    + "/" + itemNeeded + ", chemical=" + chemicalCount + "/" + chemicalNeeded + ")", details, inputs);
        }

        long operations = itemCount / itemNeeded;
        if (operations <= 0 || operations != chemicalCount / chemicalNeeded) {
            return rejectPattern("item and chemical operation counts differ", details, inputs);
        }
        long current = getTotalQueuedOperations();
        long bufferLimit = getBufferOperationLimit();
        if (current > bufferLimit || operations > bufferLimit - current) {
            return rejectPattern("internal operation buffer full", details, inputs);
        }

        enqueueJob(details.getDefinition().toStack(), itemKey, itemCount, (int) itemNeeded,
                chemicalKey, chemicalCount, chemicalNeeded, operations);
        setChanged();
        return true;
    }

    private boolean rejectPattern(String reason, IPatternDetails details, KeyCounter[] inputs) {
        if (level != null && !level.isClientSide() && level.getGameTime() >= nextPatternRejectLogTime) {
            nextPatternRejectLogTime = level.getGameTime() + 20;
            StringBuilder delivered = new StringBuilder();
            for (int counterIndex = 0; counterIndex < inputs.length; counterIndex++) {
                if (counterIndex > 0) {
                    delivered.append("; ");
                }
                delivered.append(counterIndex).append('[');
                boolean first = true;
                for (Object2LongMap.Entry<AEKey> entry : inputs[counterIndex]) {
                    if (!first) {
                        delivered.append(", ");
                    }
                    first = false;
                    delivered.append(entry.getKey().getClass().getSimpleName())
                            .append(':').append(entry.getKey().getId())
                            .append('=').append(entry.getLongValue());
                }
                delivered.append(']');
            }
            MekanismAeMod.LOGGER.warn("{} rejected pattern {} at {}: {}; delivered={}",
                    rejectionLogName, details.getDefinition(), worldPosition, reason, delivered);
        }
        return false;
    }

    @Override
    public boolean isBusy() {
        if (!networkEnabled || processingFaulted) {
            return true;
        }
        return getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    private void enqueueJob(ItemStack patternDefinition, AEItemKey itemKey, long itemCount, int itemNeeded,
            MekanismKey chemicalKey, long chemicalCount, long chemicalNeeded, long operations) {
        if (operations <= 0 || itemKey == null || itemCount <= 0 || chemicalKey == null || chemicalCount <= 0) {
            return;
        }

        ProcessingJob newJob = new ProcessingJob(patternDefinition, itemKey, itemCount, itemNeeded,
                chemicalKey, chemicalCount, chemicalNeeded, operations);

        if (pendingOperations <= 0 && activeItemKey == null && activeChemicalKey == null
                && pendingOutputKey == null && queuedJobs.isEmpty()) {
            activateJob(newJob);
            return;
        }

        if (samePatternJob(activePatternDefinition, itemPerOperation, chemicalPerOperation,
                patternDefinition, itemNeeded, chemicalNeeded)
                && (activeItemKey == null || activeItemKey.equals(itemKey))
                && (activeChemicalKey == null || activeChemicalKey.equals(chemicalKey))
                && canAdd(activeItemCount, itemCount) && canAdd(activeChemicalCount, chemicalCount)
                && canAdd(pendingOperations, operations)) {
            if (activeItemKey == null) {
                activeItemKey = itemKey;
            }
            if (activeChemicalKey == null) {
                activeChemicalKey = chemicalKey;
            }
            activeItemCount += itemCount;
            activeChemicalCount += chemicalCount;
            pendingOperations += operations;
            return;
        }

        for (ProcessingJob job : queuedJobs) {
            if (job.matches(patternDefinition, itemKey, itemNeeded, chemicalKey, chemicalNeeded)
                    && job.canGrow(itemCount, chemicalCount, operations)) {
                job.itemCount += itemCount;
                job.chemicalCount += chemicalCount;
                job.operations += operations;
                return;
            }
        }
        queuedJobs.add(newJob);
    }

    private boolean samePatternJob(ItemStack firstPattern, int firstItemNeeded, long firstChemicalNeeded,
            ItemStack secondPattern, int secondItemNeeded, long secondChemicalNeeded) {
        return firstItemNeeded == secondItemNeeded && firstChemicalNeeded == secondChemicalNeeded
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
        return getTotalQueuedOperations() > 0 || (activeItemKey != null && activeItemCount > 0)
                || (activeChemicalKey != null && activeChemicalCount > 0)
                || (pendingOutputKey != null && pendingOutputCount > 0) || !queuedJobs.isEmpty();
    }

    public boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
            AbstractItemChemicalToItemMeMachineBlockEntity blockEntity) {
        blockEntity.tickServer();
    }

    @Override
    public final void tickServer() {
        if (level == null || level.isClientSide()) {
            return;
        }
        chargeFromNetwork();

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

        if (activeItemKey == null || activeItemCount < itemPerOperation
                || activeChemicalKey == null || activeChemicalCount < chemicalPerOperation) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        ItemStack oneInput = activeItemKey.toStack(itemPerOperation);
        ChemicalStack oneChemical = activeChemicalKey.withAmount(chemicalPerOperation);
        ItemStack result = getRecipeOutput(oneInput, oneChemical);
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
        availableOperations = Math.min(availableOperations, activeItemCount / itemPerOperation);
        availableOperations = Math.min(availableOperations, activeChemicalCount / chemicalPerOperation);
        if (availableOperations <= 0) {
            return;
        }

        long consumedItems = availableOperations * itemPerOperation;
        long consumedChemical = availableOperations * chemicalPerOperation;
        long produced = availableOperations * resultCount;
        if (pendingOutputKey != null
                && (!pendingOutputKey.equals(resultKey) || !canAdd(pendingOutputCount, produced))) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        energyStorage.consumeEnergy(getEnergyCostForOperations(availableOperations));
        activeItemCount -= consumedItems;
        activeChemicalCount -= consumedChemical;
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

    @Override
    protected final boolean isVisuallyWorking() {
        return !processingFaulted
                && !shouldPauseForRedstone()
                && pendingOperations > 0
                && activeItemKey != null
                && activeItemCount >= itemPerOperation
                && activeChemicalKey != null
                && activeChemicalCount >= chemicalPerOperation
                && (pendingOutputKey == null || pendingOutputCount <= 0)
                && energyStorage.getEnergyStored() >= getEnergyPerOperation();
    }

    private void markProcessingFault() {
        processingFaulted = true;
        progress = 0;
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0 && activeItemCount <= 0 && activeItemKey != null) {
            activeItemKey = null;
        }
        if (pendingOperations <= 0 && activeChemicalCount <= 0 && activeChemicalKey != null) {
            activeChemicalKey = null;
        }
        if (pendingOperations <= 0 && activeItemKey == null && activeChemicalKey == null && pendingOutputKey == null) {
            activePatternDefinition = ItemStack.EMPTY;
            itemPerOperation = 1;
            chemicalPerOperation = 1;
            progress = 0;
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
    }

    private void activateNextJobIfIdle() {
        if (activeItemKey != null || activeChemicalKey != null || pendingOperations > 0
                || (pendingOutputKey != null && pendingOutputCount > 0)
                || queuedJobs.isEmpty()) {
            return;
        }
        ProcessingJob next = queuedJobs.removeFirst();
        activateJob(next);
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        activeItemKey = job.itemKey;
        activeItemCount = job.itemCount;
        activeChemicalKey = job.chemicalKey;
        activeChemicalCount = job.chemicalCount;
        pendingOperations = job.operations;
        itemPerOperation = Math.max(1, job.itemPerOperation);
        chemicalPerOperation = Math.max(1, job.chemicalPerOperation);
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
        if (activeItemKey != null && activeItemCount > 0 || activeChemicalKey != null && activeChemicalCount > 0) {
            ReturnedInputs returned;
            if (processingFaulted) {
                returned = new ReturnedInputs(returnKeyToNetwork(activeItemKey, activeItemCount),
                        returnKeyToNetwork(activeChemicalKey, activeChemicalCount));
            } else if (activeItemKey != null && activeChemicalKey != null
                    && activeItemCount > 0 && activeChemicalCount > 0) {
                returned = returnJobInputs(activeItemKey, activeItemCount, itemPerOperation,
                        activeChemicalKey, activeChemicalCount, chemicalPerOperation);
            } else {
                returned = ReturnedInputs.NONE;
                processingFaulted = true;
            }
            activeItemCount -= returned.items();
            activeChemicalCount -= returned.chemical();
            pendingOperations = Math.min(activeItemCount / Math.max(1, itemPerOperation),
                    activeChemicalCount / Math.max(1, chemicalPerOperation));
            if (activeItemCount <= 0) {
                activeItemKey = null;
            }
            if (activeChemicalCount <= 0) {
                activeChemicalKey = null;
            }
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            ReturnedInputs returned = processingFaulted
                    ? new ReturnedInputs(returnKeyToNetwork(job.itemKey, job.itemCount),
                            returnKeyToNetwork(job.chemicalKey, job.chemicalCount))
                    : returnJobInputs(job.itemKey, job.itemCount, job.itemPerOperation,
                            job.chemicalKey, job.chemicalCount, job.chemicalPerOperation);
            job.itemCount -= returned.items();
            job.chemicalCount -= returned.chemical();
            job.operations = Math.min(job.itemCount / job.itemPerOperation,
                    job.chemicalCount / job.chemicalPerOperation);
            if (job.itemCount <= 0 && job.chemicalCount <= 0) {
                queuedJobs.remove(index);
            }
        }
        if (activeItemKey == null && activeChemicalKey == null) {
            pendingOperations = 0;
            progress = 0;
            activePatternDefinition = ItemStack.EMPTY;
        }
        if (activeItemKey == null && activeChemicalKey == null && pendingOutputKey == null && queuedJobs.isEmpty()) {
            processingFaulted = false;
        }
        setChanged();
        return activeItemKey == null && activeChemicalKey == null && pendingOutputKey == null && queuedJobs.isEmpty();
    }

    private long returnKeyToNetwork(AEKey key, long count) {
        var grid = getMainNode().getGrid();
        if (grid == null || key == null || count <= 0) {
            return 0;
        }
        return Math.max(0, grid.getStorageService().getInventory().insert(
                key, count, Actionable.MODULATE, IActionSource.ofMachine(this)));
    }

    private ReturnedInputs returnJobInputs(AEItemKey itemKey, long itemCount, int itemUnit,
            MekanismKey chemicalKey, long chemicalCount, long chemicalUnit) {
        var grid = getMainNode().getGrid();
        if (grid == null || itemKey == null || chemicalKey == null || itemCount <= 0 || chemicalCount <= 0) {
            return ReturnedInputs.NONE;
        }
        var inventory = grid.getStorageService().getInventory();
        long possibleOperations = Math.min(itemCount / itemUnit, chemicalCount / chemicalUnit);
        long itemAmount = possibleOperations * itemUnit;
        long chemicalAmount = possibleOperations * chemicalUnit;
        long acceptedItems = inventory.insert(itemKey, itemAmount, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long acceptedChemical = inventory.insert(chemicalKey, chemicalAmount, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long acceptedOperations = Math.min(Math.max(0, acceptedItems) / itemUnit,
                Math.max(0, acceptedChemical) / chemicalUnit);
        if (acceptedOperations <= 0) {
            return ReturnedInputs.NONE;
        }
        itemAmount = acceptedOperations * itemUnit;
        chemicalAmount = acceptedOperations * chemicalUnit;
        long insertedItems = inventory.insert(itemKey, itemAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        long insertedChemical = inventory.insert(chemicalKey, chemicalAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (insertedItems != itemAmount || insertedChemical != chemicalAmount
                || insertedItems % itemUnit != 0 || insertedChemical % chemicalUnit != 0) {
            processingFaulted = true;
        }
        return new ReturnedInputs(Math.min(itemAmount, Math.max(0, insertedItems)),
                Math.min(chemicalAmount, Math.max(0, insertedChemical)));
    }

    private static ItemStack createDisplayStack(AEItemKey key, long count) {
        ItemStack display = key.toStack(1);
        display.setCount((int) Math.max(1, Math.min(count, display.getMaxStackSize())));
        return display;
    }

    private ItemStack getRecipeOutput(ItemStack item, ChemicalStack chemical) {
        var recipe = findRecipe(item, chemical);
        return recipe == null ? ItemStack.EMPTY : recipe.getOutput(item, chemical);
    }

    private ItemStackChemicalToItemStackRecipe findRecipe(ItemStack item, ChemicalStack chemical) {
        if (level == null) {
            return null;
        }
        return level.getRecipeManager()
                .getRecipeFor(recipeType,
                        new SingleItemChemicalRecipeInput(item, chemical), level)
                .map(holder -> holder.value())
                .orElse(null);
    }

    /**
     * Mirrors Mekanism's ItemStackChemicalToItemStackRecipeCategory exactly.
     * Per-tick recipes use the official advanced-machine base duration, never
     * this ME machine's configured speed or installed speed cards.
     */
    private long getChemicalNeededPerOperation(ItemStackChemicalToItemStackRecipe recipe,
            ChemicalStack chemical) {
        long amount = recipe.getChemicalInput().getNeededAmount(chemical);
        if (recipe.perTickUsage() && amount > 0) {
            int jeiUsageTicks = TileEntityAdvancedElectricMachine.BASE_TICKS_REQUIRED;
            return amount > Long.MAX_VALUE / jeiUsageTicks
                    ? Long.MAX_VALUE
                    : amount * jeiUsageTicks;
        }
        return amount;
    }

    private boolean supportsPattern(IPatternDetails details) {
        if (details.getInputs().length != 2 || details.getOutputs().size() != 1) {
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
        if (level == null || details.getInputs().length != 2 || details.getOutputs().size() != 1) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        if (!(output.what() instanceof AEItemKey) || output.amount() <= 0) {
            return false;
        }
        List<GenericStack> itemInputs = new ArrayList<>();
        List<GenericStack> chemicalInputs = new ArrayList<>();
        for (var input : details.getInputs()) {
            Boolean itemSlot = null;
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                if (possibleInput.amount() <= 0) {
                    return false;
                }
                if (possibleInput.what() instanceof AEItemKey) {
                    if (itemSlot == Boolean.FALSE) {
                        return false;
                    }
                    itemSlot = true;
                    itemInputs.add(possibleInput);
                } else if (possibleInput.what() instanceof MekanismKey) {
                    if (itemSlot == Boolean.TRUE) {
                        return false;
                    }
                    itemSlot = false;
                    chemicalInputs.add(possibleInput);
                } else {
                    return false;
                }
            }
            if (itemSlot == null) {
                return false;
            }
        }
        if (itemInputs.isEmpty() || chemicalInputs.isEmpty()) {
            return false;
        }

        // Structural compatibility is not enough: otherwise every item+chemical
        // machine advertises every other machine's pattern to AE2. At least one
        // declared alternative pair must match this machine's own recipe type
        // and output item. Do not enforce the exact operation ratio here: AE2
        // calls this method while discovering installed patterns, and JEI-backed
        // processing patterns may normalize the displayed stack amount into the
        // input multiplier. pushPattern performs the authoritative amount and
        // operation-ratio validation once AE2 delivers the real KeyCounters.
        for (GenericStack itemInput : itemInputs) {
            AEItemKey itemKey = (AEItemKey) itemInput.what();
            long declaredItems = getDeclaredInputAmount(details, itemKey);
            if (declaredItems <= 0 || declaredItems > Integer.MAX_VALUE) {
                continue;
            }
            ItemStack itemProbe = itemKey.toStack((int) declaredItems);
            for (GenericStack chemicalInput : chemicalInputs) {
                MekanismKey chemicalKey = (MekanismKey) chemicalInput.what();
                long declaredChemical = getDeclaredInputAmount(details, chemicalKey);
                if (declaredChemical <= 0) {
                    continue;
                }
                ChemicalStack chemicalProbe = chemicalKey.withAmount(declaredChemical);
                ItemStackChemicalToItemStackRecipe recipe = findRecipe(itemProbe, chemicalProbe);
                if (recipe != null && outputItemMatches(recipe, details)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean outputItemMatches(ItemStackChemicalToItemStackRecipe recipe, IPatternDetails details) {
        GenericStack output = details.getOutputs().getFirst();
        return output.what() instanceof AEItemKey outputKey
                && outputKey.matches(recipe.getResultItem(level.registryAccess()));
    }

    private boolean matchesPatternOutput(ItemStackChemicalToItemStackRecipe recipe, IPatternDetails details,
            ItemStack item, ChemicalStack chemical) {
        GenericStack output = details.getOutputs().getFirst();
        ItemStack result = recipe.getOutput(item, chemical);
        if (!(output.what() instanceof AEItemKey outputKey) || !outputKey.matches(result)) {
            return false;
        }

        AEItemKey itemKey = AEItemKey.of(item);
        MekanismKey chemicalKey = MekanismKey.of(chemical);
        if (itemKey == null || chemicalKey == null) {
            return false;
        }
        long declaredItems = getDeclaredInputAmount(details, itemKey);
        long declaredChemical = getDeclaredInputAmount(details, chemicalKey);
        long itemNeeded = recipe.getItemInput().getNeededAmount(item);
        long chemicalNeeded = getChemicalNeededPerOperation(recipe, chemical);
        if (declaredItems <= 0 || declaredChemical <= 0 || itemNeeded <= 0 || chemicalNeeded <= 0
                || declaredItems % itemNeeded != 0 || declaredChemical % chemicalNeeded != 0) {
            return false;
        }
        long declaredOperations = declaredItems / itemNeeded;
        if (declaredOperations != declaredChemical / chemicalNeeded
                || declaredOperations > Long.MAX_VALUE / Math.max(1, result.getCount())) {
            return false;
        }
        return output.amount() == declaredOperations * result.getCount();
    }

    private static long getDeclaredInputAmount(IPatternDetails details, AEKey key) {
        for (var input : details.getInputs()) {
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                if (key.equals(possibleInput.what())) {
                    long amount = possibleInput.amount();
                    long multiplier = input.getMultiplier();
                    if (amount <= 0 || multiplier <= 0 || amount > Long.MAX_VALUE / multiplier) {
                        return 0;
                    }
                    return amount * multiplier;
                }
            }
        }
        return 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TaskDataVersion", TASK_DATA_VERSION);
        if (activeItemKey != null && activeItemCount > 0) {
            CompoundTag itemTag = new CompoundTag();
            saveItemKey(activeItemKey, itemTag, registries);
            tag.put("ActiveItem", itemTag);
            tag.putLong("ActiveItemCount", activeItemCount);
        }
        if (activeChemicalKey != null && activeChemicalCount > 0) {
            tag.put("ActiveChemical", activeChemicalKey.toTag(registries));
            tag.putLong("ActiveChemicalCount", activeChemicalCount);
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
            CompoundTag itemTag = new CompoundTag();
            saveItemKey(job.itemKey, itemTag, registries);
            savedJob.put("Item", itemTag);
            savedJob.putLong("ItemCount", job.itemCount);
            savedJob.put("Chemical", job.chemicalKey.toTag(registries));
            savedJob.putLong("ChemicalCount", job.chemicalCount);
            savedJob.putLong("Operations", job.operations);
            savedJob.putInt("ItemPerOperation", job.itemPerOperation);
            savedJob.putLong("ChemicalPerOperation", job.chemicalPerOperation);
            jobs.add(savedJob);
        }
        tag.put("ProcessingQueue", jobs);
        tag.putLong("PendingOperations", pendingOperations);
        tag.putInt("ItemPerOperation", itemPerOperation);
        tag.putLong("ChemicalPerOperation", chemicalPerOperation);
        tag.putInt("Progress", progress);
        tag.putBoolean("ProcessingFaulted", processingFaulted);
    }

    private static void saveItemKey(AEItemKey key, CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("Item", key.toStack(1).save(registries));
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        int dataVersion = tag.getInt("TaskDataVersion");
        activeItemKey = null;
        activeItemCount = 0;
        activeChemicalKey = null;
        activeChemicalCount = 0;
        pendingOutputKey = null;
        pendingOutputCount = 0;
        if (dataVersion == TASK_DATA_VERSION && tag.contains("ActiveItem")) {
            ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("ActiveItem").getCompound("Item"));
            if (!template.isEmpty()) {
                activeItemKey = AEItemKey.of(template);
                activeItemCount = Math.max(0, tag.getLong("ActiveItemCount"));
            }
        }
        if (dataVersion == TASK_DATA_VERSION && tag.contains("ActiveChemical")) {
            activeChemicalKey = MekanismKey.fromTag(registries, tag.getCompound("ActiveChemical"));
            activeChemicalCount = Math.max(0, tag.getLong("ActiveChemicalCount"));
        }
        if (tag.contains("PendingOutputItem")) {
            ItemStack template = ItemStack.parseOptional(registries, tag.getCompound("PendingOutputItem").getCompound("Item"));
            if (!template.isEmpty()) {
                pendingOutputKey = AEItemKey.of(template);
                pendingOutputCount = Math.max(0, tag.getLong("PendingOutputCount"));
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
                ItemStack template = ItemStack.parseOptional(registries, savedJob.getCompound("Item").getCompound("Item"));
                AEItemKey itemKey = template.isEmpty() ? null : AEItemKey.of(template);
                long itemCount = Math.max(0, savedJob.getLong("ItemCount"));
                MekanismKey chemicalKey = MekanismKey.fromTag(registries, savedJob.getCompound("Chemical"));
                long chemicalCount = Math.max(0, savedJob.getLong("ChemicalCount"));
                long operations = Math.max(0, savedJob.getLong("Operations"));
                int itemNeeded = Math.max(1, savedJob.getInt("ItemPerOperation"));
                long chemicalNeeded = Math.max(1, savedJob.getLong("ChemicalPerOperation"));
                if (itemKey != null && itemCount > 0 && chemicalKey != null && chemicalCount > 0) {
                    queuedJobs.add(new ProcessingJob(pattern, itemKey, itemCount, itemNeeded,
                            chemicalKey, chemicalCount, chemicalNeeded, operations));
                }
            }
        }
        pendingOperations = Math.max(0, tag.getLong("PendingOperations"));
        itemPerOperation = Math.max(1, tag.getInt("ItemPerOperation"));
        chemicalPerOperation = Math.max(1, tag.getLong("ChemicalPerOperation"));
        progress = Math.max(0, tag.getInt("Progress"));
        processingFaulted = tag.getBoolean("ProcessingFaulted");
        if (activeItemKey == null || activeItemCount <= 0 || activeChemicalKey == null || activeChemicalCount <= 0) {
            activeItemKey = null;
            activeItemCount = 0;
            activeChemicalKey = null;
            activeChemicalCount = 0;
            pendingOperations = 0;
        } else if (activePatternDefinition.isEmpty()
                || activeItemCount % itemPerOperation != 0
                || activeChemicalCount % chemicalPerOperation != 0
                || pendingOperations != activeItemCount / itemPerOperation
                || pendingOperations != activeChemicalCount / chemicalPerOperation) {
            processingFaulted = true;
        }
        for (ProcessingJob job : queuedJobs) {
            if (job.patternDefinition.isEmpty()
                    || job.itemCount % job.itemPerOperation != 0
                    || job.chemicalCount % job.chemicalPerOperation != 0
                    || job.operations != job.itemCount / job.itemPerOperation
                    || job.operations != job.chemicalCount / job.chemicalPerOperation) {
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
        private final AEItemKey itemKey;
        private long itemCount;
        private final int itemPerOperation;
        private final MekanismKey chemicalKey;
        private long chemicalCount;
        private final long chemicalPerOperation;
        private long operations;

        private ProcessingJob(ItemStack patternDefinition, AEItemKey itemKey, long itemCount, int itemPerOperation,
                MekanismKey chemicalKey, long chemicalCount, long chemicalPerOperation, long operations) {
            this.patternDefinition = patternDefinition.copyWithCount(1);
            this.itemKey = itemKey;
            this.itemCount = Math.max(0, itemCount);
            this.itemPerOperation = Math.max(1, itemPerOperation);
            this.chemicalKey = chemicalKey;
            this.chemicalCount = Math.max(0, chemicalCount);
            this.chemicalPerOperation = Math.max(1, chemicalPerOperation);
            this.operations = Math.max(0, operations);
        }

        private boolean matches(ItemStack pattern, AEItemKey otherItem, int otherItemPerOperation,
                MekanismKey otherChemical, long otherChemicalPerOperation) {
            return itemPerOperation == otherItemPerOperation
                    && chemicalPerOperation == otherChemicalPerOperation
                    && ItemStack.isSameItemSameComponents(patternDefinition, pattern)
                    && itemKey.equals(otherItem)
                    && chemicalKey.equals(otherChemical);
        }

        private boolean canGrow(long itemAddition, long chemicalAddition, long operationAddition) {
            return canAdd(itemCount, itemAddition) && canAdd(chemicalCount, chemicalAddition)
                    && canAdd(operations, operationAddition);
        }
    }

    private record ReturnedInputs(long items, long chemical) {
        private static final ReturnedInputs NONE = new ReturnedInputs(0, 0);
    }

}
