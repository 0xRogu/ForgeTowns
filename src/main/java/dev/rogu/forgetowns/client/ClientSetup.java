package dev.rogu.forgetowns.client;

import dev.rogu.forgetowns.ForgeTowns;
import dev.rogu.forgetowns.gui.TownMenuProvider;
import dev.rogu.forgetowns.gui.TownScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Handles client-side initialization for the ForgeTowns mod.
 */
@EventBusSubscriber(modid = ForgeTowns.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    /**
     * Register client-side components during client setup.
     * This includes registering menu screens for the GUI.
     *
     * @param event The client setup event
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Client setup code if needed
        ForgeTowns.LOGGER.info("Client setup initialized");
    }
    
    @SubscribeEvent
    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        // Register the TownMenu screen
        event.register(TownMenuProvider.TYPE, TownScreen::new);
        ForgeTowns.LOGGER.info("Registered TownMenu screen");
    }
}
