package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeAntiprotonicNucleosynthesizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeAntiprotonicNucleosynthesizerMenu extends AbstractItemChemicalToItemMeMachineMenu {
    public MeAntiprotonicNucleosynthesizerMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeAntiprotonicNucleosynthesizerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER.get(), containerId, inventory, pos,
                MeAntiprotonicNucleosynthesizerBlockEntity.class, "ME antiprotonic nucleosynthesizer");
    }
}
