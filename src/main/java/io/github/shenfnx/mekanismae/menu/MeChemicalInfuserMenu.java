package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalInfuserMenu extends AbstractDualKeyMeMachineMenu {
    public MeChemicalInfuserMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalInfuserMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_INFUSER.get(), containerId, inventory, pos,
                MeChemicalInfuserBlockEntity.class, "ME chemical infuser");
    }
}
