package dev.rogu.forgetowns.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.network.chat.Component;
import com.mojang.authlib.GameProfile;

public class Plot {

    private UUID owner; // null if for sale or community plot
    private final List<BlockPos> corners; // Exactly 4 corners
    private int price;
    private BlockPos signPos;
    private List<UUID> invited = new ArrayList<>(); // Players invited by owner
    private PlotType type = PlotType.PURCHASABLE; // Default type

    public enum PlotType {
        PURCHASABLE("Purchasable", true),
        COMMUNITY("Community", false);

        private final String name;
        private final boolean purchasable;

        PlotType(String name, boolean purchasable) {
            this.name = name;
            this.purchasable = purchasable;
        }

        public String getName() {
            return name;
        }

        public boolean isPurchasable() {
            return purchasable;
        }
    }

    public Plot(List<BlockPos> corners, int price, PlotType type) {
        if (corners.size() != 4) throw new IllegalArgumentException(
            "Plot must have exactly 4 corners"
        );
        this.owner = null;
        this.corners = corners;
        this.price = price;
        this.type = type;
    }

    public UUID getOwner() {
        return owner;
    }

    public List<BlockPos> getCorners() {
        return corners;
    }

    public int getPrice() {
        return price;
    }

    public BlockPos getSignPos() {
        return signPos;
    }

    public List<UUID> getInvited() {
        return invited;
    }

    public PlotType getType() {
        return type;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setSignPos(BlockPos signPos) {
        this.signPos = signPos;
    }

    public void invite(UUID player) {
        invited.add(player);
    }

    public void uninvite(UUID player) {
        invited.remove(player);
    }

    public boolean contains(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        boolean inside = false;

        for (int i = 0, j = corners.size() - 1; i < corners.size(); j = i++) {
            int xi = corners.get(i).getX();
            int yi = corners.get(i).getY();
            int xj = corners.get(j).getX();
            int yj = corners.get(j).getY();

            if (
                (yi > y) != (yj > y) &&
                x < ((xj - xi) * (y - yi)) / (yj - yi) + xi
            ) {
                inside = !inside;
            }
        }
        return inside && isWithinHeight(pos);
    }

    private boolean isWithinHeight(BlockPos pos) {
        int minY = corners.stream().mapToInt(BlockPos::getY).min().orElse(0);
        int maxY = corners.stream().mapToInt(BlockPos::getY).max().orElse(0);
        return pos.getY() >= minY && pos.getY() <= maxY;
    }

    public boolean overlapsSpawn(BlockPos homeBlock) {
        return contains(homeBlock);
    }

    public void updateSign(Level level, Town town) {
        if (signPos == null) return;
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity sign)) return;

        SignText frontText;
        if (owner == null && type == PlotType.PURCHASABLE) {
            frontText = new SignText()
                .setMessage(0, Component.literal("[Plot For Sale]"))
                .setMessage(1, Component.literal(price + " Emeralds"))
                .setMessage(2, Component.literal("Right-click to buy"))
                .setMessage(3, Component.empty());
        } else {
            String ownerName = owner != null ? level.getServer().getProfileCache().get(owner).map(GameProfile::getName).orElse("Unknown") : "";
            frontText = new SignText()
                .setMessage(0, Component.literal("[" + type.getName() + " Plot]")) // Use type name
                .setMessage(1, Component.literal(owner != null ? "Owner: " + ownerName : "Unowned")) // Show owner or unowned
                .setMessage(2, Component.empty())
                .setMessage(3, Component.empty()); // Clear line 3 for non-purchasable/owned
        }
        sign.setText(frontText, true); // Set front text
    }

    public void removeSign(Level level) {
        if (signPos != null) { // Check signPos exists
            level.removeBlock(signPos, false); // Just remove the block
        }
    }
}
