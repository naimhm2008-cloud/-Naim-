package com.samu.ai.ai;

import com.samu.ai.App;
import com.samu.ai.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Weather via open-meteo.com — free, no API key needed.
 */
public class WeatherClient {

    public interface Callback {
        void onWeather(String text);
    }

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    public static void fetch(String place, Callback cb) {
        EXEC.execute(() -> {
            try {
                String city = place == null || place.trim().isEmpty() ? "Dhaka" : place.trim();
                String geo = "https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(city, "UTF-8") + "&count=1&language=bn";
                JSONObject g = new JSONObject(httpGet(geo));
                if (!g.has("results")) {
                    cb.onWeather(App.get().getString(R.string.weather_not_found));
                    return;
                }
                JSONObject loc = g.getJSONArray("results").getJSONObject(0);
                String name = loc.optString("name", city);
                double lat = loc.getDouble("latitude");
                double lon = loc.getDouble("longitude");

                String wx = "https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m";
                JSONObject w = new JSONObject(httpGet(wx)).getJSONObject("current");
                double temp = w.getDouble("temperature_2m");
                int code = w.optInt("weather_code", 0);
                double wind = w.optDouble("wind_speed_10m", 0);

                cb.onWeather(String.format(App.get().getString(R.string.weather_report),
                        name, Math.round(temp), describe(code), Math.round(wind)));
            } catch (Exception e) {
                cb.onWeather(App.get().getString(R.string.weather_error));
            }
        });
    }

    private static String describe(int code) {
        if (code == 0) return App.get().getString(R.string.wx_clear);
        if (code <= 3) return App.get().getString(R.string.wx_cloudy);
        if (code <= 48) return App.get().getString(R.string.wx_fog);
        if (code <= 67) return App.get().getString(R.string.wx_rain);
        if (code <= 77) return App.get().getString(R.string.wx_snow);
        if (code <= 82) return App.get().getString(R.string.wx_shower);
        if (code >= 95) return App.get().getString(R.string.wx_storm);
        return App.get().getString(R.string.wx_cloudy);
    }

    private static String httpGet(String url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(12000);
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
