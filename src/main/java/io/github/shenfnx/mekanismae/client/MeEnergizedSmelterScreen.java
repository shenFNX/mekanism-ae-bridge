package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeEnergizedSmelterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeEnergizedSmelterScreen extends AbstractItemToItemMeMachineScreen<MeEnergizedSmelterMenu> {
    public MeEnergizedSmelterScreen(MeEnergizedSmelterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
