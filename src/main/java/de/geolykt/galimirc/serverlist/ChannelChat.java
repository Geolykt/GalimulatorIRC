package de.geolykt.galimirc.serverlist;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.User;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.galimirc.irc.ColorConverter;
import de.geolykt.starloader.api.NullUtils;
import de.geolykt.starloader.api.gui.Drawing;

public class ChannelChat {

    @NotNull
    private final Deque<ChatMessage> buffer = new ConcurrentLinkedDeque<>();
    @NotNull
    private final String channelName;
    @NotNull
    private final GalimulatorIRC extension;
    @NotNull
    private final List<ChatMessage> log = new ArrayList<>();
    private boolean naggedDesktopNotifNoWork = false;
    @NotNull
    private final Network net;

    @NotNull
    private ChannelStatus status = ChannelStatus.READ;

    public ChannelChat(@NotNull GalimulatorIRC extension, @NotNull Network net, @NotNull String channelName) {
        this.extension = extension;
        this.net = net;
        this.channelName = channelName;
    }

    public void addMessage(@NotNull String msg, @NotNull String sender, @NotNull TemporalAccessor timestamp) {
        if (status != ChannelStatus.SELECTED) {
            if (net.isMonitoring(channelName) || msg.toLowerCase(Locale.ROOT).contains(extension.nick.toLowerCase(Locale.ROOT))) {
                status = ChannelStatus.UNREAD_PINGS;
                if (extension.desktopNotifications) {
                    sendDesktopNotification("Message from " + sender + " " + channelName + "@" + net.name + ":", ColorConverter.strip(msg), extension.criticalNotification);
                }
            } else if (status != ChannelStatus.UNREAD_PINGS) {
                status = ChannelStatus.UNREAD_MESSAGES;
            }
        }
        buffer.add(new ChatMessage(ColorConverter.toGDX(msg), sender, timestamp, false));
    }

    @SuppressWarnings("null")
    @NotNull
    public List<ChatMessage> getLog() {
        if (!buffer.isEmpty()) {
            // Superior thread-safety code
            for (ChatMessage message = buffer.poll(); message != null; message = buffer.poll()) {
                log.add(message);
            }
        }
        return Collections.unmodifiableList(log);
    }

    @NotNull
    public ChannelStatus getStatus() {
        return this.status;
    }

    public void registerJoin(@NotNull User user, @NotNull TemporalAccessor timestamp) {
        if (!user.getNick().equals(extension.nick) && !(status == ChannelStatus.UNREAD_MESSAGES || status == ChannelStatus.UNREAD_PINGS || status == ChannelStatus.SELECTED)) {
            status = ChannelStatus.UNREAD_JOINS;
        }
        buffer.add(new ChatMessage("joined.", NullUtils.requireNotNull(user.getNick()), timestamp, true));
    }

    public void registerPart(@NotNull User user, @NotNull TemporalAccessor timestamp, @NotNull String message) {
        if (status != ChannelStatus.UNREAD_MESSAGES && status != ChannelStatus.UNREAD_PINGS && status != ChannelStatus.SELECTED) {
            status = ChannelStatus.UNREAD_JOINS;
        }
        buffer.add(new ChatMessage("parted (" + message + ")", NullUtils.requireNotNull(user.getNick()), timestamp, true));
    }

    private void sendDesktopNotification(String summary, String body, boolean critical) {
        extension.getLogger().info("{}: {}", summary, body);
        try {
            Runtime.getRuntime().exec(new String[] {"notify-send", "-a", "StarloaderLauncher", "-t", "5000", "-c", "im.received", "-u", critical ? "critical" : "low", summary, body});
        } catch (Exception e1) {
            if (!naggedDesktopNotifNoWork) {
                e1.printStackTrace();
                naggedDesktopNotifNoWork = true;
                Drawing.toast("notify-send does not appear to be installed on your system. Consider installing it if you are on linux.");
            }
        }
    }

    public void setStatus(@NotNull ChannelStatus status) {
        this.status = status;
    }
}
