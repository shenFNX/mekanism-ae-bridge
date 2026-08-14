package io.github.shenfnx.mekanismae;

import com.mojang.logging.LogUtils;
import appeng.api.AECapabilities;
import io.github.shenfnx.mekanismae.config.MekanismAeConfig;
import io.github.shenfnx.mekanismae.registry.ModBlocks;
import io.github.shenfnx.mekanismae.registry.ModBlockEntities;
import io.github.shenfnx.mekanismae.registry.ModItems;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import io.github.shenfnx.mekanismae.registry.ModCreativeModeTabs;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(MekanismAeMod.MOD_ID)
public final class MekanismAeMod {
    public static final String MOD_ID = "mekanismae";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MekanismAeMod(IEventBus modEventBus, ModContainer modContainer) {
        MekanismAeConfig.register(modContainer);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeModeTabs.TABS.register(modEventBus);
        modEventBus.addListener(MekanismAeMod::addCreativeTabItems);
        modEventBus.addListener(MekanismAeMod::registerCapabilities);
        LOGGER.info("Loading {}", MOD_ID);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(),
                (blockEntity, ignored) -> blockEntity);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(
                mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),
                ModBlockEntities.ME_ENRICHMENT_CHAMBER.get(),
                (blockEntity, side) -> blockEntity.getStrictEnergyHandler());

        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_CRUSHER.get(),
                (blockEntity, ignored) -> blockEntity);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ME_CRUSHER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(
                mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),
                ModBlockEntities.ME_CRUSHER.get(),
                (blockEntity, side) -> blockEntity.getStrictEnergyHandler());

        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.ME_METALLURGIC_INFUSER.get(),
                (blockEntity, ignored) -> blockEntity);
        event.registerBlockEntity(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.ME_METALLURGIC_INFUSER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(
                mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(),
                ModBlockEntities.ME_METALLURGIC_INFUSER.get(),
                (blockEntity, side) -> blockEntity.getStrictEnergyHandler());

        registerMachineCapabilities(event, ModBlockEntities.ME_OSMIUM_COMPRESSOR.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_PURIFICATION_CHAMBER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_INJECTION_CHAMBER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_ENERGIZED_SMELTER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_COMBINER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_PRECISION_SAWMILL.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_OXIDIZER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_CRYSTALLIZER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_ANTIPROTONIC_NUCLEOSYNTHESIZER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_DISSOLUTION_CHAMBER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_INFUSER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_ELECTROLYTIC_SEPARATOR.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_ROTARY_CONDENSENTRATOR.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_CHEMICAL_WASHER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_NUTRITIONAL_LIQUIFIER.get());
        registerMachineCapabilities(event, ModBlockEntities.ME_PRESSURIZED_REACTION_CHAMBER.get());
    }

    private static <T extends io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity>
            void registerMachineCapabilities(RegisterCapabilitiesEvent event,
                    net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST, type,
                (blockEntity, ignored) -> blockEntity);
        event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, type,
                (blockEntity, side) -> blockEntity.getEnergyStorage());
        event.registerBlockEntity(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.block(), type,
                (blockEntity, side) -> blockEntity.getStrictEnergyHandler());
    }

    private static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        // Items are exposed through the mod's dedicated creative tab.
    }
}
