package net.silthus.regions.util;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {

    }

    public static String formatDateTime(@Nullable Instant instant, String format) {

        if (instant == null) {
            return "N/A";
        }


        return new SimpleDateFormat(format, Locale.GERMAN).format(instant.atZone(ZoneId.systemDefault()));
    }

    public static String formatDateTime(Instant instant) {

        return formatDateTime(instant, "dd.MM.yyyy HH:mm");
    }
}
