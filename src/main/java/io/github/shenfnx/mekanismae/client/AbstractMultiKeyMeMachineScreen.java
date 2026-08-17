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

/** Item/fluid/chemical-aware screen with a compact shared processing arrow. */
public abstract class AbstractMultiKeyMeMachineScreen<M extends AbstractMultiKeyMeMachineMenu>
        extends AbstractItemToItemMeMachineScreen<M> {
    protected AbstractMultiKeyMeMachineScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected final void drawProcessingArea(GuiGraphics graphics, int left, int top) {
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            int x = menu.displaySlotX(slot);
            int y = menu.displaySlotY(slot);
            if (x >= 0) {
                drawSlot(graphics, left + x, top + y);
            }
        }
        drawProcessingArrow(graphics, left, top, arrowStartX(), arrowEndX());
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            int x = menu.displaySlotX(slot);
            int y = menu.displaySlotY(slot);
            if (x >= 0) {
                drawResourceSwatch(graphics, left + x, top + y,
                        menu.resourceType(slot), menu.resourceRegistryId(slot));
            }
        }
    }

    private int arrowStartX() {
        int rightmostInput = 0;
        for (int slot = 0; slot < menu.inputSlotCount(); slot++) {
            rightmostInput = Math.max(rightmostInput, menu.displaySlotX(slot) + 18);
        }
        return rightmostInput + 3;
    }

    private int arrowEndX() {
        int leftmostOutput = Integer.MAX_VALUE;
        for (int output = 0; output < menu.outputSlotCount(); output++) {
            int slot = AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS + output;
            leftmostOutput = Math.min(leftmostOutput, menu.displaySlotX(slot));
        }
        return leftmostOutput - 3;
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
}
