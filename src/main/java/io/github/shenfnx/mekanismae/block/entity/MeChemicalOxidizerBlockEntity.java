package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalOxidizerMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ItemStackToChemicalRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;

/** Item -> chemical processing using Mekanism's native oxidizing recipes. */
public final class MeChemicalOxidizerBlockEntity extends AbstractSingleKeyMeMachineBlockEntity {
    public MeChemicalOxidizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_OXIDIZER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_OXIDIZER_ITEM.get(), MachineType.ME_CHEMICAL_OXIDIZER);
    }

    @Override
    protected RecipeOperation resolveRecipe(AEKey inputKey, long availableAmount) {
        if (level == null || !(inputKey instanceof AEItemKey itemKey) || availableAmount <= 0) {
            return null;
        }
        int probeCount = (int) Math.min(Integer.MAX_VALUE, Math.max(1, availableAmount));
        ItemStack probe = itemKey.toStack(probeCount);
        ItemStackToChemicalRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_OXIDIZING.get(), new SingleRecipeInput(probe), level)
                .map(holder -> holder.value())
                .orElse(null);
        if (recipe == null) {
            return null;
        }
        long needed = recipe.getInput().getNeededAmount(probe);
        ChemicalStack output = recipe.getOutput(probe);
        if (needed <= 0 || output.isEmpty() || output.getAmount() <= 0) {
            return null;
        }
        return new RecipeOperation(needed, MekanismKey.of(output), output.getAmount());
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEItemKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_oxidizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalOxidizerMenu(containerId, inventory, worldPosition);
    }
}
