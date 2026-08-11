package io.github.shenfnx.mekanismae.compat.jade;

import io.github.shenfnx.mekanismae.block.MeEnrichmentChamberBlock;
import io.github.shenfnx.mekanismae.block.MeCrusherBlock;
import io.github.shenfnx.mekanismae.block.MeMetallurgicInfuserBlock;
import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeCrusherBlockEntity;
import io.github.shenfnx.mekanismae.block.entity.MeMetallurgicInfuserBlockEntity;
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
                MeMetallurgicInfuserJadeProvider.INSTANCE,
                MeMetallurgicInfuserBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                MeEnrichmentChamberJadeProvider.INSTANCE,
                MeEnrichmentChamberBlock.class);
        registration.registerBlockComponent(MeCrusherJadeProvider.INSTANCE, MeCrusherBlock.class);
        registration.registerBlockComponent(
                MeMetallurgicInfuserJadeProvider.INSTANCE,
                MeMetallurgicInfuserBlock.class);
    }
}
