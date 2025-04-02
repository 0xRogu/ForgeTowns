package dev.rogu.forgetowns.gui;

import io.netty.buffer.Unpooled;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class NationMenuProvider implements MenuProvider {

    private final NationMenu.MenuMode mode;

    public NationMenuProvider(NationMenu.MenuMode mode) {
        this.mode = mode;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(
            switch (mode) {
                case MAIN -> "Nation Menu";
                case GOVERNMENT -> "Select Government";
            }
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
        int id,
        Inventory playerInv,
        Player player
    ) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        return new NationMenu(id, playerInv, buf);
    }
}
