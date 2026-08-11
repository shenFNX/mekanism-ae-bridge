package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeCrusherBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeCrusherMenu extends MeEnrichmentChamberMenu {
    public MeCrusherMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeCrusherMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CRUSHER.get(), containerId, inventory, pos,
                MeCrusherBlockEntity.class, "ME crusher");
    }
}
