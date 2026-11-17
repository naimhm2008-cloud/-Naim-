package com.samu.ai.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.backup.BackupManager;
import com.samu.ai.face.BossPhotoActivity;
import com.samu.ai.service.FloatingIconService;
import com.samu.ai.service.SamuForegroundService;
import com.samu.ai.util.TtsManager;

/**
 * Settings tab: Gemini key, special name, boss photo, emergency numbers,
 * voice speed/pitch, app lock, backup/restore, service on/off.
 */
public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);
        SettingsStore store = SettingsStore.get(requireContext());

        EditText keyInput = v.findViewById(R.id.set_gemini_key);
        keyInput.setText(store.getGeminiKey());
        Button saveKey = v.findViewById(R.id.btn_save_gemini);
        saveKey.setOnClickListener(b -> {
            store.setGeminiKey(keyInput.getText().toString());
            Toast.makeText(getContext(), R.string.saved, Toast.LENGTH_SHORT).show();
        });

        EditText nameInput = v.findViewById(R.id.set_special_name);
        nameInput.setText(store.getSpecialName());
        Button saveName = v.findViewById(R.id.btn_save_name);
        saveName.setOnClickListener(b -> {
            store.setSpecialName(nameInput.getText().toString());
            Toast.makeText(getContext(), R.string.saved, Toast.LENGTH_SHORT).show();
        });

        Button bossPhoto = v.findViewById(R.id.btn_boss_photo);
        bossPhoto.setOnClickListener(b ->
                startActivity(new Intent(requireContext(), BossPhotoActivity.class)));

        EditText em1 = v.findViewById(R.id.set_emergency_1);
        EditText em2 = v.findViewById(R.id.set_emergency_2);
        em1.setText(store.getEmergencyNumber1());
        em2.setText(store.getEmergencyNumber2());
        Button saveEm = v.findViewById(R.id.btn_save_emergency);
        saveEm.setOnClickListener(b -> {
            store.setEmergencyNumbers(em1.getText().toString(), em2.getText().toString());
            Toast.makeText(getContext(), R.string.saved, Toast.LENGTH_SHORT).show();
        });

        SeekBar speed = v.findViewById(R.id.set_voice_speed);
        SeekBar pitch = v.findViewById(R.id.set_voice_pitch);
        speed.setProgress(Math.round((store.getVoiceSpeed() - 0.5f) * 100));
        pitch.setProgress(Math.round((store.getVoicePitch() - 0.5f) * 100));
        SeekBar.OnSeekBarChangeListener voice = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                store.setVoice(0.5f + speed.getProgress() / 100f,
                        0.5f + pitch.getProgress() / 100f);
                TtsManager.get(getContext()).speak(getString(R.string.voice_test_line));
            }
        };
        speed.setOnSeekBarChangeListener(voice);
        pitch.setOnSeekBarChangeListener(voice);

        Button appLock = v.findViewById(R.id.btn_app_lock);
        appLock.setOnClickListener(b ->
                startActivity(new Intent(requireContext(), AppLockActivity.class)));

        Button backup = v.findViewById(R.id.btn_backup);
        Button restore = v.findViewById(R.id.btn_restore);
        backup.setOnClickListener(b -> Toast.makeText(getContext(),
                BackupManager.backup(requireContext()), Toast.LENGTH_LONG).show());
        restore.setOnClickListener(b -> {
            String r = BackupManager.restoreLatest(requireContext());
            Toast.makeText(getContext(), r, Toast.LENGTH_LONG).show();
            FloatingIconService.refresh(requireContext());
        });

        Button service = v.findViewById(R.id.btn_service_toggle);
        service.setOnClickListener(b -> {
            if (store.isServiceWanted()) {
                store.setServiceWanted(false);
                SamuForegroundService.stop(requireContext());
                Toast.makeText(getContext(), R.string.service_off, Toast.LENGTH_SHORT).show();
            } else {
                store.setServiceWanted(true);
                SamuForegroundService.start(requireContext());
                Toast.makeText(getContext(), R.string.service_on, Toast.LENGTH_SHORT).show();
            }
        });

        TextView privacy = v.findViewById(R.id.set_privacy_note);
        privacy.setText(R.string.privacy_note);
    }
}
