package io.github.shenfnx.mekanismae.menu;

import appeng.api.crafting.PatternDetailsHelper;
import io.github.shenfnx.mekanismae.block.entity.AbstractSingleKeyMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModItems;
import mekanism.common.item.ItemTierInstaller;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Shared menu for item/chemical single-input, single-output machines. */
public abstract class AbstractSingleKeyMeMachineMenu extends AbstractContainerMenu
        implements MeProcessingMachineMenu {
    private static final int DATA_COUNT = 21;
    private static final int REAL_MACHINE_SLOT_COUNT = AbstractSingleKeyMeMachineBlockEntity.TIER_SLOT_INDEX + 1;
    private static final int DISPLAY_SLOT_COUNT = 2;
    private static final int MACHINE_MENU_SLOT_COUNT = REAL_MACHINE_SLOT_COUNT + DISPLAY_SLOT_COUNT;
    private final AbstractSingleKeyMeMachineBlockEntity machine;
    private final ContainerData data;
    private final SimpleContainer processingDisplay = new SimpleContainer(DISPLAY_SLOT_COUNT);

    protected AbstractSingleKeyMeMachineMenu(MenuType<?> menuType, int containerId, Inventory inventory,
            BlockPos pos, Class<? extends AbstractSingleKeyMeMachineBlockEntity> expectedType,
            String machineName) {
        super(menuType, containerId);
        if (!expectedType.isInstance(inventory.player.level().getBlockEntity(pos))) {
            throw new IllegalStateException("Missing " + machineName + " at " + pos);
        }
        machine = expectedType.cast(inventory.player.level().getBlockEntity(pos));
        for (int index = 0; index < AbstractSingleKeyMeMachineBlockEntity.PATTERN_SLOT_COUNT; index++) {
            int slotIndex = index;
            addSlot(new Slot(machine, slotIndex, 31 + index * 18, 29) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return PatternDetailsHelper.isEncodedPattern(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return machine.canTakePattern();
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int index = 0; index < AbstractSingleKeyMeMachineBlockEntity.UPGRADE_SLOT_COUNT; index++) {
            int slotIndex = AbstractSingleKeyMeMachineBlockEntity.PATTERN_SLOT_COUNT + index;
            int upgradeIndex = index;
            addSlot(new Slot(machine, slotIndex, 262, 9 + upgradeIndex * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return machine.canPlaceItem(slotIndex, stack);
                }

                @Override
                public int getMaxStackSize(ItemStack stack) {
                    return Math.min(super.getMaxStackSize(stack), machine.getUpgradeLimitForSlot(slotIndex, stack));
                }
            });
        }
        addSlot(new Slot(machine, AbstractSingleKeyMeMachineBlockEntity.TIER_SLOT_INDEX, 8, 29) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(AbstractSingleKeyMeMachineBlockEntity.TIER_SLOT_INDEX, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        refreshDisplay();
        addSlot(readOnlyDisplaySlot(0, 43, 73));
        addSlot(readOnlyDisplaySlot(1, 92, 73));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 32 + column * 18, 135 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 32 + column * 18, 193));
        }

        data = inventory.player.level().isClientSide()
                ? new SimpleContainerData(DATA_COUNT) : machine.getContainerData();
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

    private void refreshDisplay() {
        processingDisplay.setItem(0, machine.getProcessingInputDisplay());
        processingDisplay.setItem(1, machine.getProcessingOutputDisplay());
    }

    @Override
    public void broadcastChanges() {
        if (!machine.getLevel().isClientSide()) {
            refreshDisplay();
        }
        super.broadcastChanges();
    }

    @Override
    public AbstractSingleKeyMeMachineBlockEntity chamber() {
        return machine;
    }

    public int inputChemicalRegistryId() {
        return data.get(17);
    }

    public int inputChemicalAmount() {
        return Math.max(0, data.get(18));
    }

    public int outputChemicalRegistryId() {
        return data.get(19);
    }

    public int outputChemicalAmount() {
        return Math.max(0, data.get(20));
    }

    @Override
    public int energy() {
        return data.get(0);
    }

    @Override
    public int maxEnergy() {
        return Math.max(1, data.get(5));
    }

    @Override
    public int progress() {
        return data.get(1);
    }

    @Override
    public int pendingOperations() {
        return data.get(2);
    }

    @Override
    public boolean networkOnline() {
        return data.get(4) != 0;
    }

    @Override
    public boolean networkEnabled() {
        return data.get(6) != 0;
    }

    @Override
    public int processingTicks() {
        return Math.max(1, data.get(3));
    }

    @Override
    public int speedUpgrades() {
        return data.get(7);
    }

    @Override
    public int parallelUpgrades() {
        return data.get(8);
    }

    @Override
    public int energyUpgrades() {
        return data.get(9);
    }

    @Override
    public int maxEnergyReceive() {
        return data.get(10);
    }

    @Override
    public int bufferOps() {
        return data.get(11);
    }

    @Override
    public int bufferOpsCap() {
        return data.get(12);
    }

    @Override
    public int parallelMultiplier() {
        return data.get(13);
    }

    @Override
    public int pendingOutputDisplay() {
        return data.get(14);
    }

    @Override
    public boolean processingFaulted() {
        return data.get(15) != 0;
    }

    @Override
    public int speedMultiplier() {
        return data.get(16);
    }

    @Override
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
        return pendingOperations() > 0
                ? Component.translatable("gui.mekanismae.status.processing")
                : Component.translatable("gui.mekanismae.status.idle");
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0) {
            machine.returnAllResourcesToNetwork();
            return true;
        }
        if (id == 1) {
            machine.toggleNetworkEnabled();
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
            moved = moveItemStackTo(moving, 0, AbstractSingleKeyMeMachineBlockEntity.PATTERN_SLOT_COUNT, false);
        } else if (ModItems.isMachineUpgrade(moving)) {
            moved = moveItemStackTo(moving, AbstractSingleKeyMeMachineBlockEntity.PATTERN_SLOT_COUNT,
                    AbstractSingleKeyMeMachineBlockEntity.TIER_SLOT_INDEX, false);
        } else if (moving.getItem() instanceof ItemTierInstaller) {
            moved = moveItemStackTo(moving, AbstractSingleKeyMeMachineBlockEntity.TIER_SLOT_INDEX,
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
        return machine.stillValid(player);
    }
}
