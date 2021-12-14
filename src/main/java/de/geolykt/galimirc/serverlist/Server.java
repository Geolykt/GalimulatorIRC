package de.geolykt.galimirc.serverlist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;

public record Server(@NotNull String address, int port, @NotNull SecurityType security, @Nullable String password) {
}
