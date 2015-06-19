package org.oneedu.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by dongseok0 on 19/06/15.
 */
public class PackageReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("PackageReceiver", action);

        if(action.equals(Intent.ACTION_PACKAGE_ADDED)){

        } else if(action.equals(Intent.ACTION_PACKAGE_REMOVED)){

        } else if(action.equals(Intent.ACTION_PACKAGE_REPLACED)){
            Intent i = new Intent("org.oneedu.connection.PROXY");
            context.startService(i);
        }
    }
}