package app.telemetry.market.launch;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;

import app.telemetry.market.R;
import app.telemetry.market.config.ConfigActivity;
import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteCache;
import app.telemetry.market.widget.WidgetPrefs;

public class WeekendActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Launch next;
    private int appWidgetId;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (next != null) {
                ((TextView) findViewById(R.id.countdown)).setText(Launch.countdownFull(next.netMs));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekend);
        appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 0);

        next = LaunchFetcher.next(this);
        bindLaunch(next);
        bindTickers();
        bindUpcoming(LaunchFetcher.load(this));

        findViewById(R.id.settings).setOnClickListener(v -> {
            Intent cfg = new Intent(this, ConfigActivity.class);
            cfg.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivity(cfg);
        });

        new Thread(() -> {
            LaunchFetcher.refresh(this);
            Launch fresh = LaunchFetcher.next(this);
            List<Launch> all = LaunchFetcher.load(this);
            runOnUiThread(() -> {
                next = fresh;
                bindLaunch(fresh);
                bindUpcoming(all);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        super.onPause();
    }

    private void bindLaunch(Launch launch) {
        if (launch == null) {
            ((TextView) findViewById(R.id.mission)).setText("일정 없음");
            ((TextView) findViewById(R.id.rocket)).setText("SpaceX");
            ((TextView) findViewById(R.id.countdown)).setText("--:--:--");
            ((TextView) findViewById(R.id.status)).setText("NET");
            return;
        }
        ((TextView) findViewById(R.id.mission)).setText(launch.shortMission());
        String where = launch.rocket;
        if (!launch.location.isEmpty()) where += "  ·  " + launch.location;
        ((TextView) findViewById(R.id.rocket)).setText(where);
        ((TextView) findViewById(R.id.countdown)).setText(Launch.countdownFull(launch.netMs));
        ((TextView) findViewById(R.id.status)).setText(
                (launch.isGo() ? "GO  " : launch.status.toUpperCase(Locale.US) + "  ") + Launch.countdown(launch.netMs));
    }

    private void bindTickers() {
        WidgetPrefs prefs = WidgetPrefs.load(this, appWidgetId);
        List<Quote> quotes = QuoteCache.merge(prefs.tickers, null, QuoteCache.load(this));
        StringBuilder sb = new StringBuilder();
        for (Quote q : quotes) {
            sb.append(String.format(Locale.US, "%-6s  $%,10.2f", q.symbol, q.price));
            if (Launch.isLaunchTicker(q.symbol) && next != null) {
                sb.append("   ").append(Launch.countdown(next.netMs));
            } else if (q.weekPct() >= 0) {
                sb.append(String.format(Locale.US, "   52w %3d%%", q.weekPct()));
            }
            sb.append('\n');
        }
        ((TextView) findViewById(R.id.tickers)).setText(sb.toString().trim());
    }

    private void bindUpcoming(List<Launch> all) {
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (Launch l : all) {
            if (next != null && l.netMs == next.netMs) continue;
            sb.append(l.rocketShort()).append("  ").append(l.shortMission()).append('\n');
            sb.append("   ").append(Launch.countdown(l.netMs));
            if (!l.location.isEmpty()) sb.append("  ·  ").append(l.location);
            sb.append("\n\n");
            if (++n == 3) break;
        }
        ((TextView) findViewById(R.id.upcoming)).setText(sb.toString().trim());
    }
}
