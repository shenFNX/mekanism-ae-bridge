package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeMetallurgicInfuserBlock extends AbstractItemChemicalToItemMeMachineBlock {
    public MeMetallurgicInfuserBlock(BlockBehaviour.Properties properties) {
        super(properties, MeMetallurgicInfuserBlockEntity::new, ModBlockEntities.ME_METALLURGIC_INFUSER);
    }
}
