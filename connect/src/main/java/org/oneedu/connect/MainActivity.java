package org.oneedu.connect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import org.oneedu.connect.fragments.APListFragment;
import org.oneedu.connection.ProxyService;
import org.oneedu.connection.WifiService;
import org.oneedu.uikit.activites.BaseActivity;

public class MainActivity extends BaseActivity {

    public WifiService  mWifiService;
    public ProxyService mProxyService;
    private boolean mIsBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent("org.oneedu.connection.PROXY"));
    }

    @Override
    protected void onResume() {
        super.onResume();

        bindService(new Intent(this, ProxyService.class), mProxyConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, WifiService.class), mWifiConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mProxyConnection);
            unbindService(mWifiConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 1) {
            this.finish();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    private ServiceConnection mWifiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mWifiService = ((WifiService.LocalBinder)service).getService();
            getFragmentManager().beginTransaction().addToBackStack("APList")
                    .replace(R.id.container, new APListFragment())
                    .commit();
        }

        public void onServiceDisconnected(ComponentName className) {
            mWifiService = null;
        }
    };

    private ServiceConnection mProxyConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mProxyService = ((ProxyService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mProxyService = null;
        }
    };
}
