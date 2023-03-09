package io.github.townyadvanced.townymenus.utils;

import com.palmergames.bukkit.towny.object.Translatable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Time {
    public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");
    private static final long MINUTE_SECONDS = TimeUnit.MINUTES.toSeconds(1);
    private static final long HOUR_SECONDS = TimeUnit.HOURS.toSeconds(1);
    private static final long DAY_SECONDS = TimeUnit.DAYS.toSeconds(1);

    public static Translatable ago(long time) {
        final long now = System.currentTimeMillis();

        if (time > now)
            return Translatable.of("time-future");

        final long diff = TimeUnit.MILLISECONDS.toSeconds(now - time);

        if (diff < MINUTE_SECONDS)
            return Translatable.of("just now");
        else if (diff < 2 * MINUTE_SECONDS)
            return Translatable.of("time-a-minute-ago");
        else if (diff < 60 * MINUTE_SECONDS)
            return Translatable.of("time-x-minutes-ago", diff / MINUTE_SECONDS);
        else if (diff < 2 * HOUR_SECONDS)
            return Translatable.of("time-an-hour-ago");
        else if (diff < 24 * HOUR_SECONDS)
            return Translatable.of("time-x-hours-ago", diff / HOUR_SECONDS);
        else if (diff < 48 * HOUR_SECONDS)
            return Translatable.of("time-yesterday");
        else
            return Translatable.of("time-x-days-ago", diff / DAY_SECONDS);
    }

    public static String formatRegistered(long registered) {
        return registeredFormat.format(new Date(registered));
    }

    /**
     * Formats the specified timestamp using the ago format if it's less than one month ago, otherwise uses the full registered format.
     * @param time The time
     * @param locale The locale used for translating if the last online is less than one month ago
     * @return The formatted registered time.
     */
    public static String registeredOrAgo(long time, Locale locale) {
        if (time + TimeUnit.DAYS.toMillis(30) < System.currentTimeMillis())
            return formatRegistered(time);
        else
            return ago(time).translate(locale);
    }
}
