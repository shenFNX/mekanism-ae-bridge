package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.AbstractDualKeyMeMachineMenu;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Three-slot item/chemical-aware view for the dual-key processing ledger. */
public abstract class AbstractDualKeyMeMachineScreen<M extends AbstractDualKeyMeMachineMenu>
        extends AbstractItemToItemMeMachineScreen<M> {
    protected AbstractDualKeyMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        drawSlot(graphics, left + 30, top + 72);
        drawSlot(graphics, left + 55, top + 72);
        drawSlot(graphics, left + 91, top + 72);
        graphics.fill(left + 50, top + 78, left + 52, top + 86, 0xFF555555);
        graphics.fill(left + 47, top + 81, left + 55, top + 83, 0xFF555555);
        graphics.fill(left + 75, top + 81, left + 84, top + 84, 0xFF555555);
        int progressWidth = Math.min(8, Math.max(0,
                menu.progress() * 8 / menu.processingTicks()));
        graphics.fill(left + 76, top + 82, left + 76 + progressWidth, top + 83, 0xFF23C987);
        graphics.fill(left + 84, top + 80, left + 86, top + 85, 0xFF555555);
        graphics.fill(left + 86, top + 81, left + 88, top + 84, 0xFF555555);

        drawChemicalSwatch(graphics, left + 30, top + 72, menu.firstChemicalRegistryId());
        drawChemicalSwatch(graphics, left + 55, top + 72, menu.secondChemicalRegistryId());
        drawChemicalSwatch(graphics, left + 91, top + 72, menu.outputChemicalRegistryId());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.outputChemicalRegistryId() >= 0) {
            Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(menu.outputChemicalRegistryId());
            Component output = chemical == null
                    ? Component.translatable("gui.mekanismae.chemical.unknown",
                            formatAmount(menu.outputChemicalAmount()))
                    : Component.translatable("gui.mekanismae.chemical_output",
                            chemical.getTextComponent(), formatAmount(menu.outputChemicalAmount()));
            graphics.drawString(font, output, 121, 99 + CONTENT_Y_OFFSET, 0xFFE0E0E0, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isOver(mouseX, mouseY, 30, 72 + CONTENT_Y_OFFSET, 18, 18)
                && menu.firstChemicalRegistryId() >= 0) {
            renderChemicalTooltip(graphics, mouseX, mouseY,
                    menu.firstChemicalRegistryId(), menu.firstChemicalAmount());
        } else if (isOver(mouseX, mouseY, 55, 72 + CONTENT_Y_OFFSET, 18, 18)
                && menu.secondChemicalRegistryId() >= 0) {
            renderChemicalTooltip(graphics, mouseX, mouseY,
                    menu.secondChemicalRegistryId(), menu.secondChemicalAmount());
        } else if (isOver(mouseX, mouseY, 91, 72 + CONTENT_Y_OFFSET, 18, 18)
                && menu.outputChemicalRegistryId() >= 0) {
            renderChemicalTooltip(graphics, mouseX, mouseY,
                    menu.outputChemicalRegistryId(), menu.outputChemicalAmount());
        }
    }

    private void drawChemicalSwatch(GuiGraphics graphics, int x, int y, int registryId) {
        Chemical chemical = registryId < 0 ? null : MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
        if (chemical == null) {
            return;
        }
        int color = 0xFF000000 | chemical.getTint() & 0x00FFFFFF;
        graphics.fill(x + 3, y + 3, x + 15, y + 15, 0xFF303030);
        graphics.fill(x + 4, y + 4, x + 14, y + 14, color);
        graphics.fill(x + 5, y + 5, x + 13, y + 7, lighten(color));
    }

    private void renderChemicalTooltip(GuiGraphics graphics, int mouseX, int mouseY,
            int registryId, int amount) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
        if (chemical != null) {
            graphics.renderTooltip(font, List.of(
                    chemical.getTextComponent().copy().withStyle(ChatFormatting.AQUA)),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    private boolean isOver(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + width
                && mouseY >= topPos + y && mouseY < topPos + y + height;
    }

    private static int lighten(int color) {
        int red = Math.min(255, ((color >> 16) & 0xFF) + 55);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 55);
        int blue = Math.min(255, (color & 0xFF) + 55);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    private static String formatAmount(long amount) {
        if (amount >= 1_000_000) {
            return amount % 1_000_000 == 0 ? amount / 1_000_000 + "M"
                    : String.format(Locale.ROOT, "%.2fM", amount / 1_000_000.0);
        }
        if (amount >= 1_000) {
            return amount % 1_000 == 0 ? amount / 1_000 + "k"
                    : String.format(Locale.ROOT, "%.1fk", amount / 1_000.0);
        }
        return Long.toString(Math.max(0, amount));
    }
}
