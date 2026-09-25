package app.telemetry.market.quote;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class QuoteFetcher {
    private QuoteFetcher() {}

    private static final String UA =
            "Mozilla/5.0 (Linux; Android 15; SM-S938B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";

    public static List<Quote> fetch(String[] symbols) {
        List<Quote> out = fetchOnce(symbols, "https://query1.finance.yahoo.com", "1m", "1d");
        if (!enough(out, symbols)) {
            sleep(350);
            out = fetchOnce(symbols, "https://query2.finance.yahoo.com", "1m", "1d");
        }
        if (!enough(out, symbols)) {
            sleep(350);
            out = fetchOnce(symbols, "https://query1.finance.yahoo.com", "5m", "5d");
        }
        return out;
    }

    private static boolean enough(List<Quote> out, String[] symbols) {
        int want = 0;
        for (String s : symbols) if (s != null && !s.trim().isEmpty()) want++;
        if (want == 0) return true;
        int ok = 0;
        for (Quote q : out) if (q.price > 0) ok++;
        return ok >= Math.min(want, 1) && ok >= want / 2;
    }

    private static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private static List<Quote> fetchOnce(String[] symbols, String host, String interval, String range) {
        List<Quote> out = new ArrayList<>();
        for (String raw : symbols) {
            if (raw == null) continue;
            String symbol = raw.trim().toUpperCase(java.util.Locale.US);
            if (!symbol.startsWith("^")) symbol = symbol.replace('.', '-');
            if (symbol.isEmpty()) continue;
            try {
                Quote q = fetchOne(symbol, host, interval, range);
                if (q != null && q.price > 0) out.add(q);
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private static Quote fetchOne(String symbol, String host, String interval, String range) throws Exception {
        String path = host + "/v8/finance/chart/"
                + java.net.URLEncoder.encode(symbol, "UTF-8")
                + "?interval=" + interval + "&range=" + range + "&includePrePost=true";
        HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();
        conn.setRequestProperty("User-Agent", UA);
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return null;
        }
        String body = read(conn);
        conn.disconnect();
        JSONObject result = new JSONObject(body)
                .getJSONObject("chart")
                .getJSONArray("result")
                .getJSONObject(0);
        JSONObject meta = result.getJSONObject("meta");
        double regular = meta.optDouble("regularMarketPrice", 0);
        if (regular <= 0) regular = meta.optDouble("chartPreviousClose", 0);
        if (regular <= 0) return null;
        double prev = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", regular));
        JSONArray ts = result.optJSONArray("timestamp");
        JSONArray close = null;
        try {
            close = result.getJSONObject("indicators")
                    .getJSONArray("quote")
                    .getJSONObject(0)
                    .getJSONArray("close");
        } catch (Exception ignored) {
        }
        JSONObject periods = meta.optJSONObject("currentTradingPeriod");
        Double pre = lastIn(ts, close, period(periods, "pre"));
        Double post = lastIn(ts, close, period(periods, "post"));
        long[] regularP = period(periods, "regular");
        long[] preP = period(periods, "pre");
        long[] postP = period(periods, "post");
        long now = System.currentTimeMillis() / 1000L;

        String session = "REGULAR";
        if (in(now, preP)) session = "PRE";
        else if (in(now, regularP)) session = "REGULAR";
        else if (in(now, postP)) session = "POST";
        else if (post != null) session = "POST";
        else if (pre != null && regularP != null && now < regularP[0]) session = "PRE";

        double price = regular;
        if ("PRE".equals(session) && pre != null && pre > 0) price = pre;
        if ("POST".equals(session) && post != null && post > 0) price = post;
        if (price <= 0) return null;
        double pct = prev == 0 ? 0 : (price - prev) / prev * 100.0;
        if ("REGULAR".equals(session) && meta.has("regularMarketChangePercent")) {
            pct = meta.getDouble("regularMarketChangePercent");
        }
        return new Quote(
                meta.optString("symbol", symbol),
                meta.optString("shortName", symbol),
                price,
                pct,
                downsample(close),
                session,
                meta.optDouble("fiftyTwoWeekHigh", 0),
                meta.optDouble("fiftyTwoWeekLow", 0)
        );
    }

    private static long[] period(JSONObject periods, String key) {
        if (periods == null || !periods.has(key)) return null;
        JSONObject p = periods.optJSONObject(key);
        if (p == null) return null;
        return new long[]{p.optLong("start"), p.optLong("end")};
    }

    private static boolean in(long now, long[] p) {
        return p != null && now >= p[0] && now < p[1];
    }

    private static Double lastIn(JSONArray ts, JSONArray close, long[] p) {
        if (ts == null || close == null || p == null) return null;
        Double last = null;
        int n = Math.min(ts.length(), close.length());
        for (int i = 0; i < n; i++) {
            if (close.isNull(i)) continue;
            long t = ts.optLong(i);
            double v = close.optDouble(i);
            if (v <= 0) continue;
            if (t >= p[0] && t < p[1]) last = v;
        }
        return last;
    }

    private static float[] downsample(JSONArray arr) {
        if (arr == null) return new float[0];
        ArrayList<Float> nums = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            if (arr.isNull(i)) continue;
            float v = (float) arr.optDouble(i);
            if (v > 0) nums.add(v);
        }
        int n = nums.size();
        if (n == 0) return new float[0];
        if (n <= 48) {
            float[] out = new float[n];
            for (int i = 0; i < n; i++) out[i] = nums.get(i);
            return out;
        }
        float[] out = new float[48];
        double step = (n - 1) / 47.0;
        for (int i = 0; i < 48; i++) out[i] = nums.get((int) Math.round(i * step));
        return out;
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

    public static boolean isClosedSession() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return true;
        int mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        return mins < 4 * 60 || mins >= 20 * 60;
    }

    public static String sessionLabel() {
        if (isClosedSession()) {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
            int day = cal.get(Calendar.DAY_OF_WEEK);
            if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) return "CLOSED";
            return "CLOSED";
        }
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        int mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
        if (mins >= 9 * 60 + 30 && mins < 16 * 60) return "OPEN";
        if (mins >= 4 * 60 && mins < 9 * 60 + 30) return "PRE-MKT";
        if (mins >= 16 * 60 && mins < 20 * 60) return "AFTER-HRS";
        return "CLOSED";
    }

    public static String nyTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        return String.format("%02d:%02d EDT", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }
}
