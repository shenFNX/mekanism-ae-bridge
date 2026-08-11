package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeEnrichmentChamberBlock extends AbstractItemToItemMeMachineBlock {
    public MeEnrichmentChamberBlock(BlockBehaviour.Properties properties) {
        super(properties, MeEnrichmentChamberBlockEntity::new, ModBlockEntities.ME_ENRICHMENT_CHAMBER);
    }
}
