package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import io.github.shenfnx.mekanismae.block.AbstractMeMachineBlock;
import io.github.shenfnx.mekanismae.config.MachineSettings;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.config.MekanismAeConfig;
import io.github.shenfnx.mekanismae.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.integration.energy.forgeenergy.ForgeStrictEnergyHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Shared AE node, energy, pattern-slot, and upgrade-slot infrastructure for all
 * single-block ME processing machines. Recipe parsing and task ledgers remain in
 * specialized subclasses so item-only and chemical machines cannot accidentally
 * share resources.
 */
public abstract class AbstractMeProcessingBlockEntity extends AENetworkedBlockEntity
        implements ICraftingProvider, Container, MenuProvider {
    public static final int PATTERN_SLOT_COUNT = 9;
    public static final int UPGRADE_SLOT_COUNT = 8;
    public static final int MAX_UPGRADES_PER_TYPE = 8;

    protected final MachineSettings settings;
    protected final MachineEnergyStorage energyStorage;
    private final IStrictEnergyHandler strictEnergyHandler;
    protected final NonNullList<ItemStack> patternSlots =
            NonNullList.withSize(PATTERN_SLOT_COUNT, ItemStack.EMPTY);
    protected final NonNullList<ItemStack> upgradeSlots =
            NonNullList.withSize(UPGRADE_SLOT_COUNT, ItemStack.EMPTY);
    protected int speedUpgrades;
    protected int parallelUpgrades;
    protected boolean networkEnabled = true;

    protected AbstractMeProcessingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
            ItemLike visualRepresentation, MachineType machineType) {
        super(type, pos, state);
        settings = MekanismAeConfig.settings(machineType);
        energyStorage = new MachineEnergyStorage(settings.baseEnergyCapacity(), settings.baseEnergyReceive());
        strictEnergyHandler = new ForgeStrictEnergyHandler(energyStorage);
        var mainNode = getMainNode()
                .setIdlePowerUsage(settings.idleAePower())
                .setVisualRepresentation(visualRepresentation)
                .addService(ICraftingProvider.class, this);
        if (settings.requireChannel()) {
            mainNode.setFlags(GridFlags.REQUIRE_CHANNEL);
        }
    }

    @Override
    public abstract Component getDisplayName();

    @Override
    public final void onMainNodeStateChanged(IGridNodeListener.State state) {
        ICraftingProvider.requestUpdate(getMainNode());
        updateVisualState();
        setChanged();
    }

    public final IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public final IStrictEnergyHandler getStrictEnergyHandler() {
        return strictEnergyHandler;
    }

    public final boolean setPattern(ItemStack stack) {
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

    public final ItemStack takePattern() {
        if (hasProcessingWork()) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < PATTERN_SLOT_COUNT; slot++) {
            if (!patternSlots.get(slot).isEmpty()) {
                ItemStack result = patternSlots.get(slot);
                patternSlots.set(slot, ItemStack.EMPTY);
                onPatternRemoved(result);
                ICraftingProvider.requestUpdate(getMainNode());
                setChanged();
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public final boolean canTakePattern() {
        return !hasProcessingWork();
    }

    @Override
    public final boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    public final boolean isNetworkEnabled() {
        return networkEnabled;
    }

    /** Read-only overlay status; never advances the task queue. */
    public final boolean isNetworkOnline() {
        return getMainNode().isOnline();
    }

    public abstract boolean isProcessingFaulted();

    /** Server-owned task tick used by the common machine block ticker. */
    public abstract void tickServer();

    /** True when breaking the block would discard patterns, upgrades, inputs, or outputs. */
    public abstract boolean hasStoredContents();

    public abstract ContainerData getContainerData();

    public abstract ItemStack getProcessingInputDisplay();

    public ItemStack getProcessingSecondaryInputDisplay() {
        return ItemStack.EMPTY;
    }

    public abstract ItemStack getProcessingOutputDisplay();

    public ItemStack getProcessingSecondaryOutputDisplay() {
        return ItemStack.EMPTY;
    }

    public abstract boolean returnAllResourcesToNetwork();

    /** Common Jade/debug view of the task ledger without exposing mutable storage. */
    public abstract long getBufferedOperationCount();

    public abstract long getCurrentOperationCount();

    public abstract ItemStack getBufferedInputDisplay();

    public abstract ItemStack getBufferedOutputDisplay();

    public abstract long getBufferedOutputCount();

    public final long getBufferOperationLimit() {
        return settings.maxBufferedOperations();
    }

    public final int getParallelMultiplier() {
        return settings.parallelMultiplier(parallelUpgrades);
    }

    public final int getSpeedMultiplier() {
        return settings.speedMultiplier(speedUpgrades);
    }

    public final void toggleNetworkEnabled() {
        if (!networkEnabled && isProcessingFaulted() && hasProcessingWork()) {
            return;
        }
        networkEnabled = !networkEnabled;
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
    }

    @Override
    public final List<IPatternDetails> getAvailablePatterns() {
        if (level == null || !networkEnabled) {
            return List.of();
        }
        List<IPatternDetails> result = new ArrayList<>(PATTERN_SLOT_COUNT);
        for (ItemStack pattern : patternSlots) {
            if (!pattern.isEmpty()) {
                IPatternDetails details = PatternDetailsHelper.decodePattern(pattern, level);
                if (details != null && isPatternForThisMachine(details)) {
                    result.add(details);
                }
            }
        }
        return result;
    }

    protected abstract boolean isPatternForThisMachine(IPatternDetails details);

    protected abstract boolean hasProcessingWork();

    /** Synchronizes the shared online/working model state for every ME machine family. */
    protected final void updateVisualState() {
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(AbstractMeMachineBlock.ONLINE)
                || !state.hasProperty(AbstractMeMachineBlock.WORKING)) {
            return;
        }
        boolean online = isNetworkOnline();
        boolean working = isVisuallyWorking();
        if (state.getValue(AbstractMeMachineBlock.ONLINE) != online
                || state.getValue(AbstractMeMachineBlock.WORKING) != working) {
            level.setBlock(worldPosition, state
                    .setValue(AbstractMeMachineBlock.ONLINE, online)
                    .setValue(AbstractMeMachineBlock.WORKING, working), 3);
        }
    }

    /** True only when this machine family can genuinely advance its active recipe. */
    protected abstract boolean isVisuallyWorking();

    protected final int getParallelBatchSize() {
        return getParallelMultiplier();
    }

    protected final int getProcessingTicks() {
        return settings.processingTicks();
    }

    protected final int getEnergyPerOperation() {
        return settings.energyPerOperation();
    }

    protected final boolean shouldPauseForRedstone() {
        return settings.redstonePausesProcessing() && level != null && level.hasNeighborSignal(worldPosition);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        var patterns = new net.minecraft.nbt.ListTag();
        for (int index = 0; index < patternSlots.size(); index++) {
            ItemStack stack = patternSlots.get(index);
            if (!stack.isEmpty()) {
                CompoundTag pattern = (CompoundTag) stack.save(registries);
                pattern.putByte("Slot", (byte) index);
                patterns.add(pattern);
            }
        }
        tag.put("Patterns", patterns);
        tag.putInt("Energy", energyStorage.getEnergyStored());
        tag.putBoolean("NetworkEnabled", networkEnabled);

        var upgrades = new net.minecraft.nbt.ListTag();
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
        energyStorage.loadEnergy(tag.getInt("Energy"));
    }

    @Override
    public final int getContainerSize() {
        return PATTERN_SLOT_COUNT + UPGRADE_SLOT_COUNT;
    }

    @Override
    public final boolean isEmpty() {
        if (patternSlots.stream().anyMatch(stack -> !stack.isEmpty())) {
            return false;
        }
        return upgradeSlots.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public final ItemStack getItem(int slot) {
        return slot < PATTERN_SLOT_COUNT ? patternSlots.get(slot) : upgradeSlots.get(slot - PATTERN_SLOT_COUNT);
    }

    @Override
    public final ItemStack removeItem(int slot, int amount) {
        if (slot < PATTERN_SLOT_COUNT && !canTakePattern()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getItem(slot);
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = current.split(amount);
        if (slot < PATTERN_SLOT_COUNT && !result.isEmpty()) {
            onPatternRemoved(result);
        }
        if (current.isEmpty()) {
            setItem(slot, ItemStack.EMPTY);
        } else {
            setChanged();
        }
        return result;
    }

    @Override
    public final ItemStack removeItemNoUpdate(int slot) {
        if (slot < PATTERN_SLOT_COUNT && !canTakePattern()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = getItem(slot);
        if (slot < PATTERN_SLOT_COUNT) {
            patternSlots.set(slot, ItemStack.EMPTY);
            if (!result.isEmpty()) {
                onPatternRemoved(result);
            }
            ICraftingProvider.requestUpdate(getMainNode());
        } else {
            upgradeSlots.set(slot - PATTERN_SLOT_COUNT, ItemStack.EMPTY);
            recalculateUpgrades();
        }
        setChanged();
        return result;
    }

    @Override
    public final void setItem(int slot, ItemStack stack) {
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
        speedUpgrades = getUpgradeCount(ModItems.SPEED_CARD.get());
        parallelUpgrades = getUpgradeCount(ModItems.PARALLEL_CARD.get());
        energyStorage.updateUpgrades(getUpgradeCount(ModItems.ENERGY_CARD.get()));
    }

    public final int getUpgradeCount(Item item) {
        return upgradeSlots.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    public final int getUpgradeLimitForSlot(int slot, ItemStack stack) {
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
    public final void clearContent() {
        for (ItemStack pattern : patternSlots) {
            if (!pattern.isEmpty()) {
                onPatternRemoved(pattern);
            }
        }
        patternSlots.replaceAll(stack -> ItemStack.EMPTY);
        upgradeSlots.replaceAll(stack -> ItemStack.EMPTY);
        recalculateUpgrades();
        ICraftingProvider.requestUpdate(getMainNode());
        setChanged();
    }

    @Override
    public final boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < PATTERN_SLOT_COUNT ? PatternDetailsHelper.isEncodedPattern(stack)
                : isSupportedUpgrade(stack) && getUpgradeLimitForSlot(slot, stack) > 0;
    }

    protected final class MachineEnergyStorage extends EnergyStorage {
        private MachineEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }

        protected void loadEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        private void updateUpgrades(int upgrades) {
            capacity = settings.energyCapacity(upgrades);
            maxReceive = settings.energyReceive(upgrades);
            energy = Math.min(energy, capacity);
        }

        protected int consumeEnergy(int amount) {
            int consumed = Math.min(energy, Math.max(0, amount));
            if (consumed > 0) {
                energy -= consumed;
                setChanged();
            }
            return consumed;
        }

        protected int getReceiveLimit() {
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

    /** Allows specialized ledgers to discard non-physical metadata for a deliberately removed pattern. */
    protected void onPatternRemoved(ItemStack pattern) {
    }

}
