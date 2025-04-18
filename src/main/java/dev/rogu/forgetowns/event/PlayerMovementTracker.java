package dev.rogu.forgetowns.event;

import dev.rogu.forgetowns.ForgeTowns;
import dev.rogu.forgetowns.data.ClaimManager;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.util.MessageHelper;
import dev.rogu.forgetowns.util.TextFormatter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Tracks player movement between towns and displays welcome/farewell messages.
 */
public class PlayerMovementTracker {
    /**
     * Clears all static player tracking data. Call on world unload/server stop to prevent memory leaks.
     */
    public static void clearStaticData() {
        playerCurrentTownName.clear();
        playerLastChunkPos.clear();
    }
    
    // Map to track which town name each player is currently associated with
    private static final Map<UUID, String> playerCurrentTownName = new HashMap<>();
    // Map to track the last known chunk position for each player
    private static final Map<UUID, ChunkPos> playerLastChunkPos = new HashMap<>();
    private static ScheduledExecutorService scheduler;
    
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Only schedule on dedicated server (not on client or logical client)
        if (event.getServer().isDedicatedServer() || event.getServer().isSingleplayer()) {
            // Proceed
        } else {
            return;
        }
        // Set up a scheduled task to check player positions every 500ms (10 ticks)
        ForgeTowns.LOGGER.info(TextFormatter.consoleInfo("Initializing Town Movement Tracker"));
        
        // Create a daemon thread scheduler that won't prevent server shutdown
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ForgeTowns-MovementTracker");
            thread.setDaemon(true);
            return thread;
        });
        
        // Schedule the player position checking task
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MinecraftServer server = event.getServer();
                if (server != null && server.isRunning()) {
                    for (ServerLevel level : server.getAllLevels()) {
                        for (ServerPlayer player : level.players()) {
                            checkPlayerTownPosition(player);
                        }
                    }
                }
            } catch (Exception e) {
                ForgeTowns.LOGGER.error("Error in town movement tracker", e);
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Checks if a player has entered or left a town and sends appropriate messages,
     * but only if their chunk position has changed since the last check.
     */
    private static void checkPlayerTownPosition(ServerPlayer player) {
        // Get the chunk the player is currently in
        ChunkPos currentChunkPos = new ChunkPos(player.blockPosition());
        ChunkPos previousChunkPos = playerLastChunkPos.get(player.getUUID());
        
        // Only proceed if the chunk position is new or has changed
        if (previousChunkPos == null || !currentChunkPos.equals(previousChunkPos)) {
            // Get the town at the player's current position
            Town currentTown = ClaimManager.getTownAt(player.level(), currentChunkPos);
            String currentTownName = currentTown != null ? currentTown.getName() : null;
            
            // Get the town name the player was previously associated with
            String previousTownName = playerCurrentTownName.get(player.getUUID());
            
            // If the town status has changed, send appropriate messages
            if (currentTownName != null && !currentTownName.equals(previousTownName)) {
                // Player has entered a town (or moved from one town directly to another)
                player.sendSystemMessage(MessageHelper.styled(currentTownName, MessageHelper.MessageType.TOWN_WELCOME));
                playerCurrentTownName.put(player.getUUID(), currentTownName); // Update stored town name
            } else if (currentTownName == null && previousTownName != null) {
                // Player has left a town into the wilderness
                player.sendSystemMessage(MessageHelper.styled(previousTownName, MessageHelper.MessageType.TOWN_FAREWELL));
                playerCurrentTownName.remove(player.getUUID()); // Remove stored town name
            } else if (currentTownName != null && previousTownName == null) {
                // Player was in wilderness or just logged in, now entering a town
                player.sendSystemMessage(MessageHelper.styled(currentTownName, MessageHelper.MessageType.TOWN_WELCOME));
                playerCurrentTownName.put(player.getUUID(), currentTownName); // Update stored town name
            }
            // Note: No message if currentTownName == null && previousTownName == null (still in wilderness)
            // Note: No message if currentTownName != null && currentTownName.equals(previousTownName) (still in same town chunk)
            
            // Update the last known chunk position regardless of town status change
            playerLastChunkPos.put(player.getUUID(), currentChunkPos);
        }
        // If chunk hasn't changed, do nothing, even if position flickers near border
    }
    
    // Clean up when a player logs out
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Only manipulate server-side data
        if (event.getEntity() instanceof ServerPlayer player) {
            playerCurrentTownName.remove(player.getUUID());
            playerLastChunkPos.remove(player.getUUID()); // Remove from new map too
        }
    }
    
    // Initialize when a player logs in
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // Only manipulate server-side data
        if (event.getEntity() instanceof ServerPlayer player) {
            // Get the chunk the player is currently in
            ChunkPos currentChunkPos = new ChunkPos(player.blockPosition());
            playerLastChunkPos.put(player.getUUID(), currentChunkPos); // Initialize last chunk pos
            
            // Get the town at the player's current position
            Town currentTown = ClaimManager.getTownAt(player.level(), currentChunkPos);
            if (currentTown != null) {
                String currentTownName = currentTown.getName();
                playerCurrentTownName.put(player.getUUID(), currentTownName); // Initialize current town name
                // Send initial welcome message on login if starting in a town
                player.sendSystemMessage(MessageHelper.styled(currentTownName, MessageHelper.MessageType.TOWN_WELCOME));
            } else {
                playerCurrentTownName.remove(player.getUUID()); // Ensure no stale name if logging into wilderness
            }
        }
    }
}
