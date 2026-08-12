package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MePrecisionSawmillBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MePrecisionSawmillMenu extends AbstractMultiItemMeMachineMenu {
    public MePrecisionSawmillMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MePrecisionSawmillMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_PRECISION_SAWMILL.get(), containerId, inventory, pos,
                MePrecisionSawmillBlockEntity.class, "ME precision sawmill");
    }
}
