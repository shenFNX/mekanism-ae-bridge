package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalInjectionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MeChemicalInjectionChamberBlock extends AbstractItemChemicalToItemMeMachineBlock {
    public MeChemicalInjectionChamberBlock(BlockBehaviour.Properties properties) {
        super(properties, MeChemicalInjectionChamberBlockEntity::new, ModBlockEntities.ME_CHEMICAL_INJECTION_CHAMBER);
    }
}
