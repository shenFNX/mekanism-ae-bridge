package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MePrecisionSawmillBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MePrecisionSawmillBlock extends AbstractProcessingMeMachineBlock {
    public MePrecisionSawmillBlock(BlockBehaviour.Properties properties) {
        super(properties, MePrecisionSawmillBlockEntity::new, ModBlockEntities.ME_PRECISION_SAWMILL);
    }
}
