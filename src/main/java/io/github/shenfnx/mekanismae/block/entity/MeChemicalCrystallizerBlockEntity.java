package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalCrystallizerMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.vanilla_input.SingleChemicalRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Chemical -> item processing using Mekanism's native crystallizing recipes. */
public final class MeChemicalCrystallizerBlockEntity extends AbstractSingleKeyMeMachineBlockEntity {
    public MeChemicalCrystallizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_CRYSTALLIZER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_CRYSTALLIZER_ITEM.get(), MachineType.ME_CHEMICAL_CRYSTALLIZER);
    }

    @Override
    protected RecipeOperation resolveRecipe(AEKey inputKey, long availableAmount) {
        if (level == null || !(inputKey instanceof MekanismKey chemicalKey) || availableAmount <= 0) {
            return null;
        }
        ChemicalStack probe = chemicalKey.withAmount(availableAmount);
        ChemicalCrystallizerRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_CRYSTALLIZING.get(),
                        new SingleChemicalRecipeInput(probe), level)
                .map(holder -> holder.value())
                .orElse(null);
        if (recipe == null) {
            return null;
        }
        long needed = recipe.getInput().getNeededAmount(probe);
        ItemStack output = recipe.getOutput(probe);
        AEItemKey outputKey = AEItemKey.of(output);
        if (needed <= 0 || output.isEmpty() || outputKey == null || output.getCount() <= 0) {
            return null;
        }
        return new RecipeOperation(needed, outputKey, output.getCount());
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof AEItemKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_crystallizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalCrystallizerMenu(containerId, inventory, worldPosition);
    }
}
