package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeNutritionalLiquifierBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeNutritionalLiquifierBlock extends AbstractProcessingMeMachineBlock {
    public MeNutritionalLiquifierBlock(BlockBehaviour.Properties properties) {
        super(properties, MeNutritionalLiquifierBlockEntity::new,
                ModBlockEntities.ME_NUTRITIONAL_LIQUIFIER);
    }
}
