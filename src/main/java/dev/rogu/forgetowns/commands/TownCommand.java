package dev.rogu.forgetowns.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogu.forgetowns.data.ClaimManager;
import dev.rogu.forgetowns.data.ClaimCapability;
import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.ModCapabilities;
import dev.rogu.forgetowns.data.Plot;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.gui.TownMenuProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
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
                                        Component.literal(
                                            "Town already exists!"
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
                                .sendSuccess(
                                    () ->
                                        Component.literal(
                                            "Town " + name + " created!"
                                        ),
                                    false
                                );
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
                                    Component.literal("You’re not in a town!")
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
                                    Component.literal(
                                        "Only Owners and Assistants can claim chunks!"
                                    )
                                );
                            return 0;
                        }
                        ChunkPos pos = new ChunkPos(player.blockPosition());
                        if (ClaimManager.claimChunk(town, pos, player)) {
                            ctx
                                .getSource()
                                .sendSuccess(
                                    () ->
                                        Component.literal(
                                            "Chunk claimed for " +
                                            town.getName() +
                                            "!"
                                        ),
                                    false
                                );
                            return 1;
                        } else {
                            ctx
                                .getSource()
                                .sendFailure(
                                    Component.literal(
                                        "Failed to claim chunk! (Already claimed, limit reached, or insufficient emeralds)"
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
                                    Component.literal("You’re not in a town!")
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
                                    Component.literal(
                                        "Only Owners and Assistants can unclaim chunks!"
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
                                .sendSuccess(
                                    () ->
                                        Component.literal(
                                            "Chunk unclaimed from " +
                                            town.getName() +
                                            "!"
                                        ),
                                    false
                                );
                            return 1;
                        } else {
                            ctx
                                .getSource()
                                .sendFailure(
                                    Component.literal(
                                        "This chunk isn’t claimed by " +
                                        town.getName() +
                                        "!"
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
                                        Component.literal(
                                            "You’re not in a town!"
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
                                        Component.literal(
                                            "Only Owners and Assistants can invite!"
                                        )
                                    );
                                return 0;
                            }
                            town.addResident(target.getUUID());
                            ctx
                                .getSource()
                                .sendSuccess(
                                    () ->
                                        Component.literal(
                                            target.getName().getString() +
                                            " invited to " +
                                            town.getName() +
                                            "!"
                                        ),
                                    false
                                );
                            target.sendSystemMessage(
                                Component.literal(
                                    "You’ve been invited to " +
                                    town.getName() +
                                    "!"
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
                                        Component.literal(
                                            "You’re not in a town!"
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Only the Owner can kick!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                target.getName().getString() +
                                                " kicked from " +
                                                town.getName() +
                                                "!"
                                            ),
                                        false
                                    );
                                target.sendSystemMessage(
                                    Component.literal(
                                        "You’ve been kicked from " +
                                        town.getName() +
                                        "!"
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            target.getName().getString() +
                                            " isn’t a resident!"
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
                                        Component.literal(
                                            "You’re not in a town!"
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Only the Owner can promote!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                target.getName().getString() +
                                                " promoted to Assistant in " +
                                                town.getName() +
                                                "!"
                                            ),
                                        false
                                    );
                                target.sendSystemMessage(
                                    Component.literal(
                                        "You’ve been promoted to Assistant in " +
                                        town.getName() +
                                        "!"
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            target.getName().getString() +
                                            " isn’t a resident!"
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
                                        Component.literal(
                                            "You’re not in a town!"
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(owner.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Only the Owner can demote!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                target.getName().getString() +
                                                " demoted to resident in " +
                                                town.getName() +
                                                "!"
                                            ),
                                        false
                                    );
                                target.sendSystemMessage(
                                    Component.literal(
                                        "You’ve been demoted to resident in " +
                                        town.getName() +
                                        "!"
                                    )
                                );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            target.getName().getString() +
                                            " isn’t an Assistant!"
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
                                        Component.literal(
                                            "You’re not in a town!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                amount +
                                                " emeralds deposited into " +
                                                town.getName() +
                                                "!"
                                            ),
                                        false
                                    );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "You don’t have enough emeralds!"
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
                                        Component.literal(
                                            "You’re not in a town!"
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(player.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Only the Owner can withdraw!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                amount +
                                                " emeralds withdrawn from " +
                                                town.getName() +
                                                "!"
                                            ),
                                        false
                                    );
                                return 1;
                            } else {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Not enough emeralds in " +
                                            town.getName() +
                                            "'s treasury!"
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
                                        Component.literal(
                                            "You’re not in a town!"
                                        )
                                    );
                                return 0;
                            }
                            if (!town.getOwner().equals(player.getUUID())) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Only the Owner can set the government type!"
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
                                    .sendSuccess(
                                        () ->
                                            Component.literal(
                                                town.getName() +
                                                " government set to " +
                                                type.name() +
                                                "!"
                                            ),
                                        false
                                    );
                                return 1;
                            } catch (IllegalArgumentException e) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        Component.literal(
                                            "Invalid government type! Use: ANARCHY, DEMOCRACY, MONARCHY"
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
                                                        Component.literal(
                                                            "You must be an Owner or Assistant!"
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
                                                        Component.literal(
                                                            "Invalid plot type! Use: purchasable, community"
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
                                                    Component.literal(
                                                        "You must be an Owner or Assistant!"
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
                                                Component.literal(
                                                    "You’re not in a town!"
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
                // Open the town menu
                .then(
                    Commands.literal("menu").executes(ctx -> {
                        ServerPlayer player = ctx
                            .getSource()
                            .getPlayerOrException();
                        player.openMenu(
                            new TownMenuProvider(TownMenuProvider.MenuMode.MAIN)
                        );
                        return 1;
                    })
                )
        );
    }

    public static Town findPlayerTown(ServerPlayer player) {
        for (Town town : TownDataStorage.getTowns().values()) {
            if (town.getResidents().contains(player.getUUID())) return town;
        }
        return null;
    }
}
