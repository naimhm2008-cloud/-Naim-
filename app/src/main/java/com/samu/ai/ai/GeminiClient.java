package com.samu.ai.ai;

import com.samu.ai.App;
import com.samu.ai.R;
import com.samu.ai.SettingsStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Talks to the Google Gemini API using the key the boss saves inside the app.
 */
public class GeminiClient {

    public interface Callback {
        void onAnswer(String text);
    }

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    public static boolean hasKey() {
        return !SettingsStore.get(App.get()).getGeminiKey().isEmpty();
    }

    public static void ask(String userText, Callback cb) {
        String key = SettingsStore.get(App.get()).getGeminiKey();
        if (key.isEmpty()) {
            cb.onAnswer(App.get().getString(R.string.no_api_key));
            return;
        }
        EXEC.execute(() -> {
            try {
                String answer = request(key, userText);
                cb.onAnswer(answer);
            } catch (Exception e) {
                cb.onAnswer(App.get().getString(R.string.ai_error));
            }
        });
    }

    private static String request(String key, String userText) throws Exception {
        JSONObject body = new JSONObject();
        JSONObject textPart = new JSONObject().put("text", systemPrompt() + "\n\nUser: " + userText);
        JSONArray parts = new JSONArray().put(textPart);
        JSONObject content = new JSONObject().put("parts", parts);
        JSONArray contents = new JSONArray().put(content);
        body.put("contents", contents);
        JSONObject gen = new JSONObject()
                .put("maxOutputTokens", 300)
                .put("temperature", 0.8);
        body.put("generationConfig", gen);

        HttpURLConnection conn = (HttpURLConnection)
                new URL(ENDPOINT + key).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(20000);
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        if (code >= 400) throw new IllegalStateException("HTTP " + code);

        JSONObject resp = new JSONObject(sb.toString());
        JSONArray candidates = resp.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) throw new IllegalStateException("empty");
        JSONArray respParts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < respParts.length(); i++) {
            out.append(respParts.getJSONObject(i).optString("text", ""));
        }
        return out.toString().trim();
    }

    private static String systemPrompt() {
        return App.get().getString(R.string.gemini_system_prompt);
    }
}
