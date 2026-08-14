package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MePressurizedReactionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MePressurizedReactionChamberBlock extends AbstractProcessingMeMachineBlock {
    public MePressurizedReactionChamberBlock(BlockBehaviour.Properties properties) {
        super(properties, MePressurizedReactionChamberBlockEntity::new,
                ModBlockEntities.ME_PRESSURIZED_REACTION_CHAMBER);
    }
}
