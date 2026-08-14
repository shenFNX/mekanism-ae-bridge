package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalInfuserMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalInfuserScreen extends AbstractDualKeyMeMachineScreen<MeChemicalInfuserMenu> {
    public MeChemicalInfuserScreen(MeChemicalInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
