package org.oneedu.connection.controllers;

import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.oneedu.connection.R;
import org.oneedu.connection.fragments.WifiConnectingFragment;
import org.oneedu.connectservice.AccessPoint;
import org.oneedu.connectservice.WifiService;
import org.oneedu.uikit.widgets.ProgressBar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by dongseok0 on 27/03/15.
 */
public class WifiConnectingController {

    private WifiService mWifiService;
    private WifiConnectingFragment mFragment;
    private View mView;
    private AccessPoint mAP;
    private Handler mHandler;

    public WifiConnectingController(WifiConnectingFragment fragment, AccessPoint ap, WifiService service) {
        mFragment = fragment;
        mView = fragment.getView();
        mAP = ap;
        mWifiService = service;

        mWifiService.setOnUpdateConnectionStateListener(new WifiService.onUpdateConnectionStateListener() {
            @Override
            public void onUpdateConnectionStateChanged(WifiInfo wifiInfo, NetworkInfo.DetailedState state, int supplicantError) {
                Log.d("ConnectionStateChanged", wifiInfo.getSSID() + " : " + state.name() + " / supplicantError: " + supplicantError);
                if (mView == null) {
                    mView = mFragment.getView();
                }

                if (AccessPoint.convertToQuotedString(mAP.ssid).equals(wifiInfo.getSSID())) {
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
    }

    private void accessPointResult(final boolean result) {
        mWifiService.setOnUpdateConnectionStateListener(null);

        if (result) {
            ((TextView)mView.findViewById(R.id.connectToNetwork)).setTextColor(mFragment.getResources().getColor(R.color.oneEduGreen));
            ((ProgressBar)mView.findViewById(R.id.connectToNetworkProgress)).done();
            internetTest();
        } else {
            ((TextView) mView.findViewById(R.id.connectToNetwork)).setTextColor(mFragment.getResources().getColor(R.color.oneEduPink));
            ((ProgressBar) mView.findViewById(R.id.connectToNetworkProgress)).fail();
        }
    }

    private void internetTestResult(final boolean result) {
        Log.d("WifiConnecting", "internetTestResult: "+result);
        if (mFragment == null) return;

        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!result) {
                    ((TextView) mView.findViewById(R.id.connectToInternet)).setTextColor(mFragment.getResources().getColor(R.color.oneEduPink));
                    ((ProgressBar) mView.findViewById(R.id.connectToInternetProgress)).fail();
                } else {

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

    private void popFragment(String stage) {
        mWifiService.setOnUpdateConnectionStateListener(null);
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
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
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
