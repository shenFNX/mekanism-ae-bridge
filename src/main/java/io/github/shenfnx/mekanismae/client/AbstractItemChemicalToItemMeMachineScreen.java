package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.AbstractItemChemicalToItemMeMachineMenu;
import io.github.shenfnx.mekanismae.block.entity.AbstractItemChemicalToItemMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModItems;
import io.github.shenfnx.mekanismae.util.EnergyFormatter;
import appeng.client.gui.Icon;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.common.registries.MekanismItems;
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
public abstract class AbstractItemChemicalToItemMeMachineScreen<M extends AbstractItemChemicalToItemMeMachineMenu>
        extends AbstractContainerScreen<M> {
    private static final int MAIN_WIDTH = 256;
    private static final int UPGRADE_PANEL_X = 256;
    private static final int NETWORK_TAB_X = -26;
    private static final int NETWORK_TAB_Y = 55;
    private static final int RETURN_TAB_Y = 84;
    private static final int ENERGY_TAB_Y = 142;
    private static final int TIER_SLOT_X = 7;
    private static final int TIER_SLOT_Y = 28;

    private static final ResourceLocation NORMAL_SLOT =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/slot/normal.png");
    private static final ResourceLocation ENERGY_ICON =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/energy.png");
    private static final ResourceLocation BACK_ICON =
            ResourceLocation.fromNamespaceAndPath("mekanism", "gui/button/back.png");

    protected AbstractItemChemicalToItemMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 288;
        imageHeight = 217;
        inventoryLabelX = 32;
        inventoryLabelY = 123;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        drawMekanismPanel(graphics, left, top, MAIN_WIDTH, imageHeight);
        drawInset(graphics, left + 24, top + 52, 226, 67, 0xFF9A9A9A);
        drawDigitalScreen(graphics, left + 116, top + 55, 132, 61);

        // Nine processing-pattern slots across the top.
        drawSlot(graphics, left + TIER_SLOT_X, top + TIER_SLOT_Y);
        for (int index = 0; index < AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT; index++) {
            drawSlot(graphics, left + 30 + index * 18, top + 28);
        }

        // Read-only input and expected/finished output for the active operation.
        drawSlot(graphics, left + 42, top + 72);
        drawSlot(graphics, left + 91, top + 72);
        int progressWidth = Math.min(22, Math.max(0,
                menu.progress() * 22 / menu.processingTicks()));
        // The arrow is constrained to the 31 px gap between the two slots.
        graphics.fill(left + 69, top + 81, left + 82, top + 84, 0xFF555555);
        int compactProgressWidth = Math.min(12, progressWidth * 12 / 22);
        graphics.fill(left + 70, top + 82, left + 70 + compactProgressWidth, top + 83, 0xFF23C987);
        graphics.fill(left + 82, top + 80, left + 84, top + 85, 0xFF555555);
        graphics.fill(left + 84, top + 81, left + 86, top + 84, 0xFF555555);
        graphics.fill(left + 86, top + 82, left + 87, top + 83, 0xFF555555);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, left + 31 + column * 18, top + 134 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, left + 31 + column * 18, top + 192);
        }

        drawNetworkTab(graphics, left, top);
        drawReturnTab(graphics, left, top);
        drawEnergyTab(graphics, left, top);
        drawUpgradeDrawer(graphics, left, top);
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
        drawMekanismPanel(graphics, left + UPGRADE_PANEL_X, top + 4, 32, 153);
        for (int index = 0; index < AbstractItemChemicalToItemMeMachineBlockEntity.UPGRADE_SLOT_COUNT; index++) {
            int slotX = left + 262;
            int slotY = top + 8 + index * 18;
            drawAeSlot(graphics, slotX, slotY);
            if (!menu.getSlot(AbstractItemChemicalToItemMeMachineBlockEntity.PATTERN_SLOT_COUNT + index).hasItem()) {
                Icon.BACKGROUND_UPGRADE.getBlitter().dest(slotX + 1, slotY + 1).blit(graphics);
            }
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

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(NORMAL_SLOT, x, y, 0, 0, 18, 18, 18, 18);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0xFF404040, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.patterns"), 31, 17, 0xFF404040, false);
        Component currentProcess = Component.translatable("gui.mekanismae.current_process");
        graphics.drawString(font, currentProcess, 70 - font.width(currentProcess) / 2, 57, 0xFF404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFF404040, false);

        int statusColor = menu.processingFaulted() ? 0xFFFF6868
                : !menu.networkEnabled() ? 0xFFFFB347
                : menu.networkOnline() && !menu.processingFaulted() ? 0xFF53E2AC : 0xFFFF6868;
        graphics.drawString(font, menu.statusText(), 121, 57, statusColor, false);
        graphics.drawString(font, Component.translatable("gui.mekanismae.buffer_info",
                formatAmount(menu.bufferOps()), formatAmount(menu.bufferOpsCap())),
                121, 68, 0xFFE0E0E0, false);
        graphics.drawString(font, compactChemicalLine(), 121, 79, 0xFFE0E0E0, false);
        graphics.drawString(font, compactEnergyLine(), 121, 90, 0xFFE0E0E0, false);
        Component progressText = Component.literal(Math.min(100,
                menu.progress() * 100 / menu.processingTicks()) + "%");
        int progressWidth = font.width(progressText);
        int progressX = Math.max(26, Math.min(112 - progressWidth, 70 - progressWidth / 2));
        graphics.drawString(font, progressText, progressX, 96, 0xFF404040, false);
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
        } else if (isOver(mouseX, mouseY, TIER_SLOT_X, TIER_SLOT_Y, 18, 18)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.tier_slot").withStyle(ChatFormatting.AQUA),
                    upgradeAvailability(MekanismItems.BASIC_TIER_INSTALLER.get().getDescription(), 1),
                    upgradeAvailability(MekanismItems.ADVANCED_TIER_INSTALLER.get().getDescription(), 1),
                    upgradeAvailability(MekanismItems.ELITE_TIER_INSTALLER.get().getDescription(), 1),
                    upgradeAvailability(MekanismItems.ULTIMATE_TIER_INSTALLER.get().getDescription(), 1)),
                    Optional.empty(), mouseX, mouseY);
        } else if (isOver(mouseX, mouseY, UPGRADE_PANEL_X, 4, 32, 153)
                && (hoveredSlot == null || !hoveredSlot.hasItem())) {
            graphics.renderTooltip(font, List.of(
                    Component.translatable("gui.mekanismae.available_upgrades").withStyle(ChatFormatting.AQUA),
                    upgradeAvailability(ModItems.SPEED_CARD.get().getDescription(),
                            AbstractItemChemicalToItemMeMachineBlockEntity.MAX_UPGRADES_PER_TYPE),
                    upgradeAvailability(ModItems.PARALLEL_CARD.get().getDescription(),
                            AbstractItemChemicalToItemMeMachineBlockEntity.MAX_UPGRADES_PER_TYPE),
                    upgradeAvailability(ModItems.ENERGY_CARD.get().getDescription(),
                            AbstractItemChemicalToItemMeMachineBlockEntity.MAX_UPGRADES_PER_TYPE)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private Component upgradeAvailability(Component itemName, int maximum) {
        return itemName.copy().append(Component.literal(" (" + maximum + ")"));
    }

    private void drawAeSlot(GuiGraphics graphics, int x, int y) {
        Icon.SLOT_BACKGROUND.getBlitter().dest(x, y).blit(graphics);
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

    private Component compactChemicalLine() {
        int registryId = menu.chemicalRegistryId();
        if (registryId < 0 || menu.chemicalAmount() <= 0) {
            return Component.translatable("gui.mekanismae.chemical.empty");
        }
        var chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
        if (chemical == null) {
            return Component.translatable("gui.mekanismae.chemical.unknown", formatAmount(menu.chemicalAmount()));
        }
        return Component.translatable("gui.mekanismae.chemical",
                chemical.getTextComponent(), formatAmount(menu.chemicalAmount()));
    }
}
