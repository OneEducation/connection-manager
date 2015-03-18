package org.oneedu.connection;

import android.content.BroadcastReceiver;
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
        Intent i = new Intent("org.oneedu.connection.PROXY");
        context.startService(i);
    }
}
