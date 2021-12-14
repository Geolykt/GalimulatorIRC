package de.geolykt.galimirc;

import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.Client;

import de.geolykt.galimirc.gui.GUIManager;
import de.geolykt.starloader.api.Galimulator;
import de.geolykt.starloader.api.event.EventHandler;
import de.geolykt.starloader.api.event.Listener;
import de.geolykt.starloader.api.event.lifecycle.ApplicationStartedEvent;
import de.geolykt.starloader.api.event.lifecycle.ApplicationStopEvent;
import de.geolykt.starloader.api.resource.DataFolderProvider;

public class GalimIRCEventListener implements Listener {

    @NotNull
    private GalimulatorIRC extension;

    public GalimIRCEventListener(@NotNull GalimulatorIRC galimulatorIRC) {
        this.extension = galimulatorIRC;
    }

    @EventHandler
    public void onApplicationStartFinish(ApplicationStartedEvent e) {
        extension.guiManager = new GUIManager(extension);
        Galimulator.registerKeybind(new OpenIRCWindowKeybind(extension));

        extension.loadConfig(DataFolderProvider.getProvider().provideAsPath().resolve("galimulatorirc.json"));
        extension.startClients();
    }

    @EventHandler
    public void onShutdown(ApplicationStopEvent e) {
        extension.getLogger().info("Shutting down IRC Connections...");
        for (Client client : extension.ircClients) {
            client.shutdown("Game closed.");
        }
        extension.getLogger().info("All IRC Connections shut down.");
    }
}
