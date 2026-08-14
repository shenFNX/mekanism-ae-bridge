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
        registration.addRecipeCatalyst(ModBlocks.ME_ENERGIZED_SMELTER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.SMELTING));
        registration.addRecipeCatalyst(ModBlocks.ME_METALLURGIC_INFUSER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.METALLURGIC_INFUSING));
        registration.addRecipeCatalyst(ModBlocks.ME_OSMIUM_COMPRESSOR_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.COMPRESSING));
        registration.addRecipeCatalyst(ModBlocks.ME_PURIFICATION_CHAMBER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.PURIFYING));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_INJECTION_CHAMBER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.INJECTING));
        registration.addRecipeCatalyst(ModBlocks.ME_COMBINER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.COMBINING));
        registration.addRecipeCatalyst(ModBlocks.ME_PRECISION_SAWMILL_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.SAWING));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_OXIDIZER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.OXIDIZING));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_CRYSTALLIZER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.CRYSTALLIZING));
        registration.addRecipeCatalyst(ModBlocks.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.NUCLEOSYNTHESIZING));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_DISSOLUTION_CHAMBER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.DISSOLUTION));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_INFUSER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.CHEMICAL_INFUSING));
        registration.addRecipeCatalyst(ModBlocks.ME_ELECTROLYTIC_SEPARATOR_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.SEPARATING));
        registration.addRecipeCatalyst(ModBlocks.ME_ROTARY_CONDENSENTRATOR_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.CONDENSENTRATING));
        registration.addRecipeCatalyst(ModBlocks.ME_ROTARY_CONDENSENTRATOR_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.DECONDENSENTRATING));
        registration.addRecipeCatalyst(ModBlocks.ME_CHEMICAL_WASHER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.WASHING));
        registration.addRecipeCatalyst(ModBlocks.ME_NUTRITIONAL_LIQUIFIER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.NUTRITIONAL_LIQUIFICATION));
        registration.addRecipeCatalyst(ModBlocks.ME_PRESSURIZED_REACTION_CHAMBER_ITEM.get(),
                MekanismJEI.genericRecipeType(RecipeViewerRecipeType.REACTION));
    }
}
