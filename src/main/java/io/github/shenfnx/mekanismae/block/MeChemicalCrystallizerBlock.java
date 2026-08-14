package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalCrystallizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalCrystallizerBlock extends AbstractProcessingMeMachineBlock {
    public MeChemicalCrystallizerBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalCrystallizerBlockEntity::new, ModBlockEntities.ME_CHEMICAL_CRYSTALLIZER);
    }
}
