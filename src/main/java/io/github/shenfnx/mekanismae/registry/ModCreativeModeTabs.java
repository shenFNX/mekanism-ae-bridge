package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MekanismAeMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
            "main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("mod.mekanismae.name"))
                    .icon(() -> ModBlocks.ME_ENRICHMENT_CHAMBER_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.ME_ENRICHMENT_CHAMBER_ITEM.get());
                        output.accept(ModBlocks.ME_CRUSHER_ITEM.get());
                        output.accept(ModBlocks.ME_ENERGIZED_SMELTER_ITEM.get());
                        output.accept(ModBlocks.ME_METALLURGIC_INFUSER_ITEM.get());
                        output.accept(ModBlocks.ME_OSMIUM_COMPRESSOR_ITEM.get());
                        output.accept(ModBlocks.ME_PURIFICATION_CHAMBER_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_INJECTION_CHAMBER_ITEM.get());
                        output.accept(ModBlocks.ME_COMBINER_ITEM.get());
                        output.accept(ModBlocks.ME_PRECISION_SAWMILL_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_OXIDIZER_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_CRYSTALLIZER_ITEM.get());
                        output.accept(ModBlocks.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_DISSOLUTION_CHAMBER_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_INFUSER_ITEM.get());
                        output.accept(ModBlocks.ME_ELECTROLYTIC_SEPARATOR_ITEM.get());
                        output.accept(ModBlocks.ME_ROTARY_CONDENSENTRATOR_ITEM.get());
                        output.accept(ModBlocks.ME_CHEMICAL_WASHER_ITEM.get());
                        output.accept(ModBlocks.ME_NUTRITIONAL_LIQUIFIER_ITEM.get());
                        output.accept(ModBlocks.ME_PRESSURIZED_REACTION_CHAMBER_ITEM.get());
                        output.accept(ModItems.SPEED_CARD.get());
                        output.accept(ModItems.PARALLEL_CARD.get());
                        output.accept(ModItems.ENERGY_CARD.get());
                    })
                    .build());

    private ModCreativeModeTabs() {
    }
}
