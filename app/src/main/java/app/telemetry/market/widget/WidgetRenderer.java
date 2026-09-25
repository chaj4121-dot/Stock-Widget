package app.telemetry.market.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.telemetry.market.launch.Launch;
import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteFetcher;

public final class WidgetRenderer {
    private WidgetRenderer() {}

    public static Bitmap render(
            int width, int height, float density, WidgetPrefs prefs,
            List<Quote> quotes, Map<String, Bitmap> logos, Launch launch) {
        return render(width, height, density, prefs, quotes, logos, launch, null, false);
    }

    public static Bitmap render(
            int width, int height, float density, WidgetPrefs prefs,
            List<Quote> quotes, Map<String, Bitmap> logos, Launch launch, Bitmap reuse) {
        return render(width, height, density, prefs, quotes, logos, launch, reuse, "compact".equals(prefs.size));
    }

    public static Bitmap render(
            int width, int height, float density, WidgetPrefs prefs,
            List<Quote> quotes, Map<String, Bitmap> logos, Launch launch, Bitmap reuse, boolean halfBar) {
        return render(width, height, density, prefs, quotes, logos, launch, reuse, halfBar, 0f);
    }

    public static Bitmap render(
            int width, int height, float density, WidgetPrefs prefs,
            List<Quote> quotes, Map<String, Bitmap> logos, Launch launch, Bitmap reuse, boolean halfBar,
            float marqueeOffset) {
        int w = Math.max(480, width);
        int h = Math.max(80, height);
        Bitmap bmp;
        if (reuse != null && !reuse.isRecycled() && reuse.getWidth() == w && reuse.getHeight() == h) {
            reuse.eraseColor(Color.TRANSPARENT);
            bmp = reuse;
        } else {
            bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        }
        Canvas c = new Canvas(bmp);

        boolean compact = halfBar || "compact".equals(prefs.size);
        boolean wideRow = (float) w / Math.max(1, h) >= 3.2f;
        boolean large = !compact && "large".equals(prefs.size);
        boolean dark = prefs.darkText();
        float tickerS = Math.max(0.5f, Math.min(2.5f, prefs.tickerScale <= 0 ? 1.25f : prefs.tickerScale));
        float priceS = Math.max(0.5f, Math.min(2.5f, prefs.fontScale));
        float dp = density <= 0 ? 3f : density;

        float headerPx = 9f * dp * tickerS;
        float symbolPx = (compact ? 13.5f : 15f) * dp * tickerS;
        float pricePx = (compact ? 15f : 17f) * dp * priceS;
        float pctPx = 10f * dp * priceS;
        float pad = (compact ? 6f : large ? 12f : 8f) * dp;
        float gap = 2.5f * dp;
        float logo = prefs.showLogos ? symbolPx * 1.2f : 0;
        int total = Math.max(1, Math.min(10, quotes.isEmpty() ? prefs.tickers.length : quotes.size()));
        boolean spark = prefs.showSparklines && !compact && (large || h > 100 * dp);
        float sparkH = spark ? (large ? 14f : 11f) * dp : 0;
        boolean header = prefs.showHeader;
        boolean closed = QuoteFetcher.isClosedSession();
        boolean showMove = !(prefs.hideMoveWhenClosed && closed);
        boolean ribbon = prefs.weekendMode && closed && launch != null;

        float gapY = compact ? 2.2f * dp : 5f * dp;
        float descent = pricePx * 0.34f;
        float bodyH = Math.max(logo, symbolPx) + sparkH + gapY + pricePx + descent
                + (showMove ? pctPx + 6f * dp : 8f * dp);
        float headH = (header || ribbon) ? headerPx + gap * 1.6f : 0;
        float avail = h - pad * 2f - headH - 4f;
        if (halfBar) avail = Math.min(avail, h * 0.50f - pad * 2f - headH);
        if (avail > 40 && bodyH > avail) {
            float s = avail / bodyH;
            logo *= s;
            symbolPx *= s;
            pricePx *= s;
            pctPx *= s;
            sparkH *= s;
            bodyH = avail;
        }
        float contentH = pad * 2 + headH + bodyH;
        if (halfBar) contentH = Math.min(contentH, h * 0.50f);
        else if (wideRow) contentH = Math.min(contentH, h * 0.92f);
        else if ("compact".equals(prefs.size)) contentH = Math.min(contentH, h * 0.66f);
        else if (!large) contentH = Math.min(contentH, h * 0.88f);
        contentH = Math.min(contentH, h);
        float top;
        if ("top".equals(prefs.alignV)) top = 0;
        else if ("bottom".equals(prefs.alignV)) top = Math.max(0, h - contentH);
        else top = Math.max(0, (h - contentH) / 2f);
        float radius = prefs.cornerPx(dp, contentH);

        if (prefs.glassOn) drawGlass(c, w, top, contentH, radius, prefs);

        int fg = dark ? Color.rgb(18, 26, 34) : Color.rgb(248, 251, 253);
        int muted = dark ? Color.argb(170, 18, 26, 34) : Color.argb(200, 248, 251, 253);
        int upC = dark ? Color.rgb(12, 118, 104) : Color.rgb(94, 234, 212);
        int downC = dark ? Color.rgb(196, 52, 56) : Color.rgb(240, 113, 120);

        float left = pad;
        float right = w - pad;
        float extraY = Math.max(0, contentH - pad * 2 - headH - bodyH);
        float bodyTop = top + pad + headH;
        if ("center".equals(prefs.alignV)) bodyTop += extraY / 2f;
        else if ("bottom".equals(prefs.alignV)) bodyTop += extraY;

        if (ribbon) {
            Paint text = paint(Color.rgb(46, 230, 198), headerPx, "medium", dark);
            c.drawText(launch.rocketShort() + "  " + launch.shortMission(), left, top + pad + headerPx, text);
        } else if (header) {
            Paint text = paint(fg, headerPx, "medium", dark);
            c.drawText("MARKET TELEMETRY", left, top + pad + headerPx, text);
            Paint meta = paint(muted, headerPx, "regular", dark);
            meta.setTextAlign(Paint.Align.RIGHT);
            c.drawText(QuoteFetcher.sessionLabel() + "  " + QuoteFetcher.nyTime(), right, top + pad + headerPx, meta);
        }

        if (prefs.tickers.length > 4) {
            drawMarqueeStrip(c, quotes, logos, prefs, left, right, bodyTop, h,
                    fg, upC, downC, dark, dp, symbolPx, pricePx, marqueeOffset);
            return bmp;
        }

        if (wideRow) {
            float rowH = Math.max(1, contentH - pad * 2f - headH);
            float symPx = rowH * 0.30f * tickerS;
            float prcPx = rowH * 0.34f * priceS;
            float pctPxW = rowH * 0.24f * priceS;
            float logoSz = prefs.showLogos ? rowH * 0.58f : 0;
            float colW2 = (right - left) / Math.max(1, total);
            float yBase = bodyTop + rowH * 0.62f;
            for (int i = 0; i < total; i++) {
                String symbol = i < prefs.tickers.length ? prefs.tickers[i] : "\u2014";
                Quote q = quoteOf(quotes, symbol, i);
                float x = left + colW2 * i;
                if (prefs.showDividers && i > 0) {
                    Paint div = new Paint();
                    div.setColor(dark ? Color.argb(28, 18, 26, 34) : Color.argb(40, 255, 255, 255));
                    div.setStrokeWidth(dp);
                    c.drawLine(x, bodyTop + rowH * 0.1f, x, bodyTop + rowH * 0.9f, div);
                }
                Bitmap logoBmp = logos == null ? null : logos.get(symbol);
                // 컬럼 폭에 맞게 필요시만 축소 (맞춤)
                Paint mt = paint(fg, symPx, "medium", dark);
                Paint mp = paint(fg, prcPx, prefs.fontWeight, dark);
                boolean upv = q != null && q.changePct >= 0;
                Paint mc = paint(upv ? upC : downC, pctPxW, "medium", dark);
                String pTxt = formatPrice(symbol, q == null ? 0 : q.price);
                String cTxt = (upv ? "\u25B2 " : "\u25BC ") + String.format(Locale.US, "%.2f%%", q == null ? 0 : Math.abs(q.changePct));
                float gapw = 8f * dp;
                float need = (logoSz > 0 ? logoSz + gapw : 0) + mt.measureText(symbol) + gapw
                        + mp.measureText(pTxt) + (showMove && q != null && q.price > 0 ? gapw + mc.measureText(cTxt) : 0);
                float f = need > colW2 * 0.92f ? (colW2 * 0.92f) / need : 1f;
                drawWideCol(c, q, symbol, x + colW2 * 0.04f, yBase, colW2 * 0.92f, prefs, showMove,
                        fg, upC, downC, dark, dp, symPx * f, prcPx * f, pctPxW * f, logoBmp, logoSz * f);
            }
            return bmp;
        }

        float colW = (right - left) / Math.max(1, total);
        float bottomLine = top + contentH - pad * 0.45f;
        for (int i = 0; i < total; i++) {
            String symbol = i < prefs.tickers.length ? prefs.tickers[i] : "—";
            Quote q = quoteOf(quotes, symbol, i);
            float x = left + colW * i;
            if (prefs.showDividers && i > 0) {
                Paint div = new Paint();
                div.setColor(dark ? Color.argb(28, 18, 26, 34) : Color.argb(40, 255, 255, 255));
                c.drawLine(x, bodyTop, x, bottomLine, div);
            }
            Bitmap logoBmp = logos == null ? null : logos.get(symbol);
            drawCol(c, q, symbol, x + colW * 0.05f, bodyTop, colW * 0.90f, prefs, spark, showMove,
                    fg, upC, downC, dark, dp, symbolPx, pricePx, pctPx, logoBmp, logo, sparkH);
        }
        return bmp;
    }

