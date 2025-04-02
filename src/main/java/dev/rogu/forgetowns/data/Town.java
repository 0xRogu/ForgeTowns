package dev.rogu.forgetowns.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

public class Town {

    private String name;
    private UUID owner;
    private BlockPos homeBlock;
    private List<UUID> residents = new ArrayList<>();
    private List<UUID> assistants = new ArrayList<>();
    private List<ChunkPos> claimedChunks = new ArrayList<>();
    private List<Plot> plots = new ArrayList<>();
    private int emeraldBalance;
    private GovernmentType governmentType = GovernmentType.ANARCHY;
    private transient Level level;

    public Town(String name, UUID owner, BlockPos homeBlock) {
        this.name = name;
        this.owner = owner;
        this.homeBlock = homeBlock;
        this.residents.add(owner);
        this.emeraldBalance = 0;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public BlockPos getHomeBlock() {
        return homeBlock;
    }

    public List<UUID> getResidents() {
        return residents;
    }

    public List<UUID> getAssistants() {
        return assistants;
    }

    public List<ChunkPos> getClaimedChunks() {
        return claimedChunks;
    }

    public List<Plot> getPlots() {
        return plots;
    }

    public int getEmeraldBalance() {
        return emeraldBalance;
    }

    public GovernmentType getGovernmentType() {
        return governmentType;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void setGovernmentType(GovernmentType type) {
        this.governmentType = type;
    }

    public void addResident(UUID player) {
        residents.add(player);
    }

    public void removeResident(UUID player) {
        residents.remove(player);
        assistants.remove(player);
    }

    public void addAssistant(UUID player) {
        assistants.add(player);
    }

    public void depositEmeralds(int amount) {
        emeraldBalance += amount;
    }

    public boolean withdrawEmeralds(int amount) {
        if (emeraldBalance >= amount) {
            emeraldBalance -= amount;
            return true;
        }
        return false;
    }

    public void addPlot(Plot plot) {
        plots.add(plot);
        if (level != null) plot.updateSign(level, this);
    }

    public Plot getPlotAt(BlockPos pos) {
        return plots
            .stream()
            .filter(plot -> plot.contains(pos))
            .findFirst()
            .orElse(null);
    }
}
