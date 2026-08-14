package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MeNutritionalLiquifierBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MeNutritionalLiquifierMenu extends AbstractMultiKeyMeMachineMenu {
    public MeNutritionalLiquifierMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MeNutritionalLiquifierMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_NUTRITIONAL_LIQUIFIER.get(), containerId, inventory, pos,
                MeNutritionalLiquifierBlockEntity.class, "ME nutritional liquifier");
    }
}
