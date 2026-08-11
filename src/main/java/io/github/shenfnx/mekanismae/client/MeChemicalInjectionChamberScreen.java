package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalInjectionChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalInjectionChamberScreen
        extends AbstractItemChemicalToItemMeMachineScreen<MeChemicalInjectionChamberMenu> {
    public MeChemicalInjectionChamberScreen(MeChemicalInjectionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
