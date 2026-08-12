package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeCombinerMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Pattern-isolated ME form of Mekanism's item + item Combiner. */
public final class MeCombinerBlockEntity extends AbstractTwoItemToItemMeMachineBlockEntity {
    public MeCombinerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_COMBINER.get(), pos, state,
                ModBlocks.ME_COMBINER_ITEM.get(), MachineType.ME_COMBINER);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_combiner");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeCombinerMenu(containerId, inventory, worldPosition);
    }
}
