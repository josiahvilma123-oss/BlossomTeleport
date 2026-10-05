package com.blossomsmp.teleport.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {

    // Understands &a style colours and &#FF69B4 hex colours
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexCharacter('#')
            .hexColors()
            .build();

    private Text() {
    }

    public static Component color(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LEGACY.deserialize(text);
    }

    /** Same as color(), but without the purple italics Minecraft puts on item names. */
    public static Component item(String text) {
        return color(text).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /** 75000 ms -> "1m 15s" */
    public static String time(long millis) {
        long total = Math.max(1, (millis + 999) / 1000);
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }
}
