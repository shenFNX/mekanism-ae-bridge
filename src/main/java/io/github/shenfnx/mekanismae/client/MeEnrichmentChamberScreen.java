package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeEnrichmentChamberScreen
        extends AbstractItemToItemMeMachineScreen<MeEnrichmentChamberMenu> {
    public MeEnrichmentChamberScreen(MeEnrichmentChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
