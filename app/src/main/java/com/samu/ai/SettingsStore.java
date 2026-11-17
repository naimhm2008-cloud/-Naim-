package com.samu.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Central store for every Samu AI setting. All data stays on the phone.
 */
public class SettingsStore {

    private static final String PREFS = "samu_settings";

    private static SettingsStore instance;
    private final SharedPreferences prefs;

    public static synchronized SettingsStore get(Context ctx) {
        if (instance == null) instance = new SettingsStore(ctx.getApplicationContext());
        return instance;
    }

    private SettingsStore(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---- Gemini ----
    public String getGeminiKey() {
        return prefs.getString("gemini_key", "");
    }

    public void setGeminiKey(String key) {
        prefs.edit().putString("gemini_key", key == null ? "" : key.trim()).apply();
    }

    // ---- Boss / voice lock ----
    public String getSpecialName() {
        return prefs.getString("special_name", "Boss Samu");
    }

    public void setSpecialName(String name) {
        prefs.edit().putString("special_name", name == null || name.trim().isEmpty()
                ? "Boss Samu" : name.trim()).apply();
    }

    public String getBossPhotoPath() {
        return prefs.getString("boss_photo", "");
    }

    public void setBossPhotoPath(String path) {
        prefs.edit().putString("boss_photo", path == null ? "" : path).apply();
    }

    // ---- Emergency ----
    public String getEmergencyNumber1() {
        return prefs.getString("emergency_1", "");
    }

    public String getEmergencyNumber2() {
        return prefs.getString("emergency_2", "");
    }

    public void setEmergencyNumbers(String n1, String n2) {
        prefs.edit().putString("emergency_1", n1 == null ? "" : n1.trim())
                .putString("emergency_2", n2 == null ? "" : n2.trim()).apply();
    }

    // ---- Voice ----
    public float getVoiceSpeed() {
        return prefs.getFloat("voice_speed", 1.0f);
    }

    public float getVoicePitch() {
        return prefs.getFloat("voice_pitch", 1.15f);
    }

    public void setVoice(float speed, float pitch) {
        prefs.edit().putFloat("voice_speed", speed).putFloat("voice_pitch", pitch).apply();
    }

    // ---- App lock ----
    public boolean isAppLockEnabled() {
        return prefs.getBoolean("app_lock_enabled", false);
    }

    public void setAppLockEnabled(boolean enabled) {
        prefs.edit().putBoolean("app_lock_enabled", enabled).apply();
    }

    public Set<String> getLockedApps() {
        return prefs.getStringSet("locked_apps", new HashSet<>());
    }

    public void setLockedApps(Set<String> packages) {
        prefs.edit().putStringSet("locked_apps", packages).apply();
    }

    public void addLockedApp(String pkg) {
        Set<String> set = new HashSet<>(getLockedApps());
        set.add(pkg);
        setLockedApps(set);
    }

    public void removeLockedApp(String pkg) {
        Set<String> set = new HashSet<>(getLockedApps());
        set.remove(pkg);
        setLockedApps(set);
    }

    // ---- Floating icon ----
    public String getIconData() {
        return prefs.getString("icon_data", "");
    }

    public void setIconData(String json) {
        prefs.edit().putString("icon_data", json).apply();
    }

    // ---- Service ----
    public boolean isServiceWanted() {
        return prefs.getBoolean("service_wanted", true);
    }

    public void setServiceWanted(boolean wanted) {
        prefs.edit().putBoolean("service_wanted", wanted).apply();
    }

    public boolean isSleepMode() {
        return prefs.getBoolean("sleep_mode", false);
    }

    public void setSleepMode(boolean sleep) {
        prefs.edit().putBoolean("sleep_mode", sleep).apply();
    }

    // ---- Backup / restore ----
    public JSONObject exportJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("special_name", getSpecialName());
            o.put("boss_photo", getBossPhotoPath());
            o.put("emergency_1", getEmergencyNumber1());
            o.put("emergency_2", getEmergencyNumber2());
            o.put("voice_speed", getVoiceSpeed());
            o.put("voice_pitch", getVoicePitch());
            o.put("app_lock_enabled", isAppLockEnabled());
            o.put("locked_apps", new JSONArray(getLockedApps()));
            o.put("icon_data", getIconData());
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public void importJson(JSONObject o) {
        try {
            if (o.has("special_name")) setSpecialName(o.getString("special_name"));
            if (o.has("boss_photo")) setBossPhotoPath(o.getString("boss_photo"));
            if (o.has("emergency_1") || o.has("emergency_2"))
                setEmergencyNumbers(o.optString("emergency_1", ""), o.optString("emergency_2", ""));
            if (o.has("voice_speed") || o.has("voice_pitch"))
                setVoice((float) o.optDouble("voice_speed", 1.0), (float) o.optDouble("voice_pitch", 1.15));
            if (o.has("app_lock_enabled")) setAppLockEnabled(o.getBoolean("app_lock_enabled"));
            if (o.has("locked_apps")) {
                Set<String> set = new HashSet<>();
                JSONArray arr = o.getJSONArray("locked_apps");
                for (int i = 0; i < arr.length(); i++) set.add(arr.getString(i));
                setLockedApps(set);
            }
            if (o.has("icon_data")) setIconData(o.getString("icon_data"));
        } catch (Exception ignored) {
        }
    }
}
