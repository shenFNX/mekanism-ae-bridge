package io.github.shenfnx.mekanismae.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-authoritative tuning loaded from the global file or a native per-world override. */
public final class MekanismAeConfig {
    private static final int CURVE_SIZE = 9;
    private static final int MAX_MULTIPLIER = 256;
    private static final long MAX_BUFFERED_OPERATIONS = 16_777_216L;
    private static final List<Integer> DEFAULT_SPEED_CURVE = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
    private static final List<Integer> DEFAULT_PARALLEL_CURVE = List.of(1, 2, 3, 4, 6, 8, 10, 12, 16);

    public static final ModConfigSpec SPEC;

    private static final BalanceValues VALUES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        VALUES = new BalanceValues(builder);
        SPEC = builder.build();
    }

    private MekanismAeConfig() {
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "mekanismae-server.toml");
    }

    public static MachineSettings settings(MachineType machineType) {
        return VALUES.resolve(machineType);
    }

    private static final class BalanceValues {
        private final ModConfigSpec.BooleanValue requireChannel;
        private final ModConfigSpec.DoubleValue idleAePower;
        private final ModConfigSpec.BooleanValue redstonePausesProcessing;
        private final ModConfigSpec.IntValue energyCapacityPerUpgrade;
        private final ModConfigSpec.IntValue energyReceivePerUpgrade;
        private final ModConfigSpec.ConfigValue<List<? extends Integer>> speedMultipliers;
        private final ModConfigSpec.ConfigValue<List<? extends Integer>> parallelMultipliers;
        private final MachineValues defaults;
        private final Map<MachineType, MachineValues> machines = new EnumMap<>(MachineType.class);

        private BalanceValues(ModConfigSpec.Builder builder) {
            builder.comment("AE network behavior shared by all machines.").push("network");
            requireChannel = builder
                    .comment("Whether each machine consumes an AE2 channel.")
                    .translation("config.mekanismae.require_channel")
                    .worldRestart()
                    .define("require_channel", true);
            idleAePower = builder
                    .comment("Idle AE power used by each connected machine.")
                    .translation("config.mekanismae.idle_ae_power")
                    .worldRestart()
                    .defineInRange("idle_ae_power", 2.0, 0.0, 10_000.0);
            redstonePausesProcessing = builder
                    .comment("Pause processing while the machine receives a redstone signal.")
                    .translation("config.mekanismae.redstone_pauses_processing")
                    .worldRestart()
                    .define("redstone_pauses_processing", true);
            builder.pop();

            builder.comment("Upgrade card behavior shared by all machines.").push("upgrades");
            energyCapacityPerUpgrade = builder
                    .comment("Additional FE capacity granted by each energy card.")
                    .translation("config.mekanismae.energy_capacity_per_upgrade")
                    .worldRestart()
                    .defineInRange("energy_capacity_per_upgrade", 500_000, 0, 250_000_000);
            energyReceivePerUpgrade = builder
                    .comment("Additional FE/t input granted by each energy card.")
                    .translation("config.mekanismae.energy_receive_per_upgrade")
                    .worldRestart()
                    .defineInRange("energy_receive_per_upgrade", 200_000, 0, 250_000_000);
            speedMultipliers = builder
                    .comment("Speed multipliers for 0 through 8 speed cards. Missing entries repeat the last value; extra entries are ignored.")
                    .translation("config.mekanismae.speed_multipliers")
                    .worldRestart()
                    .defineList("speed_multipliers", DEFAULT_SPEED_CURVE, () -> 1,
                            value -> value instanceof Integer integer && integer >= 1 && integer <= MAX_MULTIPLIER);
            parallelMultipliers = builder
                    .comment("Parallel multipliers for 0 through 8 parallel cards. Missing entries repeat the last value; extra entries are ignored.")
                    .translation("config.mekanismae.parallel_multipliers")
                    .worldRestart()
                    .defineList("parallel_multipliers", DEFAULT_PARALLEL_CURVE, () -> 1,
                            value -> value instanceof Integer integer && integer >= 1 && integer <= MAX_MULTIPLIER);
            builder.pop();

            builder.comment("Default machine values and optional per-machine replacements.").push("machines");
            defaults = new MachineValues(builder, "defaults", false);
            for (MachineType machineType : MachineType.values()) {
                machines.put(machineType, new MachineValues(builder, machineType.configKey(), true));
            }
            builder.pop();
        }

        private MachineSettings resolve(MachineType machineType) {
            MachineValues machine = machines.get(machineType);
            MachineValues selected = machine.useDefaults != null && machine.useDefaults.get() ? defaults : machine;
            return new MachineSettings(
                    requireChannel.get(),
                    idleAePower.get(),
                    redstonePausesProcessing.get(),
                    selected.baseEnergyCapacity.get(),
                    selected.baseEnergyReceive.get(),
                    energyCapacityPerUpgrade.get(),
                    energyReceivePerUpgrade.get(),
                    selected.energyPerOperation.get(),
                    selected.processingTicks.get(),
                    selected.maxBufferedOperations.get(),
                    normalizeCurve(speedMultipliers.get(), DEFAULT_SPEED_CURVE),
                    normalizeCurve(parallelMultipliers.get(), DEFAULT_PARALLEL_CURVE));
        }
    }

    private static final class MachineValues {
        private final ModConfigSpec.BooleanValue useDefaults;
        private final ModConfigSpec.IntValue baseEnergyCapacity;
        private final ModConfigSpec.IntValue baseEnergyReceive;
        private final ModConfigSpec.IntValue energyPerOperation;
        private final ModConfigSpec.IntValue processingTicks;
        private final ModConfigSpec.LongValue maxBufferedOperations;

        private MachineValues(ModConfigSpec.Builder builder, String name, boolean allowDefaults) {
            builder.push(name);
            useDefaults = allowDefaults
                    ? builder.comment("Use the machines.defaults values for this machine.")
                            .translation("config.mekanismae.use_machine_defaults")
                            .worldRestart()
                            .define("use_defaults", true)
                    : null;
            baseEnergyCapacity = builder
                    .comment("Base internal FE capacity before energy cards.")
                    .translation("config.mekanismae.base_energy_capacity")
                    .worldRestart()
                    .defineInRange("base_energy_capacity", 1_000_000, 1, 2_000_000_000);
            baseEnergyReceive = builder
                    .comment("Base maximum FE/t accepted before energy cards.")
                    .translation("config.mekanismae.base_energy_receive")
                    .worldRestart()
                    .defineInRange("base_energy_receive", 200_000, 1, 2_000_000_000);
            energyPerOperation = builder
                    .comment("FE consumed for each completed recipe operation.")
                    .translation("config.mekanismae.energy_per_operation")
                    .worldRestart()
                    .defineInRange("energy_per_operation", 10_000, 1, 2_000_000_000);
            processingTicks = builder
                    .comment("Base processing duration before speed cards.")
                    .translation("config.mekanismae.processing_ticks")
                    .worldRestart()
                    .defineInRange("processing_ticks", 20, 1, 1_000_000);
            maxBufferedOperations = builder
                    .comment("Maximum complete recipe operations accepted from AE2. Existing work is never deleted if this is lowered.")
                    .translation("config.mekanismae.max_buffered_operations")
                    .worldRestart()
                    .defineInRange("max_buffered_operations", 1_048_576L, 1L, MAX_BUFFERED_OPERATIONS);
            builder.pop();
        }
    }

    private static List<Integer> normalizeCurve(List<? extends Integer> configured, List<Integer> defaults) {
        List<Integer> result = new ArrayList<>(CURVE_SIZE);
        for (int index = 0; index < CURVE_SIZE; index++) {
            int value;
            if (configured.isEmpty()) {
                value = defaults.get(index);
            } else if (index < configured.size()) {
                value = configured.get(index);
            } else {
                value = configured.getLast();
            }
            result.add(Math.min(MAX_MULTIPLIER, Math.max(1, value)));
        }
        return List.copyOf(result);
    }
}
