package app.telemetry.market.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Optional hairline around glyphs. Off draws the fill alone.
 */
public final class TextInk {
    private static final ThreadLocal<Boolean> ON = new ThreadLocal<>();

    private TextInk() {}

    public static void use(boolean on) {
        ON.set(on);
    }

    public static void draw(Canvas c, String text, float x, float y, Paint fill) {
        if (c == null || text == null || text.isEmpty() || fill == null) return;
        if (!Boolean.TRUE.equals(ON.get())) {
            c.drawText(text, x, y, fill);
            return;
        }
        float size = Math.max(1f, fill.getTextSize());
        boolean light = luminance(fill.getColor()) >= 148;
        Paint edge = new Paint(fill);
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeJoin(Paint.Join.ROUND);
        edge.setStrokeWidth(Math.max(0.8f, size * 0.035f));
        edge.clearShadowLayer();
        edge.setColor(light ? Color.argb(210, 8, 10, 14) : Color.argb(210, 255, 255, 255));
        c.drawText(text, x, y, edge);
        c.drawText(text, x, y, fill);
    }

    private static int luminance(int color) {
        return (Color.red(color) * 3 + Color.green(color) * 6 + Color.blue(color)) / 10;
    }
}
