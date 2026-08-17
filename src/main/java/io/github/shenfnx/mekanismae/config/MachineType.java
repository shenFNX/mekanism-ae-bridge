package io.github.shenfnx.mekanismae.config;

public enum MachineType {
    ME_ENRICHMENT_CHAMBER("me_enrichment_chamber"),
    ME_CRUSHER("me_crusher"),
    ME_ENERGIZED_SMELTER("me_energized_smelter"),
    ME_METALLURGIC_INFUSER("me_metallurgic_infuser"),
    ME_OSMIUM_COMPRESSOR("me_osmium_compressor"),
    ME_PURIFICATION_CHAMBER("me_purification_chamber"),
    ME_CHEMICAL_INJECTION_CHAMBER("me_chemical_injection_chamber"),
    ME_COMBINER("me_combiner"),
    ME_PRECISION_SAWMILL("me_precision_sawmill"),
    ME_CHEMICAL_OXIDIZER("me_chemical_oxidizer"),
    ME_CHEMICAL_CRYSTALLIZER("me_chemical_crystallizer"),
    ME_ANTIPROTONIC_NUCLEOSYNTHESIZER("me_antiprotonic_nucleosynthesizer"),
    ME_CHEMICAL_DISSOLUTION_CHAMBER("me_chemical_dissolution_chamber"),
    ME_CHEMICAL_INFUSER("me_chemical_infuser", 1_000),
    ME_ELECTROLYTIC_SEPARATOR("me_electrolytic_separator", 1_000),
    ME_ROTARY_CONDENSENTRATOR("me_rotary_condensentrator", 1_000),
    ME_CHEMICAL_WASHER("me_chemical_washer", 1_000),
    ME_NUTRITIONAL_LIQUIFIER("me_nutritional_liquifier"),
    ME_PRESSURIZED_REACTION_CHAMBER("me_pressurized_reaction_chamber");

    private final String configKey;
    private final int defaultBaseOperationsPerCycle;

    MachineType(String configKey) {
        this(configKey, 1);
    }

    MachineType(String configKey, int defaultBaseOperationsPerCycle) {
        this.configKey = configKey;
        this.defaultBaseOperationsPerCycle = defaultBaseOperationsPerCycle;
    }

    public String configKey() {
        return configKey;
    }

    public int defaultBaseOperationsPerCycle() {
        return defaultBaseOperationsPerCycle;
    }
}
