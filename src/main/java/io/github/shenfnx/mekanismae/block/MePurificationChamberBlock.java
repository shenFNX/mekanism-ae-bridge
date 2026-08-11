package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MePurificationChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class MePurificationChamberBlock extends AbstractItemChemicalToItemMeMachineBlock {
    public MePurificationChamberBlock(BlockBehaviour.Properties properties) {
        super(properties, MePurificationChamberBlockEntity::new, ModBlockEntities.ME_PURIFICATION_CHAMBER);
    }
}
