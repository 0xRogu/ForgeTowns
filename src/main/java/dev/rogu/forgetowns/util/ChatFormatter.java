package dev.rogu.forgetowns.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Utility class for sending formatted chat messages to players.
 */
public class ChatFormatter {

    /**
     * Sends a formatted town message to a player
     */
    public static void sendTownMessage(ServerPlayer player, String message) {
        MutableComponent prefix = Component.literal("[Town] ").withStyle(
            Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
        );
        
        MutableComponent content = Component.literal(message).withStyle(
            Style.EMPTY.withColor(ChatFormatting.WHITE)
        );
        
        player.sendSystemMessage(prefix.append(content));
    }
    
    /**
     * Sends a formatted nation message to a player
     */
    public static void sendNationMessage(ServerPlayer player, String message) {
        MutableComponent prefix = Component.literal("[Nation] ").withStyle(
            Style.EMPTY.withColor(ChatFormatting.BLUE).withBold(true)
        );
        
        MutableComponent content = Component.literal(message).withStyle(
            Style.EMPTY.withColor(ChatFormatting.WHITE)
        );
        
        player.sendSystemMessage(prefix.append(content));
    }
    
    /**
     * Sends a formatted plot message to a player
     */
    public static void sendPlotMessage(ServerPlayer player, String message) {
        MutableComponent prefix = Component.literal("[Plot] ").withStyle(
            Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true)
        );
        
        MutableComponent content = Component.literal(message).withStyle(
            Style.EMPTY.withColor(ChatFormatting.WHITE)
        );
        
        player.sendSystemMessage(prefix.append(content));
    }
    
    /**
     * Sends a formatted warning message to a player
     */
    public static void sendWarningMessage(ServerPlayer player, String message) {
        MutableComponent prefix = Component.literal("⚠ ").withStyle(
            Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
        );
        
        MutableComponent content = Component.literal(message).withStyle(
            Style.EMPTY.withColor(ChatFormatting.YELLOW)
        );
        
        player.sendSystemMessage(prefix.append(content));
    }
    
    /**
     * Sends a formatted success message to a player
     */
    public static void sendSuccessMessage(ServerPlayer player, String message) {
        MutableComponent prefix = Component.literal("✓ ").withStyle(
            Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
        );
        
        MutableComponent content = Component.literal(message).withStyle(
            Style.EMPTY.withColor(ChatFormatting.WHITE)
        );
        
        player.sendSystemMessage(prefix.append(content));
    }
}
