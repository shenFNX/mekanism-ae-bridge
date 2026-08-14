package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.menu.AbstractSingleKeyMeMachineMenu;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Item/chemical-aware variant of the shared Mekanism-style machine screen. */
public abstract class AbstractSingleKeyMeMachineScreen<M extends AbstractSingleKeyMeMachineMenu>
        extends AbstractItemToItemMeMachineScreen<M> {
    protected AbstractSingleKeyMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        super.drawProcessingArea(graphics, left, top);
        drawChemicalSwatch(graphics, left + 42, top + 72, menu.inputChemicalRegistryId());
        drawChemicalSwatch(graphics, left + 91, top + 72, menu.outputChemicalRegistryId());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component chemicalLine = chemicalLine();
        if (chemicalLine != null) {
            graphics.drawString(font, chemicalLine, 121, 99, 0xFFE0E0E0, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isOver(mouseX, mouseY, 42, 72, 18, 18) && menu.inputChemicalRegistryId() >= 0) {
            renderChemicalTooltip(graphics, mouseX, mouseY,
                    menu.inputChemicalRegistryId(), menu.inputChemicalAmount());
        } else if (isOver(mouseX, mouseY, 91, 72, 18, 18) && menu.outputChemicalRegistryId() >= 0) {
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

    private Component chemicalLine() {
        int registryId;
        int amount;
        String translation;
        if (menu.inputChemicalRegistryId() >= 0) {
            registryId = menu.inputChemicalRegistryId();
            amount = menu.inputChemicalAmount();
            translation = "gui.mekanismae.chemical_input";
        } else if (menu.outputChemicalRegistryId() >= 0) {
            registryId = menu.outputChemicalRegistryId();
            amount = menu.outputChemicalAmount();
            translation = "gui.mekanismae.chemical_output";
        } else {
            return null;
        }
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
        return chemical == null
                ? Component.translatable("gui.mekanismae.chemical.unknown", formatAmount(amount))
                : Component.translatable(translation, chemical.getTextComponent(), formatAmount(amount));
    }

    private void renderChemicalTooltip(GuiGraphics graphics, int mouseX, int mouseY, int registryId, int amount) {
        Chemical chemical = MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
        if (chemical == null) {
            return;
        }
        graphics.renderTooltip(font, List.of(
                chemical.getTextComponent().copy().withStyle(ChatFormatting.AQUA),
                Component.literal(formatAmount(amount))), Optional.empty(), mouseX, mouseY);
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
