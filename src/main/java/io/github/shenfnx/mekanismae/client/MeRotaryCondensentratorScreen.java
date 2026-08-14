package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeRotaryCondensentratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeRotaryCondensentratorScreen
        extends AbstractMultiKeyMeMachineScreen<MeRotaryCondensentratorMenu> {
    public MeRotaryCondensentratorScreen(
            MeRotaryCondensentratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Diagram diagram() {
        return Diagram.ROTARY;
    }
}
