package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.block.entity.AbstractMultiKeyMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.menu.AbstractMultiKeyMeMachineMenu;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Item/fluid/chemical-aware screen with distinct process diagrams for each machine. */
public abstract class AbstractMultiKeyMeMachineScreen<M extends AbstractMultiKeyMeMachineMenu>
        extends AbstractItemToItemMeMachineScreen<M> {
    protected AbstractMultiKeyMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract Diagram diagram();

    @Override
    protected final void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            int x = menu.displaySlotX(slot);
            int y = menu.displaySlotY(slot);
            if (x >= 0) {
                drawSlot(graphics, left + x, top + y);
            }
        }
        switch (diagram()) {
            case ELECTROLYSIS -> drawElectrolysis(graphics, left, top);
            case ROTARY -> drawRotary(graphics, left, top);
            case WASHER -> drawWasher(graphics, left, top);
            case NUTRITIONAL -> drawNutritional(graphics, left, top);
            case REACTION -> drawReaction(graphics, left, top);
        }
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            int x = menu.displaySlotX(slot);
            int y = menu.displaySlotY(slot);
            if (x >= 0) {
                drawResourceSwatch(graphics, left + x, top + y,
                        menu.resourceType(slot), menu.resourceRegistryId(slot));
            }
        }
    }

    private void drawElectrolysis(GuiGraphics graphics, int left, int top) {
        line(graphics, left + 61, top + 81, left + 70, top + 84, 0xFF187C9B);
        line(graphics, left + 69, top + 70, left + 72, top + 95, 0xFF4A4F52);
        line(graphics, left + 75, top + 66, left + 78, top + 99, 0xFF4A4F52);
        line(graphics, left + 78, top + 72, left + 91, top + 74, 0xFF20B9D2);
        line(graphics, left + 78, top + 91, left + 91, top + 94, 0xFFAF67D5);
    }

    private void drawRotary(GuiGraphics graphics, int left, int top) {
        graphics.fill(left + 64, top + 72, left + 85, top + 93, 0xFF4B5054);
        graphics.fill(left + 67, top + 75, left + 82, top + 90, 0xFF202426);
        graphics.fill(left + 72, top + 77, left + 77, top + 88, 0xFF21B8C7);
        graphics.fill(left + 69, top + 80, left + 80, top + 85, 0xFF21B8C7);
        line(graphics, left + 60, top + 81, left + 64, top + 84, 0xFF696F73);
        line(graphics, left + 85, top + 81, left + 91, top + 84, 0xFF696F73);
    }

    private void drawWasher(GuiGraphics graphics, int left, int top) {
        line(graphics, left + 50, top + 73, left + 62, top + 78, 0xFF2396BA);
        line(graphics, left + 50, top + 93, left + 62, top + 88, 0xFF8C55A6);
        graphics.fill(left + 62, top + 72, left + 84, top + 95, 0xFF555B5E);
        graphics.fill(left + 65, top + 75, left + 81, top + 92, 0xFF1D5967);
        graphics.fill(left + 67, top + 85, left + 79, top + 90, 0xFF27BCD0);
        graphics.fill(left + 69, top + 78, left + 71, top + 80, 0xFFB8F4FF);
        graphics.fill(left + 75, top + 81, left + 77, top + 83, 0xFFB8F4FF);
        line(graphics, left + 84, top + 81, left + 91, top + 84, 0xFF696F73);
    }

    private void drawNutritional(GuiGraphics graphics, int left, int top) {
        line(graphics, left + 60, top + 81, left + 67, top + 84, 0xFF69716A);
        graphics.fill(left + 66, top + 72, left + 83, top + 94, 0xFF4C5350);
        graphics.fill(left + 69, top + 76, left + 80, top + 90, 0xFF354B39);
        graphics.fill(left + 73, top + 77, left + 76, top + 89, 0xFF9EC957);
        graphics.fill(left + 70, top + 81, left + 79, top + 84, 0xFF9EC957);
        line(graphics, left + 83, top + 78, left + 91, top + 74, 0xFF6AAA45);
        line(graphics, left + 83, top + 88, left + 91, top + 94, 0xFF8B8F83);
    }

    private void drawReaction(GuiGraphics graphics, int left, int top) {
        line(graphics, left + 46, top + 70, left + 62, top + 78, 0xFF6C7174);
        line(graphics, left + 46, top + 94, left + 62, top + 88, 0xFF258DA8);
        line(graphics, left + 70, top + 81, left + 78, top + 84, 0xFF925DAA);
        graphics.fill(left + 60, top + 72, left + 82, top + 95, 0xFF565B60);
        graphics.fill(left + 64, top + 76, left + 78, top + 91, 0xFF25292C);
        graphics.fill(left + 67, top + 79, left + 75, top + 88, 0xFFB45A47);
        graphics.fill(left + 69, top + 80, left + 73, top + 83, 0xFFF0B46D);
        line(graphics, left + 82, top + 78, left + 91, top + 74, 0xFF6C7174);
        line(graphics, left + 82, top + 88, left + 91, top + 94, 0xFF925DAA);
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int deltaX = x1 - x0;
        int deltaY = y1 - y0;
        int steps = Math.max(Math.abs(deltaX), Math.abs(deltaY));
        for (int step = 0; step <= steps; step++) {
            int x = x0 + Math.round(deltaX * (step / (float) Math.max(1, steps)));
            int y = y0 + Math.round(deltaY * (step / (float) Math.max(1, steps)));
            graphics.fill(x, y, x + 2, y + 2, color);
        }
    }

    private void drawResourceSwatch(GuiGraphics graphics, int x, int y, int type, int registryId) {
        int color;
        if (type == AbstractMultiKeyMeMachineBlockEntity.RESOURCE_CHEMICAL) {
            Chemical chemical = registryId < 0 ? null : MekanismAPI.CHEMICAL_REGISTRY.byId(registryId);
            if (chemical == null) {
                return;
            }
            color = 0xFF000000 | chemical.getTint() & 0x00FFFFFF;
        } else if (type == AbstractMultiKeyMeMachineBlockEntity.RESOURCE_FLUID) {
            Fluid fluid = registryId < 0 ? null : BuiltInRegistries.FLUID.byId(registryId);
            if (fluid == null) {
                return;
            }
            color = IClientFluidTypeExtensions.of(fluid).getTintColor();
            color = 0xFF000000 | color & 0x00FFFFFF;
        } else {
            return;
        }
        graphics.fill(x + 3, y + 3, x + 15, y + 15, 0xFF303030);
        graphics.fill(x + 4, y + 4, x + 14, y + 14, color);
        graphics.fill(x + 5, y + 5, x + 13, y + 7, lighten(color));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            int x = menu.displaySlotX(slot);
            int y = menu.displaySlotY(slot);
            if (x >= 0 && isOver(mouseX, mouseY, x, y, 18, 18)) {
                renderResourceTooltip(graphics, mouseX, mouseY, slot);
                return;
            }
        }
    }

    private void renderResourceTooltip(GuiGraphics graphics, int mouseX, int mouseY, int slot) {
        int type = menu.resourceType(slot);
        int id = menu.resourceRegistryId(slot);
        Component name = null;
        if (type == AbstractMultiKeyMeMachineBlockEntity.RESOURCE_CHEMICAL) {
            Chemical chemical = id < 0 ? null : MekanismAPI.CHEMICAL_REGISTRY.byId(id);
            name = chemical == null ? null : chemical.getTextComponent();
        } else if (type == AbstractMultiKeyMeMachineBlockEntity.RESOURCE_FLUID) {
            Fluid fluid = id < 0 ? null : BuiltInRegistries.FLUID.byId(id);
            name = fluid == null ? null : fluid.getFluidType().getDescription();
        }
        if (name != null) {
            graphics.renderTooltip(font, List.of(
                    name.copy().withStyle(ChatFormatting.AQUA),
                    Component.literal(formatAmount(menu.resourceAmount(slot)) + " mB")),
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

    protected enum Diagram {
        ELECTROLYSIS,
        ROTARY,
        WASHER,
        NUTRITIONAL,
        REACTION
    }
}
