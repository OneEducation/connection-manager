package org.oneedu.connection.controllers;

import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.oneedu.connection.R;
import org.oneedu.connection.fragments.WifiConnectingFragment;
import org.oneedu.connection.views.AccessPointTitleLayout;
import org.oneedu.connectservice.AccessPoint;
import org.oneedu.connectservice.ProxyDB;
import org.oneedu.connectservice.WifiService;
import org.oneedu.uikit.widgets.ProgressBar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created by dongseok0 on 27/03/15.
 */
public class WifiConnectingController {

    private WifiService mWifiService;
    private WifiConnectingFragment mFragment;
    private View mView;
    private AccessPoint mAP;
    private Handler mHandler;
    private WifiInfo mLastInfo;
    private Handler mTimeoutHandler;
    private AccessPointTitleLayout m_l_title;

    public WifiConnectingController(WifiConnectingFragment fragment, View rootView, AccessPoint ap, WifiConfiguration config, WifiService service) {
        mFragment = fragment;
        mView = rootView;
        mAP = ap;
        mWifiService = service;
        m_l_title = (AccessPointTitleLayout) mView.findViewById(R.id.main);

        mWifiService.setOnUpdateConnectionStateListener(new WifiService.onUpdateConnectionStateListener() {
            @Override
            public void onUpdateConnectionStateChanged(WifiInfo wifiInfo, NetworkInfo.DetailedState state, int supplicantError) {
                Log.d("ConnectionStateChanged", wifiInfo.getSSID() + " : " + state.name() + " / supplicantError: " + supplicantError);

                // When getting authenticate error event from supplicant_state_change_action, there is no info about which network.
                // So keep the last wifi info and use it if there is no active wifi info.
                if (!"0x".equals(wifiInfo.getSSID())) {
                    mLastInfo = wifiInfo;
                }

                if (mLastInfo != null && AccessPoint.convertToQuotedString(mAP.ssid).equals(mLastInfo.getSSID())) {
                    if (supplicantError == WifiManager.ERROR_AUTHENTICATING) {
                        accessPointResult(false);
                        return;
                    }

                    switch (state) {
                        case CONNECTED:
                            accessPointResult(true);
                            break;

                        case BLOCKED:
                        case FAILED:
                            accessPointResult(false);
                            break;
                    }
                }
            }
        });

        mTimeoutHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                accessPointResult(false);
            }
        };
        mTimeoutHandler.sendEmptyMessageDelayed(1, 30000);

        if (config == null) {
            if (mAP.networkId != -1) {     //saved network
                mWifiService.connect(mAP.networkId);
            } else if (mAP.security == AccessPoint.SECURITY_NONE) {
                /** Bypass dialog for unsecured networks */
                mAP.generateOpenNetworkConfig();
                mWifiService.connect(mAP.getConfig());
            }
        } else {
            mWifiService.updateAndReconnect(config);
        }
    }

    private void accessPointResult(final boolean result) {
        clearListeners();

        if (result) {
            m_l_title.setConnected(true);
            ((TextView)mView.findViewById(R.id.connectToNetwork)).setTextColor(mFragment.getResources().getColor(R.color.oneEduGreen));
            ((ProgressBar)mView.findViewById(R.id.connectToNetworkProgress)).done();
            internetTest();
        } else {
            ((TextView) mView.findViewById(R.id.connectToNetwork)).setTextColor(mFragment.getResources().getColor(R.color.oneEduPink));
            ((ProgressBar) mView.findViewById(R.id.connectToNetworkProgress)).fail();
            mView.findViewById(R.id.l_buttons).setVisibility(View.VISIBLE);
        }
    }

    private void internetTestResult(final boolean result) {
        Log.d("WifiConnecting", "internetTestResult: " + result);

        ProxyDB.getInstance(mFragment.getActivity()).updateInternetConnectStatus(AccessPoint.convertToQuotedString(mAP.ssid), result ? 1 : 0);

        // Testing internet connection do not affect to wifi configuration, so need to trigger scan wifi to update on list
        mWifiService.scanWifi();

        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!result) {
                    ((TextView) mView.findViewById(R.id.connectToInternet)).setTextColor(mFragment.getResources().getColor(R.color.oneEduPink));
                    ((ProgressBar) mView.findViewById(R.id.connectToInternetProgress)).fail();
                    mView.findViewById(R.id.l_buttons).setVisibility(View.VISIBLE);
                } else {
                    m_l_title.setInternet(true);
                    ((TextView) mView.findViewById(R.id.connectToInternet)).setTextColor(mFragment.getResources().getColor(R.color.oneEduGreen));
                    ((ProgressBar) mView.findViewById(R.id.connectToInternetProgress)).done();
                    mView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            popFragment("APList");
                        }
                    }, 1000);
                }
            }
        });
    }

    public void clearListeners() {
        mWifiService.setOnUpdateConnectionStateListener(null);
        mTimeoutHandler.removeMessages(1);
    }

    private void popFragment(String stage) {
        clearListeners();
        mFragment.getFragmentManager().popBackStack(stage, 0);
    }

    public AccessPoint getAP() {
        return mAP;
    }

    public boolean ping() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int     exitValue = ipProcess.waitFor();
            return (exitValue == 0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void internetTest() {
        mView.findViewById(R.id.l_connectInternet).setVisibility(View.VISIBLE);

        if (mHandler == null) {
            mHandler = new Handler() {
                int retry = 3;

                @Override
                public void handleMessage(Message msg) {
                    switch(msg.what) {
                        case 1:
                            internetTestResult(true);
                            break;

                        case 0:
                            if (--retry > 0) {
                                internetTest();
                            } else {
                                internetTestResult(false);
                            }
                            break;
                    }
                }
            };
        }

        isNetworkAvailable(mHandler, 1000);
    }

    private void isNetworkAvailable(final Handler handler, final int timeout) {
        // ask fo message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be send before before within the 'timeout' (in milliseconds)

        new Thread() {
            @Override
            public void run() {
                HttpURLConnection urlc = null;
                try {
                    URL url = new URL("http://www.google.com");
                    urlc = (HttpURLConnection) url.openConnection();
                    urlc.setRequestProperty("User-Agent", "Android Application: Connect 1.0");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(timeout);
                    urlc.setUseCaches(false);
                    urlc.connect();
                    if (urlc.getResponseCode() == 200) {
                        handler.sendEmptyMessage(1);
                        return;
                    }
                } catch (MalformedURLException e1) {
                    e1.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (urlc != null)
                        urlc.disconnect();
                }

                handler.sendEmptyMessage(0);
            }
        }.start();
    }
}
