package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeElectrolyticSeparatorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeElectrolyticSeparatorScreen
        extends AbstractMultiKeyMeMachineScreen<MeElectrolyticSeparatorMenu> {
    public MeElectrolyticSeparatorScreen(
            MeElectrolyticSeparatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Diagram diagram() {
        return Diagram.ELECTROLYSIS;
    }
}
