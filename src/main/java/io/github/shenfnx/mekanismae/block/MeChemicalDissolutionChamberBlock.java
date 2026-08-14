package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalDissolutionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalDissolutionChamberBlock extends AbstractProcessingMeMachineBlock {
    public MeChemicalDissolutionChamberBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalDissolutionChamberBlockEntity::new,
                ModBlockEntities.ME_CHEMICAL_DISSOLUTION_CHAMBER);
    }
}
