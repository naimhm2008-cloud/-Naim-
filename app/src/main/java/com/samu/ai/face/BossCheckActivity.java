package com.samu.ai.face;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.face.FaceDetector;
import com.samu.ai.R;
import com.samu.ai.util.TtsManager;

import java.util.concurrent.ExecutionException;

/**
 * Invisible 1-2 second front-camera check used by the voice lock.
 */
public class BossCheckActivity extends AppCompatActivity {

    private FaceDetector detector;
    private ProcessCameraProvider cameraProvider;
    private boolean done = false;
    private int framesAnalyzed = 0;
    private static final int MAX_FRAMES = 12;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_check);
        detector = FaceVerifier.createDetector();
        startCamera();
        android.os.Handler h = new android.os.Handler(getMainLooper());
        h.postDelayed(() -> finishCheck(false, true), 2500);
    }

    private void startCamera() {
        if (!com.samu.ai.util.PermissionUtils.camera(this)) {
            finishCheck(false, true);
            return;
        }
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(
                        ((androidx.camera.view.PreviewView) findViewById(R.id.boss_preview))
                                .getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyze);

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this,
                        CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis);
            } catch (Exception e) {
                finishCheck(false, true);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyze(@NonNull ImageProxy proxy) {
        if (done) {
            proxy.close();
            return;
        }
        framesAnalyzed++;
        Bitmap bmp = proxy.toBitmap();
        int rotation = proxy.getImageInfo().getRotationDegrees();
        proxy.close();
        if (bmp == null) return;
        if (rotation != 0) {
            android.graphics.Matrix m = new android.graphics.Matrix();
            m.postRotate(rotation);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }
        Bitmap boss = FaceVerifier.loadBossPhoto(this);
        if (boss == null) {
            finishCheck(false, false);
            return;
        }
        FaceVerifier.analyzeFrame(bmp, detector, boss, isBoss -> {
            if (isBoss && !done) finishCheck(true, true);
            else if (framesAnalyzed >= MAX_FRAMES && !done) finishCheck(false, true);
        });
    }

    private void finishCheck(boolean isBoss, boolean hasPhoto) {
        if (done) return;
        done = true;
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (detector != null) detector.close();
        FaceVerifier.deliver(isBoss, hasPhoto);
        if (!hasPhoto) {
            TtsManager.get(this).speak(getString(R.string.boss_photo_missing));
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        finishCheck(false, true);
    }
}
