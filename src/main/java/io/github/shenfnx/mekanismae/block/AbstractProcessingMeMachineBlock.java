package io.github.shenfnx.mekanismae.block;

import io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;

/** Common interaction, ticker, menu, and safe-break behavior for all ME processors. */
public abstract class AbstractProcessingMeMachineBlock extends AbstractMeMachineBlock {
    private final BiFunction<BlockPos, BlockState, ? extends AbstractMeProcessingBlockEntity> blockEntityFactory;
    private final Supplier<? extends BlockEntityType<?>> blockEntityType;

    protected AbstractProcessingMeMachineBlock(BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends AbstractMeProcessingBlockEntity> blockEntityFactory,
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
        return (tickLevel, tickPos, tickState, blockEntity) ->
                ((AbstractMeProcessingBlockEntity) blockEntity).tickServer();
    }

    @Override
    protected final ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return openMenu(level, pos, player);
    }

    private ItemInteractionResult openMenu(Level level, BlockPos pos, Player player) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractMeProcessingBlockEntity machine)) {
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
    public final boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
            boolean willHarvest, FluidState fluid) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractMeProcessingBlockEntity machine && machine.hasProcessingResources()) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mekanismae.empty_before_breaking"), true);
            }
            return false;
        }
        List<ItemStack> inventoryDrops = new ArrayList<>();
        if (!level.isClientSide() && blockEntity instanceof AbstractMeProcessingBlockEntity machine) {
            for (int slot = 0; slot < machine.getContainerSize(); slot++) {
                ItemStack stack = machine.getItem(slot);
                if (!stack.isEmpty()) {
                    inventoryDrops.add(stack.copy());
                }
            }
        }
        boolean destroyed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (destroyed && !level.isClientSide()) {
            if (blockEntity instanceof AbstractMeProcessingBlockEntity machine) {
                machine.clearInstalledItemsAfterBreak();
            }
            for (ItemStack stack : inventoryDrops) {
                popResource(level, pos, stack);
            }
        }
        return destroyed;
    }

    @Override
    protected final net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AbstractMeProcessingBlockEntity machine)) {
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
