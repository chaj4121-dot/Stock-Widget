package app.telemetry.market.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * A tight halo around glyphs so light type stays readable on a bright photo
 * and dark type stays readable on the rocket, without a bar over the wallpaper.
 */
public final class TextInk {
    private TextInk() {}

    public static void draw(Canvas c, String text, float x, float y, Paint fill) {
        if (c == null || text == null || text.isEmpty() || fill == null) return;
        float size = Math.max(1f, fill.getTextSize());
        boolean light = luminance(fill.getColor()) >= 148;

        Paint soft = new Paint(fill);
        soft.setStyle(Paint.Style.STROKE);
        soft.setStrokeJoin(Paint.Join.ROUND);
        soft.setStrokeWidth(Math.max(2f, size * 0.20f));
        soft.clearShadowLayer();
        soft.setColor(light ? Color.argb(110, 0, 0, 0) : Color.argb(140, 255, 255, 255));
        c.drawText(text, x, y, soft);

        Paint edge = new Paint(fill);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeJoin(Paint.Join.ROUND);
        edge.setStrokeWidth(Math.max(1.5f, size * 0.09f));
        edge.clearShadowLayer();
        edge.setColor(light ? Color.argb(225, 8, 10, 14) : Color.argb(230, 255, 255, 255));
        c.drawText(text, x, y, edge);
        c.drawText(text, x, y, fill);
    }

    private static int luminance(int color) {
        return (Color.red(color) * 3 + Color.green(color) * 6 + Color.blue(color)) / 10;
    }
}
