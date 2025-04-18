package dev.rogu.forgetowns.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.Nation;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.gui.NationMenu;
import dev.rogu.forgetowns.gui.NationMenuProvider;
import dev.rogu.forgetowns.util.MessageHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

/**
 * NationCommand class
 */
public class NationCommand {

    public static void register(
        CommandDispatcher<CommandSourceStack> dispatcher
    ) {
        dispatcher.register(
            Commands.literal("nation")
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
                            if (
                                TownDataStorage.getNations().containsKey(name)
                            ) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Nation already exists!",
                                            MessageHelper.MessageType.NATION_ERROR
                                        )
                                    );
                                return 0;
                            }
                            Town town = TownCommand.findPlayerTown(player);
                            if (
                                town == null ||
                                !town.getOwner().equals(player.getUUID())
                            ) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "You must be a town owner!",
                                            MessageHelper.MessageType.NATION_ERROR
                                        )
                                    );
                                return 0;
                            }
                            Nation nation = new Nation(name, town);
                            TownDataStorage.getNations().put(name, nation);
                            ctx
                                .getSource()
                                .sendSystemMessage(
                                    MessageHelper.styled(
                                        "Nation " + name + " created!",
                                        MessageHelper.MessageType.NATION_SUCCESS
                                    )
                                );
                            return 1;
                        })
                    )
                )
                .then(
                    Commands.literal("invite").then(
                        Commands.argument(
                            "town",
                            StringArgumentType.string()
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            String townName = StringArgumentType.getString(
                                ctx,
                                "town"
                            );
                            Town targetTown = TownDataStorage.getTowns()
                                .get(townName);
                            if (targetTown == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Town not found!",
                                            MessageHelper.MessageType.NATION_ERROR
                                        )
                                    );
                                return 0;
                            }
                            for (Nation nation : TownDataStorage.getNations()
                                .values()) {
                                if (
                                    nation
                                        .getLeaderTown()
                                        .getOwner()
                                        .equals(player.getUUID())
                                ) {
                                    nation.addTown(targetTown);
                                    ctx
                                        .getSource()
                                        .sendSystemMessage(
                                            MessageHelper.styled(
                                                "Invited " +
                                                townName +
                                                " to " +
                                                nation.getName(),
                                                MessageHelper.MessageType.NATION_SUCCESS
                                            )
                                        );
                                    return 1;
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "You must be a nation leader!",
                                        MessageHelper.MessageType.NATION_ERROR
                                    )
                                );
                            return 0;
                        })
                    )
                )
                .then(
                    Commands.literal("war").then(
                        Commands.argument(
                            "nation",
                            StringArgumentType.string()
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            String nationName = StringArgumentType.getString(
                                ctx,
                                "nation"
                            );
                            Nation targetNation = TownDataStorage.getNations()
                                .get(nationName);
                            if (targetNation == null) {
                                ctx
                                    .getSource()
                                    .sendFailure(
                                        MessageHelper.styled(
                                            "Nation not found!",
                                            MessageHelper.MessageType.NATION_ERROR
                                        )
                                    );
                                return 0;
                            }
                            for (Nation nation : TownDataStorage.getNations()
                                .values()) {
                                if (
                                    nation
                                        .getLeaderTown()
                                        .getOwner()
                                        .equals(player.getUUID())
                                ) {
                                    nation.setWar(true);
                                    targetNation.setWar(true);
                                    ctx
                                        .getSource()
                                        .sendSystemMessage(
                                            MessageHelper.styled(
                                                "Declared war on " +
                                                nationName,
                                                MessageHelper.MessageType.NATION_SUCCESS
                                            )
                                        );
                                    return 1;
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "You must be a nation leader!",
                                        MessageHelper.MessageType.NATION_ERROR
                                    )
                                );
                            return 0;
                        })
                    )
                )
                .then(
                    Commands.literal("government").then(
                        Commands.argument(
                            "type",
                            StringArgumentType.string()
                        ).executes(ctx -> {
                            ServerPlayer player = ctx
                                .getSource()
                                .getPlayerOrException();
                            String typeStr = StringArgumentType.getString(
                                ctx,
                                "type"
                            ).toUpperCase();
                            for (Nation nation : TownDataStorage.getNations()
                                .values()) {
                                if (
                                    nation
                                        .getLeaderTown()
                                        .getOwner()
                                        .equals(player.getUUID())
                                ) {
                                    try {
                                        GovernmentType type =
                                            GovernmentType.valueOf(typeStr);
                                        nation.setGovernmentType(type);
                                        ctx
                                            .getSource()
                                            .sendSystemMessage(
                                                MessageHelper.styled(
                                                    "Set " +
                                                    nation.getName() +
                                                    " government to " +
                                                    type.getName(),
                                                    MessageHelper.MessageType.NATION_SUCCESS
                                                )
                                            );
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        ctx
                                            .getSource()
                                            .sendFailure(
                                                MessageHelper.styled(
                                                    "Invalid government type! Use: democracy, monarchy, anarchy",
                                                    MessageHelper.MessageType.NATION_ERROR
                                                )
                                            );
                                        return 0;
                                    }
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    MessageHelper.styled(
                                        "You must be a nation leader!",
                                        MessageHelper.MessageType.NATION_ERROR
                                    )
                                );
                            return 0;
                        })
                    )
                )
                .then(
                    Commands.literal("menu").executes(ctx -> {
                        ServerPlayer player = ctx
                            .getSource()
                            .getPlayerOrException();
                        player.openMenu(
                            new NationMenuProvider(NationMenu.MenuMode.MAIN)
                        );
                        return 1;
                    })
                )
        );
    }
}
