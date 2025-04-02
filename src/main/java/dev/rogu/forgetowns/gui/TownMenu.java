package dev.rogu.forgetowns.gui;

import dev.rogu.forgetowns.data.ClaimManager;
import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.Plot;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.data.ClaimCapability; // Import ClaimCapability
import dev.rogu.forgetowns.data.ModCapabilities; // Import ModCapabilities
import dev.rogu.forgetowns.network.PacketHandler;
import dev.rogu.forgetowns.network.TextInputPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel; // Import ServerLevel
import net.minecraft.server.level.ServerPlayer; // Import ServerPlayer
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents; // Correct import path
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.UUID;

public class TownMenu extends AbstractContainerMenu {

    private final Town town;
    private final TownMenuProvider.MenuMode mode;
    
    /**
     * Gets the current menu mode.
     * 
     * @return The current menu mode
     */
    public TownMenuProvider.MenuMode getMode() {
        return mode;
    }
    private final IItemHandler inventory;
    private final Inventory playerInventory; // Add field to store player inventory

    public TownMenu(
        int containerId,
        Inventory playerInventory,
        Town town,
        TownMenuProvider.MenuMode mode
    ) {
        super(TownMenuProvider.TYPE, containerId);
        this.town = town;
        this.mode = mode;
        this.inventory = new ItemStackHandler(18); // 18 slots for all options (2 rows)
        this.playerInventory = playerInventory; // Initialize the field

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(
                        new Slot(
                            playerInventory,
                            col + row * 9 + 9,
                            8 + col * 18,
                            102 + row * 18
                        )
                    );
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 160));
        }

        // Custom slots based on mode
        if (town != null) {
            if (mode == TownMenuProvider.MenuMode.MAIN) {
                setupMainMenu(playerInventory.player);
            } else if (mode == TownMenuProvider.MenuMode.PLOT_MANAGEMENT) {
                setupPlotManagementMenu(playerInventory.player);
            }
        }
    }

    private void setupMainMenu(Player player) {
        boolean isOwner = town.getOwner().equals(player.getUUID());
        boolean isOwnerOrAssistant =
            isOwner || town.getAssistants().contains(player.getUUID());

        // Slot 0: Town Info
        // In NeoForge 1.21.1, we need to use a different approach for setting items in the inventory
        // The ItemStackHandler class implements IItemHandlerModifiable which provides the setStackInSlot method
        if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
            modifiableInventory.setStackInSlot(
                0,
                createNamedItem(Items.PAPER,
                    Component.literal(
                        "Town: " +
                        town.getName() +
                        "\nOwner: " +
                        getPlayerName(town.getOwner()) +
                        "\nResidents: " +
                        town.getResidents().size() +
                        "\nGovernment: " +
                        town.getGovernmentType().name()
                    )
                )
            );
        }
        addSlot(
            new SlotItemHandler(inventory, 0, 8, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            }
        );

        // Slot 1: Claim Chunk
        if (isOwnerOrAssistant) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    1,
                    createNamedItem(Items.MAP, 
                        Component.literal("Claim Chunk\nCost: 5 Emeralds")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 1, 26, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            ClaimManager.claimChunk(
                                town,
                                new ChunkPos(serverPlayer.blockPosition()),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 2: Unclaim Chunk
        if (isOwnerOrAssistant) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    2,
                    createNamedItem(Items.FILLED_MAP,
                        Component.literal("Unclaim Chunk")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 2, 44, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            ServerLevel serverLevel = serverPlayer.serverLevel(); // Get ServerLevel
                            ChunkPos pos = new ChunkPos(
                                serverPlayer.blockPosition()
                            );
                            // Check capability instead of old list
                            ClaimCapability capability = serverLevel.getChunk(pos.x, pos.z).getData(ModCapabilities.TOWN_CLAIM.get());
                            if (capability != null && town.getName().equals(capability.getTownName())) {
                                ClaimManager.unclaimChunk(town, pos, serverLevel); // Pass ServerLevel
                            }
                        }
                    }
                }
            );
        }

        // Slot 3: Invite Resident
        if (isOwnerOrAssistant) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    3,
                    createNamedItem(Items.PLAYER_HEAD,
                        Component.literal("Invite Resident")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 3, 62, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter player name to invite:"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 4: Kick Resident
        if (isOwner) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    4,
                    createNamedItem(Items.BARRIER,
                        Component.literal("Kick Resident")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 4, 80, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter player name to kick:"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 5: Promote to Assistant
        if (isOwner) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    5,
                    createNamedItem(Items.GOLDEN_HELMET,
                        Component.literal("Promote to Assistant")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 5, 98, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter player name to promote:"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 6: Demote Assistant
        if (isOwner) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    6,
                    createNamedItem(Items.IRON_HELMET,
                        Component.literal("Demote Assistant")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 6, 116, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter player name to demote:"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 7: Deposit Emeralds
        if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
            modifiableInventory.setStackInSlot(
                7,
                createNamedItem(Items.EMERALD,
                    Component.literal(
                        "Deposit Emeralds\nBalance: " + town.getEmeraldBalance()
                    )
                )
            );
        }
        addSlot(
            new SlotItemHandler(inventory, 7, 134, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() == Items.EMERALD;
                }

                @Override
                public void set(ItemStack stack) {
                    if (!stack.isEmpty()) {
                        int amount = stack.getCount();
                        town.depositEmeralds(amount);
                        stack.setCount(0);
                        setChanged();
                    }
                }
            }
        );

        // Slot 8: Withdraw Emeralds
        if (isOwner) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    8,
                    createNamedItem(Items.EMERALD_BLOCK,
                        Component.literal("Withdraw 9 Emeralds")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 8, 152, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (town.withdrawEmeralds(9)) {
                            p
                                .getInventory()
                                .add(new ItemStack(Items.EMERALD, 9));
                        }
                    }
                }
            );
        }

        // Slot 9: Set Government
        if (isOwner) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    9,
                    createNamedItem(Items.BOOK,
                        Component.literal(
                            "Set Government\nCurrent: " +
                            town.getGovernmentType().name()
                    )
                )
            );
            }
            addSlot(
                new SlotItemHandler(inventory, 9, 8, 38) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter government type (ANARCHY, DEMOCRACY, MONARCHY):"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 10: Plot Management
        if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
            modifiableInventory.setStackInSlot(
                10,
                createNamedItem(Items.OAK_SIGN,
                    Component.literal("Manage Plots")
                )
            );
        }
        addSlot(
            new SlotItemHandler(inventory, 10, 26, 38) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public void onTake(Player p, ItemStack stack) {
                    if (p instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(
                            new TownMenuProvider(
                                TownMenuProvider.MenuMode.PLOT_MANAGEMENT,
                                town
                            )
                        );
                    }
                }
            }
        );
    }

    private void setupPlotManagementMenu(Player player) {
        boolean isOwnerOrAssistant =
            town.getOwner().equals(player.getUUID()) ||
            town.getAssistants().contains(player.getUUID());
        Plot ownedPlot = town
            .getPlots()
            .stream()
            .filter(
                p ->
                    p.getOwner() != null &&
                    p.getOwner().equals(player.getUUID()) &&
                    p.getType() == Plot.PlotType.PURCHASABLE
            )
            .findFirst()
            .orElse(null);

        // Slot 0: Create Purchasable Plot
        if (isOwnerOrAssistant) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    0,
                    createNamedItem(Items.GOLD_INGOT,
                        Component.literal(
                            "Create Purchasable Plot\nPrice: 10 Emeralds"
                        )
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 0, 8, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            ClaimManager.createPlot(
                                serverPlayer,
                                town,
                                10,
                                Plot.PlotType.PURCHASABLE
                            );
                        }
                    }
                }
            );
        }

        // Slot 1: Create Community Plot
        if (isOwnerOrAssistant) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    1,
                    createNamedItem(Items.IRON_INGOT,
                        Component.literal(
                            "Create Community Plot\nPrice: 5 Emeralds"
                        )
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 1, 26, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            ClaimManager.createPlot(
                                serverPlayer,
                                town,
                                5,
                                Plot.PlotType.COMMUNITY
                            );
                        }
                    }
                }
            );
        }

        // Slot 2: Invite to Plot
        if (ownedPlot != null) {
            if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
                modifiableInventory.setStackInSlot(
                    2,
                    createNamedItem(Items.PLAYER_HEAD,
                        Component.literal("Invite to Your Plot")
                    )
                );
            }
            addSlot(
                new SlotItemHandler(inventory, 2, 44, 20) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player p, ItemStack stack) {
                        if (p instanceof ServerPlayer serverPlayer) {
                            PacketHandler.sendToClient(
                                new TextInputPacket("Enter player name to invite to your plot:"),
                                serverPlayer
                            );
                        }
                    }
                }
            );
        }

        // Slot 3: Plot Info
        if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
            modifiableInventory.setStackInSlot(
                3,
                createNamedItem(Items.BOOK,
                    Component.literal("Plots: " + town.getPlots().size())
                )
            );
        }
        addSlot(
            new SlotItemHandler(inventory, 3, 62, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            }
        );

        // Slot 4: Back to Main Menu
        if (inventory instanceof IItemHandlerModifiable modifiableInventory) {
            modifiableInventory.setStackInSlot(
                4,
                createNamedItem(Items.ARROW,
                    Component.literal("Back to Main Menu")
                )
            );
        }
        addSlot(
            new SlotItemHandler(inventory, 4, 80, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public void onTake(Player p, ItemStack stack) {
                    if (p instanceof ServerPlayer serverPlayer) {
                        serverPlayer.openMenu(
                            new TownMenuProvider(
                                TownMenuProvider.MenuMode.MAIN,
                                town
                            )
                        );
                    }
                }
            }
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return town != null && town.getResidents().contains(player.getUUID());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < 36) { // Player inventory to GUI
                if (!moveItemStackTo(stack, 36, slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else { // GUI to player inventory
                if (!moveItemStackTo(stack, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }
        return ItemStack.EMPTY;
    }

    public void handleTextInput(ServerPlayer player, String input) {
        if (mode == TownMenuProvider.MenuMode.MAIN) {
            if (
                slots
                    .get(36 + 3)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Invite")
            ) { // Invite Resident
                ServerPlayer target = player
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(input);
                if (
                    target != null &&
                    (town.getOwner().equals(player.getUUID()) ||
                        town.getAssistants().contains(player.getUUID()))
                ) {
                    town.addResident(target.getUUID());
                    player.sendSystemMessage(
                        Component.literal(
                            "Invited " + input + " to " + town.getName() + "!"
                        )
                    );
                    target.sendSystemMessage(
                        Component.literal(
                            "You’ve been invited to " + town.getName() + "!"
                        )
                    );
                }
            } else if (
                slots
                    .get(36 + 4)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Kick") &&
                town.getOwner().equals(player.getUUID())
            ) { // Kick Resident
                ServerPlayer target = player
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(input);
                if (
                    target != null &&
                    town.getResidents().contains(target.getUUID())
                ) {
                    town.removeResident(target.getUUID());
                    player.sendSystemMessage(
                        Component.literal(
                            "Kicked " + input + " from " + town.getName() + "!"
                        )
                    );
                    target.sendSystemMessage(
                        Component.literal(
                            "You’ve been kicked from " + town.getName() + "!"
                        )
                    );
                }
            } else if (
                slots
                    .get(36 + 5)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Promote") &&
                town.getOwner().equals(player.getUUID())
            ) { // Promote
                ServerPlayer target = player
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(input);
                if (
                    target != null &&
                    town.getResidents().contains(target.getUUID())
                ) {
                    town.addAssistant(target.getUUID());
                    player.sendSystemMessage(
                        Component.literal(
                            "Promoted " + input + " to Assistant!"
                        )
                    );
                    target.sendSystemMessage(
                        Component.literal(
                            "You’ve been promoted to Assistant in " +
                            town.getName() +
                            "!"
                        )
                    );
                }
            } else if (
                slots
                    .get(36 + 6)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Demote") &&
                town.getOwner().equals(player.getUUID())
            ) { // Demote
                ServerPlayer target = player
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(input);
                if (
                    target != null &&
                    town.getAssistants().contains(target.getUUID())
                ) {
                    town.getAssistants().remove(target.getUUID());
                    player.sendSystemMessage(
                        Component.literal("Demoted " + input + " to resident!")
                    );
                    target.sendSystemMessage(
                        Component.literal(
                            "You’ve been demoted to resident in " +
                            town.getName() +
                            "!"
                        )
                    );
                }
            } else if (
                slots
                    .get(36 + 9)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Set Government") &&
                town.getOwner().equals(player.getUUID())
            ) { // Government
                try {
                    GovernmentType type = GovernmentType.valueOf(
                        input.toUpperCase()
                    );
                    town.setGovernmentType(type);
                    player.sendSystemMessage(
                        Component.literal(
                            town.getName() +
                            " government set to " +
                            type.name() +
                            "!"
                        )
                    );
                } catch (IllegalArgumentException e) {
                    player.sendSystemMessage(
                        Component.literal(
                            "Invalid government type! Use: ANARCHY, DEMOCRACY, MONARCHY"
                        )
                    );
                }
            }
        } else if (mode == TownMenuProvider.MenuMode.PLOT_MANAGEMENT) {
            if (
                slots
                    .get(36 + 2)
                    .getItem()
                    .getHoverName()
                    .getString()
                    .startsWith("Invite to Your Plot")
            ) { // Invite to Plot
                ServerPlayer target = player
                    .getServer()
                    .getPlayerList()
                    .getPlayerByName(input);
                if (target != null) {
                    ClaimManager.inviteToPlot(player, town, target);
                }
            }
        }
        broadcastChanges();
    }

    private String getPlayerName(UUID uuid) {
        Player player = getPlayer()
            .level()
            .getServer()
            .getPlayerList()
            .getPlayer(uuid);
        return player != null ? player.getName().getString() : "Unknown";
    }
    
    private ItemStack createNamedItem(Item item, Component name) {
        // Create a basic ItemStack without setting a custom name for now
        // This is a temporary solution until we can properly investigate the correct method
        // for setting custom names in NeoForge 1.21.1
        ItemStack stack = new ItemStack(item);
        // Use DataComponents.CUSTOM_NAME to set the item's display name
        stack.set(DataComponents.CUSTOM_NAME, name);
        return stack;
    }

    private Player getPlayer() {
        // Access the player via the playerInventory field
        return this.playerInventory.player;
    }

    public TownMenu(
        int containerId,
        Inventory playerInventory,
        FriendlyByteBuf extraData
    ) {
        this(
            containerId,
            playerInventory,
            TownDataStorage.getTowns().get(extraData.readUtf()),
            extraData.readEnum(TownMenuProvider.MenuMode.class)
        );
    }
}
