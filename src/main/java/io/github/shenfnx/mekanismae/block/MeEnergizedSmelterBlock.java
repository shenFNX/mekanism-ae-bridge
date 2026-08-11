package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeEnergizedSmelterBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeEnergizedSmelterBlock extends AbstractItemToItemMeMachineBlock {
    public MeEnergizedSmelterBlock(BlockBehaviour.Properties properties) {
        super(properties, MeEnergizedSmelterBlockEntity::new, ModBlockEntities.ME_ENERGIZED_SMELTER);
    }
}
