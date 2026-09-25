package app.telemetry.market;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import app.telemetry.market.work.MarqueeScheduler;
import app.telemetry.market.work.QuoteScheduler;

public class TelemetryApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        QuoteScheduler.ensure(this);

        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver rcv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent i) {
                if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) {
                    if (MarqueeScheduler.anyActive(ctx)) MarqueeScheduler.start(ctx);
                } else {
                    MarqueeScheduler.stop(ctx);
                }
            }
        };
        // Android 14+: 런타임 리시버 등록 시 export 플래그 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(rcv, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(rcv, f);
        }

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && pm.isInteractive() && MarqueeScheduler.anyActive(this)) {
            MarqueeScheduler.start(this);
        }
    }
}
