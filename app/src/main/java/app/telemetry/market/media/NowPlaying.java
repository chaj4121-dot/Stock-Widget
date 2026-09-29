package app.telemetry.market.media;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

public final class NowPlaying {
    private static final String PREF = "telemetry_widget";
    private static final long STALE_MS = 2L * 60L * 60L * 1000L;
    private static final ThreadLocal<Hold> DRAW = new ThreadLocal<>();

    public static final class Track {
        public final String title;
        public final String artist;
        public final String pkg;
        public final boolean playing;

        public Track(String title, String artist, String pkg, boolean playing) {
            this.title = title == null ? "" : title;
            this.artist = artist == null ? "" : artist;
            this.pkg = pkg == null ? "" : pkg;
            this.playing = playing;
        }

        boolean same(Track o) {
            if (o == null) return false;
            return playing == o.playing
                    && title.equals(o.title)
                    && artist.equals(o.artist)
                    && pkg.equals(o.pkg);
        }
    }

    private static final class Hold {
        final Track track;
        final Bitmap icon;

        Hold(Track track, Bitmap icon) {
            this.track = track;
            this.icon = icon;
        }
    }

    private NowPlaying() {}

    public static boolean listening(Context ctx) {
        String flat = Settings.Secure.getString(
                ctx.getContentResolver(), "enabled_notification_listeners");
        return flat != null && flat.contains(ctx.getPackageName());
    }

    public static Track current(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!p.getBoolean("np_playing", false)) return null;
        long at = p.getLong("np_at", 0);
        if (at <= 0 || System.currentTimeMillis() - at > STALE_MS) return null;
        String title = p.getString("np_title", "");
        if (title == null || title.trim().isEmpty()) return null;
        return new Track(title.trim(), p.getString("np_artist", ""), p.getString("np_pkg", ""), true);
    }

    public static void save(Context ctx, Track track) {
        SharedPreferences.Editor ed = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit();
        if (track == null || !track.playing || track.title.trim().isEmpty()) {
            ed.putBoolean("np_playing", false).putLong("np_at", System.currentTimeMillis()).apply();
            return;
        }
        ed.putBoolean("np_playing", true)
                .putString("np_title", track.title.trim())
                .putString("np_artist", track.artist == null ? "" : track.artist.trim())
                .putString("np_pkg", track.pkg)
                .putLong("np_at", System.currentTimeMillis())
                .apply();
    }

    public static boolean unchanged(Context ctx, Track track) {
        Track prev = current(ctx);
        if (track == null || !track.playing) return prev == null;
        return track.same(prev);
    }

    public static Bitmap icon(Context ctx, String pkg, int size) {
        if (pkg == null || pkg.isEmpty() || size <= 0) return null;
        try {
            Drawable d = ctx.getPackageManager().getApplicationIcon(pkg);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            d.setBounds(0, 0, size, size);
            d.draw(c);
            return bmp;
        } catch (PackageManager.NameNotFoundException ignored) {
            return null;
        }
    }

    public static void bind(Track track, Bitmap icon) {
        if (track == null) DRAW.remove();
        else DRAW.set(new Hold(track, icon));
    }

    public static Track bound() {
        Hold h = DRAW.get();
        return h == null ? null : h.track;
    }

    public static Bitmap boundIcon() {
        Hold h = DRAW.get();
        return h == null ? null : h.icon;
    }
}
