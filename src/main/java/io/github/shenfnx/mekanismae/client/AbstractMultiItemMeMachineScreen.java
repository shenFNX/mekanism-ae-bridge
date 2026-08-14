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
        drawSlot(graphics, left + 34, top + 68);
        drawSlot(graphics, left + 34, top + 86);
        drawSlot(graphics, left + 91, top + 68);
        drawSlot(graphics, left + 91, top + 86);
        int progressWidth = Math.min(26, Math.max(0, menu.progress() * 26 / menu.processingTicks()));
        graphics.fill(left + 62, top + 84, left + 78, top + 87, 0xFF555555);
        int compactProgressWidth = Math.min(15, progressWidth * 15 / 26);
        graphics.fill(left + 63, top + 85, left + 63 + compactProgressWidth, top + 86, 0xFF23C987);
        graphics.fill(left + 78, top + 83, left + 80, top + 88, 0xFF555555);
        graphics.fill(left + 80, top + 84, left + 82, top + 87, 0xFF555555);
        graphics.fill(left + 82, top + 85, left + 83, top + 86, 0xFF555555);
    }

    @Override
    protected int progressLabelY() {
        return 108;
    }
}
