package io.github.shenfnx.mekanismae.compat.jade;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.AbstractDualKeyMeMachineBlockEntity;
import io.github.shenfnx.mekanismae.util.EnergyFormatter;
import me.ramidzkh.mekae2.ae2.MekanismKey;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Jade status for two-input item/chemical processing ledgers. */
public abstract class AbstractDualKeyMeMachineJadeProvider
        implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    private static final String NETWORK_ENABLED = "NetworkEnabled";
    private static final String NETWORK_ONLINE = "NetworkOnline";
    private static final String FAULTED = "Faulted";
    private static final String BUFFERED_OPERATIONS = "BufferedOperations";
    private static final String BUFFER_LIMIT = "BufferLimit";
    private static final String CURRENT_OPERATIONS = "CurrentOperations";
    private static final String SPEED = "Speed";
    private static final String PARALLEL = "Parallel";
    private static final String ENERGY = "Energy";
    private static final String ENERGY_CAPACITY = "EnergyCapacity";
    private static final String FIRST_ITEM = "FirstItem";
    private static final String SECOND_ITEM = "SecondItem";
    private static final String OUTPUT_ITEM = "OutputItem";
    private static final String OUTPUT_COUNT = "OutputCount";
    private static final String FIRST_CHEMICAL = "FirstChemical";
    private static final String FIRST_CHEMICAL_COUNT = "FirstChemicalCount";
    private static final String SECOND_CHEMICAL = "SecondChemical";
    private static final String SECOND_CHEMICAL_COUNT = "SecondChemicalCount";
    private static final String OUTPUT_CHEMICAL = "OutputChemical";
    private static final String OUTPUT_CHEMICAL_COUNT = "OutputChemicalCount";

    private final ResourceLocation uid;
    private final String dataKey;

    protected AbstractDualKeyMeMachineJadeProvider(String id) {
        uid = ResourceLocation.fromNamespaceAndPath(MekanismAeMod.MOD_ID, id + "_status");
        dataKey = MekanismAeMod.MOD_ID + ":" + id;
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof AbstractDualKeyMeMachineBlockEntity machineEntity)) {
            return;
        }
        CompoundTag machine = new CompoundTag();
        machine.putBoolean(NETWORK_ENABLED, machineEntity.isNetworkEnabled());
        machine.putBoolean(NETWORK_ONLINE, machineEntity.isNetworkOnline());
        machine.putBoolean(FAULTED, machineEntity.isProcessingFaulted());
        machine.putLong(BUFFERED_OPERATIONS, machineEntity.getBufferedOperationCount());
        machine.putLong(BUFFER_LIMIT, machineEntity.getBufferOperationLimit());
        machine.putLong(CURRENT_OPERATIONS, machineEntity.getCurrentOperationCount());
        machine.putInt(SPEED, machineEntity.getSpeedMultiplier());
        machine.putInt(PARALLEL, machineEntity.getParallelMultiplier());
        machine.putLong(ENERGY, machineEntity.getEnergyStorage().getEnergyStored());
        machine.putLong(ENERGY_CAPACITY, machineEntity.getEnergyStorage().getMaxEnergyStored());
        putItem(machine, FIRST_ITEM, machineEntity.getBufferedInputDisplay(), accessor);
        putItem(machine, SECOND_ITEM, machineEntity.getSecondBufferedInputDisplay(), accessor);
        ItemStack output = machineEntity.getBufferedOutputDisplay();
        putItem(machine, OUTPUT_ITEM, output, accessor);
        if (!output.isEmpty()) {
            machine.putLong(OUTPUT_COUNT, machineEntity.getBufferedOutputCount());
        }
        putChemical(machine, FIRST_CHEMICAL, FIRST_CHEMICAL_COUNT,
                machineEntity.getFirstBufferedChemicalKey(), machineEntity.getFirstBufferedChemicalCount());
        putChemical(machine, SECOND_CHEMICAL, SECOND_CHEMICAL_COUNT,
                machineEntity.getSecondBufferedChemicalKey(), machineEntity.getSecondBufferedChemicalCount());
        putChemical(machine, OUTPUT_CHEMICAL, OUTPUT_CHEMICAL_COUNT,
                machineEntity.getBufferedOutputChemicalKey(), machineEntity.getBufferedOutputChemicalCount());
        data.put(dataKey, machine);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(dataKey)) {
            return;
        }
        CompoundTag machine = accessor.getServerData().getCompound(dataKey);
        tooltip.add(Component.translatable("jade.mekanismae.network", status(machine)));
        long buffered = machine.getLong(BUFFERED_OPERATIONS);
        tooltip.add(Component.translatable("jade.mekanismae.buffer", buffered, machine.getLong(BUFFER_LIMIT)));

        ItemStack firstItem = readItem(machine, FIRST_ITEM, accessor);
        ItemStack secondItem = readItem(machine, SECOND_ITEM, accessor);
        Chemical firstChemical = readChemical(machine, FIRST_CHEMICAL);
        Chemical secondChemical = readChemical(machine, SECOND_CHEMICAL);
        if (!firstItem.isEmpty()) {
            tooltip.add(Component.translatable("jade.mekanismae.current",
                    firstItem.getHoverName(), machine.getLong(CURRENT_OPERATIONS)));
        } else if (firstChemical != null) {
            tooltip.add(Component.translatable("jade.mekanismae.current_chemical",
                    firstChemical.getTextComponent(), machine.getLong(FIRST_CHEMICAL_COUNT)));
        } else if (buffered > 0) {
            tooltip.add(Component.translatable("jade.mekanismae.current.queued"));
        } else {
            tooltip.add(Component.translatable("jade.mekanismae.current.idle"));
        }
        if (!secondItem.isEmpty()) {
            tooltip.add(Component.translatable("jade.mekanismae.secondary_item", secondItem.getHoverName()));
        } else if (secondChemical != null) {
            tooltip.add(Component.translatable("jade.mekanismae.secondary_chemical",
                    secondChemical.getTextComponent(), machine.getLong(SECOND_CHEMICAL_COUNT)));
        }

        ItemStack outputItem = readItem(machine, OUTPUT_ITEM, accessor);
        Chemical outputChemical = readChemical(machine, OUTPUT_CHEMICAL);
        if (!outputItem.isEmpty()) {
            tooltip.add(Component.translatable("jade.mekanismae.pending_output",
                    outputItem.getHoverName(), machine.getLong(OUTPUT_COUNT)));
        } else if (outputChemical != null) {
            tooltip.add(Component.translatable("jade.mekanismae.pending_chemical",
                    outputChemical.getTextComponent(), machine.getLong(OUTPUT_CHEMICAL_COUNT)));
        }

        tooltip.add(Component.translatable("jade.mekanismae.upgrades",
                machine.getInt(SPEED), machine.getInt(PARALLEL)));
        tooltip.add(Component.translatable("jade.mekanismae.energy",
                EnergyFormatter.format(machine.getLong(ENERGY)),
                EnergyFormatter.format(machine.getLong(ENERGY_CAPACITY))));
    }

    private static Component status(CompoundTag machine) {
        if (machine.getBoolean(FAULTED)) {
            return Component.translatable("jade.mekanismae.status.faulted").withStyle(ChatFormatting.RED);
        }
        if (!machine.getBoolean(NETWORK_ENABLED)) {
            return Component.translatable("jade.mekanismae.status.disabled").withStyle(ChatFormatting.YELLOW);
        }
        if (!machine.getBoolean(NETWORK_ONLINE)) {
            return Component.translatable("jade.mekanismae.status.offline").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("jade.mekanismae.status.online").withStyle(ChatFormatting.GREEN);
    }

    private static void putItem(CompoundTag data, String name, ItemStack stack, BlockAccessor accessor) {
        if (!stack.isEmpty()) {
            data.put(name, stack.save(accessor.getLevel().registryAccess()));
        }
    }

    private static void putChemical(CompoundTag data, String keyName, String countName,
            MekanismKey key, long count) {
        if (key != null && count > 0) {
            data.putString(keyName, key.getId().toString());
            data.putLong(countName, count);
        }
    }

    private static Chemical readChemical(CompoundTag data, String name) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString(name));
        return id == null ? null : MekanismAPI.CHEMICAL_REGISTRY.get(id);
    }

    private static ItemStack readItem(CompoundTag data, String name, BlockAccessor accessor) {
        return data.contains(name)
                ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), data.getCompound(name))
                : ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getUid() {
        return uid;
    }
}
