package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeOsmiumCompressorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeOsmiumCompressorBlock extends AbstractItemChemicalToItemMeMachineBlock {
    public MeOsmiumCompressorBlock(BlockBehaviour.Properties properties) {
        super(properties, MeOsmiumCompressorBlockEntity::new, ModBlockEntities.ME_OSMIUM_COMPRESSOR);
    }
}
