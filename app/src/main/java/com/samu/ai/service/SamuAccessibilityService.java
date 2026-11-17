package com.samu.ai.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.samu.ai.App;
import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.lock.AppLockManager;
import com.samu.ai.util.TtsManager;

/**
 * Accessibility service used for: screenshots, home/back presses,
 * media keys and App Lock enforcement.
 */
public class SamuAccessibilityService extends AccessibilityService {

    private static SamuAccessibilityService instance;
    private String lastPackage = "";

    public static SamuAccessibilityService get() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (pkg.equals(getPackageName())) return;
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        if (pkg.equals(lastPackage)) return;
        lastPackage = pkg;

        if (SettingsStore.get(this).isAppLockEnabled()) {
            AppLockManager.get(this).onAppOpened(pkg, this::goHome);
        }
    }

    private void goHome() {
        performGlobalAction(GLOBAL_ACTION_HOME);
        TtsManager.get(this).speak(getString(R.string.refuse_not_boss));
    }

    @Override
    public void onInterrupt() { }

    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        return super.onUnbind(intent);
    }

    // ---- Static helpers used by PhoneActions ----

    public static boolean takeScreenshot() {
        SamuAccessibilityService s = instance;
        if (s == null) return false;
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            s.takeScreenshot(android.view.Display.DEFAULT_DISPLAY,
                    s.getMainExecutor(), new TakeScreenshotCallbackShim());
            return true;
        }
        return false;
    }

    private static class TakeScreenshotCallbackShim
            implements AccessibilityService.TakeScreenshotCallback {
        @Override
        public void onSuccess(android.accessibilityservice.AccessibilityService.ScreenshotResult result) {
            android.graphics.Bitmap bmp = android.graphics.Bitmap.wrapHardwareBuffer(
                    result.getHardwareBuffer(), result.getColorSpace());
            if (bmp == null) return;
            android.graphics.Bitmap copy = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, false);
            result.getHardwareBuffer().close();
            save(copy);
        }

        @Override
        public void onFailure(int errorCode) { }

        private void save(android.graphics.Bitmap bmp) {
            try {
                Context ctx = App.get();
                java.io.File dir = new java.io.File(ctx.getExternalFilesDir(null), "Screenshots");
                if (!dir.exists()) dir.mkdirs();
                java.io.File f = new java.io.File(dir, "screenshot_" + System.currentTimeMillis() + ".png");
                java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
                bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 95, fo);
                fo.close();
            } catch (Exception ignored) { }
        }
    }

    public static void pressHome() {
        if (instance != null) instance.performGlobalAction(GLOBAL_ACTION_HOME);
    }

    public static void pressBack() {
        if (instance != null) instance.performGlobalAction(GLOBAL_ACTION_BACK);
    }

    public static void pressRecents() {
        if (instance != null) instance.performGlobalAction(GLOBAL_ACTION_RECENTS);
    }
}
