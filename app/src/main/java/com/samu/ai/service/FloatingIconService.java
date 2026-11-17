package com.samu.ai.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.util.PermissionUtils;

import org.json.JSONObject;

/**
 * The floating Samu icon: any image / emoji, any shape, drag anywhere,
 * pinch to resize, double-tap to lock, snaps half-hidden on screen edges.
 */
public class FloatingIconService extends Service {

    private WindowManager wm;
    private View iconView;
    private boolean locked = false;
    private float lastDownX, lastDownY;
    private long lastDownTime;
    private int iconSize = 140;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        startForegroundInternal();
        showIcon();
        return START_STICKY;
    }

    private void startForegroundInternal() {
        android.app.Notification n = new androidx.core.app.NotificationCompat.Builder(this,
                com.samu.ai.App.CHANNEL_SERVICE)
                .setSmallIcon(R.drawable.ic_stat_samu)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setOngoing(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(1002, n,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(1002, n);
        }
    }

    @SuppressLint("InflateParams")
    private void showIcon() {
        if (iconView != null) return;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        iconSize = dp(140);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iconView = iv;
        applyIconVisual();

        WindowManager.LayoutParams lp = buildParams(0, 0);
        restorePosition(lp);
        try {
            wm.addView(iconView, lp);
        } catch (Exception e) {
            stopSelf();
            return;
        }
        attachGestures(lp);
    }

    private WindowManager.LayoutParams buildParams(int x, int y) {
        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                iconSize, iconSize, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = x;
        lp.y = y;
        return lp;
    }

    private void applyIconVisual() {
        String data = SettingsStore.get(this).getIconData();
        ImageView iv = (ImageView) iconView;
        Drawable d = null;
        String shape = "circle";
        if (!TextUtils.isEmpty(data)) {
            try {
                JSONObject o = new JSONObject(data);
                shape = o.optString("shape", "circle");
                if (o.has("emoji")) {
                    TextView tv = new TextView(this);
                    tv.setText(o.getString("emoji"));
                    tv.setTextSize(34);
                    tv.setGravity(Gravity.CENTER);
                    iconView = tv;
                    iv.setVisibility(View.GONE);
                } else if (o.has("path")) {
                    Bitmap bmp = BitmapFactory.decodeFile(o.getString("path"));
                    if (bmp != null) d = new BitmapDrawable(getResources(), bmp);
                }
            } catch (Exception ignored) { }
        }
        if (iconView instanceof ImageView) {
            if (d == null) d = getResources().getDrawable(R.drawable.samu_logo, getTheme());
            ((ImageView) iconView).setImageDrawable(d);
        }
        applyShape(iconView, shape);
    }

    /** Any shape: circle, rounded square, star-ish blob, or plain free image. */
    private void applyShape(View v, String shape) {
        if ("none".equals(shape)) return;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.TRANSPARENT);
        int r = iconSize / 2;
        switch (shape) {
            case "circle": gd.setShape(GradientDrawable.OVAL); break;
            case "square": r = iconSize / 6; break;
            case "heart":
            case "star":
            case "robot": r = iconSize / 3; break;
            default: r = iconSize / 2;
        }
        gd.setCornerRadius(r);
        if ("none".equals(shape) || "free".equals(shape)) return;
        v.setBackground(gd);
        v.setClipToOutline(true);
        final int radius = r;
        try {
            v.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    if ("circle".equals(shape)) outline.setOval(0, 0, view.getWidth(), view.getHeight());
                    else outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            v.setClipToOutline(true);
        } catch (Exception ignored) { }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void attachGestures(WindowManager.LayoutParams lp) {
        ScaleGestureDetector scale = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (locked) return true;
                        iconSize = (int) (iconSize * detector.getScaleFactor());
                        iconSize = Math.max(dp(56), Math.min(dp(320), iconSize));
                        lp.width = iconSize;
                        lp.height = iconSize;
                        try { wm.updateViewLayout(iconView, lp); } catch (Exception ignored) { }
                        return true;
                    }
                });

        iconView.setOnTouchListener((v, event) -> {
            scale.onTouchEvent(event);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastDownX = event.getRawX();
                    lastDownY = event.getRawY();
                    lastDownTime = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (locked || scale.isInProgress()) return true;
                    lp.x += (int) (event.getRawX() - lastDownX);
                    lp.y += (int) (event.getRawY() - lastDownY);
                    lastDownX = event.getRawX();
                    lastDownY = event.getRawY();
                    snapEdge(lp);
                    try { wm.updateViewLayout(iconView, lp); } catch (Exception ignored) { }
                    return true;
                case MotionEvent.ACTION_UP:
                    long dt = System.currentTimeMillis() - lastDownTime;
                    float dist = Math.abs(event.getRawX() - lastDownX)
                            + Math.abs(event.getRawY() - lastDownY);
                    if (dt < 250 && dist < dp(10)) {
                        if (iconView.getTag() != null
                                && (Long) iconView.getTag() > 0) {
                            locked = !locked;
                            saveState(lp);
                            showToast(locked ? R.string.icon_locked : R.string.icon_unlocked);
                            iconView.setTag(null);
                        } else {
                            iconView.setTag(1L);
                            handler.postDelayed(() -> iconView.setTag(null), 350);
                        }
                    } else if (!locked) {
                        saveState(lp);
                    }
                    return true;
                default:
                    return true;
            }
        });
    }

    /** Touching an edge hides half the icon outside the screen. */
    private void snapEdge(WindowManager.LayoutParams lp) {
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        int half = iconSize / 2;
        if (lp.x < 0 && lp.x > -half) lp.x = 0;
        if (lp.x + iconSize > w && lp.x + iconSize < w + half) lp.x = w - iconSize;
        if (lp.x < -half) lp.x = -half;
        if (lp.x + iconSize > w + half) lp.x = w - half;
        lp.y = Math.max(-half, Math.min(lp.y, h - half));
    }

    private void restorePosition(WindowManager.LayoutParams lp) {
        try {
            JSONObject o = new JSONObject(SettingsStore.get(this).getIconData());
            lp.x = o.optInt("x", 0);
            lp.y = o.optInt("y", 100);
            iconSize = o.optInt("size", dp(140));
            lp.width = iconSize;
            lp.height = iconSize;
            locked = o.optBoolean("locked", false);
        } catch (Exception ignored) { }
    }

    private void saveState(WindowManager.LayoutParams lp) {
        try {
            String data = SettingsStore.get(this).getIconData();
            JSONObject o = TextUtils.isEmpty(data) ? new JSONObject() : new JSONObject(data);
            o.put("x", lp.x);
            o.put("y", lp.y);
            o.put("size", iconSize);
            o.put("locked", locked);
            SettingsStore.get(this).setIconData(o.toString());
        } catch (Exception ignored) { }
    }

    private void showToast(int res) {
        android.widget.Toast.makeText(this, res, android.widget.Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        if (iconView != null && wm != null) {
            try { wm.removeView(iconView); } catch (Exception ignored) { }
            iconView = null;
        }
        super.onDestroy();
    }

    public static void start(Context ctx) {
        if (!PermissionUtils.overlay(ctx)) return;
        ctx.startService(new Intent(ctx, FloatingIconService.class));
    }

    public static void refresh(Context ctx) {
        ctx.stopService(new Intent(ctx, FloatingIconService.class));
        if (PermissionUtils.overlay(ctx)) {
            ctx.startService(new Intent(ctx, FloatingIconService.class));
        }
    }
}
