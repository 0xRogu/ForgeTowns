package dev.rogu.forgetowns.gui;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;



public class TownMenuProvider implements net.minecraft.world.MenuProvider {

    private final MenuMode mode;
    private final Town town;

    public TownMenuProvider(MenuMode mode) {
        this.mode = mode;
        this.town = null;
    }

    public TownMenuProvider(MenuMode mode, Town town) {
        this.mode = mode;
        this.town = town;
    }


    @Override
    public Component getDisplayName() {
        return Component.literal(
            town != null ? town.getName() + " Menu" : "Town Menu"
        );
    }

    @Override
    public AbstractContainerMenu createMenu(
        int containerId,
        Inventory playerInventory,
        Player player
    ) {
        Town playerTown = town != null ? town : findPlayerTown(player);
        return new TownMenu(containerId, playerInventory, playerTown, mode);
    }

    public enum MenuMode {
        MAIN, // General town management
        PLOT_MANAGEMENT, // Plot-specific options
        TOWN_CREATION, // Town creation menu
        TOWN_MANAGEMENT, // Town management menu
        NATION_MANAGEMENT, // Nation management menu
        TOWN_SETTINGS // Town settings menu
    }

    private static Town findPlayerTown(Player player) {
        for (Town t : TownDataStorage.getTowns().values()) {
            if (t.getResidents().contains(player.getUUID())) return t;
        }
        return null;
    }

    public static void encodeExtraData(
        FriendlyByteBuf buffer,
        Town town,
        MenuMode mode
    ) {
        buffer.writeUtf(town != null ? town.getName() : "");
        buffer.writeEnum(mode);
    }
}
