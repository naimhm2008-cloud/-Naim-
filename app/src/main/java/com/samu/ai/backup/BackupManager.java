package com.samu.ai.backup;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.face.FaceVerifier;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 100% offline backup. Everything is written to the Downloads folder through
 * MediaStore so it survives uninstall; restoring reads it back. Nothing is
 * ever uploaded.
 */
public class BackupManager {

    public static String backup(Context ctx) {
        try {
            SettingsStore s = SettingsStore.get(ctx);
            JSONObject data = s.exportJson();

            String bossPath = s.getBossPhotoPath();
            if (!bossPath.isEmpty()) {
                Bitmap bmp = BitmapFactory.decodeFile(bossPath);
                if (bmp != null) data.put("boss_photo_b64", encodePng(bmp));
            }

            Uri uri = writeDownloads(ctx, "samu_backup_"
                    + System.currentTimeMillis() + ".json", data.toString().getBytes());
            return uri != null ? ctx.getString(R.string.backup_done) : ctx.getString(R.string.backup_failed);
        } catch (Exception e) {
            return ctx.getString(R.string.backup_failed);
        }
    }

    public static String restoreLatest(Context ctx) {
        try {
            byte[] bytes = readLatestBackup(ctx);
            if (bytes == null) return ctx.getString(R.string.backup_none);
            JSONObject data = new JSONObject(new String(bytes));
            if (data.has("boss_photo_b64")) {
                Bitmap bmp = decodePng(data.getString("boss_photo_b64"));
                if (bmp != null) {
                    String p = FaceVerifier.saveBossBitmap(ctx, bmp);
                    data.put("boss_photo", p);
                }
            }
            SettingsStore.get(ctx).importJson(data);
            return ctx.getString(R.string.restore_done);
        } catch (Exception e) {
            return ctx.getString(R.string.restore_failed);
        }
    }

    // ---- MediaStore helpers ----

    private static Uri writeDownloads(Context ctx, String name, byte[] bytes) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
            cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = ctx.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
            if (uri == null) return null;
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                os.write(bytes);
            }
            return uri;
        }
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(dir, name);
        try (FileOutputStream fo = new FileOutputStream(f)) {
            fo.write(bytes);
        }
        return Uri.fromFile(f);
    }

    private static byte[] readLatestBackup(Context ctx) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            android.database.Cursor c = ctx.getContentResolver().query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID},
                    MediaStore.MediaColumns.DISPLAY_NAME + " LIKE ?",
                    new String[]{"samu_backup_%.json"},
                    MediaStore.MediaColumns.DATE_ADDED + " DESC");
            if (c != null) {
                if (c.moveToFirst()) {
                    long id = c.getLong(0);
                    c.close();
                    Uri uri = android.content.ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
                    InputStream is = ctx.getContentResolver().openInputStream(uri);
                    byte[] out = readAll(is);
                    if (is != null) is.close();
                    return out;
                }
                c.close();
            }
            return null;
        }
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = dir.listFiles((d, n) -> n.startsWith("samu_backup_") && n.endsWith(".json"));
        if (files == null || files.length == 0) return null;
        java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        FileInputStream fis = new FileInputStream(files[0]);
        byte[] out = readAll(fis);
        fis.close();
        return out;
    }

    private static byte[] readAll(InputStream is) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    // ---- PNG <-> base64 ----

    private static String encodePng(Bitmap bmp) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 90, bos);
        return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP);
    }

    private static Bitmap decodePng(String b64) {
        try {
            byte[] bytes = android.util.Base64.decode(b64, android.util.Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
