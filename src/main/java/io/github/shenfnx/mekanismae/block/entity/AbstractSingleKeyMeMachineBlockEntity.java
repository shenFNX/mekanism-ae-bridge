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

/**
 * Pattern-isolated ledger for one AE resource input and one AE resource output.
 * Both sides may be items or Applied Mekanistics chemicals.
 */
public abstract class AbstractSingleKeyMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 1;

    private AEKey activeInputKey;
    private long activeInputCount;
    private AEKey activeRecipeOutputKey;
    private long outputPerOperation = 1;
    private AEKey pendingOutputKey;
    private long pendingOutputCount;
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private long inputPerOperation = 1;
    private int progress;
    private boolean processingFaulted;

    protected AbstractSingleKeyMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType) {
        super(type, pos, state, visualRepresentation, machineType);
    }

    protected abstract RecipeOperation resolveRecipe(AEKey inputKey, long availableAmount);

    protected abstract boolean acceptsInputKey(AEKey key);

    protected abstract boolean acceptsOutputKey(AEKey key);

    @Override
    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        return itemDisplay(activeInputKey, Math.min(activeInputCount, inputPerOperation));
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
        return itemDisplay(activeInputKey, 1);
    }

    @Override
    public ItemStack getBufferedOutputDisplay() {
        return itemDisplay(pendingOutputKey, 1);
    }

    @Override
    public long getBufferedOutputCount() {
        return Math.max(0, pendingOutputCount);
    }

    public MekanismKey getProcessingInputChemicalKey() {
        activateNextJobIfIdle();
        return activeInputKey instanceof MekanismKey chemical ? chemical : null;
    }

    public long getProcessingInputChemicalAmount() {
        return getProcessingInputChemicalKey() == null ? 0 : Math.min(activeInputCount, inputPerOperation);
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

    public MekanismKey getBufferedInputChemicalKey() {
        return activeInputKey instanceof MekanismKey chemical && activeInputCount > 0 ? chemical : null;
    }

    public long getBufferedInputChemicalCount() {
        return getBufferedInputChemicalKey() == null ? 0 : Math.max(0, activeInputCount);
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
                    case 17 -> chemicalRegistryId(getProcessingInputChemicalKey());
                    case 18 -> clampToInt(getProcessingInputChemicalAmount());
                    case 19 -> chemicalRegistryId(getProcessingOutputChemicalKey());
                    case 20 -> clampToInt(getProcessingOutputChemicalAmount());
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-owned state.
            }

            @Override
            public int getCount() {
                return 21;
            }
        };
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy() || !supportsPattern(details)) {
            return false;
        }
        AEKey deliveredKey = null;
        long deliveredAmount = 0;
        for (KeyCounter counter : inputs) {
            for (Object2LongMap.Entry<AEKey> entry : counter) {
                long amount = entry.getLongValue();
                AEKey key = entry.getKey();
                if (amount <= 0 || !acceptsInputKey(key)
                        || deliveredKey != null && !deliveredKey.equals(key)
                        || !canAdd(deliveredAmount, amount)) {
                    return false;
                }
                deliveredKey = key;
                deliveredAmount += amount;
            }
        }
        if (deliveredKey == null || deliveredAmount <= 0) {
            return false;
        }

        RecipeOperation recipe = resolveRecipe(deliveredKey, deliveredAmount);
        if (!validRecipe(recipe) || deliveredAmount % recipe.inputAmount() != 0) {
            return false;
        }
        long operations = deliveredAmount / recipe.inputAmount();
        if (!matchesDeclaredOutput(details, recipe, operations)) {
            return false;
        }
        long buffered = getTotalQueuedOperations();
        long limit = getBufferOperationLimit();
        if (buffered > limit || operations > limit - buffered) {
            return false;
        }

        enqueueJob(new ProcessingJob(details.getDefinition().toStack(), deliveredKey, deliveredAmount,
                recipe.inputAmount(), recipe.outputKey(), recipe.outputAmount(), operations));
        setChanged();
        return true;
    }

    @Override
    public boolean isBusy() {
        return !networkEnabled || processingFaulted || getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    @Override
    protected boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0 || activeInputKey != null && activeInputCount > 0
                || pendingOutputKey != null && pendingOutputCount > 0 || !queuedJobs.isEmpty();
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
        if (activeInputKey == null || activeInputCount < inputPerOperation || activeRecipeOutputKey == null) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }
        RecipeOperation currentRecipe = resolveRecipe(activeInputKey, inputPerOperation);
        if (!validRecipe(currentRecipe) || currentRecipe.inputAmount() != inputPerOperation
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
        operations = Math.min(operations, activeInputCount / inputPerOperation);
        if (operations <= 0 || !canMultiply(operations, outputPerOperation)) {
            return;
        }
        long consumed = operations * inputPerOperation;
        long produced = operations * outputPerOperation;
        if (pendingOutputKey != null && (!pendingOutputKey.equals(activeRecipeOutputKey)
                || !canAdd(pendingOutputCount, produced))) {
            markProcessingFault();
            updateVisualState();
            setChanged();
            return;
        }

        energyStorage.consumeEnergy(getEnergyCostForOperations(operations));
        activeInputCount -= consumed;
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
                && activeInputKey != null && activeInputCount >= inputPerOperation
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
        if (activeInputKey != null && activeInputCount > 0) {
            long returned = returnInput(activeInputKey, activeInputCount,
                    processingFaulted ? 1 : inputPerOperation);
            activeInputCount -= returned;
            pendingOperations = activeInputCount / Math.max(1, inputPerOperation);
            if (activeInputCount <= 0) {
                activeInputKey = null;
            }
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            long returned = returnInput(job.inputKey, job.inputCount,
                    processingFaulted ? 1 : job.inputPerOperation);
            job.inputCount -= returned;
            job.operations = job.inputCount / Math.max(1, job.inputPerOperation);
            if (job.inputCount <= 0) {
                queuedJobs.remove(index);
            }
        }
        finishActiveJobIfDrained();
        setChanged();
        return activeInputKey == null && pendingOutputKey == null && queuedJobs.isEmpty();
    }

    @Override
    protected final boolean isPatternForThisMachine(IPatternDetails details) {
        if (level == null || details.getInputs().length != 1 || details.getOutputs().size() != 1) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        if (output.amount() <= 0 || !acceptsOutputKey(output.what())) {
            return false;
        }
        for (GenericStack possibleInput : details.getInputs()[0].getPossibleInputs()) {
            if (possibleInput.amount() <= 0 || !acceptsInputKey(possibleInput.what())) {
                continue;
            }
            long amount = declaredInputAmount(details, possibleInput.what());
            RecipeOperation recipe = resolveRecipe(possibleInput.what(), amount);
            if (validRecipe(recipe) && amount % recipe.inputAmount() == 0
                    && matchesDeclaredOutput(details, recipe, amount / recipe.inputAmount())) {
                return true;
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

    private static long declaredInputAmount(IPatternDetails details, AEKey key) {
        for (var input : details.getInputs()) {
            for (GenericStack possibleInput : input.getPossibleInputs()) {
                if (key.equals(possibleInput.what())) {
                    long amount = possibleInput.amount();
                    long multiplier = input.getMultiplier();
                    return amount > 0 && multiplier > 0 && canMultiply(amount, multiplier)
                            ? amount * multiplier : 0;
                }
            }
        }
        return 0;
    }

    private boolean matchesDeclaredOutput(IPatternDetails details, RecipeOperation recipe, long operations) {
        if (details.getOutputs().size() != 1 || operations <= 0 || !canMultiply(operations, recipe.outputAmount())) {
            return false;
        }
        GenericStack output = details.getOutputs().getFirst();
        return recipe.outputKey().equals(output.what())
                && output.amount() == operations * recipe.outputAmount();
    }

    private void enqueueJob(ProcessingJob job) {
        if (pendingOperations <= 0 && activeInputKey == null && pendingOutputKey == null && queuedJobs.isEmpty()) {
            activateJob(job);
            return;
        }
        if (job.matches(activePatternDefinition, activeInputKey, inputPerOperation,
                activeRecipeOutputKey, outputPerOperation)
                && canAdd(activeInputCount, job.inputCount) && canAdd(pendingOperations, job.operations)) {
            activeInputCount += job.inputCount;
            pendingOperations += job.operations;
            return;
        }
        for (ProcessingJob queued : queuedJobs) {
            if (queued.matches(job.patternDefinition, job.inputKey, job.inputPerOperation,
                    job.outputKey, job.outputPerOperation)
                    && canAdd(queued.inputCount, job.inputCount) && canAdd(queued.operations, job.operations)) {
                queued.inputCount += job.inputCount;
                queued.operations += job.operations;
                return;
            }
        }
        queuedJobs.add(job);
    }

    private void activateNextJobIfIdle() {
        if (activeInputKey != null || pendingOperations > 0 || pendingOutputKey != null && pendingOutputCount > 0
                || queuedJobs.isEmpty()) {
            return;
        }
        activateJob(queuedJobs.removeFirst());
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        activeInputKey = job.inputKey;
        activeInputCount = job.inputCount;
        inputPerOperation = job.inputPerOperation;
        activeRecipeOutputKey = job.outputKey;
        outputPerOperation = job.outputPerOperation;
        pendingOperations = job.operations;
        progress = 0;
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0 && activeInputCount <= 0) {
            activeInputKey = null;
        }
        if (pendingOperations <= 0 && activeInputKey == null && pendingOutputKey == null) {
            activePatternDefinition = ItemStack.EMPTY;
            activeRecipeOutputKey = null;
            inputPerOperation = 1;
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

    private long returnInput(AEKey key, long count, long unit) {
        var grid = getMainNode().getGrid();
        if (grid == null || key == null || count <= 0) {
            return 0;
        }
        var inventory = grid.getStorageService().getInventory();
        long accepted = inventory.insert(key, count, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long whole = Math.min(count, Math.max(0, accepted));
        whole -= whole % Math.max(1, unit);
        if (whole <= 0) {
            return 0;
        }
        long inserted = inventory.insert(key, whole, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (inserted % Math.max(1, unit) != 0) {
            processingFaulted = true;
        }
        return Math.min(whole, Math.max(0, inserted));
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
        tag.putInt("SingleKeyTaskVersion", TASK_DATA_VERSION);
        if (activeInputKey != null && activeInputCount > 0) {
            tag.put("ActiveInputKey", activeInputKey.toTagGeneric(registries));
            tag.putLong("ActiveInputCount", activeInputCount);
        }
        if (activeRecipeOutputKey != null) {
            tag.put("ActiveRecipeOutputKey", activeRecipeOutputKey.toTagGeneric(registries));
        }
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            tag.put("PendingOutputKey", pendingOutputKey.toTagGeneric(registries));
            tag.putLong("PendingOutputCount", pendingOutputCount);
        }
        if (!activePatternDefinition.isEmpty()) {
            tag.put("ActivePattern", activePatternDefinition.save(registries));
        }
        var jobs = new net.minecraft.nbt.ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag saved = new CompoundTag();
            saved.put("Pattern", job.patternDefinition.save(registries));
            saved.put("InputKey", job.inputKey.toTagGeneric(registries));
            saved.putLong("InputCount", job.inputCount);
            saved.putLong("InputPerOperation", job.inputPerOperation);
            saved.put("OutputKey", job.outputKey.toTagGeneric(registries));
            saved.putLong("OutputPerOperation", job.outputPerOperation);
            saved.putLong("Operations", job.operations);
            jobs.add(saved);
        }
        tag.put("SingleKeyQueue", jobs);
        tag.putLong("PendingOperations", pendingOperations);
        tag.putLong("InputPerOperation", inputPerOperation);
        tag.putLong("OutputPerOperation", outputPerOperation);
        tag.putInt("Progress", progress);
        tag.putBoolean("ProcessingFaulted", processingFaulted);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        activeInputKey = readAcceptedKey(tag, "ActiveInputKey", registries, true);
        activeInputCount = activeInputKey == null ? 0 : Math.max(0, tag.getLong("ActiveInputCount"));
        activeRecipeOutputKey = readAcceptedKey(tag, "ActiveRecipeOutputKey", registries, false);
        pendingOutputKey = readAcceptedKey(tag, "PendingOutputKey", registries, false);
        pendingOutputCount = pendingOutputKey == null ? 0 : Math.max(0, tag.getLong("PendingOutputCount"));
        activePatternDefinition = tag.contains("ActivePattern")
                ? ItemStack.parseOptional(registries, tag.getCompound("ActivePattern")) : ItemStack.EMPTY;
        pendingOperations = Math.max(0, tag.getLong("PendingOperations"));
        inputPerOperation = Math.max(1, tag.getLong("InputPerOperation"));
        outputPerOperation = Math.max(1, tag.getLong("OutputPerOperation"));
        progress = Math.max(0, tag.getInt("Progress"));
        processingFaulted = tag.getBoolean("ProcessingFaulted");

        queuedJobs.clear();
        if (tag.getInt("SingleKeyTaskVersion") == TASK_DATA_VERSION
                && tag.contains("SingleKeyQueue", net.minecraft.nbt.Tag.TAG_LIST)) {
            var jobs = tag.getList("SingleKeyQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < jobs.size(); index++) {
                CompoundTag saved = jobs.getCompound(index);
                ItemStack pattern = ItemStack.parseOptional(registries, saved.getCompound("Pattern"));
                AEKey input = readAcceptedKey(saved, "InputKey", registries, true);
                AEKey output = readAcceptedKey(saved, "OutputKey", registries, false);
                long inputCount = Math.max(0, saved.getLong("InputCount"));
                long inputUnit = Math.max(1, saved.getLong("InputPerOperation"));
                long outputUnit = Math.max(1, saved.getLong("OutputPerOperation"));
                long operations = Math.max(0, saved.getLong("Operations"));
                if (!pattern.isEmpty() && input != null && output != null && inputCount > 0) {
                    queuedJobs.add(new ProcessingJob(pattern, input, inputCount, inputUnit,
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
        if (activeInputKey == null || activeInputCount <= 0) {
            activeInputKey = null;
            activeInputCount = 0;
            pendingOperations = 0;
            activeRecipeOutputKey = null;
        } else if (activePatternDefinition.isEmpty() || activeRecipeOutputKey == null
                || activeInputCount % inputPerOperation != 0
                || pendingOperations != activeInputCount / inputPerOperation) {
            processingFaulted = true;
        }
        for (ProcessingJob job : queuedJobs) {
            if (job.inputCount % job.inputPerOperation != 0
                    || job.operations != job.inputCount / job.inputPerOperation) {
                processingFaulted = true;
            }
        }
    }

    private static boolean validRecipe(RecipeOperation recipe) {
        return recipe != null && recipe.inputAmount() > 0 && recipe.outputKey() != null
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

    protected record RecipeOperation(long inputAmount, AEKey outputKey, long outputAmount) {
    }

    private static final class ProcessingJob {
        private final ItemStack patternDefinition;
        private final AEKey inputKey;
        private long inputCount;
        private final long inputPerOperation;
        private final AEKey outputKey;
        private final long outputPerOperation;
        private long operations;

        private ProcessingJob(ItemStack patternDefinition, AEKey inputKey, long inputCount, long inputPerOperation,
                AEKey outputKey, long outputPerOperation, long operations) {
            this.patternDefinition = patternDefinition.copyWithCount(1);
            this.inputKey = inputKey;
            this.inputCount = Math.max(0, inputCount);
            this.inputPerOperation = Math.max(1, inputPerOperation);
            this.outputKey = outputKey;
            this.outputPerOperation = Math.max(1, outputPerOperation);
            this.operations = Math.max(0, operations);
        }

        private boolean matches(ItemStack pattern, AEKey input, long inputUnit, AEKey output, long outputUnit) {
            return input != null && output != null && inputPerOperation == inputUnit
                    && outputPerOperation == outputUnit
                    && ItemStack.isSameItemSameComponents(patternDefinition, pattern)
                    && inputKey.equals(input) && outputKey.equals(output);
        }
    }
}
