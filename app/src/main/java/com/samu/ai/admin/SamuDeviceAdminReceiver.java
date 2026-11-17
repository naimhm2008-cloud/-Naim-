package com.samu.ai.admin;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

import com.samu.ai.R;

/**
 * Device admin - only used for "lock the screen" on voice command.
 */
public class SamuDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return context.getString(R.string.device_admin_disable_warn);
    }
}
