package de.geolykt.galimirc.serverlist;

import com.badlogic.gdx.graphics.Color;

public enum ChannelStatus {

    READ,
    SELECTED,
    UNREAD_JOINS,
    UNREAD_MESSAGES,
    UNREAD_PINGS;

    public Color getColor() {
        return switch (this) {
        case READ -> Color.WHITE;
        case UNREAD_MESSAGES -> Color.DARK_GRAY;
        case UNREAD_JOINS -> Color.SKY;
        case UNREAD_PINGS -> Color.MAGENTA;
        case SELECTED -> Color.LIME;
        default -> Color.FIREBRICK;
        };
    }
}
