package io.github.shenfnx.mekanismae.client;

import guideme.Guide;
import io.github.shenfnx.mekanismae.MekanismAeMod;
import net.minecraft.resources.ResourceLocation;

/** Registers the GuideME content used by the machine screens and GuideME hotkey. */
public final class MekanismAeGuide {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(MekanismAeMod.MOD_ID, "guide");

    private static final Guide GUIDE = Guide.builder(ID)
            .folder("guide")
            .build();

    private MekanismAeGuide() {
    }

    public static void initialize() {
        // Loading this class registers the static guide with GuideME.
    }

    public static Guide getGuide() {
        return GUIDE;
    }
}
