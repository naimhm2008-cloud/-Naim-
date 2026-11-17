package com.samu.ai.ui;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.samu.ai.R;
import com.samu.ai.admin.SamuDeviceAdminReceiver;
import com.samu.ai.model.PermissionItem;
import com.samu.ai.util.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * All 24 Samu permissions, one row each, with live status and a request button.
 */
public class PermissionsFragment extends Fragment {

    public PermissionsFragment() {
        super(R.layout.fragment_permissions);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View v, @androidx.annotation.Nullable Bundle s) {
        super.onViewCreated(v, s);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.permission_list);
        rv.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        rv.setAdapter(new PermissionAdapter(items(), this));
    }

    @Override
    public void onResume() {
        super.onResume();
        androidx.recyclerview.widget.RecyclerView rv =
                requireView().findViewById(R.id.permission_list);
        androidx.recyclerview.widget.RecyclerView.Adapter<?> a = rv.getAdapter();
        if (a != null) a.notifyDataSetChanged();
    }

    private List<PermissionItem> items() {
        Context c = requireContext();
        List<PermissionItem> list = new ArrayList<>();

        list.add(new PermissionItem(1, str(c, R.string.perm_title_1), str(c, R.string.perm_reason_1),
                PermissionUtils::mic, (f, ctx) -> requestRuntime(ctx, f, Manifest.permission.RECORD_AUDIO)));

        list.add(new PermissionItem(2, str(c, R.string.perm_title_2), str(c, R.string.perm_reason_2),
                PermissionUtils::camera, (f, ctx) -> requestRuntime(ctx, f, Manifest.permission.CAMERA)));

        list.add(new PermissionItem(3, str(c, R.string.perm_title_3), str(c, R.string.perm_reason_3),
                PermissionUtils::call, (f, ctx) -> requestRuntime(ctx, f, Manifest.permission.CALL_PHONE)));

        list.add(new PermissionItem(4, str(c, R.string.perm_title_4), str(c, R.string.perm_reason_4),
                PermissionUtils::contacts, (f, ctx) -> requestRuntime(ctx, f, Manifest.permission.READ_CONTACTS)));

        list.add(new PermissionItem(5, str(c, R.string.perm_title_5), str(c, R.string.perm_reason_5),
                PermissionUtils::sms, (f, ctx) -> requestRuntime(ctx, f, Manifest.permission.SEND_SMS)));

        list.add(new PermissionItem(6, str(c, R.string.perm_title_6), str(c, R.string.perm_reason_6),
                PermissionUtils::gallery, (f, ctx) -> {
                    if (Build.VERSION.SDK_INT >= 33)
                        requestRuntime(ctx, f, Manifest.permission.READ_MEDIA_IMAGES);
                    else
                        requestRuntime(ctx, f, Manifest.permission.READ_EXTERNAL_STORAGE);
                }));

        list.add(new PermissionItem(7, str(c, R.string.perm_title_7), str(c, R.string.perm_reason_7),
                PermissionUtils::location, (f, ctx) -> requestRuntime(ctx, f,
                        Manifest.permission.ACCESS_FINE_LOCATION)));

        list.add(new PermissionItem(8, str(c, R.string.perm_title_8), str(c, R.string.perm_reason_8),
                PermissionUtils::backgroundLocation, (f, ctx) -> {
                    if (Build.VERSION.SDK_INT >= 29)
                        requestRuntime(ctx, f, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                    else
                        requestRuntime(ctx, f, Manifest.permission.ACCESS_FINE_LOCATION);
                }));

        list.add(new PermissionItem(9, str(c, R.string.perm_title_9), str(c, R.string.perm_reason_9),
                PermissionUtils::notifications, (f, ctx) -> {
                    if (Build.VERSION.SDK_INT >= 33)
                        requestRuntime(ctx, f, Manifest.permission.POST_NOTIFICATIONS);
                }));

        list.add(new PermissionItem(10, str(c, R.string.perm_title_10), str(c, R.string.perm_reason_10),
                PermissionUtils::overlay, (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + ctx.getPackageName()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(11, str(c, R.string.perm_title_11), str(c, R.string.perm_reason_11),
                PermissionUtils::accessibility, (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(12, str(c, R.string.perm_title_12), str(c, R.string.perm_reason_12),
                PermissionUtils::batteryIgnored, (f, ctx) -> {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + ctx.getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        ctx.startActivity(i);
                    } catch (Exception e) {
                        ctx.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                }));

        list.add(new PermissionItem(13, str(c, R.string.perm_title_13), str(c, R.string.perm_reason_13),
                PermissionUtils::deviceAdmin, (f, ctx) -> {
                    Intent i = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    i.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(ctx,
                            SamuDeviceAdminReceiver.class));
                    i.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            ctx.getString(R.string.perm_reason_13));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivity(i);
                }));

        list.add(new PermissionItem(14, str(c, R.string.perm_title_14), str(c, R.string.perm_reason_14),
                PermissionUtils::bluetooth, (f, ctx) -> {
                    if (Build.VERSION.SDK_INT >= 31)
                        requestRuntime(ctx, f, Manifest.permission.BLUETOOTH_CONNECT);
                    else
                        requestRuntime(ctx, f, Manifest.permission.BLUETOOTH);
                }));

        list.add(new PermissionItem(15, str(c, R.string.perm_title_15), str(c, R.string.perm_reason_15),
                PermissionUtils::usageStats, (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(16, str(c, R.string.perm_title_16), str(c, R.string.perm_reason_16),
                PermissionUtils::writeSettings, (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:" + ctx.getPackageName()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(17, str(c, R.string.perm_title_17), str(c, R.string.perm_reason_17),
                PermissionUtils::exactAlarm, (f, ctx) -> {
                    if (Build.VERSION.SDK_INT >= 31)
                        ctx.startActivity(new Intent(
                                android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:" + ctx.getPackageName()))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }));

        list.add(new PermissionItem(18, str(c, R.string.perm_title_18), str(c, R.string.perm_reason_18),
                PermissionUtils::dndAccess, (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(19, str(c, R.string.perm_title_19), str(c, R.string.perm_reason_19),
                (ctx) -> true, (f, ctx) -> { }));

        list.add(new PermissionItem(20, str(c, R.string.perm_title_20), str(c, R.string.perm_reason_20),
                (ctx) -> com.samu.ai.util.NetUtils.online(ctx), (f, ctx) ->
                        ctx.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))));

        list.add(new PermissionItem(21, str(c, R.string.perm_title_21), str(c, R.string.perm_reason_21),
                (ctx) -> com.samu.ai.service.SamuForegroundService.isRunning(), (f, ctx) ->
                        com.samu.ai.service.SamuForegroundService.start(ctx)));

        list.add(new PermissionItem(22, str(c, R.string.perm_title_22), str(c, R.string.perm_reason_22),
                PermissionUtils::batteryIgnored, (f, ctx) -> {
                    Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + ctx.getPackageName()));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        ctx.startActivity(i);
                    } catch (Exception ignored) { }
                }));

        list.add(new PermissionItem(23, str(c, R.string.perm_title_23), str(c, R.string.perm_reason_23),
                (ctx) -> true, (f, ctx) -> { }));

        list.add(new PermissionItem(24, str(c, R.string.perm_title_24), str(c, R.string.perm_reason_24),
                (ctx) -> PermissionUtils.mic(ctx) || PermissionUtils.gallery(ctx), (f, ctx) ->
                        requestRuntime(ctx, f, Manifest.permission.RECORD_AUDIO)));

        return list;
    }

    private static String str(Context c, int res) {
        return c.getString(res);
    }

    private void requestRuntime(Context ctx, Fragment f, String perm) {
        if (ContextCompat.checkSelfPermission(ctx, perm)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        ActivityCompat.requestPermissions(f.requireActivity(), new String[]{perm}, 100 + f.getId());
    }
}
