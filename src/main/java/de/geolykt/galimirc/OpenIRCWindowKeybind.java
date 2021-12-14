package de.geolykt.galimirc;

import org.jetbrains.annotations.NotNull;

import de.geolykt.starloader.api.gui.Dynbind;

public class OpenIRCWindowKeybind implements Dynbind {

    @NotNull
    private GalimulatorIRC extension;

    public OpenIRCWindowKeybind(@NotNull GalimulatorIRC extension) {
        this.extension = extension;
    }

    @Override
    public @NotNull String getDescription() {
        return "Opens the IRC Client window";
    }

    @Override
    public @NotNull String getKeyDescription() {
        return "Shift + I";
    }

    @Override
    public boolean isValidChar(char character) {
        return character == 'I';
    }

    @Override
    public boolean isValidKeycode(int key) {
        return false;
    }

    @Override
    public void performAction() {
        extension.guiManager.open();
    }
}
