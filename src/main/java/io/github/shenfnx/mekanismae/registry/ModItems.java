package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import net.minecraft.world.item.Item;
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

    private ModItems() {
    }
}
