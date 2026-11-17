package com.samu.ai.util;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

public class PermissionUtils {

    public static boolean granted(Context ctx, String perm) {
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean mic(Context c) { return granted(c, Manifest.permission.RECORD_AUDIO); }

    public static boolean camera(Context c) { return granted(c, Manifest.permission.CAMERA); }

    public static boolean call(Context c) { return granted(c, Manifest.permission.CALL_PHONE); }

    public static boolean contacts(Context c) { return granted(c, Manifest.permission.READ_CONTACTS); }

    public static boolean sms(Context c) { return granted(c, Manifest.permission.SEND_SMS); }

    public static boolean gallery(Context c) {
        if (Build.VERSION.SDK_INT >= 33) return granted(c, Manifest.permission.READ_MEDIA_IMAGES);
        return granted(c, Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    public static boolean location(Context c) {
        return granted(c, Manifest.permission.ACCESS_FINE_LOCATION)
                || granted(c, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    public static boolean backgroundLocation(Context c) {
        if (Build.VERSION.SDK_INT < 29) return location(c);
        return granted(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    public static boolean notifications(Context c) {
        if (Build.VERSION.SDK_INT >= 33) return granted(c, Manifest.permission.POST_NOTIFICATIONS);
        return true;
    }

    public static boolean overlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    public static boolean accessibility(Context c) {
        String expected = c.getPackageName() + "/" + "com.samu.ai.service.SamuAccessibilityService";
        String enabled = Settings.Secure.getString(c.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(expected);
    }

    public static boolean batteryIgnored(Context c) {
        android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(c.getPackageName());
    }

    public static boolean deviceAdmin(Context c) {
        android.app.admin.DevicePolicyManager dpm =
                (android.app.admin.DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm != null && dpm.isAdminActive(new android.content.ComponentName(c,
                com.samu.ai.admin.SamuDeviceAdminReceiver.class));
    }

    public static boolean dndAccess(Context c) {
        android.app.NotificationManager nm =
                (android.app.NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static boolean usageStats(Context c) {
        AppOpsManager ops = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
        if (ops == null) return false;
        try {
            int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), c.getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean writeSettings(Context c) {
        return Settings.System.canWrite(c);
    }

    public static boolean exactAlarm(Context c) {
        if (Build.VERSION.SDK_INT >= 31) {
            android.app.AlarmManager am = (android.app.AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            return am != null && am.canScheduleExactAlarms();
        }
        return true;
    }

    public static boolean bluetooth(Context c) {
        if (Build.VERSION.SDK_INT >= 31) return granted(c, Manifest.permission.BLUETOOTH_CONNECT);
        return granted(c, Manifest.permission.BLUETOOTH);
    }
}
