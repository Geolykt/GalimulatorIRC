package de.geolykt.galimirc.serverlist;

import com.badlogic.gdx.graphics.Color;

public enum ChannelStatus {

    READ,
    SELECTED,
    UNREAD_JOINS,
    UNREAD_MESSAGES,
    UNREAD_PINGS;

    public Color getColor() {
        switch (this) {
            case READ:
                return Color.WHITE;
            case UNREAD_MESSAGES:
                return Color.DARK_GRAY;
            case UNREAD_JOINS:
                return Color.SKY;
            case UNREAD_PINGS:
                return Color.MAGENTA;
            case SELECTED:
                return Color.LIME;
            default:
                return Color.FIREBRICK;
        }
    }
}
