package de.geolykt.galimirc.serverlist;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public final class Channel {
    @NotNull
    private final ChannelChat chat;
    @NotNull
    private final Network chatNet;
    @NotNull
    private final String name;

    public Channel(@NotNull String name, @NotNull Network chatNet, @NotNull ChannelChat chat) {
        this.name = name;
        this.chatNet = chatNet;
        this.chat = chat;
    }

    @NotNull
    public final ChannelChat chat() {
        return this.chat;
    }

    @NotNull
    public final Network chatNet() {
        return this.chatNet;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Channel) {
            return ((Channel) obj).name.equals(this.name)
                    && ((Channel) obj).chatNet.equals(this.chatNet)
                    && ((Channel) obj).chat.equals(this.chat);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.chatNet, this.chat);
    }

    @NotNull
    public final String name() {
        return this.name;
    }
}
