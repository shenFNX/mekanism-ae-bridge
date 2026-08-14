package io.github.shenfnx.mekanismae.block.entity;

import io.github.shenfnx.mekanismae.config.MachineType;
import io.github.shenfnx.mekanismae.menu.MeAntiprotonicNucleosynthesizerMenu;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.api.recipes.MekanismRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class MeAntiprotonicNucleosynthesizerBlockEntity
        extends AbstractItemChemicalToItemMeMachineBlockEntity {
    public MeAntiprotonicNucleosynthesizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER.get(), pos, state,
                ModBlocks.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER_ITEM.get(),
                MachineType.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER,
                MekanismRecipeTypes.TYPE_NUCLEOSYNTHESIZING.get(),
                "ME antiprotonic nucleosynthesizer");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.mekanismae.me_antiprotonic_nucleosynthesizer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MeAntiprotonicNucleosynthesizerMenu(containerId, inventory, worldPosition);
    }
}
