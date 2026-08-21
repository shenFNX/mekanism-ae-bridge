package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Pattern-isolated GTNH-style ledger for one to three inputs and up to two outputs. */
public abstract class AbstractMultiKeyMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    public static final int MAX_INPUT_SLOTS = 3;
    public static final int MAX_OUTPUT_SLOTS = 2;
    public static final int RESOURCE_DISPLAY_SLOTS = MAX_INPUT_SLOTS + MAX_OUTPUT_SLOTS;
    public static final int CONTAINER_DATA_COUNT = 17 + RESOURCE_DISPLAY_SLOTS * 3;
    public static final int RESOURCE_NONE = 0;
    public static final int RESOURCE_ITEM = 1;
    public static final int RESOURCE_CHEMICAL = 2;
    public static final int RESOURCE_FLUID = 3;

    private static final int TASK_DATA_VERSION = 1;

    private final int inputSlotCount;
    private final int outputSlotCount;
    private final AEKey[] activeInputKeys = new AEKey[MAX_INPUT_SLOTS];
    private final long[] activeInputCounts = new long[MAX_INPUT_SLOTS];
    private final long[] inputPerOperation = filledLongArray(MAX_INPUT_SLOTS, 1);
    private final AEKey[] activeOutputKeys = new AEKey[MAX_OUTPUT_SLOTS];
    private final long[] outputPerOperation = filledLongArray(MAX_OUTPUT_SLOTS, 1);
    private final long[] pendingOutputCounts = new long[MAX_OUTPUT_SLOTS];
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private int progress;
    private boolean processingFaulted;

    protected AbstractMultiKeyMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType, int inputSlotCount, int outputSlotCount) {
        super(type, pos, state, visualRepresentation, machineType);
        if (inputSlotCount < 1 || inputSlotCount > MAX_INPUT_SLOTS
                || outputSlotCount < 1 || outputSlotCount > MAX_OUTPUT_SLOTS) {
            throw new IllegalArgumentException("Unsupported multi-key machine shape");
        }
        this.inputSlotCount = inputSlotCount;
        this.outputSlotCount = outputSlotCount;
    }

    protected abstract RecipeOperation resolveRecipe(List<ResourceAmount> inputs);

    protected abstract boolean acceptsInputKey(AEKey key);

    protected abstract boolean acceptsOutputKey(AEKey key);

    public final int getInputSlotCount() {
        return inputSlotCount;
    }

    public final int getOutputSlotCount() {
        return outputSlotCount;
    }

    public final ItemStack getProcessingResourceDisplay(int slot) {
        AEKey key = getProcessingResourceKey(slot);
        return itemDisplay(key, getProcessingResourceAmount(slot));
    }

    public final AEKey getProcessingResourceKey(int slot) {
        activateNextJobIfIdle();
        if (slot >= 0 && slot < MAX_INPUT_SLOTS) {
            return slot < inputSlotCount ? activeInputKeys[slot] : null;
        }
        int output = slot - MAX_INPUT_SLOTS;
        return output >= 0 && output < outputSlotCount ? activeOutputKeys[output] : null;
    }

    public final long getProcessingResourceAmount(int slot) {
        AEKey key = getProcessingResourceKey(slot);
        if (key == null) {
            return 0;
        }
        if (slot < MAX_INPUT_SLOTS) {
            return Math.min(activeInputCounts[slot], inputPerOperation[slot]);
        }
        int output = slot - MAX_INPUT_SLOTS;
        return pendingOutputCounts[output] > 0 ? pendingOutputCounts[output] : outputPerOperation[output];
    }

    public final AEKey getBufferedResourceKey(int slot) {
        if (slot >= 0 && slot < MAX_INPUT_SLOTS) {
            return slot < inputSlotCount && activeInputCounts[slot] > 0 ? activeInputKeys[slot] : null;
        }
        int output = slot - MAX_INPUT_SLOTS;
        return output >= 0 && output < outputSlotCount && pendingOutputCounts[output] > 0
                ? activeOutputKeys[output] : null;
    }

    public final long getBufferedResourceCount(int slot) {
        AEKey key = getBufferedResourceKey(slot);
        if (key == null) {
            return 0;
        }
        return slot < MAX_INPUT_SLOTS ? activeInputCounts[slot] : pendingOutputCounts[slot - MAX_INPUT_SLOTS];
    }

    @Override
    public final ItemStack getProcessingInputDisplay() {
        return getProcessingResourceDisplay(0);
    }

    @Override
    public final ItemStack getProcessingSecondaryInputDisplay() {
        return getProcessingResourceDisplay(1);
    }

    @Override
    public final ItemStack getProcessingOutputDisplay() {
        return getProcessingResourceDisplay(MAX_INPUT_SLOTS);
    }

    @Override
    public final ItemStack getProcessingSecondaryOutputDisplay() {
        return getProcessingResourceDisplay(MAX_INPUT_SLOTS + 1);
    }

    @Override
    public final ItemStack getBufferedInputDisplay() {
        return itemDisplay(getBufferedResourceKey(0), 1);
    }

    @Override
    public final ItemStack getBufferedOutputDisplay() {
        return itemDisplay(getBufferedResourceKey(MAX_INPUT_SLOTS), 1);
    }

    @Override
    public final long getBufferedOutputCount() {
        long total = 0;
        for (int index = 0; index < outputSlotCount; index++) {
            total = saturatingAdd(total, pendingOutputCounts[index]);
        }
        return total;
    }

    @Override
    public final ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index >= 17 && index < CONTAINER_DATA_COUNT) {
                    int resourceSlot = (index - 17) / 3;
                    int field = (index - 17) % 3;
                    AEKey key = getProcessingResourceKey(resourceSlot);
                    return switch (field) {
                        case 0 -> resourceType(key);
                        case 1 -> resourceRegistryId(key);
                        case 2 -> clampToInt(getProcessingResourceAmount(resourceSlot));
                        default -> 0;
                    };
                }
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored();
                    case 1 -> progress;
                    case 2, 11 -> clampToInt(getTotalQueuedOperations());
                    case 3 -> getProcessingTicks();
                    case 4 -> getMainNode().isOnline() ? 1 : 0;
                    case 5 -> energyStorage.getMaxEnergyStored();
                    case 6 -> networkEnabled ? 1 : 0;
                    case 7 -> speedUpgrades;
                    case 8 -> parallelUpgrades;
                    case 9 -> getUpgradeCount(ModItems.ENERGY_CARD.get());
                    case 10 -> energyStorage.getReceiveLimit();
                    case 12 -> clampToInt(getBufferOperationLimit());
                    case 13 -> getParallelMultiplier();
                    case 14 -> clampToInt(getBufferedOutputCount());
                    case 15 -> processingFaulted ? 1 : 0;
                    case 16 -> getSpeedMultiplier();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server-owned state.
            }

            @Override
            public int getCount() {
                return CONTAINER_DATA_COUNT;
            }
        };
    }

    @Override
    public final boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy()
                || inputs.length != inputSlotCount || !supportsPattern(details)) {
            return false;
        }
        List<ResourceAmount> delivered = new ArrayList<>(inputSlotCount);
        for (KeyCounter input : inputs) {
            ResourceAmount resource = readDeliveredInput(input);
            if (resource == null) {
                return false;
            }
            delivered.add(resource);
        }
        RecipeOperation recipe = normalizeRecipe(resolveRecipe(delivered));
        long operations = operationCount(delivered, recipe);
        if (!validRecipe(recipe) || operations <= 0 || !matchesDeclaredOutputs(details, recipe, operations)) {
            return false;
        }
        long buffered = getTotalQueuedOperations();
        long limit = getBufferOperationLimit();
        if (buffered > limit || operations > limit - buffered) {
            return false;
        }
        enqueueJob(new ProcessingJob(details.getDefinition().toStack(), delivered, recipe, operations));
        setChanged();
        return true;
    }

    private ResourceAmount readDeliveredInput(KeyCounter counter) {
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
        return key == null || amount <= 0 ? null : new ResourceAmount(key, amount);
    }

    @Override
    public final boolean isBusy() {
        return !networkEnabled || processingFaulted || getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    @Override
    protected final boolean hasProcessingWork() {
        if (pendingOperations > 0 || !queuedJobs.isEmpty()) {
            return true;
        }
        for (int index = 0; index < inputSlotCount; index++) {
            if (activeInputKeys[index] != null && activeInputCounts[index] > 0) {
                return true;
            }
        }
        return hasPendingOutputs();
    }

    @Override
    public final boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
    }

    @Override
    public final boolean isProcessingFaulted() {
        return processingFaulted;
    }

    @Override
    public final long getBufferedOperationCount() {
        return getTotalQueuedOperations();
    }

    @Override
    public final long getCurrentOperationCount() {
        return Math.max(0, pendingOperations);
    }

    @Override
    public final void tickServer() {
        if (level == null || level.isClientSide()) {
            return;
        }
        chargeFromNetwork();
        flushOutputs();
        finishActiveJobIfDrained();
        activateNextJobIfIdle();
        updateVisualState();
        if (processingFaulted || shouldPauseForRedstone() || pendingOperations <= 0 || hasPendingOutputs()) {
            return;
        }
        List<ResourceAmount> operationInputs = activeOperationInputs();
        RecipeOperation currentRecipe = normalizeRecipe(resolveRecipe(operationInputs));
        if (!validRecipe(currentRecipe) || !recipeMatchesActive(currentRecipe)) {
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
        for (int index = 0; index < inputSlotCount; index++) {
            operations = Math.min(operations, activeInputCounts[index] / inputPerOperation[index]);
        }
        if (operations <= 0 || !canProcessBatch(operations)) {
            return;
        }

        energyStorage.consumeEnergy(getEnergyCostForOperations(operations));
        for (int index = 0; index < inputSlotCount; index++) {
            activeInputCounts[index] -= operations * inputPerOperation[index];
        }
        pendingOperations -= operations;
        for (int index = 0; index < outputSlotCount; index++) {
            if (activeOutputKeys[index] != null) {
                pendingOutputCounts[index] = operations * outputPerOperation[index];
            }
        }
        progress = 0;
        setChanged();
        flushOutputs();
        finishActiveJobIfDrained();
        activateNextJobIfIdle();
        updateVisualState();
    }

    @Override
    protected final boolean isVisuallyWorking() {
        if (processingFaulted || shouldPauseForRedstone() || pendingOperations <= 0
                || hasPendingOutputs() || energyStorage.getEnergyStored() < getEnergyPerOperation()) {
            return false;
        }
        for (int index = 0; index < inputSlotCount; index++) {
            if (activeInputKeys[index] == null || activeInputCounts[index] < inputPerOperation[index]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final boolean returnAllResourcesToNetwork() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        networkEnabled = false;
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
        if (!getMainNode().isOnline()) {
            return false;
        }
        flushOutputs();
        returnActiveInputs();
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            returnJobInputs(job);
            if (job.isEmpty()) {
                queuedJobs.remove(index);
            }
        }
        finishActiveJobIfDrained();
        setChanged();
        return !hasProcessingWork();
    }

    @Override
    protected final boolean isPatternForThisMachine(IPatternDetails details) {
        if (level == null || details.getInputs().length != inputSlotCount
                || details.getOutputs().isEmpty() || details.getOutputs().size() > outputSlotCount) {
            return false;
        }
        for (GenericStack output : details.getOutputs()) {
            if (output.amount() <= 0 || !acceptsOutputKey(output.what())) {
                return false;
            }
        }
        return matchesAnyInputAlternative(details, 0, new ArrayList<>(inputSlotCount));
    }

    private boolean matchesAnyInputAlternative(IPatternDetails details, int index,
            List<ResourceAmount> selected) {
        if (index >= inputSlotCount) {
            RecipeOperation recipe = normalizeRecipe(resolveRecipe(selected));
            long operations = operationCount(selected, recipe);
            return validRecipe(recipe) && operations > 0
                    && matchesDeclaredOutputs(details, recipe, operations);
        }
        IPatternDetails.IInput input = details.getInputs()[index];
        for (GenericStack possible : input.getPossibleInputs()) {
            long amount = declaredInputAmount(input, possible);
            if (amount <= 0 || !acceptsInputKey(possible.what())) {
                continue;
            }
            selected.add(new ResourceAmount(possible.what(), amount));
            if (matchesAnyInputAlternative(details, index + 1, selected)) {
                return true;
            }
            selected.removeLast();
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

    private boolean matchesDeclaredOutputs(IPatternDetails details, RecipeOperation recipe, long operations) {
        if (details.getOutputs().isEmpty() || details.getOutputs().size() > outputSlotCount) {
            return false;
        }
        List<AEKey> seen = new ArrayList<>();
        for (GenericStack declared : details.getOutputs()) {
            if (seen.contains(declared.what())) {
                return false;
            }
            seen.add(declared.what());
            ResourceAmount actual = null;
            for (ResourceAmount output : recipe.outputs()) {
                if (output.key().equals(declared.what())) {
                    actual = output;
                    break;
                }
            }
            if (actual == null || !canMultiply(operations, actual.amount())
                    || declared.amount() != operations * actual.amount()) {
                return false;
            }
        }
        return true;
    }

    private long operationCount(List<ResourceAmount> delivered, RecipeOperation recipe) {
        if (!validRecipe(recipe) || delivered.size() != inputSlotCount) {
            return 0;
        }
        long operations = -1;
        for (int index = 0; index < inputSlotCount; index++) {
            ResourceAmount actual = delivered.get(index);
            ResourceAmount unit = recipe.inputs().get(index);
            if (!actual.key().equals(unit.key()) || actual.amount() % unit.amount() != 0) {
                return 0;
            }
            long count = actual.amount() / unit.amount();
            if (operations < 0) {
                operations = count;
            } else if (operations != count) {
                return 0;
            }
        }
        return Math.max(0, operations);
    }

    private void enqueueJob(ProcessingJob job) {
        if (!hasActiveInputs() && !hasPendingOutputs() && pendingOperations <= 0 && queuedJobs.isEmpty()) {
            activateJob(job);
            return;
        }
        if (job.matches(activePatternDefinition, activeInputKeys, inputPerOperation,
                activeOutputKeys, outputPerOperation) && canGrowActive(job)) {
            for (int index = 0; index < inputSlotCount; index++) {
                activeInputCounts[index] += job.inputCounts[index];
            }
            pendingOperations += job.operations;
            return;
        }
        for (ProcessingJob queued : queuedJobs) {
            if (queued.matches(job.patternDefinition, job.inputKeys, job.inputUnits,
                    job.outputKeys, job.outputUnits) && queued.canGrow(job)) {
                queued.grow(job);
                return;
            }
        }
        queuedJobs.add(job);
    }

    private boolean canGrowActive(ProcessingJob job) {
        if (!canAdd(pendingOperations, job.operations)) {
            return false;
        }
        for (int index = 0; index < inputSlotCount; index++) {
            if (!canAdd(activeInputCounts[index], job.inputCounts[index])) {
                return false;
            }
        }
        return true;
    }

    private void activateNextJobIfIdle() {
        if (hasActiveInputs() || pendingOperations > 0 || hasPendingOutputs() || queuedJobs.isEmpty()) {
            return;
        }
        activateJob(queuedJobs.removeFirst());
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        System.arraycopy(job.inputKeys, 0, activeInputKeys, 0, MAX_INPUT_SLOTS);
        System.arraycopy(job.inputCounts, 0, activeInputCounts, 0, MAX_INPUT_SLOTS);
        System.arraycopy(job.inputUnits, 0, inputPerOperation, 0, MAX_INPUT_SLOTS);
        System.arraycopy(job.outputKeys, 0, activeOutputKeys, 0, MAX_OUTPUT_SLOTS);
        System.arraycopy(job.outputUnits, 0, outputPerOperation, 0, MAX_OUTPUT_SLOTS);
        Arrays.fill(pendingOutputCounts, 0);
        pendingOperations = job.operations;
        progress = 0;
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0) {
            for (int index = 0; index < inputSlotCount; index++) {
                if (activeInputCounts[index] <= 0) {
                    activeInputKeys[index] = null;
                    activeInputCounts[index] = 0;
                }
            }
        }
        if (pendingOperations <= 0 && !hasActiveInputs() && !hasPendingOutputs()) {
            clearActiveJob();
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
    }

    private void clearActiveJob() {
        activePatternDefinition = ItemStack.EMPTY;
        Arrays.fill(activeInputKeys, null);
        Arrays.fill(activeInputCounts, 0);
        Arrays.fill(inputPerOperation, 1);
        Arrays.fill(activeOutputKeys, null);
        Arrays.fill(outputPerOperation, 1);
        Arrays.fill(pendingOutputCounts, 0);
        pendingOperations = 0;
        progress = 0;
    }

    private void flushOutputs() {
        if (!getMainNode().isOnline()) {
            return;
        }
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }
        var inventory = grid.getStorageService().getInventory();
        boolean changed = false;
        for (int index = 0; index < outputSlotCount; index++) {
            AEKey key = activeOutputKeys[index];
            long count = pendingOutputCounts[index];
            if (key == null || count <= 0) {
                continue;
            }
            long inserted = inventory.insert(key, count, Actionable.MODULATE, IActionSource.ofMachine(this));
            if (inserted > 0) {
                pendingOutputCounts[index] -= Math.min(count, inserted);
                changed = true;
            }
        }
        if (changed) {
            setChanged();
        }
    }

    private void returnActiveInputs() {
        if (!hasActiveInputs()) {
            return;
        }
        long[] returned = processingFaulted
                ? returnKeys(activeInputKeys, activeInputCounts, inputSlotCount)
                : returnCompleteOperations(activeInputKeys, activeInputCounts, inputPerOperation, inputSlotCount);
        for (int index = 0; index < inputSlotCount; index++) {
            activeInputCounts[index] -= returned[index];
            if (activeInputCounts[index] <= 0) {
                activeInputKeys[index] = null;
                activeInputCounts[index] = 0;
            }
        }
        pendingOperations = remainingOperations(activeInputCounts, inputPerOperation, inputSlotCount);
    }

    private void returnJobInputs(ProcessingJob job) {
        long[] returned = processingFaulted
                ? returnKeys(job.inputKeys, job.inputCounts, inputSlotCount)
                : returnCompleteOperations(job.inputKeys, job.inputCounts, job.inputUnits, inputSlotCount);
        for (int index = 0; index < inputSlotCount; index++) {
            job.inputCounts[index] -= returned[index];
        }
        job.operations = remainingOperations(job.inputCounts, job.inputUnits, inputSlotCount);
    }

    private long[] returnCompleteOperations(AEKey[] keys, long[] counts, long[] units, int size) {
        long[] returned = new long[MAX_INPUT_SLOTS];
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return returned;
        }
        long operations = remainingOperations(counts, units, size);
        if (operations <= 0) {
            return returned;
        }
        var inventory = grid.getStorageService().getInventory();
        for (int index = 0; index < size; index++) {
            if (keys[index] == null || !canMultiply(operations, units[index])) {
                return returned;
            }
            long accepted = inventory.insert(keys[index], operations * units[index],
                    Actionable.SIMULATE, IActionSource.ofMachine(this));
            operations = Math.min(operations, Math.max(0, accepted) / units[index]);
        }
        if (operations <= 0) {
            return returned;
        }
        for (int index = 0; index < size; index++) {
            long expected = operations * units[index];
            long inserted = inventory.insert(keys[index], expected,
                    Actionable.MODULATE, IActionSource.ofMachine(this));
            returned[index] = Math.min(expected, Math.max(0, inserted));
            if (inserted != expected || inserted % units[index] != 0) {
                processingFaulted = true;
            }
        }
        return returned;
    }

    private long[] returnKeys(AEKey[] keys, long[] counts, int size) {
        long[] returned = new long[MAX_INPUT_SLOTS];
        for (int index = 0; index < size; index++) {
            returned[index] = returnKeyToNetwork(keys[index], counts[index]);
        }
        return returned;
    }

    private long returnKeyToNetwork(AEKey key, long count) {
        var grid = getMainNode().getGrid();
        if (grid == null || key == null || count <= 0) {
            return 0;
        }
        return Math.min(count, Math.max(0, grid.getStorageService().getInventory().insert(
                key, count, Actionable.MODULATE, IActionSource.ofMachine(this))));
    }

    private List<ResourceAmount> activeOperationInputs() {
        List<ResourceAmount> result = new ArrayList<>(inputSlotCount);
        for (int index = 0; index < inputSlotCount; index++) {
            if (activeInputKeys[index] == null || activeInputCounts[index] < inputPerOperation[index]) {
                return List.of();
            }
            result.add(new ResourceAmount(activeInputKeys[index], inputPerOperation[index]));
        }
        return result;
    }

    private boolean recipeMatchesActive(RecipeOperation recipe) {
        if (recipe.inputs().size() != inputSlotCount || recipe.outputs().size() > outputSlotCount) {
            return false;
        }
        for (int index = 0; index < inputSlotCount; index++) {
            ResourceAmount input = recipe.inputs().get(index);
            if (!input.key().equals(activeInputKeys[index]) || input.amount() != inputPerOperation[index]) {
                return false;
            }
        }
        for (int index = 0; index < outputSlotCount; index++) {
            ResourceAmount output = index < recipe.outputs().size() ? recipe.outputs().get(index) : null;
            if (output == null) {
                if (activeOutputKeys[index] != null) {
                    return false;
                }
            } else if (!output.key().equals(activeOutputKeys[index])
                    || output.amount() != outputPerOperation[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean canProcessBatch(long operations) {
        for (int index = 0; index < inputSlotCount; index++) {
            if (!canMultiply(operations, inputPerOperation[index])) {
                return false;
            }
        }
        for (int index = 0; index < outputSlotCount; index++) {
            if (activeOutputKeys[index] != null && !canMultiply(operations, outputPerOperation[index])) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveInputs() {
        for (int index = 0; index < inputSlotCount; index++) {
            if (activeInputKeys[index] != null && activeInputCounts[index] > 0) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPendingOutputs() {
        for (int index = 0; index < outputSlotCount; index++) {
            if (activeOutputKeys[index] != null && pendingOutputCounts[index] > 0) {
                return true;
            }
        }
        return false;
    }

    private long getTotalQueuedOperations() {
        long total = Math.max(0, pendingOperations);
        for (ProcessingJob job : queuedJobs) {
            total = saturatingAdd(total, Math.max(0, job.operations));
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
        tag.putInt("MultiKeyTaskVersion", TASK_DATA_VERSION);
        tag.putInt("MultiInputSlots", inputSlotCount);
        tag.putInt("MultiOutputSlots", outputSlotCount);
        tag.put("MultiActiveInputs", saveResources(activeInputKeys, activeInputCounts,
                inputPerOperation, inputSlotCount, registries));
        tag.put("MultiActiveOutputs", saveResources(activeOutputKeys, pendingOutputCounts,
                outputPerOperation, outputSlotCount, registries));
        if (!activePatternDefinition.isEmpty()) {
            tag.put("MultiActivePattern", activePatternDefinition.save(registries));
        }
        ListTag jobs = new ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag saved = new CompoundTag();
            saved.put("Pattern", job.patternDefinition.save(registries));
            saved.put("Inputs", saveResources(job.inputKeys, job.inputCounts,
                    job.inputUnits, inputSlotCount, registries));
            saved.put("Outputs", saveResources(job.outputKeys, new long[MAX_OUTPUT_SLOTS],
                    job.outputUnits, outputSlotCount, registries));
            saved.putLong("Operations", job.operations);
            jobs.add(saved);
        }
        tag.put("MultiKeyQueue", jobs);
        tag.putLong("MultiPendingOperations", pendingOperations);
        tag.putInt("MultiProgress", progress);
        tag.putBoolean("MultiProcessingFaulted", processingFaulted);
    }

    private static ListTag saveResources(AEKey[] keys, long[] counts, long[] units, int size,
            HolderLookup.Provider registries) {
        ListTag resources = new ListTag();
        for (int index = 0; index < size; index++) {
            if (keys[index] == null) {
                continue;
            }
            CompoundTag resource = new CompoundTag();
            resource.putInt("Slot", index);
            resource.put("Key", keys[index].toTagGeneric(registries));
            resource.putLong("Count", Math.max(0, counts[index]));
            resource.putLong("Unit", Math.max(1, units[index]));
            resources.add(resource);
        }
        return resources;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        clearActiveJob();
        queuedJobs.clear();
        processingFaulted = false;
        if (tag.getInt("MultiKeyTaskVersion") != TASK_DATA_VERSION
                || tag.getInt("MultiInputSlots") != inputSlotCount
                || tag.getInt("MultiOutputSlots") != outputSlotCount) {
            return;
        }
        loadResources(tag.getList("MultiActiveInputs", net.minecraft.nbt.Tag.TAG_COMPOUND),
                activeInputKeys, activeInputCounts, inputPerOperation, inputSlotCount, registries, true);
        loadResources(tag.getList("MultiActiveOutputs", net.minecraft.nbt.Tag.TAG_COMPOUND),
                activeOutputKeys, pendingOutputCounts, outputPerOperation, outputSlotCount, registries, false);
        activePatternDefinition = tag.contains("MultiActivePattern")
                ? ItemStack.parseOptional(registries, tag.getCompound("MultiActivePattern")) : ItemStack.EMPTY;
        pendingOperations = Math.max(0, tag.getLong("MultiPendingOperations"));
        progress = Math.max(0, tag.getInt("MultiProgress"));
        processingFaulted = tag.getBoolean("MultiProcessingFaulted");

        ListTag jobs = tag.getList("MultiKeyQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < jobs.size(); index++) {
            CompoundTag saved = jobs.getCompound(index);
            ItemStack pattern = ItemStack.parseOptional(registries, saved.getCompound("Pattern"));
            AEKey[] inputKeys = new AEKey[MAX_INPUT_SLOTS];
            long[] inputCounts = new long[MAX_INPUT_SLOTS];
            long[] inputUnits = filledLongArray(MAX_INPUT_SLOTS, 1);
            AEKey[] outputKeys = new AEKey[MAX_OUTPUT_SLOTS];
            long[] outputUnits = filledLongArray(MAX_OUTPUT_SLOTS, 1);
            loadResources(saved.getList("Inputs", net.minecraft.nbt.Tag.TAG_COMPOUND),
                    inputKeys, inputCounts, inputUnits, inputSlotCount, registries, true);
            loadResources(saved.getList("Outputs", net.minecraft.nbt.Tag.TAG_COMPOUND),
                    outputKeys, new long[MAX_OUTPUT_SLOTS], outputUnits, outputSlotCount, registries, false);
            long operations = Math.max(0, saved.getLong("Operations"));
            ProcessingJob job = new ProcessingJob(pattern, inputKeys, inputCounts, inputUnits,
                    outputKeys, outputUnits, operations);
            if (job.isStructurallyValid(inputSlotCount, outputSlotCount)) {
                queuedJobs.add(job);
            } else {
                processingFaulted = true;
            }
        }
        validateLoadedLedger();
    }

    private void loadResources(ListTag resources, AEKey[] keys, long[] counts, long[] units,
            int size, HolderLookup.Provider registries, boolean input) {
        for (int index = 0; index < resources.size(); index++) {
            CompoundTag saved = resources.getCompound(index);
            int slot = saved.getInt("Slot");
            if (slot < 0 || slot >= size || !saved.contains("Key", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                continue;
            }
            AEKey key = AEKey.fromTagGeneric(registries, saved.getCompound("Key"));
            if (key != null && (input ? acceptsInputKey(key) : acceptsOutputKey(key))) {
                keys[slot] = key;
                counts[slot] = Math.max(0, saved.getLong("Count"));
                units[slot] = Math.max(1, saved.getLong("Unit"));
            }
        }
    }

    private void validateLoadedLedger() {
        if (pendingOperations > 0) {
            for (int index = 0; index < inputSlotCount; index++) {
                if (activeInputKeys[index] == null || activeInputCounts[index] <= 0
                        || activeInputCounts[index] % inputPerOperation[index] != 0
                        || pendingOperations != activeInputCounts[index] / inputPerOperation[index]) {
                    processingFaulted = true;
                }
            }
        }
        if ((hasActiveInputs() || hasPendingOutputs()) && activePatternDefinition.isEmpty()) {
            processingFaulted = true;
        }
        for (ProcessingJob job : queuedJobs) {
            if (!job.isStructurallyValid(inputSlotCount, outputSlotCount)) {
                processingFaulted = true;
            }
        }
    }

    private RecipeOperation normalizeRecipe(RecipeOperation recipe) {
        if (recipe == null || recipe.outputs() == null || recipe.inputs() == null) {
            return null;
        }
        List<ResourceAmount> outputs = new ArrayList<>();
        for (ResourceAmount output : recipe.outputs()) {
            if (output == null || output.key() == null || output.amount() <= 0) {
                continue;
            }
            int existing = -1;
            for (int index = 0; index < outputs.size(); index++) {
                if (outputs.get(index).key().equals(output.key())) {
                    existing = index;
                    break;
                }
            }
            if (existing < 0) {
                outputs.add(output);
            } else {
                ResourceAmount previous = outputs.get(existing);
                if (!canAdd(previous.amount(), output.amount())) {
                    return null;
                }
                outputs.set(existing, new ResourceAmount(previous.key(), previous.amount() + output.amount()));
            }
        }
        return new RecipeOperation(List.copyOf(recipe.inputs()), List.copyOf(outputs));
    }

    private boolean validRecipe(RecipeOperation recipe) {
        if (recipe == null || recipe.inputs().size() != inputSlotCount
                || recipe.outputs().isEmpty() || recipe.outputs().size() > outputSlotCount) {
            return false;
        }
        for (ResourceAmount input : recipe.inputs()) {
            if (input == null || input.key() == null || input.amount() <= 0 || !acceptsInputKey(input.key())) {
                return false;
            }
        }
        for (ResourceAmount output : recipe.outputs()) {
            if (output == null || output.key() == null || output.amount() <= 0 || !acceptsOutputKey(output.key())) {
                return false;
            }
        }
        return true;
    }

    private static long declaredInputAmount(IPatternDetails.IInput input, GenericStack possible) {
        long amount = possible.amount();
        long multiplier = input.getMultiplier();
        return amount > 0 && multiplier > 0 && canMultiply(amount, multiplier)
                ? amount * multiplier : 0;
    }

    private static long remainingOperations(long[] counts, long[] units, int size) {
        long operations = Long.MAX_VALUE;
        for (int index = 0; index < size; index++) {
            operations = Math.min(operations, counts[index] / Math.max(1, units[index]));
        }
        return operations == Long.MAX_VALUE ? 0 : Math.max(0, operations);
    }

    private static int resourceType(AEKey key) {
        if (key instanceof AEItemKey) {
            return RESOURCE_ITEM;
        }
        if (key instanceof MekanismKey) {
            return RESOURCE_CHEMICAL;
        }
        return key instanceof AEFluidKey ? RESOURCE_FLUID : RESOURCE_NONE;
    }

    private static int resourceRegistryId(AEKey key) {
        if (key instanceof MekanismKey chemical) {
            return MekanismAPI.CHEMICAL_REGISTRY.getId(chemical.getStack().getChemical());
        }
        if (key instanceof AEFluidKey fluid) {
            return BuiltInRegistries.FLUID.getId(fluid.getFluid());
        }
        return -1;
    }

    private static ItemStack itemDisplay(AEKey key, long count) {
        if (!(key instanceof AEItemKey itemKey) || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack display = itemKey.toStack(1);
        display.setCount((int) Math.max(1, Math.min(count, display.getMaxStackSize())));
        return display;
    }

    private static long saturatingAdd(long first, long second) {
        return canAdd(first, second) ? first + second : Long.MAX_VALUE;
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

    private static long[] filledLongArray(int size, long value) {
        long[] result = new long[size];
        Arrays.fill(result, value);
        return result;
    }

    protected record ResourceAmount(AEKey key, long amount) {
        public ResourceAmount {
            if (key == null || amount <= 0) {
                throw new IllegalArgumentException("Resource amount must be positive");
            }
        }
    }

    protected record RecipeOperation(List<ResourceAmount> inputs, List<ResourceAmount> outputs) {
    }

    private static final class ProcessingJob {
        private final ItemStack patternDefinition;
        private final AEKey[] inputKeys;
        private final long[] inputCounts;
        private final long[] inputUnits;
        private final AEKey[] outputKeys;
        private final long[] outputUnits;
        private long operations;

        private ProcessingJob(ItemStack pattern, List<ResourceAmount> delivered,
                RecipeOperation recipe, long operations) {
            this(pattern, keys(delivered, MAX_INPUT_SLOTS), amounts(delivered, MAX_INPUT_SLOTS),
                    amounts(recipe.inputs(), MAX_INPUT_SLOTS), keys(recipe.outputs(), MAX_OUTPUT_SLOTS),
                    amounts(recipe.outputs(), MAX_OUTPUT_SLOTS), operations);
        }

        private ProcessingJob(ItemStack pattern, AEKey[] inputKeys, long[] inputCounts,
                long[] inputUnits, AEKey[] outputKeys, long[] outputUnits, long operations) {
            patternDefinition = pattern.copyWithCount(1);
            this.inputKeys = inputKeys.clone();
            this.inputCounts = inputCounts.clone();
            this.inputUnits = inputUnits.clone();
            this.outputKeys = outputKeys.clone();
            this.outputUnits = outputUnits.clone();
            this.operations = Math.max(0, operations);
        }

        private boolean matches(ItemStack pattern, AEKey[] inputs, long[] inputUnits,
                AEKey[] outputs, long[] outputUnits) {
            return ItemStack.isSameItemSameComponents(patternDefinition, pattern)
                    && Arrays.equals(inputKeys, inputs) && Arrays.equals(this.inputUnits, inputUnits)
                    && Arrays.equals(outputKeys, outputs) && Arrays.equals(this.outputUnits, outputUnits);
        }

        private boolean canGrow(ProcessingJob other) {
            if (!canAdd(operations, other.operations)) {
                return false;
            }
            for (int index = 0; index < inputCounts.length; index++) {
                if (!canAdd(inputCounts[index], other.inputCounts[index])) {
                    return false;
                }
            }
            return true;
        }

        private void grow(ProcessingJob other) {
            for (int index = 0; index < inputCounts.length; index++) {
                inputCounts[index] += other.inputCounts[index];
            }
            operations += other.operations;
        }

        private boolean isStructurallyValid(int inputSize, int outputSize) {
            if (patternDefinition.isEmpty() || operations <= 0) {
                return false;
            }
            for (int index = 0; index < inputSize; index++) {
                if (inputKeys[index] == null || inputCounts[index] <= 0 || inputUnits[index] <= 0
                        || inputCounts[index] % inputUnits[index] != 0
                        || operations != inputCounts[index] / inputUnits[index]) {
                    return false;
                }
            }
            boolean output = false;
            for (int index = 0; index < outputSize; index++) {
                if (outputKeys[index] != null && outputUnits[index] > 0) {
                    output = true;
                }
            }
            return output;
        }

        private boolean isEmpty() {
            for (long count : inputCounts) {
                if (count > 0) {
                    return false;
                }
            }
            return true;
        }

        private static AEKey[] keys(List<ResourceAmount> resources, int size) {
            AEKey[] result = new AEKey[size];
            for (int index = 0; index < Math.min(size, resources.size()); index++) {
                result[index] = resources.get(index).key();
            }
            return result;
        }

        private static long[] amounts(List<ResourceAmount> resources, int size) {
            long[] result = filledLongArray(size, 1);
            for (int index = 0; index < Math.min(size, resources.size()); index++) {
                result[index] = resources.get(index).amount();
            }
            return result;
        }
    }
}
