package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalWasherBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalWasherBlock extends AbstractProcessingMeMachineBlock {
    public MeChemicalWasherBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalWasherBlockEntity::new,
                ModBlockEntities.ME_CHEMICAL_WASHER);
    }
}
