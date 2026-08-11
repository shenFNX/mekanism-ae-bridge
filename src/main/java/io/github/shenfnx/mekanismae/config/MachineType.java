package io.github.shenfnx.mekanismae.config;

public enum MachineType {
    ME_ENRICHMENT_CHAMBER("me_enrichment_chamber"),
    ME_CRUSHER("me_crusher"),
    ME_METALLURGIC_INFUSER("me_metallurgic_infuser"),
    ME_OSMIUM_COMPRESSOR("me_osmium_compressor"),
    ME_PURIFICATION_CHAMBER("me_purification_chamber"),
    ME_CHEMICAL_INJECTION_CHAMBER("me_chemical_injection_chamber");

    private final String configKey;

    MachineType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
