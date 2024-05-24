package de.geolykt.galimirc.serverlist;

import java.time.temporal.TemporalAccessor;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class ChatMessage {
    @NotNull
    private final String message;
    private boolean metamessage;
    @NotNull
    private final String sender;
    @NotNull
    private final TemporalAccessor timestamp;

    public ChatMessage(@NotNull String message, @NotNull String sender, @NotNull TemporalAccessor timestamp, boolean metamessage) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
        this.metamessage = metamessage;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ChatMessage) {
            ChatMessage other = (ChatMessage) obj;
            return this.metamessage == other.metamessage
                    && this.message.equals(other.message)
                    && this.sender.equals(other.sender)
                    && this.timestamp.equals(other.timestamp);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.message, this.sender, this.timestamp, this.metamessage);
    }

    @NotNull
    public String message() {
        return this.message;
    }

    public boolean metamessage() {
        return this.metamessage;
    }

    @NotNull
    public String sender() {
        return this.sender;
    }

    @NotNull
    public TemporalAccessor timestamp() {
        return this.timestamp;
    }
}