    private static Quote quoteOf(List<Quote> quotes, String symbol, int i) {
        if (quotes == null) return null;
        for (Quote q : quotes) {
            if (q != null && symbol.equalsIgnoreCase(q.symbol)) return q;
        }
        return i < quotes.size() ? quotes.get(i) : null;
    }

    private static void drawGlass(Canvas c, int w, float top, float contentH, float radius, WidgetPrefs prefs) {
        float alpha = prefs.alpha();
        RectF box = new RectF(2, top + 2, w - 2, top + contentH - 2);
        int fill;
        switch (prefs.glassStyle) {
            case "dark":
            case "night":
                fill = Color.argb(Math.round(alpha * 255f), 10, 18, 26);
                break;
            case "tint":
                fill = Color.argb(Math.round(alpha * 255f), 186, 214, 230);
                break;
            case "edge":
                fill = Color.argb(Math.round(alpha * 255f), 255, 255, 255);
                break;
            default:
                fill = Color.argb(Math.round(alpha * 255f), 255, 255, 255);
        }
        Paint glass = new Paint(Paint.ANTI_ALIAS_FLAG);
        glass.setColor(fill);
        c.drawRoundRect(box, radius, radius, glass);

        if (!"edge".equals(prefs.glassStyle) && !"clear".equals(prefs.glassStyle)) {
            int hi = ("dark".equals(prefs.glassStyle) || "night".equals(prefs.glassStyle))
                    ? Color.argb(46, 255, 255, 255)
                    : Color.argb(Math.round(70 + alpha * 35), 255, 255, 255);
            Paint frost = new Paint(Paint.ANTI_ALIAS_FLAG);
            frost.setShader(new LinearGradient(
                    0, box.top, 0, box.top + contentH * 0.48f,
                    hi, Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP));
            c.drawRoundRect(box, radius, radius, frost);
        }

        boolean stroke = prefs.showBorder || "edge".equals(prefs.glassStyle) || "night".equals(prefs.glassStyle);
        if (stroke) {
            Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth("edge".equals(prefs.glassStyle) ? 2.2f : 1.4f);
            int lc = "night".equals(prefs.glassStyle)
                    ? Color.argb(160, 46, 230, 198)
                    : Color.argb("edge".equals(prefs.glassStyle) ? 150 : 110, 255, 255, 255);
            line.setColor(lc);
            c.drawRoundRect(box, radius, radius, line);
        }
    }

