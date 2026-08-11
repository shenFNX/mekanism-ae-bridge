package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.menu.MeCrusherMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Item-only ME processor backed by Mekanism's official crushing recipe type. */
public final class MeCrusherBlockEntity extends AbstractItemToItemMeMachineBlockEntity {
    public MeCrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CRUSHER.get(), pos, state,
                ModBlocks.ME_CRUSHER_ITEM.get(), MekanismRecipeTypes.TYPE_CRUSHING.value());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_crusher");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
            Player player) {
        return new MeCrusherMenu(containerId, inventory, worldPosition);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MeCrusherBlockEntity blockEntity) {
        AbstractItemToItemMeMachineBlockEntity.serverTick(level, pos, state, blockEntity);
    }
}
