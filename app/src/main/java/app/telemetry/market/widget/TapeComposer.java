package app.telemetry.market.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteFetcher;

/**
 * One wide bitmap of the chyron, drawn twice so a -50% slide loops cleanly.
 * Logos are pixels in that bitmap. A TextView cannot carry them into the launcher.
 */
public final class TapeComposer {
    public static final float BASE_DP_PER_SEC = 160f;
    public static final int[] DURATIONS_MS = {
            700, 1000, 1400, 2000, 2800, 4000, 5600, 8000, 11000, 16000,
            24000, 36000, 52000, 80000, 120000, 180000, 240000
    };

    public static final class Tape {
        public Bitmap bitmap;
        public int widthPx;
        public int heightPx;
        public int durationMs;
        public int padTopPx;
    }

    private TapeComposer() {}

    public static int logoPx(WidgetPrefs prefs, float density, boolean halfBar) {
        float dp = density <= 0 ? 3f : density;
        float tickerS = clamp(prefs.tickerScale <= 0 ? 1.25f : prefs.tickerScale, 0.5f, 2.5f);
        boolean compact = halfBar || "compact".equals(prefs.size);
        float symbolPx = (compact ? 13.5f : 15f) * dp * tickerS;
        return Math.max(32, Math.round(symbolPx * 1.2f));
    }

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
            case 16000: return app.telemetry.market.R.layout.tape_host_16000;
            case 24000: return app.telemetry.market.R.layout.tape_host_24000;
            case 36000: return app.telemetry.market.R.layout.tape_host_36000;
            case 52000: return app.telemetry.market.R.layout.tape_host_52000;
            case 80000: return app.telemetry.market.R.layout.tape_host_80000;
            case 120000: return app.telemetry.market.R.layout.tape_host_120000;
            case 180000: return app.telemetry.market.R.layout.tape_host_180000;
            default: return app.telemetry.market.R.layout.tape_host_240000;
        }
    }

    public static Tape compose(
            WidgetPrefs prefs, List<Quote> quotes, Map<String, Bitmap> logos,
            float density, int viewWidthPx, boolean halfBar) {
        float dp = density <= 0 ? 3f : density;
        float tickerS = clamp(prefs.tickerScale <= 0 ? 1.25f : prefs.tickerScale, 0.5f, 2.5f);
        float priceS = clamp(prefs.fontScale, 0.5f, 2.5f);
        boolean compact = halfBar || "compact".equals(prefs.size);
        float symbolPx = (compact ? 13.5f : 15f) * dp * tickerS;
        float pricePx = (compact ? 15f : 17f) * dp * priceS;
        float pctSize = Math.max(10f * dp, pricePx * 0.62f);
        boolean dark = prefs.darkText();
        int fg = dark ? Color.rgb(18, 26, 34) : Color.rgb(248, 251, 253);
        int upC = dark ? Color.rgb(12, 118, 104) : Color.rgb(94, 234, 212);
        int downC = dark ? Color.rgb(196, 52, 56) : Color.rgb(240, 113, 120);
        boolean showMove = !(prefs.hideMoveWhenClosed && QuoteFetcher.isClosedSession());
        boolean wantLogo = prefs.showLogos;
        float logoS = wantLogo ? symbolPx * 1.2f : 0f;

        String[] tickers = prefs.tickers == null ? new String[0] : prefs.tickers;
        float unitW = 0f;
        for (int i = 0; i < tickers.length; i++) {
            unitW += segmentWidth(tickers[i], quoteOf(quotes, tickers[i], i), symbolPx, pricePx, pctSize,
                    prefs.fontWeight, dark, dp, logoS, showMove);
        }
        if (unitW < 8f) unitW = 8f;

        int viewport = Math.max(1, viewWidthPx);
        int reps = 1;
        while (unitW * reps < viewport && reps < 6) reps++;
        int periodPx = Math.max(1, Math.round(unitW * reps));
        int full = periodPx * 2;

        float row = Math.max(logoS, Math.max(symbolPx, pricePx));
        int height = Math.max(1, Math.round(row + 8f * dp));
        float baseline = height - Math.max(3f * dp, pricePx * 0.28f);

        Bitmap bitmap = Bitmap.createBitmap(full, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float x = 0f;
        for (int copy = 0; copy < 2; copy++) {
            float start = copy * periodPx;
            x = start;
            for (int r = 0; r < reps; r++) {
                for (int i = 0; i < tickers.length; i++) {
                    String symbol = tickers[i] == null ? "—" : tickers[i];
                    x = drawSegment(canvas, x, symbol, quoteOf(quotes, symbol, i), logos,
                            fg, upC, downC, symbolPx, pricePx, pctSize, prefs.fontWeight, dark, dp,
                            logoS, baseline, showMove);
                }
            }
        }

        float speed = BASE_DP_PER_SEC * dp * clamp(prefs.marqueeSpeed, 0.1f, 4f);
        Tape tape = new Tape();
        tape.bitmap = bitmap;
        tape.widthPx = full;
        tape.heightPx = height;
        tape.durationMs = nearest(Math.round(periodPx / Math.max(40f, speed) * 1000f));
        float headerPx = 9f * dp * tickerS;
        boolean header = prefs.showHeader;
        tape.padTopPx = header ? Math.round(headerPx + 6f * dp) : 0;
        return tape;
    }

    private static float segmentWidth(
            String symbol, Quote q, float symbolPx, float pricePx, float pctSize,
            String weight, boolean dark, float dp, float logoS, boolean showMove) {
        Paint sym = paint(Color.WHITE, symbolPx, "medium", dark);
        Paint prc = paint(Color.WHITE, pricePx, weight, dark);
        String priceTxt = WidgetRenderer.formatPrice(symbol, q == null ? 0 : q.price);
        float w = sym.measureText(symbol == null ? "—" : symbol) + 8f * dp + prc.measureText(priceTxt);
        if (showMove && q != null && q.price > 0) {
            boolean up = q.changePct >= 0;
            String pctTxt = (up ? "▲ " : "▼ ") + String.format(Locale.US, "%.2f%%", Math.abs(q.changePct));
            Paint ch = paint(Color.WHITE, pctSize, "medium", dark);
            w += 8f * dp + ch.measureText(pctTxt);
        }
        w += 26f * dp;
        if (logoS > 0f) w += logoS + 6f * dp;
        return w;
    }

    private static float drawSegment(
            Canvas c, float x, String symbol, Quote q, Map<String, Bitmap> logos,
            int fg, int upC, int downC, float symbolPx, float pricePx, float pctSize,
            String weight, boolean dark, float dp, float logoS, float baseline, boolean showMove) {
        if (logoS > 0f) {
            Bitmap lg = logos == null ? null : logos.get(symbol);
            if (lg != null && !lg.isRecycled()) {
                c.drawBitmap(lg, null, new RectF(x, baseline - logoS * 0.82f, x + logoS, baseline - logoS * 0.82f + logoS),
                        new Paint(Paint.FILTER_BITMAP_FLAG));
            }
            x += logoS + 6f * dp;
        }
        Paint sym = paint(fg, symbolPx, "medium", dark);
        c.drawText(symbol, x, baseline, sym);
        x += sym.measureText(symbol) + 8f * dp;
        String priceTxt = WidgetRenderer.formatPrice(symbol, q == null ? 0 : q.price);
        Paint prc = paint(fg, pricePx, weight, dark);
        c.drawText(priceTxt, x, baseline, prc);
        x += prc.measureText(priceTxt);
        if (showMove && q != null && q.price > 0) {
            boolean up = q.changePct >= 0;
            String pctTxt = (up ? "▲ " : "▼ ") + String.format(Locale.US, "%.2f%%", Math.abs(q.changePct));
            Paint ch = paint(up ? upC : downC, pctSize, "medium", dark);
            x += 8f * dp;
            c.drawText(pctTxt, x, baseline, ch);
            x += ch.measureText(pctTxt);
        }
        return x + 26f * dp;
    }

    private static Quote quoteOf(List<Quote> quotes, String symbol, int i) {
        if (quotes == null || symbol == null) return i >= 0 && quotes != null && i < quotes.size() ? quotes.get(i) : null;
        for (Quote q : quotes) {
            if (q != null && symbol.equalsIgnoreCase(q.symbol)) return q;
        }
        return i < quotes.size() ? quotes.get(i) : null;
    }

    private static Paint paint(int color, float size, String weight, boolean dark) {
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(color);
        t.setTextSize(size);
        t.setLinearText(true);
        t.setSubpixelText(false);
        t.setLetterSpacing(0.01f);
        if ("bold".equals(weight)) {
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        } else if ("medium".equals(weight)) {
            t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        } else {
            t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (!dark) t.setShadowLayer(2.2f, 0, 1f, Color.argb(70, 0, 0, 0));
        return t;
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
