package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.AbstractMultiItemMeMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Two-row internal ledger: two isolated inputs on the left and two outputs on the right. */
public abstract class AbstractMultiItemMeMachineScreen<M extends AbstractMultiItemMeMachineMenu>
        extends AbstractItemToItemMeMachineScreen<M> {
    protected AbstractMultiItemMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        drawSlot(graphics, left + 34, top + 62);
        drawSlot(graphics, left + 34, top + 80);
        drawSlot(graphics, left + 91, top + 62);
        drawSlot(graphics, left + 91, top + 80);
        int progressWidth = Math.min(26, Math.max(0, menu.progress() * 26 / menu.processingTicks()));
        graphics.fill(left + 55, top + 75, left + 84, top + 84, 0xFF555555);
        graphics.fill(left + 56, top + 76, left + 56 + progressWidth, top + 83, 0xFF23C987);
        graphics.fill(left + 84, top + 73, left + 87, top + 86, 0xFF555555);
        graphics.fill(left + 87, top + 76, left + 90, top + 83, 0xFF555555);
    }

    @Override
    protected int progressLabelX() {
        return 57;
    }
}
