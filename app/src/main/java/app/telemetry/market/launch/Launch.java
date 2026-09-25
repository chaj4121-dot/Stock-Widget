package app.telemetry.market.launch;

import java.util.Locale;

public final class Launch {
    public final String name;
    public final String mission;
    public final String rocket;
    public final String pad;
    public final String location;
    public final String status;
    public final long netMs;

    public Launch(String name, String mission, String rocket, String pad, String location, String status, long netMs) {
        this.name = name == null ? "" : name;
        this.mission = mission == null || mission.isEmpty() ? this.name : mission;
        this.rocket = rocket == null ? "" : rocket;
        this.pad = pad == null ? "" : pad;
        this.location = location == null ? "" : location;
        this.status = status == null ? "" : status;
        this.netMs = netMs;
    }

    public String shortMission() {
        String m = mission;
        m = m.replace("Starlink Group ", "STARLINK ");
        m = m.replace("Starship Flight ", "FLIGHT ");
        if (m.contains("|")) m = m.substring(m.indexOf('|') + 1).trim();
        if (m.length() > 22) m = m.substring(0, 22);
        return m.toUpperCase(Locale.US);
    }

    public String rocketShort() {
        if (rocket.toLowerCase(Locale.US).contains("starship")) return "STARSHIP";
        if (rocket.toLowerCase(Locale.US).contains("heavy")) return "FH";
        if (rocket.toLowerCase(Locale.US).contains("falcon")) return "F9";
        return rocket.isEmpty() ? "SX" : rocket.toUpperCase(Locale.US);
    }

    public boolean isGo() {
        return "Go".equalsIgnoreCase(status) || status.toLowerCase(Locale.US).contains("go for");
    }

    public static String countdown(long netMs) {
        long s = (netMs - System.currentTimeMillis()) / 1000L;
        if (s < 0) return "LIFTOFF";
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (d > 0) return String.format(Locale.US, "T-%dd %02dh", d, h);
        if (h > 0) return String.format(Locale.US, "T-%dh %02dm", h, m);
        return String.format(Locale.US, "T-%02d:%02d", m, sec);
    }

    public static String countdownFull(long netMs) {
        long s = Math.max(0, (netMs - System.currentTimeMillis()) / 1000L);
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (d > 0) return String.format(Locale.US, "%d  %02d:%02d:%02d", d, h, m, sec);
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec);
    }

    public static boolean isLaunchTicker(String symbol) {
        if (symbol == null) return false;
        String s = symbol.toUpperCase(Locale.US);
        return s.equals("SPCX") || s.equals("SPACE") || s.equals("RKLB");
    }
}
