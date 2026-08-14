package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.menu.MeEnrichmentChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeEnergizedSmelterMenu;
import io.github.shenfnx.mekanismae.menu.MeCrusherMenu;
import io.github.shenfnx.mekanismae.menu.MeMetallurgicInfuserMenu;
import io.github.shenfnx.mekanismae.menu.MeOsmiumCompressorMenu;
import io.github.shenfnx.mekanismae.menu.MePurificationChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalInjectionChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeCombinerMenu;
import io.github.shenfnx.mekanismae.menu.MePrecisionSawmillMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalOxidizerMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalCrystallizerMenu;
import io.github.shenfnx.mekanismae.menu.MeAntiprotonicNucleosynthesizerMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalDissolutionChamberMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalInfuserMenu;
import io.github.shenfnx.mekanismae.menu.MeElectrolyticSeparatorMenu;
import io.github.shenfnx.mekanismae.menu.MeRotaryCondensentratorMenu;
import io.github.shenfnx.mekanismae.menu.MeChemicalWasherMenu;
import io.github.shenfnx.mekanismae.menu.MeNutritionalLiquifierMenu;
import io.github.shenfnx.mekanismae.menu.MePressurizedReactionChamberMenu;
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

    public static final DeferredHolder<MenuType<?>, MenuType<MeCrusherMenu>> ME_CRUSHER =
            MENUS.register("me_crusher", () -> IMenuTypeExtension.create(MeCrusherMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MeEnergizedSmelterMenu>> ME_ENERGIZED_SMELTER =
            MENUS.register("me_energized_smelter", () -> IMenuTypeExtension.create(MeEnergizedSmelterMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MeMetallurgicInfuserMenu>> ME_METALLURGIC_INFUSER =
            MENUS.register("me_metallurgic_infuser", () -> IMenuTypeExtension.create(MeMetallurgicInfuserMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<MeOsmiumCompressorMenu>> ME_OSMIUM_COMPRESSOR =
            MENUS.register("me_osmium_compressor", () -> IMenuTypeExtension.create(MeOsmiumCompressorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MePurificationChamberMenu>> ME_PURIFICATION_CHAMBER =
            MENUS.register("me_purification_chamber", () -> IMenuTypeExtension.create(MePurificationChamberMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalInjectionChamberMenu>>
            ME_CHEMICAL_INJECTION_CHAMBER = MENUS.register("me_chemical_injection_chamber",
                    () -> IMenuTypeExtension.create(MeChemicalInjectionChamberMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeCombinerMenu>> ME_COMBINER =
            MENUS.register("me_combiner", () -> IMenuTypeExtension.create(MeCombinerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MePrecisionSawmillMenu>> ME_PRECISION_SAWMILL =
            MENUS.register("me_precision_sawmill",
                    () -> IMenuTypeExtension.create(MePrecisionSawmillMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalOxidizerMenu>> ME_CHEMICAL_OXIDIZER =
            MENUS.register("me_chemical_oxidizer",
                    () -> IMenuTypeExtension.create(MeChemicalOxidizerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalCrystallizerMenu>> ME_CHEMICAL_CRYSTALLIZER =
            MENUS.register("me_chemical_crystallizer",
                    () -> IMenuTypeExtension.create(MeChemicalCrystallizerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeAntiprotonicNucleosynthesizerMenu>>
            ME_ANTIPROTONIC_NUCLEOSYNTHESIZER = MENUS.register("me_antiprotonic_nucleosynthesizer",
                    () -> IMenuTypeExtension.create(MeAntiprotonicNucleosynthesizerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalDissolutionChamberMenu>>
            ME_CHEMICAL_DISSOLUTION_CHAMBER = MENUS.register("me_chemical_dissolution_chamber",
                    () -> IMenuTypeExtension.create(MeChemicalDissolutionChamberMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalInfuserMenu>> ME_CHEMICAL_INFUSER =
            MENUS.register("me_chemical_infuser",
                    () -> IMenuTypeExtension.create(MeChemicalInfuserMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeElectrolyticSeparatorMenu>>
            ME_ELECTROLYTIC_SEPARATOR = MENUS.register("me_electrolytic_separator",
                    () -> IMenuTypeExtension.create(MeElectrolyticSeparatorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeRotaryCondensentratorMenu>>
            ME_ROTARY_CONDENSENTRATOR = MENUS.register("me_rotary_condensentrator",
                    () -> IMenuTypeExtension.create(MeRotaryCondensentratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeChemicalWasherMenu>> ME_CHEMICAL_WASHER =
            MENUS.register("me_chemical_washer",
                    () -> IMenuTypeExtension.create(MeChemicalWasherMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MeNutritionalLiquifierMenu>>
            ME_NUTRITIONAL_LIQUIFIER = MENUS.register("me_nutritional_liquifier",
                    () -> IMenuTypeExtension.create(MeNutritionalLiquifierMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<MePressurizedReactionChamberMenu>>
            ME_PRESSURIZED_REACTION_CHAMBER = MENUS.register("me_pressurized_reaction_chamber",
                    () -> IMenuTypeExtension.create(MePressurizedReactionChamberMenu::new));

    private ModMenus() {
    }
}
