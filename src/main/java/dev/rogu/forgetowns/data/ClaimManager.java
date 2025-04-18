package dev.rogu.forgetowns.data;

import dev.rogu.forgetowns.util.MessageHelper;
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
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import dev.rogu.forgetowns.commands.TownCommand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimManager {
    /**
     * Removes up to 'count' of the given item from the player's inventory.
     * Returns the number of items removed.
     */
    public static int removeItems(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
        int removed = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                int toRemove = Math.min(count - removed, stack.getCount());
                stack.shrink(toRemove);
                removed += toRemove;
                if (removed >= count) break;
            }
        }
        return removed;
    }
    /**
     * Clears all static claim selection data. Call on world unload/server stop to prevent memory leaks.
     */
    public static void clearStaticData() {
        plotSelections.clear();
    }
    public static final Map<UUID, List<BlockPos>> plotSelections = new ConcurrentHashMap<>();

    public static ClaimResult claimChunk(Town town, ChunkPos pos, ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!level.hasChunk(pos.x, pos.z)) {
            return new ClaimResult(false, "Chunk not loaded.", MessageHelper.MessageType.TOWN_WARNING);
        }
        var chunk = level.getChunk(pos.x, pos.z);

        if (chunk.hasData(ModCapabilities.TOWN_CLAIM.get())) {
            ClaimCapability existingCap = chunk.getData(ModCapabilities.TOWN_CLAIM.get());
            if (existingCap.getTown() != null) {
                return new ClaimResult(false, "This chunk is already claimed by " + existingCap.getTown().getName(), MessageHelper.MessageType.TOWN_WARNING);
            }
        }

        int claimCost = dev.rogu.forgetowns.config.ForgeTownsConfig.chunkClaimCost;
        if (town.getClaimedChunks().size() >= 10) {
            return new ClaimResult(false, "Town has reached the maximum number of claimed chunks.", MessageHelper.MessageType.TOWN_ERROR);
        }
        if (player.getInventory().countItem(Items.EMERALD) < claimCost) {
            return new ClaimResult(false, "You need " + claimCost + " emeralds to claim a chunk.", MessageHelper.MessageType.TOWN_WARNING);
        }
        int removed = removeItems(player, Items.EMERALD, claimCost);
        if (removed < claimCost) {
            return new ClaimResult(false, "You need " + claimCost + " emeralds to claim a chunk.", MessageHelper.MessageType.TOWN_WARNING);
        }
        town.getClaimedChunks().add(pos);

        ClaimCapability capability = chunk.getData(ModCapabilities.TOWN_CLAIM.get());
        capability.setTown(town);
        chunk.setUnsaved(true);

        return new ClaimResult(true, "Chunk claimed successfully!", MessageHelper.MessageType.TOWN_SUCCESS);
    }

    public static void unclaimChunk(Town town, ChunkPos pos, ServerLevel level) {
        if (!level.hasChunk(pos.x, pos.z)) return;
        var chunk = level.getChunk(pos.x, pos.z);

        town.getClaimedChunks().remove(pos);

        chunk.setData(ModCapabilities.TOWN_CLAIM.get(), new ClaimCapability());
    }

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            // Only run on the server side!
            return;
        }
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
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            // Only run on the server side!
            return;
        }
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
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            // Only run on the server side!
            return;
        }
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
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            // Only run on the server side!
            return;
        }

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

    public static ClaimResult selectPlotCorner(ServerPlayer player, BlockPos pos, ItemStack usedItem) {
        CustomData customData = usedItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.copyTag().getBoolean("ForgeTownsPlotWand")) {
            return new ClaimResult(false, "You must use the Plot Wand to select corners!", MessageHelper.MessageType.TOWN_ERROR);
        }

        Town town = TownCommand.findPlayerTown(player);
        if (town == null || (!town.getOwner().equals(player.getUUID()) && !town.getAssistants().contains(player.getUUID()))) {
            return new ClaimResult(false, "You must be an Owner or Assistant of a town to select plot corners!", MessageHelper.MessageType.TOWN_ERROR);
        }

        List<BlockPos> selections = plotSelections.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        ServerLevel level = player.serverLevel();
        ChunkPos chunkPos = new ChunkPos(pos);
        Town chunkTown = getTownAt(level, chunkPos);
        if (chunkTown == null || !chunkTown.equals(town)) {
            return new ClaimResult(false, "Corner must be within a chunk claimed by your town!", MessageHelper.MessageType.TOWN_ERROR);
        }

        selections.add(pos);
        int size = selections.size();
        if (size == 4) {
            return new ClaimResult(true, "All 4 corners selected. Use /town plot create <price> [type] to finalize.", MessageHelper.MessageType.TOWN_SUCCESS);
        } else {
            return new ClaimResult(true, "Corner " + size + " selected at " + pos.toShortString(), MessageHelper.MessageType.TOWN_INFO);
        }
    }

    public static ClaimResult createPlot(ServerPlayer player, Town town, int price, Plot.PlotType type) {
        List<BlockPos> corners = plotSelections.remove(player.getUUID());
        if (corners == null || corners.size() != 4) {
            return new ClaimResult(false, "Select all 4 corners with the Plot Wand first!", MessageHelper.MessageType.TOWN_ERROR);
        }

        Plot plot = new Plot(corners, price, type);
        if (plot.overlapsSpawn(town.getHomeBlock())) {
            return new ClaimResult(false, "Plot cannot overlap the town spawn!", MessageHelper.MessageType.TOWN_ERROR);
        }

        BlockPos signPos = player.blockPosition().above();
        if (player.level().getBlockState(signPos).isAir()) {
            player.level().setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);
            plot.setSignPos(signPos);
            town.addPlot(plot);
            return new ClaimResult(true, type.getName() + " plot created with price " + price + " emeralds!", MessageHelper.MessageType.TOWN_SUCCESS);
        } else {
            return new ClaimResult(false, "Cannot place sign at " + signPos.toShortString() + "! Plot creation failed.", MessageHelper.MessageType.TOWN_ERROR);
        }
    }

    public static ClaimResult inviteToPlot(ServerPlayer owner, Town town, ServerPlayer target) {
        Plot plot = town.getPlots().stream()
            .filter(p -> p.getOwner() != null && p.getOwner().equals(owner.getUUID()))
            .findFirst()
            .orElse(null);
        if (plot == null || plot.getType() != Plot.PlotType.PURCHASABLE) {
            return new ClaimResult(false, "You don’t own a purchasable plot to invite to!", MessageHelper.MessageType.TOWN_ERROR);
        }

        if (!town.getResidents().contains(target.getUUID())) {
            return new ClaimResult(false, target.getName().getString() + " must be a resident of " + town.getName() + "!", MessageHelper.MessageType.TOWN_ERROR);
        }

        plot.invite(target.getUUID());
        // The owner and target should be notified by the command handler
        return new ClaimResult(true, "Invited " + target.getName().getString() + " to your plot!", MessageHelper.MessageType.TOWN_SUCCESS);
    }

    public static ClaimResult handlePlotPurchase(ServerPlayer player, Town town, BlockPos signPos) {
        Plot plot = town.getPlots().stream().filter(p -> p.getSignPos().equals(signPos)).findFirst().orElse(null);
        if (plot == null || plot.getOwner() != null || !plot.getType().isPurchasable()) {
            return new ClaimResult(false, "This plot is not for sale!", MessageHelper.MessageType.TOWN_WARNING);
        }

        if (!town.getResidents().contains(player.getUUID())) {
            return new ClaimResult(false, "You must be a resident of " + town.getName() + " to buy a plot!", MessageHelper.MessageType.TOWN_WARNING);
        }

        int price = plot.getPrice();
        if (player.getInventory().countItem(Items.EMERALD) < price) {
            return new ClaimResult(false, "You need " + price + " emeralds to buy this plot!", MessageHelper.MessageType.TOWN_WARNING);
        }

        int removed = removeItems(player, Items.EMERALD, price);
        if (removed < price) {
            return new ClaimResult(false, "You need " + price + " emeralds to buy this plot!", MessageHelper.MessageType.TOWN_WARNING);
        }
        plot.setOwner(player.getUUID());
        town.depositEmeralds(price);
        if (player.level().getBlockEntity(signPos) instanceof SignBlockEntity sign) {
            SignText frontText = sign.getText(true);
            frontText.setMessage(0, MessageHelper.styled("[Purchased Plot]", MessageHelper.MessageType.TOWN_SUCCESS));
            frontText.setMessage(1, MessageHelper.styled("Owner: " + player.getName().getString(), MessageHelper.MessageType.TOWN_INFO));
            frontText.setMessage(2, Component.empty());
            frontText.setMessage(3, Component.empty());
            sign.setChanged();
            player.level().sendBlockUpdated(signPos, sign.getBlockState(), sign.getBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        return new ClaimResult(true, "You’ve purchased this plot for " + price + " emeralds!", MessageHelper.MessageType.TOWN_SUCCESS);
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
