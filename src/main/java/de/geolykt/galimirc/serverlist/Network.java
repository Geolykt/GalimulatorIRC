package de.geolykt.galimirc.serverlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class Network {

    @NotNull
    public final Optional<AuthentificationDetails> auth;
    @NotNull
    private final Map<String, Channel> channelNames = new HashMap<>();
    @NotNull
    private final List<Channel> channels = new ArrayList<>();
    @NotNull
    private final Set<String> monitored = new HashSet<>();
    @NotNull
    public final String name;
    @NotNull
    private final List<Server> servers = new ArrayList<>();

    public Network(@NotNull String name, @NotNull Optional<AuthentificationDetails> authDetails) {
        this.name = name;
        this.auth = authDetails;
    }

    public void addChannel(@NotNull Channel channel) {
        if (channel.chatNet() != this) {
            throw new IllegalStateException("This channel is assigned to another network.");
        }
        this.channels.add(channel);
        this.channelNames.put(channel.name().toLowerCase(Locale.ROOT), channel);
    }

    public void addServer(@NotNull Server server) {
        this.servers.add(server);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Network) {
            return this.name.equals(((Network) obj).name); // We only really need the network be unique by name. Furthermore, ServerList enforces this
        }
        return false;
    }

    @NotNull
    public Optional<Channel> getChannelByName(@NotNull String name) {
        return Optional.ofNullable(this.channelNames.get(name.toLowerCase(Locale.ROOT)));
    }

    @SuppressWarnings("null")
    @NotNull
    public List<Channel> getChannels() {
        return Collections.unmodifiableList(this.channels);
    }

    @NotNull
    public Iterable<String> getMonitoredChannels() {
        return this.monitored;
    }

    @SuppressWarnings("null")
    @NotNull
    public List<Server> getServers() {
        return Collections.unmodifiableList(this.servers);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode(); // We only really need the network be unique by name
    }

    public boolean isMonitoring(@NotNull String channel) {
        return monitored.contains(channel);
    }

    public void monitor(@NotNull String channel) {
        this.monitored.add(channel);
    }

    public void removeMonitor(@NotNull String channel) {
        this.monitored.remove(channel);
    }
}
