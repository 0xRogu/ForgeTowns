package dev.rogu.forgetowns.network;

import dev.rogu.forgetowns.ForgeTowns;
import dev.rogu.forgetowns.gui.TownMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Changed to a record for simplicity
public record TextResponsePacket(int containerId, String response) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ForgeTowns.MOD_ID, "text_response");
    public static final Type<TextResponsePacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, TextResponsePacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, TextResponsePacket::containerId,
        ByteBufCodecs.STRING_UTF8, TextResponsePacket::response,
        TextResponsePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the TextResponsePacket on the server side.
     * Expected signature for registration: (PayloadType, IPayloadContext)
     */
    public static void handle(final TextResponsePacket payload, final IPayloadContext context) {
        // Ensure logic runs on the main server thread
        context.enqueueWork(() -> {
            // Player is required for server-side handling
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof TownMenu menu && menu.containerId == payload.containerId) {
                    menu.handleTextInput(player, payload.response);
                    ForgeTowns.LOGGER.debug("Processed response '{}' for container {} from player {}",
                        payload.response, payload.containerId, player.getName().getString()); // Use ForgeTowns.LOGGER
                } else {
                    ForgeTowns.LOGGER.warn("Received TextResponsePacket for wrong/closed menu (Container ID: {}, Menu: {}) from player {}",
                        payload.containerId, player.containerMenu, player.getName().getString()); // Use ForgeTowns.LOGGER
                }
            } else {
                 ForgeTowns.LOGGER.warn("Received TextResponsePacket in a context without a ServerPlayer: {}", context.player()); // Use ForgeTowns.LOGGER
            }
        });
    }
}
