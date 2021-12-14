package de.geolykt.galimirc.serverlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.geolykt.starloader.api.NullUtils;

public class ServerList {

    @NotNull
    private final List<Network> networks = new ArrayList<>();

    @NotNull
    private final Map<String, Network> networksByName = new HashMap<>();

    @NotNull
    public List<Network> getNetworks() {
        return NullUtils.requireNotNull(Collections.unmodifiableList(networks));
    }

    public void addNetwork(@NotNull Network network) {
        if (networksByName.containsKey(network.name)) {
            throw new IllegalStateException("There can only be one network with a given name.");
        }
        this.networksByName.put(network.name, network);
        this.networks.add(network);
    }

    @Nullable
    public Network getNetworkByName(@NotNull String name) {
        return networksByName.get(name);
    }
}
