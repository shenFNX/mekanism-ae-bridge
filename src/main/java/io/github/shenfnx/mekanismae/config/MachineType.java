package io.github.shenfnx.mekanismae.config;

public enum MachineType {
    ME_ENRICHMENT_CHAMBER("me_enrichment_chamber"),
    ME_CRUSHER("me_crusher"),
    ME_METALLURGIC_INFUSER("me_metallurgic_infuser");

    private final String configKey;

    MachineType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
