package com.samu.ai;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {

    public static final String CHANNEL_SERVICE = "samu_service";
    public static final String CHANNEL_ALERTS = "samu_alerts";

    private static App instance;

    public static App get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createChannels();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel service = new NotificationChannel(CHANNEL_SERVICE,
                    getString(R.string.channel_service_name), NotificationManager.IMPORTANCE_LOW);
            service.setDescription(getString(R.string.channel_service_desc));
            service.setShowBadge(false);
            NotificationChannel alerts = new NotificationChannel(CHANNEL_ALERTS,
                    getString(R.string.channel_alerts_name), NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(service);
            nm.createNotificationChannel(alerts);
        }
    }
}
