package io.github.shenfnx.mekanismae.menu;

import appeng.api.crafting.PatternDetailsHelper;
import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModItems;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

public final class MeMetallurgicInfuserMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 18;
    private static final int REAL_MACHINE_SLOT_COUNT = MeMetallurgicInfuserBlockEntity.PATTERN_SLOT_COUNT
            + MeMetallurgicInfuserBlockEntity.UPGRADE_SLOT_COUNT;
    private static final int DISPLAY_SLOT_COUNT = 2;
    private static final int MACHINE_MENU_SLOT_COUNT = REAL_MACHINE_SLOT_COUNT + DISPLAY_SLOT_COUNT;
    private final MeMetallurgicInfuserBlockEntity infuser;
    private final ContainerData data;
    private final SimpleContainer processingDisplay = new SimpleContainer(DISPLAY_SLOT_COUNT);

    public MeMetallurgicInfuserMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeMetallurgicInfuserMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_METALLURGIC_INFUSER.get(), containerId);
        if (!(inventory.player.level().getBlockEntity(pos) instanceof MeMetallurgicInfuserBlockEntity found)) {
            throw new IllegalStateException("Missing ME metallurgic infuser at " + pos);
        }
        infuser = found;
        for (int index = 0; index < MeMetallurgicInfuserBlockEntity.PATTERN_SLOT_COUNT; index++) {
            int slotIndex = index;
            addSlot(new Slot(infuser, slotIndex, 31 + index * 18, 29) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PatternDetailsHelper.isEncodedPattern(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return infuser.canTakePattern();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int index = 0; index < MeMetallurgicInfuserBlockEntity.UPGRADE_SLOT_COUNT; index++) {
            int slotIndex = MeMetallurgicInfuserBlockEntity.PATTERN_SLOT_COUNT + index;
            int upgradeIndex = index;
            addSlot(new Slot(infuser, slotIndex, 263 + (upgradeIndex % 2) * 18, 33 + (upgradeIndex / 2) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return infuser.canPlaceItem(slotIndex, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return Math.min(super.getMaxStackSize(stack), infuser.getUpgradeLimitForSlot(slotIndex, stack));
                }
            });
        }

        processingDisplay.setItem(0, infuser.getProcessingInputDisplay());
        processingDisplay.setItem(1, infuser.getProcessingOutputDisplay());
        addSlot(readOnlyDisplaySlot(0, 43, 70));
        addSlot(readOnlyDisplaySlot(1, 92, 70));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 32 + column * 18, 117 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 32 + column * 18, 175));
        }

        data = inventory.player.level().isClientSide()
                ? new SimpleContainerData(DATA_COUNT)
                : infuser.getContainerData();
        addDataSlots(data);
    }

    private Slot readOnlyDisplaySlot(int index, int x, int y) {
        return new Slot(processingDisplay, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        };
    }

    @Override
    public void broadcastChanges() {
        if (!infuser.getLevel().isClientSide()) {
            processingDisplay.setItem(0, infuser.getProcessingInputDisplay());
            processingDisplay.setItem(1, infuser.getProcessingOutputDisplay());
        }
        super.broadcastChanges();
    }

    public MeMetallurgicInfuserBlockEntity infuser() {
        return infuser;
    }

    public int energy() {
        return data.get(0);
    }

    public int maxEnergy() {
        return Math.max(1, data.get(5));
    }

    public int progress() {
        return data.get(1);
    }

    public int pendingOperations() {
        return data.get(2);
    }

    public boolean networkOnline() {
        return data.get(4) != 0;
    }

    public boolean networkEnabled() {
        return data.get(6) != 0;
    }

    public int processingTicks() {
        return Math.max(1, data.get(3));
    }

    public int speedUpgrades() {
        return data.get(7);
    }

    public int parallelUpgrades() {
        return data.get(8);
    }

    public int energyUpgrades() {
        return data.get(9);
    }

    public int maxEnergyReceive() {
        return data.get(10);
    }

    public int bufferOps() {
        return data.get(11);
    }

    public int bufferOpsCap() {
        return data.get(12);
    }

    public int parallelMultiplier() {
        return data.get(13);
    }

    public int pendingOutputDisplay() {
        return data.get(14);
    }

    public boolean processingFaulted() {
        return data.get(15) != 0;
    }

    public int chemicalRegistryId() {
        return data.get(16);
    }

    public int chemicalAmount() {
        return Math.max(0, data.get(17));
    }

    public Component statusText() {
        if (processingFaulted()) {
            return Component.translatable("gui.mekanismae.status.faulted");
        }
        if (!networkEnabled()) {
            return Component.translatable("gui.mekanismae.status.disabled");
        }
        if (!networkOnline()) {
            return Component.translatable("gui.mekanismae.status.offline");
        }
        if (pendingOperations() > 0) {
            return Component.translatable("gui.mekanismae.status.processing");
        }
        return Component.translatable("gui.mekanismae.status.idle");
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            infuser.returnAllResourcesToNetwork();
            return true;
        }
        if (id == 1) {
            infuser.toggleNetworkEnabled();
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < MACHINE_MENU_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        Slot source = slots.get(index);
        if (!source.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack original = source.getItem().copy();
        ItemStack moving = source.getItem();
        boolean moved = false;
        if (PatternDetailsHelper.isEncodedPattern(moving)) {
            moved = moveItemStackTo(moving, 0, MeMetallurgicInfuserBlockEntity.PATTERN_SLOT_COUNT, false);
        } else if (moving.is(ModItems.SPEED_CARD.get()) || moving.is(ModItems.PARALLEL_CARD.get())
                || moving.is(ModItems.ENERGY_CARD.get())) {
            moved = moveItemStackTo(moving, MeMetallurgicInfuserBlockEntity.PATTERN_SLOT_COUNT,
                    REAL_MACHINE_SLOT_COUNT, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) {
            source.set(ItemStack.EMPTY);
        } else {
            source.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return infuser.stillValid(player);
    }
}
