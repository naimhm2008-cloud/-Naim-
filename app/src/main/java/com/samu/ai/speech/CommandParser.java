package com.samu.ai.speech;

/**
 * Turns one spoken sentence into a structured command.
 * Understands Bengali + English keywords, works fully offline.
 */
public class CommandParser {

    public enum Type {
        CHAT, CALL, SMS, OPEN_APP, CLOSE_APP, ALARM, TIMER, FLASHLIGHT_ON, FLASHLIGHT_OFF,
        WIFI_ON, WIFI_OFF, BT_ON, BT_OFF, MUSIC_PLAY, MUSIC_PAUSE, VOLUME_UP, VOLUME_DOWN,
        VOLUME_MUTE, BRIGHTNESS_UP, BRIGHTNESS_DOWN, SCREENSHOT, LOCK_SCREEN, DND_ON, DND_OFF,
        WEATHER, TIME, DATE, EMERGENCY, TAKE_PHOTO, SHOW_GALLERY, TRANSLATE, BATTERY
    }

    /** true = needs the special name + boss face check, false = works with plain "Hey Samu". */
    public final boolean needsBoss;
    public final Type type;
    public final String arg;

    public CommandParser(boolean needsBoss, Type type, String arg) {
        this.needsBoss = needsBoss;
        this.type = type;
        this.arg = arg == null ? "" : arg;
    }

