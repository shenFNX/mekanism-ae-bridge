package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeCombinerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeCombinerScreen extends AbstractMultiItemMeMachineScreen<MeCombinerMenu> {
    public MeCombinerScreen(MeCombinerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
