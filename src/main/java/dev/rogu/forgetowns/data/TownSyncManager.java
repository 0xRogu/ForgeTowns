package dev.rogu.forgetowns.data;

import dev.rogu.forgetowns.gui.TownMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages pending town menu opens and synchronization handshakes for town data.
 */
public class TownSyncManager {
    // Map: player UUID -> pending menu mode
    private static final Map<UUID, TownMenuProvider.MenuMode> pendingMenuOpens = new ConcurrentHashMap<>();
    // Map: player UUID -> town name (for which sync is pending)
    private static final Map<UUID, String> pendingTownSync = new ConcurrentHashMap<>();

    /**
     * Register a pending menu open for a player after sending town sync data.
     */
    public static void queueMenuOpen(ServerPlayer player, String townName, TownMenuProvider.MenuMode mode) {
        pendingMenuOpens.put(player.getUUID(), mode);
        pendingTownSync.put(player.getUUID(), townName);
    }

    /**
     * Called when the server receives a sync confirmation from the client.
     * If a menu open is pending for the player and town, open the menu now.
     */
    public static void onSyncConfirmed(ServerPlayer player, String townName) {
        UUID uuid = player.getUUID();
        if (townName.equals(pendingTownSync.get(uuid))) {
            TownMenuProvider.MenuMode mode = pendingMenuOpens.remove(uuid);
            pendingTownSync.remove(uuid);
            if (mode != null) {
                Town town = TownDataStorage.getTowns().get(townName);
                player.openMenu(
                    new TownMenuProvider(mode, town),
                    buf -> dev.rogu.forgetowns.gui.TownMenuProvider.encodeExtraData(buf, town, mode)
                );
            }
        }
    }

    /**
     * Clear all pending syncs (e.g., on player logout).
     */
    public static void clear(ServerPlayer player) {
        UUID uuid = player.getUUID();
        pendingMenuOpens.remove(uuid);
        pendingTownSync.remove(uuid);
    }
}
