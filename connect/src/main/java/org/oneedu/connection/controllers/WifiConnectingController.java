package org.oneedu.connection.controllers;

import android.app.Fragment;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.oneedu.connection.NoSSLv3Factory;
import org.oneedu.connection.R;
import org.oneedu.connection.fragments.WifiConnectingFragment;
import org.oneedu.connection.views.AccessPointTitleLayout;
import org.oneedu.connectservice.AccessPoint;
import org.oneedu.connectservice.ProxyDB;
import org.oneedu.connectservice.WifiService;
import org.oneedu.uikit.widgets.ProgressBar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

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
    private Bundle                  mTargetBundle;

    public WifiConnectingController(WifiConnectingFragment fragment, View rootView, AccessPoint ap, WifiConfiguration config, WifiService service) {
        mFragment = fragment;
        mView = rootView;
        mAP = ap;
        mWifiService = service;
        m_l_title = (AccessPointTitleLayout) mView.findViewById(R.id.main);

        Fragment targetFragment = fragment.getTargetFragment();
        if (targetFragment != null) {
            mTargetBundle = targetFragment.getArguments();
        }

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
                        if (mTargetBundle != null) {
                            mTargetBundle.putInt(".ErrorCode", WifiManager.ERROR_AUTHENTICATING);
                        }
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

        mTimeoutHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                accessPointResult(false);
                if (mTargetBundle != null) {
                    mTargetBundle.putInt(".ErrorCode", WifiManager.ERROR_AUTHENTICATING);
                }
                return true;
            }
        });
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
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void internetTest() {
        mView.findViewById(R.id.l_connectInternet).setVisibility(View.VISIBLE);

        if (mHandler == null) {
            mHandler = new Handler(new Handler.Callback() {
                int retry = 3;

                @Override
                public boolean handleMessage(Message msg) {
                    Log.d("internetTest", "reponse code: " + msg.what + " / retry remains: " + retry);
                    switch(msg.what) {
                        case 301:
                        case 200:
                            internetTestResult(true);
                            break;

                        // Retry if unable to connect to local proxy (sandroProxy)
                        case -1:
                            if (--retry > 0) {
                                internetTest();
                            } else {
                                internetTestResult(false);
                            }
                            break;

                        default:
                            if (mTargetBundle != null) {
                                mTargetBundle.putInt(".ErrorCode", msg.what);
                            }
                            internetTestResult(false);
                            break;
                    }
                    return true;
                }
            });
        }

        isInternetAvailable(mHandler, 1000);
    }

    private void isInternetAvailable(final Handler handler, final int timeout) {
        // ask fo message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be send before before within the 'timeout' (in milliseconds)

        new Thread() {
            @Override
            public void run() {
                HttpsURLConnection urlc = null;
                int responseCode = -1;
                try {
                    URL url = new URL("https://www.wikipedia.org/");
                    urlc = (HttpsURLConnection) url.openConnection();
                    urlc.setRequestProperty("User-Agent", "Android Application: Connect 1.0");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(timeout);
                    urlc.setUseCaches(false);
                    urlc.setSSLSocketFactory(new NoSSLv3Factory());
                    urlc.connect();
                    responseCode = urlc.getResponseCode();
                    Log.d("isInternetAvailable", "Response code: " + responseCode);

                    for (Map.Entry entry : urlc.getHeaderFields().entrySet()) {
                        Log.d("isInternetAvailable", entry.getKey() + ": " + entry.getValue());
                    }

                } catch (SocketTimeoutException ste) {
                    ste.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();

                    try {
                        if (urlc != null) {
                            responseCode = urlc.getResponseCode();
                            Log.d("isInternetAvailable", "Response code after exception: " + responseCode);
                        }
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                } finally {
                    if (urlc != null)
                        urlc.disconnect();
                }

                handler.sendEmptyMessage(responseCode);
            }
        }.start();
    }
}
