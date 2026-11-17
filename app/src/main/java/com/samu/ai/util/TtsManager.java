package com.samu.ai.util;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.samu.ai.App;
import com.samu.ai.R;
import com.samu.ai.SettingsStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bengali + English text-to-speech with adjustable speed / pitch / female voice.
 */
public class TtsManager {

    public interface ReadyListener {
        void onReady();
    }

    private static TtsManager instance;
    private TextToSpeech tts;
    private boolean ready = false;
    private final List<ReadyListener> pending = new ArrayList<>();
    private String currentLang = "";

    public static synchronized TtsManager get(Context ctx) {
        if (instance == null) instance = new TtsManager(ctx.getApplicationContext());
        return instance;
    }

    private TtsManager(Context ctx) {
        tts = new TextToSpeech(ctx, status -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String utteranceId) { SamuAvatarBridge.setSpeaking(true); }
                    @Override public void onDone(String utteranceId) { SamuAvatarBridge.setSpeaking(false); }
                    @Override public void onError(String utteranceId) { SamuAvatarBridge.setSpeaking(false); }
                });
                List<ReadyListener> copy = new ArrayList<>(pending);
                pending.clear();
                for (ReadyListener l : copy) l.onReady();
            }
        });
    }

    public boolean isReady() {
        return ready;
    }

    public void whenReady(ReadyListener l) {
        if (ready) l.onReady();
        else pending.add(l);
    }

    public void speak(String text) {
        if (!ready || text == null || text.trim().isEmpty()) return;
        SettingsStore s = SettingsStore.get(App.get());
        String lang = detectLanguage(text);
        if (!lang.equals(currentLang)) {
            Locale locale = lang.equals("bn") ? new Locale("bn", "BD") : Locale.US;
            int res = tts.setLanguage(locale);
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.getDefault());
            }
            currentLang = lang;
        }
        tts.setSpeechRate(s.getVoiceSpeed());
        tts.setPitch(s.getVoicePitch());
        String id = String.valueOf(System.currentTimeMillis());
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id);
    }

    public void stop() {
        if (tts != null) tts.stop();
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            ready = false;
        }
    }

    /** Very small heuristic: Bengali unicode block present => Bengali. */
    public static String detectLanguage(String text) {
        if (text == null) return "en";
        for (char c : text.toCharArray()) {
            if (c >= '\u0980' && c <= '\u09FF') return "bn";
        }
        return "en";
    }
}
