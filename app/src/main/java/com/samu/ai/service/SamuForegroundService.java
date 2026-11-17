package com.samu.ai.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.samu.ai.App;
import com.samu.ai.MainActivity;
import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.ai.SamuBrain;
import com.samu.ai.face.FaceVerifier;
import com.samu.ai.speech.WakeWordListener;
import com.samu.ai.util.PermissionUtils;
import com.samu.ai.util.SamuAvatarBridge;
import com.samu.ai.util.TtsManager;

/**
 * The heart of Samu AI: a foreground service that keeps listening for the wake
 * words around the clock, sleeps to low power when the screen is off and to
 * sleep mode under 15% battery.
 */
public class SamuForegroundService extends LifecycleService implements WakeWordListener.Listener {

    public static final String ACTION_START = "com.samu.ai.START_SERVICE";
    public static final String ACTION_STOP = "com.samu.ai.STOP_SERVICE";

    private static volatile boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    private WakeWordListener wakeWord;
    private SamuBrain brain;
    private PowerManager.WakeLock wakeLock;
    private BatteryReceiver batteryReceiver;

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, SamuForegroundService.class).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void stop(Context ctx) {
        ctx.startService(new Intent(ctx, SamuForegroundService.class).setAction(ACTION_STOP));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        brain = new SamuBrain(this, TtsManager.get(this)::speak);
        wakeWord = new WakeWordListener(this, this);
        startForegroundInternal();
        acquireWakeLock();
        registerBattery();
        TtsManager.get(this).whenReady(() -> wakeWord.start());
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            SettingsStore.get(this).setServiceWanted(false);
            stopSelf();
            return START_NOT_STICKY;
        }
        SettingsStore.get(this).setServiceWanted(true);
        if (wakeWord != null && !PermissionUtils.mic(this)) {
            SamuAvatarBridge.setStatus(getString(R.string.status_need_mic));
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (wakeWord != null) wakeWord.stop();
        releaseWakeLock();
        unregisterBattery();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private void startForegroundInternal() {
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification n = new NotificationCompat.Builder(this, App.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_stat_samu)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setOngoing(true)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1001, n,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1001, n);
        }
    }

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SamuAI::listener");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }

    private void registerBattery() {
        batteryReceiver = new BatteryReceiver();
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, f);
        IntentFilter screen = new IntentFilter();
        screen.addAction(Intent.ACTION_SCREEN_ON);
        screen.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, screen);
    }

    private void unregisterBattery() {
        try {
            unregisterReceiver(batteryReceiver);
            unregisterReceiver(screenReceiver);
        } catch (Exception ignored) { }
    }

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean screenOff = Intent.ACTION_SCREEN_OFF.equals(intent.getAction());
            if (wakeWord != null) wakeWord.setLowPower(screenOff);
        }
    };

    private class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level < 0 || scale <= 0) return;
            int pct = Math.round(level * 100f / scale);
            boolean charging = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    == BatteryManager.BATTERY_STATUS_CHARGING;
            boolean sleep = pct < 15 && !charging;
            if (sleep != SettingsStore.get(SamuForegroundService.this).isSleepMode()) {
                SettingsStore.get(SamuForegroundService.this).setSleepMode(sleep);
                if (wakeWord != null) wakeWord.setLowPower(sleep || isScreenOff);
                SamuAvatarBridge.setStatus(sleep
                        ? getString(R.string.status_sleep_mode) : getString(R.string.status_active));
            }
        }
    }

    private boolean isScreenOff = false;

    // ---- Wake word heard ----
    @Override
    public void onHeard(String afterWakeText) {
        String special = SettingsStore.get(this).getSpecialName().toLowerCase();
        String norm = afterWakeText == null ? "" : afterWakeText.toLowerCase();

        if (norm.contains(special)) {
            // Full control mode: verify the boss face first.
            SamuAvatarBridge.setStatus(getString(R.string.status_checking_boss));
            FaceVerifier.verifyBoss(this, isBoss -> {
                if (isBoss) {
                    String cmd = norm.replace(special, "").trim();
                    if (cmd.isEmpty()) {
                        TtsManager.get(this).speak(getString(R.string.boss_verified));
                    } else {
                        brain.think(SamuBrain.Trigger.SPECIAL_NAME, cmd);
                    }
                } else {
                    TtsManager.get(this).speak(getString(R.string.refuse_not_boss));
                }
                SamuAvatarBridge.setStatus(getString(R.string.status_active));
            });
            return;
        }

        // Plain "Hey Samu" => chat only.
        String cmd = afterWakeText == null ? "" : afterWakeText.trim();
        brain.think(SamuBrain.Trigger.HEY_SAMU, cmd);
    }
}
