package app.telemetry.market.work;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import app.telemetry.market.widget.SlimWidgetProvider;
import app.telemetry.market.widget.TelemetryWidgetProvider;
import app.telemetry.market.widget.WidgetPrefs;
import app.telemetry.market.widget.Wide4WidgetProvider;

public final class MarqueeScheduler {
    public static final String ACTION_TICK = "app.telemetry.market.MARQUEE_TICK";
    private static final long INTERVAL_MS = 200;
    private static final float DP_PER_SEC = 160f;

    private MarqueeScheduler() {}

    public static void start(Context ctx) {
        if (Build.VERSION.SDK_INT >= 31) {
            stop(ctx);
            return;
        }
        schedule(ctx, SystemClock.elapsedRealtime() + INTERVAL_MS);
    }

    public static void stop(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(ctx));
    }

    public static boolean anyActive(Context ctx) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(ctx);
        Class<?>[] providers = {TelemetryWidgetProvider.class, SlimWidgetProvider.class, Wide4WidgetProvider.class};
        for (Class<?> cls : providers) {
            for (int id : mgr.getAppWidgetIds(new ComponentName(ctx, cls))) {
                if (WidgetPrefs.load(ctx, id).tickers.length > 4) return true;
            }
        }
        return false;
    }

    private static void schedule(Context ctx, long atElapsed) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(ctx);
        try {
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            if (exact) am.setExact(AlarmManager.ELAPSED_REALTIME, atElapsed, p);
            else am.set(AlarmManager.ELAPSED_REALTIME, atElapsed, p);
        } catch (Throwable t) {
            try {
                am.set(AlarmManager.ELAPSED_REALTIME, atElapsed, p);
            } catch (Throwable ignored) {
            }
        }
    }

    private static PendingIntent pi(Context ctx) {
        Intent i = new Intent(ACTION_TICK).setPackage(ctx.getPackageName());
        return PendingIntent.getBroadcast(ctx, 991, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static class TickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_TICK.equals(intent.getAction())) return;
            // API 31+: the chyron runs inside the launcher. Never resume the alarm chain.
            if (Build.VERSION.SDK_INT >= 31) {
                stop(context);
                return;
            }

            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            Class<?>[] providers = {TelemetryWidgetProvider.class, SlimWidgetProvider.class, Wide4WidgetProvider.class};
            float density = context.getResources().getDisplayMetrics().density;
            boolean active = false;
            for (Class<?> cls : providers) {
                for (int id : mgr.getAppWidgetIds(new ComponentName(context, cls))) {
                    WidgetPrefs prefs = WidgetPrefs.load(context, id);
                    if (prefs.tickers.length <= 4) continue;
                    active = true;
                    long now = SystemClock.elapsedRealtime();
                    long start = WidgetPrefs.marqueeStart(context, id);
                    if (start <= 0 || start > now) {
                        start = now;
                        WidgetPrefs.putMarqueeStart(context, id, start);
                    }
                    float pxPerSec = DP_PER_SEC * density * Math.max(0.1f, prefs.marqueeSpeed);
                    float offset = (float) (((now - start) / 1000.0) * pxPerSec);
                    WidgetPrefs.putOffset(context, id, offset);
                    TelemetryWidgetProvider.updateOne(context, mgr, id, false);
                }
            }
            if (active) schedule(context, SystemClock.elapsedRealtime() + INTERVAL_MS);
        }
    }
}
