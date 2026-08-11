package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MePurificationChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MePurificationChamberScreen
        extends AbstractItemChemicalToItemMeMachineScreen<MePurificationChamberMenu> {
    public MePurificationChamberScreen(MePurificationChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
