package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeMetallurgicInfuserMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeMetallurgicInfuserScreen
        extends AbstractItemChemicalToItemMeMachineScreen<MeMetallurgicInfuserMenu> {
    public MeMetallurgicInfuserScreen(MeMetallurgicInfuserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
