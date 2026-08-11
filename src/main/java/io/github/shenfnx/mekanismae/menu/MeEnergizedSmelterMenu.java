package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeEnergizedSmelterBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeEnergizedSmelterMenu extends MeEnrichmentChamberMenu {
    public MeEnergizedSmelterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeEnergizedSmelterMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_ENERGIZED_SMELTER.get(), containerId, inventory, pos,
                MeEnergizedSmelterBlockEntity.class, "ME energized smelter");
    }
}
