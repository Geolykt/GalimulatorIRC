package de.geolykt.galimirc.serverlist;

import org.jetbrains.annotations.NotNull;

public record Channel(@NotNull String name, @NotNull Network chatNet, @NotNull ChannelChat chat) {}
