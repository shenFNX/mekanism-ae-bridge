package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeRotaryCondensentratorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeRotaryCondensentratorMenu extends AbstractMultiKeyMeMachineMenu {
    public MeRotaryCondensentratorMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeRotaryCondensentratorMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_ROTARY_CONDENSENTRATOR.get(), containerId, inventory, pos,
                MeRotaryCondensentratorBlockEntity.class, "ME rotary condensentrator");
    }
}
