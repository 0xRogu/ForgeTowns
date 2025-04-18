package dev.rogu.forgetowns.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogu.forgetowns.data.ClaimManager;
import dev.rogu.forgetowns.data.ClaimResult;
import dev.rogu.forgetowns.data.ClaimCapability;
import dev.rogu.forgetowns.ForgeTowns; // Import ForgeTowns for item access
import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.ModCapabilities;
import dev.rogu.forgetowns.data.Plot;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.gui.TownMenuProvider;
import dev.rogu.forgetowns.util.MessageHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;

/**
 * Handles the /town command.
 */
public class TownCommand {

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
            Commands.literal("town")
                // Create a new town
                .then(
                    Commands.literal("create").then(
                        Commands.argument(
                            "name",
                            StringArgumentType.string()
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            String name = StringArgumentType.getString(
                                ctx,
                                "name"
                            );
                            if (TownDataStorage.getTowns().containsKey(name)) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Town already exists!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            // Prevent player from creating a new town if already in one
                            if (findPlayerTown(player) != null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You are already a member of a town! Leave your current town before creating a new one.",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            // Deduct emeralds for town creation
                            int cost = dev.rogu.forgetowns.config.ForgeTownsConfig.townCreationCost;
                            int removed = ClaimManager.removeItems(player, Items.EMERALD, cost);
                            if (removed < cost) {
                                ctx.getSource().sendFailure(
                                    MessageHelper.styled(
                                        "You need " + cost + " emeralds to create a town!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                );
                                return 0;
                            }
                            Town town = new Town(
                                name,
                                player.getUUID(),
                                player.blockPosition()
                            );
                            town.setLevel(player.level());
                            TownDataStorage.getTowns().put(name, town);
                            ctx
                                .getSource()
                                .sendSystemMessage(
                                    MessageHelper.styled(
                                        "Town " + name + " created!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                ); // Only send once
                            return 1;
                        })
                    )
                )
                // Claim a chunk for the town
                .then(
                    Commands.literal("claim").executes(ctx -> {
                        ServerPlayer player = ctx
                            .getSource()
                            .getPlayerOrException();
                        Town town = findPlayerTown(player);
                        if (town == null) {
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "You’re not in a town!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                );
                            return 0;
                        }
                        if (
                            !town.getOwner().equals(player.getUUID()) &&
                            !town.getAssistants().contains(player.getUUID())
                        ) {
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "Only Owners and Assistants can claim chunks!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                ); // Only send once
                            return 0;
                        }
                        ChunkPos pos = new ChunkPos(player.blockPosition());
                        ClaimResult claimResult = ClaimManager.claimChunk(town, pos, player);
if (claimResult.success) {
                            ctx.getSource().sendSystemMessage(
    MessageHelper.styled(
        claimResult.message,
        claimResult.type
    )
);
return 1;
} else {
    ctx.getSource().sendFailure(
        MessageHelper.styled(
            claimResult.message,
            claimResult.type
        )
    );
    return 0;
                        }
                    })
                )
                // Unclaim a chunk from the town
                .then(
                    Commands.literal("unclaim").executes(ctx -> {
                        ServerPlayer player = ctx
                            .getSource()
                            .getPlayerOrException();
                        Town town = findPlayerTown(player);
                        if (town == null) {
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "You’re not in a town!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                );
                            return 0;
                        }
                        if (
                            !town.getOwner().equals(player.getUUID()) &&
                            !town.getAssistants().contains(player.getUUID())
                        ) {
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "Only Owners and Assistants can unclaim chunks!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                );
                            return 0;
                        }
                        ServerLevel serverLevel = player.serverLevel();
                        ChunkPos pos = new ChunkPos(player.blockPosition());
                        // Check if the chunk has the claim capability associated with this town
                        ClaimCapability capability = serverLevel.getChunk(pos.x, pos.z).getData(ModCapabilities.TOWN_CLAIM.get());
                        if (capability != null && town.getName().equals(capability.getTownName())) {
                            ClaimManager.unclaimChunk(town, pos, serverLevel);
                            ctx
                                .getSource()
                                .sendSystemMessage(
                                    MessageHelper.styled(
                                        "Chunk unclaimed from " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                );
                            return 1;
                        } else {
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "This chunk isn’t claimed by " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_ERROR
                                    )
                                );
                            return 0;
                        }
                    })
                )
                // Invite a player to the town
                .then(
                    Commands.literal("invite").then(
                        Commands.argument(
                            "player",
                            EntityArgument.player()
                        ).executes(ctx -> {
                            ServerPlayer owner = ctx
                                .getSource()
                                .getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(
                                ctx,
                                "player"
                            );
                            Town town = findPlayerTown(owner);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (
                                !town.getOwner().equals(owner.getUUID()) &&
                                !town.getAssistants().contains(owner.getUUID())
                            ) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only Owners and Assistants can invite!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            town.addResident(target.getUUID());
                            ctx
                                .getSource()
                                .sendSystemMessage(
                                    MessageHelper.styled(
                                        target.getName().getString() +
                                        " invited to " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                );
                            target.sendSystemMessage(
                                MessageHelper.styled(
                                    "You’ve been invited to " +
                                    town.getName() +
                                    "!",
                                    MessageHelper.MessageType.TOWN_SUCCESS
                                )
                            );
                            return 1;
                        })
                    )
                )
                // Kick a player from the town
                .then(
                    Commands.literal("kick").then(
                        Commands.argument(
                            "player",
                            EntityArgument.player()
                        ).executes(ctx -> {
                            ServerPlayer owner = ctx
                                .getSource()
                                .getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(
                                ctx,
                                "player"
                            );
                            Town town = findPlayerTown(owner);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only the Owner can kick!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (
                                town.getResidents().contains(target.getUUID())
                            ) {
                                town.removeResident(target.getUUID());
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " kicked from " +
                                            town.getName() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                target.sendSystemMessage(
                                    MessageHelper.styled(
                                        "You’ve been kicked from " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " isn’t a resident!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Promote a resident to assistant
                .then(
                    Commands.literal("promote").then(
                        Commands.argument(
                            "player",
                            EntityArgument.player()
                        ).executes(ctx -> {
                            ServerPlayer owner = ctx
                                .getSource()
                                .getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(
                                ctx,
                                "player"
                            );
                            Town town = findPlayerTown(owner);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only the Owner can promote!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (
                                town.getResidents().contains(target.getUUID())
                            ) {
                                town.addAssistant(target.getUUID());
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " promoted to Assistant in " +
                                            town.getName() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                target.sendSystemMessage(
                                    MessageHelper.styled(
                                        "You’ve been promoted to Assistant in " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " isn’t a resident!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Demote an assistant to resident
                .then(
                    Commands.literal("demote").then(
                        Commands.argument(
                            "player",
                            EntityArgument.player()
                        ).executes(ctx -> {
                            ServerPlayer owner = ctx
                                .getSource()
                                .getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(
                                ctx,
                                "player"
                            );
                            Town town = findPlayerTown(owner);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only the Owner can demote!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (
                                town.getAssistants().contains(target.getUUID())
                            ) {
                                town.getAssistants().remove(target.getUUID());
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " demoted to resident in " +
                                            town.getName() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                target.sendSystemMessage(
                                    MessageHelper.styled(
                                        "You’ve been demoted to resident in " +
                                        town.getName() +
                                        "!",
                                        MessageHelper.MessageType.TOWN_SUCCESS
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            target.getName().getString() +
                                            " isn’t an Assistant!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Deposit emeralds into the town treasury
                .then(
                    Commands.literal("deposit").then(
                        Commands.argument(
                            "amount",
                            IntegerArgumentType.integer(1)
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            Town town = findPlayerTown(player);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            int amount = IntegerArgumentType.getInteger(
                                ctx,
                                "amount"
                            );
                            if (
                                player
                                    .getInventory()
                                    .countItem(Items.EMERALD) >=
                                amount
                            ) {
                                player
                                    .getInventory()
                                    .removeItem(new ItemStack(Items.EMERALD, amount));
                                town.depositEmeralds(amount);
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            amount +
                                            " emeralds deposited into " +
                                            town.getName() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You don’t have enough emeralds!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Withdraw emeralds from the town treasury
                .then(
                    Commands.literal("withdraw").then(
                        Commands.argument(
                            "amount",
                            IntegerArgumentType.integer(1)
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            Town town = findPlayerTown(player);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(player.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only the Owner can withdraw!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            int amount = IntegerArgumentType.getInteger(
                                ctx,
                                "amount"
                            );
                            if (town.withdrawEmeralds(amount)) {
                                player
                                    .getInventory()
                                    .add(new ItemStack(Items.EMERALD, amount));
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            amount +
                                            " emeralds withdrawn from " +
                                            town.getName() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Not enough emeralds in " +
                                            town.getName() +
                                            "'s treasury!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Set the government type
                .then(
                    Commands.literal("government").then(
                        Commands.argument(
                            "type",
                            StringArgumentType.string()
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            Town town = findPlayerTown(player);
                            if (town == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You’re not in a town!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(player.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Only the Owner can set the government type!",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                            String typeStr = StringArgumentType.getString(
                                ctx,
                                "type"
                            ).toUpperCase();
                            try {
                                GovernmentType type = GovernmentType.valueOf(
                                    typeStr
                                );
                                town.setGovernmentType(type);
                                ctx
                                    .getSource()
                                    .sendSystemMessage(
                                        MessageHelper.styled(
                                            town.getName() +
                                            " government set to " +
                                            type.name() +
                                            "!",
                                            MessageHelper.MessageType.TOWN_SUCCESS
                                        )
                                    );
                                return 1;
                            } catch (IllegalArgumentException e) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Invalid government type! Use: ANARCHY, DEMOCRACY, MONARCHY",
                                            MessageHelper.MessageType.TOWN_ERROR
                                        )
                                    );
                                return 0;
                            }
                        })
                    )
                )
                // Plot-related commands
                .then(
                    Commands.literal("plot")
                        // Create a plot with optional type
                        .then(
                            Commands.literal("create").then(
                                Commands.argument(
                                    "price",
                                    IntegerArgumentType.integer(1)
                                )
                                    .then(
                                        Commands.argument(
                                            "type",
                                            StringArgumentType.string()
                                        ).executes(ctx -> {
                                            ServerPlayer player = ctx
                                                .getSource()
                                                .getPlayerOrException();
                                            Town town = findPlayerTown(player);
                                            if (
                                                town == null ||
                                                (!town
                                                        .getOwner()
                                                        .equals(
                                                            player.getUUID()
                                                        ) &&
                                                    !town
                                                        .getAssistants()
                                                        .contains(
                                                            player.getUUID()
                                                        ))
                                            ) {
                                                ctx
                                                    .getSource()
                                                    .sendFailure(
                                                        MessageHelper.styled(
                                                            "You must be an Owner or Assistant!",
                                                            MessageHelper.MessageType.TOWN_ERROR
                                                        )
                                                    );
                                                return 0;
                                            }
                                            int price =
                                                IntegerArgumentType.getInteger(
                                                    ctx,
                                                    "price"
                                                );
                                            String typeStr =
                                                StringArgumentType.getString(
                                                    ctx,
                                                    "type"
                                                ).toUpperCase();
                                            Plot.PlotType type;
                                            try {
                                                type = Plot.PlotType.valueOf(
                                                    typeStr
                                                );
                                            } catch (
                                                IllegalArgumentException e
                                            ) {
                                                ctx
                                                    .getSource()
                                                    .sendFailure(
                                                        MessageHelper.styled(
                                                            "Invalid plot type! Use: purchasable, community",
                                                            MessageHelper.MessageType.TOWN_ERROR
                                                        )
                                                    );
                                                return 0;
                                            }
                                            ClaimManager.createPlot(
                                                player,
                                                town,
                                                price,
                                                type
                                            );
                                            return 1;
                                        })
                                    )
                                    .executes(ctx -> {
                                        ServerPlayer player = ctx
                                            .getSource()
                                            .getPlayerOrException();
                                        Town town = findPlayerTown(player);
                                        if (
                                            town == null ||
                                            (!town
                                                    .getOwner()
                                                    .equals(player.getUUID()) &&
                                                !town
                                                    .getAssistants()
                                                    .contains(player.getUUID()))
                                        ) {
                                            ctx
                                                .getSource()
                                                .sendFailure(
                                                    MessageHelper.styled(
                                                        "You must be an Owner or Assistant!",
                                                        MessageHelper.MessageType.TOWN_ERROR
                                                    )
                                                );
                                            return 0;
                                        }
                                        int price =
                                            IntegerArgumentType.getInteger(
                                                ctx,
                                                "price"
                                            );
                                        ClaimManager.createPlot(
                                            player,
                                            town,
                                            price,
                                            Plot.PlotType.PURCHASABLE
                                        );
                                        return 1;
                                    })
                            )
                        )
                        // Invite a player to a purchasable plot
                        .then(
                            Commands.literal("invite").then(
                                Commands.argument(
                                    "player",
                                    EntityArgument.player()
                                ).executes(ctx -> {
                                    ServerPlayer owner = ctx
                                        .getSource()
                                        .getPlayerOrException();
                                    ServerPlayer target =
                                        EntityArgument.getPlayer(ctx, "player");
                                    Town town = findPlayerTown(owner);
                                    if (town == null) {
                                        ctx
                                            .getSource()
                                            .sendFailure(
                                                MessageHelper.styled(
                                                    "You’re not in a town!",
                                                    MessageHelper.MessageType.TOWN_ERROR
                                                )
                                            );
                                        return 0;
                                    }
                                    ClaimManager.inviteToPlot(
                                        owner,
                                        town,
                                        target
                                    );
                                    return 1;
                                })
                            )
                        )
                )
                // Give plot wand (Owner/Assistant only)
                .then(Commands.literal("give")
                    .then(Commands.literal("wand")
                        // /town give wand
                        .executes(ctx -> givePlotWandTownMember(ctx.getSource(), ctx.getSource().getPlayerOrException(), ctx.getSource().getPlayerOrException(), 1))
                        // /town give wand [count]
                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                            .executes(ctx -> givePlotWandTownMember(ctx.getSource(), ctx.getSource().getPlayerOrException(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "count")))
                        )
                        // /town give wand [player]
                        .then(Commands.argument("player", EntityArgument.player())
                            .executes(ctx -> givePlotWandTownMember(ctx.getSource(), ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), 1))
                            // /town give wand [player] [count]
                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> givePlotWandTownMember(ctx.getSource(), ctx.getSource().getPlayerOrException(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count")))
                            )
                        )
                    )
                )
                // Open the town menu
                .then(
                    Commands.literal("menu").executes(ctx -> {
                        ServerPlayer player = ctx
                            .getSource()
                            .getPlayerOrException();
                        // Find the town the player belongs to (using existing helper method)
                        Town playerTown = findPlayerTown(player);
                        if (playerTown != null) {
    dev.rogu.forgetowns.data.TownSyncManager.queueMenuOpen(player, playerTown.getName(), TownMenuProvider.MenuMode.MAIN);
    dev.rogu.forgetowns.network.PacketHandler.sendToClient(new dev.rogu.forgetowns.network.SyncTownDataPacket(playerTown), player);
} else {
    ctx.getSource().sendFailure(dev.rogu.forgetowns.util.MessageHelper.styled(
        "You are not a member of a town!",
        dev.rogu.forgetowns.util.MessageHelper.MessageType.TOWN_ERROR
    ));
}
                        return 1;
                    })
                )
                // Admin commands (requires OP level 2)
                .then(Commands.literal("admin")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("give")
                        .then(Commands.literal("wand")
                            // /town admin give wand
                            .executes(ctx -> givePlotWand(ctx.getSource(), ctx.getSource().getPlayerOrException(), 1))
                            // /town admin give wand [count]
                            .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                .executes(ctx -> givePlotWand(ctx.getSource(), ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "count")))
                            )
                            // /town admin give wand [player]
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> givePlotWand(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), 1))
                                // /town admin give wand [player] [count]
                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                    .executes(ctx -> givePlotWand(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "count")))
                                )
                            )
                        )
                    )
                )
        );
    }

    public static Town findPlayerTown(ServerPlayer player) {
        for (Town town : TownDataStorage.getTowns().values()) {
            if (town.getResidents().contains(player.getUUID())) return town;
        }
        return null;
    }

    // Helper method for '/town give wand' (Owner/Assistant permissions)
    private static int givePlotWandTownMember(CommandSourceStack source, ServerPlayer sender, ServerPlayer targetPlayer, int count) {
        Town senderTown = findPlayerTown(sender);

        // Check if sender is in a town and is Owner/Assistant
        if (senderTown == null || (!senderTown.getOwner().equals(sender.getUUID()) && !senderTown.getAssistants().contains(sender.getUUID()))) {
            source.sendFailure(MessageHelper.styled("You must be an Owner or Assistant in a town to use this command.", MessageHelper.MessageType.TOWN_ERROR));
            return 0;
        }

        // Check if target is the sender or an assistant in the sender's town
        boolean isTargetSelf = sender.getUUID().equals(targetPlayer.getUUID());
        boolean isTargetAssistant = senderTown.getAssistants().contains(targetPlayer.getUUID());

        if (!isTargetSelf && !isTargetAssistant) {
             source.sendFailure(MessageHelper.styled("You can only give Plot Wands to yourself or Assistants in your town.", MessageHelper.MessageType.TOWN_ERROR));
             return 0;
        }

        // Use the admin give method's logic, but with town success/error messages
        if (count <= 0) {
            source.sendFailure(MessageHelper.styled("Count must be positive!", MessageHelper.MessageType.TOWN_ERROR));
            return 0;
        }

        ItemStack wandStack = new ItemStack(ForgeTowns.PLOT_WAND.get(), count);
        boolean added = targetPlayer.getInventory().add(wandStack);

        if (added) {
            source.sendSystemMessage(MessageHelper.styled(
                "Gave " + count + " Plot Wand(s) to " + targetPlayer.getName().getString(), 
                MessageHelper.MessageType.TOWN_SUCCESS // Use regular town success
            ));
            return 1;
        } else {
             source.sendFailure(MessageHelper.styled(
                targetPlayer.getName().getString() + "'s inventory is full!", 
                MessageHelper.MessageType.TOWN_ERROR // Use regular town error
            ));
             targetPlayer.drop(wandStack, false);
             source.sendSystemMessage(MessageHelper.styled(
                "Dropped " + count + " Plot Wand(s) near " + targetPlayer.getName().getString() + ".", 
                MessageHelper.MessageType.TOWN_INFO // Use town info for drop message
            ));
            return 1; 
        }
    }

    // Helper method for the give wand command
    private static int givePlotWand(CommandSourceStack source, ServerPlayer targetPlayer, int count) {
        if (count <= 0) {
            source.sendFailure(MessageHelper.styled("Count must be positive!", MessageHelper.MessageType.TOWN_ADMIN_ERROR));
            return 0;
        }

        ItemStack wandStack = new ItemStack(ForgeTowns.PLOT_WAND.get(), count);
        // The NBT tag is automatically added by PlotWandItem.getDefaultInstance()

        boolean added = targetPlayer.getInventory().add(wandStack);

        if (added) {
            source.sendSystemMessage(MessageHelper.styled(
                "Gave " + count + " Plot Wand(s) to " + targetPlayer.getName().getString(), 
                MessageHelper.MessageType.TOWN_ADMIN_SUCCESS
            ));
            return 1;
        } else {
             source.sendFailure(MessageHelper.styled(
                targetPlayer.getName().getString() + "'s inventory is full!", 
                MessageHelper.MessageType.TOWN_ADMIN_ERROR
            ));
            // Try dropping the item at the player's feet as a fallback
             targetPlayer.drop(wandStack, false);
             source.sendSystemMessage(MessageHelper.styled(
                "Dropped " + count + " Plot Wand(s) near " + targetPlayer.getName().getString() + ".", 
                MessageHelper.MessageType.TOWN_ADMIN_SUCCESS
            ));
            return 1; // Still counts as success in this case
        }
    }
}
