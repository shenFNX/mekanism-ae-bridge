package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalCrystallizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalCrystallizerMenu extends AbstractSingleKeyMeMachineMenu {
    public MeChemicalCrystallizerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalCrystallizerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_CRYSTALLIZER.get(), containerId, inventory, pos,
                MeChemicalCrystallizerBlockEntity.class, "ME chemical crystallizer");
    }
}
