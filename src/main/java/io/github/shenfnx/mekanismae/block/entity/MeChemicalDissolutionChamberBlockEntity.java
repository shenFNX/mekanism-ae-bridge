package io.github.shenfnx.mekanismae.block.entity;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalDissolutionChamberMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.ChemicalDissolutionRecipe;
import mekanism.api.recipes.MekanismRecipeTypes;
import mekanism.api.recipes.vanilla_input.SingleItemChemicalRecipeInput;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Item + chemical -> chemical processing using Mekanism dissolution recipes. */
public final class MeChemicalDissolutionChamberBlockEntity extends AbstractDualKeyMeMachineBlockEntity {
    public MeChemicalDissolutionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_DISSOLUTION_CHAMBER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_DISSOLUTION_CHAMBER_ITEM.get(),
                MachineType.ME_CHEMICAL_DISSOLUTION_CHAMBER);
    }

    @Override
    protected RecipeOperation resolveRecipe(AEKey firstKey, long firstAvailable,
            AEKey secondKey, long secondAvailable) {
        boolean itemFirst = firstKey instanceof AEItemKey && secondKey instanceof MekanismKey;
        boolean itemSecond = secondKey instanceof AEItemKey && firstKey instanceof MekanismKey;
        if (level == null || !itemFirst && !itemSecond || firstAvailable <= 0 || secondAvailable <= 0) {
            return null;
        }
        AEItemKey itemKey = (AEItemKey) (itemFirst ? firstKey : secondKey);
        MekanismKey chemicalKey = (MekanismKey) (itemFirst ? secondKey : firstKey);
        long itemAvailable = itemFirst ? firstAvailable : secondAvailable;
        long chemicalAvailable = itemFirst ? secondAvailable : firstAvailable;
        ItemStack item = itemKey.toStack((int) Math.min(Integer.MAX_VALUE, Math.max(1, itemAvailable)));
        ChemicalStack chemical = chemicalKey.withAmount(chemicalAvailable);
        ChemicalDissolutionRecipe recipe = level.getRecipeManager()
                .getRecipeFor(MekanismRecipeTypes.TYPE_DISSOLUTION.get(),
                        new SingleItemChemicalRecipeInput(item, chemical), level)
                .map(holder -> holder.value())
                .orElse(null);
        if (recipe == null) {
            return null;
        }
        long itemNeeded = recipe.getItemInput().getNeededAmount(item);
        long chemicalNeeded = recipe.getChemicalInput().getNeededAmount(chemical);
        if (recipe.perTickUsage() && chemicalNeeded > 0) {
            int ticks = TileEntityAdvancedElectricMachine.BASE_TICKS_REQUIRED;
            chemicalNeeded = chemicalNeeded > Long.MAX_VALUE / ticks ? Long.MAX_VALUE : chemicalNeeded * ticks;
        }
        ChemicalStack output = recipe.getOutput(item, chemical);
        if (itemNeeded <= 0 || chemicalNeeded <= 0 || output.isEmpty() || output.getAmount() <= 0) {
            return null;
        }
        return itemFirst
                ? new RecipeOperation(itemNeeded, chemicalNeeded, MekanismKey.of(output), output.getAmount())
                : new RecipeOperation(chemicalNeeded, itemNeeded, MekanismKey.of(output), output.getAmount());
    }

    @Override
    protected boolean acceptsInputKey(AEKey key) {
        return key instanceof AEItemKey || key instanceof MekanismKey;
    }

    @Override
    protected boolean acceptsOutputKey(AEKey key) {
        return key instanceof MekanismKey;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_dissolution_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalDissolutionChamberMenu(containerId, inventory, worldPosition);
    }
}
