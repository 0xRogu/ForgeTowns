package dev.rogu.forgetowns.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Helper utility for enhancing messages throughout the ForgeTowns mod.
 * This provides a centralized way to apply consistent styling to messages.
 */
public class MessageHelper {

    /**
     * Creates a styled Component with appropriate ForgeTowns prefix and colors.
     *
     * @param message The message text (e.g., town name for welcome/farewell)
     * @param type The type of message (determines prefix and color scheme)
     * @return A styled Component ready to be sent
     */
    public static Component styled(String message, MessageType type) {
        MutableComponent prefix = Component.empty();
        MutableComponent mainContent = Component.literal(message); // Use input 'message' as main content by default
        Style contentStyle = Style.EMPTY.withColor(ChatFormatting.WHITE); // Default content color

        switch (type) {
            case TOWN_SUCCESS:
                prefix = Component.literal("[Town] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.GREEN);
                break;
            case TOWN_ERROR:
                prefix = Component.literal("[Town] ").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.RED);
                break;
            case TOWN_INFO:
                prefix = Component.literal("[Town] ").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.AQUA);
                break;
            case TOWN_WARNING:
                prefix = Component.literal("[Town] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.GOLD);
                break;
            case TOWN_WELCOME:
                prefix = Component.literal("✦ Welcome to ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
                mainContent = Component.literal(message).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true)); // Town name
                contentStyle = Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true); // Suffix color
                message = " ✦"; // Suffix text
                break;
            case TOWN_FAREWELL:
                prefix = Component.literal("✦ Thank you for visiting ").withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true));
                mainContent = Component.literal(message).withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withBold(true)); // Town name
                contentStyle = Style.EMPTY.withColor(ChatFormatting.AQUA).withBold(true); // Suffix color
                message = " ✦"; // Suffix text
                break;
            case TOWN_ADMIN_SUCCESS:
                prefix = Component.literal("[Admin] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.GREEN);
                break;
            case TOWN_ADMIN_ERROR:
                prefix = Component.literal("[Admin] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.RED);
                break;
            case NATION_SUCCESS:
                prefix = Component.literal("[Nation] ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GREEN).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
                break;
            case NATION_ERROR:
                prefix = Component.literal("[Nation] ").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_RED).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.DARK_RED);
                break;
            case NATION_INFO:
                prefix = Component.literal("[Nation] ").withStyle(Style.EMPTY.withColor(ChatFormatting.BLUE).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.BLUE);
                break;
            case PLOT_INFO:
                prefix = Component.literal("[Plot] ").withStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.GOLD);
                break;
            case GENERIC_SUCCESS:
                prefix = Component.literal("✓ ").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.WHITE); // White content for generic success
                break;
            case GENERIC_WARNING:
                prefix = Component.literal("⚠ ").withStyle(Style.EMPTY.withColor(ChatFormatting.RED).withBold(true));
                contentStyle = Style.EMPTY.withColor(ChatFormatting.YELLOW); // Yellow content for generic warning
                break;
            case GENERAL:
            default:
                // No prefix for general messages, default white content style is already set
                break;
        }

        // Append the main content first, then the potentially modified 'message' as suffix/content
        MutableComponent finalContent = Component.literal(message).withStyle(contentStyle);
        return prefix.append(mainContent).append(finalContent);
    }

    /**
     * Sends a styled message to a player using the ForgeTowns formatting.
     *
     * @param player The player to send the message to
     * @param message The message text
     * @param type The type of message (determines prefix and color scheme)
     */
    public static void sendMessage(ServerPlayer player, String message, MessageType type) {
        player.sendSystemMessage(styled(message, type));
    }

    /**
     * Enum defining different message types for consistent styling.
     */
    public enum MessageType {
        TOWN_SUCCESS,    // [Town] Green
        TOWN_ERROR,      // [Town] Red
        TOWN_INFO,       // [Town] Aqua
        TOWN_WARNING,    // [Town] Gold
        TOWN_WELCOME,    // Welcome to [TownName] (Green/Yellow)
        TOWN_FAREWELL,   // Thank you for visiting [TownName] (Aqua/Yellow)
        TOWN_ADMIN_SUCCESS,    // [Admin] Green
        TOWN_ADMIN_ERROR,      // [Admin] Red
        NATION_SUCCESS,  // [Nation] Dark Green
        NATION_ERROR,    // [Nation] Dark Red
        NATION_INFO,     // [Nation] Blue
        PLOT_INFO,       // [Plot] Gold
        GENERIC_SUCCESS, // Green prefix, White text
        GENERIC_WARNING, // Red prefix, Yellow text
        GENERAL          // No prefix, White text
    }
}
