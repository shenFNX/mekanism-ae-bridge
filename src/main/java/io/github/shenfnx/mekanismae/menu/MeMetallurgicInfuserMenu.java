package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeMetallurgicInfuserMenu extends AbstractItemChemicalToItemMeMachineMenu {
    public MeMetallurgicInfuserMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeMetallurgicInfuserMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_METALLURGIC_INFUSER.get(), containerId, inventory, pos,
                MeMetallurgicInfuserBlockEntity.class, "ME metallurgic infuser");
    }
}
