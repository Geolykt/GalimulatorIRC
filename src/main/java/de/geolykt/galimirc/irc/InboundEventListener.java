package de.geolykt.galimirc.irc;

import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.kitteh.irc.client.library.element.User;
import org.kitteh.irc.client.library.event.channel.ChannelJoinEvent;
import org.kitteh.irc.client.library.event.channel.ChannelMessageEvent;
import org.kitteh.irc.client.library.event.user.UserQuitEvent;
import org.slf4j.Logger;

import net.engio.mbassy.listener.Handler;
import net.engio.mbassy.listener.Invoke;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.galimirc.serverlist.Channel;
import de.geolykt.galimirc.serverlist.ChannelChat;
import de.geolykt.galimirc.serverlist.Network;
import de.geolykt.starloader.api.NullUtils;

public class InboundEventListener {

    @NotNull
    private final GalimulatorIRC extension;

    @NotNull
    private final Logger logger;

    @NotNull
    private final Network net;

    public InboundEventListener(@NotNull Network network, @NotNull GalimulatorIRC extension) {
        this.extension = extension;
        this.net = network;
        this.logger = extension.getLogger();
    }

    @SuppressWarnings("null")
    @NotNull
    private Channel getChannel(@NotNull org.kitteh.irc.client.library.element.Channel chan) {
        Optional<Channel> channel = net.getChannelByName(NullUtils.requireNotNull(chan.getLowerCaseMessagingName()));
        if (channel.isPresent()) {
            return channel.get();
        } else {
            // We were not able to find the channel, so we create it
            String channelName = NullUtils.requireNotNull(chan.getLowerCaseMessagingName());
            logger.info("Unable to look up channel {}. Creating it.", channelName);
            Channel galimIRCChan = new Channel(channelName, net, new ChannelChat(extension, net, channelName));
            net.addChannel(galimIRCChan);
            return galimIRCChan;
        }
    }

    @Handler(delivery = Invoke.Asynchronously)
    public void onJoin(ChannelJoinEvent event) {
        User user = NullUtils.requireNotNull(event.getActor());
        TemporalAccessor timestamp = NullUtils.requireNotNull(LocalTime.now(Clock.systemDefaultZone()));
        getChannel(event.getChannel()).chat().registerJoin(user, timestamp);
    }

    @Handler(delivery = Invoke.Asynchronously)
    public void onMessage(ChannelMessageEvent event) {
        String message = NullUtils.requireNotNull(event.getMessage());
        String userNick = NullUtils.requireNotNull(event.getActor().getNick());
        TemporalAccessor timestamp = NullUtils.requireNotNull(LocalTime.now(Clock.systemDefaultZone()));
        getChannel(event.getChannel()).chat().addMessage(message, userNick, timestamp);
    }

    @Handler(delivery = Invoke.Asynchronously)
    public void onPart(UserQuitEvent event) {
        User user = NullUtils.requireNotNull(event.getActor());
        TemporalAccessor timestamp = NullUtils.requireNotNull(LocalTime.now(Clock.systemDefaultZone()));
        String message = ColorConverter.toGDX(NullUtils.requireNotNull(event.getMessage()));
        if (!event.getAffectedChannel().isPresent()) {
            event.getActor().getChannels().forEach(channelName -> {
                Optional<Channel> channel = net.getChannelByName(NullUtils.requireNotNull(channelName.toLowerCase(Locale.ROOT)));
                if (channel.isPresent()) {
                    channel.get().chat().registerPart(user, timestamp, message);
                }
            });
        } else {
            getChannel(event.getAffectedChannel().get()).chat().registerPart(user, timestamp, message);
        }
    }
}
