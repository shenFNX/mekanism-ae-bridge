package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalInfuserBlock extends AbstractProcessingMeMachineBlock {
    public MeChemicalInfuserBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalInfuserBlockEntity::new, ModBlockEntities.ME_CHEMICAL_INFUSER);
    }
}
