package app.telemetry.market.quote;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class QuoteCache {
    private QuoteCache() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("telemetry_quotes", Context.MODE_PRIVATE);
    }

    public static void save(Context ctx, List<Quote> quotes) {
        if (quotes == null || quotes.isEmpty()) return;
        boolean any = false;
        for (Quote q : quotes) if (q.price > 0) any = true;
        if (!any) return;
        try {
            JSONArray arr = new JSONArray();
            for (Quote q : quotes) {
                if (q.price <= 0) continue;
                JSONObject o = new JSONObject();
                o.put("symbol", q.symbol);
                o.put("name", q.name);
                o.put("price", q.price);
                o.put("changePct", q.changePct);
                o.put("session", q.session);
                JSONArray spark = new JSONArray();
                if (q.spark != null) {
                    for (float v : q.spark) spark.put(v);
                }
                o.put("spark", spark);
                o.put("weekHigh", q.weekHigh);
                o.put("weekLow", q.weekLow);
                arr.put(o);
            }
            prefs(ctx).edit().putString("last", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static List<Quote> load(Context ctx) {
        List<Quote> out = new ArrayList<>();
        String raw = prefs(ctx).getString("last", null);
        if (raw == null) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                JSONArray spark = o.optJSONArray("spark");
                float[] s = new float[spark == null ? 0 : spark.length()];
                for (int j = 0; j < s.length; j++) s[j] = (float) spark.optDouble(j);
                out.add(new Quote(
                        o.optString("symbol"),
                        o.optString("name"),
                        o.optDouble("price"),
                        o.optDouble("changePct"),
                        s,
                        o.optString("session", "REGULAR"),
                        o.optDouble("weekHigh", 0),
                        o.optDouble("weekLow", 0)
                ));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static List<Quote> merge(String[] tickers, List<Quote> fresh, List<Quote> cached) {
        List<Quote> out = new ArrayList<>();
        for (String raw : tickers) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String symbol = raw.trim().toUpperCase();
            Quote hit = find(fresh, symbol);
            if (hit == null || hit.price <= 0) hit = find(cached, symbol);
            if (hit != null && hit.price > 0) out.add(hit);
            else out.add(new Quote(symbol, symbol, 0, 0, new float[0], "CLOSED"));
        }
        return out;
    }

    private static Quote find(List<Quote> list, String symbol) {
        if (list == null) return null;
        for (Quote q : list) {
            if (q.symbol.equalsIgnoreCase(symbol)) return q;
        }
        return null;
    }
}
