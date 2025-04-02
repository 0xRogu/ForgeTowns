package dev.rogu.forgetowns;

import dev.rogu.forgetowns.gui.TownMenu;
import dev.rogu.forgetowns.network.PacketHandler;
import dev.rogu.forgetowns.network.TextResponsePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

public class ChatListener {

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        AbstractContainerMenu containerMenu = player.containerMenu;
        if (containerMenu instanceof TownMenu menu) {
            String message = event.getMessage().getString();
            // Use the direct method instead of the static field
            PacketHandler.sendToServer(
                new TextResponsePacket(menu.containerId, message)
            );
            event.setCanceled(true); // Prevent chat message from broadcasting
        }
    }
}
