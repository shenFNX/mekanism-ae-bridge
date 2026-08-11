package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeOsmiumCompressorMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class MeOsmiumCompressorBlockEntity extends AbstractItemChemicalToItemMeMachineBlockEntity {
    public MeOsmiumCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_OSMIUM_COMPRESSOR.get(), pos, state,
                ModBlocks.ME_OSMIUM_COMPRESSOR_ITEM.get(), MachineType.ME_OSMIUM_COMPRESSOR,
                MekanismRecipeTypes.TYPE_COMPRESSING.get(), "ME osmium compressor");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_osmium_compressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeOsmiumCompressorMenu(containerId, inventory, worldPosition);
    }
}
