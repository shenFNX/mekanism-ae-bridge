package io.github.shenfnx.mekanismae.menu;

import io.github.shenfnx.mekanismae.block.entity.MePressurizedReactionChamberBlockEntity;
import io.github.shenfnx.mekanismae.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public final class MePressurizedReactionChamberMenu extends AbstractMultiKeyMeMachineMenu {
    public MePressurizedReactionChamberMenu(
            int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos());
    }

    public MePressurizedReactionChamberMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.ME_PRESSURIZED_REACTION_CHAMBER.get(), containerId, inventory, pos,
                MePressurizedReactionChamberBlockEntity.class, "ME pressurized reaction chamber");
    }
}
