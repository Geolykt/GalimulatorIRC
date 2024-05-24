package de.geolykt.galimirc.gui;

import org.jetbrains.annotations.NotNull;
import org.stianloader.micromixin.transform.internal.util.Objects;

import com.badlogic.gdx.graphics.Color;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.starloader.api.gui.Drawing;
import de.geolykt.starloader.api.gui.screen.Screen;
import de.geolykt.starloader.api.gui.screen.ScreenBuilder;

public class GUIManager {

    @NotNull
    private final GalimulatorIRC extension;

    public GUIManager(@NotNull GalimulatorIRC extension) {
        this.extension = extension;
    }

    public void open() {
        ScreenBuilder builder = ScreenBuilder.getBuilder().addComponentSupplier(new IRCScreenComponentSupplier(this.extension));
        builder.withHeaderColor(Objects.requireNonNull(Color.BLUE));
        builder.withWidth(1600);
        builder.withHeaderEnabled(true);
        builder.withTitle("Galimulator IRC");
        Screen screen = builder.build();
        Drawing.showScreen(screen);
    }
}
