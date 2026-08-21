package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Pattern-isolated ledger for two AE resource inputs and one AE resource output. */
public abstract class AbstractDualKeyMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 1;

    private AEKey activeFirstKey;
    private long activeFirstCount;
    private long firstPerOperation = 1;
    private AEKey activeSecondKey;
    private long activeSecondCount;
    private long secondPerOperation = 1;
    private AEKey activeRecipeOutputKey;
    private long outputPerOperation = 1;
    private AEKey pendingOutputKey;
    private long pendingOutputCount;
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private int progress;
    private boolean processingFaulted;

    protected AbstractDualKeyMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType) {
        super(type, pos, state, visualRepresentation, machineType);
    }

    /** Returns amounts in the same input order supplied by the caller. */
    protected abstract RecipeOperation resolveRecipe(AEKey firstKey, long firstAvailable,
            AEKey secondKey, long secondAvailable);

    protected abstract boolean acceptsInputKey(AEKey key);

    protected abstract boolean acceptsOutputKey(AEKey key);

    @Override
    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        return itemDisplay(activeFirstKey, Math.min(activeFirstCount, firstPerOperation));
    }

    public ItemStack getSecondProcessingInputDisplay() {
        activateNextJobIfIdle();
        return itemDisplay(activeSecondKey, Math.min(activeSecondCount, secondPerOperation));
    }

    @Override
    public ItemStack getProcessingOutputDisplay() {
        activateNextJobIfIdle();
        AEKey key = pendingOutputKey != null ? pendingOutputKey : activeRecipeOutputKey;
        long amount = pendingOutputKey != null ? pendingOutputCount : outputPerOperation;
        return itemDisplay(key, amount);
    }

    @Override
    public ItemStack getBufferedInputDisplay() {
        return itemDisplay(activeFirstKey, 1);
    }

    public ItemStack getSecondBufferedInputDisplay() {
        return itemDisplay(activeSecondKey, 1);
    }

    @Override
    public ItemStack getBufferedOutputDisplay() {
        return itemDisplay(pendingOutputKey, 1);
    }

    @Override
    public long getBufferedOutputCount() {
        return Math.max(0, pendingOutputCount);
    }

    public MekanismKey getFirstProcessingChemicalKey() {
        activateNextJobIfIdle();
        return activeFirstKey instanceof MekanismKey chemical ? chemical : null;
    }

    public long getFirstProcessingChemicalAmount() {
        return getFirstProcessingChemicalKey() == null ? 0 : Math.min(activeFirstCount, firstPerOperation);
    }

    public MekanismKey getSecondProcessingChemicalKey() {
        activateNextJobIfIdle();
        return activeSecondKey instanceof MekanismKey chemical ? chemical : null;
    }

    public long getSecondProcessingChemicalAmount() {
        return getSecondProcessingChemicalKey() == null ? 0 : Math.min(activeSecondCount, secondPerOperation);
    }

    public MekanismKey getProcessingOutputChemicalKey() {
        activateNextJobIfIdle();
        AEKey key = pendingOutputKey != null ? pendingOutputKey : activeRecipeOutputKey;
        return key instanceof MekanismKey chemical ? chemical : null;
    }

    public long getProcessingOutputChemicalAmount() {
        return getProcessingOutputChemicalKey() == null ? 0
                : pendingOutputKey != null ? pendingOutputCount : outputPerOperation;
    }

    public MekanismKey getFirstBufferedChemicalKey() {
        return activeFirstKey instanceof MekanismKey chemical && activeFirstCount > 0 ? chemical : null;
    }

    public long getFirstBufferedChemicalCount() {
        return getFirstBufferedChemicalKey() == null ? 0 : Math.max(0, activeFirstCount);
    }

    public MekanismKey getSecondBufferedChemicalKey() {
        return activeSecondKey instanceof MekanismKey chemical && activeSecondCount > 0 ? chemical : null;
    }

    public long getSecondBufferedChemicalCount() {
        return getSecondBufferedChemicalKey() == null ? 0 : Math.max(0, activeSecondCount);
    }

    public MekanismKey getBufferedOutputChemicalKey() {
        return pendingOutputKey instanceof MekanismKey chemical && pendingOutputCount > 0 ? chemical : null;
    }

    public long getBufferedOutputChemicalCount() {
        return getBufferedOutputChemicalKey() == null ? 0 : Math.max(0, pendingOutputCount);
    }

    @Override
    public ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored();
                    case 1 -> progress;
                    case 2 -> clampToInt(getTotalQueuedOperations());
                    case 3 -> getProcessingTicks();
                    case 4 -> getMainNode().isOnline() ? 1 : 0;
                    case 5 -> energyStorage.getMaxEnergyStored();
                    case 6 -> networkEnabled ? 1 : 0;
                    case 7 -> speedUpgrades;
                    case 8 -> parallelUpgrades;
                    case 9 -> getUpgradeCount(ModItems.ENERGY_CARD.get());
                    case 10 -> energyStorage.getReceiveLimit();
                    case 11 -> clampToInt(getTotalQueuedOperations());
                    case 12 -> clampToInt(getBufferOperationLimit());
                    case 13 -> getParallelMultiplier();
                    case 14 -> clampToInt(pendingOutputCount);
                    case 15 -> processingFaulted ? 1 : 0;
                    case 16 -> getSpeedMultiplier();
                    case 17 -> chemicalRegistryId(getFirstProcessingChemicalKey());
                    case 18 -> clampToInt(getFirstProcessingChemicalAmount());
                    case 19 -> chemicalRegistryId(getSecondProcessingChemicalKey());
                    case 20 -> clampToInt(getSecondProcessingChemicalAmount());
                    case 21 -> chemicalRegistryId(getProcessingOutputChemicalKey());
                    case 22 -> clampToInt(getProcessingOutputChemicalAmount());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-owned state.
            }

            @Override
            public int getCount() {
                return 23;
            }
        };
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy()
                || inputs.length != 2 || !supportsPattern(details)) {
            return false;
        }
        DeliveredInput first = readDeliveredInput(inputs[0]);
        DeliveredInput second = readDeliveredInput(inputs[1]);
        if (first == null || second == null) {
            return false;
        }
        RecipeOperation recipe = resolveRecipe(first.key(), first.amount(), second.key(), second.amount());
        if (!validRecipe(recipe)
                || first.amount() % recipe.firstAmount() != 0
                || second.amount() % recipe.secondAmount() != 0) {
            return false;
        }
        long operations = first.amount() / recipe.firstAmount();
        if (operations <= 0 || operations != second.amount() / recipe.secondAmount()
                || !matchesDeclaredOutput(details, recipe, operations)) {
            return false;
        }
        long buffered = getTotalQueuedOperations();
        long limit = getBufferOperationLimit();
        if (buffered > limit || operations > limit - buffered) {
            return false;
        }

        enqueueJob(new ProcessingJob(details.getDefinition().toStack(),
                first.key(), first.amount(), recipe.firstAmount(),
                second.key(), second.amount(), recipe.secondAmount(),
                recipe.outputKey(), recipe.outputAmount(), operations));
        setChanged();
        return true;
    }

    private DeliveredInput readDeliveredInput(KeyCounter counter) {
        AEKey key = null;
        long amount = 0;
        for (Object2LongMap.Entry<AEKey> entry : counter) {
            long addition = entry.getLongValue();
            if (addition <= 0 || !acceptsInputKey(entry.getKey())
                    || key != null && !key.equals(entry.getKey()) || !canAdd(amount, addition)) {
                return null;
            }
            key = entry.getKey();
            amount += addition;
        }
        return key == null || amount <= 0 ? null : new DeliveredInput(key, amount);
    }

    @Override
    public boolean isBusy() {
        return !networkEnabled || processingFaulted || getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    @Override
    protected boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0
                || activeFirstKey != null && activeFirstCount > 0
                || activeSecondKey != null && activeSecondCount > 0
                || pendingOutputKey != null && pendingOutputCount > 0
                || !queuedJobs.isEmpty();
    }

    @Override
    public boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
    }

    @Override
    public boolean isProcessingFaulted() {
        return processingFaulted;
    }

    @Override
    public long getBufferedOperationCount() {
        return getTotalQueuedOperations();
    }

    @Override
    public long getCurrentOperationCount() {
        return Math.max(0, pendingOperations);
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
        if (processingFaulted || shouldPauseForRedstone() || pendingOperations <= 0
                || pendingOutputKey != null && pendingOutputCount > 0) {
            return;
        }
        if (activeFirstKey == null || activeFirstCount < firstPerOperation
                || activeSecondKey == null || activeSecondCount < secondPerOperation
                || activeRecipeOutputKey == null) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        RecipeOperation currentRecipe = resolveRecipe(
                activeFirstKey, firstPerOperation, activeSecondKey, secondPerOperation);
        if (!validRecipe(currentRecipe)
                || currentRecipe.firstAmount() != firstPerOperation
                || currentRecipe.secondAmount() != secondPerOperation
                || !currentRecipe.outputKey().equals(activeRecipeOutputKey)
                || currentRecipe.outputAmount() != outputPerOperation) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }

        int processingTicks = getProcessingTicks();
        progress = (int) Math.min(processingTicks, (long) Math.max(0, progress) + getSpeedMultiplier());
        updateVisualState();
        int energyPerOperation = getEnergyPerOperation();
        if (progress < processingTicks || energyStorage.getEnergyStored() < energyPerOperation) {
            return;
        }

        long operations = Math.min(getParallelBatchSize(), pendingOperations);
        operations = Math.min(operations, energyStorage.getEnergyStored() / energyPerOperation);
        operations = Math.min(operations, activeFirstCount / firstPerOperation);
        operations = Math.min(operations, activeSecondCount / secondPerOperation);
        if (operations <= 0
                || !canMultiply(operations, firstPerOperation)
                || !canMultiply(operations, secondPerOperation)
                || !canMultiply(operations, outputPerOperation)) {
            return;
        }
        long consumedFirst = operations * firstPerOperation;
        long consumedSecond = operations * secondPerOperation;
        long produced = operations * outputPerOperation;
        if (pendingOutputKey != null && (!pendingOutputKey.equals(activeRecipeOutputKey)
                || !canAdd(pendingOutputCount, produced))) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }

        energyStorage.consumeEnergy(getEnergyCostForOperations(operations));
        activeFirstCount -= consumedFirst;
        activeSecondCount -= consumedSecond;
        pendingOperations -= operations;
        if (pendingOutputKey == null) {
            pendingOutputKey = activeRecipeOutputKey;
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
        return !processingFaulted && !shouldPauseForRedstone() && pendingOperations > 0
                && activeFirstKey != null && activeFirstCount >= firstPerOperation
                && activeSecondKey != null && activeSecondCount >= secondPerOperation
                && activeRecipeOutputKey != null
                && (pendingOutputKey == null || pendingOutputCount <= 0)
                && energyStorage.getEnergyStored() >= getEnergyPerOperation();
    }

    @Override
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
        if (activeFirstKey != null && activeFirstCount > 0
                || activeSecondKey != null && activeSecondCount > 0) {
            ReturnedInputs returned = processingFaulted
                    ? new ReturnedInputs(returnKeyToNetwork(activeFirstKey, activeFirstCount),
                            returnKeyToNetwork(activeSecondKey, activeSecondCount))
                    : returnInputs(activeFirstKey, activeFirstCount, firstPerOperation,
                            activeSecondKey, activeSecondCount, secondPerOperation);
            activeFirstCount -= returned.first();
            activeSecondCount -= returned.second();
            pendingOperations = Math.min(activeFirstCount / Math.max(1, firstPerOperation),
                    activeSecondCount / Math.max(1, secondPerOperation));
            if (activeFirstCount <= 0) {
                activeFirstKey = null;
            }
            if (activeSecondCount <= 0) {
                activeSecondKey = null;
            }
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            ReturnedInputs returned = processingFaulted
                    ? new ReturnedInputs(returnKeyToNetwork(job.firstKey, job.firstCount),
                            returnKeyToNetwork(job.secondKey, job.secondCount))
                    : returnInputs(job.firstKey, job.firstCount, job.firstPerOperation,
                            job.secondKey, job.secondCount, job.secondPerOperation);
            job.firstCount -= returned.first();
            job.secondCount -= returned.second();
            job.operations = Math.min(job.firstCount / job.firstPerOperation,
                    job.secondCount / job.secondPerOperation);
            if (job.firstCount <= 0 && job.secondCount <= 0) {
                queuedJobs.remove(index);
            }
        }
        finishActiveJobIfDrained();
        setChanged();
        return activeFirstKey == null && activeSecondKey == null
                && pendingOutputKey == null && queuedJobs.isEmpty();
    }

    @Override
    protected final boolean isPatternForThisMachine(IPatternDetails details) {
        if (level == null || details.getInputs().length != 2 || details.getOutputs().size() != 1) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        if (output.amount() <= 0 || !acceptsOutputKey(output.what())) {
            return false;
        }
        IPatternDetails.IInput firstInput = details.getInputs()[0];
        IPatternDetails.IInput secondInput = details.getInputs()[1];
        for (GenericStack possibleFirst : firstInput.getPossibleInputs()) {
            if (possibleFirst.amount() <= 0 || !acceptsInputKey(possibleFirst.what())) {
                continue;
            }
            long firstAmount = declaredInputAmount(firstInput, possibleFirst);
            for (GenericStack possibleSecond : secondInput.getPossibleInputs()) {
                if (possibleSecond.amount() <= 0 || !acceptsInputKey(possibleSecond.what())) {
                    continue;
                }
                long secondAmount = declaredInputAmount(secondInput, possibleSecond);
                RecipeOperation recipe = resolveRecipe(
                        possibleFirst.what(), firstAmount, possibleSecond.what(), secondAmount);
                if (validRecipe(recipe) && recipe.outputKey().equals(output.what())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean supportsPattern(IPatternDetails details) {
        if (!isPatternForThisMachine(details)) {
            return false;
        }
        for (ItemStack storedPattern : patternSlots) {
            if (!storedPattern.isEmpty()) {
                IPatternDetails stored = PatternDetailsHelper.decodePattern(storedPattern, level);
                if (stored != null && stored.getDefinition().equals(details.getDefinition())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long declaredInputAmount(IPatternDetails.IInput input, GenericStack possibleInput) {
        long amount = possibleInput.amount();
        long multiplier = input.getMultiplier();
        return amount > 0 && multiplier > 0 && canMultiply(amount, multiplier)
                ? amount * multiplier : 0;
    }

    private boolean matchesDeclaredOutput(IPatternDetails details, RecipeOperation recipe, long operations) {
        if (details.getOutputs().size() != 1 || operations <= 0
                || !canMultiply(operations, recipe.outputAmount())) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        return recipe.outputKey().equals(output.what())
                && output.amount() == operations * recipe.outputAmount();
    }

    private void enqueueJob(ProcessingJob job) {
        if (pendingOperations <= 0 && activeFirstKey == null && activeSecondKey == null
                && pendingOutputKey == null && queuedJobs.isEmpty()) {
            activateJob(job);
            return;
        }
        if (job.matches(activePatternDefinition,
                activeFirstKey, firstPerOperation, activeSecondKey, secondPerOperation,
                activeRecipeOutputKey, outputPerOperation)
                && canAdd(activeFirstCount, job.firstCount)
                && canAdd(activeSecondCount, job.secondCount)
                && canAdd(pendingOperations, job.operations)) {
            activeFirstCount += job.firstCount;
            activeSecondCount += job.secondCount;
            pendingOperations += job.operations;
            return;
        }
        for (ProcessingJob queued : queuedJobs) {
            if (queued.matches(job.patternDefinition,
                    job.firstKey, job.firstPerOperation, job.secondKey, job.secondPerOperation,
                    job.outputKey, job.outputPerOperation)
                    && canAdd(queued.firstCount, job.firstCount)
                    && canAdd(queued.secondCount, job.secondCount)
                    && canAdd(queued.operations, job.operations)) {
                queued.firstCount += job.firstCount;
                queued.secondCount += job.secondCount;
                queued.operations += job.operations;
                return;
            }
        }
        queuedJobs.add(job);
    }

    private void activateNextJobIfIdle() {
        if (activeFirstKey != null || activeSecondKey != null || pendingOperations > 0
                || pendingOutputKey != null && pendingOutputCount > 0 || queuedJobs.isEmpty()) {
            return;
        }
        activateJob(queuedJobs.removeFirst());
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        activeFirstKey = job.firstKey;
        activeFirstCount = job.firstCount;
        firstPerOperation = job.firstPerOperation;
        activeSecondKey = job.secondKey;
        activeSecondCount = job.secondCount;
        secondPerOperation = job.secondPerOperation;
        activeRecipeOutputKey = job.outputKey;
        outputPerOperation = job.outputPerOperation;
        pendingOperations = job.operations;
        progress = 0;
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0 && activeFirstCount <= 0) {
            activeFirstKey = null;
        }
        if (pendingOperations <= 0 && activeSecondCount <= 0) {
            activeSecondKey = null;
        }
        if (pendingOperations <= 0 && activeFirstKey == null && activeSecondKey == null
                && pendingOutputKey == null) {
            activePatternDefinition = ItemStack.EMPTY;
            activeRecipeOutputKey = null;
            firstPerOperation = 1;
            secondPerOperation = 1;
            outputPerOperation = 1;
            progress = 0;
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
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
                pendingOutputKey, pendingOutputCount, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (inserted > 0) {
            pendingOutputCount -= Math.min(pendingOutputCount, inserted);
            if (pendingOutputCount <= 0) {
                pendingOutputKey = null;
                pendingOutputCount = 0;
            }
            setChanged();
        }
    }

    private ReturnedInputs returnInputs(AEKey firstKey, long firstCount, long firstUnit,
            AEKey secondKey, long secondCount, long secondUnit) {
        var grid = getMainNode().getGrid();
        if (grid == null || firstKey == null || secondKey == null || firstCount <= 0 || secondCount <= 0) {
            return ReturnedInputs.NONE;
        }
        var inventory = grid.getStorageService().getInventory();
        long possibleOperations = Math.min(firstCount / firstUnit, secondCount / secondUnit);
        if (possibleOperations <= 0
                || !canMultiply(possibleOperations, firstUnit)
                || !canMultiply(possibleOperations, secondUnit)) {
            return ReturnedInputs.NONE;
        }
        long firstAmount = possibleOperations * firstUnit;
        long secondAmount = possibleOperations * secondUnit;
        long acceptedFirst = inventory.insert(
                firstKey, firstAmount, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long acceptedSecond = inventory.insert(
                secondKey, secondAmount, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long acceptedOperations = Math.min(Math.max(0, acceptedFirst) / firstUnit,
                Math.max(0, acceptedSecond) / secondUnit);
        if (acceptedOperations <= 0) {
            return ReturnedInputs.NONE;
        }
        firstAmount = acceptedOperations * firstUnit;
        secondAmount = acceptedOperations * secondUnit;
        long insertedFirst = inventory.insert(
                firstKey, firstAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        long insertedSecond = inventory.insert(
                secondKey, secondAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (insertedFirst != firstAmount || insertedSecond != secondAmount
                || insertedFirst % firstUnit != 0 || insertedSecond % secondUnit != 0) {
            processingFaulted = true;
        }
        return new ReturnedInputs(Math.min(firstAmount, Math.max(0, insertedFirst)),
                Math.min(secondAmount, Math.max(0, insertedSecond)));
    }

    private long returnKeyToNetwork(AEKey key, long count) {
        var grid = getMainNode().getGrid();
        if (grid == null || key == null || count <= 0) {
            return 0;
        }
        return Math.min(count, Math.max(0, grid.getStorageService().getInventory().insert(
                key, count, Actionable.MODULATE, IActionSource.ofMachine(this))));
    }

    private long getTotalQueuedOperations() {
        long total = Math.max(0, pendingOperations);
        for (ProcessingJob job : queuedJobs) {
            if (!canAdd(total, job.operations)) {
                return Long.MAX_VALUE;
            }
            total += Math.max(0, job.operations);
        }
        return total;
    }

    private void markProcessingFault() {
        processingFaulted = true;
        progress = 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("DualKeyTaskVersion", TASK_DATA_VERSION);
        putKey(tag, "DualActiveFirstKey", activeFirstKey, activeFirstCount, registries);
        if (activeFirstKey != null && activeFirstCount > 0) {
            tag.putLong("DualActiveFirstCount", activeFirstCount);
        }
        putKey(tag, "DualActiveSecondKey", activeSecondKey, activeSecondCount, registries);
        if (activeSecondKey != null && activeSecondCount > 0) {
            tag.putLong("DualActiveSecondCount", activeSecondCount);
        }
        if (activeRecipeOutputKey != null) {
            tag.put("DualActiveRecipeOutputKey", activeRecipeOutputKey.toTagGeneric(registries));
        }
        putKey(tag, "DualPendingOutputKey", pendingOutputKey, pendingOutputCount, registries);
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            tag.putLong("DualPendingOutputCount", pendingOutputCount);
        }
        if (!activePatternDefinition.isEmpty()) {
            tag.put("DualActivePattern", activePatternDefinition.save(registries));
        }
        var jobs = new net.minecraft.nbt.ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag saved = new CompoundTag();
            saved.put("Pattern", job.patternDefinition.save(registries));
            saved.put("FirstKey", job.firstKey.toTagGeneric(registries));
            saved.putLong("FirstCount", job.firstCount);
            saved.putLong("FirstPerOperation", job.firstPerOperation);
            saved.put("SecondKey", job.secondKey.toTagGeneric(registries));
            saved.putLong("SecondCount", job.secondCount);
            saved.putLong("SecondPerOperation", job.secondPerOperation);
            saved.put("OutputKey", job.outputKey.toTagGeneric(registries));
            saved.putLong("OutputPerOperation", job.outputPerOperation);
            saved.putLong("Operations", job.operations);
            jobs.add(saved);
        }
        tag.put("DualKeyQueue", jobs);
        tag.putLong("DualPendingOperations", pendingOperations);
        tag.putLong("DualFirstPerOperation", firstPerOperation);
        tag.putLong("DualSecondPerOperation", secondPerOperation);
        tag.putLong("DualOutputPerOperation", outputPerOperation);
        tag.putInt("DualProgress", progress);
        tag.putBoolean("DualProcessingFaulted", processingFaulted);
    }

    private static void putKey(CompoundTag tag, String name, AEKey key, long count,
            HolderLookup.Provider registries) {
        if (key != null && count > 0) {
            tag.put(name, key.toTagGeneric(registries));
        }
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        activeFirstKey = readAcceptedKey(tag, "DualActiveFirstKey", registries, true);
        activeFirstCount = activeFirstKey == null ? 0 : Math.max(0, tag.getLong("DualActiveFirstCount"));
        activeSecondKey = readAcceptedKey(tag, "DualActiveSecondKey", registries, true);
        activeSecondCount = activeSecondKey == null ? 0 : Math.max(0, tag.getLong("DualActiveSecondCount"));
        activeRecipeOutputKey = readAcceptedKey(tag, "DualActiveRecipeOutputKey", registries, false);
        pendingOutputKey = readAcceptedKey(tag, "DualPendingOutputKey", registries, false);
        pendingOutputCount = pendingOutputKey == null ? 0 : Math.max(0, tag.getLong("DualPendingOutputCount"));
        activePatternDefinition = tag.contains("DualActivePattern")
                ? ItemStack.parseOptional(registries, tag.getCompound("DualActivePattern")) : ItemStack.EMPTY;
        pendingOperations = Math.max(0, tag.getLong("DualPendingOperations"));
        firstPerOperation = Math.max(1, tag.getLong("DualFirstPerOperation"));
        secondPerOperation = Math.max(1, tag.getLong("DualSecondPerOperation"));
        outputPerOperation = Math.max(1, tag.getLong("DualOutputPerOperation"));
        progress = Math.max(0, tag.getInt("DualProgress"));
        processingFaulted = tag.getBoolean("DualProcessingFaulted");

        queuedJobs.clear();
        if (tag.getInt("DualKeyTaskVersion") == TASK_DATA_VERSION
                && tag.contains("DualKeyQueue", net.minecraft.nbt.Tag.TAG_LIST)) {
            var jobs = tag.getList("DualKeyQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < jobs.size(); index++) {
                CompoundTag saved = jobs.getCompound(index);
                ItemStack pattern = ItemStack.parseOptional(registries, saved.getCompound("Pattern"));
                AEKey first = readAcceptedKey(saved, "FirstKey", registries, true);
                AEKey second = readAcceptedKey(saved, "SecondKey", registries, true);
                AEKey output = readAcceptedKey(saved, "OutputKey", registries, false);
                long firstCount = Math.max(0, saved.getLong("FirstCount"));
                long firstUnit = Math.max(1, saved.getLong("FirstPerOperation"));
                long secondCount = Math.max(0, saved.getLong("SecondCount"));
                long secondUnit = Math.max(1, saved.getLong("SecondPerOperation"));
                long outputUnit = Math.max(1, saved.getLong("OutputPerOperation"));
                long operations = Math.max(0, saved.getLong("Operations"));
                if (!pattern.isEmpty() && first != null && second != null && output != null
                        && firstCount > 0 && secondCount > 0) {
                    queuedJobs.add(new ProcessingJob(pattern,
                            first, firstCount, firstUnit, second, secondCount, secondUnit,
                            output, outputUnit, operations));
                }
            }
        }
        validateLoadedLedger();
    }

    private AEKey readAcceptedKey(CompoundTag tag, String name, HolderLookup.Provider registries, boolean input) {
        if (!tag.contains(name, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        AEKey key = AEKey.fromTagGeneric(registries, tag.getCompound(name));
        return key != null && (input ? acceptsInputKey(key) : acceptsOutputKey(key)) ? key : null;
    }

    private void validateLoadedLedger() {
        if (activeFirstKey == null || activeFirstCount <= 0
                || activeSecondKey == null || activeSecondCount <= 0) {
            activeFirstKey = null;
            activeFirstCount = 0;
            activeSecondKey = null;
            activeSecondCount = 0;
            pendingOperations = 0;
            activeRecipeOutputKey = null;
        } else if (activePatternDefinition.isEmpty() || activeRecipeOutputKey == null
                || activeFirstCount % firstPerOperation != 0
                || activeSecondCount % secondPerOperation != 0
                || pendingOperations != activeFirstCount / firstPerOperation
                || pendingOperations != activeSecondCount / secondPerOperation) {
            processingFaulted = true;
        }
        for (ProcessingJob job : queuedJobs) {
            if (job.firstCount % job.firstPerOperation != 0
                    || job.secondCount % job.secondPerOperation != 0
                    || job.operations != job.firstCount / job.firstPerOperation
                    || job.operations != job.secondCount / job.secondPerOperation) {
                processingFaulted = true;
            }
        }
    }

    private boolean validRecipe(RecipeOperation recipe) {
        return recipe != null && recipe.firstAmount() > 0 && recipe.secondAmount() > 0
                && recipe.outputKey() != null && acceptsOutputKey(recipe.outputKey())
                && recipe.outputAmount() > 0;
    }

    private static boolean canAdd(long first, long second) {
        return first >= 0 && second >= 0 && first <= Long.MAX_VALUE - second;
    }

    private static boolean canMultiply(long first, long second) {
        return first >= 0 && second > 0 && first <= Long.MAX_VALUE / second;
    }

    private static int clampToInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
    }

    private static int chemicalRegistryId(MekanismKey key) {
        return key == null ? -1 : MekanismAPI.CHEMICAL_REGISTRY.getId(key.getStack().getChemical());
    }

    private static ItemStack itemDisplay(AEKey key, long count) {
        if (!(key instanceof AEItemKey itemKey) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = itemKey.toStack(1);
        display.setCount((int) Math.max(1, Math.min(count, display.getMaxStackSize())));
        return display;
    }

    protected record RecipeOperation(long firstAmount, long secondAmount,
                                     AEKey outputKey, long outputAmount) {
    }

    private record DeliveredInput(AEKey key, long amount) {
    }

    private record ReturnedInputs(long first, long second) {
        private static final ReturnedInputs NONE = new ReturnedInputs(0, 0);
    }

    private static final class ProcessingJob {
        private final ItemStack patternDefinition;
        private final AEKey firstKey;
        private long firstCount;
        private final long firstPerOperation;
        private final AEKey secondKey;
        private long secondCount;
        private final long secondPerOperation;
        private final AEKey outputKey;
        private final long outputPerOperation;
        private long operations;

        private ProcessingJob(ItemStack patternDefinition,
                AEKey firstKey, long firstCount, long firstPerOperation,
                AEKey secondKey, long secondCount, long secondPerOperation,
                AEKey outputKey, long outputPerOperation, long operations) {
            this.patternDefinition = patternDefinition.copyWithCount(1);
            this.firstKey = firstKey;
            this.firstCount = Math.max(0, firstCount);
            this.firstPerOperation = Math.max(1, firstPerOperation);
            this.secondKey = secondKey;
            this.secondCount = Math.max(0, secondCount);
            this.secondPerOperation = Math.max(1, secondPerOperation);
            this.outputKey = outputKey;
            this.outputPerOperation = Math.max(1, outputPerOperation);
            this.operations = Math.max(0, operations);
        }

        private boolean matches(ItemStack pattern,
                AEKey first, long firstUnit, AEKey second, long secondUnit,
                AEKey output, long outputUnit) {
            return first != null && second != null && output != null
                    && firstPerOperation == firstUnit && secondPerOperation == secondUnit
                    && outputPerOperation == outputUnit
                    && ItemStack.isSameItemSameComponents(patternDefinition, pattern)
                    && firstKey.equals(first) && secondKey.equals(second) && outputKey.equals(output);
        }
    }
}
