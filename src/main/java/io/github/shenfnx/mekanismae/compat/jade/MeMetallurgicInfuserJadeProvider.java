package io.github.shenfnx.mekanismae.compat.jade;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
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

public enum MeMetallurgicInfuserJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(
            MekanismAeMod.MOD_ID, "me_metallurgic_infuser_status");
    private static final String DATA_KEY = MekanismAeMod.MOD_ID + ":me_metallurgic_infuser";
    private static final String NETWORK_ENABLED = "NetworkEnabled";
    private static final String NETWORK_ONLINE = "NetworkOnline";
    private static final String FAULTED = "Faulted";
    private static final String BUFFERED_OPERATIONS = "BufferedOperations";
    private static final String BUFFER_LIMIT = "BufferLimit";
    private static final String CURRENT_OPERATIONS = "CurrentOperations";
    private static final String PARALLEL = "Parallel";
    private static final String ENERGY = "Energy";
    private static final String ENERGY_CAPACITY = "EnergyCapacity";
    private static final String CURRENT_INPUT = "CurrentInput";
    private static final String PENDING_OUTPUT = "PendingOutput";
    private static final String PENDING_OUTPUT_COUNT = "PendingOutputCount";
    private static final String CHEMICAL = "Chemical";
    private static final String CHEMICAL_COUNT = "ChemicalCount";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof MeMetallurgicInfuserBlockEntity infuser)) {
            return;
        }

        CompoundTag machine = new CompoundTag();
        machine.putBoolean(NETWORK_ENABLED, infuser.isNetworkEnabled());
        machine.putBoolean(NETWORK_ONLINE, infuser.isNetworkOnline());
        machine.putBoolean(FAULTED, infuser.isProcessingFaulted());
        machine.putLong(BUFFERED_OPERATIONS, infuser.getBufferedOperationCount());
        machine.putLong(BUFFER_LIMIT, infuser.getBufferOperationLimit());
        machine.putLong(CURRENT_OPERATIONS, infuser.getCurrentOperationCount());
        machine.putInt(PARALLEL, infuser.getParallelMultiplier());
        machine.putLong(ENERGY, infuser.getEnergyStorage().getEnergyStored());
        machine.putLong(ENERGY_CAPACITY, infuser.getEnergyStorage().getMaxEnergyStored());

        ItemStack currentInput = infuser.getBufferedInputDisplay();
        if (!currentInput.isEmpty()) {
            machine.put(CURRENT_INPUT, currentInput.save(accessor.getLevel().registryAccess()));
        }
        ItemStack pendingOutput = infuser.getBufferedOutputDisplay();
        if (!pendingOutput.isEmpty()) {
            machine.put(PENDING_OUTPUT, pendingOutput.save(accessor.getLevel().registryAccess()));
            machine.putLong(PENDING_OUTPUT_COUNT, infuser.getBufferedOutputCount());
        }
        MekanismKey chemicalKey = infuser.getBufferedChemicalKey();
        if (chemicalKey != null) {
            machine.putString(CHEMICAL, chemicalKey.getId().toString());
            machine.putLong(CHEMICAL_COUNT, infuser.getBufferedChemicalCount());
        }
        data.put(DATA_KEY, machine);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(DATA_KEY)) {
            return;
        }
        CompoundTag machine = accessor.getServerData().getCompound(DATA_KEY);

        Component status;
        if (machine.getBoolean(FAULTED)) {
            status = Component.translatable("jade.mekanismae.status.faulted").withStyle(ChatFormatting.RED);
        } else if (!machine.getBoolean(NETWORK_ENABLED)) {
            status = Component.translatable("jade.mekanismae.status.disabled").withStyle(ChatFormatting.YELLOW);
        } else if (!machine.getBoolean(NETWORK_ONLINE)) {
            status = Component.translatable("jade.mekanismae.status.offline").withStyle(ChatFormatting.RED);
        } else {
            status = Component.translatable("jade.mekanismae.status.online").withStyle(ChatFormatting.GREEN);
        }
        tooltip.add(Component.translatable("jade.mekanismae.network", status));

        long buffered = machine.getLong(BUFFERED_OPERATIONS);
        tooltip.add(Component.translatable(
                "jade.mekanismae.buffer",
                buffered,
                machine.getLong(BUFFER_LIMIT)));

        ItemStack currentInput = machine.contains(CURRENT_INPUT)
                ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), machine.getCompound(CURRENT_INPUT))
                : ItemStack.EMPTY;
        ItemStack pendingOutput = machine.contains(PENDING_OUTPUT)
                ? ItemStack.parseOptional(accessor.getLevel().registryAccess(), machine.getCompound(PENDING_OUTPUT))
                : ItemStack.EMPTY;
        if (!currentInput.isEmpty()) {
            tooltip.add(Component.translatable(
                    "jade.mekanismae.current",
                    currentInput.getHoverName(),
                    machine.getLong(CURRENT_OPERATIONS)));
        } else if (!pendingOutput.isEmpty()) {
            tooltip.add(Component.translatable(
                    "jade.mekanismae.pending_output",
                    pendingOutput.getHoverName(),
                    machine.getLong(PENDING_OUTPUT_COUNT)));
        } else if (buffered > 0) {
            tooltip.add(Component.translatable("jade.mekanismae.current.queued"));
        } else {
            tooltip.add(Component.translatable("jade.mekanismae.current.idle"));
        }

        String chemicalId = machine.getString(CHEMICAL);
        long chemicalCount = machine.getLong(CHEMICAL_COUNT);
        if (chemicalId.isEmpty() || chemicalCount <= 0) {
            tooltip.add(Component.translatable("gui.mekanismae.chemical.empty"));
        } else {
            ResourceLocation chemicalLocation = ResourceLocation.tryParse(chemicalId);
            Chemical chemical = chemicalLocation == null
                    ? null
                    : MekanismAPI.CHEMICAL_REGISTRY.get(chemicalLocation);
            if (chemical == null) {
                tooltip.add(Component.translatable("gui.mekanismae.chemical.unknown", chemicalCount));
            } else {
                tooltip.add(Component.translatable(
                        "jade.mekanismae.chemical",
                        chemical.getTextComponent(),
                        chemicalCount));
            }
        }

        tooltip.add(Component.translatable("jade.mekanismae.parallel", machine.getInt(PARALLEL)));
        tooltip.add(Component.translatable(
                "jade.mekanismae.energy",
                machine.getLong(ENERGY),
                machine.getLong(ENERGY_CAPACITY)));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
