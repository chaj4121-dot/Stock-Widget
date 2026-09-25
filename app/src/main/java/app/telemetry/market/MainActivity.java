package app.telemetry.market;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import app.telemetry.market.config.ConfigActivity;
import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteFetcher;
import app.telemetry.market.widget.WidgetPrefs;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button open = findViewById(R.id.open_config);
        open.setOnClickListener(v -> startActivity(new Intent(this, ConfigActivity.class)));
        TextView quotesView = findViewById(R.id.quotes);
        WidgetPrefs prefs = WidgetPrefs.load(this, 0);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Quote> quotes = QuoteFetcher.fetch(prefs.tickers);
            StringBuilder sb = new StringBuilder();
            sb.append(QuoteFetcher.sessionLabel()).append("  ").append(QuoteFetcher.nyTime()).append("\n\n");
            for (Quote q : quotes) {
                sb.append(String.format(Locale.US, "%-6s  $%,10.2f   %+.2f%%\n", q.symbol, q.price, q.changePct));
            }
            if (quotes.isEmpty()) sb.append("시세를 불러오지 못했습니다.");
            runOnUiThread(() -> quotesView.setText(sb.toString()));
        });
    }
}
