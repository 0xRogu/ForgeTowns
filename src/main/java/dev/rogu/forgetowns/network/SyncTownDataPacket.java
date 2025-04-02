package dev.rogu.forgetowns.network;

import dev.rogu.forgetowns.ForgeTowns; // Keep this import
import dev.rogu.forgetowns.data.Town;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

/**
 * Packet for syncing town data to the client.
 */
public class SyncTownDataPacket implements CustomPacketPayload {

    // Unique ID for this packet type
    // Need explicit type argument for the constructor
    // Use ResourceLocation.fromNamespaceAndPath factory method
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ForgeTowns.MOD_ID, "sync_town_data"); // Use MOD_ID
    public static final CustomPacketPayload.Type<SyncTownDataPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    // StreamCodec for encoding/decoding
    public static final StreamCodec<FriendlyByteBuf, SyncTownDataPacket> CODEC = StreamCodec.of(
        (buf, pkt) -> pkt.write(buf), // Encoder ((buffer, packet) -> void)
        buf -> new SyncTownDataPacket(buf)  // Decoder (buffer -> packet)
    );

    private final String townName;
    private final UUID owner;
    private final BlockPos homeBlock;
    private final List<UUID> residents;
    private final List<UUID> assistants;
    private final int emeraldBalance;

    public SyncTownDataPacket(Town town) {
        this.townName = town.getName();
        this.owner = town.getOwner();
        this.homeBlock = town.getHomeBlock();
        this.residents = new ArrayList<>(town.getResidents());
        this.assistants = new ArrayList<>(town.getAssistants());
        this.emeraldBalance = town.getEmeraldBalance();
    }

    public SyncTownDataPacket(FriendlyByteBuf buf) {
        this.townName = buf.readUtf();
        this.owner = buf.readUUID();
        this.homeBlock = buf.readBlockPos();
        int residentCount = buf.readInt();
        this.residents = new ArrayList<>(residentCount);
        for (int i = 0; i < residentCount; i++) {
            this.residents.add(buf.readUUID());
        }
        int assistantCount = buf.readInt();
        this.assistants = new ArrayList<>(assistantCount);
        for (int i = 0; i < assistantCount; i++) {
            this.assistants.add(buf.readUUID());
        }
        this.emeraldBalance = buf.readInt();
    }

    // Implement the write method from CustomPacketPayload
    // Removing @Override temporarily due to potential IDE/classpath issue
    // @Override 
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.townName);
        buf.writeUUID(this.owner);
        buf.writeBlockPos(this.homeBlock);
        buf.writeInt(this.residents.size());
        for (UUID resident : this.residents) {
            buf.writeUUID(resident);
        }
        buf.writeInt(this.assistants.size());
        for (UUID assistant : this.assistants) {
            buf.writeUUID(assistant);
        }
        buf.writeInt(this.emeraldBalance);
    }

    // Implement the type method from CustomPacketPayload
    @Override
    public CustomPacketPayload.Type<SyncTownDataPacket> type() {
        return TYPE;
    }

    public String getTownName() { return townName; }
    public UUID getOwner() { return owner; }
    public BlockPos getHomeBlock() { return homeBlock; }
    public List<UUID> getResidents() { return residents; }
    public List<UUID> getAssistants() { return assistants; }
    public int getEmeraldBalance() { return emeraldBalance; }

    public Town reconstructTown() {
        Town town = new Town(townName, owner, homeBlock);
        residents.forEach(town::addResident);
        assistants.forEach(town::addAssistant);
        town.depositEmeralds(emeraldBalance);
        return town;
    }
}
