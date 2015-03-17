package org.oneedu.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectionManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectionManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnected();

        Intent i = new Intent("org.oneedu.connection.PROXY");

        if (isConnected) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            android.net.wifi.WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();

            i.putExtra("SSID", ssid);
        }

        context.startService(i);
    }
}
