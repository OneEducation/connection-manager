package org.oneedu.connectservice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ConnectivityChangeReceiver", intent.toString());
        Intent i = new Intent("org.oneedu.connection.PROXY.WIFI_STATE_CHANGE");
        i.setComponent(new ComponentName("org.oneedu.connection", "org.oneedu.connectservice.ProxyService"));
        i.putExtras(intent.getExtras());
        context.startService(i);
    }
}
