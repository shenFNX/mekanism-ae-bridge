package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeRotaryCondensentratorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeRotaryCondensentratorBlock extends AbstractProcessingMeMachineBlock {
    public MeRotaryCondensentratorBlock(BlockBehaviour.Properties properties) {
        super(properties, MeRotaryCondensentratorBlockEntity::new,
                ModBlockEntities.ME_ROTARY_CONDENSENTRATOR);
    }
}
