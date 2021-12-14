package de.geolykt.galimirc.irc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import de.geolykt.starloader.api.NullUtils;

public class ColorConverter {

    public static final char IRC_COLOR_CODE = 0x03; // Also known as ETX (End of text), often represented as ^C
    public static final char IRC_COLOR_STRIP_CODE = 0x0F; // Also known as SI (Shift in), often represented as ^O

    public static final Map<String, String> IRC_TO_GDX;

    static {
        HashMap<String, String> irc2Gdx = new HashMap<>();
        irc2Gdx.put("00", "[#FFFFFF]"); // white
        irc2Gdx.put("01", "[#000000]"); // black
        irc2Gdx.put("02", "[#00007F]"); // blue
        irc2Gdx.put("03", "[#009300]"); // green
        irc2Gdx.put("04", "[#FF0000]"); // light red
        irc2Gdx.put("05", "[#7F0000]"); // brown
        irc2Gdx.put("06", "[#9C009C]"); // purple
        irc2Gdx.put("07", "[#FC7F00]"); // orange
        irc2Gdx.put("08", "[#FFFF00]"); // yellow
        irc2Gdx.put("09", "[#00FC00]"); // light green
        irc2Gdx.put("10", "[#009393]"); // cyan
        irc2Gdx.put("11", "[#00FFFF]"); // light cyan
        irc2Gdx.put("12", "[#0000FC]"); // light blue
        irc2Gdx.put("13", "[#FF00FF]"); // pink
        irc2Gdx.put("14", "[#7F7F7F]"); // grey
        irc2Gdx.put("15", "[#D2D2D2]"); // light grey
        IRC_TO_GDX = Collections.unmodifiableMap(irc2Gdx);
    }

    @NotNull
    public static String strip(@NotNull String input) {
        if (input.indexOf(IRC_COLOR_CODE) == -1) {
            return input;
        }
        // TODO background colors, and remove other attributes like bold, italics, etc
        char[] original = input.toCharArray();
        StringBuilder out = new StringBuilder(original.length);
        for (int i = 0; i < original.length; i++) {
            if (original[i] == IRC_COLOR_CODE) {
                if ((i + 2) >= original.length) {
                    break;
                }
                if (Character.isDigit(original[i + 2])) {
                    i += 2;
                } else {
                    i++;
                }
            } else if (original[i] != IRC_COLOR_STRIP_CODE) {
                out.append(original[i]);
            }
        }
        return NullUtils.requireNotNull(out.toString());
    }

    @NotNull
    public static String toGDX(@NotNull String input) {
        if (input.indexOf(IRC_COLOR_CODE) == -1) {
            return input;
        }
        // TODO background colors (is that even possible in GDX?), maybe even remove other attributes like bold, italics, etc
        // since that is not supported by gdx directly.
        char[] original = input.toCharArray();
        StringBuilder out = new StringBuilder(original.length);
        for (int i = 0; i < original.length; i++) {
            if (original[i] == IRC_COLOR_CODE) {
                if ((i + 2) >= original.length) {
                    break;
                }
                if (Character.isDigit(original[i + 2])) {
                    out.append(IRC_TO_GDX.getOrDefault(original[++i] + "" + original[++i], "[]"));
                } else {
                    out.append(IRC_TO_GDX.getOrDefault("0" + original[++i], "[]"));
                }
            } else if (original[i] == IRC_COLOR_STRIP_CODE) {
                out.append("[]");
            } else {
                out.append(original[i]);
            }
        }
        return NullUtils.requireNotNull(out.toString());
    }
}
