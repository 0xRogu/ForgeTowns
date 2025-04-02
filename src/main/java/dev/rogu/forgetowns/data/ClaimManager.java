package dev.rogu.forgetowns.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import dev.rogu.forgetowns.commands.TownCommand;

import java.util.*;

public class ClaimManager {
    public static final Map<UUID, List<BlockPos>> plotSelections = new HashMap<>();

    public static boolean claimChunk(Town town, ChunkPos pos, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.hasChunk(pos.x, pos.z)) {
            player.sendSystemMessage(Component.literal("Chunk not loaded."));
            return false;
        }
        var chunk = level.getChunk(pos.x, pos.z);

        if (chunk.hasData(ModCapabilities.TOWN_CLAIM.get())) {
            ClaimCapability existingCap = chunk.getData(ModCapabilities.TOWN_CLAIM.get());
            if (existingCap.getTown() != null) {
                player.sendSystemMessage(Component.literal("This chunk is already claimed by " + existingCap.getTown().getName()));
                return false;
            }
        }

        if (town.getClaimedChunks().size() >= 10 || player.getInventory().countItem(Items.EMERALD) < 5) {
            if (town.getClaimedChunks().size() >= 10) player.sendSystemMessage(Component.literal("Town has reached the maximum number of claimed chunks."));
            else player.sendSystemMessage(Component.literal("You need 5 emeralds to claim a chunk."));
            return false;
        }

        player.getInventory().removeItem(new ItemStack(Items.EMERALD, 5));
        town.getClaimedChunks().add(pos);

        ClaimCapability capability = chunk.getData(ModCapabilities.TOWN_CLAIM.get());
        capability.setTown(town);
        chunk.setUnsaved(true);

