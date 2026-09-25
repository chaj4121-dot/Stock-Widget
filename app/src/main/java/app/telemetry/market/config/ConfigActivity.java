package app.telemetry.market.config;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.telemetry.market.R;
import app.telemetry.market.quote.Quote;
import app.telemetry.market.quote.QuoteCache;
import app.telemetry.market.quote.QuoteFetcher;
import app.telemetry.market.widget.LogoCache;
import app.telemetry.market.widget.TelemetryWidgetProvider;
import app.telemetry.market.widget.WidgetPrefs;
import app.telemetry.market.widget.WidgetRenderer;

public class ConfigActivity extends AppCompatActivity {
    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private List<Quote> previewQuotes = new ArrayList<>();
    private boolean ready;
    private String previewLogoKey = "";
    private final Map<String, Bitmap> previewLogos = new HashMap<>();
    private Bitmap previewBmp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(Activity.RESULT_CANCELED);
        setContentView(R.layout.activity_config);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) appWidgetId = 0;

        WidgetPrefs prefs = WidgetPrefs.load(this, appWidgetId);
        EditText tickers = findViewById(R.id.tickers);
        tickers.setText(String.join(", ", prefs.tickers));

        ((CheckBox) findViewById(R.id.glass_on)).setChecked(prefs.glassOn);
        switch (prefs.glassStyle) {
            case "mist":
                ((RadioButton) findViewById(R.id.glass_mist)).setChecked(true);
                break;
            case "tint":
                ((RadioButton) findViewById(R.id.glass_tint)).setChecked(true);
                break;
            case "dark":
                ((RadioButton) findViewById(R.id.glass_dark)).setChecked(true);
                break;
            case "milk":
                ((RadioButton) findViewById(R.id.glass_milk)).setChecked(true);
                break;
            case "clear":
                ((RadioButton) findViewById(R.id.glass_clear)).setChecked(true);
                break;
            case "night":
                ((RadioButton) findViewById(R.id.glass_night)).setChecked(true);
                break;
            case "edge":
                ((RadioButton) findViewById(R.id.glass_edge)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.glass_frost)).setChecked(true);
        }

        switch (prefs.cornerStyle) {
            case "square":
                ((RadioButton) findViewById(R.id.corner_square)).setChecked(true);
                break;
            case "soft":
                ((RadioButton) findViewById(R.id.corner_soft)).setChecked(true);
                break;
            case "pill":
                ((RadioButton) findViewById(R.id.corner_pill)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.corner_round)).setChecked(true);
        }

        switch (prefs.fontWeight) {
            case "regular":
                ((RadioButton) findViewById(R.id.weight_regular)).setChecked(true);
                break;
            case "medium":
                ((RadioButton) findViewById(R.id.weight_medium)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.weight_bold)).setChecked(true);
        }

        SeekBar customOp = findViewById(R.id.custom_opacity);
        TextView opacityLabel = findViewById(R.id.opacity_label);
        int startOp = Math.round(prefs.alpha() * 100f);
        customOp.setProgress(Math.max(0, startOp));
        opacityLabel.setText(String.format(Locale.US, "투명도 %d%%", startOp));

        switch (prefs.size) {
            case "standard":
                ((RadioButton) findViewById(R.id.size_standard)).setChecked(true);
                break;
            case "large":
                ((RadioButton) findViewById(R.id.size_large)).setChecked(true);
                break;
            default:
                ((RadioButton) findViewById(R.id.size_compact)).setChecked(true);
        }

        if ("dark".equals(prefs.textTone)) {
            ((RadioButton) findViewById(R.id.tone_dark)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.tone_light)).setChecked(true);
        }

        SeekBar ticker = findViewById(R.id.ticker_scale);
        ticker.setProgress(Math.round((prefs.tickerScale - 0.8f) * 100f));
        TextView tickerLabel = findViewById(R.id.ticker_label);
        tickerLabel.setText(String.format(Locale.US, "티커 글씨 %d%%", Math.round(prefs.tickerScale * 100)));

        SeekBar font = findViewById(R.id.font_scale);
        font.setProgress(Math.round((prefs.fontScale - 0.8f) * 100f));
        TextView fontLabel = findViewById(R.id.font_label);
        fontLabel.setText(String.format(Locale.US, "주가 글씨 %d%%", Math.round(prefs.fontScale * 100)));

        SeekBar mSpeed = findViewById(R.id.marquee_speed);
        mSpeed.setProgress(Math.round((prefs.marqueeSpeed - 0.5f) * 100f));
        ((TextView) findViewById(R.id.marquee_label)).setText(
                String.format(Locale.US, "흐름 속도 x%.1f", prefs.marqueeSpeed));

        ((CheckBox) findViewById(R.id.header)).setChecked(prefs.showHeader);
        ((CheckBox) findViewById(R.id.logos)).setChecked(prefs.showLogos);
        ((CheckBox) findViewById(R.id.hide_move)).setChecked(prefs.hideMoveWhenClosed);
        ((CheckBox) findViewById(R.id.weekend)).setChecked(prefs.weekendMode);
        ((CheckBox) findViewById(R.id.border)).setChecked(prefs.showBorder);
        ((CheckBox) findViewById(R.id.sparklines)).setChecked(prefs.showSparklines);
        ((CheckBox) findViewById(R.id.dividers)).setChecked(prefs.showDividers);
        if ("center".equals(prefs.alignH)) ((RadioButton) findViewById(R.id.align_h_center)).setChecked(true);
        else if ("right".equals(prefs.alignH)) ((RadioButton) findViewById(R.id.align_right)).setChecked(true);
        else ((RadioButton) findViewById(R.id.align_left)).setChecked(true);
        if ("top".equals(prefs.alignV)) ((RadioButton) findViewById(R.id.align_top)).setChecked(true);
        else if ("bottom".equals(prefs.alignV)) ((RadioButton) findViewById(R.id.align_bottom)).setChecked(true);
        else ((RadioButton) findViewById(R.id.align_v_center)).setChecked(true);

        previewQuotes = QuoteCache.merge(prefs.tickers, null, QuoteCache.load(this));
        ready = true;
        bindPreviewListeners();
        refreshPreview();

        final String[] startTickers = prefs.tickers;
        new Thread(() -> {
            List<Quote> fresh = QuoteFetcher.fetch(startTickers);
            List<Quote> merged = QuoteCache.merge(startTickers, fresh, QuoteCache.load(this));
            if (!fresh.isEmpty()) QuoteCache.save(this, merged);
            previewQuotes = merged;
            runOnUiThread(this::refreshPreview);
        }).start();

        findViewById(R.id.save).setOnClickListener(v -> {
            WidgetPrefs next = collect();
            next.save(this, appWidgetId);
            WidgetPrefs.putMarqueeStart(this, appWidgetId, android.os.SystemClock.elapsedRealtime());
            if (appWidgetId != 0) {
                TelemetryWidgetProvider.updateAll(this);
                Intent result = new Intent();
                result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(Activity.RESULT_OK, result);
            }
            finish();
        });
    }

    private void bindPreviewListeners() {
        android.view.View.OnClickListener click = v -> refreshPreview();
        int[] boxes = {R.id.glass_on, R.id.header, R.id.logos, R.id.hide_move, R.id.weekend, R.id.border, R.id.sparklines, R.id.dividers};
        for (int id : boxes) ((CheckBox) findViewById(id)).setOnClickListener(click);
        int[] groups = {R.id.glass_style, R.id.glass_style2, R.id.size, R.id.tone, R.id.corner, R.id.weight, R.id.align_h, R.id.align_v};
        for (int id : groups) {
            ((RadioGroup) findViewById(id)).setOnCheckedChangeListener((g, i) -> {
                if (g.getId() == R.id.glass_style && i != -1) {
                    ((RadioGroup) findViewById(R.id.glass_style2)).clearCheck();
                }
                if (g.getId() == R.id.glass_style2 && i != -1) {
                    ((RadioGroup) findViewById(R.id.glass_style)).clearCheck();
                }
                refreshPreview();
            });
        }
        SeekBar.OnSeekBarChangeListener seek = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (seekBar.getId() == R.id.font_scale) {
                    ((TextView) findViewById(R.id.font_label))
                            .setText(String.format(Locale.US, "주가 글씨 %d%%", 80 + progress));
                } else if (seekBar.getId() == R.id.ticker_scale) {
                    ((TextView) findViewById(R.id.ticker_label))
                            .setText(String.format(Locale.US, "티커 글씨 %d%%", 80 + progress));
                } else if (seekBar.getId() == R.id.marquee_speed) {
                    ((TextView) findViewById(R.id.marquee_label))
                            .setText(String.format(Locale.US, "흐름 속도 x%.1f", 0.5f + progress / 100f));
                } else {
                    ((TextView) findViewById(R.id.opacity_label))
                            .setText(String.format(Locale.US, "투명도 %d%%", progress));
                }
                refreshPreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        ((SeekBar) findViewById(R.id.font_scale)).setOnSeekBarChangeListener(seek);
        ((SeekBar) findViewById(R.id.marquee_speed)).setOnSeekBarChangeListener(seek);
        ((SeekBar) findViewById(R.id.ticker_scale)).setOnSeekBarChangeListener(seek);
        ((SeekBar) findViewById(R.id.custom_opacity)).setOnSeekBarChangeListener(seek);
        ((EditText) findViewById(R.id.tickers)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refreshPreview(); }
        });
    }

    private WidgetPrefs collect() {
        WidgetPrefs next = new WidgetPrefs();
        next.tickers = parseTickers(((EditText) findViewById(R.id.tickers)).getText().toString());
        next.theme = "market";
        next.glassOn = ((CheckBox) findViewById(R.id.glass_on)).isChecked();
        if (((RadioButton) findViewById(R.id.glass_mist)).isChecked()) next.glassStyle = "mist";
        else if (((RadioButton) findViewById(R.id.glass_tint)).isChecked()) next.glassStyle = "tint";
        else if (((RadioButton) findViewById(R.id.glass_dark)).isChecked()) next.glassStyle = "dark";
        else if (((RadioButton) findViewById(R.id.glass_milk)).isChecked()) next.glassStyle = "milk";
        else if (((RadioButton) findViewById(R.id.glass_clear)).isChecked()) next.glassStyle = "clear";
        else if (((RadioButton) findViewById(R.id.glass_night)).isChecked()) next.glassStyle = "night";
        else if (((RadioButton) findViewById(R.id.glass_edge)).isChecked()) next.glassStyle = "edge";
        else next.glassStyle = "frost";
        if (((RadioButton) findViewById(R.id.corner_square)).isChecked()) next.cornerStyle = "square";
        else if (((RadioButton) findViewById(R.id.corner_soft)).isChecked()) next.cornerStyle = "soft";
        else if (((RadioButton) findViewById(R.id.corner_pill)).isChecked()) next.cornerStyle = "pill";
        else next.cornerStyle = "round";
        if (((RadioButton) findViewById(R.id.weight_regular)).isChecked()) next.fontWeight = "regular";
        else if (((RadioButton) findViewById(R.id.weight_medium)).isChecked()) next.fontWeight = "medium";
        else next.fontWeight = "bold";
        int op = ((SeekBar) findViewById(R.id.custom_opacity)).getProgress();
        next.customOpacity = op / 100f;
        next.opacityMode = "custom";
        if (((RadioButton) findViewById(R.id.size_standard)).isChecked()) next.size = "standard";
        else if (((RadioButton) findViewById(R.id.size_large)).isChecked()) next.size = "large";
        else next.size = "compact";
        next.textTone = ((RadioButton) findViewById(R.id.tone_dark)).isChecked() ? "dark" : "light";
        next.fontScale = 0.8f + ((SeekBar) findViewById(R.id.font_scale)).getProgress() / 100f;
        next.tickerScale = 0.8f + ((SeekBar) findViewById(R.id.ticker_scale)).getProgress() / 100f;
        next.marqueeSpeed = 0.5f + ((SeekBar) findViewById(R.id.marquee_speed)).getProgress() / 100f;
        next.showHeader = ((CheckBox) findViewById(R.id.header)).isChecked();
        next.showLogos = ((CheckBox) findViewById(R.id.logos)).isChecked();
        next.hideMoveWhenClosed = ((CheckBox) findViewById(R.id.hide_move)).isChecked();
        next.weekendMode = ((CheckBox) findViewById(R.id.weekend)).isChecked();
        next.showBorder = ((CheckBox) findViewById(R.id.border)).isChecked();
        next.showSparklines = ((CheckBox) findViewById(R.id.sparklines)).isChecked();
        next.showDividers = ((CheckBox) findViewById(R.id.dividers)).isChecked();
        if (((RadioButton) findViewById(R.id.align_h_center)).isChecked()) next.alignH = "center";
        else if (((RadioButton) findViewById(R.id.align_right)).isChecked()) next.alignH = "right";
        else next.alignH = "left";
        if (((RadioButton) findViewById(R.id.align_top)).isChecked()) next.alignV = "top";
        else if (((RadioButton) findViewById(R.id.align_bottom)).isChecked()) next.alignV = "bottom";
        else next.alignV = "center";
        next.showSlogan = false;
        return next;
    }

    private void refreshPreview() {
        if (!ready) return;
        WidgetPrefs prefs = collect();
        List<Quote> quotes = QuoteCache.merge(prefs.tickers, previewQuotes, previewQuotes);
        String logoKey = String.join(",", prefs.tickers) + (prefs.showLogos ? "|1" : "|0");
        if (!logoKey.equals(previewLogoKey)) {
            previewLogos.clear();
            if (prefs.showLogos) {
                int size = Math.max(64, Math.round(getResources().getDisplayMetrics().density * 30f));
                for (String s : prefs.tickers) previewLogos.put(s, LogoCache.get(this, s, size, false));
            }
            previewLogoKey = logoKey;
        }
        Map<String, Bitmap> logos = prefs.showLogos ? previewLogos : new HashMap<>();
        float density = getResources().getDisplayMetrics().density;
        ImageView iv = findViewById(R.id.preview);
        int w = iv.getWidth() > 80 ? iv.getWidth() : Math.max(720, Math.round(getResources().getDisplayMetrics().widthPixels * 0.92f));
        int h = Math.max(iv.getHeight(), Math.round(150 * density));
        boolean half = "compact".equals(prefs.size);
        previewBmp = WidgetRenderer.render(w, h, density, prefs, quotes, logos,
                app.telemetry.market.launch.LaunchFetcher.next(this), previewBmp, half);
        iv.setImageBitmap(previewBmp);
    }

    private String[] parseTickers(String raw) {
        String[] parts = raw.toUpperCase(Locale.US).split("[,\\s]+");
        ArrayList<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!p.startsWith("^")) p = p.replace('.', '-');
            if (!out.contains(p) && p.matches("[\\^A-Z0-9.=-]+")) out.add(p);
            if (out.size() == 10) break;
        }
        if (out.isEmpty()) out.add("META");
        return out.toArray(new String[0]);
    }
}
