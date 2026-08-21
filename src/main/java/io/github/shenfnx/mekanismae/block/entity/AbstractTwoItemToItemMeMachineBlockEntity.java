package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.CombinerRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GTNH-style isolated task ledger for Mekanism item + item -> item recipes.
 * Both delivered input slots remain attached to the encoded pattern that caused
 * the submission, so ingredients from compatible recipes can never cross-feed.
 */
public abstract class AbstractTwoItemToItemMeMachineBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 1;

    private AEItemKey activeMainKey;
    private long activeMainCount;
    private int mainPerOperation = 1;
    private AEItemKey activeExtraKey;
    private long activeExtraCount;
    private int extraPerOperation = 1;
    private AEItemKey pendingOutputKey;
    private long pendingOutputCount;
    private ItemStack activePatternDefinition = ItemStack.EMPTY;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private long pendingOperations;
    private int progress;
    private boolean processingFaulted;

    protected AbstractTwoItemToItemMeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType) {
        super(type, pos, state, visualRepresentation, machineType);
    }

    @Override
    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        return activeMainKey == null ? ItemStack.EMPTY
                : createDisplayStack(activeMainKey, Math.min(activeMainCount, mainPerOperation));
    }

    @Override
    public ItemStack getProcessingSecondaryInputDisplay() {
        activateNextJobIfIdle();
        return activeExtraKey == null ? ItemStack.EMPTY
                : createDisplayStack(activeExtraKey, Math.min(activeExtraCount, extraPerOperation));
    }

    @Override
    public ItemStack getProcessingOutputDisplay() {
        activateNextJobIfIdle();
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            return createDisplayStack(pendingOutputKey, pendingOutputCount);
        }
        CombinerRecipe recipe = findRecipeForActive();
        return recipe == null ? ItemStack.EMPTY
                : recipe.getOutput(activeMainKey.toStack(mainPerOperation), activeExtraKey.toStack(extraPerOperation));
    }

    @Override
    public ContainerData getContainerData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energyStorage.getEnergyStored();
                    case 1 -> progress;
                    case 2, 11 -> (int) Math.min(Integer.MAX_VALUE, getTotalQueuedOperations());
                    case 3 -> getProcessingTicks();
                    case 4 -> getMainNode().isOnline() ? 1 : 0;
                    case 5 -> energyStorage.getMaxEnergyStored();
                    case 6 -> networkEnabled ? 1 : 0;
                    case 7 -> speedUpgrades;
                    case 8 -> parallelUpgrades;
                    case 9 -> getUpgradeCount(ModItems.ENERGY_CARD.get());
                    case 10 -> energyStorage.getReceiveLimit();
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
            }

            @Override
            public int getCount() {
                return 17;
            }
        };
    }

    public long getBufferedOperationCount() {
        return getTotalQueuedOperations();
    }

    public long getCurrentOperationCount() {
        return Math.max(0, pendingOperations);
    }

    public ItemStack getBufferedInputDisplay() {
        return activeMainKey == null ? ItemStack.EMPTY : activeMainKey.toStack(1);
    }

    public ItemStack getBufferedExtraInputDisplay() {
        return activeExtraKey == null ? ItemStack.EMPTY : activeExtraKey.toStack(1);
    }

    public ItemStack getBufferedOutputDisplay() {
        return pendingOutputKey == null ? ItemStack.EMPTY : pendingOutputKey.toStack(1);
    }

    public long getBufferedOutputCount() {
        return Math.max(0, pendingOutputCount);
    }

    @Override
    public boolean isProcessingFaulted() {
        return processingFaulted;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy() || !supportsPattern(details)
                || inputs.length != 2) {
            return false;
        }
        DeliveredInput first = readInput(inputs[0]);
        DeliveredInput second = readInput(inputs[1]);
        if (first == null || second == null) {
            return false;
        }
        RecipeMatch match = matchRecipe(first, second);
        if (match == null) {
            return false;
        }
        long mainNeeded = match.recipe.getMainInput().getNeededAmount(match.main.key.toStack(1));
        long extraNeeded = match.recipe.getExtraInput().getNeededAmount(match.extra.key.toStack(1));
        if (mainNeeded <= 0 || mainNeeded > Integer.MAX_VALUE || extraNeeded <= 0
                || extraNeeded > Integer.MAX_VALUE || match.main.count % mainNeeded != 0
                || match.extra.count % extraNeeded != 0) {
            return false;
        }
        long operations = match.main.count / mainNeeded;
        if (operations <= 0 || operations != match.extra.count / extraNeeded
                || !matchesDeclaredOutputs(match.recipe, details, match.main.key, (int) mainNeeded,
                        match.extra.key, (int) extraNeeded, operations)) {
            return false;
        }
        long buffered = getTotalQueuedOperations();
        if (buffered > getBufferOperationLimit() || operations > getBufferOperationLimit() - buffered) {
            return false;
        }
        enqueueJob(new ProcessingJob(details.getDefinition().toStack(), match.main.key, match.main.count,
                (int) mainNeeded, match.extra.key, match.extra.count, (int) extraNeeded, operations));
        setChanged();
        return true;
    }

    @Override
    public boolean isBusy() {
        return !networkEnabled || processingFaulted || getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    @Override
    protected boolean isPatternForThisMachine(IPatternDetails details) {
        if (level == null || details.getInputs().length != 2 || details.getOutputs().size() != 1) {
            return false;
        }
        for (GenericStack first : details.getInputs()[0].getPossibleInputs()) {
            if (!(first.what() instanceof AEItemKey firstKey)) {
                continue;
            }
            for (GenericStack second : details.getInputs()[1].getPossibleInputs()) {
                if (!(second.what() instanceof AEItemKey secondKey)) {
                    continue;
                }
                long firstCount = declaredInputAmount(details.getInputs()[0], firstKey);
                long secondCount = declaredInputAmount(details.getInputs()[1], secondKey);
                RecipeMatch match = matchRecipe(new DeliveredInput(firstKey, firstCount),
                        new DeliveredInput(secondKey, secondCount));
                if (match == null) {
                    continue;
                }
                long mainNeeded = match.recipe.getMainInput().getNeededAmount(match.main.key.toStack(1));
                long extraNeeded = match.recipe.getExtraInput().getNeededAmount(match.extra.key.toStack(1));
                if (mainNeeded > 0 && extraNeeded > 0 && match.main.count % mainNeeded == 0
                        && match.extra.count % extraNeeded == 0) {
                    long operations = match.main.count / mainNeeded;
                    if (operations > 0 && operations == match.extra.count / extraNeeded
                            && matchesDeclaredOutputs(match.recipe, details, match.main.key, (int) mainNeeded,
                                    match.extra.key, (int) extraNeeded, operations)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean supportsPattern(IPatternDetails details) {
        if (!isPatternForThisMachine(details)) {
            return false;
        }
        for (ItemStack pattern : patternSlots) {
            IPatternDetails stored = pattern.isEmpty() ? null : PatternDetailsHelper.decodePattern(pattern, level);
            if (stored != null && stored.getDefinition().equals(details.getDefinition())) {
                return true;
            }
        }
        return false;
    }

    private RecipeMatch matchRecipe(DeliveredInput first, DeliveredInput second) {
        if (level == null || first.count <= 0 || second.count <= 0) {
            return null;
        }
        ItemStack firstStack = first.key.toStack((int) Math.min(Integer.MAX_VALUE, first.count));
        ItemStack secondStack = second.key.toStack((int) Math.min(Integer.MAX_VALUE, second.count));
        CombinerRecipe recipe = MekanismRecipeType.COMBINING.getInputCache()
                .findFirstRecipe(level, firstStack, secondStack);
        if (recipe != null) {
            return new RecipeMatch(recipe, first, second);
        }
        recipe = MekanismRecipeType.COMBINING.getInputCache().findFirstRecipe(level, secondStack, firstStack);
        return recipe == null ? null : new RecipeMatch(recipe, second, first);
    }

    private boolean matchesDeclaredOutputs(CombinerRecipe recipe, IPatternDetails details,
            AEItemKey mainKey, int mainAmount, AEItemKey extraKey, int extraAmount, long operations) {
        if (details.getOutputs().size() != 1) {
            return false;
        }
        ItemStack output = recipe.getOutput(mainKey.toStack(mainAmount), extraKey.toStack(extraAmount));
        GenericStack declared = details.getOutputs().getFirst();
        return !output.isEmpty() && declared.what() instanceof AEItemKey outputKey && outputKey.matches(output)
                && operations <= Long.MAX_VALUE / output.getCount()
                && declared.amount() == operations * output.getCount();
    }

    private static DeliveredInput readInput(KeyCounter counter) {
        AEItemKey key = null;
        long count = 0;
        for (Object2LongMap.Entry<appeng.api.stacks.AEKey> entry : counter) {
            if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0
                    || key != null && !key.equals(itemKey) || !canAdd(count, entry.getLongValue())) {
                return null;
            }
            key = itemKey;
            count += entry.getLongValue();
        }
        return key == null || count <= 0 ? null : new DeliveredInput(key, count);
    }

    private static long declaredInputAmount(IPatternDetails.IInput input, AEItemKey key) {
        for (GenericStack possible : input.getPossibleInputs()) {
            if (key.equals(possible.what()) && possible.amount() > 0 && input.getMultiplier() > 0
                    && possible.amount() <= Long.MAX_VALUE / input.getMultiplier()) {
                return possible.amount() * input.getMultiplier();
            }
        }
        return 0;
    }

    private void enqueueJob(ProcessingJob job) {
        if (pendingOperations <= 0 && activeMainKey == null && activeExtraKey == null
                && pendingOutputKey == null && queuedJobs.isEmpty()) {
            activateJob(job);
            return;
        }
        if (matchesActive(job) && canAdd(activeMainCount, job.mainCount)
                && canAdd(activeExtraCount, job.extraCount) && canAdd(pendingOperations, job.operations)) {
            activeMainCount += job.mainCount;
            activeExtraCount += job.extraCount;
            pendingOperations += job.operations;
            return;
        }
        for (ProcessingJob queued : queuedJobs) {
            if (queued.matches(job) && queued.canGrow(job)) {
                queued.mainCount += job.mainCount;
                queued.extraCount += job.extraCount;
                queued.operations += job.operations;
                return;
            }
        }
        queuedJobs.add(job);
    }

    private boolean matchesActive(ProcessingJob job) {
        return activeMainKey != null && activeExtraKey != null
                && activeMainKey.equals(job.mainKey) && activeExtraKey.equals(job.extraKey)
                && mainPerOperation == job.mainPerOperation && extraPerOperation == job.extraPerOperation
                && ItemStack.isSameItemSameComponents(activePatternDefinition, job.patternDefinition);
    }

    private long getTotalQueuedOperations() {
        long total = Math.max(0, pendingOperations);
        for (ProcessingJob job : queuedJobs) {
            if (!canAdd(total, job.operations)) {
                return Long.MAX_VALUE;
            }
            total += job.operations;
        }
        return total;
    }

    @Override
    protected boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0 || activeMainCount > 0 || activeExtraCount > 0
                || pendingOutputCount > 0 || !queuedJobs.isEmpty();
    }

    @Override
    public boolean hasStoredContents() {
        return hasProcessingWork() || !isEmpty();
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
        if (processingFaulted || shouldPauseForRedstone() || pendingOutputCount > 0 || pendingOperations <= 0) {
            return;
        }
        CombinerRecipe recipe = findRecipeForActive();
        if (recipe == null || activeMainCount < mainPerOperation || activeExtraCount < extraPerOperation) {
            markFault();
            return;
        }
        ItemStack output = recipe.getOutput(activeMainKey.toStack(mainPerOperation),
                activeExtraKey.toStack(extraPerOperation));
        if (output.isEmpty()) {
            markFault();
            return;
        }
        int processingTicks = getProcessingTicks();
        progress = (int) Math.min(processingTicks, (long) progress + getSpeedMultiplier());
        updateVisualState();
        int energyPerOperation = getEnergyPerOperation();
        if (progress < processingTicks || energyStorage.getEnergyStored() < energyPerOperation) {
            return;
        }
        long operations = Math.min(getParallelBatchSize(), pendingOperations);
        operations = Math.min(operations, energyStorage.getEnergyStored() / energyPerOperation);
        operations = Math.min(operations, activeMainCount / mainPerOperation);
        operations = Math.min(operations, activeExtraCount / extraPerOperation);
        if (operations <= 0 || operations > Long.MAX_VALUE / output.getCount()) {
            return;
        }
        long produced = operations * output.getCount();
        AEItemKey outputKey = AEItemKey.of(output);
        if (pendingOutputKey != null && (!pendingOutputKey.equals(outputKey)
                || !canAdd(pendingOutputCount, produced))) {
            markFault();
            return;
        }
        energyStorage.consumeEnergy(getEnergyCostForOperations(operations));
        activeMainCount -= operations * mainPerOperation;
        activeExtraCount -= operations * extraPerOperation;
        pendingOperations -= operations;
        pendingOutputKey = outputKey;
        pendingOutputCount += produced;
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
                && activeMainKey != null && activeMainCount >= mainPerOperation
                && activeExtraKey != null && activeExtraCount >= extraPerOperation
                && pendingOutputCount <= 0 && energyStorage.getEnergyStored() >= getEnergyPerOperation();
    }

    private CombinerRecipe findRecipeForActive() {
        return activeMainKey == null || activeExtraKey == null || level == null ? null
                : MekanismRecipeType.COMBINING.getInputCache().findFirstRecipe(level,
                        activeMainKey.toStack(mainPerOperation), activeExtraKey.toStack(extraPerOperation));
    }

    private void markFault() {
        processingFaulted = true;
        progress = 0;
        updateVisualState();
        setChanged();
    }

    private void flushOutput() {
        if (pendingOutputKey == null || pendingOutputCount <= 0 || !getMainNode().isOnline()
                || getMainNode().getGrid() == null) {
            return;
        }
        long inserted = getMainNode().getGrid().getStorageService().getInventory().insert(pendingOutputKey,
                pendingOutputCount, Actionable.MODULATE, IActionSource.ofMachine(this));
        pendingOutputCount -= Math.max(0, inserted);
        if (pendingOutputCount <= 0) {
            pendingOutputKey = null;
            pendingOutputCount = 0;
        }
        if (inserted > 0) {
            setChanged();
        }
    }

    private void finishActiveJobIfDrained() {
        if (pendingOperations <= 0 && activeMainCount <= 0 && activeExtraCount <= 0) {
            activeMainKey = null;
            activeExtraKey = null;
        }
        if (activeMainKey == null && activeExtraKey == null && pendingOutputKey == null) {
            activePatternDefinition = ItemStack.EMPTY;
            mainPerOperation = 1;
            extraPerOperation = 1;
            progress = 0;
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
    }

    private void activateNextJobIfIdle() {
        if (activeMainKey != null || activeExtraKey != null || pendingOperations > 0 || pendingOutputCount > 0
                || queuedJobs.isEmpty()) {
            return;
        }
        activateJob(queuedJobs.removeFirst());
        setChanged();
    }

    private void activateJob(ProcessingJob job) {
        activePatternDefinition = job.patternDefinition.copy();
        activeMainKey = job.mainKey;
        activeMainCount = job.mainCount;
        mainPerOperation = job.mainPerOperation;
        activeExtraKey = job.extraKey;
        activeExtraCount = job.extraCount;
        extraPerOperation = job.extraPerOperation;
        pendingOperations = job.operations;
        progress = 0;
    }

    @Override
    public boolean returnAllResourcesToNetwork() {
        if (level == null || level.isClientSide()) {
            return false;
        }
        networkEnabled = false;
        ICraftingProvider.requestUpdate(getMainNode());
        flushOutput();
        if (!getMainNode().isOnline() || getMainNode().getGrid() == null) {
            return false;
        }
        if (activeMainKey != null && activeExtraKey != null) {
            ReturnedInputs returned = returnInputs(activeMainKey, activeMainCount, mainPerOperation,
                    activeExtraKey, activeExtraCount, extraPerOperation);
            activeMainCount -= returned.main;
            activeExtraCount -= returned.extra;
            pendingOperations = Math.min(activeMainCount / mainPerOperation, activeExtraCount / extraPerOperation);
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            ReturnedInputs returned = returnInputs(job.mainKey, job.mainCount, job.mainPerOperation,
                    job.extraKey, job.extraCount, job.extraPerOperation);
            job.mainCount -= returned.main;
            job.extraCount -= returned.extra;
            job.operations = Math.min(job.mainCount / job.mainPerOperation, job.extraCount / job.extraPerOperation);
            if (job.mainCount <= 0 && job.extraCount <= 0) {
                queuedJobs.remove(index);
            }
        }
        if (activeMainCount <= 0 && activeExtraCount <= 0) {
            activeMainKey = null;
            activeExtraKey = null;
            pendingOperations = 0;
            activePatternDefinition = ItemStack.EMPTY;
            progress = 0;
        }
        if (!hasProcessingWork()) {
            processingFaulted = false;
        }
        setChanged();
        return !hasProcessingWork();
    }

    private ReturnedInputs returnInputs(AEItemKey mainKey, long mainCount, int mainUnit,
            AEItemKey extraKey, long extraCount, int extraUnit) {
        var inventory = getMainNode().getGrid().getStorageService().getInventory();
        long operations = Math.min(mainCount / mainUnit, extraCount / extraUnit);
        long acceptedMain = inventory.insert(mainKey, operations * mainUnit, Actionable.SIMULATE,
                IActionSource.ofMachine(this));
        long acceptedExtra = inventory.insert(extraKey, operations * extraUnit, Actionable.SIMULATE,
                IActionSource.ofMachine(this));
        operations = Math.min(acceptedMain / mainUnit, acceptedExtra / extraUnit);
        if (operations <= 0) {
            return ReturnedInputs.NONE;
        }
        long mainAmount = operations * mainUnit;
        long extraAmount = operations * extraUnit;
        long insertedMain = inventory.insert(mainKey, mainAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        long insertedExtra = inventory.insert(extraKey, extraAmount, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (insertedMain != mainAmount || insertedExtra != extraAmount) {
            processingFaulted = true;
        }
        return new ReturnedInputs(Math.max(0, insertedMain), Math.max(0, insertedExtra));
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("TwoItemTaskDataVersion", TASK_DATA_VERSION);
        saveJobState(tag, registries, "Active", activePatternDefinition, activeMainKey, activeMainCount,
                mainPerOperation, activeExtraKey, activeExtraCount, extraPerOperation, pendingOperations);
        if (pendingOutputKey != null && pendingOutputCount > 0) {
            tag.put("PendingOutput", pendingOutputKey.toStack(1).save(registries));
            tag.putLong("PendingOutputCount", pendingOutputCount);
        }
        var jobs = new net.minecraft.nbt.ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag saved = new CompoundTag();
            saveJobState(saved, registries, "", job.patternDefinition, job.mainKey, job.mainCount,
                    job.mainPerOperation, job.extraKey, job.extraCount, job.extraPerOperation, job.operations);
            jobs.add(saved);
        }
        tag.put("TwoItemProcessingQueue", jobs);
        tag.putInt("TwoItemProgress", progress);
        tag.putBoolean("TwoItemProcessingFaulted", processingFaulted);
    }

    private static void saveJobState(CompoundTag tag, HolderLookup.Provider registries, String prefix,
            ItemStack pattern, AEItemKey mainKey, long mainCount, int mainUnit, AEItemKey extraKey,
            long extraCount, int extraUnit, long operations) {
        if (!pattern.isEmpty()) {
            tag.put(prefix + "Pattern", pattern.save(registries));
        }
        if (mainKey != null) {
            tag.put(prefix + "Main", mainKey.toStack(1).save(registries));
            tag.putLong(prefix + "MainCount", mainCount);
        }
        if (extraKey != null) {
            tag.put(prefix + "Extra", extraKey.toStack(1).save(registries));
            tag.putLong(prefix + "ExtraCount", extraCount);
        }
        tag.putInt(prefix + "MainPerOperation", mainUnit);
        tag.putInt(prefix + "ExtraPerOperation", extraUnit);
        tag.putLong(prefix + "Operations", operations);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        activePatternDefinition = readStack(tag, "ActivePattern", registries);
        ItemStack main = readStack(tag, "ActiveMain", registries);
        activeMainKey = main.isEmpty() ? null : AEItemKey.of(main);
        activeMainCount = Math.max(0, tag.getLong("ActiveMainCount"));
        mainPerOperation = Math.max(1, tag.getInt("ActiveMainPerOperation"));
        ItemStack extra = readStack(tag, "ActiveExtra", registries);
        activeExtraKey = extra.isEmpty() ? null : AEItemKey.of(extra);
        activeExtraCount = Math.max(0, tag.getLong("ActiveExtraCount"));
        extraPerOperation = Math.max(1, tag.getInt("ActiveExtraPerOperation"));
        pendingOperations = Math.max(0, tag.getLong("ActiveOperations"));
        ItemStack output = readStack(tag, "PendingOutput", registries);
        pendingOutputKey = output.isEmpty() ? null : AEItemKey.of(output);
        pendingOutputCount = Math.max(0, tag.getLong("PendingOutputCount"));
        progress = Math.max(0, tag.getInt("TwoItemProgress"));
        processingFaulted = tag.getBoolean("TwoItemProcessingFaulted");
        queuedJobs.clear();
        var jobs = tag.getList("TwoItemProcessingQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < jobs.size(); index++) {
            CompoundTag saved = jobs.getCompound(index);
            ItemStack pattern = readStack(saved, "Pattern", registries);
            ItemStack savedMain = readStack(saved, "Main", registries);
            ItemStack savedExtra = readStack(saved, "Extra", registries);
            if (!pattern.isEmpty() && !savedMain.isEmpty() && !savedExtra.isEmpty()) {
                queuedJobs.add(new ProcessingJob(pattern, AEItemKey.of(savedMain),
                        Math.max(0, saved.getLong("MainCount")),
                        Math.max(1, saved.getInt("MainPerOperation")), AEItemKey.of(savedExtra),
                        Math.max(0, saved.getLong("ExtraCount")),
                        Math.max(1, saved.getInt("ExtraPerOperation")),
                        Math.max(0, saved.getLong("Operations"))));
            }
        }
        validateLoadedState();
    }

    private void validateLoadedState() {
        if (activeMainKey == null || activeExtraKey == null || activePatternDefinition.isEmpty()
                || activeMainCount % mainPerOperation != 0 || activeExtraCount % extraPerOperation != 0
                || pendingOperations != activeMainCount / mainPerOperation
                || pendingOperations != activeExtraCount / extraPerOperation) {
            if (activeMainCount > 0 || activeExtraCount > 0 || pendingOperations > 0) {
                processingFaulted = true;
            }
        }
        for (ProcessingJob job : queuedJobs) {
            if (!job.isConsistent()) {
                processingFaulted = true;
            }
        }
    }

    private static ItemStack readStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        return tag.contains(key) ? ItemStack.parseOptional(registries, tag.getCompound(key)) : ItemStack.EMPTY;
    }

    private static ItemStack createDisplayStack(AEItemKey key, long count) {
        ItemStack stack = key.toStack(1);
        stack.setCount((int) Math.max(1, Math.min(count, stack.getMaxStackSize())));
        return stack;
    }

    private static boolean canAdd(long first, long second) {
        return first >= 0 && second >= 0 && first <= Long.MAX_VALUE - second;
    }

    private record DeliveredInput(AEItemKey key, long count) {
    }

    private record RecipeMatch(CombinerRecipe recipe, DeliveredInput main, DeliveredInput extra) {
    }

    private record ReturnedInputs(long main, long extra) {
        private static final ReturnedInputs NONE = new ReturnedInputs(0, 0);
    }

    private static final class ProcessingJob {
        private final ItemStack patternDefinition;
        private final AEItemKey mainKey;
        private long mainCount;
        private final int mainPerOperation;
        private final AEItemKey extraKey;
        private long extraCount;
        private final int extraPerOperation;
        private long operations;

        private ProcessingJob(ItemStack patternDefinition, AEItemKey mainKey, long mainCount, int mainPerOperation,
                AEItemKey extraKey, long extraCount, int extraPerOperation, long operations) {
            this.patternDefinition = patternDefinition.copyWithCount(1);
            this.mainKey = mainKey;
            this.mainCount = Math.max(0, mainCount);
            this.mainPerOperation = Math.max(1, mainPerOperation);
            this.extraKey = extraKey;
            this.extraCount = Math.max(0, extraCount);
            this.extraPerOperation = Math.max(1, extraPerOperation);
            this.operations = Math.max(0, operations);
        }

        private boolean matches(ProcessingJob other) {
            return mainPerOperation == other.mainPerOperation && extraPerOperation == other.extraPerOperation
                    && mainKey.equals(other.mainKey) && extraKey.equals(other.extraKey)
                    && ItemStack.isSameItemSameComponents(patternDefinition, other.patternDefinition);
        }

        private boolean canGrow(ProcessingJob other) {
            return canAdd(mainCount, other.mainCount) && canAdd(extraCount, other.extraCount)
                    && canAdd(operations, other.operations);
        }

        private boolean isConsistent() {
            return !patternDefinition.isEmpty() && mainCount % mainPerOperation == 0
                    && extraCount % extraPerOperation == 0 && operations == mainCount / mainPerOperation
                    && operations == extraCount / extraPerOperation;
        }
    }
}
