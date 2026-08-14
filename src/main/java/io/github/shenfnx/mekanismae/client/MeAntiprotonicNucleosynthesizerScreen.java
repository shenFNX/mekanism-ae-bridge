package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeAntiprotonicNucleosynthesizerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeAntiprotonicNucleosynthesizerScreen
        extends AbstractItemChemicalToItemMeMachineScreen<MeAntiprotonicNucleosynthesizerMenu> {
    public MeAntiprotonicNucleosynthesizerScreen(
            MeAntiprotonicNucleosynthesizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
