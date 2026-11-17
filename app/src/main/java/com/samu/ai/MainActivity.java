package com.samu.ai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.samu.ai.service.FloatingIconService;
import com.samu.ai.service.SamuForegroundService;
import com.samu.ai.ui.ChatFragment;
import com.samu.ai.ui.HomeFragment;
import com.samu.ai.ui.IconFragment;
import com.samu.ai.ui.PermissionsFragment;
import com.samu.ai.ui.SettingsFragment;
import com.samu.ai.util.PermissionUtils;

/**
 * Samu AI shell - 5 tabs: Home, Chat, Permissions, Icon, Settings.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) f = new HomeFragment();
            else if (id == R.id.nav_chat) f = new ChatFragment();
            else if (id == R.id.nav_permissions) f = new PermissionsFragment();
            else if (id == R.id.nav_icon) f = new IconFragment();
            else if (id == R.id.nav_settings) f = new SettingsFragment();
            if (f == null) return false;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .commit();
            return true;
        });
        if (savedInstanceState == null) nav.setSelectedItemId(R.id.nav_home);

        askStartupPermissions();
        SamuForegroundService.start(this);
        FloatingIconService.start(this);
    }

    private void askStartupPermissions() {
        String[] wanted;
        if (Build.VERSION.SDK_INT >= 33) {
            wanted = new String[]{Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS};
        } else {
            wanted = new String[]{Manifest.permission.RECORD_AUDIO};
        }
        boolean needed = false;
        for (String p : wanted) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                needed = true;
        }
        if (needed) ActivityCompat.requestPermissions(this, wanted, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && PermissionUtils.mic(this)) {
            SamuForegroundService.start(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
}
