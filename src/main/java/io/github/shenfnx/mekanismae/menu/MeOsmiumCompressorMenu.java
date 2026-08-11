package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeOsmiumCompressorBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeOsmiumCompressorMenu extends AbstractItemChemicalToItemMeMachineMenu {
    public MeOsmiumCompressorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeOsmiumCompressorMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_OSMIUM_COMPRESSOR.get(), containerId, inventory, pos,
                MeOsmiumCompressorBlockEntity.class, "ME osmium compressor");
    }
}
