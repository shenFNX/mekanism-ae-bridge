package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.MeEnrichmentChamberBlock;
import io.github.shenfnx.mekanismae.block.MeEnergizedSmelterBlock;
import io.github.shenfnx.mekanismae.block.MeCrusherBlock;
import io.github.shenfnx.mekanismae.block.MeMetallurgicInfuserBlock;
import io.github.shenfnx.mekanismae.block.MeOsmiumCompressorBlock;
import io.github.shenfnx.mekanismae.block.MePurificationChamberBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalInjectionChamberBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MekanismAeMod.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MekanismAeMod.MOD_ID);

    public static final DeferredBlock<MeEnrichmentChamberBlock> ME_ENRICHMENT_CHAMBER = BLOCKS.registerBlock(
            "me_enrichment_chamber",
            MeEnrichmentChamberBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> ME_ENRICHMENT_CHAMBER_ITEM =
            ITEMS.registerSimpleBlockItem("me_enrichment_chamber", ME_ENRICHMENT_CHAMBER);

    public static final DeferredBlock<MeCrusherBlock> ME_CRUSHER = BLOCKS.registerBlock(
            "me_crusher",
            MeCrusherBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> ME_CRUSHER_ITEM =
            ITEMS.registerSimpleBlockItem("me_crusher", ME_CRUSHER);

    public static final DeferredBlock<MeEnergizedSmelterBlock> ME_ENERGIZED_SMELTER = BLOCKS.registerBlock(
            "me_energized_smelter", MeEnergizedSmelterBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_ENERGIZED_SMELTER_ITEM =
            ITEMS.registerSimpleBlockItem("me_energized_smelter", ME_ENERGIZED_SMELTER);

    public static final DeferredBlock<MeMetallurgicInfuserBlock> ME_METALLURGIC_INFUSER = BLOCKS.registerBlock(
            "me_metallurgic_infuser",
            MeMetallurgicInfuserBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> ME_METALLURGIC_INFUSER_ITEM =
            ITEMS.registerSimpleBlockItem("me_metallurgic_infuser", ME_METALLURGIC_INFUSER);

    public static final DeferredBlock<MeOsmiumCompressorBlock> ME_OSMIUM_COMPRESSOR = BLOCKS.registerBlock(
            "me_osmium_compressor", MeOsmiumCompressorBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_OSMIUM_COMPRESSOR_ITEM =
            ITEMS.registerSimpleBlockItem("me_osmium_compressor", ME_OSMIUM_COMPRESSOR);

    public static final DeferredBlock<MePurificationChamberBlock> ME_PURIFICATION_CHAMBER = BLOCKS.registerBlock(
            "me_purification_chamber", MePurificationChamberBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_PURIFICATION_CHAMBER_ITEM =
            ITEMS.registerSimpleBlockItem("me_purification_chamber", ME_PURIFICATION_CHAMBER);

    public static final DeferredBlock<MeChemicalInjectionChamberBlock> ME_CHEMICAL_INJECTION_CHAMBER =
            BLOCKS.registerBlock("me_chemical_injection_chamber", MeChemicalInjectionChamberBlock::new,
                    machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_INJECTION_CHAMBER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_injection_chamber", ME_CHEMICAL_INJECTION_CHAMBER);

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private ModBlocks() {
    }
}
