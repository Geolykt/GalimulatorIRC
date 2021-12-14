package de.geolykt.galimirc.serverlist;

import java.time.temporal.TemporalAccessor;

import org.jetbrains.annotations.NotNull;

public record ChatMessage(@NotNull String message, @NotNull String sender, @NotNull TemporalAccessor timestamp, boolean metamessage) { }
