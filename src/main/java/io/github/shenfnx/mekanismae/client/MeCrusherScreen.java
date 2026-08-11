package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeCrusherMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeCrusherScreen extends AbstractItemToItemMeMachineScreen<MeCrusherMenu> {
    public MeCrusherScreen(MeCrusherMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
