package com.czbix.xposed.mediaindicator;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastReceiver extends BroadcastReceiver {
    public static final String INTENT_ACTION = BuildConfig.APPLICATION_ID + ".intent.action.TOAST";

    public static final String KEY_PKG_NAME = "pkg_name";
    public static final String KEY_MSG = "msg";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String pkgName = intent.getStringExtra(KEY_PKG_NAME);
        final String msg = intent.getStringExtra(KEY_MSG);

        CharSequence label;
        try {
            final PackageManager packageManager = context.getPackageManager();
            final ApplicationInfo appInfo = packageManager.getApplicationInfo(pkgName, 0);
            label = packageManager.getApplicationLabel(appInfo);
        } catch (PackageManager.NameNotFoundException e) {
            // ignore it
            return;
        }

        final String message = String.format("%s %s", label, msg);

        final Notification.Builder builder = new Notification.Builder(context);
        final Notification notification = builder.setAutoCancel(true)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("Media Indicator")
                .setContentText(message)
                .setLocalOnly(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .build();

        final NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, notification);
    }
}
