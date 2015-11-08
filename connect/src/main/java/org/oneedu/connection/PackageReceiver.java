package org.oneedu.connection;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

/**
 * Created by dongseok0 on 19/06/15.
 *
 * 1. Do restart proxy service.
 * 2. Uninstall and install AppUniverse 2.0 that changed as system application.
 */
public class PackageReceiver extends BroadcastReceiver {
    private final String TAG = "PackageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, action);
        Log.d(TAG, "Version : " + BuildConfig.VERSION_CODE);

        switch (action) {
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                Intent i = new Intent("org.oneedu.connection.PROXY");
                i.setComponent(new ComponentName("org.oneedu.connection", "org.oneedu.connectservice.ProxyService"));
                context.startService(i);
                checkAppUniverse(context);
                break;
        }
    }

    private void checkAppUniverse(Context context) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.putExtra(InstallerService.EXTRA_ASSETS_APK, "AppUniverse_v2.0.apk");

        try {
            // Skip if current version is higher than 2.0.0
            PackageInfo info = context.getPackageManager().getPackageInfo("org.oneedu.appuniverse", 0);
            if (info.versionCode >= 200) {
                return;
            }
            // Uninstall and install if find under 2.0 version.
            intent.setAction(InstallerService.ACTION_REINSTALL);
            intent.putExtra(InstallerService.EXTRA_PACKAGE_NAME, "org.oneedu.appuniverse");
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "App Universe is not installed.");
            intent.setAction(InstallerService.ACTION_INSTALL);
        }
        intent.setComponent(new ComponentName("org.oneedu.connection", "org.oneedu.connection.InstallerService"));
        context.startService(intent);
    }
}