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

    private static class ButtonPositioningMetaEntry {
        @NotNull
        private final Runnable action;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public ButtonPositioningMetaEntry(@NotNull Runnable action, float x, float y, float width, float height) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class ChannelListPositioningMetaEntry {
        @NotNull
        private final Channel ch;
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public ChannelListPositioningMetaEntry(@NotNull Channel ch, float x, float y, float width, float height) {
            this.ch = ch;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static final Color DARKER_YELLOW = new Color(0.5F, 0.5F, 0.0F, 1F);

    @NotNull
    private static final DateTimeFormatter TIMESTAMP_FORMATTER;

    static {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendLiteral("[GRAY][").appendValue(ChronoField.HOUR_OF_DAY, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral(':').appendValue(ChronoField.MINUTE_OF_HOUR, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral(':').appendValue(ChronoField.SECOND_OF_MINUTE, 2, 2, SignStyle.NORMAL);
        builder.appendLiteral("] []");
        TIMESTAMP_FORMATTER = NullUtils.requireNotNull(builder.toFormatter());
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
    private static Optional<Channel> selectedChannel = Optional.empty();

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
            this.font = NullUtils.requireNotNull(Drawing.getFontBitmap(fontName));
        } else {
            this.font = tempFont;
        }
        tempFont = Drawing.getFontBitmap("FRIENDLY");
        if (tempFont == null) {
            extension.getLogger().warn("IRCMainScreenComponent: Unable to resolve requested chat font! Using the main font instead.");
            this.chatFont = this.font;
        } else {
            this.chatFont = tempFont;
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

        String text = this.configureMode ? "Save configuration" : "Configure";
        screenCoords.y += this.renderButton(screenCoords.x, screenCoords.y, this.getWidth() * this.channelListSize, text, batch, () -> {
            if (this.configureMode) {
                this.configureMode = false;
                for (Runnable action : this.unappliedConfigChanges) {
                    action.run();
                }
                this.extension.saveConfig(DataFolderProvider.getProvider().provideAsPath().resolve("galimulatorirc.json"));
            } else {
                this.configureMode = true;
            }
        });
        screenCoords.y += this.renderButton(screenCoords.x, screenCoords.y, this.getWidth() * this.channelListSize, "Send message", batch, () -> {
            Drawing.textInputBuilder("Send IRC Message", "", "Set the message you want to send")
                .addHook(message -> {
                    if (message == null) {
                        return; // User aborted send operation
                    }
                    this.extension.handleMessage(message, IRCMainScreenComponent.selectedChannel);
                }).build();
        });
        // Not a button but it still falls under the same area in a graphical sense, so I put it in this method
        text = "Scroll [LIME]" + scroll + "[] Memory [RED]" + (Runtime.getRuntime().totalMemory() / 1_000_000L) + "[] MB.";
        this.primaryGlyphLayout.setText(font, text, Color.WHITE, getWidth() * this.channelListSize - 10, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenCoords.x + 7.5F, screenCoords.y);
    }

    private void drawChannels(final float x, final float y, @NotNull Camera camera, SpriteBatch batch) {
        this.channelListPositioningMeta.clear();
        final Vector3 screenCoords = camera.project(new Vector3(x, y, 0.0F));
        float screenY = screenCoords.y + this.getHeight();
        final float width = this.getWidth() * this.channelListSize;

        for (Network network : this.extension.serverlist.getNetworks()) {
            screenY -= this.primaryGlyphLayout.height;
            this.primaryGlyphLayout.setText(this.font, network.name, Color.WHITE, width - 20, Align.topLeft, true);
            screenY -= this.primaryGlyphLayout.height;
            this.font.draw(batch, this.primaryGlyphLayout, screenCoords.x + 10, screenY);

            for (Channel channel : network.getChannels()) {
                ChannelStatus status = channel.chat().getStatus();
                screenY -= this.primaryGlyphLayout.height;
                this.primaryGlyphLayout.setText(this.font, channel.name(), status.getColor(), width - 30, Align.topLeft, true);
                screenY -= this.primaryGlyphLayout.height;
                this. font.draw(batch, this.primaryGlyphLayout, screenCoords.x + 20, screenY);
                this.channelListPositioningMeta.add(new ChannelListPositioningMetaEntry(channel, screenCoords.x , screenY, width, this.primaryGlyphLayout.height * 2.0F));
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
        widthTotal = this.getWidth() * (1 - this.channelListSize);
        widthSenders = widthTotal * this.senderNickSize;
        screenXSenders = screenCoords.x + this.getWidth() * this.channelListSize;
        screenXMessages = screenXSenders + widthSenders;
        screenY = screenCoords.y + this.getHeight();

        if (!IRCMainScreenComponent.selectedChannel.isPresent()) {
            this.primaryGlyphLayout.setText(this.chatFont, this.extension.errorMessage, Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= this.primaryGlyphLayout.height * 0.6;
            this.chatFont.draw(batch, this.primaryGlyphLayout, screenXSenders + 5, screenY);
            return;
        }

        Channel channel = IRCMainScreenComponent.selectedChannel.get();

        List<ChatMessage> messages = channel.chat().getLog();
        if (messages.isEmpty()) {
            this.primaryGlyphLayout.setText(this.chatFont, "No messages in this channel yet.", Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= this.primaryGlyphLayout.height * 0.6;
            this.chatFont.draw(batch, this.primaryGlyphLayout, screenXSenders + 5, screenY);
            return; // Else an IOOBE would happen in the next lines.
        }

        // We want to iterate backwards
        int startPosition = messages.size() + this.scroll;
        if (startPosition <= 0) {
            this.primaryGlyphLayout.setText(this.chatFont, "No older messages.", Color.FIREBRICK, widthTotal - 15, Align.topLeft, true);
            screenY -= this.primaryGlyphLayout.height * 0.6;
            this.chatFont.draw(batch, this.primaryGlyphLayout, screenXSenders + 5, screenY);
            this.scroll = -messages.size();
            return; // Else an IOOBE would happen at the next line.
        }
        ListIterator<ChatMessage> logIterator = messages.listIterator(startPosition);
        boolean firstMessage = true;
        while (logIterator.previousIndex() != -1) {
            ChatMessage message = logIterator.previous();
            String leftHandText = IRCMainScreenComponent.TIMESTAMP_FORMATTER.format(message.timestamp());
            String rightHandText = message.message();
            if (message.metamessage()) {
                leftHandText += "[RED]**[]";
                Color senderColor = this.getSenderColor(NullUtils.requireNotNull(message.sender()));
                rightHandText = NullUtils.format("[#%s] %s [] %s", senderColor.toString(), message.sender(), message.message());
            } else {
                leftHandText += message.sender();
            }
            this.primaryGlyphLayout.setText(this.chatFont, leftHandText, this.getSenderColor(NullUtils.requireNotNull(message.sender())), widthSenders - 15, Align.topLeft, false);
            float leftHandSize = Math.max(this.primaryGlyphLayout.width, screenXMessages) + 10;
            this.secondaryGlyphLayout.setText(this.chatFont, rightHandText, Color.BLACK, widthTotal - leftHandSize - 10, Align.bottomLeft, true);
            if (firstMessage) {
                screenY -= 10;
                firstMessage = false;
            }
            this.chatFont.draw(batch, this.primaryGlyphLayout, screenXSenders + 5, screenY);
            this.chatFont.draw(batch, this.secondaryGlyphLayout, leftHandSize, screenY);
            screenY -= Math.max(this.primaryGlyphLayout.height, this.secondaryGlyphLayout.height) * 1.2;
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
            widthKey = this.getWidth() * (1 - this.channelListSize) * this.senderNickSize;
            screenXKey = screenCoords.x + this.getWidth() * this.channelListSize;
            screenXValue = screenXKey + widthKey;
            widthValue = this.getWidth() * (1 - this.channelListSize) * (1 - this.senderNickSize);
            screenY = screenCoords.y + this.getHeight();
        }
        screenY -= 20.0F;

        this.primaryGlyphLayout.setText(font, "Changes only fully apply with a restart", Color.RED, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);
        screenY -= this.primaryGlyphLayout.height * 1.1F;

        float buttonHeight = this.renderButton(screenXValue, screenY, widthValue, this.unappliedUsername, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC username", "", "[WHITE]Current value is [GRAY]" + this.extension.name).setInitialText(extension.name)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    this.unappliedUsername = string;
                    this.unappliedConfigChanges.add(() -> {
                        this.extension.name = string;
                    });
                    this.getParentScreen().markDirty();
                }).build();
        });
        this.primaryGlyphLayout.setText(font, "Set Username", Color.WHITE, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = this.renderButton(screenXValue, screenY, widthValue, unappliedNick, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC nickname", "", "[WHITE]Current value is [GRAY]" + extension.nick).setInitialText(extension.nick)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    this.unappliedNick = string;
                    this.unappliedConfigChanges.add(() -> {
                        this.extension.nick = string;
                    });
                    this.getParentScreen().markDirty();
                }).build();
        });
        this.primaryGlyphLayout.setText(this.font, "Set Nickname", Color.WHITE, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = this.renderButton(screenXValue, screenY, widthValue, this.unappliedRealname, batch, () -> {
            // Change name
            Drawing.textInputBuilder("Change IRC realname", "", "[WHITE]Current value is [GRAY]" + this.extension.realname).setInitialText(extension.realname)
                .addHook(string -> {
                    if (string == null) {
                        return;
                    }
                    this.unappliedRealname = string;
                    this.unappliedConfigChanges.add(() -> {
                        this.extension.realname = string;
                    });
                    this.getParentScreen().markDirty();
                }).build();
        });
        this.primaryGlyphLayout.setText(this.font, "Set real name", Color.WHITE, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = this.renderButton(screenXValue, screenY, widthValue, this.unappliedDesktopNotify + " (might not work on all machines)", batch, () -> {
            // Toggle desktop-notify option
            this.unappliedDesktopNotify = !this.unappliedDesktopNotify;
            this.unappliedConfigChanges.add(() -> {
                this.extension.desktopNotifications = this.unappliedDesktopNotify;
            });
            this.getParentScreen().markDirty();
        });
        this.primaryGlyphLayout.setText(this.font, "Send desktop notifications", Color.WHITE, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);

        screenY -= buttonHeight;
        buttonHeight = this.renderButton(screenXValue, screenY, widthValue, NullUtils.requireNotNull(Boolean.toString(this.unappliedCriticalNotify)), batch, () -> {
            // Toggle desktop-notify option
            this.unappliedCriticalNotify = !this.unappliedCriticalNotify;
            this.unappliedConfigChanges.add(() -> {
                this.extension.criticalNotification = this.unappliedCriticalNotify;
            });
            this.getParentScreen().markDirty();
        });
        this.primaryGlyphLayout.setText(this.font, "Use critical category", Color.WHITE, widthKey, Align.topLeft, true);
        this.font.draw(batch, this.primaryGlyphLayout, screenXKey + 10, screenY);
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
        return this.parent;
    }

    @SuppressWarnings("null")
    @NotNull
    private Color getSenderColor(@NotNull String sender) {
        switch (sender.hashCode() % 32) {
        case 0:
            return Color.BLACK;
        case 1:
            return Color.BLUE;
        case 2:
            return Color.BROWN;
        case 3:
            return Color.CHARTREUSE;
        case 4:
            return Color.CORAL;
        case 5:
            return Color.CYAN;
        case 6:
            return Color.DARK_GRAY;
        case 7:
            return Color.FIREBRICK;
        case 8:
            return Color.FOREST;
        case 9:
            return Color.GOLD;
        case 10:
            return Color.GOLDENROD;
        case 11:
            return Color.GRAY;
        case 12:
            return Color.GREEN;
        case 13:
            return Color.LIGHT_GRAY;
        case 14:
            return Color.LIME;
        case 15:
            return Color.MAGENTA;
        case 16:
            return Color.MAROON;
        case 17:
            return Color.OLIVE;
        case 18:
            return Color.ORANGE;
        case 19:
            return Color.PINK;
        case 20:
            return Color.PURPLE;
        case 21:
            return Color.RED;
        case 22:
            return Color.ROYAL;
        case 23:
            return Color.SALMON;
        case 24:
            return Color.SCARLET;
        case 25:
            return Color.SKY;
        case 26:
            return Color.SLATE;
        case 28:
            return Color.TAN;
        case 29:
            return Color.TEAL;
        case 30:
            return Color.VIOLET;
        case 31:
            return IRCMainScreenComponent.DARKER_YELLOW;
        default:
            return Color.BLACK;
        }
    }

    @Override
    public int getWidth() {
        return this.parent.getInnerWidth() + 20;
    }

    @Override
    public boolean isSameType(@NotNull ScreenComponent component) {
        return component instanceof IRCMainScreenComponent;
    }

    @Override
    public void onClick(int screenX, int screenY, int componentX, int componentY, @NotNull Camera camera) {
        for (ChannelListPositioningMetaEntry metaentry : this.channelListPositioningMeta) {
            if (metaentry.x < screenX
                    && screenX < (metaentry.x + metaentry.width)
                    && (screenY) < metaentry.y
                    && (screenY) > (metaentry.y - metaentry.height)) {
                if (!IRCMainScreenComponent.selectedChannel.isPresent()) {
                    IRCMainScreenComponent.selectedChannel = Optional.of(metaentry.ch);
                } else if (IRCMainScreenComponent.selectedChannel.get() != metaentry.ch) {
                    this.scroll = 0;
                    IRCMainScreenComponent.selectedChannel.get().chat().setStatus(ChannelStatus.READ);
                    IRCMainScreenComponent.selectedChannel = Optional.of(metaentry.ch);
                }
                metaentry.ch.chat().setStatus(ChannelStatus.SELECTED);
                return;
            }
        }
        for (ButtonPositioningMetaEntry metaentry : this.buttonPositioningMeta) {
            if (metaentry.x < screenX
                    && screenX < (metaentry.x + metaentry.width)
                    && (screenY) < metaentry.y
                    && (screenY) > (metaentry.y - metaentry.height)) {
                metaentry.action.run();
                this.getParentScreen().markDirty();
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
        this.getParentScreen().markDirty();
    }

    @Override
    public int renderAt(final float x, final float y, final @NotNull Camera camera) {
        this.buttonPositioningMeta.clear();
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
        windowNine.draw(batch, screenCoords.x, screenCoords.y, width * this.channelListSize, height);

        // Chat frame
        windowNine.draw(batch, screenCoords.x + width * this.channelListSize, screenCoords.y, width * (1 - this.channelListSize), height);

        this.drawChannels(x, y, camera, batch);
        this.drawAdditionalButtons(x, y, camera, batch);

        if (this.configureMode) {
            this.drawConfigMode(x, y, camera, batch);
        } else {
            this.drawChat(x, y, camera, batch);
        }

        if (wasNotDrawing) {
            batch.end();
        }
        return (int) width;
    }

    private float renderButton(float screenX, float screenY, float width,
            @NotNull String text, @NotNull SpriteBatch batch, @NotNull Runnable action) {
        this.primaryGlyphLayout.setText(this.font, text, Color.WHITE, width - 15, Align.center, true);
        NinePatch background = Drawing.getTextureProvider().getWindowNinepatch();
        batch.setColor(Color.ORANGE);
        background.draw(batch, screenX, screenY - this.primaryGlyphLayout.height * 2.0F, width, this.primaryGlyphLayout.height * 3.0F);
        this.font.draw(batch, this.primaryGlyphLayout, screenX + 7.5F, screenY);
        float buttonY = screenY + this.primaryGlyphLayout.height;
        float buttonHeight = this.primaryGlyphLayout.height * 3.0F;
        this.buttonPositioningMeta.add(new ButtonPositioningMetaEntry(action, screenX, buttonY, width, buttonHeight));
        return buttonHeight;
    }
}