        player.sendSystemMessage(Component.literal("Chunk claimed for " + town.getName()));
        return true;
    }

    public static void unclaimChunk(Town town, ChunkPos pos, ServerLevel level) {
        if (!level.hasChunk(pos.x, pos.z)) return;
        var chunk = level.getChunk(pos.x, pos.z);

        town.getClaimedChunks().remove(pos);

        chunk.setData(ModCapabilities.TOWN_CLAIM.get(), new ClaimCapability());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Entity entity = event.getPlayer();
        if (!(entity instanceof ServerPlayer player)) return;

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        Town town = getTownAt(serverLevel, chunkPos);
        if (town == null) return;

        if (!canBuild(player, town, event.getPos())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You can’t build here in " + town.getName() + "!"));
        }
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Entity entity = event.getEntity();
        ChunkPos chunkPos = new ChunkPos(event.getPos());

        if (!(entity instanceof ServerPlayer player)) {
            Town town = getTownAt(serverLevel, chunkPos);
            if (town != null && !isAtWarWithTown(town)) {
                event.setCanceled(true);
            }
            return;
        }

        Town town = getTownAt(serverLevel, chunkPos);
        if (town == null) return;

        if (!canBuild(player, town, event.getPos())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You can’t build here in " + town.getName() + "!"));
        }
    }

    public static void onMultiBlockPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        Entity entity = event.getEntity();

        if (!(entity instanceof ServerPlayer player)) {
            for (var state : event.getReplacedBlockSnapshots()) {
                ChunkPos pos = new ChunkPos(state.getPos());
                Town town = getTownAt(serverLevel, pos);
                if (town != null && !isAtWarWithTown(town)) {
                    event.setCanceled(true);
                    return;
                }
            }
            return;
        }

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        Town town = getTownAt(serverLevel, chunkPos);
        if (town == null) return;

        if (!canBuild(player, town, event.getPos())) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("You can’t build here in " + town.getName() + "!"));
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        event.getAffectedBlocks().removeIf(pos -> {
            ChunkPos chunkPos = new ChunkPos(pos);
            Town town = getTownAt(serverLevel, chunkPos);
            if (town != null && !isAtWarWithTown(town)) {
                Entity exploder = event.getExplosion().getDirectSourceEntity();
                if (exploder instanceof Player player) {
                    player.sendSystemMessage(Component.literal("Explosion blocked in " + town.getName() + "'s territory!"));
                }
                return true;
            }
            return false;
        });
    }

    public static void onInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer) || !(event.getLevel() instanceof ServerLevel serverLevel)) return;

        ChunkPos chunkPos = new ChunkPos(event.getPos());
        Town town = getTownAt(serverLevel, chunkPos);
        if (town == null) return;

        BlockEntity be = serverLevel.getBlockEntity(event.getPos());
        if (be instanceof SignBlockEntity sign) {
            SignText signText = sign.getText(true);
            if (signText != null && signText.getMessage(0, false).getString().equals("[Plot For Sale]")) {
                handlePlotPurchase(serverPlayer, town, event.getPos());
                event.setCanceled(true);
                return;
            }
        }

        if (!canInteract(serverPlayer, town, event.getPos())) {
            event.setCanceled(true);
            serverPlayer.sendSystemMessage(Component.literal("You can’t interact with this in " + town.getName() + "'s territory!"));
        }
    }

    public static void onTick(ServerLevel level) {
        // Removed old logic
    }

    public static Town getTownAt(Level level, ChunkPos pos) {
        if (!level.hasChunk(pos.x, pos.z)) return null;
        var chunk = level.getChunk(pos.x, pos.z);
        if (chunk.hasData(ModCapabilities.TOWN_CLAIM.get())) {
            ClaimCapability capability = chunk.getData(ModCapabilities.TOWN_CLAIM.get());
            return capability.getTown();
        }
        return null;
    }

    public static void selectPlotCorner(ServerPlayer player, BlockPos pos) {
        Town town = TownCommand.findPlayerTown(player);
        if (town == null || (!town.getOwner().equals(player.getUUID()) && !town.getAssistants().contains(player.getUUID()))) {
            player.sendSystemMessage(Component.literal("You must be an Owner or Assistant!"));
            return;
        }

        List<BlockPos> selections = plotSelections.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = new ChunkPos(pos);
        Town chunkTown = getTownAt(level, chunkPos);
        if (chunkTown == null || !chunkTown.equals(town)) {
            player.sendSystemMessage(Component.literal("Corner must be within a chunk claimed by your town!"));
            return;
        }

        selections.add(pos);
        player.sendSystemMessage(Component.literal("Corner " + selections.size() + " selected at " + pos.toShortString()));
        if (selections.size() == 4) {
            player.sendSystemMessage(Component.literal("All 4 corners selected. Use /town plot create <price> [type] to finalize."));
        }
    }

    public static void createPlot(ServerPlayer player, Town town, int price, Plot.PlotType type) {
        List<BlockPos> corners = plotSelections.remove(player.getUUID());
        if (corners == null || corners.size() != 4) {
            player.sendSystemMessage(Component.literal("Select all 4 corners with the Plot Wand first!"));
            return;
        }

        Plot plot = new Plot(corners, price, type);
        if (plot.overlapsSpawn(town.getHomeBlock())) {
            player.sendSystemMessage(Component.literal("Plot cannot overlap the town spawn!"));
            return;
        }

        BlockPos signPos = player.blockPosition().above();
        if (player.level().getBlockState(signPos).isAir()) {
            player.level().setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            plot.setSignPos(signPos);
            town.addPlot(plot);
            player.sendSystemMessage(Component.literal(type.getName() + " plot created with price " + price + " emeralds!"));
        } else {
            player.sendSystemMessage(Component.literal("Cannot place sign at " + signPos.toShortString() + "! Plot creation failed."));
        }
    }

    public static void inviteToPlot(ServerPlayer owner, Town town, ServerPlayer target) {
        Plot plot = town.getPlots().stream()
            .filter(p -> p.getOwner() != null && p.getOwner().equals(owner.getUUID()))
            .findFirst()
            .orElse(null);
        if (plot == null || plot.getType() != Plot.PlotType.PURCHASABLE) {
            owner.sendSystemMessage(Component.literal("You don’t own a purchasable plot to invite to!"));
            return;
        }

        if (!town.getResidents().contains(target.getUUID())) {
            owner.sendSystemMessage(Component.literal(target.getName().getString() + " must be a resident of " + town.getName() + "!"));
            return;
        }

        plot.invite(target.getUUID());
        owner.sendSystemMessage(Component.literal("Invited " + target.getName().getString() + " to your plot!"));
        target.sendSystemMessage(Component.literal("You’ve been invited to " + owner.getName().getString() + "'s plot!"));
    }

    private static void handlePlotPurchase(ServerPlayer player, Town town, BlockPos signPos) {
        Plot plot = town.getPlots().stream().filter(p -> p.getSignPos().equals(signPos)).findFirst().orElse(null);
        if (plot == null || plot.getOwner() != null || !plot.getType().isPurchasable()) {
            player.sendSystemMessage(Component.literal("This plot is not for sale!"));
            return;
        }

        if (!town.getResidents().contains(player.getUUID())) {
            player.sendSystemMessage(Component.literal("You must be a resident of " + town.getName() + " to buy a plot!"));
            return;
        }

        int price = plot.getPrice();
        if (player.getInventory().countItem(Items.EMERALD) < price) {
            player.sendSystemMessage(Component.literal("You need " + price + " emeralds to buy this plot!"));
            return;
        }

        player.getInventory().removeItem(new ItemStack(Items.EMERALD, price));
        plot.setOwner(player.getUUID());
        town.depositEmeralds(price);
        player.sendSystemMessage(Component.literal("You’ve purchased this plot for " + price + " emeralds!"));
        if (player.level().getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText purchasedText = new SignText()
                .setMessage(0, Component.literal("[Purchased Plot]"))
                .setMessage(1, Component.literal("Owner: " + player.getName().getString()))
                .setMessage(2, Component.empty()) // Clear other lines if needed
                .setMessage(3, Component.empty());
            sign.setText(purchasedText, true); // Set front text
            // We might need to mark the sign dirty? Let's assume setText does this for now.
            // serverLevel.sendBlockUpdated(signPos, sign.getBlockState(), sign.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private static boolean canBuild(ServerPlayer player, Town town, BlockPos pos) {
        boolean isOwner = town.getOwner().equals(player.getUUID());
        if (isOwner) return true;

        boolean isAssistant = town.getAssistants().contains(player.getUUID());
        Plot plot = town.getPlotAt(pos);
        if (plot != null) {
            if (plot.getType() == Plot.PlotType.COMMUNITY) {
                return isOwner || isAssistant;
            } else if (plot.getOwner() != null && plot.getOwner().equals(player.getUUID())) {
                return true;
            } else if (plot.getInvited().contains(player.getUUID())) {
                return true;
            }
        }

        boolean isResident = town.getResidents().contains(player.getUUID());
        boolean isAtWar = isAtWarWithTown(town);
        return isResident && isAtWar;
    }

    private static boolean canInteract(ServerPlayer player, Town town, BlockPos pos) {
        boolean isOwner = town.getOwner().equals(player.getUUID());
        if (isOwner) return true;

        boolean isAssistant = town.getAssistants().contains(player.getUUID());
        Plot plot = town.getPlotAt(pos);
        if (plot != null) {
            if (plot.getType() == Plot.PlotType.COMMUNITY) {
                return isOwner || isAssistant || town.getResidents().contains(player.getUUID());
            } else if (plot.getOwner() != null && plot.getOwner().equals(player.getUUID())) {
                return true;
            } else if (plot.getInvited().contains(player.getUUID())) {
                return true;
            }
        }

        boolean isResident = town.getResidents().contains(player.getUUID());
        boolean isAtWar = isAtWarWithTown(town);
        return isResident && isAtWar;
    }

    private static boolean isAtWarWithTown(Town town) {
        return TownDataStorage.getNations().values().stream()
            .anyMatch(n -> n.isAtWar() && n.getMemberTowns().contains(town));
    }
}
