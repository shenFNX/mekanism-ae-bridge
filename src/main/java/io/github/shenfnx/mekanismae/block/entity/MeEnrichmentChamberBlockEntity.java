package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class MeEnrichmentChamberBlockEntity extends AbstractItemToItemMeMachineBlockEntity {
    public MeEnrichmentChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(), pos, state,
                ModBlocks.ME_ENRICHMENT_CHAMBER_ITEM.get(), MekanismRecipeTypes.TYPE_ENRICHING.value());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_enrichment_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
            Player player) {
        return new MeEnrichmentChamberMenu(containerId, inventory, worldPosition);
    }
}
