package io.github.shenfnx.mekanismae;

import io.github.shenfnx.mekanismae.client.MekanismAeGuide;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = MekanismAeMod.MOD_ID, dist = Dist.CLIENT)
public final class MekanismAeClient {
    public MekanismAeClient(ModContainer container) {
        MekanismAeGuide.initialize();
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
