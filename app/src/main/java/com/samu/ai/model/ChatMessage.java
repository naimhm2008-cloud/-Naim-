package com.samu.ai.model;

public class ChatMessage {
    public static final int FROM_USER = 0;
    public static final int FROM_SAMU = 1;

    public final int from;
    public final String text;
    public final long time;

    public ChatMessage(int from, String text) {
        this.from = from;
        this.text = text == null ? "" : text;
        this.time = System.currentTimeMillis();
    }
}
