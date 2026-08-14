package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalWasherBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalWasherMenu extends AbstractMultiKeyMeMachineMenu {
    public MeChemicalWasherMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalWasherMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_WASHER.get(), containerId, inventory, pos,
                MeChemicalWasherBlockEntity.class, "ME chemical washer");
    }
}
