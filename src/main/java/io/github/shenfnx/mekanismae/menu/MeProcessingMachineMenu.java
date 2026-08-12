package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity;
import net.minecraft.network.chat.Component;

/** Read-only machine state consumed by the shared Mekanism-style screens. */
public interface MeProcessingMachineMenu {
    AbstractMeProcessingBlockEntity chamber();

    int energy();

    int maxEnergy();

    int progress();

    int pendingOperations();

    boolean networkOnline();

    boolean networkEnabled();

    int processingTicks();

    int speedUpgrades();

    int parallelUpgrades();

    int energyUpgrades();

    int maxEnergyReceive();

    int bufferOps();

    int bufferOpsCap();

    int parallelMultiplier();

    int speedMultiplier();

    int pendingOutputDisplay();

    boolean processingFaulted();

    Component statusText();
}
