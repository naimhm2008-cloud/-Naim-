package com.samu.ai.lock;

import android.content.Context;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.util.TtsManager;

/**
 * App lock: when a locked app opens, Samu blocks it unless the boss has been
 * verified in the last 30 seconds.
 */
public class AppLockManager {

    private static final long UNLOCK_WINDOW_MS = 30_000;
    private static volatile long bossVerifiedAt = 0;

    public interface Blocker {
        void block();
    }

    private static AppLockManager instance;

    public static synchronized AppLockManager get(Context ctx) {
        if (instance == null) instance = new AppLockManager(ctx.getApplicationContext());
        return instance;
    }

    private final Context ctx;

    private AppLockManager(Context ctx) {
        this.ctx = ctx;
    }

    public static void markBossVerified() {
        bossVerifiedAt = System.currentTimeMillis();
    }

    public boolean isLocked(String pkg) {
        SettingsStore s = SettingsStore.get(ctx);
        return s.isAppLockEnabled() && s.getLockedApps().contains(pkg);
    }

    public void onAppOpened(String pkg, Blocker blocker) {
        if (!isLocked(pkg)) return;
        boolean isBoss = System.currentTimeMillis() - bossVerifiedAt < UNLOCK_WINDOW_MS;
        if (isBoss) return;
        blocker.block();
    }

    public String lockedWarning() {
        return ctx.getString(R.string.refuse_not_boss);
    }

    public static void warn(Context ctx) {
        TtsManager.get(ctx).speak(ctx.getString(R.string.refuse_not_boss));
    }
}
