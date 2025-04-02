package dev.rogu.forgetowns.client;

import dev.rogu.forgetowns.data.Town;
import net.minecraft.client.Minecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manages town data on the client side.
 * This data is typically received from the server via packets.
 */
public class ClientTownData {

    private static final Map<String, Town> clientTowns = new HashMap<>();

    /**
     * Updates or adds town data received from the server.
     * Ensures this runs on the client thread if called from a network thread.
     *
     * @param town The town data received.
     */
    public static void updateTown(Town town) {
        if (town == null) return;

        // If not on the client thread (e.g., called from packet handler), enqueue the work
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> {
                clientTowns.put(town.getName(), town);
                System.out.println("[ForgeTowns DEBUG Client] Updated/Added client town data for: " + town.getName());
            });
        } else {
            // Already on the client thread
            clientTowns.put(town.getName(), town);
            System.out.println("[ForgeTowns DEBUG Client] Updated/Added client town data for: " + town.getName());
        }
    }

    /**
     * Retrieves town data by name.
     *
     * @param name The name of the town.
     * @return An Optional containing the Town if found, otherwise empty.
     */
    public static Optional<Town> getTown(String name) {
        return Optional.ofNullable(clientTowns.get(name));
    }

    /**
     * Clears all client-side town data (e.g., when disconnecting from a server).
     */
    public static void clearTowns() {
         // If not on the client thread, enqueue the work
        if (!Minecraft.getInstance().isSameThread()) {
            Minecraft.getInstance().execute(() -> {
                 clientTowns.clear();
                 System.out.println("[ForgeTowns DEBUG Client] Cleared all client town data.");
            });
        } else {
            clientTowns.clear();
            System.out.println("[ForgeTowns DEBUG Client] Cleared all client town data.");
        }
    }
}
