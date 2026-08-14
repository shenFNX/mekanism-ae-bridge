package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalWasherMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.FluidChemicalToChemicalRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.vanilla_input.SingleFluidChemicalRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Fluid + chemical -> chemical washing with input order preserved by the ledger. */
public final class MeChemicalWasherBlockEntity extends AbstractMultiKeyMeMachineBlockEntity {
    public MeChemicalWasherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_WASHER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_WASHER_ITEM.get(), MachineType.ME_CHEMICAL_WASHER, 2, 1);
    }

    @Override
    protected RecipeOperation resolveRecipe(List<ResourceAmount> inputs) {
        if (level == null || inputs.size() != 2) {
            return null;
        }
        int fluidIndex = inputs.get(0).key() instanceof AEFluidKey ? 0
                : inputs.get(1).key() instanceof AEFluidKey ? 1 : -1;
        int chemicalIndex = inputs.get(0).key() instanceof MekanismKey ? 0
                : inputs.get(1).key() instanceof MekanismKey ? 1 : -1;
        if (fluidIndex < 0 || chemicalIndex < 0 || fluidIndex == chemicalIndex) {
            return null;
        }
        AEFluidKey fluidKey = (AEFluidKey) inputs.get(fluidIndex).key();
        MekanismKey chemicalKey = (MekanismKey) inputs.get(chemicalIndex).key();
        FluidStack fluid = fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, inputs.get(fluidIndex).amount()));
        ChemicalStack chemical = chemicalKey.withAmount(inputs.get(chemicalIndex).amount());
        FluidChemicalToChemicalRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_WASHING.get(),
                        new SingleFluidChemicalRecipeInput(fluid, chemical), level)
                .map(holder -> holder.value()).orElse(null);
        if (recipe == null) {
            return null;
        }
        long fluidNeeded = recipe.getFluidInput().getNeededAmount(fluid);
        long chemicalNeeded = recipe.getChemicalInput().getNeededAmount(chemical);
        ChemicalStack output = recipe.getOutput(fluid, chemical);
        if (fluidNeeded <= 0 || chemicalNeeded <= 0 || output.isEmpty()) {
            return null;
        }
        ResourceAmount fluidUnit = new ResourceAmount(fluidKey, fluidNeeded);
        ResourceAmount chemicalUnit = new ResourceAmount(chemicalKey, chemicalNeeded);
        return new RecipeOperation(
                fluidIndex == 0 ? List.of(fluidUnit, chemicalUnit) : List.of(chemicalUnit, fluidUnit),
                List.of(new ResourceAmount(MekanismKey.of(output), output.getAmount())));
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEFluidKey || key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_washer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalWasherMenu(containerId, inventory, worldPosition);
    }
}
