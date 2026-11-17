package com.samu.ai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.samu.ai.R;
import com.samu.ai.util.SamuAvatarBridge;
import com.samu.ai.util.TtsManager;
import com.samu.ai.view.SamuAvatarView;

/**
 * Home tab: the animated Samu robot girl + status + big mic button.
 */
public class HomeFragment extends Fragment implements SamuAvatarBridge.Listener {

    private SamuAvatarView avatar;
    private TextView status;
    private TextView bubble;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        avatar = v.findViewById(R.id.samu_avatar);
        status = v.findViewById(R.id.home_status);
        bubble = v.findViewById(R.id.home_bubble);
        TextView greeting = v.findViewById(R.id.home_greeting);
        greeting.setOnClickListener(vg -> TtsManager.get(getContext())
                .speak(getString(R.string.home_tap_hint)));
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        SamuAvatarBridge.addListener(this);
        status.setText(R.string.status_active);
        bubble.setText(R.string.home_welcome);
        TtsManager.get(getContext()).whenReady(() ->
                TtsManager.get(getContext()).speak(getString(R.string.home_welcome)));
    }

    @Override
    public void onDestroyView() {
        SamuAvatarBridge.removeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSpeakingChanged(boolean speaking) {
        if (avatar != null) avatar.setSpeaking(speaking);
    }

    @Override
    public void onListeningChanged(boolean listening) {
        if (avatar != null) avatar.setListening(listening);
        if (status != null && getView() != null) {
            status.post(() -> status.setText(listening
                    ? R.string.status_listening : R.string.status_active));
        }
    }

    @Override
    public void onStatusChanged(String s) {
        if (status != null) status.post(() -> status.setText(s));
    }
}
