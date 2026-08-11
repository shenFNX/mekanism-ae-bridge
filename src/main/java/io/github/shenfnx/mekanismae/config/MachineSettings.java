package io.github.shenfnx.mekanismae.config;

import java.util.List;

/** Immutable, validated settings used by one machine for the lifetime of a loaded world. */
public record MachineSettings(
        boolean requireChannel,
        double idleAePower,
        boolean redstonePausesProcessing,
        int baseEnergyCapacity,
        int baseEnergyReceive,
        int energyCapacityPerUpgrade,
        int energyReceivePerUpgrade,
        int energyPerOperation,
        int processingTicks,
        long maxBufferedOperations,
        List<Integer> speedMultipliers,
        List<Integer> parallelMultipliers) {

    public MachineSettings {
        speedMultipliers = List.copyOf(speedMultipliers);
        parallelMultipliers = List.copyOf(parallelMultipliers);
        if (speedMultipliers.isEmpty() || parallelMultipliers.isEmpty()) {
            throw new IllegalArgumentException("Upgrade multiplier curves cannot be empty");
        }
    }

    public int speedMultiplier(int installedCards) {
        return curveValue(speedMultipliers, installedCards);
    }

    public int parallelMultiplier(int installedCards) {
        return curveValue(parallelMultipliers, installedCards);
    }

    public int energyCapacity(int installedCards) {
        return safeEnergyValue(baseEnergyCapacity, energyCapacityPerUpgrade, installedCards);
    }

    public int energyReceive(int installedCards) {
        return safeEnergyValue(baseEnergyReceive, energyReceivePerUpgrade, installedCards);
    }

    private static int curveValue(List<Integer> curve, int installedCards) {
        int index = Math.min(Math.max(0, installedCards), curve.size() - 1);
        return curve.get(index);
    }

    private static int safeEnergyValue(int base, int perCard, int installedCards) {
        long result = (long) base + (long) Math.max(0, installedCards) * perCard;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, result));
    }
}
