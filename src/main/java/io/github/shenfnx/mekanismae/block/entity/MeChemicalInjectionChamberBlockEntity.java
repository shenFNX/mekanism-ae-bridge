package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeChemicalInjectionChamberMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class MeChemicalInjectionChamberBlockEntity extends AbstractItemChemicalToItemMeMachineBlockEntity {
    public MeChemicalInjectionChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_CHEMICAL_INJECTION_CHAMBER.get(), pos, state,
                ModBlocks.ME_CHEMICAL_INJECTION_CHAMBER_ITEM.get(), MachineType.ME_CHEMICAL_INJECTION_CHAMBER,
                MekanismRecipeTypes.TYPE_INJECTING.get(), "ME chemical injection chamber");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_chemical_injection_chamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeChemicalInjectionChamberMenu(containerId, inventory, worldPosition);
    }
}
