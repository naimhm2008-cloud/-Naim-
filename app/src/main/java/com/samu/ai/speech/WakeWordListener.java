package com.samu.ai.speech;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.samu.ai.util.SamuAvatarBridge;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Always-on wake word listener built on Android's offline-capable SpeechRecognizer.
 * It restarts itself after every result / error so Samu keeps listening
 * while the screen is on or off (low power = longer silence window).
 */
public class WakeWordListener implements RecognitionListener {

    public interface Listener {
        void onHeard(String text);
    }

    public static final String WAKE_HEY = "hey samu";
    public static final String WAKE_HEY_BN = "হে সামু";

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private boolean running = false;
    private boolean lowPower = false;
    private int errorCount = 0;

    public WakeWordListener(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public synchronized void start() {
        if (running || !SpeechRecognizer.isRecognitionAvailable(context)) return;
        running = true;
        errorCount = 0;
        createRecognizer();
        beginListening();
    }

    public synchronized void stop() {
        running = false;
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        SamuAvatarBridge.setListening(false);
    }

    public void setLowPower(boolean low) {
        lowPower = low;
    }

    private void createRecognizer() {
        if (recognizer != null) return;
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);
    }

    private void beginListening() {
        if (!running || recognizer == null) return;
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "bn-BD");
        try {
            recognizer.startListening(intent);
            SamuAvatarBridge.setListening(true);
        } catch (Exception e) {
            scheduleRestart();
        }
    }

    private void scheduleRestart() {
        android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
        long delay = lowPower ? 4000L : 1200L;
        h.postDelayed(this::beginListening, delay);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        handle(partialResults);
    }

    @Override
    public void onResults(Bundle results) {
        handle(results);
        beginListening();
    }

    private void handle(Bundle bundle) {
        if (bundle == null || !running) return;
        ArrayList<String> list = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list == null) return;
        for (String text : list) {
            if (text == null) continue;
            String norm = normalize(text);
            if (norm.contains(WAKE_HEY) || norm.contains(WAKE_HEY_BN)) {
                String after = stripWake(norm);
                listener.onHeard(after.isEmpty() ? "" : after);
                return;
            }
        }
    }

    /** Removes the wake phrase so only the command part remains. */
    public static String stripWake(String norm) {
        String t = norm;
        int i = t.indexOf(WAKE_HEY_BN);
        if (i >= 0) t = t.substring(i + WAKE_HEY_BN.length());
        else {
            i = t.indexOf(WAKE_HEY);
            if (i >= 0) t = t.substring(i + WAKE_HEY.length());
        }
        return t.trim();
    }

    /** Lowercase + strip punctuation so Bengali and English compare cleanly. */
    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[.,!?;:\"'\\-–—]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Override
    public void onError(int error) {
        errorCount++;
        if (error == SpeechRecognizer.ERROR_NO_MATCH
                || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            beginListening();
        } else if (running && errorCount < 8) {
            scheduleRestart();
        } else if (running) {
            errorCount = 0;
            scheduleRestart();
        }
    }

    @Override public void onReadyForSpeech(Bundle params) { errorCount = 0; }
    @Override public void onBeginningOfSpeech() { }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { }
    @Override public void onEvent(int eventType, Bundle params) { }
}
