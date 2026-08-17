package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MePressurizedReactionChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MePressurizedReactionChamberScreen
        extends AbstractMultiKeyMeMachineScreen<MePressurizedReactionChamberMenu> {
    public MePressurizedReactionChamberScreen(
            MePressurizedReactionChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
