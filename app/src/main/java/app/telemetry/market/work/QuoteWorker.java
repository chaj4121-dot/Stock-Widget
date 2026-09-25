package app.telemetry.market.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import app.telemetry.market.widget.TelemetryWidgetProvider;

public class QuoteWorker extends Worker {
    public QuoteWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        TelemetryWidgetProvider.updateAll(getApplicationContext());
        return Result.success();
    }
}
