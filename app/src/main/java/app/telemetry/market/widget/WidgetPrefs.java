package app.telemetry.market.widget;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

public final class WidgetPrefs {
    public String[] tickers = new String[]{"GOOGL", "META", "AMZN", "UNH"};
    public String opacityMode = "translucent";
    public float customOpacity = 0.30f;
    public String theme = "market";
    public String size = "compact";
    public float fontScale = 1f;
    public float tickerScale = 1.25f;
    public boolean showSparklines = true;
    public boolean showSlogan = false;
    public boolean showBorder = false;
    public boolean showLogos = true;
    public boolean showHeader = false;
    public boolean glassOn = true;
    public String glassStyle = "frost";
    public String cornerStyle = "round";
    public String fontWeight = "bold";
    public boolean hideMoveWhenClosed = true;
    public boolean weekendMode = true;
    public String textTone = "light";
    public boolean showDividers = true;
    public float marqueeSpeed = 1f;
    public String alignH = "left";
    public String alignV = "center";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("telemetry_widget", Context.MODE_PRIVATE);
    }

    public static WidgetPrefs load(Context ctx, int appWidgetId) {
        WidgetPrefs p = new WidgetPrefs();
        String raw = prefs(ctx).getString(key(appWidgetId), prefs(ctx).getString("default", null));
        if (raw == null) return p;
        try {
            JSONObject o = new JSONObject(raw);
            JSONArray t = o.optJSONArray("tickers");
            if (t != null && t.length() > 0) {
                String[] arr = new String[Math.min(10, t.length())];
                for (int i = 0; i < arr.length; i++) arr[i] = t.getString(i);
                p.tickers = arr;
            }
            p.opacityMode = o.optString("opacityMode", p.opacityMode);
            p.customOpacity = (float) o.optDouble("customOpacity", p.customOpacity);
            p.theme = o.optString("theme", p.theme);
            p.size = o.optString("size", p.size);
            p.fontScale = (float) o.optDouble("fontScale", p.fontScale);
            p.tickerScale = (float) o.optDouble("tickerScale", Math.max(1.2, p.fontScale));
            p.showSparklines = o.optBoolean("showSparklines", true);
            p.showSlogan = o.optBoolean("showSlogan", false);
            p.showBorder = o.optBoolean("showBorder", false);
            p.showLogos = o.optBoolean("showLogos", true);
            p.showHeader = o.optBoolean("showHeader", false);
            p.glassOn = o.optBoolean("glassOn", true);
            p.glassStyle = o.optString("glassStyle", "frost");
            p.cornerStyle = o.optString("cornerStyle", "round");
            p.fontWeight = o.optString("fontWeight", "bold");
            p.hideMoveWhenClosed = o.optBoolean("hideMoveWhenClosed", true);
            p.weekendMode = o.optBoolean("weekendMode", true);
            p.textTone = o.optString("textTone", "light");
            p.showDividers = o.optBoolean("showDividers", true);
            p.alignH = o.optString("alignH", "left");
            p.alignV = o.optString("alignV", "center");
            p.marqueeSpeed = (float) o.optDouble("marqueeSpeed", o.optDouble("flowSpeed", 200) / 160.0);
        } catch (Exception ignored) {
        }
        return p;
    }

    public void save(Context ctx, int appWidgetId) {
        try {
            JSONObject o = new JSONObject();
            JSONArray t = new JSONArray();
            for (String s : tickers) t.put(s);
            o.put("tickers", t);
            o.put("opacityMode", opacityMode);
            o.put("customOpacity", customOpacity);
            o.put("theme", theme);
            o.put("size", size);
            o.put("fontScale", fontScale);
            o.put("tickerScale", tickerScale);
            o.put("showSparklines", showSparklines);
            o.put("showSlogan", showSlogan);
            o.put("showBorder", showBorder);
            o.put("showLogos", showLogos);
            o.put("showHeader", showHeader);
            o.put("glassOn", glassOn);
            o.put("glassStyle", glassStyle);
            o.put("cornerStyle", cornerStyle);
            o.put("fontWeight", fontWeight);
            o.put("hideMoveWhenClosed", hideMoveWhenClosed);
            o.put("weekendMode", weekendMode);
            o.put("textTone", textTone);
            o.put("showDividers", showDividers);
            o.put("alignH", alignH);
            o.put("alignV", alignV);
            o.put("marqueeSpeed", marqueeSpeed);
            String json = o.toString();
            SharedPreferences.Editor ed = prefs(ctx).edit();
            ed.putString(key(appWidgetId), json);
            ed.putString("default", json);
            ed.apply();
        } catch (Exception ignored) {
        }
    }

    public static void delete(Context ctx, int appWidgetId) {
        prefs(ctx).edit()
                .remove(key(appWidgetId))
                .remove("story_" + appWidgetId)
                .apply();
    }

    public static float offsetOf(Context ctx, int appWidgetId) {
        return prefs(ctx).getFloat("off_" + appWidgetId, 0f);
    }

    public static void putOffset(Context ctx, int appWidgetId, float offset) {
        prefs(ctx).edit().putFloat("off_" + appWidgetId, offset).apply();
    }

    public static long marqueeStart(Context ctx, int appWidgetId) {
        return prefs(ctx).getLong("mq_s_" + appWidgetId, 0L);
    }

    public static void putMarqueeStart(Context ctx, int appWidgetId, long at) {
        prefs(ctx).edit().putLong("mq_s_" + appWidgetId, at).apply();
    }

    public static long lastMarqueeAt(Context ctx, int appWidgetId) {
        return prefs(ctx).getLong("off_t_" + appWidgetId, 0L);
    }

    public static void putMarqueeAt(Context ctx, int appWidgetId, long at) {
        prefs(ctx).edit().putLong("off_t_" + appWidgetId, at).apply();
    }

    private static String key(int id) {
        return "w_" + id;
    }

    public static void saveSize(Context ctx, int appWidgetId, int w, int h, float density) {
        prefs(ctx).edit()
                .putInt("sz_w_" + appWidgetId, w)
                .putInt("sz_h_" + appWidgetId, h)
                .putInt("sz_d_" + appWidgetId, Float.floatToIntBits(density))
                .apply();
    }

    public static int[] sizeOf(Context ctx, int appWidgetId) {
        return new int[]{
                prefs(ctx).getInt("sz_w_" + appWidgetId, 720),
                prefs(ctx).getInt("sz_h_" + appWidgetId, 180),
                prefs(ctx).getInt("sz_d_" + appWidgetId, Float.floatToIntBits(3f))
        };
    }

    public float alpha() {
        if (!glassOn) return 0f;
        switch (opacityMode) {
            case "transparent":
                return 0.05f;
            case "solid":
                return 0.82f;
            case "custom":
                return Math.max(0f, Math.min(0.9f, customOpacity));
            default:
                switch (glassStyle) {
                    case "mist":
                        return 0.16f;
                    case "tint":
                        return 0.38f;
                    case "dark":
                        return 0.42f;
                    case "milk":
                        return 0.52f;
                    case "clear":
                        return 0.08f;
                    case "night":
                        return 0.55f;
                    case "edge":
                        return 0.04f;
                    default:
                        return 0.30f;
                }
        }
    }

    public float cornerPx(float density, float contentH) {
        switch (cornerStyle) {
            case "square":
                return 4f * density;
            case "soft":
                return 12f * density;
            case "pill":
                return contentH / 2f;
            default:
                return 22f * density;
        }
    }

    public boolean darkText() {
        return !"light".equals(textTone);
    }
}
