package org.oneedu.connection.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.dongseok0.library.wifi.Wifi;
import org.oneedu.connection.controllers.WifiDialogController;
import org.oneedu.connection.data.ProxyDB;
import org.sandrop.webscarab.model.Preferences;
import org.sandrop.webscarab.model.StoreException;
import org.sandrop.webscarab.plugin.Framework;
import org.sandrop.webscarab.plugin.proxy.Proxy;
import org.sandroproxy.utils.NetworkHostNameResolver;
import org.sandroproxy.utils.PreferenceUtils;
import org.sandroproxy.utils.network.ClientResolver;
import org.sandroproxy.utils.pac.ProxyEvaluationException;
import org.sandroproxy.webscarab.store.sql.SqlLiteStore;

import java.io.File;
import java.io.IOException;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class ProxyService extends Service {

    public static final String ACTION_FAILED_START_PROXY = "action.failed.start.proxy";
    public static final String ACTION_PROXY_STARTED = "action.proxy.started";
    public static final int PAC_ERROR_CODE = 1000;
    public static final int ERROR_CODE = 1001;

    private final String tag = "ProxyService";
    private boolean proxyStarted;
    private Framework framework;
    private NetworkHostNameResolver networkHostNameResolver;
    private ClientResolver clientResolver;
    private Context mContext;
    private ProxyDB proxyDB;
    private WifiManager mWifiManager;
    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate() {
        super.onCreate();

        mContext = getApplicationContext();
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        proxyDB = ProxyDB.getInstance(mContext);

        Preferences.init(mContext);
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putBoolean(PreferenceUtils.chainProxyEnabled, true).commit();
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putString(PreferenceUtils.proxyPort, "9008").commit();

        framework = new Framework(mContext);
        setStore(getApplicationContext());
        networkHostNameResolver = new NetworkHostNameResolver(mContext);
        clientResolver = new ClientResolver(mContext);
        Proxy proxy = new Proxy(framework, networkHostNameResolver, clientResolver);
        framework.addPlugin(proxy);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (framework != null){
            framework.stop();
        }
        if (networkHostNameResolver != null){
            networkHostNameResolver.cleanUp();
        }
        networkHostNameResolver = null;
        framework = null;
        proxyStarted = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(tag, "Received start id " + startId + ": " + intent);

        NetworkInfo networkInfo;
        WifiInfo wifiInfo;
        if (intent == null) {  // Network connected and restarting service
            networkInfo = connectivityManager.getActiveNetworkInfo();
            wifiInfo = mWifiManager.getConnectionInfo();
        } else {
            networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
        }

        if (networkInfo == null) {
            Log.d(tag, "networkInfo is null!");
            toggleProxy(null);
        } else {
            NetworkInfo.State state = networkInfo.getState();
            Log.d(tag, "networkInfo.state : " + state);
            if (state == NetworkInfo.State.DISCONNECTED) {
                toggleProxy(null);
            } else if (state == NetworkInfo.State.CONNECTED) {
                toggleProxy(wifiInfo.getSSID());
            }
        }

        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends Binder {
        public ProxyService getService() {
            return ProxyService.this;
        }
    }

    public WifiConfiguration addOrUpdateProxy(WifiDialogController controller) {
        WifiConfiguration config = controller.getConfig();
        String ssid = config.SSID;

        if (controller.isProxyEnabled()) {

            if (controller.isUsePac()) {
                proxyDB.addOrUpdateProxy(ssid, controller.getPacURL(), controller.getProxyUsername(), controller.getProxyPassword());
                Wifi.getWifiConfigurationHelper().setProxyFields(config, "localhost", "9008", "");
                return config;
            } else if (controller.getProxyHost().length() > 0) {

                String username = controller.getProxyUsername();
                String password = controller.getProxyPassword();

                if (username.length() > 0 && password.length() > 0) {
                    proxyDB.addOrUpdateProxy(ssid, controller.getProxyHost(), Integer.parseInt(controller.getProxyPort()), username, password);
                    Wifi.getWifiConfigurationHelper().setProxyFields(config, "localhost", "9008", "");
                    return config;
                } else {
                    Wifi.getWifiConfigurationHelper().setProxyFields(config, controller.getProxyHost(), controller.getProxyPort(), "");
                }
            }
        }

        proxyDB.deleteProxy(ssid);

        return config;
    }

    public void toggleProxy(String ssid) {
        Log.d(tag, "toggleProxy SSID: " + ssid);
        org.oneedu.connection.data.Proxy proxy = null;
        if (ssid != null) {
            proxy = proxyDB.getProxy(ssid);
        }

        if (!proxyStarted && proxy != null) {
            String host = proxy.getHost();
            int port = proxy.getPort();
            String username = proxy.getUsername();
            String password = proxy.getPassword();
            String pacUrl = proxy.getPacUrl();

            boolean usePac = pacUrl != null && pacUrl.length() > 0;

            Preferences.setPreference(PreferenceUtils.chainProxyUsername, "\\" + username);
            Preferences.setPreference(PreferenceUtils.chainProxyPassword, password);
            Preferences.setPreference(PreferenceUtils.chainProxyHttp, usePac ? "" : host + ":" + port);
            Preferences.setPreference(PreferenceUtils.chainProxyHttps, usePac ? "" : host + ":" + port);
            Preferences.setPreference(PreferenceUtils.chainProxyPacUrl, pacUrl);

            // start
            Thread thread = new Thread()
            {
                @Override
                public void run() {

                    try {
                        Log.d(tag, "Configuring HTTPClient");
                        framework.configureHTTPClient(mContext);

                        Log.d(tag, "Starting proxy");
                        proxyStarted = true;
                        framework.start();
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(ACTION_PROXY_STARTED));
                    } catch (ProxyEvaluationException | IOException e) {
                        Intent i = new Intent(ACTION_FAILED_START_PROXY);
                        i.putExtra("ErrorCode", PAC_ERROR_CODE);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
                    } catch (Exception e) {
                        Intent i = new Intent(ACTION_FAILED_START_PROXY);
                        i.putExtra("ErrorCode", ERROR_CODE);
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(i);
                    }
                }
            };
            thread.setName("Starting proxy");
            thread.start();
        } else if (proxyStarted && proxy == null) {
            //stop
            Thread thread = new Thread()
            {
                @Override
                public void run() {
                    Log.d(tag, "Stopping proxy");
                    if (framework != null){
                        framework.stop();
                    }
                    proxyStarted = false;
                }
            };
            thread.setName("Stopping proxy");
            thread.start();
        }

    }

    private void setStore(Context context){
        if (framework != null){
            try {
                File file =  PreferenceUtils.getDataStorageDir(context);
                if (file != null){
                    File rootDir = new File(file.getAbsolutePath() + "/content");
                    if (!rootDir.exists()){
                        rootDir.mkdir();
                    }
                    framework.setSession("Database", SqlLiteStore.getInstance(context, rootDir.getAbsolutePath()), "");
                }
            } catch (StoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


}
