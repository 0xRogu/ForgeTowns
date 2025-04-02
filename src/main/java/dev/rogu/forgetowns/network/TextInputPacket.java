package dev.rogu.forgetowns.network;

import dev.rogu.forgetowns.ForgeTowns;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

// Combine Packet definition and Handler logic for simplicity
public record TextInputPacket(String prompt) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ForgeTowns.MOD_ID, "text_input");
    public static final Type<TextInputPacket> TYPE = new Type<>(ID);
    public static final StreamCodec<ByteBuf, TextInputPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, TextInputPacket::prompt,
        TextInputPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * Handles the TextInputPacket on the server side.
     * Expected signature for registration: (PayloadType, IPayloadContext)
     */
    public static void handle(final TextInputPacket payload, final IPayloadContext context) {
        // Ensure logic runs on the main server thread
        context.enqueueWork(() -> {
            // Player is required for server-side handling
            if (context.player() instanceof ServerPlayer player) {
                // Send the prompt message to the player
                player.sendSystemMessage(Component.literal(payload.prompt));
                ForgeTowns.LOGGER.debug("Sent prompt '{}' to player {}", payload.prompt, player.getName().getString()); // Use ForgeTowns.LOGGER
            } else {
                // Log a warning if the context doesn't provide a ServerPlayer (should not happen in normal play)
                ForgeTowns.LOGGER.warn("Received TextInputPacket in a context without a ServerPlayer: {}", context.player()); // Use ForgeTowns.LOGGER
            }
        });
    }
}
