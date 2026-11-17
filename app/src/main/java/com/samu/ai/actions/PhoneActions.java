package com.samu.ai.actions;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.samu.ai.R;
import com.samu.ai.service.SamuAccessibilityService;
import com.samu.ai.speech.CommandParser;

import java.util.Locale;

/**
 * Every physical phone action Samu can perform. Bengali + English friendly.
 */
public class PhoneActions {

    public interface Talker {
        void say(String text);
    }

    private final Context ctx;
    private final Talker talker;

    public PhoneActions(Context ctx, Talker talker) {
        this.ctx = ctx.getApplicationContext();
        this.talker = talker;
    }

    public String run(CommandParser.Type type, String arg) {
        switch (type) {
            case CALL: return doCall(arg);
            case SMS: return doSms(arg);
            case OPEN_APP: return doOpenApp(arg);
            case CLOSE_APP: return doCloseApp();
            case ALARM: return doAlarm(arg, false);
            case TIMER: return doAlarm(arg, true);
            case FLASHLIGHT_ON: return doTorch(true);
            case FLASHLIGHT_OFF: return doTorch(false);
            case WIFI_ON: return doWifi(true);
            case WIFI_OFF: return doWifi(false);
            case BT_ON: return doBluetooth(true);
            case BT_OFF: return doBluetooth(false);
            case MUSIC_PLAY: return doMedia(true);
            case MUSIC_PAUSE: return doMedia(false);
            case VOLUME_UP: return doVolume(1);
            case VOLUME_DOWN: return doVolume(-1);
            case VOLUME_MUTE: return doMute();
            case BRIGHTNESS_UP: return doBrightness(1);
            case BRIGHTNESS_DOWN: return doBrightness(-1);
            case SCREENSHOT: return doScreenshot();
            case LOCK_SCREEN: return doLockScreen();
            case DND_ON: return doDnd(true);
            case DND_OFF: return doDnd(false);
            case SHOW_GALLERY: return doOpenApp("gallery");
            case TAKE_PHOTO: return doOpenApp("camera");
            default: return ctx.getString(R.string.cmd_unknown);
        }
    }

