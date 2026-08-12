package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.AbstractItemChemicalToItemMeMachineBlockEntity;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractItemChemicalToItemMeMachineBlock extends AbstractProcessingMeMachineBlock {
    protected AbstractItemChemicalToItemMeMachineBlock(BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends AbstractItemChemicalToItemMeMachineBlockEntity> blockEntityFactory,
            Supplier<? extends BlockEntityType<?>> blockEntityType) {
        super(properties, blockEntityFactory, blockEntityType);
    }
}
