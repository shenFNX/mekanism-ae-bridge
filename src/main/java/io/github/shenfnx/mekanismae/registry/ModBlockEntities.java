package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeEnergizedSmelterBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeCrusherBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeOsmiumCompressorBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MePurificationChamberBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalInjectionChamberBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeCombinerBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MePrecisionSawmillBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalOxidizerBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalCrystallizerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MekanismAeMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeEnrichmentChamberBlockEntity>>
            ME_ENRICHMENT_CHAMBER = BLOCK_ENTITY_TYPES.register(
                    "me_enrichment_chamber",
                    () -> BlockEntityType.Builder.of(
                            MeEnrichmentChamberBlockEntity::new,
                            ModBlocks.ME_ENRICHMENT_CHAMBER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeCrusherBlockEntity>> ME_CRUSHER =
            BLOCK_ENTITY_TYPES.register(
                    "me_crusher",
                    () -> BlockEntityType.Builder.of(
                            MeCrusherBlockEntity::new,
                            ModBlocks.ME_CRUSHER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeEnergizedSmelterBlockEntity>>
            ME_ENERGIZED_SMELTER = BLOCK_ENTITY_TYPES.register("me_energized_smelter",
                    () -> BlockEntityType.Builder.of(MeEnergizedSmelterBlockEntity::new,
                            ModBlocks.ME_ENERGIZED_SMELTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeMetallurgicInfuserBlockEntity>>
            ME_METALLURGIC_INFUSER = BLOCK_ENTITY_TYPES.register(
                    "me_metallurgic_infuser",
                    () -> BlockEntityType.Builder.of(
                            MeMetallurgicInfuserBlockEntity::new,
                            ModBlocks.ME_METALLURGIC_INFUSER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeOsmiumCompressorBlockEntity>>
            ME_OSMIUM_COMPRESSOR = BLOCK_ENTITY_TYPES.register("me_osmium_compressor",
                    () -> BlockEntityType.Builder.of(MeOsmiumCompressorBlockEntity::new,
                            ModBlocks.ME_OSMIUM_COMPRESSOR.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MePurificationChamberBlockEntity>>
            ME_PURIFICATION_CHAMBER = BLOCK_ENTITY_TYPES.register("me_purification_chamber",
                    () -> BlockEntityType.Builder.of(MePurificationChamberBlockEntity::new,
                            ModBlocks.ME_PURIFICATION_CHAMBER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeChemicalInjectionChamberBlockEntity>>
            ME_CHEMICAL_INJECTION_CHAMBER = BLOCK_ENTITY_TYPES.register("me_chemical_injection_chamber",
                    () -> BlockEntityType.Builder.of(MeChemicalInjectionChamberBlockEntity::new,
                            ModBlocks.ME_CHEMICAL_INJECTION_CHAMBER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeCombinerBlockEntity>> ME_COMBINER =
            BLOCK_ENTITY_TYPES.register("me_combiner",
                    () -> BlockEntityType.Builder.of(MeCombinerBlockEntity::new,
                            ModBlocks.ME_COMBINER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MePrecisionSawmillBlockEntity>>
            ME_PRECISION_SAWMILL = BLOCK_ENTITY_TYPES.register("me_precision_sawmill",
                    () -> BlockEntityType.Builder.of(MePrecisionSawmillBlockEntity::new,
                            ModBlocks.ME_PRECISION_SAWMILL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeChemicalOxidizerBlockEntity>>
            ME_CHEMICAL_OXIDIZER = BLOCK_ENTITY_TYPES.register("me_chemical_oxidizer",
                    () -> BlockEntityType.Builder.of(MeChemicalOxidizerBlockEntity::new,
                            ModBlocks.ME_CHEMICAL_OXIDIZER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MeChemicalCrystallizerBlockEntity>>
            ME_CHEMICAL_CRYSTALLIZER = BLOCK_ENTITY_TYPES.register("me_chemical_crystallizer",
                    () -> BlockEntityType.Builder.of(MeChemicalCrystallizerBlockEntity::new,
                            ModBlocks.ME_CHEMICAL_CRYSTALLIZER.get()).build(null));

    private ModBlockEntities() {
    }
}