    /**
     * Parses the text spoken AFTER the wake phrase.
     * Returns null when nothing actionable was found (=> treat as chat).
     */
    public static CommandParser parse(String raw) {
        String t = WakeWordListener.normalize(raw);
        if (t.isEmpty()) return null;

        if (has(t, "বাঁচাও", "বাঁচান", "help me", "emergency", "ইমার্জেন্সি", "সাহায্য করো বিপদ"))
            return new CommandParser(false, Type.EMERGENCY, "");

        if (has(t, "কল দাও", "কল করো", "কল দাও", "ফোন করো", "call", "phone", "ডায়াল")) {
            String who = afterKeyword(t, "কে", "to", "call", "কল");
            return new CommandParser(true, Type.CALL, who);
        }
        if (has(t, "মেসেজ পাঠাও", "এসএমএস", "sms", "text", "মেসেজ দাও"))
            return new CommandParser(true, Type.SMS, t);

        if (has(t, "খোলো", "খুলে দাও", "ওপেন", "open", "লঞ্চ", "launch")) {
            String app = afterKeyword(t, "খোলো", "open", "launch", "খুলে দাও", "ওপেন");
            return new CommandParser(true, Type.OPEN_APP, app);
        }
        if (has(t, "বন্ধ করো", "ক্লোজ", "close", "বন্ধ করে দাও"))
            return new CommandParser(true, Type.CLOSE_APP, t);

        if (has(t, "অ্যালার্ম", "alarm", "জাগাও", "ঘুম থেকে জাগাও"))
            return new CommandParser(true, Type.ALARM, extractTime(t));
        if (has(t, "টাইমার", "timer"))
            return new CommandParser(true, Type.TIMER, extractTime(t));

        if (has(t, "টর্চ", "ফ্ল্যাশলাইট", "torch", "flashlight", "আলো জ্বালাও"))
            return new CommandParser(true, has(t, "নিভাও", "বন্ধ", "off") ? Type.FLASHLIGHT_OFF : Type.FLASHLIGHT_ON, "");

        if (has(t, "ওয়াইফাই", "wifi", "wi-fi", "ওয়াই ফাই"))
            return new CommandParser(true, has(t, "বন্ধ", "off", "নিভাও") ? Type.WIFI_OFF : Type.WIFI_ON, "");
        if (has(t, "ব্লুটুথ", "bluetooth"))
            return new CommandParser(true, has(t, "বন্ধ", "off") ? Type.BT_OFF : Type.BT_ON, "");

        if (has(t, "গান চালাও", "গান বাজাও", "মিউজিক", "music", "song", "play", "প্লে"))
            return new CommandParser(true, has(t, "থামাও", "বন্ধ", "pause", "stop") ? Type.MUSIC_PAUSE : Type.MUSIC_PLAY, "");
        if (has(t, "ইউটিউব", "youtube"))
            return new CommandParser(true, Type.OPEN_APP, "youtube");

        if (has(t, "ভলিউম", "volume", "সাউন্ড", "শব্দ")) {
            if (has(t, "বাড়াও", "বাড়ান", "up", "জোরে")) return new CommandParser(true, Type.VOLUME_UP, "");
            if (has(t, "কমাও", "কমান", "down", "আস্তে")) return new CommandParser(true, Type.VOLUME_DOWN, "");
            if (has(t, "সাইলেন্ট", "মিউট", "mute", "বন্ধ")) return new CommandParser(true, Type.VOLUME_MUTE, "");
            return new CommandParser(true, Type.VOLUME_UP, "");
        }

        if (has(t, "ব্রাইটনেস", "উজ্জ্বলতা", "brightness", "স্ক্রিনের আলো")) {
            if (has(t, "কমাও", "down", "কমান")) return new CommandParser(true, Type.BRIGHTNESS_DOWN, "");
            return new CommandParser(true, Type.BRIGHTNESS_UP, "");
        }

        if (has(t, "স্ক্রিনশট", "screenshot", "স্ক্রিন শট"))
            return new CommandParser(true, Type.SCREENSHOT, "");
        if (has(t, "স্ক্রিন লক", "লক করো", "lock screen", "লক করে দাও", "স্ক্রিন বন্ধ করো"))
            return new CommandParser(true, Type.LOCK_SCREEN, "");

        if (has(t, "ডিস্টার্ব", "dnd", "নয় ডিস্টার্ব", "সাইলেন্ট মোড"))
            return new CommandParser(true, has(t, "বন্ধ", "off") ? Type.DND_OFF : Type.DND_ON, "");

        if (has(t, "আবহাওয়া", "weather", "গরম কত", "বৃষ্টি হবে"))
            return new CommandParser(false, Type.WEATHER, extractPlace(t));
        if (has(t, "কয়টা বাজে", "সময় কত", "time", "ঘড়িতে কত"))
            return new CommandParser(false, Type.TIME, "");
        if (has(t, "আজ কি বার", "তারিখ", "date", "কাল কি বার"))
            return new CommandParser(false, Type.DATE, "");
        if (has(t, "ব্যাটারি", "battery", "চার্জ কত"))
            return new CommandParser(false, Type.BATTERY, "");

        if (has(t, "ছবি দেখাও", "গ্যালারি", "gallery", "photo", "ছবি খোলো"))
            return new CommandParser(true, Type.SHOW_GALLERY, "");
        if (has(t, "ছবি তুলো", "সেলফি", "take photo", "ক্যামেরা খোলো", "camera"))
            return new CommandParser(true, Type.TAKE_PHOTO, "");

        if (has(t, "অনুবাদ", "translate", "ইংরেজিতে বলো", "বাংলায় বলো"))
            return new CommandParser(false, Type.TRANSLATE, t);

        return null;
    }

    private static boolean has(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    private static String afterKeyword(String t, String... keys) {
        for (String k : keys) {
            int i = t.indexOf(k);
            if (i >= 0) {
                String rest = t.substring(i + k.length()).trim();
                if (!rest.isEmpty()) return rest;
            }
        }
        return "";
    }

    private static String extractTime(String t) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2})").matcher(t);
        if (m.find()) return m.group(1);
        return "";
    }

    private static String extractPlace(String t) {
        for (String k : new String[]{"আবহাওয়া", "weather", "এ", "in", "কেমন", "how is", "কত"}) {
            t = t.replace(k, " ");
        }
        return t.replaceAll("\\s+", " ").trim();
    }
}
