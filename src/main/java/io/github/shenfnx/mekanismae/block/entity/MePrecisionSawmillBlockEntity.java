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
import io.github.shenfnx.mekanismae.menu.MePrecisionSawmillMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import io.github.shenfnx.mekanismae.registry.ModItems;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.SawmillRecipe;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * ME Precision Sawmill with deterministic expected-value secondary outputs.
 * Fractions are persisted per encoded pattern; two patterns never share a
 * remainder even when they use the same input and recipe.
 */
public final class MePrecisionSawmillBlockEntity extends AbstractMeProcessingBlockEntity {
    private static final int TASK_DATA_VERSION = 1;

    private AEItemKey activeInputKey;
    private long activeInputCount;
    private int inputPerOperation = 1;
    private long pendingOperations;
    private ItemStack activePattern = ItemStack.EMPTY;
    private AEItemKey activeMainOutputKey;
    private int mainOutputPerOperation;
    private AEItemKey activeSecondaryOutputKey;
    private int secondaryOutputCount;
    private long chanceNumerator;
    private long chanceDenominator = 1;
    private long activeFraction;
    private boolean secondaryDeclared;

    private AEItemKey pendingMainKey;
    private long pendingMainCount;
    private AEItemKey pendingSecondaryKey;
    private long pendingSecondaryCount;
    private final List<ProcessingJob> queuedJobs = new ArrayList<>();
    private final List<PatternRemainder> remainders = new ArrayList<>();
    private int progress;
    private boolean processingFaulted;

