package org.oneedu.connection;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.oneedu.connection.installer.Installer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dongseok0 on 10/07/15.
 *
 */
public class InstallerService extends IntentService {
    private final String TAG = "InstallerService";

    public static final String ACTION_REINSTALL = "InstallerService.Reinstall";
    public static final String ACTION_INSTALL = "InstallerService.Install";
    public static final String ACTION_UNINSTALL = "InstallerService.Uninstall";
    public static final String EXTRA_PACKAGE_NAME = "Extra.PackageName";
    public static final String EXTRA_APK_URL = "Extra.Apk.URL";
    public static final String EXTRA_ASSETS_APK = "Extra.Assets.Apk";

    public InstallerService() {
        super("InstallerService");
    }

    class ReInstallCallback implements Installer.InstallerCallback {
        private Intent intent;

        public ReInstallCallback(Intent intent) {
            this.intent = intent;
        }

        @Override
        public void onSuccess(int operation) {
            Intent install = new Intent(getApplicationContext(), InstallerService.class);
            install.setAction(ACTION_INSTALL);
            install.putExtras(intent);
            install.setComponent(new ComponentName("org.oneedu.connection", "org.oneedu.connection.InstallerService"));
            startService(install);
        }

        @Override
        public void onError(int operation, int errorCode) {

        }
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        Log.d(TAG, intent.toString());

        switch (intent.getAction()) {
            case ACTION_REINSTALL: {
                try {
                    Installer.getUnattendedInstaller(this, getPackageManager(), new ReInstallCallback(intent))
                            .deletePackage(intent.getStringExtra(EXTRA_PACKAGE_NAME));
                } catch (Installer.AndroidNotCompatibleException e) {
                    e.printStackTrace();
                }
                break;
            }

            case ACTION_INSTALL:
                try {
                    String filePath = null;
                    String assetApk = intent.getStringExtra(EXTRA_ASSETS_APK);
                    if (assetApk != null) {
                        filePath = copyApk(assetApk);
                    }

                    if (filePath == null) {
                        filePath = intent.getStringExtra(EXTRA_APK_URL);
                    }

                    if (filePath == null) {
                        Log.d(TAG, "Nothing to install!");
                        return;
                    }

                    final File apk = new File(filePath);
                    if (!apk.exists()) {
                        Log.d(TAG, apk + " is not exist!!");
                        return;
                    }

                    Installer.getUnattendedInstaller(this, getPackageManager(), new Installer.InstallerCallback() {
                        @Override
                        public void onSuccess(int operation) {
                            apk.delete();
                        }

                        @Override
                        public void onError(int operation, int errorCode) {

                        }
                    }).installPackage(apk);
                } catch (Installer.AndroidNotCompatibleException e) {
                    e.printStackTrace();
                }
                break;

            case ACTION_UNINSTALL:
                try {
                    Installer.getUnattendedInstaller(this, getPackageManager(), new Installer.InstallerCallback() {
                        @Override
                        public void onSuccess(int operation) {

                        }

                        @Override
                        public void onError(int operation, int errorCode) {

                        }
                    }).deletePackage(intent.getStringExtra(EXTRA_PACKAGE_NAME));
                } catch (Installer.AndroidNotCompatibleException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Nullable
    private String copyApk(String uri) {
        Log.d(TAG, "Copying asset " + uri + " to temporary");
        AssetManager assetManager = getAssets();

        try {
            String tempPath = getExternalCacheDir() + "/temp.apk";
            InputStream in = assetManager.open(uri);
            OutputStream out = new FileOutputStream(tempPath);

            byte[] buffer = new byte[1024];

            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            in.close();

            out.flush();
            out.close();
            return tempPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
