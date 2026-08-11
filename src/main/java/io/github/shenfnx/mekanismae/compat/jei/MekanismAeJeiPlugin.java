package io.github.shenfnx.mekanismae.compat.jei;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import mekanism.client.recipe_viewer.jei.MekanismJEI;
import mekanism.client.recipe_viewer.type.RecipeViewerRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class MekanismAeJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MekanismAeMod.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        if (!MekanismJEI.shouldLoad()) {
            return;
        }
        registration.addRecipeCatalyst(ModBlocks.ME_ENRICHMENT_CHAMBER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.ENRICHING));
        registration.addRecipeCatalyst(ModBlocks.ME_CRUSHER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.CRUSHING));
        registration.addRecipeCatalyst(ModBlocks.ME_METALLURGIC_INFUSER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.METALLURGIC_INFUSING));
    }
}
