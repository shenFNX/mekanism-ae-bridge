package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeElectrolyticSeparatorMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.recipes.ElectrolysisRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.vanilla_input.SingleFluidRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid -> two chemicals, with both products returned to the ME network. */
public final class MeElectrolyticSeparatorBlockEntity extends AbstractMultiKeyMeMachineBlockEntity {
    public MeElectrolyticSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ELECTROLYTIC_SEPARATOR.get(), pos, state,
                ModBlocks.ME_ELECTROLYTIC_SEPARATOR_ITEM.get(), MachineType.ME_ELECTROLYTIC_SEPARATOR, 1, 2);
    }

    @Override
    protected RecipeOperation resolveRecipe(List<ResourceAmount> inputs) {
        if (level == null || inputs.size() != 1 || !(inputs.getFirst().key() instanceof AEFluidKey fluidKey)) {
            return null;
        }
        FluidStack fluid = fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, inputs.getFirst().amount()));
        ElectrolysisRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_SEPARATING.get(), new SingleFluidRecipeInput(fluid), level)
                .map(holder -> holder.value()).orElse(null);
        if (recipe == null) {
            return null;
        }
        long needed = recipe.getInput().getNeededAmount(fluid);
        ElectrolysisRecipe.ElectrolysisRecipeOutput output = recipe.getOutput(fluid);
        if (needed <= 0 || output.left().isEmpty() || output.right().isEmpty()) {
            return null;
        }
        return new RecipeOperation(
                List.of(new ResourceAmount(fluidKey, needed)),
                List.of(new ResourceAmount(MekanismKey.of(output.left()), output.left().getAmount()),
                        new ResourceAmount(MekanismKey.of(output.right()), output.right().getAmount())));
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEFluidKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_electrolytic_separator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeElectrolyticSeparatorMenu(containerId, inventory, worldPosition);
    }
}
