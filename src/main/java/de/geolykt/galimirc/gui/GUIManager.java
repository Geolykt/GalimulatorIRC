package de.geolykt.galimirc.gui;

import org.jetbrains.annotations.NotNull;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.starloader.api.gui.Drawing;
import de.geolykt.starloader.api.gui.screen.Screen;
import de.geolykt.starloader.api.gui.screen.ScreenBuilder;
import de.geolykt.starloader.api.gui.text.TextColor;

public class GUIManager {

    @NotNull
    private final GalimulatorIRC extension;

    public GUIManager(@NotNull GalimulatorIRC extension) {
        this.extension = extension;
    }

    public void open() {
        ScreenBuilder builder = ScreenBuilder.getBuilder().addComponentSupplier(new IRCScreenComponentSupplier(extension));
        builder.setHeaderColor(TextColor.BLUE);
        builder.setWidth(1600);
        builder.setHeaderEnabled(true);
        builder.setTitle("Galimulator IRC");
        Screen screen = builder.build();
        Drawing.showScreen(screen);
    }
}
