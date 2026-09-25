package app.telemetry.market.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class LogoCache {
    private LogoCache() {}

    public static Bitmap get(Context ctx, String symbol, int sizePx) {
        return get(ctx, symbol, sizePx, true);
    }

    public static Bitmap get(Context ctx, String symbol, int sizePx, boolean network) {
        File dir = new File(ctx.getCacheDir(), "logos");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, symbol + ".png");
        Bitmap raw = null;
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < 7L * 24 * 3600 * 1000) {
            raw = BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        if (raw == null && network) {
            raw = download(symbol, file);
        }
        if (raw == null) return letter(symbol, sizePx);
        return circle(raw, sizePx);
    }

    private static Bitmap download(String symbol, File dest) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://financialmodelingprep.com/image-stock/" + symbol + ".png");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "MarketTelemetry/1.2");
            if (conn.getResponseCode() != 200) return null;
            try (InputStream in = conn.getInputStream()) {
                Bitmap bmp = BitmapFactory.decodeStream(in);
                if (bmp == null) return null;
                try (FileOutputStream out = new FileOutputStream(dest)) {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                }
                return bmp;
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static Bitmap circle(Bitmap src, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float scale = Math.max(size / (float) src.getWidth(), size / (float) src.getHeight());
        Matrix m = new Matrix();
        m.setScale(scale, scale);
        m.postTranslate((size - src.getWidth() * scale) / 2f, (size - src.getHeight() * scale) / 2f);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shader.setLocalMatrix(m);
        p.setShader(shader);
        c.drawCircle(size / 2f, size / 2f, size / 2f - 1, p);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setColor(Color.argb(36, 255, 255, 255));
        ring.setStrokeWidth(1.2f);
        c.drawCircle(size / 2f, size / 2f, size / 2f - 1.2f, ring);
        return out;
    }

    private static Bitmap letter(String symbol, int size) {
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        bg.setColor(tint(symbol));
        c.drawCircle(size / 2f, size / 2f, size / 2f, bg);
        Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
        t.setColor(Color.WHITE);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setTextAlign(Paint.Align.CENTER);
        t.setTextSize(size * 0.48f);
        String mark = symbol.startsWith("^") && symbol.length() > 1 ? symbol.substring(1, 2) : symbol.substring(0, 1);
        c.drawText(mark, size / 2f, size * 0.68f, t);
        return out;
    }

    private static int tint(String symbol) {
        switch (symbol) {
            case "GOOGL":
            case "GOOG":
                return Color.rgb(66, 133, 244);
            case "META":
                return Color.rgb(6, 104, 225);
            case "AMZN":
                return Color.rgb(255, 153, 0);
            case "UNH":
                return Color.rgb(0, 38, 119);
            case "AAPL":
                return Color.rgb(162, 170, 173);
            case "MSFT":
                return Color.rgb(0, 164, 239);
            case "NVDA":
                return Color.rgb(118, 185, 0);
            case "TSLA":
                return Color.rgb(232, 33, 39);
            case "NFLX":
                return Color.rgb(229, 9, 20);
            default:
                return Color.rgb(100, 116, 139);
        }
    }
}
