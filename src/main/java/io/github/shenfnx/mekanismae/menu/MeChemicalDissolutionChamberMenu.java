package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalDissolutionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalDissolutionChamberMenu extends AbstractDualKeyMeMachineMenu {
    public MeChemicalDissolutionChamberMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalDissolutionChamberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_DISSOLUTION_CHAMBER.get(), containerId, inventory, pos,
                MeChemicalDissolutionChamberBlockEntity.class, "ME chemical dissolution chamber");
    }
}
