package com.samu.ai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.samu.ai.R;
import com.samu.ai.ai.SamuBrain;
import com.samu.ai.util.TtsManager;

/**
 * Chat tab: type anything, Samu answers in the same language and speaks it.
 */
public class ChatFragment extends Fragment implements SamuBrain.Responder {

    private ChatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_chat, container, false);
        RecyclerView list = v.findViewById(R.id.chat_list);
        adapter = new ChatAdapter();
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);
        adapter.add(new com.samu.ai.model.ChatMessage(com.samu.ai.model.ChatMessage.FROM_SAMU,
                getString(R.string.chat_hello)));

        EditText input = v.findViewById(R.id.chat_input);
        ImageButton send = v.findViewById(R.id.chat_send);
        send.setOnClickListener(btn -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            adapter.add(new com.samu.ai.model.ChatMessage(
                    com.samu.ai.model.ChatMessage.FROM_USER, text));
            input.setText("");
            new SamuBrain(requireContext(), this).think(SamuBrain.Trigger.CHAT_UI, text);
        });
        return v;
    }

    @Override
    public void say(String text) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (adapter != null) adapter.add(
                    new com.samu.ai.model.ChatMessage(com.samu.ai.model.ChatMessage.FROM_SAMU, text));
            TtsManager.get(getContext()).speak(text);
        });
    }
}
