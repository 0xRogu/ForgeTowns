package dev.rogu.forgetowns.data;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.minecraft.core.HolderLookup;

/**
 * Capability for storing town claim data.
 * Implements INBTSerializable for the Attachment system.
 */
public class ClaimCapability implements INBTSerializable<CompoundTag> {

    // We'll implement the capability registration in a future update
    
    private Town town;
    private String townName;

    public ClaimCapability() {
        this.town = null;
        this.townName = null;
    }

    public void setTown(Town town) {
        this.town = town;
    }

    public Town getTown() {
        return town;
    }

    public String getTownName() {
        return townName;
    }

    public void setTownName(String townName) {
        this.townName = townName;
    }

    /**
     * Serializes the capability data to NBT
     * @return The NBT data
     */
    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (town != null) {
            tag.putString("town", town.getName());
        }
        return tag;
    }

    /**
     * Deserializes the capability data from NBT
     * @param nbt The NBT data
     */
    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        if (nbt.contains("town")) {
            String townName = nbt.getString("town");
            this.town = TownDataStorage.getTowns().get(townName);
        }
    }
}
