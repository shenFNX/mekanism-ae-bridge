package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeNutritionalLiquifierMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class MeNutritionalLiquifierScreen
        extends AbstractMultiKeyMeMachineScreen<MeNutritionalLiquifierMenu> {
    public MeNutritionalLiquifierScreen(
            MeNutritionalLiquifierMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }
}
