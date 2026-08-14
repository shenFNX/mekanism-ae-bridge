package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.MeEnrichmentChamberBlock;
import io.github.shenfnx.mekanismae.block.MeEnergizedSmelterBlock;
import io.github.shenfnx.mekanismae.block.MeCrusherBlock;
import io.github.shenfnx.mekanismae.block.MeMetallurgicInfuserBlock;
import io.github.shenfnx.mekanismae.block.MeOsmiumCompressorBlock;
import io.github.shenfnx.mekanismae.block.MePurificationChamberBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalInjectionChamberBlock;
import io.github.shenfnx.mekanismae.block.MeCombinerBlock;
import io.github.shenfnx.mekanismae.block.MePrecisionSawmillBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalOxidizerBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalCrystallizerBlock;
import io.github.shenfnx.mekanismae.block.MeAntiprotonicNucleosynthesizerBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalDissolutionChamberBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalInfuserBlock;
import io.github.shenfnx.mekanismae.block.MeElectrolyticSeparatorBlock;
import io.github.shenfnx.mekanismae.block.MeRotaryCondensentratorBlock;
import io.github.shenfnx.mekanismae.block.MeChemicalWasherBlock;
import io.github.shenfnx.mekanismae.block.MeNutritionalLiquifierBlock;
import io.github.shenfnx.mekanismae.block.MePressurizedReactionChamberBlock;
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

    public static final DeferredBlock<MeCombinerBlock> ME_COMBINER = BLOCKS.registerBlock(
            "me_combiner", MeCombinerBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_COMBINER_ITEM =
            ITEMS.registerSimpleBlockItem("me_combiner", ME_COMBINER);

    public static final DeferredBlock<MePrecisionSawmillBlock> ME_PRECISION_SAWMILL = BLOCKS.registerBlock(
            "me_precision_sawmill", MePrecisionSawmillBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_PRECISION_SAWMILL_ITEM =
            ITEMS.registerSimpleBlockItem("me_precision_sawmill", ME_PRECISION_SAWMILL);

    public static final DeferredBlock<MeChemicalOxidizerBlock> ME_CHEMICAL_OXIDIZER = BLOCKS.registerBlock(
            "me_chemical_oxidizer", MeChemicalOxidizerBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_OXIDIZER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_oxidizer", ME_CHEMICAL_OXIDIZER);

    public static final DeferredBlock<MeChemicalCrystallizerBlock> ME_CHEMICAL_CRYSTALLIZER = BLOCKS.registerBlock(
            "me_chemical_crystallizer", MeChemicalCrystallizerBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_CRYSTALLIZER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_crystallizer", ME_CHEMICAL_CRYSTALLIZER);

    public static final DeferredBlock<MeAntiprotonicNucleosynthesizerBlock> ME_ANTIPROTONIC_NUCLEOSYNTHESIZER =
            BLOCKS.registerBlock("me_antiprotonic_nucleosynthesizer",
                    MeAntiprotonicNucleosynthesizerBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_ANTIPROTONIC_NUCLEOSYNTHESIZER_ITEM =
            ITEMS.registerSimpleBlockItem("me_antiprotonic_nucleosynthesizer", ME_ANTIPROTONIC_NUCLEOSYNTHESIZER);

    public static final DeferredBlock<MeChemicalDissolutionChamberBlock> ME_CHEMICAL_DISSOLUTION_CHAMBER =
            BLOCKS.registerBlock("me_chemical_dissolution_chamber",
                    MeChemicalDissolutionChamberBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_DISSOLUTION_CHAMBER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_dissolution_chamber", ME_CHEMICAL_DISSOLUTION_CHAMBER);

    public static final DeferredBlock<MeChemicalInfuserBlock> ME_CHEMICAL_INFUSER = BLOCKS.registerBlock(
            "me_chemical_infuser", MeChemicalInfuserBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_INFUSER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_infuser", ME_CHEMICAL_INFUSER);

    public static final DeferredBlock<MeElectrolyticSeparatorBlock> ME_ELECTROLYTIC_SEPARATOR =
            BLOCKS.registerBlock("me_electrolytic_separator",
                    MeElectrolyticSeparatorBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_ELECTROLYTIC_SEPARATOR_ITEM =
            ITEMS.registerSimpleBlockItem("me_electrolytic_separator", ME_ELECTROLYTIC_SEPARATOR);

    public static final DeferredBlock<MeRotaryCondensentratorBlock> ME_ROTARY_CONDENSENTRATOR =
            BLOCKS.registerBlock("me_rotary_condensentrator",
                    MeRotaryCondensentratorBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_ROTARY_CONDENSENTRATOR_ITEM =
            ITEMS.registerSimpleBlockItem("me_rotary_condensentrator", ME_ROTARY_CONDENSENTRATOR);

    public static final DeferredBlock<MeChemicalWasherBlock> ME_CHEMICAL_WASHER = BLOCKS.registerBlock(
            "me_chemical_washer", MeChemicalWasherBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_CHEMICAL_WASHER_ITEM =
            ITEMS.registerSimpleBlockItem("me_chemical_washer", ME_CHEMICAL_WASHER);

    public static final DeferredBlock<MeNutritionalLiquifierBlock> ME_NUTRITIONAL_LIQUIFIER =
            BLOCKS.registerBlock("me_nutritional_liquifier",
                    MeNutritionalLiquifierBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_NUTRITIONAL_LIQUIFIER_ITEM =
            ITEMS.registerSimpleBlockItem("me_nutritional_liquifier", ME_NUTRITIONAL_LIQUIFIER);

    public static final DeferredBlock<MePressurizedReactionChamberBlock> ME_PRESSURIZED_REACTION_CHAMBER =
            BLOCKS.registerBlock("me_pressurized_reaction_chamber",
                    MePressurizedReactionChamberBlock::new, machineProperties());
    public static final DeferredItem<BlockItem> ME_PRESSURIZED_REACTION_CHAMBER_ITEM =
            ITEMS.registerSimpleBlockItem("me_pressurized_reaction_chamber",
                    ME_PRESSURIZED_REACTION_CHAMBER);

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                .strength(5.0F, 6.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();
    }

    private ModBlocks() {
    }
}
