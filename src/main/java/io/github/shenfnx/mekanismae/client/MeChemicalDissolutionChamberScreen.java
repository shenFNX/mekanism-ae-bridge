package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalDissolutionChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalDissolutionChamberScreen
        extends AbstractDualKeyMeMachineScreen<MeChemicalDissolutionChamberMenu> {
    public MeChemicalDissolutionChamberScreen(
            MeChemicalDissolutionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
