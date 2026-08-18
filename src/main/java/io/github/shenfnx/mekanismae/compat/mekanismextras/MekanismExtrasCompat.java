package io.github.shenfnx.mekanismae.compat.mekanismextras;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import mekanism.api.tier.BaseTier;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.registries.MekanismItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Optional registry-id based integration for Mekanism Extras tier installers. */
public final class MekanismExtrasCompat {
    private static final String MOD_ID = "mekanism_extras";
    private static final List<String> EXTRA_TIER_INSTALLER_PATHS = List.of(
            "absolute_tier_installer",
            "supreme_tier_installer",
            "cosmic_tier_installer",
            "infinite_tier_installer");
    private static final Map<String, Integer> EXTRA_TIER_INDICES = Map.of(
            "absolute_tier_installer", 4,
            "supreme_tier_installer", 5,
            "cosmic_tier_installer", 6,
            "infinite_tier_installer", 7);

    private MekanismExtrasCompat() {
    }

    /** Returns the unified tier index, or -1 for an unsupported item. */
    public static int getTierIndex(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        if (stack.getItem() instanceof ItemTierInstaller installer) {
            return vanillaTierIndex(installer.getToTier());
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null && MOD_ID.equals(id.getNamespace())) {
            return EXTRA_TIER_INDICES.getOrDefault(id.getPath(), -1);
        }
        return -1;
    }

    public static boolean isTierInstaller(ItemStack stack) {
        return getTierIndex(stack) >= 0;
    }

    /**
     * Lists the vanilla installers and appends Mekanism Extras installers only
     * when that optional mod has registered them.
     */
    public static List<Item> availableTierInstallers() {
        List<Item> result = new ArrayList<>(List.of(
                MekanismItems.BASIC_TIER_INSTALLER.get(),
                MekanismItems.ADVANCED_TIER_INSTALLER.get(),
                MekanismItems.ELITE_TIER_INSTALLER.get(),
                MekanismItems.ULTIMATE_TIER_INSTALLER.get()));
        for (String path : EXTRA_TIER_INSTALLER_PATHS) {
            Item item = BuiltInRegistries.ITEM.get(
                    ResourceLocation.fromNamespaceAndPath(MOD_ID, path));
            if (item != null && item != Items.AIR) {
                result.add(item);
            }
        }
        return List.copyOf(result);
    }

    private static int vanillaTierIndex(BaseTier tier) {
        return switch (tier) {
            case BASIC -> 0;
            case ADVANCED -> 1;
            case ELITE -> 2;
            case ULTIMATE -> 3;
            default -> -1;
        };
    }
}