    // ---- Call ----
    private String doCall(String who) {
        String number = ContactLookup.resolve(ctx, who);
        if (number == null || number.isEmpty()) return ctx.getString(R.string.cmd_contact_not_found);
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            openDialer(number);
            return ctx.getString(R.string.cmd_call_dialer);
        }
        try {
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return String.format(Locale.getDefault(), ctx.getString(R.string.cmd_calling), number);
        } catch (Exception e) {
            openDialer(number);
            return ctx.getString(R.string.cmd_call_dialer);
        }
    }

    private void openDialer(String number) {
        Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    // ---- SMS ----
    private String doSms(String text) {
        String number = ContactLookup.resolve(ctx, text);
        if (number == null || number.isEmpty()) {
            Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return ctx.getString(R.string.cmd_sms_open);
        }
        try {
            SmsManager sm = Build.VERSION.SDK_INT >= 31
                    ? ctx.getSystemService(SmsManager.class) : SmsManager.getDefault();
            if (sm != null) sm.sendTextMessage(number, null, text, null, null);
            return ctx.getString(R.string.cmd_sms_sent);
        } catch (Exception e) {
            return ctx.getString(R.string.cmd_sms_failed);
        }
    }

    // ---- Apps ----
    private String doOpenApp(String name) {
        String pkg = AppLookup.findPackage(ctx, name);
        if (pkg == null) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return String.format(Locale.getDefault(), ctx.getString(R.string.cmd_app_not_found), name);
        }
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return String.format(Locale.getDefault(), ctx.getString(R.string.cmd_app_opened), name);
            }
        } catch (Exception ignored) { }
        return String.format(Locale.getDefault(), ctx.getString(R.string.cmd_app_not_found), name);
    }

    private String doCloseApp() {
        SamuAccessibilityService.pressHome();
        return ctx.getString(R.string.cmd_app_closed);
    }

    // ---- Alarm / timer ----
    private String doAlarm(String hourArg, boolean timer) {
        try {
            Intent i;
            if (timer) {
                int seconds = parseSeconds(hourArg);
                i = new Intent(AlarmClock.ACTION_SET_TIMER)
                        .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        .putExtra(AlarmClock.EXTRA_MESSAGE, "Samu AI Timer")
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            } else {
                int hour = 7, minutes = 0;
                try { hour = Integer.parseInt(hourArg.trim()); } catch (Exception ignored) { }
                i = new Intent(AlarmClock.ACTION_SET_ALARM)
                        .putExtra(AlarmClock.EXTRA_HOUR, hour)
                        .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                        .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return timer ? ctx.getString(R.string.cmd_timer_set) : ctx.getString(R.string.cmd_alarm_set);
        } catch (Exception e) {
            return ctx.getString(R.string.cmd_alarm_failed);
        }
    }

    private int parseSeconds(String arg) {
        try {
            int n = Integer.parseInt(arg.replaceAll("[^0-9]", "").trim());
            if (n <= 0) return 60;
            return n * 60;
        } catch (Exception e) {
            return 60;
        }
    }

    // ---- Torch ----
    private String doTorch(boolean on) {
        try {
            android.hardware.camera2.CameraManager cm =
                    (android.hardware.camera2.CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            if (cm == null) return ctx.getString(R.string.cmd_failed);
            for (String id : cm.getCameraIdList()) {
                Boolean flash = cm.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (flash != null && flash) {
                    cm.setTorchMode(id, on);
                    return on ? ctx.getString(R.string.cmd_torch_on) : ctx.getString(R.string.cmd_torch_off);
                }
            }
        } catch (Exception ignored) { }
        return ctx.getString(R.string.cmd_failed);
    }

    // ---- Wi-Fi ----
    private String doWifi(boolean on) {
        if (Build.VERSION.SDK_INT >= 29) {
            Intent panel = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(panel);
            return ctx.getString(R.string.cmd_wifi_panel);
        }
        @SuppressWarnings("deprecation")
        android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            @SuppressWarnings("deprecation")
            boolean ok = wm.setWifiEnabled(on);
            return ok ? (on ? ctx.getString(R.string.cmd_wifi_on) : ctx.getString(R.string.cmd_wifi_off))
                    : ctx.getString(R.string.cmd_failed);
        }
        return ctx.getString(R.string.cmd_failed);
    }

    // ---- Bluetooth ----
    private String doBluetooth(boolean on) {
        try {
            BluetoothAdapter ba = BluetoothAdapter.getDefaultAdapter();
            if (ba == null) return ctx.getString(R.string.cmd_failed);
            if (on == ba.isEnabled()) {
                return on ? ctx.getString(R.string.cmd_bt_on) : ctx.getString(R.string.cmd_bt_off);
            }
            if (Build.VERSION.SDK_INT >= 31) {
                Intent panel = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                panel.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(panel);
                return ctx.getString(R.string.cmd_bt_panel);
            }
            if (on) ba.enable(); else ba.disable();
            return on ? ctx.getString(R.string.cmd_bt_on) : ctx.getString(R.string.cmd_bt_off);
        } catch (Exception e) {
            return ctx.getString(R.string.cmd_failed);
        }
    }

    // ---- Media / volume / brightness ----
    private String doMedia(boolean play) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ctx.getString(R.string.cmd_failed);
        int key = play ? KeyEvent.KEYCODE_MEDIA_PLAY : KeyEvent.KEYCODE_MEDIA_PAUSE;
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, key));
        am.dispatchMediaKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, key));
        return play ? ctx.getString(R.string.cmd_music_play) : ctx.getString(R.string.cmd_music_pause);
    }

    private String doVolume(int dir) {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ctx.getString(R.string.cmd_failed);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                dir > 0 ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI);
        return dir > 0 ? ctx.getString(R.string.cmd_volume_up) : ctx.getString(R.string.cmd_volume_down);
    }

    private String doMute() {
        AudioManager am = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return ctx.getString(R.string.cmd_failed);
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI);
        return ctx.getString(R.string.cmd_volume_mute);
    }

    private String doBrightness(int dir) {
        if (!Settings.System.canWrite(ctx)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            i.setData(Uri.parse("package:" + ctx.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return ctx.getString(R.string.cmd_write_settings_needed);
        }
        try {
            int now = Settings.System.getInt(ctx.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 128);
            int next = Math.max(20, Math.min(255, now + dir * 60));
            Settings.System.putInt(ctx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, next);
            return dir > 0 ? ctx.getString(R.string.cmd_brightness_up) : ctx.getString(R.string.cmd_brightness_down);
        } catch (Exception e) {
            return ctx.getString(R.string.cmd_failed);
        }
    }

    // ---- Screenshot / lock / DND ----
    private String doScreenshot() {
        boolean ok = SamuAccessibilityService.takeScreenshot();
        return ok ? ctx.getString(R.string.cmd_screenshot) : ctx.getString(R.string.cmd_accessibility_needed);
    }

    private String doLockScreen() {
        DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null && dpm.isAdminActive(new ComponentName(ctx,
                com.samu.ai.admin.SamuDeviceAdminReceiver.class))) {
            dpm.lockNow();
            return ctx.getString(R.string.cmd_locked);
        }
        return ctx.getString(R.string.cmd_device_admin_needed);
    }

    private String doDnd(boolean on) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || !nm.isNotificationPolicyAccessGranted())
            return ctx.getString(R.string.cmd_dnd_needed);
        nm.setInterruptionFilter(on ? NotificationManager.INTERRUPTION_FILTER_NONE
                : NotificationManager.INTERRUPTION_FILTER_ALL);
        return on ? ctx.getString(R.string.cmd_dnd_on) : ctx.getString(R.string.cmd_dnd_off);
    }

    public static class KeyEvent {
        public static final int KEYCODE_MEDIA_PLAY = 126;
        public static final int KEYCODE_MEDIA_PAUSE = 127;
    }
}
