package de.geolykt.galimirc;

import org.jetbrains.annotations.NotNull;

import de.geolykt.starloader.api.NamespacedKey;
import de.geolykt.starloader.api.gui.Keybind;

public class OpenIRCWindowKeybind implements Keybind {

    @NotNull
    private GalimulatorIRC extension;

    public OpenIRCWindowKeybind(@NotNull GalimulatorIRC extension) {
        this.extension = extension;
    }

    @Override
    @NotNull
    public String getDescription() {
        return "Open the IRC Client window";
    }

    @Override
    public void executeAction() {
        this.extension.guiManager.open();
    }

    @Override
    @NotNull
    public NamespacedKey getID() {
        return new NamespacedKey(this.extension, "keybind_open_irc");
    }
}
