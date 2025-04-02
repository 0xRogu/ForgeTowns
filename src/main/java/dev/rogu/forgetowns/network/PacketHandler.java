package dev.rogu.forgetowns.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;


/**
 * Handles network communication for the ForgeTowns mod.
 * Updated to use the new NeoForge 1.21.1 networking system.
 */
public class PacketHandler {

    // Packet registration is now handled in ForgeTowns.java using the RegisterPayloadHandlersEvent
    


    /**
     * Sends a packet from the server to a specific client.
     * This is a placeholder implementation until we can properly update
     * the networking system for NeoForge 1.21.1.
     */
    public static void sendToClient(CustomPacketPayload packet, ServerPlayer player) {
        // This will be implemented properly in a future update
    }
    
    /**
     * Sends a packet from the client to the server.
     * This is a placeholder implementation until we can properly update
     * the networking system for NeoForge 1.21.1.
     */
    public static void sendToServer(CustomPacketPayload packet) {
        // This will be implemented properly in a future update
    }
}
