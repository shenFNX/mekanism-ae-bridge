package io.github.shenfnx.mekanismae.menu;

import appeng.api.crafting.PatternDetailsHelper;
import io.github.shenfnx.mekanismae.block.entity.AbstractItemChemicalToItemMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import mekanism.common.item.ItemTierInstaller;

public abstract class AbstractItemChemicalToItemMeMachineMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 19;
    private static final int REAL_MACHINE_SLOT_COUNT = AbstractItemChemicalToItemMeMachineBlockEntity.TIER_SLOT_INDEX + 1;
    private static final int DISPLAY_SLOT_COUNT = 2;
    private static final int MACHINE_MENU_SLOT_COUNT = REAL_MACHINE_SLOT_COUNT + DISPLAY_SLOT_COUNT;
    private final AbstractItemChemicalToItemMeMachineBlockEntity machine;
    private final ContainerData data;
    private final SimpleContainer processingDisplay = new SimpleContainer(DISPLAY_SLOT_COUNT);

    protected AbstractItemChemicalToItemMeMachineMenu(MenuType<?> menuType, int containerId, Inventory inventory,
            BlockPos pos, Class<? extends AbstractItemChemicalToItemMeMachineBlockEntity> expectedType,
            String machineName) {
        super(menuType, containerId);
        if (!expectedType.isInstance(inventory.player.level().getBlockEntity(pos))) {
            throw new IllegalStateException("Missing " + machineName + " at " + pos);
        }
        machine = expectedType.cast(inventory.player.level().getBlockEntity(pos));
        for (int index = 0; index < AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT; index++) {
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
        for (int index = 0; index < AbstractItemChemicalToItemMeMachineBlockEntity.UPGRADE_SLOT_COUNT; index++) {
            int slotIndex = AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT + index;
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
        addSlot(new Slot(machine, AbstractItemChemicalToItemMeMachineBlockEntity.TIER_SLOT_INDEX, 8, 29) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(AbstractItemChemicalToItemMeMachineBlockEntity.TIER_SLOT_INDEX, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        processingDisplay.setItem(0, machine.getProcessingInputDisplay());
        processingDisplay.setItem(1, machine.getProcessingOutputDisplay());
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
                ? new SimpleContainerData(DATA_COUNT)
                : machine.getContainerData();
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
        if (!machine.getLevel().isClientSide()) {
            processingDisplay.setItem(0, machine.getProcessingInputDisplay());
            processingDisplay.setItem(1, machine.getProcessingOutputDisplay());
        }
        super.broadcastChanges();
    }

    public AbstractItemChemicalToItemMeMachineBlockEntity machine() {
        return machine;
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

    public int speedMultiplier() {
        return data.get(18);
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
            moved = moveItemStackTo(moving, 0, AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT, false);
        } else if (moving.is(ModItems.SPEED_CARD.get()) || moving.is(ModItems.PARALLEL_CARD.get())
                || moving.is(ModItems.ENERGY_CARD.get())) {
            moved = moveItemStackTo(moving, AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT,
                    AbstractItemChemicalToItemMeMachineBlockEntity.TIER_SLOT_INDEX, false);
        } else if (moving.getItem() instanceof ItemTierInstaller) {
            moved = moveItemStackTo(moving, AbstractItemChemicalToItemMeMachineBlockEntity.TIER_SLOT_INDEX,
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
