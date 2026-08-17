package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeChemicalWasherMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeChemicalWasherScreen
        extends AbstractMultiKeyMeMachineScreen<MeChemicalWasherMenu> {
    public MeChemicalWasherScreen(
            MeChemicalWasherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
