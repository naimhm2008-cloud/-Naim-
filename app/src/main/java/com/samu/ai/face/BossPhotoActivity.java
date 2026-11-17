package com.samu.ai.face;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;

import java.io.File;
import java.io.InputStream;

/**
 * Boss photo: take with the camera or pick from the gallery.
 * Saved only inside the app's private storage.
 */
public class BossPhotoActivity extends AppCompatActivity {

    private static final int REQ_CAMERA = 11;
    private static final int REQ_GALLERY = 12;
    private static final int REQ_CAM_PERM = 13;

    private ImageView preview;
    private File captureFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boss_photo);
        preview = findViewById(R.id.boss_preview_img);
        Button camera = findViewById(R.id.btn_boss_camera);
        Button gallery = findViewById(R.id.btn_boss_gallery);
        Button remove = findViewById(R.id.btn_boss_remove);

        showExisting();

        camera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA}, REQ_CAM_PERM);
                return;
            }
            openCamera();
        });
        gallery.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, REQ_GALLERY);
        });
        remove.setOnClickListener(v -> {
            SettingsStore.get(this).setBossPhotoPath("");
            showExisting();
            Toast.makeText(this, R.string.boss_photo_removed, Toast.LENGTH_SHORT).show();
        });
    }

    private void showExisting() {
        Bitmap bmp = FaceVerifier.loadBossPhoto(this);
        if (bmp != null) preview.setImageBitmap(bmp);
    }

    private void openCamera() {
        captureFile = new File(getFilesDir(), "boss_capture_tmp.png");
        Uri uri = FileProvider.getUriForFile(this, "com.samu.ai.fileprovider", captureFile);
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri);
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(i, REQ_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        try {
            Bitmap bmp = null;
            if (requestCode == REQ_CAMERA && captureFile != null && captureFile.exists()) {
                bmp = BitmapFactory.decodeFile(captureFile.getAbsolutePath());
            } else if (requestCode == REQ_GALLERY && data != null && data.getData() != null) {
                InputStream is = getContentResolver().openInputStream(data.getData());
                bmp = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
            }
            if (bmp != null) {
                String path = FaceVerifier.saveBossBitmap(this, bmp);
                SettingsStore.get(this).setBossPhotoPath(path);
                preview.setImageBitmap(bmp);
                Toast.makeText(this, R.string.boss_photo_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.boss_photo_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
