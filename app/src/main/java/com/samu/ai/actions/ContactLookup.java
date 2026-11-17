package com.samu.ai.actions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Finds a phone number / installed app from a spoken (Bengali or English) name.
 */
public class ContactLookup {

    private static final Map<String, String> DIGIT_WORDS = new HashMap<>();

    static {
        DIGIT_WORDS.put("শূন্য", "0"); DIGIT_WORDS.put("এক", "1"); DIGIT_WORDS.put("দুই", "2");
        DIGIT_WORDS.put("তিন", "3"); DIGIT_WORDS.put("চার", "4"); DIGIT_WORDS.put("পাঁচ", "5");
        DIGIT_WORDS.put("ছয়", "6"); DIGIT_WORDS.put("সাত", "7"); DIGIT_WORDS.put("আট", "8");
        DIGIT_WORDS.put("নয়", "9"); DIGIT_WORDS.put("zero", "0"); DIGIT_WORDS.put("one", "1");
        DIGIT_WORDS.put("two", "2"); DIGIT_WORDS.put("three", "3"); DIGIT_WORDS.put("four", "4");
        DIGIT_WORDS.put("five", "5"); DIGIT_WORDS.put("six", "6"); DIGIT_WORDS.put("seven", "7");
        DIGIT_WORDS.put("eight", "8"); DIGIT_WORDS.put("nine", "9");
    }

    /** Returns a raw number if the text contains one, else looks in contacts. */
    public static String resolve(Context ctx, String text) {
        if (text == null) return "";
        String t = text.trim();
        String digits = t.replaceAll("[^0-9+]", "");
        if (digits.replaceAll("[^0-9]", "").length() >= 6) return digits;
        String wordDigits = replaceDigitWords(t.toLowerCase(Locale.ROOT))
                .replaceAll("[^0-9+]", "");
        if (wordDigits.replaceAll("[^0-9]", "").length() >= 6) return wordDigits;
        return lookupContact(ctx, t);
    }

    public static String replaceDigitWords(String t) {
        String out = t;
        for (Map.Entry<String, String> e : DIGIT_WORDS.entrySet()) {
            out = out.replace(e.getKey(), " " + e.getValue() + " ");
        }
        return out;
    }

    public static String lookupContact(Context ctx, String name) {
        if (name == null || name.trim().isEmpty()) return "";
        if (ContextCompat_check(ctx)) return "";
        try (Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name.trim() + "%"}, null)) {
            if (c != null && c.moveToFirst()) {
                return c.getString(0).replaceAll("\\s+", "");
            }
        } catch (Exception ignored) { }
        return "";
    }

    private static boolean ContextCompat_check(Context ctx) {
        return androidx.core.content.ContextCompat.checkSelfPermission(ctx,
                android.Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED;
    }
}
