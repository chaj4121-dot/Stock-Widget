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
        Paint ink = new Paint(fill);
        ink.clearShadowLayer();
        float blur = Math.max(1.1f, size * 0.055f);
        int shade = light ? Color.argb(185, 0, 0, 0) : Color.argb(170, 255, 255, 255);
        ink.setShadowLayer(blur, 0f, 0f, shade);
        c.drawText(text, x, y, ink);
    }

    private static int luminance(int color) {
        return (Color.red(color) * 3 + Color.green(color) * 6 + Color.blue(color)) / 10;
    }
}
