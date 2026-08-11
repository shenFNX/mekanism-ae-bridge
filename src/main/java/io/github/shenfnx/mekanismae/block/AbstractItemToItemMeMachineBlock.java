package io.github.shenfnx.mekanismae.block;

import appeng.api.crafting.PatternDetailsHelper;
import io.github.shenfnx.mekanismae.block.entity.AbstractItemToItemMeMachineBlockEntity;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/** Shared interaction, ticker, menu, and safe-break behavior for item-only ME machines. */
public abstract class AbstractItemToItemMeMachineBlock extends Block implements EntityBlock {
    private final BiFunction<BlockPos, BlockState, ? extends AbstractItemToItemMeMachineBlockEntity> blockEntityFactory;
    private final Supplier<? extends BlockEntityType<?>> blockEntityType;

    protected AbstractItemToItemMeMachineBlock(BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends AbstractItemToItemMeMachineBlockEntity> blockEntityFactory,
            Supplier<? extends BlockEntityType<?>> blockEntityType) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
        this.blockEntityType = blockEntityType;
    }

    @Override
    public final BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.apply(pos, state);
    }

    @Override
    public final <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != blockEntityType.get()) {
            return null;
        }
        return (level1, pos1, state1, blockEntity) -> AbstractItemToItemMeMachineBlockEntity.serverTick(
                level1, pos1, state1, (AbstractItemToItemMeMachineBlockEntity) blockEntity);
    }

    @Override
    protected final ItemInteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!PatternDetailsHelper.isEncodedPattern(itemStack)) {
            return openMenu(level, pos, player);
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractItemToItemMeMachineBlockEntity machine)) {
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!machine.setPattern(itemStack)) {
            return ItemInteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials()) {
            itemStack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult openMenu(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractItemToItemMeMachineBlockEntity machine)) {
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public final boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractItemToItemMeMachineBlockEntity machine && machine.hasStoredContents()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        Component.translatable("message.mekanismae.empty_before_breaking"), true);
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected final net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractItemToItemMeMachineBlockEntity machine)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack pattern = machine.takePattern();
            if (pattern.isEmpty()) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            if (!player.addItem(pattern)) {
                player.drop(pattern, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine, buffer -> buffer.writeBlockPos(pos));
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }
}