    public MePrecisionSawmillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_PRECISION_SAWMILL.get(), pos, state,
                ModBlocks.ME_PRECISION_SAWMILL_ITEM.get(), MachineType.ME_PRECISION_SAWMILL);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_precision_sawmill");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MePrecisionSawmillMenu(containerId, inventory, worldPosition);
    }

    @Override
    public ItemStack getProcessingInputDisplay() {
        activateNextJobIfIdle();
        return activeInputKey == null ? ItemStack.EMPTY
                : display(activeInputKey, Math.min(activeInputCount, inputPerOperation));
    }

    @Override
    public ItemStack getProcessingOutputDisplay() {
        activateNextJobIfIdle();
        if (pendingMainKey != null && pendingMainCount > 0) return display(pendingMainKey, pendingMainCount);
        return activeMainOutputKey == null ? ItemStack.EMPTY : activeMainOutputKey.toStack(mainOutputPerOperation);
    }

    @Override
    public ItemStack getProcessingSecondaryOutputDisplay() {
        activateNextJobIfIdle();
        if (pendingSecondaryKey != null && pendingSecondaryCount > 0) {
            return display(pendingSecondaryKey, pendingSecondaryCount);
        }
        return activeSecondaryOutputKey == null ? ItemStack.EMPTY
                : activeSecondaryOutputKey.toStack(secondaryOutputCount);
    }

    public long getBufferedOperationCount() { return getTotalQueuedOperations(); }
    public long getCurrentOperationCount() { return Math.max(0, pendingOperations); }
    public ItemStack getBufferedInputDisplay() {
        return activeInputKey == null ? ItemStack.EMPTY : activeInputKey.toStack(1);
    }
    public ItemStack getBufferedOutputDisplay() {
        if (pendingMainKey != null) return pendingMainKey.toStack(1);
        return pendingSecondaryKey == null ? ItemStack.EMPTY : pendingSecondaryKey.toStack(1);
    }
    public long getBufferedOutputCount() { return pendingMainCount + pendingSecondaryCount; }

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
                    case 14 -> (int) Math.min(Integer.MAX_VALUE, pendingMainCount + pendingSecondaryCount);
                    case 15 -> processingFaulted ? 1 : 0;
                    case 16 -> getSpeedMultiplier();
                    default -> 0;
                };
            }

            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 17; }
        };
    }

    @Override
    public boolean isProcessingFaulted() {
        return processingFaulted;
    }

    @Override
    public boolean pushPattern(IPatternDetails details, KeyCounter[] inputs) {
        if (level == null || level.isClientSide() || !networkEnabled || isBusy() || inputs.length != 1
                || !supportsInstalledPattern(details)) return false;
        DeliveredInput delivered = readInput(inputs[0]);
        if (delivered == null) return false;
        PatternMatch match = matchPattern(details, delivered.key, delivered.count);
        if (match == null) return false;
        long buffered = getTotalQueuedOperations();
        if (buffered > getBufferOperationLimit() || match.operations > getBufferOperationLimit() - buffered) {
            return false;
        }
        enqueue(new ProcessingJob(details.getDefinition().toStack(), delivered.key, delivered.count,
                match.inputPerOperation, match.operations, match.mainKey, match.mainCount,
                match.secondaryKey, match.secondaryCount, match.ratio.numerator, match.ratio.denominator,
                match.secondaryDeclared));
        setChanged();
        return true;
    }

    @Override
    public boolean isBusy() {
        return !networkEnabled || processingFaulted || getTotalQueuedOperations() >= getBufferOperationLimit();
    }

    @Override
    protected boolean isPatternForThisMachine(IPatternDetails details) {
        if (level == null || details.getInputs().length != 1) return false;
        for (GenericStack possible : details.getInputs()[0].getPossibleInputs()) {
            if (possible.what() instanceof AEItemKey key) {
                long amount = multiplyOrNegative(possible.amount(), details.getInputs()[0].getMultiplier());
                if (amount > 0 && matchPattern(details, key, amount) != null) return true;
            }
        }
        return false;
    }

    private boolean supportsInstalledPattern(IPatternDetails details) {
        if (!isPatternForThisMachine(details)) return false;
        for (ItemStack pattern : patternSlots) {
            IPatternDetails stored = pattern.isEmpty() ? null : PatternDetailsHelper.decodePattern(pattern, level);
            if (stored != null && stored.getDefinition().equals(details.getDefinition())) return true;
        }
        return false;
    }

    private PatternMatch matchPattern(IPatternDetails details, AEItemKey inputKey, long inputCount) {
        if (level == null || inputCount <= 0) return null;
        ItemStack probe = inputKey.toStack(1);
        SawmillRecipe recipe = MekanismRecipeType.SAWING.getInputCache().findFirstRecipe(level, probe);
        if (recipe == null) return null;
        long needed = recipe.getInput().getNeededAmount(probe);
        if (needed <= 0 || needed > Integer.MAX_VALUE || inputCount % needed != 0) return null;
        long operations = inputCount / needed;
        if (operations <= 0) return null;

        SawmillRecipe.ChanceOutput result = recipe.getOutput(inputKey.toStack((int) needed));
        ItemStack main = result.getMainOutput();
        ItemStack secondary = result.getMaxSecondaryOutput();
        ChanceRatio ratio = ratio(recipe.getSecondaryChance());
        if (ratio == null || main.isEmpty() && secondary.isEmpty()) return null;
        AEItemKey mainKey = main.isEmpty() ? null : AEItemKey.of(main);
        AEItemKey secondaryKey = secondary.isEmpty() ? null : AEItemKey.of(secondary);
        int mainCount = main.isEmpty() ? 0 : main.getCount();
        int secondaryCount = secondary.isEmpty() ? 0 : secondary.getCount();
        List<GenericStack> outputs = details.getOutputs();

        if (mainKey != null) {
            if (outputs.isEmpty() || outputs.size() > 2
                    || !matchesOutput(outputs.getFirst(), mainKey, multiplyOrNegative(operations, mainCount))) {
                return null;
            }
            boolean declared = outputs.size() == 2;
            if (declared) {
                long expected = expectedExact(operations, ratio, secondaryCount);
                if (secondaryKey == null || expected < 0 || !matchesOutput(outputs.get(1), secondaryKey, expected)) {
                    return null;
                }
            }
            return new PatternMatch((int) needed, operations, mainKey, mainCount, secondaryKey,
                    secondaryCount, ratio, declared);
        }

        // Pure chance-output recipes must be normalized to an integral expected batch.
        long expected = expectedExact(operations, ratio, secondaryCount);
        if (outputs.size() != 1 || secondaryKey == null || expected <= 0
                || !matchesOutput(outputs.getFirst(), secondaryKey, expected)) return null;
        return new PatternMatch((int) needed, operations, null, 0, secondaryKey,
                secondaryCount, ratio, true);
    }

    private static boolean matchesOutput(GenericStack declared, AEItemKey key, long expected) {
        return expected > 0 && declared.what() instanceof AEItemKey declaredKey
                && declaredKey.equals(key) && declared.amount() == expected;
    }

    private static ChanceRatio ratio(double chance) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) return null;
        BigDecimal decimal = BigDecimal.valueOf(chance).stripTrailingZeros();
        int scale = Math.max(0, decimal.scale());
        if (scale > 9) return null;
        long numerator = decimal.movePointRight(scale).longValueExact();
        long denominator = BigInteger.TEN.pow(scale).longValueExact();
        long gcd = gcd(numerator, denominator);
        return new ChanceRatio(numerator / gcd, denominator / gcd);
    }

    private static long gcd(long a, long b) {
        while (b != 0) {
            long next = a % b;
            a = b;
            b = next;
        }
        return Math.max(1, Math.abs(a));
    }

    private static long expectedExact(long operations, ChanceRatio ratio, int stackCount) {
        BigInteger scaled = BigInteger.valueOf(operations).multiply(BigInteger.valueOf(ratio.numerator))
                .multiply(BigInteger.valueOf(stackCount));
        BigInteger[] division = scaled.divideAndRemainder(BigInteger.valueOf(ratio.denominator));
        return division[1].signum() == 0 && division[0].bitLength() < 63 ? division[0].longValue() : -1;
    }

    private static DeliveredInput readInput(KeyCounter counter) {
        AEItemKey key = null;
        long count = 0;
        for (Object2LongMap.Entry<appeng.api.stacks.AEKey> entry : counter) {
            if (!(entry.getKey() instanceof AEItemKey itemKey) || entry.getLongValue() <= 0
                    || key != null && !key.equals(itemKey) || !canAdd(count, entry.getLongValue())) return null;
            key = itemKey;
            count += entry.getLongValue();
        }
        return key == null ? null : new DeliveredInput(key, count);
    }

    private void enqueue(ProcessingJob job) {
        if (activeInputKey == null && pendingOperations <= 0 && noPendingOutputs() && queuedJobs.isEmpty()) {
            activate(job);
            return;
        }
        if (matchesActive(job) && canAdd(activeInputCount, job.inputCount)
                && canAdd(pendingOperations, job.operations)) {
            activeInputCount += job.inputCount;
            pendingOperations += job.operations;
            return;
        }
        for (ProcessingJob queued : queuedJobs) {
            if (queued.matches(job) && queued.canGrow(job)) {
                queued.inputCount += job.inputCount;
                queued.operations += job.operations;
                return;
            }
        }
        queuedJobs.add(job);
    }

    private boolean matchesActive(ProcessingJob job) {
        return activeInputKey != null && activeInputKey.equals(job.inputKey)
                && inputPerOperation == job.inputPerOperation
                && mainOutputPerOperation == job.mainCount && secondaryOutputCount == job.secondaryCount
                && chanceNumerator == job.numerator && chanceDenominator == job.denominator
                && secondaryDeclared == job.secondaryDeclared
                && java.util.Objects.equals(activeMainOutputKey, job.mainKey)
                && java.util.Objects.equals(activeSecondaryOutputKey, job.secondaryKey)
                && ItemStack.isSameItemSameComponents(activePattern, job.pattern);
    }

    @Override
    public void tickServer() {
        if (level == null || level.isClientSide()) return;
        flushOutputs();
        finishActiveIfDrained();
        activateNextJobIfIdle();
        updateVisualState();
        if (processingFaulted || shouldPauseForRedstone() || !noPendingOutputs() || pendingOperations <= 0) return;
        if (activeInputKey == null || activeInputCount < inputPerOperation || !recipeStillMatches()) {
            fault();
            return;
        }
        int ticks = getProcessingTicks();
        progress = (int) Math.min(ticks, (long) progress + getSpeedMultiplier());
        updateVisualState();
        int energyPerOperation = getEnergyPerOperation();
        if (progress < ticks || energyStorage.getEnergyStored() < energyPerOperation) return;
        long operations = Math.min(getParallelBatchSize(), pendingOperations);
        operations = Math.min(operations, energyStorage.getEnergyStored() / energyPerOperation);
        operations = Math.min(operations, activeInputCount / inputPerOperation);
        if (operations <= 0) return;

        long mainProduced = multiplyOrNegative(operations, mainOutputPerOperation);
        SecondaryBatch secondary = calculateSecondary(operations);
        if (mainProduced < 0 || secondary == null
                || !canQueueOutput(activeMainOutputKey, mainProduced)
                || !canQueueOutput(activeSecondaryOutputKey, secondary.produced)) {
            fault();
            return;
        }
        energyStorage.consumeEnergy(getEnergyCostForOperations(operations));
        activeInputCount -= operations * inputPerOperation;
        pendingOperations -= operations;
        addPending(activeMainOutputKey, mainProduced, false);
        addPending(activeSecondaryOutputKey, secondary.produced, true);
        activeFraction = secondary.remainder;
        if (!secondaryDeclared) saveActiveRemainder();
        progress = 0;
        setChanged();
        flushOutputs();
        finishActiveIfDrained();
        activateNextJobIfIdle();
        updateVisualState();
    }

    private boolean recipeStillMatches() {
        SawmillRecipe recipe = MekanismRecipeType.SAWING.getInputCache().findFirstRecipe(level,
                activeInputKey.toStack(inputPerOperation));
        if (recipe == null) return false;
        SawmillRecipe.ChanceOutput output = recipe.getOutput(activeInputKey.toStack(inputPerOperation));
        ItemStack main = output.getMainOutput();
        ItemStack secondary = output.getMaxSecondaryOutput();
        ChanceRatio current = ratio(recipe.getSecondaryChance());
        return current != null && current.numerator == chanceNumerator && current.denominator == chanceDenominator
                && matchesKeyAndCount(activeMainOutputKey, main, mainOutputPerOperation)
                && matchesKeyAndCount(activeSecondaryOutputKey, secondary, secondaryOutputCount);
    }

    private static boolean matchesKeyAndCount(AEItemKey key, ItemStack stack, int count) {
        return key == null ? stack.isEmpty() : !stack.isEmpty() && key.matches(stack) && stack.getCount() == count;
    }

    private SecondaryBatch calculateSecondary(long operations) {
        BigInteger scaled = BigInteger.valueOf(operations).multiply(BigInteger.valueOf(chanceNumerator))
                .multiply(BigInteger.valueOf(secondaryOutputCount)).add(BigInteger.valueOf(activeFraction));
        BigInteger[] division = scaled.divideAndRemainder(BigInteger.valueOf(chanceDenominator));
        if (division[0].bitLength() >= 63 || division[1].bitLength() >= 63) return null;
        return new SecondaryBatch(division[0].longValue(), division[1].longValue());
    }

    private boolean canQueueOutput(AEItemKey key, long count) {
        if (key == null || count <= 0) return count == 0;
        if (pendingMainKey == null || pendingMainKey.equals(key)) return canAdd(pendingMainCount, count);
        return (pendingSecondaryKey == null || pendingSecondaryKey.equals(key))
                && canAdd(pendingSecondaryCount, count);
    }

    private void addPending(AEItemKey key, long count, boolean preferSecondary) {
        if (key == null || count <= 0) return;
        if (!preferSecondary || pendingMainKey != null && pendingMainKey.equals(key)) {
            if (pendingMainKey == null) pendingMainKey = key;
            pendingMainCount += count;
        } else {
            if (pendingSecondaryKey == null) pendingSecondaryKey = key;
            pendingSecondaryCount += count;
        }
    }

    private void flushOutputs() {
        if (!getMainNode().isOnline() || getMainNode().getGrid() == null) return;
        long first = flush(pendingMainKey, pendingMainCount);
        if (first > 0) {
            pendingMainCount -= first;
            if (pendingMainCount <= 0) { pendingMainCount = 0; pendingMainKey = null; }
        }
        long second = flush(pendingSecondaryKey, pendingSecondaryCount);
        if (second > 0) {
            pendingSecondaryCount -= second;
            if (pendingSecondaryCount <= 0) { pendingSecondaryCount = 0; pendingSecondaryKey = null; }
        }
        if (first > 0 || second > 0) setChanged();
    }

    private long flush(AEItemKey key, long count) {
        return key == null || count <= 0 ? 0 : getMainNode().getGrid().getStorageService().getInventory()
                .insert(key, count, Actionable.MODULATE, IActionSource.ofMachine(this));
    }

    private void activateNextJobIfIdle() {
        if (activeInputKey != null || pendingOperations > 0 || !noPendingOutputs() || queuedJobs.isEmpty()) return;
        activate(queuedJobs.removeFirst());
        setChanged();
    }

    private void activate(ProcessingJob job) {
        activePattern = job.pattern.copy();
        activeInputKey = job.inputKey;
        activeInputCount = job.inputCount;
        inputPerOperation = job.inputPerOperation;
        pendingOperations = job.operations;
        activeMainOutputKey = job.mainKey;
        mainOutputPerOperation = job.mainCount;
        activeSecondaryOutputKey = job.secondaryKey;
        secondaryOutputCount = job.secondaryCount;
        chanceNumerator = job.numerator;
        chanceDenominator = job.denominator;
        secondaryDeclared = job.secondaryDeclared;
        activeFraction = secondaryDeclared ? 0 : loadRemainder(job);
        progress = 0;
    }

    private long loadRemainder(ProcessingJob job) {
        for (PatternRemainder remainder : remainders) if (remainder.matches(job)) return remainder.remainder;
        return 0;
    }

    private void saveActiveRemainder() {
        for (PatternRemainder remainder : remainders) {
            if (remainder.matches(activePattern, activeSecondaryOutputKey, chanceNumerator, chanceDenominator)) {
                remainder.remainder = activeFraction;
                return;
            }
        }
        if (activeSecondaryOutputKey != null && activeFraction > 0) {
            remainders.add(new PatternRemainder(activePattern, activeSecondaryOutputKey,
                    chanceNumerator, chanceDenominator, activeFraction));
        }
    }

    @Override
    protected void onPatternRemoved(ItemStack pattern) {
        remainders.removeIf(remainder -> ItemStack.isSameItemSameComponents(remainder.pattern, pattern));
    }

    private void finishActiveIfDrained() {
        if (pendingOperations <= 0 && activeInputCount <= 0) activeInputKey = null;
        if (activeInputKey == null && noPendingOutputs()) clearActive();
        if (!hasProcessingWork()) processingFaulted = false;
    }

    private void clearActive() {
        if (secondaryDeclared && activeFraction != 0) processingFaulted = true;
        activePattern = ItemStack.EMPTY;
        activeInputKey = null;
        activeInputCount = 0;
        inputPerOperation = 1;
        pendingOperations = 0;
        activeMainOutputKey = null;
        mainOutputPerOperation = 0;
        activeSecondaryOutputKey = null;
        secondaryOutputCount = 0;
        chanceNumerator = 0;
        chanceDenominator = 1;
        activeFraction = 0;
        secondaryDeclared = false;
        progress = 0;
    }

    private void fault() {
        processingFaulted = true;
        progress = 0;
        updateVisualState();
        setChanged();
    }

    @Override
    protected boolean hasProcessingWork() {
        return getTotalQueuedOperations() > 0 || activeInputCount > 0 || !noPendingOutputs() || !queuedJobs.isEmpty();
    }

    @Override
    public boolean hasStoredContents() {
        // A fractional expectation is not a physical item and intentionally does not prevent breaking.
        return hasProcessingWork() || !isEmpty();
    }

    @Override
    protected boolean isVisuallyWorking() {
        return !processingFaulted && !shouldPauseForRedstone() && pendingOperations > 0
                && activeInputKey != null && activeInputCount >= inputPerOperation && noPendingOutputs()
                && energyStorage.getEnergyStored() >= getEnergyPerOperation();
    }

    @Override
    public boolean returnAllResourcesToNetwork() {
        if (level == null || level.isClientSide()) return false;
        networkEnabled = false;
        ICraftingProvider.requestUpdate(getMainNode());
        flushOutputs();
        if (!getMainNode().isOnline() || getMainNode().getGrid() == null) return false;
        if (activeInputKey != null) {
            long inserted = returnInput(activeInputKey, activeInputCount, inputPerOperation);
            activeInputCount -= inserted;
            pendingOperations = activeInputCount / inputPerOperation;
            if (activeInputCount <= 0) activeInputKey = null;
        }
        for (int index = queuedJobs.size() - 1; index >= 0; index--) {
            ProcessingJob job = queuedJobs.get(index);
            long inserted = returnInput(job.inputKey, job.inputCount, job.inputPerOperation);
            job.inputCount -= inserted;
            job.operations = job.inputCount / job.inputPerOperation;
            if (job.inputCount <= 0) queuedJobs.remove(index);
        }
        finishActiveIfDrained();
        setChanged();
        return !hasProcessingWork();
    }

    private long returnInput(AEItemKey key, long count, int unit) {
        var inventory = getMainNode().getGrid().getStorageService().getInventory();
        long accepted = inventory.insert(key, count, Actionable.SIMULATE, IActionSource.ofMachine(this));
        long whole = Math.min(count, Math.max(0, accepted));
        whole -= whole % unit;
        return whole <= 0 ? 0 : inventory.insert(key, whole, Actionable.MODULATE, IActionSource.ofMachine(this));
    }

    private long getTotalQueuedOperations() {
        long total = Math.max(0, pendingOperations);
        for (ProcessingJob job : queuedJobs) {
            if (!canAdd(total, job.operations)) return Long.MAX_VALUE;
            total += job.operations;
        }
        return total;
    }

    private boolean noPendingOutputs() {
        return pendingMainCount <= 0 && pendingSecondaryCount <= 0;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("SawmillTaskDataVersion", TASK_DATA_VERSION);
        if (activeInputKey != null) saveJob(tag, registries, "Active", activePattern, activeInputKey,
                activeInputCount, inputPerOperation, pendingOperations, activeMainOutputKey, mainOutputPerOperation,
                activeSecondaryOutputKey, secondaryOutputCount, chanceNumerator, chanceDenominator,
                secondaryDeclared, activeFraction);
        savePending(tag, registries, "PendingMain", pendingMainKey, pendingMainCount);
        savePending(tag, registries, "PendingSecondary", pendingSecondaryKey, pendingSecondaryCount);
        var queue = new net.minecraft.nbt.ListTag();
        for (ProcessingJob job : queuedJobs) {
            CompoundTag saved = new CompoundTag();
            saveJob(saved, registries, "", job.pattern, job.inputKey, job.inputCount,
                    job.inputPerOperation, job.operations, job.mainKey, job.mainCount,
                    job.secondaryKey, job.secondaryCount, job.numerator, job.denominator,
                    job.secondaryDeclared, 0);
            queue.add(saved);
        }
        tag.put("SawmillQueue", queue);
        var savedRemainders = new net.minecraft.nbt.ListTag();
        for (PatternRemainder remainder : remainders) {
            if (remainder.remainder <= 0) continue;
            CompoundTag saved = new CompoundTag();
            saved.put("Pattern", remainder.pattern.save(registries));
            saved.put("Secondary", remainder.secondaryKey.toStack(1).save(registries));
            saved.putLong("Numerator", remainder.numerator);
            saved.putLong("Denominator", remainder.denominator);
            saved.putLong("Remainder", remainder.remainder);
            savedRemainders.add(saved);
        }
        tag.put("SawmillRemainders", savedRemainders);
        tag.putInt("SawmillProgress", progress);
        tag.putBoolean("SawmillFaulted", processingFaulted);
    }

    private static void savePending(CompoundTag tag, HolderLookup.Provider registries,
            String prefix, AEItemKey key, long count) {
        if (key != null && count > 0) {
            tag.put(prefix, key.toStack(1).save(registries));
            tag.putLong(prefix + "Count", count);
        }
    }

    private static void saveJob(CompoundTag tag, HolderLookup.Provider registries, String prefix,
            ItemStack pattern, AEItemKey input, long inputCount, int inputUnit, long operations,
            AEItemKey main, int mainCount, AEItemKey secondary, int secondaryCount,
            long numerator, long denominator, boolean declared, long fraction) {
        tag.put(prefix + "Pattern", pattern.save(registries));
        tag.put(prefix + "Input", input.toStack(1).save(registries));
        tag.putLong(prefix + "InputCount", inputCount);
        tag.putInt(prefix + "InputUnit", inputUnit);
        tag.putLong(prefix + "Operations", operations);
        if (main != null) tag.put(prefix + "Main", main.toStack(1).save(registries));
        tag.putInt(prefix + "MainCount", mainCount);
        if (secondary != null) tag.put(prefix + "Secondary", secondary.toStack(1).save(registries));
        tag.putInt(prefix + "SecondaryCount", secondaryCount);
        tag.putLong(prefix + "Numerator", numerator);
        tag.putLong(prefix + "Denominator", denominator);
        tag.putBoolean(prefix + "SecondaryDeclared", declared);
        tag.putLong(prefix + "Fraction", fraction);
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        clearActive();
        queuedJobs.clear();
        remainders.clear();
        ProcessingJob active = readJob(tag, registries, "Active");
        if (active != null) {
            activate(active);
            activeFraction = Math.max(0, tag.getLong("ActiveFraction"));
        }
        ItemStack main = readStack(tag, "PendingMain", registries);
        pendingMainKey = main.isEmpty() ? null : AEItemKey.of(main);
        pendingMainCount = pendingMainKey == null ? 0 : Math.max(0, tag.getLong("PendingMainCount"));
        ItemStack secondary = readStack(tag, "PendingSecondary", registries);
        pendingSecondaryKey = secondary.isEmpty() ? null : AEItemKey.of(secondary);
        pendingSecondaryCount = pendingSecondaryKey == null ? 0 : Math.max(0, tag.getLong("PendingSecondaryCount"));
        var queue = tag.getList("SawmillQueue", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < queue.size(); index++) {
            ProcessingJob job = readJob(queue.getCompound(index), registries, "");
            if (job != null) queuedJobs.add(job);
        }
        var savedRemainders = tag.getList("SawmillRemainders", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int index = 0; index < savedRemainders.size(); index++) {
            CompoundTag saved = savedRemainders.getCompound(index);
            ItemStack pattern = readStack(saved, "Pattern", registries);
            ItemStack savedSecondary = readStack(saved, "Secondary", registries);
            long denominator = saved.getLong("Denominator");
            long remainder = saved.getLong("Remainder");
            if (!pattern.isEmpty() && !savedSecondary.isEmpty() && denominator > 0
                    && remainder > 0 && remainder < denominator) {
                remainders.add(new PatternRemainder(pattern, AEItemKey.of(savedSecondary),
                        saved.getLong("Numerator"), denominator, remainder));
            }
        }
        progress = Math.max(0, tag.getInt("SawmillProgress"));
        processingFaulted = tag.getBoolean("SawmillFaulted") || !loadedStateConsistent();
    }

    private static ProcessingJob readJob(CompoundTag tag, HolderLookup.Provider registries, String prefix) {
        ItemStack pattern = readStack(tag, prefix + "Pattern", registries);
        ItemStack input = readStack(tag, prefix + "Input", registries);
        if (pattern.isEmpty() || input.isEmpty()) return null;
        ItemStack main = readStack(tag, prefix + "Main", registries);
        ItemStack secondary = readStack(tag, prefix + "Secondary", registries);
        return new ProcessingJob(pattern, AEItemKey.of(input), Math.max(0, tag.getLong(prefix + "InputCount")),
                Math.max(1, tag.getInt(prefix + "InputUnit")), Math.max(0, tag.getLong(prefix + "Operations")),
                main.isEmpty() ? null : AEItemKey.of(main), Math.max(0, tag.getInt(prefix + "MainCount")),
                secondary.isEmpty() ? null : AEItemKey.of(secondary),
                Math.max(0, tag.getInt(prefix + "SecondaryCount")),
                Math.max(0, tag.getLong(prefix + "Numerator")),
                Math.max(1, tag.getLong(prefix + "Denominator")),
                tag.getBoolean(prefix + "SecondaryDeclared"));
    }

    private boolean loadedStateConsistent() {
        if (activeInputKey != null && (activePattern.isEmpty() || activeInputCount % inputPerOperation != 0
                || pendingOperations != activeInputCount / inputPerOperation
                || activeFraction < 0 || activeFraction >= chanceDenominator)) return false;
        for (ProcessingJob job : queuedJobs) if (!job.isConsistent()) return false;
        return true;
    }

    private static ItemStack readStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        return tag.contains(key) ? ItemStack.parseOptional(registries, tag.getCompound(key)) : ItemStack.EMPTY;
    }

    private static ItemStack display(AEItemKey key, long count) {
        ItemStack stack = key.toStack(1);
        stack.setCount((int) Math.max(1, Math.min(count, stack.getMaxStackSize())));
        return stack;
    }

    private static long multiplyOrNegative(long first, long second) {
        if (first < 0 || second < 0 || first != 0 && second > Long.MAX_VALUE / first) return -1;
        return first * second;
    }

    private static boolean canAdd(long first, long second) {
        return first >= 0 && second >= 0 && first <= Long.MAX_VALUE - second;
    }

    private record DeliveredInput(AEItemKey key, long count) { }
    private record ChanceRatio(long numerator, long denominator) { }
    private record PatternMatch(int inputPerOperation, long operations, AEItemKey mainKey, int mainCount,
            AEItemKey secondaryKey, int secondaryCount, ChanceRatio ratio, boolean secondaryDeclared) { }
    private record SecondaryBatch(long produced, long remainder) { }

    private static final class ProcessingJob {
        private final ItemStack pattern;
        private final AEItemKey inputKey;
        private long inputCount;
        private final int inputPerOperation;
        private long operations;
        private final AEItemKey mainKey;
        private final int mainCount;
        private final AEItemKey secondaryKey;
        private final int secondaryCount;
        private final long numerator;
        private final long denominator;
        private final boolean secondaryDeclared;

        private ProcessingJob(ItemStack pattern, AEItemKey inputKey, long inputCount, int inputPerOperation,
                long operations, AEItemKey mainKey, int mainCount, AEItemKey secondaryKey,
                int secondaryCount, long numerator, long denominator, boolean secondaryDeclared) {
            this.pattern = pattern.copyWithCount(1);
            this.inputKey = inputKey;
            this.inputCount = inputCount;
            this.inputPerOperation = inputPerOperation;
            this.operations = operations;
            this.mainKey = mainKey;
            this.mainCount = mainCount;
            this.secondaryKey = secondaryKey;
            this.secondaryCount = secondaryCount;
            this.numerator = numerator;
            this.denominator = denominator;
            this.secondaryDeclared = secondaryDeclared;
        }

        private boolean matches(ProcessingJob other) {
            return inputKey.equals(other.inputKey) && inputPerOperation == other.inputPerOperation
                    && java.util.Objects.equals(mainKey, other.mainKey) && mainCount == other.mainCount
                    && java.util.Objects.equals(secondaryKey, other.secondaryKey)
                    && secondaryCount == other.secondaryCount && numerator == other.numerator
                    && denominator == other.denominator && secondaryDeclared == other.secondaryDeclared
                    && ItemStack.isSameItemSameComponents(pattern, other.pattern);
        }

        private boolean canGrow(ProcessingJob other) {
            return canAdd(inputCount, other.inputCount) && canAdd(operations, other.operations);
        }

        private boolean isConsistent() {
            return !pattern.isEmpty() && inputCount >= 0 && inputCount % inputPerOperation == 0
                    && operations == inputCount / inputPerOperation && denominator > 0;
        }
    }

    private static final class PatternRemainder {
        private final ItemStack pattern;
        private final AEItemKey secondaryKey;
        private final long numerator;
        private final long denominator;
        private long remainder;

        private PatternRemainder(ItemStack pattern, AEItemKey secondaryKey, long numerator,
                long denominator, long remainder) {
            this.pattern = pattern.copyWithCount(1);
            this.secondaryKey = secondaryKey;
            this.numerator = numerator;
            this.denominator = denominator;
            this.remainder = remainder;
        }

        private boolean matches(ProcessingJob job) {
            return matches(job.pattern, job.secondaryKey, job.numerator, job.denominator);
        }

        private boolean matches(ItemStack otherPattern, AEItemKey otherSecondary, long otherNumerator,
                long otherDenominator) {
            return secondaryKey.equals(otherSecondary) && numerator == otherNumerator
                    && denominator == otherDenominator
                    && ItemStack.isSameItemSameComponents(pattern, otherPattern);
        }
    }
}
