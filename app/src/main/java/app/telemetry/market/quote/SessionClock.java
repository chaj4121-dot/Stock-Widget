package app.telemetry.market.quote;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * US session badge. Times are Korea local. Pre and overnight show time until
 * the cash open. Regular hours show the close. After-hours show the after end.
 */
public final class SessionClock {
    private static final ZoneId NY = ZoneId.of("America/New_York");
    private static final ZoneId KR = ZoneId.of("Asia/Seoul");

    public static final class Badge {
        public final String tag;
        public final String value;

        public Badge(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }
    }

    private SessionClock() {}

    public static Badge badge() {
        return badge(System.currentTimeMillis());
    }

    static Badge badge(long epochMs) {
        ZonedDateTime now = Instant.ofEpochMilli(epochMs).atZone(NY);
        if (isWeekend(now)) {
            return new Badge("OPENS", remain(Duration.between(now, nextOpen(now))));
        }
        ZonedDateTime open = at(now, 9, 30);
        ZonedDateTime close = at(now, 16, 0);
        ZonedDateTime afterEnd = at(now, 20, 0);
        ZonedDateTime preStart = at(now, 4, 0);
        if (!now.isBefore(preStart) && now.isBefore(open)) {
            return new Badge("OPENS", remain(Duration.between(now, open)));
        }
        if (!now.isBefore(open) && now.isBefore(close)) {
            return new Badge("CLOSES", krTime(close));
        }
        if (!now.isBefore(close) && now.isBefore(afterEnd)) {
            return new Badge("AFTER", krTime(afterEnd));
        }
        return new Badge("OPENS", remain(Duration.between(now, nextOpen(now))));
    }

    private static ZonedDateTime at(ZonedDateTime now, int hour, int minute) {
        return now.truncatedTo(ChronoUnit.DAYS).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private static ZonedDateTime nextOpen(ZonedDateTime now) {
        ZonedDateTime cursor = at(now, 9, 30);
        if (!cursor.isAfter(now)) cursor = at(cursor.plusDays(1), 9, 30);
        while (isWeekend(cursor)) cursor = at(cursor.plusDays(1), 9, 30);
        return cursor;
    }

    private static boolean isWeekend(ZonedDateTime t) {
        DayOfWeek d = t.getDayOfWeek();
        return d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
    }

    private static String remain(Duration d) {
        long mins = Math.max(0, d.toMinutes());
        long days = mins / (60 * 24);
        long hours = (mins / 60) % 24;
        long m = mins % 60;
        if (mins <= 0) return "now";
        if (days > 0) return hours == 0 ? days + "d " + m + "m" : days + "d " + hours + "h";
        if (hours > 0) return m == 0 ? hours + "h" : hours + "h " + m + "m";
        return mins + "m";
    }

    private static String krTime(ZonedDateTime ny) {
        ZonedDateTime kr = ny.withZoneSameInstant(KR);
        return String.format("%02d:%02d KST", kr.getHour(), kr.getMinute());
    }
}
