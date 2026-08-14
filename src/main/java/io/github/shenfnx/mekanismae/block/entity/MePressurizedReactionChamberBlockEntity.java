package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MePressurizedReactionChamberMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.PressurizedReactionRecipe;
import mekanism.api.recipes.vanilla_input.ReactionRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/** Item + fluid + chemical -> optional item and chemical products. */
public final class MePressurizedReactionChamberBlockEntity extends AbstractMultiKeyMeMachineBlockEntity {
    public MePressurizedReactionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_PRESSURIZED_REACTION_CHAMBER.get(), pos, state,
                ModBlocks.ME_PRESSURIZED_REACTION_CHAMBER_ITEM.get(),
                MachineType.ME_PRESSURIZED_REACTION_CHAMBER, 3, 2);
    }

    @Override
    protected RecipeOperation resolveRecipe(List<ResourceAmount> inputs) {
        if (level == null || inputs.size() != 3) {
            return null;
        }
        int itemIndex = findIndex(inputs, AEItemKey.class);
        int fluidIndex = findIndex(inputs, AEFluidKey.class);
        int chemicalIndex = findIndex(inputs, MekanismKey.class);
        if (itemIndex < 0 || fluidIndex < 0 || chemicalIndex < 0) {
            return null;
        }
        AEItemKey itemKey = (AEItemKey) inputs.get(itemIndex).key();
        AEFluidKey fluidKey = (AEFluidKey) inputs.get(fluidIndex).key();
        MekanismKey chemicalKey = (MekanismKey) inputs.get(chemicalIndex).key();
        ItemStack item = itemKey.toStack((int) Math.min(Integer.MAX_VALUE, inputs.get(itemIndex).amount()));
        FluidStack fluid = fluidKey.toStack((int) Math.min(Integer.MAX_VALUE, inputs.get(fluidIndex).amount()));
        ChemicalStack chemical = chemicalKey.withAmount(inputs.get(chemicalIndex).amount());
        PressurizedReactionRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_REACTION.get(),
                        new ReactionRecipeInput(item, fluid, chemical), level)
                .map(holder -> holder.value()).orElse(null);
        if (recipe == null) {
            return null;
        }
        long itemNeeded = recipe.getInputSolid().getNeededAmount(item);
        long fluidNeeded = recipe.getInputFluid().getNeededAmount(fluid);
        long chemicalNeeded = recipe.getInputChemical().getNeededAmount(chemical);
        PressurizedReactionRecipe.PressurizedReactionRecipeOutput output = recipe.getOutput(item, fluid, chemical);
        if (itemNeeded <= 0 || fluidNeeded <= 0 || chemicalNeeded <= 0) {
            return null;
        }
        List<ResourceAmount> units = new ArrayList<>(3);
        for (int index = 0; index < inputs.size(); index++) {
            if (index == itemIndex) {
                units.add(new ResourceAmount(itemKey, itemNeeded));
            } else if (index == fluidIndex) {
                units.add(new ResourceAmount(fluidKey, fluidNeeded));
            } else {
                units.add(new ResourceAmount(chemicalKey, chemicalNeeded));
            }
        }
        List<ResourceAmount> outputs = new ArrayList<>(2);
        if (!output.item().isEmpty()) {
            outputs.add(new ResourceAmount(AEItemKey.of(output.item()), output.item().getCount()));
        }
        if (!output.chemical().isEmpty()) {
            outputs.add(new ResourceAmount(MekanismKey.of(output.chemical()), output.chemical().getAmount()));
        }
        return outputs.isEmpty() ? null : new RecipeOperation(units, outputs);
    }

    private static int findIndex(List<ResourceAmount> inputs, Class<? extends AEKey> type) {
        int found = -1;
        for (int index = 0; index < inputs.size(); index++) {
            if (type.isInstance(inputs.get(index).key())) {
                if (found >= 0) {
                    return -1;
                }
                found = index;
            }
        }
        return found;
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEItemKey || key instanceof AEFluidKey || key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof AEItemKey || key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_pressurized_reaction_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MePressurizedReactionChamberMenu(containerId, inventory, worldPosition);
    }
}
