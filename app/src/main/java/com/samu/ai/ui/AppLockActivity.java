package com.samu.ai.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * App lock screen: pick which apps only the boss may open.
 */
public class AppLockActivity extends AppCompatActivity {

    private final List<AppInfo> apps = new ArrayList<>();
    private AppLockAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        Switch enable = findViewById(R.id.app_lock_switch);
        enable.setChecked(SettingsStore.get(this).isAppLockEnabled());
        enable.setOnCheckedChangeListener((b, checked) -> {
            SettingsStore.get(this).setAppLockEnabled(checked);
            Toast.makeText(this, checked ? R.string.app_lock_on : R.string.app_lock_off,
                    Toast.LENGTH_SHORT).show();
        });

        RecyclerView list = findViewById(R.id.app_lock_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppLockAdapter(apps);
        list.setAdapter(adapter);

        loadApps();

        Button done = findViewById(R.id.btn_app_lock_done);
        done.setOnClickListener(v -> finish());
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> installed = pm.getInstalledApplications(0);
        java.util.Set<String> locked = SettingsStore.get(this).getLockedApps();
        apps.clear();
        for (ApplicationInfo ai : installed) {
            if (ai.packageName.equals(getPackageName())) continue;
            String label;
            try {
                label = pm.getApplicationLabel(ai).toString();
            } catch (Exception e) {
                continue;
            }
            apps.add(new AppInfo(label, ai.packageName, locked.contains(ai.packageName)));
        }
        java.util.Collections.sort(apps, (a, b) -> a.label.compareToIgnoreCase(b.label));
        adapter.notifyDataSetChanged();
    }
}
