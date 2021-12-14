package de.geolykt.galimirc.gui;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;

import de.geolykt.galimirc.GalimulatorIRC;
import de.geolykt.galimirc.serverlist.Channel;
import de.geolykt.galimirc.serverlist.ChannelStatus;
import de.geolykt.galimirc.serverlist.ChatMessage;
import de.geolykt.galimirc.serverlist.Network;
import de.geolykt.starloader.api.NullUtils;
import de.geolykt.starloader.api.gui.Drawing;
import de.geolykt.starloader.api.gui.DrawingImpl;
import de.geolykt.starloader.api.gui.screen.LineWrappingInfo;
import de.geolykt.starloader.api.gui.screen.ReactiveComponent;
import de.geolykt.starloader.api.gui.screen.Screen;
import de.geolykt.starloader.api.gui.screen.ScreenComponent;
import de.geolykt.starloader.api.resource.DataFolderProvider;

public class IRCMainScreenComponent implements ScreenComponent, ReactiveComponent {

    private static record ButtonPositioningMetaEntry(@NotNull Runnable action, float x, float y, float width, float height) { }

    private static record ChannelListPositioningMetaEntry(@NotNull Channel ch, float x, float y, float width, float height) { }

    private static final Color DARKER_YELLOW = new Color(0.5F, 0.5F, 0.0F, 1F);

    @NotNull
    private static DateTimeFormatter timestampFormatter;

    static {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendLiteral("[GRAY][").appendValue(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral("] []");
        timestampFormatter = NullUtils.requireNotNull(builder.toFormatter());
    }

    @NotNull
    private final List<ButtonPositioningMetaEntry> buttonPositioningMeta = new ArrayList<>();

    @NotNull
    private final List<ChannelListPositioningMetaEntry> channelListPositioningMeta = new ArrayList<>();

    private final float channelListSize = 0.2F;

    @NotNull
    private final BitmapFont chatFont;

    private boolean configureMode = false;

    @NotNull
    private final GalimulatorIRC extension;

    @NotNull
    private final BitmapFont font;

    @NotNull
    private final Screen parent;

    @NotNull
    private final GlyphLayout primaryGlyphLayout = new GlyphLayout(); // As SLAPI does not expose galimulator's internal glyph layout. Not that we needed it anyways

    private int scroll;

    @NotNull
    private final GlyphLayout secondaryGlyphLayout = new GlyphLayout(); // We need multiple glyph layout for reasons

    @NotNull
    private static Optional<Channel> selectedChannel = NullUtils.emptyOptional();

    private final float senderNickSize = 0.2F;

    @NotNull
    private final List<Runnable> unappliedConfigChanges = new ArrayList<>();

    @NotNull
    private String unappliedNick;

    @NotNull
    private String unappliedRealname;

    @NotNull
    private String unappliedUsername;

    private boolean unappliedDesktopNotify;

    private boolean unappliedCriticalNotify;

    public IRCMainScreenComponent(@NotNull Screen parentScreen, @NotNull GalimulatorIRC extension) {
        this.parent = parentScreen;
        this.extension = extension;
        BitmapFont tempFont = Drawing.getFontBitmap("MONOTYPE_SMALL");
        if (tempFont == null) {
            extension.getLogger().warn("IRCMainScreenComponent: Unable to resolve requested font! Using a completely random font instead.");
            String fontName = NullUtils.requireNotNull(Drawing.getFonts().toArray(new String[0])[0]);
            font = NullUtils.requireNotNull(Drawing.getFontBitmap(fontName));
        } else {
            font = tempFont;
        }
        tempFont = Drawing.getFontBitmap("FRIENDLY");
        if (tempFont == null) {
            extension.getLogger().warn("IRCMainScreenComponent: Unable to resolve requested chat font! Using the main font instead.");
            chatFont = font;
        } else {
            chatFont = tempFont;
        }
        this.unappliedNick = extension.nick;
        this.unappliedUsername = extension.name;
        this.unappliedRealname = extension.realname;
        this.unappliedDesktopNotify = extension.desktopNotifications;
        this.unappliedCriticalNotify = extension.criticalNotification;
    }

    private void drawAdditionalButtons(float x, float y, @NotNull Camera camera, @NotNull SpriteBatch batch) {
        y += 20;
        final Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));

