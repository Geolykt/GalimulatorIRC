package de.geolykt.galimirc.gui;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.starloader.api.gui.screen.ComponentSupplier;
import de.geolykt.starloader.api.gui.screen.Screen;
import de.geolykt.starloader.api.gui.screen.ScreenComponent;

public class IRCScreenComponentSupplier implements ComponentSupplier {

    @NotNull
    private final GalimulatorIRC extension;

    public IRCScreenComponentSupplier(@NotNull GalimulatorIRC extension) {
        this.extension = extension;
    }

    @Override
    public void supplyComponent(@NotNull Screen screen,
            @NotNull List<@NotNull ScreenComponent> existingComponents) {
        existingComponents.add(new IRCMainScreenComponent(screen, this.extension));
    }
}
