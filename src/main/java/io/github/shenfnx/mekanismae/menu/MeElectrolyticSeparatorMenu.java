package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeElectrolyticSeparatorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeElectrolyticSeparatorMenu extends AbstractMultiKeyMeMachineMenu {
    public MeElectrolyticSeparatorMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeElectrolyticSeparatorMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_ELECTROLYTIC_SEPARATOR.get(), containerId, inventory, pos,
                MeElectrolyticSeparatorBlockEntity.class, "ME electrolytic separator");
    }
}
