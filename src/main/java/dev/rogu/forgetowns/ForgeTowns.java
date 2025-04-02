package dev.rogu.forgetowns;

import dev.rogu.forgetowns.commands.NationCommand;
import dev.rogu.forgetowns.commands.TownCommand;
import dev.rogu.forgetowns.config.ForgeTownsConfig;
import dev.rogu.forgetowns.data.ClaimManager;
import dev.rogu.forgetowns.data.ModCapabilities;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.gui.TownMenu;
import dev.rogu.forgetowns.gui.TownMenuProvider;
import dev.rogu.forgetowns.item.PlotWandItem;

import dev.rogu.forgetowns.network.SyncTownDataPacket;
import dev.rogu.forgetowns.network.SyncTownDataPacketHandler;
import dev.rogu.forgetowns.network.TextInputPacket;
import dev.rogu.forgetowns.network.TextResponsePacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;


import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

@Mod("forgetowns")
public class ForgeTowns {

    public static final String MOD_ID = "forgetowns";
    public static final Logger LOGGER = LogUtils.getLogger();
    private int tickCounter = 0;
    private static final int TICKS_PER_DAY = 24000;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(
        Registries.ITEM,
        MOD_ID
    );
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredHolder<Item, Item> PLOT_WAND = ITEMS.register(
        "plot_wand",
        PlotWandItem::new
    );
    public static final DeferredHolder<MenuType<?>, MenuType<TownMenu>> TOWN_MENU =
        MENU_TYPES.register("town_menu", () -> TownMenuProvider.TYPE);

    public ForgeTowns(IEventBus modEventBus) {
        // Register config
        ForgeTownsConfig.register();
        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        // Use a different approach for server ticks
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        // Register a scheduled task instead of using the tick event
        NeoForge.EVENT_BUS.addListener(ClaimManager::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ClaimManager::onBlockPlace);
        NeoForge.EVENT_BUS.addListener(ClaimManager::onMultiBlockPlace);
        NeoForge.EVENT_BUS.addListener(ClaimManager::onExplosion);
        NeoForge.EVENT_BUS.addListener(ClaimManager::onInteract);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLogout);
        NeoForge.EVENT_BUS.register(ChatListener.class); // Register ChatListener here
        ITEMS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        // PacketHandler no longer needs to be registered as it doesn't have any @SubscribeEvent methods
        ModCapabilities.ATTACHMENT_TYPES.register(modEventBus); // Register Attachment Types
        modEventBus.addListener(this::registerPackets); // Register packet handlers
        modEventBus.addListener(this::addCreative);
    }
    
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PLOT_WAND.get());
        }
    }

    private void serverStarting(ServerStartingEvent event) {
        TownCommand.register(event.getServer().getCommands().getDispatcher());
        NationCommand.register(event.getServer().getCommands().getDispatcher());
    }

    private void onServerStarted(ServerStartingEvent event) {
        // Register the server tick task using a thread
        Thread tickThread = new Thread(() -> {
            while (true) {
                try {
                    // Sleep for 1 tick (50ms)
                    Thread.sleep(50);
                    
                    // Process town maintenance
                    tickCounter++;
                    if (tickCounter >= TICKS_PER_DAY) {
                        tickCounter = 0;
                        for (dev.rogu.forgetowns.data.Town town : TownDataStorage.getTowns().values()) {
                            if (!town.withdrawEmeralds(2)) {
                                town.getClaimedChunks().clear();
                            }
                        }
                    }
                    
                    // Process claim manager ticks for all levels
                    if (event.getServer().isRunning()) {
                        event.getServer().getAllLevels().forEach(ClaimManager::onTick);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    LOGGER.error("Error in tick thread", e);
                }
            }
        });
        tickThread.setDaemon(true);
        tickThread.setName("ForgeTowns-TickThread");
        tickThread.start();
    }

    private void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ClaimManager.plotSelections.remove(player.getUUID());
        }
    }

    // Method to register network packet handlers
    private void registerPackets(final RegisterPayloadHandlersEvent event) { // Corrected Event Type
        final PayloadRegistrar registrar = event.registrar(MOD_ID); // Keep it simple for now

        // Register SyncTownDataPacket (Server -> Client)
        registrar.playToClient(
            SyncTownDataPacket.TYPE,
            SyncTownDataPacket.CODEC,
            SyncTownDataPacketHandler::handle
        );

        // Register TextInputPacket (Client -> Server)
        registrar.playToServer(
            TextInputPacket.TYPE,
            TextInputPacket.CODEC,
            TextInputPacket::handle
        );

        // Register TextResponsePacket (Client -> Server)
        registrar.playToServer(
            TextResponsePacket.TYPE,
            TextResponsePacket.CODEC,
            TextResponsePacket::handle
        );
    }
}
