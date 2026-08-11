package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeChemicalInjectionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalInjectionChamberMenu extends AbstractItemChemicalToItemMeMachineMenu {
    public MeChemicalInjectionChamberMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeChemicalInjectionChamberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_CHEMICAL_INJECTION_CHAMBER.get(), containerId, inventory, pos,
                MeChemicalInjectionChamberBlockEntity.class, "ME chemical injection chamber");
    }
}
