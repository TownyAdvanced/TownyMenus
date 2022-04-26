package io.github.townyadvanced.townymenus.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Time {
    public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");
    private static final long MINUTE_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    private static final long HOUR_SECONDS = TimeUnit.HOURS.toSeconds(1);
    private static final long DAY_SECONDS = TimeUnit.DAYS.toSeconds(1);

    public static String formatLastOnline(long lastOnline) {
        final long now = System.currentTimeMillis();

        if (lastOnline > now)
            return "in the future";

        final long diff = TimeUnit.MILLISECONDS.toSeconds(now - lastOnline);

        if (diff < MINUTE_SECONDS)
            return "just now";
        else if (diff < 2 * MINUTE_SECONDS)
            return "a minute ago";
        else if (diff < 60 * MINUTE_SECONDS)
            return (diff / MINUTE_SECONDS) + " minutes ago";
        else if (diff < 2 * HOUR_SECONDS)
            return "an hour ago";
        else if (diff < 24 * HOUR_SECONDS)
            return (diff / HOUR_SECONDS) + " hours ago";
        else if (diff < 48 * HOUR_SECONDS)
            return "yesterday";
        else
            return (diff / DAY_SECONDS) + " days ago";
    }

    public static String formatRegistered(long registered) {
        return registeredFormat.format(new Date(registered));
    }
}
