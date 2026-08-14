package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeNutritionalLiquifierMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import mekanism.api.recipes.ItemStackToFluidOptionalItemRecipe;
import mekanism.api.recipes.basic.BasicItemStackToFluidOptionalItemRecipe;
import mekanism.common.tile.machine.TileEntityNutritionalLiquifier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Food -> nutritional paste plus an optional returned container item. */
public final class MeNutritionalLiquifierBlockEntity extends AbstractMultiKeyMeMachineBlockEntity {
    public MeNutritionalLiquifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_NUTRITIONAL_LIQUIFIER.get(), pos, state,
                ModBlocks.ME_NUTRITIONAL_LIQUIFIER_ITEM.get(), MachineType.ME_NUTRITIONAL_LIQUIFIER, 1, 2);
    }

    @Override
    protected RecipeOperation resolveRecipe(List<ResourceAmount> inputs) {
        if (inputs.size() != 1 || !(inputs.getFirst().key() instanceof AEItemKey itemKey)) {
            return null;
        }
        ItemStack item = itemKey.toStack((int) Math.min(Integer.MAX_VALUE, inputs.getFirst().amount()));
        BasicItemStackToFluidOptionalItemRecipe recipe = TileEntityNutritionalLiquifier.getRecipe(item);
        if (recipe == null) {
            return null;
        }
        long needed = recipe.getInput().getNeededAmount(item);
        ItemStackToFluidOptionalItemRecipe.FluidOptionalItemOutput output = recipe.getOutput(item);
        if (needed <= 0 || output.fluid().isEmpty()) {
            return null;
        }
        List<ResourceAmount> outputs = new ArrayList<>(2);
        outputs.add(new ResourceAmount(AEFluidKey.of(output.fluid()), output.fluid().getAmount()));
        if (!output.optionalItem().isEmpty()) {
            outputs.add(new ResourceAmount(AEItemKey.of(output.optionalItem()), output.optionalItem().getCount()));
        }
        return new RecipeOperation(List.of(new ResourceAmount(itemKey, needed)), outputs);
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEItemKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof AEFluidKey || key instanceof AEItemKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_nutritional_liquifier");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeNutritionalLiquifierMenu(containerId, inventory, worldPosition);
    }
}
