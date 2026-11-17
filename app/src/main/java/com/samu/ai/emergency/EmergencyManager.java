package com.samu.ai.emergency;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import com.samu.ai.App;
import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.util.TtsManager;

/**
 * Emergency mode: auto-call the preset number, fall back to SMS with a live
 * location link. No face check here - in danger there is no time.
 */
public class EmergencyManager {

    private static long lastTrigger = 0;

    public static void trigger(Context ctx, String source) {
        long now = System.currentTimeMillis();
        if (now - lastTrigger < 20_000) return;
        lastTrigger = now;

        SettingsStore s = SettingsStore.get(ctx);
        String n1 = s.getEmergencyNumber1();
        String n2 = s.getEmergencyNumber2();

        TtsManager.get(ctx).speak(ctx.getString(R.string.emergency_response));

        if (!n1.isEmpty()) call(ctx, n1);
        else if (!n2.isEmpty()) call(ctx, n2);

        fetchLocationAndSms(ctx, n1.isEmpty() ? n2 : n1, n2.isEmpty() ? n1 : n2);
    }

    private static void call(Context ctx, String number) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + number));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            return;
        }
        Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            ctx.startActivity(i);
        } catch (Exception ignored) { }
    }

    private static void fetchLocationAndSms(Context ctx, String primary, String secondary) {
        LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            sendSms(ctx, primary, secondary, "");
            return;
        }
        try {
            String provider = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                sendSms(ctx, primary, secondary, "");
                return;
            }
            Location last = lm.getLastKnownLocation(provider);
            if (last != null) {
                sendSms(ctx, primary, secondary, link(last));
                return;
            }
            lm.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(Location location) {
                    sendSms(ctx, primary, secondary, link(location));
                }
                @Override public void onStatusChanged(String p, int st, Bundle ex) { }
                @Override public void onProviderEnabled(String p) { }
                @Override public void onProviderDisabled(String p) { }
            }, null);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    () -> sendSms(ctx, primary, secondary, ""), 8000);
        } catch (Exception e) {
            sendSms(ctx, primary, secondary, "");
        }
    }

    private static String link(Location l) {
        return "https://maps.google.com/?q=" + l.getLatitude() + "," + l.getLongitude();
    }

    private static void sendSms(Context ctx, String n1, String n2, String locationLink) {
        if (n1.isEmpty() && n2.isEmpty()) return;
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) return;
        String body = ctx.getString(R.string.emergency_sms_body) + " " + locationLink;
        SmsManager sm = Build.VERSION.SDK_INT >= 31
                ? ctx.getSystemService(SmsManager.class) : SmsManager.getDefault();
        if (sm == null) return;
        try {
            if (!n1.isEmpty()) sm.sendTextMessage(n1, null, body, null, null);
            if (!n2.isEmpty()) sm.sendTextMessage(n2, null, body, null, null);
        } catch (Exception ignored) { }
    }
}
