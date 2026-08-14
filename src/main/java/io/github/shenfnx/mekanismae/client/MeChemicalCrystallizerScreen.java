package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalCrystallizerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalCrystallizerScreen
        extends AbstractSingleKeyMeMachineScreen<MeChemicalCrystallizerMenu> {
    public MeChemicalCrystallizerScreen(MeChemicalCrystallizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
