package org.oneedu.connectservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.*;
import android.net.wifi.WifiInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dongseok0 on 24/03/15.
 */
public class WifiService extends Service {
    private static final String tag = "WifiService";
    private IntentFilter mFilter;
    private BroadcastReceiver mReceiver;
    private Scanner mScanner;
    private WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;

    // Combo scans can take 5-6s to complete - set to 10s.
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;
    private WifiInfo                    mLastInfo;
    private NetworkInfo.DetailedState   mLastState;
    private ArrayList<AccessPoint>      mAPList = new ArrayList<AccessPoint>();
    private AtomicBoolean               mConnected = new AtomicBoolean(false);
    private OnUpdateAccessPointListener mOnAPUpdateListener;
    private onUpdateConnectionStateListener mOnUpdateConnectionStateListener;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver(mReceiver);
    }

    public ArrayList<AccessPoint> getAPList() {
        return mAPList;
    }

    public interface OnUpdateAccessPointListener {
        abstract void onUpdateAPListener(ArrayList<AccessPoint> apns);
    }

    public void setOnUpdateAccessPointListener(OnUpdateAccessPointListener onUpdateAccessPointListener) {
        mOnAPUpdateListener = onUpdateAccessPointListener;
        if (mOnAPUpdateListener != null && mAPList.size() > 0) {
            mOnAPUpdateListener.onUpdateAPListener(mAPList);
        }
    }

    public interface onUpdateConnectionStateListener {
        abstract void onUpdateConnectionStateChanged(WifiInfo wifiInfo, NetworkInfo.DetailedState state, int supplicantError);
    }

    public void setOnUpdateConnectionStateListener(onUpdateConnectionStateListener onUpdateConnectionStateListener) {
        mOnUpdateConnectionStateListener = onUpdateConnectionStateListener;
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
        public WifiService getService() {
            return WifiService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.LINK_CONFIGURATION_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        getApplicationContext().registerReceiver(mReceiver, mFilter);
        //updateAccessPoints();

        mScanner = new Scanner();

        mConnectListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {

            }
            @Override
            public void onFailure(int reason) {
                Context context = getApplicationContext();
                if (context != null) {
                    Toast.makeText(context, R.string.wifi_failed_connect_message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {

            }
            @Override
            public void onFailure(int reason) {
                Context context = getApplicationContext();
                if (context != null) {
                    Toast.makeText(context, R.string.wifi_failed_save_message, Toast.LENGTH_SHORT).show();
                }
            }
        };

        mForgetListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
                Context context = getApplicationContext();
                if (context != null) {
                    Toast.makeText(context, R.string.wifi_failed_forget_message, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    public void forget(int networkId) {
        mWifiManager.forget(networkId, mForgetListener);
    }

    public void connect(int networkId) {
        mWifiManager.connect(networkId, mConnectListener);
    }

    public void connect(WifiConfiguration config) {
        mWifiManager.connect(config, mConnectListener);
    }

    public void save(WifiConfiguration config) {
        mWifiManager.save(config, mSaveListener);
    }

    public void updateAndReconnect(final WifiConfiguration config) {

        for (WifiConfiguration wifi : mWifiManager.getConfiguredNetworks()) {
            if (wifi.SSID.equals(config.SSID) && wifi.status == WifiConfiguration.Status.CURRENT) {
                // network connected right after dialog open, in that case we don't have networkID but connected.
                config.networkId = wifi.networkId;
                mWifiManager.disableNetwork(config.networkId);
                mWifiManager.updateNetwork(config);
                mWifiManager.saveConfiguration();

                for (WifiConfiguration updatedWifi : mWifiManager.getConfiguredNetworks()) {
                    if (updatedWifi.SSID.equals(config.SSID)) {
                        connect(updatedWifi.networkId);
                        return;
                    }
                }

                Log.d("WifiService", "Shouldn't be here!!!");
            }
        }

        Log.d("WifiService", "Not found " + config.SSID + " so just connect!");
        connect(config);
    }

    private void handleEvent(Context context, Intent intent) {
        Log.d(tag, intent.toString());
        String action = intent.getAction();
        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            updateWifiState(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
        } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) ||
                WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION.equals(action) ||
                WifiManager.LINK_CONFIGURATION_CHANGED_ACTION.equals(action)) {
            updateAccessPoints();
        } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
            //Ignore supplicant state changes when network is connected
            //TODO: we should deprecate SUPPLICANT_STATE_CHANGED_ACTION and
            //introduce a broadcast that combines the supplicant and network
            //network state change events so the apps dont have to worry about
            //ignoring supplicant state change when network is connected
            //to get more fine grained information.
            SupplicantState state = (SupplicantState) intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            if (!mConnected.get() && SupplicantState.isHandshakeState(state)) {
                updateConnectionState(android.net.wifi.WifiInfo.getDetailedStateOf(state));
            } else {
                // During a connect, we may have the supplicant
                // state change affect the detailed network state.
                // Make sure a lost connection is updated as well.
                updateConnectionState(null);
            }

            if (mOnUpdateConnectionStateListener != null) {
                int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);

                mOnUpdateConnectionStateListener.onUpdateConnectionStateChanged(mWifiManager.getConnectionInfo(),
                        android.net.wifi.WifiInfo.getDetailedStateOf(state), supplicantError);
            }

        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            mConnected.set(info.isConnected());
            updateAccessPoints();
            updateConnectionState(info.getDetailedState());

            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            if (mOnUpdateConnectionStateListener != null && wifiInfo != null) {
                mOnUpdateConnectionStateListener.onUpdateConnectionStateChanged(wifiInfo,
                        info.getDetailedState(), 0);
            }

        } else if (WifiManager.RSSI_CHANGED_ACTION.equals(action)) {
            // dongseok : Do not re-construct ap list to prevent too frequent updating
            //updateConnectionState(null);
        }
    }

    private void updateWifiState(int state) {

        switch (state) {
            case WifiManager.WIFI_STATE_ENABLED:
                mScanner.resume();
                return; // not break, to avoid the call to pause() below

            case WifiManager.WIFI_STATE_ENABLING:
//                addMessagePreference(R.string.wifi_starting);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
//                setOffMessage();
                break;
        }

        mLastInfo = null;
        mLastState = null;
        mScanner.pause();
    }

    private void updateConnectionState(NetworkInfo.DetailedState state) {
        /* sticky broadcasts can call this when wifi is disabled */
        if (!mWifiManager.isWifiEnabled()) {
            mScanner.pause();
            return;
        }

        if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
            mScanner.pause();
        } else {
            mScanner.resume();
        }

        mLastInfo = mWifiManager.getConnectionInfo();
        if (state != null) {
            mLastState = state;
        }

        for (AccessPoint accessPoint : mAPList) {
            // Maybe there's a WifiConfigPreference
            accessPoint.update(mLastInfo, mLastState);
        }

        if (mOnAPUpdateListener != null) {
            mOnAPUpdateListener.onUpdateAPListener(mAPList);
        }
    }

    /**
     * Shows the latest access points available with supplimental information like
     * the strength of network and the security for it.
     */
    public void updateAccessPoints() {

        final int wifiState = mWifiManager.getWifiState();

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                // AccessPoints are automatically sorted with TreeSet.
                mAPList = constructAccessPoints();
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                //getPreferenceScreen().removeAll();
                mAPList.clear();
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                //addMessagePreference(R.string.wifi_stopping);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                //setOffMessage();
                break;
        }

        if (mOnAPUpdateListener != null) {
            mOnAPUpdateListener.onUpdateAPListener(mAPList);
        }
    }

    /** Returns sorted list of access points */
    private ArrayList<AccessPoint> constructAccessPoints() {
        ArrayList<AccessPoint> accessPoints = new ArrayList<AccessPoint>();
        /** Lookup table to more quickly update AccessPoints by only considering objects with the
         * correct SSID.  Maps SSID -> List of AccessPoints with the given SSID.  */
        Multimap<String, AccessPoint> apMap = new Multimap<String, AccessPoint>();

        final List<WifiConfiguration> configs = mWifiManager.getConfiguredNetworks();
        if (configs != null) {
            for (WifiConfiguration config : configs) {
                AccessPoint accessPoint = new AccessPoint(getApplicationContext(), config);
                accessPoint.update(mLastInfo, mLastState);
                accessPoints.add(accessPoint);
                apMap.put(accessPoint.ssid, accessPoint);
            }
        }

        final List<ScanResult> results = mWifiManager.getScanResults();
        if (results != null) {
            for (ScanResult result : results) {
                // Ignore hidden and ad-hoc networks.
                if (result.SSID == null || result.SSID.length() == 0 ||
                        result.capabilities.contains("[IBSS]")) {
                    continue;
                }

                boolean found = false;
                for (AccessPoint accessPoint : apMap.getAll(result.SSID)) {
                    if (accessPoint.update(result))
                        found = true;
                }
                if (!found) {
                    AccessPoint accessPoint = new AccessPoint(getApplicationContext(), result);
                    accessPoints.add(accessPoint);
                    apMap.put(accessPoint.ssid, accessPoint);
                }
            }
        }

        // Pre-sort accessPoints to speed preference insertion
        Collections.sort(accessPoints);
        return accessPoints;
    }

    /** A restricted multimap for use in constructAccessPoints */
    private class Multimap<K,V> {
        private HashMap<K,List<V>> store = new HashMap<K,List<V>>();
        /** retrieve a non-null list of values with key K */
        List<V> getAll(K key) {
            List<V> values = store.get(key);
            return values != null ? values : Collections.<V>emptyList();
        }

        void put(K key, V val) {
            List<V> curVals = store.get(key);
            if (curVals == null) {
                curVals = new ArrayList<V>(3);
                store.put(key, curVals);
            }
            curVals.add(val);
        }
    }

    public void setWifiStatusListener() {

    }

    private class Scanner extends Handler {
        private int mRetry = 0;

        void resume() {
            if (!hasMessages(0)) {
                sendEmptyMessage(0);
            }
        }

        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void pause() {
            mRetry = 0;
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message message) {
            if (mWifiManager.startScan()) {
                mRetry = 0;
            } else if (++mRetry >= 3) {
                mRetry = 0;
                Toast.makeText(getApplicationContext(), R.string.wifi_fail_to_scan, Toast.LENGTH_LONG).show();
                return;
            }
            //sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }
    }
}


