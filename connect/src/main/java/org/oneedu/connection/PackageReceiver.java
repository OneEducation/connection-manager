package org.oneedu.connection;

import android.content.BroadcastReceiver;
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
                context.startService(i);
                checkAppUniverse(context);
                break;
        }
    }

    private void checkAppUniverse(Context context) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.putExtra(InstallerService.EXTRA_ASSETS_APK, "AppUniverse_v2.1.apk");

        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("org.oneedu.appuniverse", 0);
            if (info.versionCode < 200) {
                // Uninstall and install if find under 2.0 version.
                intent.setAction(InstallerService.ACTION_REINSTALL);
            } else if (info.versionCode < 210) {
                intent.setAction(InstallerService.ACTION_INSTALL);
            } else {
                // Skip if current version is higher than 2.1.0
                return;
            }

            intent.putExtra(InstallerService.EXTRA_PACKAGE_NAME, "org.oneedu.appuniverse");
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "App Universe is not installed.");
            intent.setAction(InstallerService.ACTION_INSTALL);
        }

        context.startService(intent);
    }
}