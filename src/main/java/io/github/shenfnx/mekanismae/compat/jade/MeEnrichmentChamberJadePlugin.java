package io.github.shenfnx.mekanismae.compat.jade;

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
import io.github.shenfnx.mekanismae.block.entity.MeAntiprotonicNucleosynthesizerBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalDissolutionChamberBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeChemicalInfuserBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class MeEnrichmentChamberJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                MeEnrichmentChamberJadeProvider.INSTANCE,
                MeEnrichmentChamberBlockEntity.class);
        registration.registerBlockDataProvider(MeCrusherJadeProvider.INSTANCE, MeCrusherBlockEntity.class);
        registration.registerBlockDataProvider(
                MeEnergizedSmelterJadeProvider.INSTANCE, MeEnergizedSmelterBlockEntity.class);
        registration.registerBlockDataProvider(
                MeMetallurgicInfuserJadeProvider.INSTANCE,
                MeMetallurgicInfuserBlockEntity.class);
        registration.registerBlockDataProvider(
                MeOsmiumCompressorJadeProvider.INSTANCE, MeOsmiumCompressorBlockEntity.class);
        registration.registerBlockDataProvider(
                MePurificationChamberJadeProvider.INSTANCE, MePurificationChamberBlockEntity.class);
        registration.registerBlockDataProvider(
                MeChemicalInjectionChamberJadeProvider.INSTANCE, MeChemicalInjectionChamberBlockEntity.class);
        registration.registerBlockDataProvider(MeCombinerJadeProvider.INSTANCE, MeCombinerBlockEntity.class);
        registration.registerBlockDataProvider(
                MePrecisionSawmillJadeProvider.INSTANCE, MePrecisionSawmillBlockEntity.class);
        registration.registerBlockDataProvider(
                MeChemicalOxidizerJadeProvider.INSTANCE, MeChemicalOxidizerBlockEntity.class);
        registration.registerBlockDataProvider(
                MeChemicalCrystallizerJadeProvider.INSTANCE, MeChemicalCrystallizerBlockEntity.class);
        registration.registerBlockDataProvider(MeAntiprotonicNucleosynthesizerJadeProvider.INSTANCE,
                MeAntiprotonicNucleosynthesizerBlockEntity.class);
        registration.registerBlockDataProvider(MeChemicalDissolutionChamberJadeProvider.INSTANCE,
                MeChemicalDissolutionChamberBlockEntity.class);
        registration.registerBlockDataProvider(
                MeChemicalInfuserJadeProvider.INSTANCE, MeChemicalInfuserBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                MeEnrichmentChamberJadeProvider.INSTANCE,
                MeEnrichmentChamberBlock.class);
        registration.registerBlockComponent(MeCrusherJadeProvider.INSTANCE, MeCrusherBlock.class);
        registration.registerBlockComponent(MeEnergizedSmelterJadeProvider.INSTANCE, MeEnergizedSmelterBlock.class);
        registration.registerBlockComponent(
                MeMetallurgicInfuserJadeProvider.INSTANCE,
                MeMetallurgicInfuserBlock.class);
        registration.registerBlockComponent(
                MeOsmiumCompressorJadeProvider.INSTANCE, MeOsmiumCompressorBlock.class);
        registration.registerBlockComponent(
                MePurificationChamberJadeProvider.INSTANCE, MePurificationChamberBlock.class);
        registration.registerBlockComponent(
                MeChemicalInjectionChamberJadeProvider.INSTANCE, MeChemicalInjectionChamberBlock.class);
        registration.registerBlockComponent(MeCombinerJadeProvider.INSTANCE, MeCombinerBlock.class);
        registration.registerBlockComponent(
                MePrecisionSawmillJadeProvider.INSTANCE, MePrecisionSawmillBlock.class);
        registration.registerBlockComponent(
                MeChemicalOxidizerJadeProvider.INSTANCE, MeChemicalOxidizerBlock.class);
        registration.registerBlockComponent(
                MeChemicalCrystallizerJadeProvider.INSTANCE, MeChemicalCrystallizerBlock.class);
        registration.registerBlockComponent(MeAntiprotonicNucleosynthesizerJadeProvider.INSTANCE,
                MeAntiprotonicNucleosynthesizerBlock.class);
        registration.registerBlockComponent(MeChemicalDissolutionChamberJadeProvider.INSTANCE,
                MeChemicalDissolutionChamberBlock.class);
        registration.registerBlockComponent(
                MeChemicalInfuserJadeProvider.INSTANCE, MeChemicalInfuserBlock.class);
    }
}
