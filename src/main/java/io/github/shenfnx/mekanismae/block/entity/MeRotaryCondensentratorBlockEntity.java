package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import com.mojang.datafixers.util.Either;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeRotaryCondensentratorMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.RotaryRecipe;
import mekanism.api.recipes.vanilla_input.RotaryRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Bidirectional fluid/chemical conversion selected from the encoded pattern. */
public final class MeRotaryCondensentratorBlockEntity extends AbstractMultiKeyMeMachineBlockEntity {
    public MeRotaryCondensentratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ROTARY_CONDENSENTRATOR.get(), pos, state,
                ModBlocks.ME_ROTARY_CONDENSENTRATOR_ITEM.get(), MachineType.ME_ROTARY_CONDENSENTRATOR, 1, 1);
    }

    @Override
    protected RecipeOperation resolveRecipe(List<ResourceAmount> inputs) {
        if (level == null || inputs.size() != 1) {
            return null;
        }
        AEKey inputKey = inputs.getFirst().key();
        long available = inputs.getFirst().amount();
        if (inputKey instanceof AEFluidKey fluidKey) {
            FluidStack fluid = fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, available));
            RotaryRecipe recipe = level.getRecipeManager()
                    .getRecipeFor(MekanismRecipeTypes.TYPE_ROTARY.get(),
                            new RotaryRecipeInput(Either.left(fluid)), level)
                    .map(holder -> holder.value()).orElse(null);
            if (recipe == null || !recipe.hasFluidToChemical()) {
                return null;
            }
            long needed = recipe.getFluidInput().getNeededAmount(fluid);
            ChemicalStack output = recipe.getChemicalOutput(fluid);
            return needed <= 0 || output.isEmpty() ? null : new RecipeOperation(
                    List.of(new ResourceAmount(fluidKey, needed)),
                    List.of(new ResourceAmount(MekanismKey.of(output), output.getAmount())));
        }
        if (inputKey instanceof MekanismKey chemicalKey) {
            ChemicalStack chemical = chemicalKey.withAmount(available);
            RotaryRecipe recipe = level.getRecipeManager()
                    .getRecipeFor(MekanismRecipeTypes.TYPE_ROTARY.get(),
                            new RotaryRecipeInput(Either.right(chemical)), level)
                    .map(holder -> holder.value()).orElse(null);
            if (recipe == null || !recipe.hasChemicalToFluid()) {
                return null;
            }
            long needed = recipe.getChemicalInput().getNeededAmount(chemical);
            FluidStack output = recipe.getFluidOutput(chemical);
            return needed <= 0 || output.isEmpty() ? null : new RecipeOperation(
                    List.of(new ResourceAmount(chemicalKey, needed)),
                    List.of(new ResourceAmount(AEFluidKey.of(output), output.getAmount())));
        }
        return null;
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEFluidKey || key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof AEFluidKey || key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_rotary_condensentrator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeRotaryCondensentratorMenu(containerId, inventory, worldPosition);
    }
}
