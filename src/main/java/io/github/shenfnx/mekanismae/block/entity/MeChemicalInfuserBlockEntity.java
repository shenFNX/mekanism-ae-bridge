package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalInfuserMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalChemicalToChemicalRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.vanilla_input.BiChemicalRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** Chemical + chemical -> chemical processing using Mekanism infusing recipes. */
public final class MeChemicalInfuserBlockEntity extends AbstractDualKeyMeMachineBlockEntity {
    public MeChemicalInfuserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_INFUSER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_INFUSER_ITEM.get(), MachineType.ME_CHEMICAL_INFUSER);
    }

    @Override
    protected RecipeOperation resolveRecipe(AEKey firstKey, long firstAvailable,
            AEKey secondKey, long secondAvailable) {
        if (level == null || !(firstKey instanceof MekanismKey firstChemical)
                || !(secondKey instanceof MekanismKey secondChemical)
                || firstAvailable <= 0 || secondAvailable <= 0) {
            return null;
        }
        ChemicalStack first = firstChemical.withAmount(firstAvailable);
        ChemicalStack second = secondChemical.withAmount(secondAvailable);
        ChemicalChemicalToChemicalRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_CHEMICAL_INFUSING.get(),
                        new BiChemicalRecipeInput(first, second), level)
                .map(holder -> holder.value())
                .orElse(null);
        if (recipe == null) {
            return null;
        }
        long firstNeeded;
        long secondNeeded;
        if (recipe.getLeftInput().test(first) && recipe.getRightInput().test(second)) {
            firstNeeded = recipe.getLeftInput().getNeededAmount(first);
            secondNeeded = recipe.getRightInput().getNeededAmount(second);
        } else if (recipe.getRightInput().test(first) && recipe.getLeftInput().test(second)) {
            firstNeeded = recipe.getRightInput().getNeededAmount(first);
            secondNeeded = recipe.getLeftInput().getNeededAmount(second);
        } else {
            return null;
        }
        ChemicalStack output = recipe.getOutput(first, second);
        if (firstNeeded <= 0 || secondNeeded <= 0 || output.isEmpty() || output.getAmount() <= 0) {
            return null;
        }
        return new RecipeOperation(firstNeeded, secondNeeded, MekanismKey.of(output), output.getAmount());
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_infuser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalInfuserMenu(containerId, inventory, worldPosition);
    }
}
