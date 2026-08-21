package io.github.shenfnx.mekanismae.compat.appliedflux;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import com.glodblock.github.appflux.common.me.key.FluxKey;
import com.glodblock.github.appflux.common.me.key.type.EnergyType;
import com.glodblock.github.appflux.config.AFConfig;
import io.github.shenfnx.mekanismae.MekanismAeMod;
import io.github.shenfnx.mekanismae.block.entity.AbstractMeProcessingBlockEntity;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Optional Applied Flux item and network-energy integration. */
public final class AppliedFluxCompat {
    public static final ResourceLocation INDUCTION_CARD_ID =
            ResourceLocation.fromNamespaceAndPath("appflux", "induction_card");
    public static final int MAX_INDUCTION_CARDS = 1;

    private static boolean bridgeAvailable = true;

    private AppliedFluxCompat() {
    }

    public static Optional<Item> inductionCard() {
        if (!ModList.get().isLoaded("appflux")) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(INDUCTION_CARD_ID);
    }

    public static boolean isInductionCard(ItemStack stack) {
        return !stack.isEmpty()
                && INDUCTION_CARD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static void chargeFromNetwork(AbstractMeProcessingBlockEntity machine) {
        if (!bridgeAvailable || !ModList.get().isLoaded("appflux") || !machine.isNetworkOnline()) {
            return;
        }
        try {
            AppliedFluxBridge.chargeFromNetwork(machine);
        } catch (LinkageError error) {
            bridgeAvailable = false;
            MekanismAeMod.LOGGER.warn(
                    "Applied Flux induction-card integration is unavailable for this Applied Flux version", error);
        }
    }

    /** Keeps Applied Flux classes from being resolved when the optional mod is absent. */
    private static final class AppliedFluxBridge {
        private AppliedFluxBridge() {
        }

        private static void chargeFromNetwork(AbstractMeProcessingBlockEntity machine) {
            var grid = machine.getMainNode().getGrid();
            if (grid == null) {
                return;
            }

            IEnergyStorage energy = machine.getEnergyStorage();
            long configuredLimit = Math.max(0, AFConfig.getFluxAccessorIO());
            int transferLimit = (int) Math.min(Integer.MAX_VALUE, configuredLimit);
            int requested = energy.receiveEnergy(transferLimit, true);
            if (requested <= 0) {
                return;
            }

            var inventory = grid.getStorageService().getInventory();
            var source = IActionSource.ofMachine(machine);
            var key = FluxKey.of(EnergyType.FE);
            long extracted = inventory.extract(key, requested, Actionable.MODULATE, source);
            if (extracted <= 0) {
                return;
            }

            int received = energy.receiveEnergy((int) Math.min(Integer.MAX_VALUE, extracted), false);
            long remainder = extracted - received;
            if (remainder > 0) {
                inventory.insert(key, remainder, Actionable.MODULATE, source);
            }
        }
    }
}
