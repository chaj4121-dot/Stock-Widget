package app.telemetry.market.widget;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.BoringLayout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.Spanned;

import java.util.List;
import java.util.Locale;

import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteFetcher;

/**
 * News-chyron text for a host-side layout animation.
 * The view holds two identical copies and translates by exactly half its width,
 * so the loop restart is pixel-identical and does not depend on AlarmManager.
 */
public final class TapeComposer {
    public static final float BASE_DP_PER_SEC = 160f;
    /** Presets must match res/layout/tape_host_*.xml and res/anim/tape_scroll_*.xml. */
    public static final int[] DURATIONS_MS = {
            700, 1000, 1400, 2000, 2800, 4000, 5600, 8000, 11000, 16000
    };

    public static final class Tape {
        public CharSequence text;
        public int widthPx;
        public float textPx;
        public int durationMs;
        public int padTopPx;
    }

    private TapeComposer() {}

    public static int layoutRes(int durationMs) {
        switch (durationMs) {
            case 700: return app.telemetry.market.R.layout.tape_host_700;
            case 1000: return app.telemetry.market.R.layout.tape_host_1000;
            case 1400: return app.telemetry.market.R.layout.tape_host_1400;
            case 2000: return app.telemetry.market.R.layout.tape_host_2000;
            case 2800: return app.telemetry.market.R.layout.tape_host_2800;
            case 4000: return app.telemetry.market.R.layout.tape_host_4000;
            case 5600: return app.telemetry.market.R.layout.tape_host_5600;
            case 8000: return app.telemetry.market.R.layout.tape_host_8000;
            case 11000: return app.telemetry.market.R.layout.tape_host_11000;
            default: return app.telemetry.market.R.layout.tape_host_16000;
        }
    }

    public static Tape compose(
            WidgetPrefs prefs, List<Quote> quotes, float density, int viewWidthPx, boolean halfBar) {
        float dp = density <= 0 ? 3f : density;
        float tickerS = clamp(prefs.tickerScale <= 0 ? 1.25f : prefs.tickerScale, 0.5f, 2.5f);
        float priceS = clamp(prefs.fontScale, 0.5f, 2.5f);
        boolean compact = halfBar || "compact".equals(prefs.size);
        float textPx = Math.max(
                (compact ? 13.5f : 15f) * dp * tickerS,
                (compact ? 15f : 17f) * dp * priceS);
        boolean dark = prefs.darkText();
        int fg = dark ? Color.rgb(18, 26, 34) : Color.rgb(248, 251, 253);
        int upC = dark ? Color.rgb(12, 118, 104) : Color.rgb(94, 234, 212);
        int downC = dark ? Color.rgb(196, 52, 56) : Color.rgb(240, 113, 120);
        boolean showMove = !(prefs.hideMoveWhenClosed && QuoteFetcher.isClosedSession());

        TextPaint paint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        paint.setTextSize(textPx);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setColor(fg);

        SpannableStringBuilder unit = new SpannableStringBuilder();
        appendUnit(unit, prefs, quotes, fg, upC, downC, showMove);
        int unitStartW = widthOf(paint, unit);
        if (unitStartW < 8) unitStartW = 8;

        int viewport = Math.max(1, viewWidthPx);
        int reps = 1;
        while (unitStartW * reps < viewport && reps < 8) reps++;

        SpannableStringBuilder block = new SpannableStringBuilder();
        for (int i = 0; i < reps; i++) block.append(unit);
        int period = Math.max(1, widthOf(paint, block));

        float speed = BASE_DP_PER_SEC * dp * clamp(prefs.marqueeSpeed, 0.25f, 4f);
        int wantMs = Math.round(period / Math.max(40f, speed) * 1000f);
        int duration = nearest(wantMs);

        SpannableStringBuilder text = new SpannableStringBuilder();
        text.append(block).append(block);
        int full = Math.max(period * 2, widthOf(paint, text));

        Tape tape = new Tape();
        tape.text = text;
        tape.widthPx = full;
        tape.textPx = textPx;
        tape.durationMs = duration;
        float headerPx = 9f * dp * tickerS;
        boolean header = prefs.showHeader;
        boolean ribbon = prefs.weekendMode && QuoteFetcher.isClosedSession();
        tape.padTopPx = (header || ribbon) ? Math.round(headerPx + 6f * dp) : 0;
        return tape;
    }

    private static void appendUnit(
            SpannableStringBuilder sb, WidgetPrefs prefs, List<Quote> quotes,
            int fg, int upC, int downC, boolean showMove) {
        String[] tickers = prefs.tickers == null ? new String[0] : prefs.tickers;
        for (int i = 0; i < tickers.length; i++) {
            String symbol = tickers[i] == null ? "—" : tickers[i];
            Quote q = quoteOf(quotes, symbol, i);
            boolean up = q != null && q.changePct >= 0;
            String price = WidgetRenderer.formatPrice(symbol, q == null ? 0 : q.price);
            int from = sb.length();
            sb.append(symbol).append("  ").append(price);
            sb.setSpan(new ForegroundColorSpan(fg), from, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (showMove && q != null && q.price > 0) {
                String pct = (up ? "  ▲ " : "  ▼ ")
                        + String.format(Locale.US, "%.2f%%", Math.abs(q.changePct));
                int p0 = sb.length();
                sb.append(pct);
                sb.setSpan(new ForegroundColorSpan(up ? upC : downC), p0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append("      ");
        }
        if (tickers.length == 0) {
            int from = sb.length();
            sb.append("MARKET  ");
            sb.setSpan(new ForegroundColorSpan(fg), from, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static Quote quoteOf(List<Quote> quotes, String symbol, int i) {
        if (quotes == null) return null;
        for (Quote q : quotes) {
            if (q != null && symbol.equalsIgnoreCase(q.symbol)) return q;
        }
        return i < quotes.size() ? quotes.get(i) : null;
    }

    private static int widthOf(TextPaint paint, CharSequence text) {
        if (text == null || text.length() == 0) return 0;
        BoringLayout.Metrics metrics = BoringLayout.isBoring(text, paint);
        if (metrics != null) return Math.max(1, Math.round(metrics.width));
        return Math.max(1, Math.round(paint.measureText(text, 0, text.length())));
    }

    private static int nearest(int want) {
        int best = DURATIONS_MS[0];
        int bestDiff = Math.abs(want - best);
        for (int d : DURATIONS_MS) {
            int diff = Math.abs(want - d);
            if (diff < bestDiff) {
                best = d;
                bestDiff = diff;
            }
        }
        return best;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
