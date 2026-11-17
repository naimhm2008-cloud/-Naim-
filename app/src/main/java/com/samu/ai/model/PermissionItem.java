package com.samu.ai.model;

import android.content.Context;

import com.samu.ai.R;

/**
 * One row of the permission screen (24 items).
 */
public class PermissionItem {

    public interface Checker {
        boolean has(Context ctx);
    }

    public interface Requester {
        void request(androidx.fragment.app.Fragment f, Context ctx);
    }

    public final int id;
    public final String title;
    public final String reason;
    public final Checker checker;
    public final Requester requester;

    public PermissionItem(int id, String title, String reason, Checker checker, Requester requester) {
        this.id = id;
        this.title = title;
        this.reason = reason;
        this.checker = checker;
        this.requester = requester;
    }

    public int stringTitle(Context ctx) {
        return ctx.getResources().getIdentifier("perm_title_" + id, "string", ctx.getPackageName());
    }

    public int stringReason(Context ctx) {
        return ctx.getResources().getIdentifier("perm_reason_" + id, "string", ctx.getPackageName());
    }
}
