package dev.rogu.forgetowns.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;


/**
 * Handles network communication for the ForgeTowns mod.
 * Updated to use the new NeoForge 1.21.1 networking system.
 */
public class PacketHandler {

    /**
     * Sends a packet from the server to a specific client using NeoForge 1.21.1 API.
     * @param packet The packet to send (must implement CustomPacketPayload)
     * @param player The target server-side player
     */
    public static void sendToClient(CustomPacketPayload packet, ServerPlayer player) {
        if (player.connection != null) {
            player.connection.send(packet);
        }
    }
    
    /**
     * Sends a packet from the client to the server using NeoForge 1.21.1 API.
     * @param packet The packet to send (must implement CustomPacketPayload)
     */
    public static void sendToServer(CustomPacketPayload packet) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.getConnection() != null) {
            mc.getConnection().send(packet);
        }
    }
}

