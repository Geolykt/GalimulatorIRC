package de.geolykt.galimirc.serverlist;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;

public class Server {
    @NotNull
    private final String address;
    @Nullable
    private final String password;
    private final int port;
    @NotNull
    private final SecurityType security;

    public Server(@NotNull String address, int port, @NotNull SecurityType security, @Nullable String password) {
        this.address = address;
        this.port = port;
        this.security = security;
        this.password = password;
    }

    @NotNull
    public String address() {
        return this.address;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Server) {
            Server other = (Server) obj;
            return this.address.equals(other.address)
                    && this.port == other.port
                    && this.security == other.security
                    && Objects.equals(this.password, other.password);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.address, this.port, this.security, this.password);
    }

    @Nullable
    public String password() {
        return this.password;
    }

    public int port() {
        return this.port;
    }

    @NotNull
    public SecurityType security() {
        return this.security;
    }
}
