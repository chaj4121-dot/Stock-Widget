package app.telemetry.market.quote;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * US cash-session clock for the tape.
 * Pre: countdown to the regular open. After-hours: after-market end in KST.
 * Regular: official close in KST. Overnight and weekends: countdown to the next open.
 */
public final class SessionClock {
    private static final ZoneId NY = ZoneId.of("America/New_York");
    private static final ZoneId KR = ZoneId.of("Asia/Seoul");

    private SessionClock() {}

    public static String tapeText() {
        return tapeText(System.currentTimeMillis());
    }

    static String tapeText(long epochMs) {
        ZonedDateTime now = Instant.ofEpochMilli(epochMs).atZone(NY);
        if (isWeekend(now)) {
            return "개장까지 " + remain(Duration.between(now, nextOpen(now)));
        }
        ZonedDateTime open = at(now, 9, 30);
        ZonedDateTime close = at(now, 16, 0);
        ZonedDateTime afterEnd = at(now, 20, 0);
        ZonedDateTime preStart = at(now, 4, 0);
        if (!now.isBefore(preStart) && now.isBefore(open)) {
            return "개장까지 " + remain(Duration.between(now, open));
        }
        if (!now.isBefore(open) && now.isBefore(close)) {
            return "마감 " + krTime(close);
        }
        if (!now.isBefore(close) && now.isBefore(afterEnd)) {
            return "애프터 종료 " + krTime(afterEnd);
        }
        return "개장까지 " + remain(Duration.between(now, nextOpen(now)));
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
        if (mins <= 0) return "곧";
        if (days > 0) {
            return hours == 0 ? days + "일 " + m + "분" : days + "일 " + hours + "시간";
        }
        if (hours > 0) {
            return m == 0 ? hours + "시간" : hours + "시간 " + m + "분";
        }
        return mins + "분";
    }

    private static String krTime(ZonedDateTime ny) {
        ZonedDateTime kr = ny.withZoneSameInstant(KR);
        return String.format("%02d:%02d KST", kr.getHour(), kr.getMinute());
    }
}
