package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeCombinerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeCombinerBlock extends AbstractProcessingMeMachineBlock {
    public MeCombinerBlock(BlockBehaviour.Properties properties) {
        super(properties, MeCombinerBlockEntity::new, ModBlockEntities.ME_COMBINER);
    }
}
