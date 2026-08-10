package io.github.shenfnx.mekanismae.compat.jade;

import io.github.shenfnx.mekanismae.block.MeEnrichmentChamberBlock;
import io.github.shenfnx.mekanismae.block.entity.MeEnrichmentChamberBlockEntity;
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
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                MeEnrichmentChamberJadeProvider.INSTANCE,
                MeEnrichmentChamberBlock.class);
    }
}
