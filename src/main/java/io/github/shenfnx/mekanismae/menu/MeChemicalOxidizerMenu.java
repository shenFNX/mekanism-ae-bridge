package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalOxidizerBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalOxidizerMenu extends AbstractSingleKeyMeMachineMenu {
    public MeChemicalOxidizerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalOxidizerMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_OXIDIZER.get(), containerId, inventory, pos,
                MeChemicalOxidizerBlockEntity.class, "ME chemical oxidizer");
    }
}
