package dev.rogu.forgetowns.data;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.attachment.AttachmentType;
import java.util.function.Supplier;

import dev.rogu.forgetowns.ForgeTowns;

public class ModCapabilities {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ForgeTowns.MOD_ID);

    public static final Supplier<AttachmentType<ClaimCapability>> TOWN_CLAIM =
        ATTACHMENT_TYPES.register("town_claim", () -> 
            AttachmentType.serializable(ClaimCapability::new).build());

    /*
    public static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.register(ClaimCapability.class);
    }
    */
}
