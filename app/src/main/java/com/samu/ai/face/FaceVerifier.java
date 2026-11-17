package com.samu.ai.face;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Offline boss verification. When the special name is spoken, BossCheckActivity
 * opens invisibly, grabs 1-2 seconds of front-camera frames, runs on-device
 * ML Kit face detection and compares a color signature with the saved boss
 * photo. Nothing is uploaded anywhere.
 */
public class FaceVerifier {

    public interface Result {
        void onVerified(boolean isBoss);
    }

    public static final String EXTRA_MODE = "mode";
    public static final String MODE_VERIFY = "verify";

    public static final int NO_PHOTO = 2;

    private static final double MATCH_THRESHOLD = 0.55;
    private static final long COOLDOWN_MS = 1500;
    private static long lastRun = 0;
    private static Result pendingResult;
    private static final AtomicBoolean busy = new AtomicBoolean(false);

    public static boolean hasBossPhoto(Context ctx) {
        String p = com.samu.ai.SettingsStore.get(ctx).getBossPhotoPath();
        return p != null && !p.isEmpty() && new File(p).exists();
    }

    /** Called by the voice service when the special name is heard. */
    public static void verifyBoss(Context ctx, Result result) {
        long now = System.currentTimeMillis();
        if (now - lastRun < COOLDOWN_MS || !busy.compareAndSet(false, true)) {
            result.onVerified(false);
            return;
        }
        lastRun = now;
        pendingResult = result;
        Intent i = new Intent(ctx, BossCheckActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        ctx.startActivity(i);
    }

    /** BossCheckActivity reports the outcome here. */
    public static void deliver(boolean isBoss, boolean hasPhoto) {
        Result r = pendingResult;
        pendingResult = null;
        busy.set(false);
        if (r != null) r.onVerified(isBoss && hasPhoto);
    }

    /** Full frame analysis used by BossCheckActivity and Boss photo capture. */
    public static void analyzeFrame(Bitmap frame, FaceDetector detector,
                                    Bitmap bossPhoto, Result result) {
        InputImage image = InputImage.fromBitmap(frame, 0);
        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.isEmpty()) {
                        result.onVerified(false);
                        return;
                    }
                    boolean matched = false;
                    for (Face face : faces) {
                        Bitmap crop = safeCrop(frame, face.getBoundingBox());
                        if (crop == null) continue;
                        if (similarity(crop, bossPhoto) >= MATCH_THRESHOLD) matched = true;
                    }
                    result.onVerified(matched);
                })
                .addOnFailureListener(f -> result.onVerified(false));
    }

    public static FaceDetector createDetector() {
        FaceDetectorOptions opts = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .setMinFaceSize(0.15f)
                .build();
        return FaceDetection.getClient(opts);
    }

    private static Bitmap safeCrop(Bitmap src, Rect box) {
        int l = Math.max(0, box.left);
        int t = Math.max(0, box.top);
        int w = Math.min(box.width(), src.getWidth() - l);
        int h = Math.min(box.height(), src.getHeight() - t);
        if (w <= 0 || h <= 0) return null;
        return Bitmap.createBitmap(src, l, t, w, h);
    }

    /**
     * Lightweight on-device color-signature similarity (0..1).
     */
    public static double similarity(Bitmap a, Bitmap b) {
        int[] ha = histogram(a);
        int[] hb = histogram(b);
        long dot = 0, na = 0, nb = 0;
        for (int i = 0; i < ha.length; i++) {
            dot += (long) ha[i] * hb[i];
            na += (long) ha[i] * ha[i];
            nb += (long) hb[i] * hb[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt((double) na) * Math.sqrt((double) nb));
    }

    private static int[] histogram(Bitmap bmp) {
        Bitmap small = Bitmap.createScaledBitmap(bmp, 32, 32, true);
        int[] px = new int[32 * 32];
        small.getPixels(px, 0, 32, 0, 0, 32, 32);
        int[] hist = new int[512];
        for (int p : px) {
            int r = (p >> 16 & 0xFF) >> 3;
            int g = (p >> 8 & 0xFF) >> 3;
            int b = (p & 0xFF) >> 3;
            hist[(r * 8 + g) * 8 + b]++;
        }
        return hist;
    }

    public static Bitmap loadBossPhoto(Context ctx) {
        String p = com.samu.ai.SettingsStore.get(ctx).getBossPhotoPath();
        if (p == null || p.isEmpty()) return null;
        return BitmapFactory.decodeFile(p);
    }

    public static String saveBossBitmap(Context ctx, Bitmap bmp) {
        try {
            File dir = new File(ctx.getFilesDir(), "boss");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "boss_photo.png");
            java.io.FileOutputStream fo = new java.io.FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 95, fo);
            fo.close();
            return f.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }
}
