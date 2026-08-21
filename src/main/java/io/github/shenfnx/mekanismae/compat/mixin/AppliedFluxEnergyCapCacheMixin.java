package io.github.shenfnx.mekanismae.compat.mixin;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Allows Applied Flux to supply FE stored in an ME network to this mod's
 * processing machines on that same network.
 *
 * <p>Applied Flux normally rejects every energy capability exposed by an AE2
 * node on the accessor's own grid to prevent power loops. These machines are
 * receive-only energy consumers, so treating their AE node as an external
 * target for this one check is safe. AE2 Lightning Tech's Overloaded Power
 * Supply reuses this cache for wireless output and benefits from the same fix.
 */
@Pseudo
@Mixin(targets = "com.glodblock.github.appflux.common.me.energy.EnergyCapCache", remap = false)
abstract class AppliedFluxEnergyCapCacheMixin {

    @Redirect(
            method = "checkGrid",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/IInWorldGridNodeHost;getGridNode(Lnet/minecraft/core/Direction;)Lappeng/api/networking/IGridNode;"),
            require = 0)
    @Nullable
    private IGridNode mekanismae$allowReceiveOnlyMachineOnSameGrid(
            IInWorldGridNodeHost host, Direction side) {
        if (host instanceof AbstractMeProcessingBlockEntity) {
            return null;
        }
        return host.getGridNode(side);
    }
}
