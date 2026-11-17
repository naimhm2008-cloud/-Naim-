package com.samu.ai.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Locale;
import java.util.Map;

/**
 * Maps spoken app names (Banglish/Bengali/English) to installed packages.
 */
public class AppLookup {

    private static final Map<String, String> KNOWN = new java.util.HashMap<>();

    static {
        KNOWN.put("whatsapp", "com.whatsapp");
        KNOWN.put("ওয়াটসঅ্যাপ", "com.whatsapp");
        KNOWN.put("ওয়াটসঅ্যাপস", "com.whatsapp");
        KNOWN.put("facebook", "com.facebook.katana");
        KNOWN.put("ফেসবুক", "com.facebook.katana");
        KNOWN.put("messenger", "com.facebook.orca");
        KNOWN.put("মেসেঞ্জার", "com.facebook.orca");
        KNOWN.put("youtube", "com.google.android.youtube");
        KNOWN.put("ইউটিউব", "com.google.android.youtube");
        KNOWN.put("gallery", "com.sec.android.gallery3d");
        KNOWN.put("গ্যালারি", "gallery");
        KNOWN.put("camera", "camera");
        KNOWN.put("ক্যামেরা", "camera");
        KNOWN.put("phone", "com.android.dialer");
        KNOWN.put("ফোন", "com.android.dialer");
        KNOWN.put("chrome", "com.android.chrome");
        KNOWN.put("ক্রোম", "com.android.chrome");
        KNOWN.put("gmail", "com.google.android.gm");
        KNOWN.put("জিমেইল", "com.google.android.gm");
        KNOWN.put("maps", "com.google.android.apps.maps");
        KNOWN.put("ম্যাপ", "com.google.android.apps.maps");
        KNOWN.put("settings", "settings");
        KNOWN.put("সেটিংস", "settings");
        KNOWN.put("bikash", "com.bKash.customerapp");
        KNOWN.put("বিকাশ", "com.bKash.customerapp");
        KNOWN.put("nagad", "com.nagad.android.wallet");
        KNOWN.put("নগদ", "com.nagad.android.wallet");
    }

    public static String findPackage(Context ctx, String spoken) {
        if (spoken == null) return null;
        String key = spoken.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return null;

        for (Map.Entry<String, String> e : KNOWN.entrySet()) {
            if (key.contains(e.getKey())) {
                String pkg = e.getValue();
                if (pkg.equals("settings")) return "com.android.settings";
                if (pkg.equals("camera")) return findCamera(ctx);
                if (pkg.equals("gallery")) return findGallery(ctx);
                if (isInstalled(ctx, pkg)) return pkg;
            }
        }

        PackageManager pm = ctx.getPackageManager();
        for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            String label;
            try {
                label = pm.getApplicationLabel(ai).toString().toLowerCase(Locale.ROOT);
            } catch (Exception e) {
                continue;
            }
            if (!label.isEmpty() && (label.contains(key) || key.contains(label))) return ai.packageName;
        }
        return null;
    }

    private static boolean isInstalled(Context ctx, String pkg) {
        try {
            ctx.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String findCamera(Context ctx) {
        Intent test = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        return resolveDefault(ctx, test, "com.android.camera");
    }

    private static String findGallery(Context ctx) {
        Intent test = new Intent(Intent.ACTION_VIEW,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        return resolveDefault(ctx, test, "com.google.android.apps.photos");
    }

    private static String resolveDefault(Context ctx, Intent test, String fallback) {
        try {
            android.content.pm.ResolveInfo ri = ctx.getPackageManager()
                    .resolveActivity(test, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri != null && ri.activityInfo != null) return ri.activityInfo.packageName;
        } catch (Exception ignored) { }
        return isInstalled(ctx, fallback) ? fallback : null;
    }
}
