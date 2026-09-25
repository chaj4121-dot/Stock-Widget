package app.telemetry.market.launch;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class LaunchFetcher {
    private LaunchFetcher() {}

    private static final String UA = "MarketTelemetry/1.5 (Android; personal widget)";

    public static List<Launch> fetchUpcoming() {
        try {
            String path = "https://ll.thespacedevs.com/2.2.0/launch/upcoming/?lsp__name=SpaceX&limit=5";
            HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return new ArrayList<>();
            }
            String body = read(conn);
            conn.disconnect();
            JSONArray results = new JSONObject(body).getJSONArray("results");
            List<Launch> out = new ArrayList<>();
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            iso.setTimeZone(TimeZone.getTimeZone("UTC"));
            for (int i = 0; i < results.length(); i++) {
                JSONObject r = results.getJSONObject(i);
                String net = r.optString("net", "");
                if (net.length() >= 19) net = net.substring(0, 19);
                long ms = 0;
                try {
                    ms = iso.parse(net).getTime();
                } catch (Exception ignored) {
                }
                JSONObject status = r.optJSONObject("status");
                JSONObject pad = r.optJSONObject("pad");
                JSONObject loc = pad == null ? null : pad.optJSONObject("location");
                JSONObject rocket = r.optJSONObject("rocket");
                JSONObject cfg = rocket == null ? null : rocket.optJSONObject("configuration");
                JSONObject mission = r.optJSONObject("mission");
                out.add(new Launch(
                        r.optString("name"),
                        mission == null ? r.optString("name") : mission.optString("name"),
                        cfg == null ? "" : cfg.optString("full_name", cfg.optString("name")),
                        pad == null ? "" : pad.optString("name"),
                        loc == null ? "" : loc.optString("name"),
                        status == null ? "" : status.optString("abbrev", status.optString("name")),
                        ms
                ));
            }
            return out;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static Launch next(Context ctx) {
        List<Launch> all = load(ctx);
        long now = System.currentTimeMillis() - 15 * 60 * 1000L;
        for (Launch l : all) if (l.netMs > now) return l;
        return all.isEmpty() ? null : all.get(0);
    }

    public static void refresh(Context ctx) {
        List<Launch> fresh = fetchUpcoming();
        if (!fresh.isEmpty()) save(ctx, fresh);
    }

    public static List<Launch> load(Context ctx) {
        List<Launch> out = new ArrayList<>();
        String raw = prefs(ctx).getString("launches", null);
        if (raw == null) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Launch(
                        o.optString("name"),
                        o.optString("mission"),
                        o.optString("rocket"),
                        o.optString("pad"),
                        o.optString("location"),
                        o.optString("status"),
                        o.optLong("netMs")
                ));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static void save(Context ctx, List<Launch> launches) {
        try {
            JSONArray arr = new JSONArray();
            for (Launch l : launches) {
                JSONObject o = new JSONObject();
                o.put("name", l.name);
                o.put("mission", l.mission);
                o.put("rocket", l.rocket);
                o.put("pad", l.pad);
                o.put("location", l.location);
                o.put("status", l.status);
                o.put("netMs", l.netMs);
                arr.put(o);
            }
            prefs(ctx).edit().putString("launches", arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("telemetry_launch", Context.MODE_PRIVATE);
    }

    private static String read(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
