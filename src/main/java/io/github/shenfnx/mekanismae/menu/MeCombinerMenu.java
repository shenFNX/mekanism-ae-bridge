package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeCombinerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeCombinerMenu extends AbstractMultiItemMeMachineMenu {
    public MeCombinerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeCombinerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_COMBINER.get(), containerId, inventory, pos,
                MeCombinerBlockEntity.class, "ME combiner");
    }
}
