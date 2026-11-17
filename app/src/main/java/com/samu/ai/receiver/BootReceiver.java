package com.samu.ai.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.samu.ai.SettingsStore;
import com.samu.ai.service.FloatingIconService;
import com.samu.ai.service.SamuForegroundService;

/**
 * Restarts Samu automatically after phone restart or app update.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action)
                || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        if (!boot) return;

        if (SettingsStore.get(context).isServiceWanted()) {
            SamuForegroundService.start(context);
        }
        FloatingIconService.start(context);
    }
}
