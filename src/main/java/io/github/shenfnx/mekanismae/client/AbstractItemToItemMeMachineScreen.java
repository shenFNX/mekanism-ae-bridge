package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeProcessingMachineMenu;
import io.github.shenfnx.mekanismae.block.entity.AbstractItemToItemMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModItems;
import io.github.shenfnx.mekanismae.util.EnergyFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Mekanism-style status screen. Processing resources stay internal, so the
 * only real machine slots are the encoded pattern and the upgrade drawer.
 */
public abstract class AbstractItemToItemMeMachineScreen<M extends net.minecraft.world.inventory.AbstractContainerMenu
        & MeProcessingMachineMenu>
        extends AbstractContainerScreen<M> {
    private static final int MAIN_WIDTH = 256;
    private static final int UPGRADE_PANEL_X = 256;
    private static final int NETWORK_TAB_X = -26;
    private static final int NETWORK_TAB_Y = 55;
    private static final int RETURN_TAB_Y = 84;
    private static final int ENERGY_TAB_Y = 142;

    private static final ResourceLocation NORMAL_SLOT =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/slot/normal.png");
    private static final ResourceLocation ENERGY_ICON =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/energy.png");
    private static final ResourceLocation UPGRADE_ICON =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/upgrade.png");
    private static final ResourceLocation BACK_ICON =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/button/back.png");

    protected AbstractItemToItemMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 304;
        imageHeight = 199;
        inventoryLabelX = 32;
        inventoryLabelY = 105;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        drawMekanismPanel(graphics, left, top, MAIN_WIDTH, imageHeight);
        drawInset(graphics, left + 24, top + 52, 226, 49, 0xFF9A9A9A);
        drawDigitalScreen(graphics, left + 116, top + 55, 132, 43);

        // Nine processing-pattern slots across the top.
        for (int index = 0; index < AbstractItemToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT; index++) {
            drawSlot(graphics, left + 30 + index * 18, top + 28);
        }

        drawProcessingArea(graphics, left, top);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, left + 31 + column * 18, top + 116 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, left + 31 + column * 18, top + 174);
        }

        drawNetworkTab(graphics, left, top);
        drawReturnTab(graphics, left, top);
        drawEnergyTab(graphics, left, top);
        drawUpgradeDrawer(graphics, left, top);
    }

    /** Draws the read-only internal processing ledger. Multi-input/output screens override this. */
    protected void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        drawSlot(graphics, left + 42, top + 69);
        drawSlot(graphics, left + 91, top + 69);
        int progressWidth = Math.min(22, Math.max(0,
                menu.progress() * 22 / menu.processingTicks()));
        // The arrow is constrained to the 31 px gap between the two slots.
        graphics.fill(left + 62, top + 75, left + 86, top + 84, 0xFF555555);
        graphics.fill(left + 63, top + 76, left + 63 + progressWidth, top + 83, 0xFF23C987);
        graphics.fill(left + 86, top + 73, left + 88, top + 86, 0xFF555555);
        graphics.fill(left + 88, top + 75, left + 90, top + 84, 0xFF555555);
        graphics.fill(left + 90, top + 77, left + 91, top + 82, 0xFF555555);

    }

    private void drawNetworkTab(GuiGraphics graphics, int left, int top) {
        int color = !menu.networkEnabled() ? 0xFFE39B32
                : menu.networkOnline() ? 0xFF20D890 : 0xFFE05252;
        drawSideTab(graphics, left + NETWORK_TAB_X, top + NETWORK_TAB_Y, 26, 26, color);
        graphics.drawString(font, "ME", left + NETWORK_TAB_X + 6, top + NETWORK_TAB_Y + 7, 0xFF303030, false);
        graphics.fill(left + NETWORK_TAB_X + 3, top + NETWORK_TAB_Y + 21,
                left + NETWORK_TAB_X + 23, top + NETWORK_TAB_Y + 23, color);
    }

    private void drawReturnTab(GuiGraphics graphics, int left, int top) {
        drawSideTab(graphics, left + NETWORK_TAB_X, top + RETURN_TAB_Y, 26, 26, 0xFF8D969C);
        graphics.blit(BACK_ICON, left + NETWORK_TAB_X + 6, top + RETURN_TAB_Y + 5,
                0, 0, 14, 14, 14, 14);
    }

    private void drawEnergyTab(GuiGraphics graphics, int left, int top) {
        drawSideTab(graphics, left + NETWORK_TAB_X, top + ENERGY_TAB_Y, 26, 34, 0xFF8D969C);
        graphics.blit(ENERGY_ICON, left + NETWORK_TAB_X + 4, top + ENERGY_TAB_Y + 3,
                0, 0, 18, 18, 18, 18);
        graphics.fill(left + NETWORK_TAB_X + 4, top + ENERGY_TAB_Y + 25,
                left + NETWORK_TAB_X + 22, top + ENERGY_TAB_Y + 30, 0xFF303030);
        int energyWidth = Math.min(16, Math.max(0, menu.energy() * 16 / menu.maxEnergy()));
        graphics.fill(left + NETWORK_TAB_X + 5, top + ENERGY_TAB_Y + 26,
                left + NETWORK_TAB_X + 5 + energyWidth, top + ENERGY_TAB_Y + 29, 0xFF2DE0A0);
    }

    private void drawUpgradeDrawer(GuiGraphics graphics, int left, int top) {
        drawSideTab(graphics, left + UPGRADE_PANEL_X, top + 4, 24, 24, 0xFF8D969C);
        graphics.blit(UPGRADE_ICON, left + UPGRADE_PANEL_X + 3, top + 7,
                0, 0, 18, 18, 18, 18);
        drawMekanismPanel(graphics, left + UPGRADE_PANEL_X, top + 28, 48, 81);
        for (int index = 0; index < AbstractItemToItemMeMachineBlockEntity.UPGRADE_SLOT_COUNT; index++) {
            drawSlot(graphics, left + 262 + (index % 2) * 18, top + 32 + (index / 2) * 18);
        }
    }

    private void drawMekanismPanel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF3F3F3F);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFF2F2F2);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFFC6C6C6);
    }

    private void drawInset(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFE8E8E8);
        graphics.fill(x + 2, y + 2, x + width - 2, y + height - 2, color);
    }

    private void drawDigitalScreen(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xFF4E4E4E);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF24282A);
    }

    private void drawSideTab(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, 0xFF303030);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFF0F0F0);
        graphics.fill(x + 3, y + 3, x + width - 2, y + height - 3, 0xFFAAAAAA);
        graphics.fill(x + 3, y + 3, x + 5, y + height - 3, accent);
    }

    protected final void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(NORMAL_SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.patterns"), 31, 17, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.current_process"), 31, 56, 0xFF404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);

        int statusColor = menu.processingFaulted() ? 0xFFFF6868
                : !menu.networkEnabled() ? 0xFFFFB347
                : menu.networkOnline() && !menu.processingFaulted() ? 0xFF53E2AC : 0xFFFF6868;
        graphics.drawString(font, menu.statusText(), 121, 59, statusColor, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.buffer_info",
                formatAmount(menu.bufferOps()), formatAmount(menu.bufferOpsCap())),
                121, 72, 0xFFE0E0E0, false);
        graphics.drawString(font, compactEnergyLine(), 121, 85, 0xFFE0E0E0, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.progress",
                Math.min(100, menu.progress() * 100 / menu.processingTicks())),
                progressLabelX(), progressLabelY(), 0xFF404040, false);
    }

    protected int progressLabelX() {
        return 43;
    }

    protected int progressLabelY() {
        return 89;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCustomTooltip(graphics, mouseX, mouseY);
    }

    private void renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isOver(mouseX, mouseY, NETWORK_TAB_X, NETWORK_TAB_Y, 26, 26)) {
            Component state = !menu.networkEnabled()
                    ? Component.translatable("gui.mekanismae.network.disabled")
                    : menu.networkOnline()
                            ? Component.translatable("gui.mekanismae.network.online")
                            : Component.translatable("gui.mekanismae.network.offline");
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.network_switch").withStyle(ChatFormatting.AQUA),
                    state,
                    Component.translatable("gui.mekanismae.network_switch.hint").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, NETWORK_TAB_X, RETURN_TAB_Y, 26, 26)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.return_resources").withStyle(ChatFormatting.AQUA),
                    Component.translatable("gui.mekanismae.return_resources.hint").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, NETWORK_TAB_X, ENERGY_TAB_Y, 26, 34)) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.energy_storage").withStyle(ChatFormatting.AQUA),
                    Component.literal(String.format(Locale.ROOT, "%,d / %,d FE", menu.energy(), menu.maxEnergy())),
                    Component.translatable("gui.mekanismae.energy_receive",
                            String.format(Locale.ROOT, "%,d", menu.maxEnergyReceive())),
                    Component.translatable("gui.mekanismae.energy_storage.hint").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, UPGRADE_PANEL_X, 4, 48, 105)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.available_upgrades").withStyle(ChatFormatting.AQUA),
                    upgradeLine(ModItems.SPEED_CARD.get().getDescription(), menu.speedUpgrades(),
                            menu.speedMultiplier()),
                    upgradeLine(ModItems.PARALLEL_CARD.get().getDescription(), menu.parallelUpgrades(),
                            menu.parallelMultiplier()),
                    upgradeLine(ModItems.ENERGY_CARD.get().getDescription(), menu.energyUpgrades()),
                    Component.translatable("gui.mekanismae.upgrades.hint").withStyle(ChatFormatting.GRAY)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private Component upgradeLine(Component itemName, int installed) {
        return Component.literal("  ").append(itemName.copy())
                .append(Component.literal(" (" + installed + "/"
                        + AbstractItemToItemMeMachineBlockEntity.MAX_UPGRADES_PER_TYPE + ")"));
    }

    private Component upgradeLine(Component itemName, int installed, int multiplier) {
        return Component.literal("  ").append(itemName.copy())
                .append(Component.literal(" (" + installed + "/"
                        + AbstractItemToItemMeMachineBlockEntity.MAX_UPGRADES_PER_TYPE
                        + ", x" + multiplier + ")"));
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && minecraft != null && minecraft.gameMode != null) {
            if (isOver(mouseX, mouseY, NETWORK_TAB_X, NETWORK_TAB_Y, 26, 26)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 1);
                return true;
            }
            if (isOver(mouseX, mouseY, NETWORK_TAB_X, RETURN_TAB_Y, 26, 26)) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private String formatEnergy(int energy) {
        return EnergyFormatter.format(energy);
    }

    private String formatAmount(int amount) {
        if (amount >= 1_000_000) {
            return amount % 1_000_000 == 0
                    ? (amount / 1_000_000) + "M"
                    : String.format(Locale.ROOT, "%.2fM", amount / 1_000_000.0);
        }
        if (amount >= 1_000) {
            return amount % 1_000 == 0
                    ? (amount / 1_000) + "k"
                    : String.format(Locale.ROOT, "%.1fk", amount / 1_000.0);
        }
        return Integer.toString(amount);
    }

    private Component compactEnergyLine() {
        String stored = formatEnergy(menu.energy());
        String capacity = formatEnergy(menu.maxEnergy());
        Component translated = Component.translatable("gui.mekanismae.energy_compact", stored, capacity);
        if (font.width(translated) <= 122) {
            return translated;
        }
        return Component.literal(stored + "/" + capacity + " FE");
    }
}
