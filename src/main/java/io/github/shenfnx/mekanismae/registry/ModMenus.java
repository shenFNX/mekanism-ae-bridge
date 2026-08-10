package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeMetallurgicInfuserMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MekanismAeMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MeEnrichmentChamberMenu>> ME_ENRICHMENT_CHAMBER =
            MENUS.register("me_enrichment_chamber", () -> IMenuTypeExtension.create(MeEnrichmentChamberMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MeMetallurgicInfuserMenu>> ME_METALLURGIC_INFUSER =
            MENUS.register("me_metallurgic_infuser", () -> IMenuTypeExtension.create(MeMetallurgicInfuserMenu::new));

    private ModMenus() {
    }
}