    private static void drawCol(
            Canvas c, Quote q, String symbol, float x, float y, float w, WidgetPrefs prefs,
            boolean spark, boolean showMove,
            int fg, int upC, int downC, boolean dark, float dp,
            float symbolPx, float pricePx, float pctPx, Bitmap logo, float logoSize, float sparkH) {
        boolean up = q != null && q.changePct >= 0;
        int tone = up ? upC : downC;
        String priceTxt = formatPrice(symbol, q == null ? 0 : q.price);
        String pctTxt = (up ? "▲ " : "▼ ") + String.format(Locale.US, "%.2f%%", q == null ? 0 : Math.abs(q.changePct));
        Paint t = paint(fg, symbolPx, "medium", dark);
        Paint price = paint(fg, pricePx, prefs.fontWeight, dark);
        Paint ch = paint(tone, pctPx, "medium", dark);
        float gap = 7f * dp / 3f;
        float logoW = (logo != null && prefs.showLogos) ? Math.round(logoSize) : 0;
        float row1 = (logoW > 0 ? logoW + gap : 0) + t.measureText(symbol);
        float row2 = price.measureText(priceTxt);
        float row3 = (showMove && q != null && q.price > 0) ? ch.measureText(pctTxt) : 0;
        float block = Math.max(row1, Math.max(row2, row3));
        float ox = 0;
        if ("center".equals(prefs.alignH)) ox = Math.max(0, (w - block) / 2f);
        else if ("right".equals(prefs.alignH)) ox = Math.max(0, w - block);
        float bx = x + ox;

        float textX = bx;
        if (logoW > 0) {
            Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG);
            float s = logoW;
            c.drawBitmap(logo, null, new RectF(bx, y, bx + s, y + s), bp);
            textX = bx + s + gap;
        }
        float baseline = y + Math.max(logoSize, symbolPx) * 0.76f;
        c.drawText(symbol, textX, baseline, t);

