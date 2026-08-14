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
    ME_CHEMICAL_CRYSTALLIZER("me_chemical_crystallizer");

    private final String configKey;

    MachineType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
