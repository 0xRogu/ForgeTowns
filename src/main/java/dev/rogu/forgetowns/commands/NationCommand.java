package dev.rogu.forgetowns.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.rogu.forgetowns.data.GovernmentType;
import dev.rogu.forgetowns.data.Nation;
import dev.rogu.forgetowns.data.Town;
import dev.rogu.forgetowns.data.TownDataStorage;
import dev.rogu.forgetowns.gui.NationMenu;
import dev.rogu.forgetowns.gui.NationMenuProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                                        Component.literal(
                                            "Nation already exists!"
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
                                        Component.literal(
                                            "You must be a town owner!"
                                        )
                                    );
                                return 0;
                            }
                            Nation nation = new Nation(name, town);
                            TownDataStorage.getNations().put(name, nation);
                            ctx
                                .getSource()
                                .sendSuccess(
                                    () ->
                                        Component.literal(
                                            "Nation " + name + " created!"
                                        ),
                                    false
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
                                        Component.literal("Town not found!")
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
                                        .sendSuccess(
                                            () ->
                                                Component.literal(
                                                    "Invited " +
                                                    townName +
                                                    " to " +
                                                    nation.getName()
                                                ),
                                            false
                                        );
                                    return 1;
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    Component.literal(
                                        "You must be a nation leader!"
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
                                        Component.literal("Nation not found!")
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
                                        .sendSuccess(
                                            () ->
                                                Component.literal(
                                                    "Declared war on " +
                                                    nationName
                                                ),
                                            false
                                        );
                                    return 1;
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    Component.literal(
                                        "You must be a nation leader!"
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
                                            .sendSuccess(
                                                () ->
                                                    Component.literal(
                                                        "Set " +
                                                        nation.getName() +
                                                        " government to " +
                                                        type.getName()
                                                    ),
                                                false
                                            );
                                        return 1;
                                    } catch (IllegalArgumentException e) {
                                        ctx
                                            .getSource()
                                            .sendFailure(
                                                Component.literal(
                                                    "Invalid government type! Use: democracy, monarchy, anarchy"
                                                )
                                            );
                                        return 0;
                                    }
                                }
                            }
                            ctx
                                .getSource()
                                .sendFailure(
                                    Component.literal(
                                        "You must be a nation leader!"
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
