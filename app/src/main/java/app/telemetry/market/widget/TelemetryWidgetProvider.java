package app.telemetry.market.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.telemetry.market.R;
import app.telemetry.market.config.ConfigActivity;
import app.telemetry.market.launch.Launch;
import app.telemetry.market.launch.LaunchFetcher;
import app.telemetry.market.launch.WeekendActivity;
import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteCache;
import app.telemetry.market.quote.QuoteFetcher;
import app.telemetry.market.work.MarqueeScheduler;
import app.telemetry.market.work.QuoteScheduler;

public class TelemetryWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_REFRESH = "app.telemetry.market.ACTION_REFRESH";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    @Override
    public void onEnabled(Context context) {
        QuoteScheduler.ensure(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        QuoteScheduler.ensure(context);
        for (int id : ids) updateOne(context, manager, id, true);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle newOptions) {
        updateOne(context, manager, id, false);
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) WidgetPrefs.delete(context, id);
        if (!MarqueeScheduler.anyActive(context)) MarqueeScheduler.stop(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (ACTION_REFRESH.equals(action) || Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            updateAll(context);
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] a = manager.getAppWidgetIds(new ComponentName(context, TelemetryWidgetProvider.class));
        for (int id : a) updateOne(context, manager, id, true);
        int[] b = manager.getAppWidgetIds(new ComponentName(context, SlimWidgetProvider.class));
        for (int id : b) updateOne(context, manager, id, true);
        int[] c = manager.getAppWidgetIds(new ComponentName(context, Wide4WidgetProvider.class));
        for (int id : c) updateOne(context, manager, id, true);
    }

    public static void updateOne(Context context, AppWidgetManager manager, int id) {
        updateOne(context, manager, id, true);
    }

    public static void updateOne(Context context, AppWidgetManager manager, int id, boolean network) {
        WidgetPrefs prefs = WidgetPrefs.load(context, id);
        float marqueeOffset = WidgetPrefs.offsetOf(context, id);
        if (Build.VERSION.SDK_INT >= 31) MarqueeScheduler.stop(context);
        else if (prefs.tickers.length > 4) MarqueeScheduler.start(context);
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int[] wh = sizePx(manager, id, dm, prefs);
        int w = wh[0];
        int h = wh[1];
        WidgetPrefs.saveSize(context, id, w, h, dm.density);

        PendingIntent cfgPi = activityPi(context, ConfigActivity.class, id, id);
        PendingIntent weekendPi = activityPi(context, WeekendActivity.class, id, id + 2000);

        List<Quote> cached = QuoteCache.merge(prefs.tickers, null, QuoteCache.load(context));
        Launch cachedLaunch = LaunchFetcher.next(context);
        push(context, manager, id, w, h, dm.density, prefs, cached, cachedLaunch, cfgPi, weekendPi, false, marqueeOffset);

        if (!network) return;
        final int fw = w;
        final int fh = h;
        final float density = dm.density;
        IO.execute(() -> {
            List<Quote> fresh = QuoteFetcher.fetch(prefs.tickers);
            List<Quote> merged = QuoteCache.merge(prefs.tickers, fresh, QuoteCache.load(context));
            if (!fresh.isEmpty()) QuoteCache.save(context, merged);
            try {
                LaunchFetcher.refresh(context);
            } catch (Exception ignored) {
            }
            Launch launch = LaunchFetcher.next(context);
            push(context, manager, id, fw, fh, density, prefs, merged, launch, cfgPi, weekendPi, true, marqueeOffset);
        });
    }

    private static int[] sizePx(AppWidgetManager manager, int id, DisplayMetrics dm, WidgetPrefs prefs) {
        Bundle opts = manager.getAppWidgetOptions(id);
        int minW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 360);
        int maxW = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW);
        int minH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 56);
        int maxH = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH);
        int dpW = Math.max(minW, maxW);
        int dpH = Math.max(minH, maxH);
        int w = Math.max(640, Math.min(1600, Math.round(dpW * dm.density)));
        int h = Math.max(100, Math.min(400, Math.round(Math.max(40, dpH) * dm.density)));
        return new int[]{w, h};
    }

    private static PendingIntent activityPi(Context context, Class<?> cls, int widgetId, int request) {
        Intent i = new Intent(context, cls);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(
                context, request, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void push(
            Context context, AppWidgetManager manager, int id,
            int w, int h, float density, WidgetPrefs prefs, List<Quote> quotes, Launch launch,
            PendingIntent cfgPi, PendingIntent weekendPi, boolean networkLogos, float marqueeOffset) {
        try {
            Map<String, Bitmap> logos = new HashMap<>();
            if (prefs.showLogos) {
                int size = Math.max(48, Math.round(28f * density));
                for (Quote q : quotes) {
                    logos.put(q.symbol, LogoCache.get(context, q.symbol, size, networkLogos));
                }
                for (String s : prefs.tickers) {
                    if (!logos.containsKey(s)) logos.put(s, LogoCache.get(context, s, size, networkLogos));
                }
            }
            boolean flow = prefs.tickers.length > 4 && Build.VERSION.SDK_INT >= 31;
            boolean slim = halfBar(manager, id, prefs);
            Bitmap bmp = WidgetRenderer.render(w, h, density, prefs, quotes, logos, launch, null, slim, marqueeOffset, flow);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_telemetry);
            views.setImageViewBitmap(R.id.widget_bitmap, bmp);
            views.removeAllViews(R.id.widget_tape_slot);
            if (flow) {
                int viewport = Math.max(1, w - Math.round(16f * density));
                TapeComposer.Tape tape = TapeComposer.compose(prefs, quotes, density, viewport, slim);
                RemoteViews host = new RemoteViews(context.getPackageName(), TapeComposer.layoutRes(tape.durationMs));
                host.setTextViewText(R.id.widget_tape, tape.text);
                host.setTextViewTextSize(R.id.widget_tape, TypedValue.COMPLEX_UNIT_PX, tape.textPx);
                host.setViewLayoutWidth(R.id.widget_tape, tape.widthPx, TypedValue.COMPLEX_UNIT_PX);
                host.setViewPadding(R.id.widget_tape_host, 0, tape.padTopPx, 0, 0);
                views.addView(R.id.widget_tape_slot, host);
            }
            boolean openBoard = prefs.weekendMode && QuoteFetcher.isClosedSession();
            PendingIntent click = openBoard ? weekendPi : cfgPi;
            views.setOnClickPendingIntent(R.id.widget_root, click);
            views.setOnClickPendingIntent(R.id.widget_tape_slot, click);
            manager.updateAppWidget(id, views);
        } catch (Throwable t) {
            try {
                Bitmap bmp = Bitmap.createBitmap(Math.max(480, w), Math.max(120, h), Bitmap.Config.ARGB_8888);
                android.graphics.Canvas c = new android.graphics.Canvas(bmp);
                c.drawColor(Color.argb(150, 6, 16, 24));
                android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                p.setColor(Color.WHITE);
                p.setTextSize(34);
                String line = prefs.tickers.length == 0 ? "Market Telemetry" : String.join("   ", prefs.tickers);
                c.drawText(line, 24, Math.max(70, h / 2f), p);
                RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_telemetry);
                views.setImageViewBitmap(R.id.widget_bitmap, bmp);
                views.setOnClickPendingIntent(R.id.widget_root, cfgPi);
                manager.updateAppWidget(id, views);
            } catch (Throwable ignored) {
            }
        }
    }

    static boolean halfBar(AppWidgetManager manager, int id, WidgetPrefs prefs) {
        if ("compact".equals(prefs.size)) return true;
        try {
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(id);
            if (info != null && info.provider != null) {
                String name = info.provider.getClassName();
                return name != null && name.contains("Slim");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
