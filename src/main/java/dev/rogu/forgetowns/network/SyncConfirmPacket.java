package dev.rogu.forgetowns.network;

import dev.rogu.forgetowns.ForgeTowns;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet sent from client to server to confirm receipt and application of fresh town data.
 */
public record SyncConfirmPacket(String townName) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ForgeTowns.MOD_ID, "sync_confirm");
    public static final Type<SyncConfirmPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, SyncConfirmPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, SyncConfirmPacket::townName,
        SyncConfirmPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the SyncConfirmPacket on the server side.
     * Expected signature for registration: (PayloadType, IPayloadContext)
     */
    public static void handle(final SyncConfirmPacket payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            // Player is required for server-side handling
            if (context.player() instanceof ServerPlayer player) {
                // Open menu or mark sync as complete for this player
                ForgeTowns.LOGGER.debug("[ForgeTowns] Received SyncConfirmPacket for town '{}' from player {}", payload.townName(), player.getName().getString());
                dev.rogu.forgetowns.data.TownSyncManager.onSyncConfirmed(player, payload.townName());
            } else {
                ForgeTowns.LOGGER.warn("[ForgeTowns] Received SyncConfirmPacket in a context without a ServerPlayer: {}", context.player());
            }
        });
    }
}
