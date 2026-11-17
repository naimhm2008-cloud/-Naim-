package com.samu.ai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.samu.ai.R;
import com.samu.ai.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private final List<ChatMessage> items = new ArrayList<>();

    public void add(ChatMessage m) {
        items.add(m);
        notifyItemInserted(items.size() - 1);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == ChatMessage.FROM_USER
                ? R.layout.item_chat_user : R.layout.item_chat_samu;
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        h.text.setText(items.get(position).text);
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).from;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.chat_bubble_text);
        }
    }
}
