package io.github.shenfnx.mekanismae.registry;

import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
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

    private ModBlockEntities() {
    }
}