        float cursor = y + Math.max(logoSize, symbolPx) + 3f * dp;
        if (spark && q != null && q.spark != null && q.spark.length > 1) {
            drawSpark(c, q.spark, bx, cursor, w, sparkH, tone);
            cursor += sparkH + 2f * dp;
        }
        c.drawText(priceTxt, bx, cursor + pricePx, price);
        if (showMove && q != null && q.price > 0) {
            c.drawText(pctTxt, bx, cursor + pricePx + pctPx + 3f * dp, ch);
        }
    }

    private static void drawWideCol(
            Canvas c, Quote q, String symbol, float x, float yBase, float w,
            WidgetPrefs prefs, boolean showMove, int fg, int upC, int downC, boolean dark, float dp,
            float symPx, float prcPx, float pctPx, Bitmap logo, float logoSize) {
        boolean up = q != null && q.changePct >= 0;
        Paint t = paint(fg, symPx, "medium", dark);
        Paint price = paint(fg, prcPx, prefs.fontWeight, dark);
        Paint ch = paint(up ? upC : downC, pctPx, "medium", dark);
        String priceTxt = formatPrice(symbol, q == null ? 0 : q.price);
        String pctTxt = (up ? "\u25B2 " : "\u25BC ") + String.format(Locale.US, "%.2f%%", q == null ? 0 : Math.abs(q.changePct));
        float gap = 8f * dp;
        float logoW = (logo != null && prefs.showLogos) ? logoSize : 0;
        float block = logoW + (logoW > 0 ? gap : 0) + t.measureText(symbol) + gap
                + price.measureText(priceTxt)
                + (showMove && q != null && q.price > 0 ? gap + ch.measureText(pctTxt) : 0);
        float cx = x;
        if ("center".equals(prefs.alignH)) cx = x + Math.max(0, (w - block) / 2f);
        else if ("right".equals(prefs.alignH)) cx = x + Math.max(0, w - block);
        if (logoW > 0) {
            c.drawBitmap(logo, null, new RectF(cx, yBase - logoSize * 0.82f, cx + logoSize, yBase - logoSize * 0.82f + logoSize),
                    new Paint(Paint.FILTER_BITMAP_FLAG));
            cx += logoSize + gap;
        }
        c.drawText(symbol, cx, yBase, t);
        cx += t.measureText(symbol) + gap;
        c.drawText(priceTxt, cx, yBase, price);
        cx += price.measureText(priceTxt);
        if (showMove && q != null && q.price > 0) {
            c.drawText(pctTxt, cx + gap, yBase, ch);
        }
    }

    private static void drawMarqueeStrip(
            Canvas c, List<Quote> quotes, Map<String, Bitmap> logos, WidgetPrefs prefs,
            float left, float right, float y, int h,
            int fg, int upC, int downC, boolean dark, float dp,
            float symbolPx, float pricePx, float offset) {
        Paint sym = paint(fg, symbolPx, "medium", dark);
        Paint prc = paint(fg, pricePx, prefs.fontWeight, dark);
        float pctSize = Math.max(10f * dp, pricePx * 0.62f);

        float logoS = prefs.showLogos ? symbolPx * 1.2f : 0;
        float segPad = 26f * dp;
        float logoPad = 6f * dp;

        // 스트립 전체 폭 계산
        float stripW = 0;
        float[] segW = new float[prefs.tickers.length];
        for (int i = 0; i < prefs.tickers.length; i++) {
            String symbol = prefs.tickers[i];
            Quote q = quoteOf(quotes, symbol, i);
            boolean up = q != null && q.changePct >= 0;
            String priceTxt = formatPrice(symbol, q == null ? 0 : q.price);
            String pctTxt = (up ? "▲ " : "▼ ") + String.format(Locale.US, "%.2f%%", q == null ? 0 : Math.abs(q.changePct));
            Paint ch = paint(up ? upC : downC, pctSize, "medium", dark);
            float w = sym.measureText(symbol) + 8f * dp + prc.measureText(priceTxt)
                    + 8f * dp + ch.measureText(pctTxt) + segPad;
            if (logoS > 0) w += logoS + logoPad;
            segW[i] = w;
            stripW += w;
        }
        if (stripW <= 0) return;

        float baseline = y + Math.max(symbolPx, pricePx) * 0.82f;
        float x0 = left - (offset % stripW);

        for (int pass = 0; pass < 2; pass++) {
            float cx = x0 + pass * stripW;
            if (cx > right) break;
            for (int i = 0; i < prefs.tickers.length; i++) {
                String symbol = prefs.tickers[i];
                Quote q = quoteOf(quotes, symbol, i);
                boolean up = q != null && q.changePct >= 0;
                String priceTxt = formatPrice(symbol, q == null ? 0 : q.price);
                String pctTxt = (up ? "▲ " : "▼ ") + String.format(Locale.US, "%.2f%%", q == null ? 0 : Math.abs(q.changePct));
                Paint ch = paint(up ? upC : downC, pctSize, "medium", dark);
                Bitmap lg = logos == null ? null : logos.get(symbol);
                if (lg != null && prefs.showLogos) {
                    Paint bp = new Paint(Paint.FILTER_BITMAP_FLAG);
                    c.drawBitmap(lg, null, new RectF(cx, baseline - logoS * 0.8f, cx + logoS, baseline - logoS * 0.8f + logoS), bp);
                    cx += logoS + logoPad;
                }
                sym.setColor(fg);
                c.drawText(symbol, cx, baseline, sym);
                cx += sym.measureText(symbol) + 8f * dp;
                prc.setColor(fg);
                c.drawText(priceTxt, cx, baseline, prc);
                cx += prc.measureText(priceTxt) + 8f * dp;
                c.drawText(pctTxt, cx, baseline, ch);
                cx += ch.measureText(pctTxt) + segPad;
            }
        }
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

    static String formatPrice(String symbol, double price) {
        if (price <= 0) return "—";
        String s = symbol == null ? "" : symbol.toUpperCase(Locale.US);
        if (s.equals("^TNX") || s.equals("^TYX") || s.equals("^IRX") || s.equals("^FVX")) {
            return String.format(Locale.US, "%.3f%%", price);
        }
        if (s.startsWith("^")) return String.format(Locale.US, "%,.2f", price);
        return String.format(Locale.US, "$%,.2f", price);
    }

    private static void drawSpark(Canvas c, float[] values, float x, float y, float w, float h, int color) {
        float min = values[0], max = values[0];
        for (float v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float span = max - min;
        if (span == 0) span = 1;
        Path path = new Path();
        for (int i = 0; i < values.length; i++) {
            float px = x + (w * i / Math.max(1, values.length - 1));
            float py = y + h - ((values[i] - min) / span) * (h - 4) - 2;
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2.2f, h * 0.09f));
        p.setColor(color);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(path, p);
    }
}
