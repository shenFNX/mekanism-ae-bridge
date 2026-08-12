package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.AbstractItemToItemMeMachineBlockEntity;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Shared interaction, ticker, menu, and safe-break behavior for item-only ME machines. */
public abstract class AbstractItemToItemMeMachineBlock extends AbstractProcessingMeMachineBlock {
    protected AbstractItemToItemMeMachineBlock(BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends AbstractItemToItemMeMachineBlockEntity> blockEntityFactory,
            Supplier<? extends BlockEntityType<?>> blockEntityType) {
        super(properties, blockEntityFactory, blockEntityType);
    }
}