        String text = configureMode ? "Save configuration" : "Configure";
        screenCoords.y += renderButton(screenCoords.x, screenCoords.y, getWidth() * channelListSize, text, batch, () -> {
            if (configureMode) {
                configureMode = false;
                for (Runnable action : unappliedConfigChanges) {
                    action.run();
                }
                extension.saveConfig(DataFolderProvider.getProvider().provideAsPath().resolve("galimulatorirc.json"));
            } else {
                configureMode = true;
            }
        });
        screenCoords.y += renderButton(screenCoords.x, screenCoords.y, getWidth() * channelListSize, "Send message", batch, () -> {
            Drawing.textInputBuilder("Send IRC Message", "", "Set the message you want to send")
                .addHook(message -> {
                    if (message == null) {
                        return; // User aborted send operation
                    }
                    extension.handleMessage(message, selectedChannel);
                }).build();
        });
        // Not a button but it still falls under the same area in a graphical sense, so I put it in this method
        text = "Scroll [LIME]" + scroll + "[] Memory [RED]" + (Runtime.getRuntime().totalMemory() / 1_000_000L) + "[] MB.";
        primaryGlyphLayout.setText(font, text, Color.WHITE, getWidth() * channelListSize - 10, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenCoords.x + 7.5F, screenCoords.y);
    }

    private void drawChannels(final float x, final float y, @NotNull Camera camera, SpriteBatch batch) {
        channelListPositioningMeta.clear();
        final Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));
        float screenY = screenCoords.y + getHeight();
        final float width = getWidth() * channelListSize;

        for (Network network : extension.serverlist.getNetworks()) {
            screenY -= primaryGlyphLayout.height;
            primaryGlyphLayout.setText(font, network.name, Color.WHITE, width - 20, Align.topLeft, true);
            screenY -= primaryGlyphLayout.height;
            font.draw(batch, primaryGlyphLayout, screenCoords.x + 10, screenY);

            for (Channel channel : network.getChannels()) {
                ChannelStatus status = channel.chat().getStatus();
                screenY -= primaryGlyphLayout.height;
                primaryGlyphLayout.setText(font, channel.name(), status.getColor(), width - 30, Align.topLeft, true);
                screenY -= primaryGlyphLayout.height;
                font.draw(batch, primaryGlyphLayout, screenCoords.x + 20, screenY);
                channelListPositioningMeta.add(new ChannelListPositioningMetaEntry(channel, screenCoords.x , screenY, width, primaryGlyphLayout.height * 2.0F));
            }
        }
    }

    private void drawChat(final float x, final float y, @NotNull Camera camera, SpriteBatch batch) {

        float screenY;
        final float screenXSenders;
        final float screenXMessages;
        final float widthSenders;
        final float widthTotal;
        final Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));
        widthTotal = getWidth() * (1 - channelListSize);
        widthSenders = widthTotal * senderNickSize;
        screenXSenders = screenCoords.x + getWidth() * channelListSize;
        screenXMessages = screenXSenders + widthSenders;
        screenY = screenCoords.y + getHeight();

        if (selectedChannel.isEmpty()) {
            primaryGlyphLayout.setText(chatFont, extension.errorMessage, Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= primaryGlyphLayout.height * 0.6;
            chatFont.draw(batch, primaryGlyphLayout, screenXSenders + 5, screenY);
            return;
        }

        Channel channel = selectedChannel.get();

        List<ChatMessage> messages = channel.chat().getLog();
        if (messages.isEmpty()) {
            primaryGlyphLayout.setText(chatFont, "No messages in this channel yet.", Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= primaryGlyphLayout.height * 0.6;
            chatFont.draw(batch, primaryGlyphLayout, screenXSenders + 5, screenY);
            return; // Else an IOOBE would happen in the next lines.
        }

        // We want to iterate backwards
        int startPosition = messages.size() + this.scroll;
        if (startPosition <= 0) {
            primaryGlyphLayout.setText(chatFont, "No older messages.", Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= primaryGlyphLayout.height * 0.6;
            chatFont.draw(batch, primaryGlyphLayout, screenXSenders + 5, screenY);
            this.scroll = -messages.size();
            return; // Else an IOOBE would happen at the next line.
        }
        ListIterator<ChatMessage> logIterator = messages.listIterator(startPosition);
        boolean firstMessage = true;
        while (logIterator.previousIndex() != -1) {
            ChatMessage message = logIterator.previous();
            String leftHandText = timestampFormatter.format(message.timestamp());
            String rightHandText = message.message();
            if (message.metamessage()) {
                leftHandText += "[RED]**[]";
                Color senderColor = getSenderColor(NullUtils.requireNotNull(message.sender()));
                rightHandText = NullUtils.format("[#%s] %s [] %s", senderColor.toString(), message.sender(), message.message());
            } else {
                leftHandText += message.sender();
            }
            primaryGlyphLayout.setText(chatFont, leftHandText, getSenderColor(NullUtils.requireNotNull(message.sender())), widthSenders - 15, Align.topLeft, false);
            float leftHandSize = Math.max(primaryGlyphLayout.width, screenXMessages) + 10;
            secondaryGlyphLayout.setText(chatFont, rightHandText, Color.BLACK, widthTotal - leftHandSize - 10, Align.bottomLeft, true);
            if (firstMessage) {
                screenY -= 10;
                firstMessage = false;
            }
            chatFont.draw(batch, primaryGlyphLayout, screenXSenders + 5, screenY);
            chatFont.draw(batch, secondaryGlyphLayout, leftHandSize, screenY);
            screenY -= Math.max(primaryGlyphLayout.height, secondaryGlyphLayout.height) * 1.2;
            if (screenY < screenCoords.y) {
                return; // No reason to render more
            }
        }
    }

    private void drawConfigMode(float x, float y, @NotNull Camera camera, @NotNull SpriteBatch batch) {

        float screenY;
        final float screenXKey;
        final float screenXValue;
        final float widthValue;
        final float widthKey;
        {
            final Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));
            widthKey = getWidth() * (1 - channelListSize) * senderNickSize;
            screenXKey = screenCoords.x + getWidth() * channelListSize;
            screenXValue = screenXKey + widthKey;
            widthValue = getWidth() * (1 - channelListSize) * (1 - senderNickSize);
            screenY = screenCoords.y + getHeight();
        }
        screenY -= 20.0F;

        primaryGlyphLayout.setText(font, "Changes only fully apply with a restart", Color.RED, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);
        screenY -= primaryGlyphLayout.height * 1.1F;

        float buttonHeight = renderButton(screenXValue, screenY, widthValue, unappliedUsername, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC username", "", "[WHITE]Current value is [GRAY]" + extension.name).setInitialText(extension.name)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    unappliedUsername = string;
                    unappliedConfigChanges.add(() -> {
                        extension.name = string;
                    });
                    getParentScreen().markDirty();
                }).build();
        });
        primaryGlyphLayout.setText(font, "Set Username", Color.WHITE, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = renderButton(screenXValue, screenY, widthValue, unappliedNick, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC nickname", "", "[WHITE]Current value is [GRAY]" + extension.nick).setInitialText(extension.nick)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    unappliedNick = string;
                    unappliedConfigChanges.add(() -> {
                        extension.nick = string;
                    });
                    getParentScreen().markDirty();
                }).build();
        });
        primaryGlyphLayout.setText(font, "Set Nickname", Color.WHITE, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = renderButton(screenXValue, screenY, widthValue, unappliedRealname, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC realname", "", "[WHITE]Current value is [GRAY]" + extension.realname).setInitialText(extension.realname)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    unappliedRealname = string;
                    unappliedConfigChanges.add(() -> {
                        extension.realname = string;
                    });
                    getParentScreen().markDirty();
                }).build();
        });
        primaryGlyphLayout.setText(font, "Set real name", Color.WHITE, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = renderButton(screenXValue, screenY, widthValue, unappliedDesktopNotify + " (might not work on all machines)", batch, () -> {
            // Toggle desktop-notify option
            unappliedDesktopNotify = !unappliedDesktopNotify;
            unappliedConfigChanges.add(() -> {
                extension.desktopNotifications = unappliedDesktopNotify;
            });
            getParentScreen().markDirty();
        });
        primaryGlyphLayout.setText(font, "Send desktop notifications", Color.WHITE, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = renderButton(screenXValue, screenY, widthValue, NullUtils.requireNotNull(Boolean.toString(unappliedCriticalNotify)), batch, () -> {
            // Toggle desktop-notify option
            unappliedCriticalNotify = !unappliedCriticalNotify;
            unappliedConfigChanges.add(() -> {
                extension.criticalNotification = unappliedCriticalNotify;
            });
            getParentScreen().markDirty();
        });
        primaryGlyphLayout.setText(font, "Use critical category", Color.WHITE, widthKey, Align.topLeft, true);
        font.draw(batch, primaryGlyphLayout, screenXKey + 10, screenY);
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    @NotNull
    public LineWrappingInfo getLineWrappingInfo() {
        return new LineWrappingInfo(true, true, true, true);
    }

    @Override
    @NotNull
    public Screen getParentScreen() {
        return parent;
    }

    @SuppressWarnings("null")
    @NotNull
    private Color getSenderColor(@NotNull String sender) {
        return switch (sender.hashCode() % 32) {
        case 0 -> Color.BLACK;
        case 1 -> Color.BLUE;
        case 2 -> Color.BROWN;
        case 3 -> Color.CHARTREUSE;
        case 4 -> Color.CORAL;
        case 5 -> Color.CYAN;
        case 6 -> Color.DARK_GRAY;
        case 7 -> Color.FIREBRICK;
        case 8 -> Color.FOREST;
        case 9 -> Color.GOLD;
        case 10 -> Color.GOLDENROD;
        case 11 -> Color.GRAY;
        case 12 -> Color.GREEN;
        case 13 -> Color.LIGHT_GRAY;
        case 14 -> Color.LIME;
        case 15 -> Color.MAGENTA;
        case 16 -> Color.MAROON;
        case 17 -> Color.NAVY;
        case 18 -> Color.OLIVE;
        case 19 -> Color.ORANGE;
        case 20 -> Color.PINK;
        case 21 -> Color.PURPLE;
        case 22 -> Color.RED;
        case 23 -> Color.ROYAL;
        case 24 -> Color.SALMON;
        case 25 -> Color.SCARLET;
        case 26 -> Color.SKY;
        case 27 -> Color.SLATE;
        case 28 -> Color.TAN;
        case 29 -> Color.TEAL;
        case 30 -> Color.VIOLET;
        case 31 -> DARKER_YELLOW;
        default -> Color.BLACK;
        };
    }

    @Override
    public int getWidth() {
        return parent.getInnerWidth() + 20;
    }

    @Override
    public boolean isSameType(@NotNull ScreenComponent component) {
        return component instanceof IRCMainScreenComponent;
    }

    @Override
    public void onClick(int screenX, int screenY, int componentX, int componentY, @NotNull Camera camera) {
        for (ChannelListPositioningMetaEntry metaentry : channelListPositioningMeta) {
            if (metaentry.x < screenX
                    && screenX < (metaentry.x + metaentry.width)
                    && (screenY) < metaentry.y
                    && (screenY) > (metaentry.y - metaentry.height)) {
                if (selectedChannel.isEmpty()) {
                    selectedChannel = NullUtils.<Channel>asOptional(metaentry.ch);
                } else if (selectedChannel.get() != metaentry.ch) {
                    scroll = 0;
                    selectedChannel.get().chat().setStatus(ChannelStatus.READ);
                    selectedChannel = NullUtils.<Channel>asOptional(metaentry.ch);
                }
                metaentry.ch().chat().setStatus(ChannelStatus.SELECTED);
                return;
            }
        }
        for (ButtonPositioningMetaEntry metaentry : buttonPositioningMeta) {
            if (metaentry.x < screenX
                    && screenX < (metaentry.x + metaentry.width)
                    && (screenY) < metaentry.y
                    && (screenY) > (metaentry.y - metaentry.height)) {
                metaentry.action.run();
                getParentScreen().markDirty();
                return;
            }
        }
    }

    @Override
    public void onHover(int screenX, int screenY, int componentX, int componentY, @NotNull Camera camera) {
         // TODO Auto-generated method stub
    }

    @Override
    public void onLongClick(int screenX, int screenY, int componentX, int componentY, @NotNull Camera camera) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onScroll(int screenX, int screenY, int componentX, int componentY, @NotNull Camera camera, int amount) {
        if (this.scroll == 0 && amount == 1) { // Scroll must always be negative
            return;
        }
        this.scroll += amount;
        getParentScreen().markDirty();
    }

    @Override
    public int renderAt(final float x, final float y, final @NotNull Camera camera) {
        buttonPositioningMeta.clear();
        NinePatch windowNine = Drawing.getTextureProvider().getWindowNinepatch();
        DrawingImpl drawing = Drawing.requireInstance();
        SpriteBatch batch = drawing.getMainDrawingBatch();
        boolean wasNotDrawing = false;
        if (!batch.isDrawing()) {
            wasNotDrawing = true;
            batch.begin();
        }
        final float height = getHeight();
        final float width = getWidth();
        batch.setProjectionMatrix(camera.combined);

        batch.setColor(Color.WHITE);
        Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));
        // Main frame
        windowNine.draw(batch, screenCoords.x, screenCoords.y, width, height);

        // Server + Channel List frame
        windowNine.draw(batch, screenCoords.x, screenCoords.y, width * channelListSize, height);

        // Chat frame
        windowNine.draw(batch, screenCoords.x + width * channelListSize, screenCoords.y, width * (1 - channelListSize), height);

        drawChannels(x, y, camera, batch);
        drawAdditionalButtons(x, y, camera, batch);

        if (configureMode) {
            drawConfigMode(x, y, camera, batch);
        } else {
            drawChat(x, y, camera, batch);
        }

        if (wasNotDrawing) {
            batch.end();
        }
        return (int) width;
    }

    private float renderButton(float screenX, float screenY, float width,
            @NotNull String text, @NotNull SpriteBatch batch, @NotNull Runnable action) {
        primaryGlyphLayout.setText(font, text, Color.WHITE, width - 15, Align.center, true);
        NinePatch background = Drawing.getTextureProvider().getWindowNinepatch();
        batch.setColor(Color.ORANGE);
        background.draw(batch, screenX, screenY - primaryGlyphLayout.height * 2.0F, width, primaryGlyphLayout.height * 3.0F);
        font.draw(batch, primaryGlyphLayout, screenX + 7.5F, screenY);
        float buttonY = screenY + primaryGlyphLayout.height;
        float buttonHeight = primaryGlyphLayout.height * 3.0F;
        buttonPositioningMeta.add(new ButtonPositioningMetaEntry(action, screenX, buttonY, width, buttonHeight));
        return buttonHeight;
    }
}
