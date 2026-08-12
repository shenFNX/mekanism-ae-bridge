package io.github.shenfnx.mekanismae.client;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = MekanismAeMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.ME_ENRICHMENT_CHAMBER.get(), MeEnrichmentChamberScreen::new);
        event.register(ModMenus.ME_CRUSHER.get(), MeCrusherScreen::new);
        event.register(ModMenus.ME_ENERGIZED_SMELTER.get(), MeEnergizedSmelterScreen::new);
        event.register(ModMenus.ME_METALLURGIC_INFUSER.get(), MeMetallurgicInfuserScreen::new);
        event.register(ModMenus.ME_OSMIUM_COMPRESSOR.get(), MeOsmiumCompressorScreen::new);
        event.register(ModMenus.ME_PURIFICATION_CHAMBER.get(), MePurificationChamberScreen::new);
        event.register(ModMenus.ME_CHEMICAL_INJECTION_CHAMBER.get(), MeChemicalInjectionChamberScreen::new);
        event.register(ModMenus.ME_COMBINER.get(), MeCombinerScreen::new);
        event.register(ModMenus.ME_PRECISION_SAWMILL.get(), MePrecisionSawmillScreen::new);
    }
}
