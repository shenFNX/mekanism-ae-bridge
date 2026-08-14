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
        List<Integer> parallelMultipliers,
        List<Integer> tierParallelMultipliers,
        List<Integer> tierEnergyCapacityMultipliers,
        List<Integer> tierEnergyReceiveMultipliers,
        List<Integer> tierBufferMultipliers) {

    public MachineSettings {
        speedMultipliers = List.copyOf(speedMultipliers);
        parallelMultipliers = List.copyOf(parallelMultipliers);
        tierParallelMultipliers = List.copyOf(tierParallelMultipliers);
        tierEnergyCapacityMultipliers = List.copyOf(tierEnergyCapacityMultipliers);
        tierEnergyReceiveMultipliers = List.copyOf(tierEnergyReceiveMultipliers);
        tierBufferMultipliers = List.copyOf(tierBufferMultipliers);
        if (speedMultipliers.isEmpty() || parallelMultipliers.isEmpty()
                || tierParallelMultipliers.isEmpty() || tierEnergyCapacityMultipliers.isEmpty()
                || tierEnergyReceiveMultipliers.isEmpty() || tierBufferMultipliers.isEmpty()) {
            throw new IllegalArgumentException("Upgrade multiplier curves cannot be empty");
        }
    }

    public int speedMultiplier(int installedCards) {
        return curveValue(speedMultipliers, installedCards);
    }

    public int parallelMultiplier(int installedCards) {
        return curveValue(parallelMultipliers, installedCards);
    }

    public int parallelMultiplier(int installedCards, int tierIndex) {
        return safeIntProduct(parallelMultiplier(installedCards), tierMultiplier(tierParallelMultipliers, tierIndex));
    }

    public int energyCapacity(int installedCards, int tierIndex) {
        return safeIntProduct(safeEnergyValue(baseEnergyCapacity, energyCapacityPerUpgrade, installedCards),
                tierMultiplier(tierEnergyCapacityMultipliers, tierIndex));
    }

    public int energyReceive(int installedCards, int tierIndex) {
        return safeIntProduct(safeEnergyValue(baseEnergyReceive, energyReceivePerUpgrade, installedCards),
                tierMultiplier(tierEnergyReceiveMultipliers, tierIndex));
    }

    public long bufferOperationLimit(int tierIndex) {
        return safeLongProduct(maxBufferedOperations, tierMultiplier(tierBufferMultipliers, tierIndex));
    }

    private static int curveValue(List<Integer> curve, int installedCards) {
        int index = Math.min(Math.max(0, installedCards), curve.size() - 1);
        return curve.get(index);
    }

    private static int tierMultiplier(List<Integer> curve, int tierIndex) {
        return tierIndex < 0 ? 1 : curve.get(Math.min(tierIndex, curve.size() - 1));
    }

    private static int safeEnergyValue(int base, int perCard, int installedCards) {
        long result = (long) base + (long) Math.max(0, installedCards) * perCard;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1, result));
    }
    private static int safeIntProduct(int value, int multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, (long) value * multiplier));
    }

    private static long safeLongProduct(long value, int multiplier) {
        if (value > Long.MAX_VALUE / multiplier) {
            return Long.MAX_VALUE;
        }
        return Math.max(1L, value * multiplier);
    }
}
