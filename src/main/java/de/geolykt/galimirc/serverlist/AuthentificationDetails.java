package de.geolykt.galimirc.serverlist;

import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public class AuthentificationDetails {
    @NotNull
    private final String accountName;
    @NotNull
    private final Optional<String> nickservService;
    @NotNull
    private final String password;
    private final boolean sasl;

    public AuthentificationDetails(boolean sasl, @NotNull String accountName, @NotNull String password, @NotNull Optional<String> nickservService) {
        this.sasl = sasl;
        this.accountName = accountName;
        this.password = password;
        this.nickservService = nickservService;
    }

    @NotNull
    public final String accountName() {
        return this.accountName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AuthentificationDetails) {
            AuthentificationDetails other = (AuthentificationDetails) obj;
            return other.sasl == this.sasl
                    && other.accountName.equals(this.accountName)
                    && other.password.equals(this.password)
                    && other.nickservService.equals(this.nickservService);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sasl, this.accountName, this.password, this.nickservService);
    }

    @NotNull
    public final Optional<String> nickservService() {
        return this.nickservService;
    }

    @NotNull
    public final String password() {
        return this.password;
    }

    public final boolean sasl() {
        return this.sasl;
    }
}
