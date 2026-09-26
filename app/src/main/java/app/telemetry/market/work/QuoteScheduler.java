package app.telemetry.market.work;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.work.WorkManager;

import app.telemetry.market.widget.TelemetryWidgetProvider;
import app.telemetry.market.widget.WidgetPrefs;

/**
 * Quote refresh shorter than 15 minutes cannot use WorkManager.
 * An exact alarm carries the interval the user picked.
 */
public final class QuoteScheduler {
    public static final String ACTION = "app.telemetry.market.QUOTE_REFRESH";

    private QuoteScheduler() {}

    public static void ensure(Context context) {
        Context app = context.getApplicationContext();
        try {
            WorkManager.getInstance(app).cancelUniqueWork("telemetry-quotes");
        } catch (Throwable ignored) {
        }
        schedule(app);
    }

    public static void schedule(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long delay = WidgetPrefs.refreshMinutes(context) * 60_000L;
        long at = SystemClock.elapsedRealtime() + delay;
        PendingIntent pi = pending(context);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, pi);
        }
    }

    private static PendingIntent pending(Context context) {
        Intent i = new Intent(context, RefreshReceiver.class);
        i.setAction(ACTION);
        return PendingIntent.getBroadcast(
                context, 44021, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PendingResult pending = goAsync();
            new Thread(() -> {
                try {
                    TelemetryWidgetProvider.updateAll(context);
                } finally {
                    schedule(context);
                    pending.finish();
                }
            }).start();
        }
    }
}
