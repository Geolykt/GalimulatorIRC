package de.geolykt.galimirc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.Client.Builder.Server.SecurityType;
import org.kitteh.irc.client.library.feature.auth.NickServ;
import org.kitteh.irc.client.library.feature.auth.SaslPlain;

import de.geolykt.galimirc.gui.GUIManager;
import de.geolykt.galimirc.irc.InboundEventListener;
import de.geolykt.galimirc.serverlist.AuthentificationDetails;
import de.geolykt.galimirc.serverlist.Channel;
import de.geolykt.galimirc.serverlist.ChannelChat;
import de.geolykt.galimirc.serverlist.Network;
import de.geolykt.galimirc.serverlist.Server;
import de.geolykt.galimirc.serverlist.ServerList;
import de.geolykt.starloader.api.NullUtils;
import de.geolykt.starloader.api.event.EventManager;
import de.geolykt.starloader.api.gui.Drawing;
import de.geolykt.starloader.mod.Extension;

public class GalimulatorIRC extends Extension {

    public GUIManager guiManager;

    @NotNull
    public final ServerList serverlist = new ServerList();

    @NotNull
    public String name = "";

    @NotNull
    public String nick = "";

    @NotNull
    public String realname = "";

    @NotNull
    protected List<Client> ircClients = new ArrayList<>();

    @NotNull
    protected Map<Network, Client> ircNetworkClients = new HashMap<>();

    @NotNull
    public String errorMessage = "Welcome to GalimulatorIRC.";

    public boolean desktopNotifications = true;

    public boolean criticalNotification = false;

    void startClients() {
        for (Network net : this.serverlist.getNetworks()) {
            if (net.getServers().isEmpty()) {
                this.getLogger().warn("Network {} does not have a server declared!", net.name);
                continue;
            }
            try {
                this.setupClient(net);
            } catch (Exception e) {
                this.getLogger().error("Unable to start client instance for network " + net.name, e);
            }
        }
    }

    private void setupClient(Network network) throws Exception {
        if (this.nick.isEmpty()) {
            return;
        }
        Server server = network.getServers().get(ThreadLocalRandom.current().nextInt(network.getServers().size()));
        Client.Builder ircClientBuilder = Client.builder();
        ircClientBuilder.name("GalimulatorIRC-bot-" + network.name).nick(this.name).realName(this.realname).user(this.name);
        ircClientBuilder.server().port(server.port(), server.security()).password(server.password())
            .host(server.address());
        Client ircClient = ircClientBuilder.buildAndConnect();
        if (network.auth.isPresent()) {
            AuthentificationDetails details = network.auth.get();
            if (details.sasl()) {
                ircClient.getAuthManager().addProtocol(new SaslPlain(ircClient, details.accountName(), details.password()));
            } else {
                ircClient.getAuthManager().addProtocol(NickServ.builder(ircClient)
                        .account(details.accountName()).password(details.password()).serviceName(details.nickservService().orElse("NickServ"))
                        .build());
            }
        }
        for (Channel channel : network.getChannels()) {
            ircClient.addChannel(channel.name());
        }
        ircClient.getEventManager().registerEventListener(new InboundEventListener(network, this));
        this.ircClients.add(ircClient);
        this.ircNetworkClients.put(network, ircClient);
        this.getLogger().info("Client set up for " + network.name);
    }

    @Override
    public void postInitialize() {
        EventManager.registerListener(new GalimIRCEventListener(this));;
    }

