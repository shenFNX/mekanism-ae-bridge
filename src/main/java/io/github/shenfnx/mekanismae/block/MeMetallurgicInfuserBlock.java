package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import appeng.api.crafting.PatternDetailsHelper;

public final class MeMetallurgicInfuserBlock extends Block implements EntityBlock {
    public MeMetallurgicInfuserBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MeMetallurgicInfuserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ME_METALLURGIC_INFUSER.get()) {
            return null;
        }
        return (level1, pos1, state1, blockEntity) -> MeMetallurgicInfuserBlockEntity.serverTick(
                level1, pos1, state1, (MeMetallurgicInfuserBlockEntity) blockEntity);
    }

    @Override
    protected ItemInteractionResult useItemOn(
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
        if (!(blockEntity instanceof MeMetallurgicInfuserBlockEntity infuser)) {
            return ItemInteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!infuser.setPattern(itemStack)) {
            return ItemInteractionResult.FAIL;
        }
        if (!player.hasInfiniteMaterials()) {
            itemStack.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private ItemInteractionResult openMenu(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MeMetallurgicInfuserBlockEntity infuser)) {
            return ItemInteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(infuser, buffer -> buffer.writeBlockPos(pos));
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean onDestroyedByPlayer(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            boolean willHarvest,
            FluidState fluid) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MeMetallurgicInfuserBlockEntity infuser && infuser.hasStoredContents()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.mekanismae.empty_before_breaking"),
                        true);
            }
            return false;
        }
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MeMetallurgicInfuserBlockEntity infuser)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            ItemStack pattern = infuser.takePattern();
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
            serverPlayer.openMenu(infuser, buffer -> buffer.writeBlockPos(pos));
            return net.minecraft.world.InteractionResult.CONSUME;
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

}
