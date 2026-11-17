package com.samu.ai.ai;

import android.content.Context;

import com.samu.ai.R;
import com.samu.ai.SettingsStore;
import com.samu.ai.actions.PhoneActions;
import com.samu.ai.emergency.EmergencyManager;
import com.samu.ai.speech.CommandParser;
import com.samu.ai.speech.WakeWordListener;
import com.samu.ai.util.NetUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The single brain both the chat UI and the background voice service use.
 * Mode rules:
 *  - "Hey Samu"          => talk only, no phone actions (needsBoss commands are refused).
 *  - special name spoken => full control, but only after the boss face check passes.
 */
public class SamuBrain {

    public interface Responder {
        void say(String text);
    }

    public enum Trigger { CHAT_UI, HEY_SAMU, SPECIAL_NAME }

    private final Context ctx;
    private final PhoneActions actions;
    private final Responder responder;

    public SamuBrain(Context ctx, Responder responder) {
        this.ctx = ctx.getApplicationContext();
        this.responder = responder;
        this.actions = new PhoneActions(this.ctx, responder::say);
    }

    /** Entry point for every sentence Samu hears or reads. */
    public void think(Trigger trigger, String text) {
        String norm = WakeWordListener.normalize(text);

        // Emergency always works, no face check, no permission needed.
        if (hasAny(norm, "বাঁচাও", "বাঁচান", "help me", "emergency", "ইমার্জেন্সি")) {
            EmergencyManager.trigger(ctx, "voice");
            responder.say(ctx.getString(R.string.emergency_response));
            return;
        }

        CommandParser cmd = CommandParser.parse(text);
        boolean chatOnlyRequest = cmd != null && cmd.type != CommandParser.Type.CHAT;

        if (trigger == Trigger.HEY_SAMU && chatOnlyRequest) {
            if (cmd.needsBoss) {
                responder.say(ctx.getString(R.string.refuse_no_boss));
                return;
            }
            // Informative commands (weather/time/date) are fine in chat mode.
        }

        if (trigger == Trigger.SPECIAL_NAME && chatOnlyRequest && cmd.needsBoss) {
            // Boss identity already verified by the caller before reaching the brain.
        }

        if (cmd != null) {
            handleCommand(cmd, trigger);
            return;
        }

        // No command matched => conversation.
        respondChat(text);
    }

    private void handleCommand(CommandParser cmd, Trigger trigger) {
        switch (cmd.type) {
            case WEATHER:
                responder.say(ctx.getString(R.string.thinking));
                WeatherClient.fetch(cmd.arg, responder::say);
                return;
            case TIME:
                responder.say(new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(new Date()));
                return;
            case DATE:
                responder.say(new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("bn"))
                        .format(new Date()));
                return;
            case BATTERY:
                responder.say(batteryText());
                return;
            case TRANSLATE:
                responder.say(ctx.getString(R.string.thinking));
                GeminiClient.ask(cmd.arg, responder::say);
                return;
            default:
                break;
        }

        if (trigger == Trigger.HEY_SAMU && cmd.needsBoss) {
            responder.say(ctx.getString(R.string.refuse_no_boss));
            return;
        }

        String result = actions.run(cmd.type, cmd.arg);
        responder.say(result);
    }

    private void respondChat(String text) {
        if (!NetUtils.online(ctx) || !GeminiClient.hasKey()) {
            responder.say(offlineAnswer(WakeWordListener.normalize(text)));
            return;
        }
        responder.say(ctx.getString(R.string.thinking));
        GeminiClient.ask(text, answer -> {
            if (answer == null || answer.trim().isEmpty())
                answer = ctx.getString(R.string.ai_error);
            responder.say(answer);
        });
    }

    private String batteryText() {
        android.os.BatteryManager bm = (android.os.BatteryManager)
                ctx.getSystemService(Context.BATTERY_SERVICE);
        int pct = bm == null ? -1 : bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
        return String.format(Locale.getDefault(), ctx.getString(R.string.cmd_battery), pct);
    }

    private boolean hasAny(String t, String... keys) {
        for (String k : keys) if (t.contains(k)) return true;
        return false;
    }

    /** Small offline small-talk + fallback so Samu is never silent without internet. */
    private String offlineAnswer(String t) {
        if (hasAny(t, "কেমন আছ", "how are you")) return ctx.getString(R.string.off_how_are_you);
        if (hasAny(t, "তোমার নাম", "your name")) return ctx.getString(R.string.off_my_name);
        if (hasAny(t, "ধন্যবাদ", "thank")) return ctx.getString(R.string.off_thanks);
        if (hasAny(t, "হ্যালো", "hello", "হাই", "hi ")) return ctx.getString(R.string.off_hello);
        if (hasAny(t, "কে তুমি", "who are you")) return ctx.getString(R.string.off_who);
        return !NetUtils.online(ctx)
                ? ctx.getString(R.string.offline_notice)
                : ctx.getString(R.string.no_api_key);
    }
}
