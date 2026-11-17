package com.samu.ai.util;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Tiny event bridge so the avatar, TTS and services can talk to each other
 * without holding Activity references.
 */
public class SamuAvatarBridge {

    public interface Listener {
        void onSpeakingChanged(boolean speaking);
        void onListeningChanged(boolean listening);
        void onStatusChanged(String status);
    }

    private static final List<Listener> listeners = new CopyOnWriteArrayList<>();

    public static void addListener(Listener l) {
        listeners.add(l);
    }

    public static void removeListener(Listener l) {
        listeners.remove(l);
    }

    public static void setSpeaking(boolean speaking) {
        for (Listener l : listeners) l.onSpeakingChanged(speaking);
    }

    public static void setListening(boolean listening) {
        for (Listener l : listeners) l.onListeningChanged(listening);
    }

    public static void setStatus(String status) {
        for (Listener l : listeners) l.onStatusChanged(status);
    }
}
