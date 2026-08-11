package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.MeEnrichmentChamberBlock;
import io.github.shenfnx.mekanismae.block.MeCrusherBlock;
import io.github.shenfnx.mekanismae.block.MeMetallurgicInfuserBlock;
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

    public static final DeferredBlock<MeMetallurgicInfuserBlock> ME_METALLURGIC_INFUSER = BLOCKS.registerBlock(
            "me_metallurgic_infuser",
            MeMetallurgicInfuserBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops());

    public static final DeferredItem<BlockItem> ME_METALLURGIC_INFUSER_ITEM =
            ITEMS.registerSimpleBlockItem("me_metallurgic_infuser", ME_METALLURGIC_INFUSER);

    private ModBlocks() {
    }
}