    @SuppressWarnings("null")
    public void loadConfig(Path configFile) {
        if (Files.notExists(configFile)) {
            this.getLogger().warn("Config file does not exist!");
            this.errorMessage = "Unable to find the galimulatorIRC config file. It is located in: " + configFile.toAbsolutePath().toString();
            return;
        }
        final JSONObject jsonData;
        try {
            jsonData = new JSONObject(new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            this.errorMessage = "Unable to read config file:\n" + sw.toString();
            this.getLogger().error("Unable to read config file: ", e);
            return;
        }

        this.nick = jsonData.getString("nick");
        this.name = jsonData.optString("name", this.nick);
        this.realname = jsonData.optString("realname", "Galimulator IRC");
        this.desktopNotifications = jsonData.optBoolean("desktop-notify", true);
        this.criticalNotification = jsonData.optBoolean("critical-notify", false);

        for (Object o : jsonData.getJSONArray("networks")) {
            if (o instanceof JSONObject) {
                JSONObject network = (JSONObject) o;
                Optional<AuthentificationDetails> auth;
                if (network.has("auth")) {
                    JSONObject jsonAuth = network.getJSONObject("auth");
                    boolean sasl = jsonAuth.optBoolean("sasl", false);
                    Optional<String> nickservService = Optional.of(jsonAuth.optString("nickservSerivce", "NickServ"));
                    String password = jsonAuth.getString("password");
                    String accountName = jsonAuth.getString("account");
                    auth = Optional.of(new AuthentificationDetails(sasl, accountName, password, nickservService));
                } else {
                    auth = Optional.empty();
                }
                Network net = new Network(network.getString("name"), auth);
                if (network.has("servers")) {
                    for (Object o2 : network.getJSONArray("servers")) {
                        if (o2 instanceof JSONObject) {
                            JSONObject server = (JSONObject) o2;
                            boolean isSecure = server.optBoolean("secure", true);
                            int port = server.getInt("port");
                            String address = server.getString("address");
                            String password = server.optString("password");
                            net.addServer(new Server(address, port, isSecure ? SecurityType.SECURE : SecurityType.INSECURE, password));
                        }
                    }
                }
                if (network.has("monitored")) {
                    network.getJSONArray("monitored").forEach(o2 -> net.monitor(o2.toString()));
                }
                if (network.has("channels")) {
                    for (Object o2 : network.getJSONArray("channels")) {
                        if (o2 instanceof JSONObject) {
                            JSONObject channel = (JSONObject) o2;
                            String channelName = NullUtils.requireNotNull(channel.getString("name"));
                            net.addChannel(new Channel(channelName, net, new ChannelChat(this, net, channelName)));
                        }
                    }
                }
                this.serverlist.addNetwork(net);
            }
        }
    }

    public void saveConfig(Path configFile) {
        final JSONObject jsonData = new JSONObject();

        jsonData.put("nick", this.nick);
        jsonData.put("name", this.name);
        jsonData.put("realname", this.realname);
        jsonData.put("desktop-notify", this.desktopNotifications);
        jsonData.put("critical-notify", this.criticalNotification);

        JSONArray networks = new JSONArray();
        jsonData.put("networks", networks);
        for (Network net : this.serverlist.getNetworks()) {
            JSONObject netJson = new JSONObject();
            networks.put(netJson);
            netJson.put("name", net.name);
            if (net.auth.isPresent()) {
                AuthentificationDetails details = net.auth.get();
                JSONObject auth = new JSONObject();
                netJson.put("auth", auth);
                auth.put("sasl", details.sasl());
                auth.put("account", details.accountName());
                auth.put("password", details.password());
                auth.put("service", details.nickservService().orElse(null));
            }
            JSONArray servers = new JSONArray();
            netJson.put("servers", servers);
            for (Server serv : net.getServers()) {
                JSONObject server = new JSONObject();
                servers.put(server);
                server.put("secure", serv.security() == SecurityType.SECURE);
                server.put("port", serv.port());
                server.put("address", serv.address());
                server.put("password", serv.password());
            }
            netJson.put("monitored", new JSONArray(net.getMonitoredChannels()));
            JSONArray channels = new JSONArray();
            netJson.put("channels", channels);
            for (Channel chan : net.getChannels()) {
                channels.put(new JSONObject().put("name", chan.name()));
            }
        }

        File file = configFile.toFile();
        if (!file.exists()) {
            this.getLogger().warn("Config file does not exist!");
            this.errorMessage = "Unable to find the galimulatorIRC config file. It is located in: " + configFile.toAbsolutePath().toString();
            return;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonData.toString(4).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            this.errorMessage = "Unable to read config file:\n" + sw.toString();
            this.getLogger().error("Unable to read config file: ", e);
            return;
        }
    }

    private void handleMonitorCommand(@NotNull String command, boolean removing) {
        String[] tokens = command.split(" ");
        if (tokens.length != 3) {
            if (removing) {
                Drawing.toast("The /demonitor command must have two arguments.\n Syntax: /demonitor <network> <channel>");
            } else {
                Drawing.toast("The /monitor command must have two arguments.\n Syntax: /monitor <network> <channel>");
            }
            return;
        }
        Network network = this.serverlist.getNetworkByName(NullUtils.requireNotNull(tokens[1]));
        if (network == null) {
            Drawing.toast("Unable to find network [GRAY]" + tokens[1] + "[]. Are you sure that you are connected to it?");
            return;
        }
        if (removing) {
            network.removeMonitor(NullUtils.requireNotNull(tokens[2]));
        } else {
            network.monitor(NullUtils.requireNotNull(tokens[2]));
        }
        Drawing.toast("Change to the monitor list was applied. Don't forget to save the config file to apply changes permamently!");
    }

    private void handleCommand(@NotNull String command, @NotNull Optional<Channel> current) {
        // Note: command must have the preceding slash
        if (command.startsWith("/join")) {
            String[] tokens = command.split(" ");
            if (tokens.length != 3) {
                Drawing.toast("The /join command must have two arguments.\n Syntax: /join <network> <channel>");
                return;
            }
            Network network = serverlist.getNetworkByName(NullUtils.requireNotNull(tokens[1]));
            if (network == null) {
                Drawing.toast("Unable to find network [GRAY]" + tokens[1] + "[]. Are you sure that you are connected to it?");
                return;
            }
            if (network.getChannelByName(NullUtils.requireNotNull(tokens[2])).isPresent()) {
                Drawing.toast("I'm already connected to that channel!");
                return;
            }
            String channelName = NullUtils.requireNotNull(tokens[2]);
            network.addChannel(new Channel(channelName, network, new ChannelChat(this, network, channelName)));
            Client cl = this.ircNetworkClients.get(network);
            cl.addChannel(channelName);
        } else if (command.startsWith("/monitor")) {
            handleMonitorCommand(command, false);
        } else if (command.startsWith("/demonitor")) {
            handleMonitorCommand(command, true);
        } else if (command.startsWith("/connect")) {
            String[] tokens = command.split(" ");
            if (tokens.length != 4 && tokens.length != 5 && tokens.length != 6) {
                Drawing.toast("The /connect command must have 3 or 4 or 5 arguments.\n Syntax: /connect <name> <address> <port> [ssl] [password]");
                return;
            }
            String name = tokens[1];
            if (this.serverlist.getNetworkByName(NullUtils.requireNotNull(name)) != null) {
                Drawing.toast("A network by that name already exists!");
                return;
            }
            String address = tokens[2];
            int port = Integer.valueOf(tokens[3]);
            boolean ssl = port == 6697;
            if (tokens.length > 4) {
                if (tokens[4].toLowerCase(Locale.ROOT).equals("yes") || tokens[4].equalsIgnoreCase("y")) { // DAU
                    ssl = true;
                } else {
                    ssl = Boolean.valueOf(tokens[4]);
                }
            }
            Server server = new Server(NullUtils.requireNotNull(address), port,
                    ssl ? SecurityType.SECURE : SecurityType.INSECURE,
                    tokens.length > 5 ? tokens[5] : null);
            this.requestAuthentificationDetails(auth -> {
                Network network = new Network(NullUtils.requireNotNull(name), NullUtils.requireNotNull(auth));
                this.serverlist.addNetwork(network);
                network.addServer(server);
                try {
                    setupClient(network);
                    Drawing.toast("Network setup successfull. Don't forget to save the configuration for it to persist!");
                } catch (Exception e) {
                    getLogger().error("Unable to start client instance for network " + network.name, e);
                    Drawing.toast("Unable to setup client. Look in the logs for further information");
                }
            });
        } else if (!current.isPresent()) {
            Drawing.toast("Cannot send command as there is no selected channel to send the command in. (click on a channel on the left)");
        } else {
            Client client = this.ircNetworkClients.get(current.get().chatNet());
            client.sendMessage(current.get().name(), command);
        }
    }

    private void requestAuthentificationDetails(Consumer<Optional<AuthentificationDetails>> authDetailsOut) {

        Drawing.textInputBuilder("Setup Authentifcation (sasl)", "", "Should SASL be used? [yes/no]").setInitialText("yes").addHook(sasl -> {
            if (sasl == null) {
                authDetailsOut.accept(Optional.empty());
                return;
            }
            Drawing.textInputBuilder("Setup Authentification (account name)", "", "Set Account name").addHook(account -> {
                if (account == null) {
                    authDetailsOut.accept(Optional.empty());
                    return;
                }
                Drawing.textInputBuilder("Setup Authentification (password)", "", "Set password (Warning: password is shown as plaintext!)").addHook(password -> {
                    if (password == null) {
                        authDetailsOut.accept(Optional.empty());
                        return;
                    }
                    AuthentificationDetails authDetails = new AuthentificationDetails(sasl.equalsIgnoreCase("yes") || Boolean.valueOf(sasl), account, password, Optional.of("NickServ"));
                    authDetailsOut.accept(Optional.of(authDetails));
                }).build();
            }).build();
        }).build();
    }

    public void handleMessage(@NotNull String message, @NotNull Optional<Channel> current) {
        if (message.isEmpty()) {
            return;
        }
        if (message.startsWith("/")) {
            this.handleCommand(message, current);
        } else if (!current.isPresent()) {
            Drawing.toast("Cannot send message as there is no selected channel to send the message in. (click on a channel on the left)");
        } else {
            Client client = this.ircNetworkClients.get(current.get().chatNet());
            client.sendMessage(current.get().name(), message);
            // echo back the message to the client
            current.get().chat().addMessage(message, NullUtils.requireNotNull(client.getNick()), NullUtils.requireNotNull(LocalTime.now(Clock.systemDefaultZone())));
        }
    }
}
