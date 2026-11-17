package com.samu.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.samu.ai.R;
import com.samu.ai.model.PermissionItem;

import java.util.List;

public class PermissionAdapter extends RecyclerView.Adapter<PermissionAdapter.Holder> {

    private final List<PermissionItem> items;
    private final PermissionsFragment fragment;

    public PermissionAdapter(List<PermissionItem> items, PermissionsFragment fragment) {
        this.items = items;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_permission, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        PermissionItem item = items.get(position);
        android.content.Context ctx = h.itemView.getContext();
        h.title.setText(position + 1 + ". " + item.title);
        h.reason.setText(item.reason);

        boolean has = false;
        try {
            has = item.checker.has(ctx);
        } catch (Exception ignored) { }

        h.status.setText(has ? R.string.perm_granted : R.string.perm_not_granted);
        h.status.setTextColor(ContextCompat.getColor(ctx,
                has ? R.color.samu_green : R.color.samu_red));
        h.action.setVisibility(has ? View.GONE : View.VISIBLE);
        h.action.setOnClickListener(v -> {
            try {
                item.requester.request(fragment, ctx);
            } catch (Exception ignored) { }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView reason;
        final TextView status;
        final Button action;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.perm_title);
            reason = itemView.findViewById(R.id.perm_reason);
            status = itemView.findViewById(R.id.perm_status);
            action = itemView.findViewById(R.id.perm_action);
        }
    }
}
