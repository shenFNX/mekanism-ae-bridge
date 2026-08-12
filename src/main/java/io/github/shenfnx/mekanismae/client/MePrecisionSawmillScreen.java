package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MePrecisionSawmillMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MePrecisionSawmillScreen extends AbstractMultiItemMeMachineScreen<MePrecisionSawmillMenu> {
    public MePrecisionSawmillScreen(MePrecisionSawmillMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
