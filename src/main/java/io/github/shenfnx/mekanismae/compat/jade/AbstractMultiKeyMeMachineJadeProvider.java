package io.github.shenfnx.mekanismae.compat.jade;

import appeng.api.stacks.AEKey;
import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.AbstractMultiKeyMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.util.EnergyFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade status for ledgers containing item, fluid, and chemical resources. */
public abstract class AbstractMultiKeyMeMachineJadeProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private final ResourceLocation uid;
    private final String dataKey;

    protected AbstractMultiKeyMeMachineJadeProvider(String id) {
        uid = ResourceLocation.fromNamespaceAndPath(MekanismAeMod.MOD_ID, id + "_status");
        dataKey = MekanismAeMod.MOD_ID + ":" + id;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AbstractMultiKeyMeMachineBlockEntity entity)) {
            return;
        }
        CompoundTag machine = new CompoundTag();
        machine.putBoolean("NetworkEnabled", entity.isNetworkEnabled());
        machine.putBoolean("NetworkOnline", entity.isNetworkOnline());
        machine.putBoolean("Faulted", entity.isProcessingFaulted());
        machine.putLong("BufferedOperations", entity.getBufferedOperationCount());
        machine.putLong("BufferLimit", entity.getBufferOperationLimit());
        machine.putInt("InputSlots", entity.getInputSlotCount());
        machine.putInt("OutputSlots", entity.getOutputSlotCount());
        machine.putInt("Speed", entity.getSpeedMultiplier());
        machine.putInt("Parallel", entity.getParallelMultiplier());
        machine.putLong("Energy", entity.getEnergyStorage().getEnergyStored());
        machine.putLong("EnergyCapacity", entity.getEnergyStorage().getMaxEnergyStored());
        for (int slot = 0; slot < AbstractMultiKeyMeMachineBlockEntity.RESOURCE_DISPLAY_SLOTS; slot++) {
            AEKey key = entity.getBufferedResourceKey(slot);
            long count = entity.getBufferedResourceCount(slot);
            if (key != null && count > 0) {
                machine.put("Resource" + slot, key.toTagGeneric(accessor.getLevel().registryAccess()));
                machine.putLong("ResourceCount" + slot, count);
            }
        }
        data.put(dataKey, machine);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(dataKey)) {
            return;
        }
        CompoundTag machine = accessor.getServerData().getCompound(dataKey);
        tooltip.add(Component.translatable("jade.mekanismae.network", status(machine)));
        tooltip.add(Component.translatable("jade.mekanismae.buffer",
                machine.getLong("BufferedOperations"), machine.getLong("BufferLimit")));
        int inputSlots = machine.getInt("InputSlots");
        int outputSlots = machine.getInt("OutputSlots");
        boolean showedInput = false;
        for (int slot = 0; slot < inputSlots; slot++) {
            AEKey key = readKey(machine, slot, accessor);
            if (key != null) {
                showedInput = true;
                tooltip.add(Component.translatable("jade.mekanismae.input_resource", slot + 1,
                        key.getDisplayName(), key.formatAmount(machine.getLong("ResourceCount" + slot),
                                appeng.api.stacks.AmountFormat.FULL)));
            }
        }
        if (!showedInput) {
            tooltip.add(machine.getLong("BufferedOperations") > 0
                    ? Component.translatable("jade.mekanismae.current.queued")
                    : Component.translatable("jade.mekanismae.current.idle"));
        }
        for (int output = 0; output < outputSlots; output++) {
            int slot = AbstractMultiKeyMeMachineBlockEntity.MAX_INPUT_SLOTS + output;
            AEKey key = readKey(machine, slot, accessor);
            if (key != null) {
                tooltip.add(Component.translatable("jade.mekanismae.output_resource", output + 1,
                        key.getDisplayName(), key.formatAmount(machine.getLong("ResourceCount" + slot),
                                appeng.api.stacks.AmountFormat.FULL)));
            }
        }
        tooltip.add(Component.translatable("jade.mekanismae.upgrades",
                machine.getInt("Speed"), machine.getInt("Parallel")));
        tooltip.add(Component.translatable("jade.mekanismae.energy",
                EnergyFormatter.format(machine.getLong("Energy")),
                EnergyFormatter.format(machine.getLong("EnergyCapacity"))));
    }

    private static AEKey readKey(CompoundTag machine, int slot, BlockAccessor accessor) {
        String name = "Resource" + slot;
        return machine.contains(name, net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? AEKey.fromTagGeneric(accessor.getLevel().registryAccess(), machine.getCompound(name)) : null;
    }

    private static Component status(CompoundTag machine) {
        if (machine.getBoolean("Faulted")) {
            return Component.translatable("jade.mekanismae.status.faulted").withStyle(ChatFormatting.RED);
        }
        if (!machine.getBoolean("NetworkEnabled")) {
            return Component.translatable("jade.mekanismae.status.disabled").withStyle(ChatFormatting.YELLOW);
        }
        if (!machine.getBoolean("NetworkOnline")) {
            return Component.translatable("jade.mekanismae.status.offline").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("jade.mekanismae.status.online").withStyle(ChatFormatting.GREEN);
    }

    @Override
    public ResourceLocation getUid() {
        return uid;
    }
}
