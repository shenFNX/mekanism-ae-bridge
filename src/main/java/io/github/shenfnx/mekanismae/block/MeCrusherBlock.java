package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeCrusherBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeCrusherBlock extends AbstractItemToItemMeMachineBlock {
    public MeCrusherBlock(BlockBehaviour.Properties properties) {
        super(properties, MeCrusherBlockEntity::new, ModBlockEntities.ME_CRUSHER);
    }
}
