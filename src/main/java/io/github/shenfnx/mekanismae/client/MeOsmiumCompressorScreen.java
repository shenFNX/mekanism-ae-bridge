package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeOsmiumCompressorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeOsmiumCompressorScreen
        extends AbstractItemChemicalToItemMeMachineScreen<MeOsmiumCompressorMenu> {
    public MeOsmiumCompressorScreen(MeOsmiumCompressorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
