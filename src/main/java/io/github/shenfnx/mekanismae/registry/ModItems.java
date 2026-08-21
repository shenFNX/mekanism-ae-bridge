package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.compat.appliedflux.AppliedFluxCompat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekanismAeMod.MOD_ID);

    public static final DeferredItem<Item> SPEED_CARD = ITEMS.registerSimpleItem(
            "speed_card", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> PARALLEL_CARD = ITEMS.registerSimpleItem(
            "parallel_card", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> ENERGY_CARD = ITEMS.registerSimpleItem(
            "energy_card", new Item.Properties().stacksTo(64));

    public static boolean isMachineUpgrade(ItemStack stack) {
        return stack.is(SPEED_CARD.get()) || stack.is(PARALLEL_CARD.get())
                || stack.is(ENERGY_CARD.get()) || AppliedFluxCompat.isInductionCard(stack);
    }

    public static int getMachineUpgradeLimit(ItemStack stack) {
        return AppliedFluxCompat.isInductionCard(stack)
                ? AppliedFluxCompat.MAX_INDUCTION_CARDS : 8;
    }

    private ModItems() {
    }
}
