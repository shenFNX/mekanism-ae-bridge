package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalOxidizerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalOxidizerScreen extends AbstractSingleKeyMeMachineScreen<MeChemicalOxidizerMenu> {
    public MeChemicalOxidizerScreen(MeChemicalOxidizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
