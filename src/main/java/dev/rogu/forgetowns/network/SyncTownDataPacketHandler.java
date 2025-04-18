package dev.rogu.forgetowns.network;

import dev.rogu.forgetowns.data.Town;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import dev.rogu.forgetowns.client.ClientTownData;

/**
 * Handler for the SyncTownDataPacket on the client side.
 */
public class SyncTownDataPacketHandler {

    // Get the singleton instance of the handler
    private static final SyncTownDataPacketHandler INSTANCE = new SyncTownDataPacketHandler();
    public static SyncTownDataPacketHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Handles the SyncTownDataPacket on the client side.
     * Expected signature for registration: (PayloadType, IPayloadContext)
     */
    public static void handle(final SyncTownDataPacket payload, final IPayloadContext context) {
        // Ensure logic runs on the main client thread
        context.enqueueWork(() -> {
            Town receivedTown = payload.reconstructTown();
            // Update the client-side storage with the received town data
            ClientTownData.updateTown(receivedTown);
            // Send SyncConfirmPacket to the server to confirm sync
            dev.rogu.forgetowns.network.PacketHandler.sendToServer(
                new dev.rogu.forgetowns.network.SyncConfirmPacket(receivedTown.getName())
            );
        });
    }
}
