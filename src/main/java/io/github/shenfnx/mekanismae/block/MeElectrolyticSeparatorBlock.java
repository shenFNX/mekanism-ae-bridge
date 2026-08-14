package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeElectrolyticSeparatorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeElectrolyticSeparatorBlock extends AbstractProcessingMeMachineBlock {
    public MeElectrolyticSeparatorBlock(BlockBehaviour.Properties properties) {
        super(properties, MeElectrolyticSeparatorBlockEntity::new,
                ModBlockEntities.ME_ELECTROLYTIC_SEPARATOR);
    }
}
