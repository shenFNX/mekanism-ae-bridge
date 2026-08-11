package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.menu.MeEnergizedSmelterMenu;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.ItemStackToItemStackRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.common.recipe.MekanismRecipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Item-only ME processor backed by Mekanism's official smelting recipe type. */
public final class MeEnergizedSmelterBlockEntity extends AbstractItemToItemMeMachineBlockEntity {
    public MeEnergizedSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ENERGIZED_SMELTER.get(), pos, state,
                ModBlocks.ME_ENERGIZED_SMELTER_ITEM.get(), MekanismRecipeTypes.TYPE_SMELTING.value(),
                MachineType.ME_ENERGIZED_SMELTER);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_energized_smelter");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, net.minecraft.world.entity.player.Inventory inventory,
            Player player) {
        return new MeEnergizedSmelterMenu(containerId, inventory, worldPosition);
    }

    @Override
    protected ItemStackToItemStackRecipe findRecipe(ItemStack input) {
        // Mekanism exposes vanilla furnace recipes as synthetic smelting recipes
        // through this cache; RecipeManager#getRecipeFor(TYPE_SMELTING, ...) does not.
        return level == null ? null : MekanismRecipeType.SMELTING.getInputCache().findFirstRecipe(level, input);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MeEnergizedSmelterBlockEntity blockEntity) {
        AbstractItemToItemMeMachineBlockEntity.serverTick(level, pos, state, blockEntity);
    }
}
