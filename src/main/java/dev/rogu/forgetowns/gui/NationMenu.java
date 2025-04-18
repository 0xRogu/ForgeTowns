package dev.rogu.forgetowns.gui;

import dev.rogu.forgetowns.ForgeTowns;

import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.Nation;
import dev.rogu.forgetowns.data.TownDataStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import java.util.UUID;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.component.CustomData;

public class NationMenu extends AbstractContainerMenu {


    private final MenuMode mode;
    private final Nation nation;

    public enum MenuMode {
        MAIN,
        GOVERNMENT,
    }

    public NationMenu(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        super(ForgeTowns.NATION_MENU.get(), id);
        this.mode = MenuMode.MAIN;
        this.nation = findPlayerNation(playerInv.player);

        if (this.nation == null) return;

        if (mode == MenuMode.MAIN) {
            // Prepare Nation Info Slot
            DummyItemHandler infoHandler = new DummyItemHandler();
            ItemStack infoStack = new ItemStack(Items.BOOK);
            infoStack.set(DataComponents.CUSTOM_NAME, Component.literal("Nation Info"));
            CustomData infoCustomData = infoStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag infoTag = infoCustomData.copyTag();
            infoTag.putString(
                    "info",
                    "Leader: " + getPlayerName(nation.getLeaderTown().getOwner(), playerInv.player) +
                    "\nTowns: " + nation.getMemberTowns().size() +
                    "\nGovernment: " + nation.getGovernmentType().getName() +
                    "\nAt War: " + (nation.isAtWar() ? "Yes" : "No")
            );
            infoStack.set(DataComponents.CUSTOM_DATA, CustomData.of(infoTag));
            infoHandler.putStack(infoStack); // Set the stack on the handler
            addSlot(new SlotItemHandler(infoHandler, 0, 8, 16)); // Pass handler to slot

            // Prepare Nation Flag Slot
            DummyItemHandler flagHandler = new DummyItemHandler();
            ItemStack flagStack = new ItemStack(Items.WHITE_BANNER);
            flagStack.set(DataComponents.CUSTOM_NAME, Component.literal("Nation Flag"));
            // No custom NBT needed for flag currently
            flagHandler.putStack(flagStack); // Set the stack on the handler
            addSlot(new SlotItemHandler(flagHandler, 0, 26, 16)); // Pass handler to slot (index 0 for this handler)

            // Prepare Toggle War Slot
            DummyItemHandler warHandler = new DummyItemHandler();
            ItemStack warStack = new ItemStack(Items.DIAMOND_SWORD);
            warStack.set(DataComponents.CUSTOM_NAME, Component.literal("Toggle War"));
            CustomData warCustomData = warStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag warTag = warCustomData.copyTag();
            warTag.putString("info", "Toggle war status");
            warStack.set(DataComponents.CUSTOM_DATA, CustomData.of(warTag));
            warHandler.putStack(warStack); // Set the stack on the handler
            addSlot(new SlotItemHandler(warHandler, 0, 44, 16)); // Pass handler to slot
            
        } else if (mode == MenuMode.GOVERNMENT) {
            // Prepare Democracy Slot
            DummyItemHandler democracyHandler = new DummyItemHandler();
            ItemStack democracyStack = new ItemStack(Items.PAPER);
            democracyStack.set(DataComponents.CUSTOM_NAME, Component.literal("Democracy"));
            CustomData democracyCustomData = democracyStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag democracyTag = democracyCustomData.copyTag();
            democracyTag.putString(
                    "desc",
                    GovernmentType.DEMOCRACY.getDescription()
                );
            democracyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(democracyTag));
            democracyHandler.putStack(democracyStack);
            addSlot(new SlotItemHandler(democracyHandler, 0, 8, 16));

            // Prepare Monarchy Slot
            DummyItemHandler monarchyHandler = new DummyItemHandler();
            ItemStack monarchyStack = new ItemStack(Items.PAPER);
            monarchyStack.set(DataComponents.CUSTOM_NAME, Component.literal("Monarchy"));
            CustomData monarchyCustomData = monarchyStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag monarchyTag = monarchyCustomData.copyTag();
            monarchyTag.putString(
                    "desc",
                    GovernmentType.MONARCHY.getDescription()
                );
            monarchyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(monarchyTag));
            monarchyHandler.putStack(monarchyStack);
            addSlot(new SlotItemHandler(monarchyHandler, 0, 26, 16));

            // Prepare Anarchy Slot
            DummyItemHandler anarchyHandler = new DummyItemHandler();
            ItemStack anarchyStack = new ItemStack(Items.PAPER);
            anarchyStack.set(DataComponents.CUSTOM_NAME, Component.literal("Anarchy"));
            CustomData anarchyCustomData = anarchyStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag anarchyTag = anarchyCustomData.copyTag();
            anarchyTag.putString(
                    "desc",
                    GovernmentType.ANARCHY.getDescription()
                );
            anarchyStack.set(DataComponents.CUSTOM_DATA, CustomData.of(anarchyTag));
            anarchyHandler.putStack(anarchyStack);
            addSlot(new SlotItemHandler(anarchyHandler, 0, 44, 16));
        }

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(
                    new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18)
                );
            }
        }
        for (int k = 0; k < 9; k++) {
            addSlot(new Slot(playerInv, k, 8 + k * 18, 142));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (mode == MenuMode.MAIN) {
            if (
                id == 1 &&
                nation.getLeaderTown().getOwner().equals(player.getUUID())
            ) {
                ((ServerPlayer) player).openMenu(
                    new NationMenuProvider(MenuMode.GOVERNMENT)
                );
                return true;
            } else if (
                id == 2 &&
                nation.getLeaderTown().getOwner().equals(player.getUUID())
            ) {
                nation.setWar(!nation.isAtWar());
                player.sendSystemMessage(
                    Component.literal(
                        "War status toggled to " +
                        (nation.isAtWar() ? "active" : "inactive")
                    )
                );
                ((ServerPlayer) player).openMenu(
                    new NationMenuProvider(MenuMode.MAIN)
                );
                return true;
            }
        } else if (
            mode == MenuMode.GOVERNMENT &&
            nation.getLeaderTown().getOwner().equals(player.getUUID())
        ) {
            if (id >= 0 && id < GovernmentType.values().length) {
                nation.setGovernmentType(GovernmentType.values()[id]);
                player.sendSystemMessage(
                    Component.literal(
                        "Set nation government to " +
                        GovernmentType.values()[id].getName()
                    )
                );
                ((ServerPlayer) player).openMenu(
                    new NationMenuProvider(MenuMode.MAIN)
                );
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private Nation findPlayerNation(Player player) {
        for (Nation n : TownDataStorage.getNations().values()) {
            if (
                n.getLeaderTown().getOwner().equals(player.getUUID()) ||
                n
                    .getMemberTowns()
                    .stream()
                    .anyMatch(t -> t.getResidents().contains(player.getUUID()))
            ) {
                return n;
            }
        }
        return null;
    }

    private String getPlayerName(UUID uuid, Player player) {
        ServerPlayer serverPlayer = null;
        if (player != null && player.getServer() != null) {
            serverPlayer = player.getServer().getPlayerList().getPlayer(uuid);
        }
        return serverPlayer != null
            ? serverPlayer.getName().getString()
            : uuid.toString();
    }

    private static class DummyItemHandler implements IItemHandler {

        private ItemStack stack = ItemStack.EMPTY;

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stack;
        }

        @Override
        public ItemStack insertItem(
            int slot,
            ItemStack stack,
            boolean simulate
        ) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        public void putStack(ItemStack stack) {
            this.stack = stack;
        }
    }
}
