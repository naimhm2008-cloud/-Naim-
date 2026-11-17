package com.samu.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.model.AppInfo;

import java.util.List;

public class AppLockAdapter extends RecyclerView.Adapter<AppLockAdapter.Holder> {

    private final List<AppInfo> apps;

    public AppLockAdapter(List<AppInfo> apps) {
        this.apps = apps;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_lock, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AppInfo app = apps.get(position);
        h.name.setText(app.label);
        h.pkg.setText(app.packageName);
        h.switcher.setOnCheckedChangeListener(null);
        h.switcher.setChecked(app.locked);
        h.switcher.setOnCheckedChangeListener((b, checked) -> {
            app.locked = checked;
            if (checked) SettingsStore.get(b.getContext()).addLockedApp(app.packageName);
            else SettingsStore.get(b.getContext()).removeLockedApp(app.packageName);
        });
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView pkg;
        final Switch switcher;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.app_name);
            pkg = itemView.findViewById(R.id.app_package);
            switcher = itemView.findViewById(R.id.app_lock_switch_item);
        }
    }
}
