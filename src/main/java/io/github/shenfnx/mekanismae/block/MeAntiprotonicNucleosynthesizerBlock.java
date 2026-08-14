package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeAntiprotonicNucleosynthesizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeAntiprotonicNucleosynthesizerBlock extends AbstractProcessingMeMachineBlock {
    public MeAntiprotonicNucleosynthesizerBlock(BlockBehaviour.Properties properties) {
        super(properties, MeAntiprotonicNucleosynthesizerBlockEntity::new,
                ModBlockEntities.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER);
    }
}
