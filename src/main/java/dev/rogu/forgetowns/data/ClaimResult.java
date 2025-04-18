package dev.rogu.forgetowns.data;

import dev.rogu.forgetowns.util.MessageHelper;

public class ClaimResult {
    public final boolean success;
    public final String message;
    public final MessageHelper.MessageType type;

    public ClaimResult(boolean success, String message, MessageHelper.MessageType type) {
        this.success = success;
        this.message = message;
        this.type = type;
    }
}
