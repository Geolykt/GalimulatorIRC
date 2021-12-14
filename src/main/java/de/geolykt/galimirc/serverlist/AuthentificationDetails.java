package de.geolykt.galimirc.serverlist;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public record AuthentificationDetails(boolean sasl, @NotNull String accountName, @NotNull String password,
        @NotNull Optional<String> nickservService) {
}
