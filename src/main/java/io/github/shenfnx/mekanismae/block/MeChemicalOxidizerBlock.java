package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalOxidizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalOxidizerBlock extends AbstractProcessingMeMachineBlock {
    public MeChemicalOxidizerBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalOxidizerBlockEntity::new, ModBlockEntities.ME_CHEMICAL_OXIDIZER);
    }
}
