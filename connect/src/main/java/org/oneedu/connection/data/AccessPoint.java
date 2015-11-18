/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.oneedu.connection.data;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import org.dongseok0.library.wifi.Wifi;
import org.dongseok0.library.wifi.wificonfiguration.WifiConfigurationHelper;
import org.oneedu.connection.R;

import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

public class AccessPoint implements Comparable {
    static final String TAG = "Settings.AccessPoint";

    private static final String KEY_DETAILEDSTATE = "key_detailedstate";
    private static final String KEY_WIFIINFO = "key_wifiinfo";
    private static final String KEY_SCANRESULT = "key_scanresult";
    private static final String KEY_CONFIG = "key_config";

    private final int INVALID_NETWORK_ID = -1;

    public static final int[] STATE_SECURED = {
        R.attr.state_encrypted
    };
    public static final int[] STATE_NONE = {};
    public static final int[] STATE_CONNECTED = {
        R.attr.state_connected
    };

    /** These values are matched in string arrays -- changes must be kept in sync */
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    private Context mContext;
    private String mSSID;
    private String mSummary;

    enum PskType {
        UNKNOWN,
        WPA,
        WPA2,
        WPA_WPA2
    }

    public String ssid;
    String bssid;
    public int security;
    public int networkId;
    boolean wpsAvailable = false;

    PskType pskType = PskType.UNKNOWN;

    private WifiConfiguration mConfig;
    /* package */ScanResult mScanResult;

    public int mRssi;
    private WifiInfo mInfo;
    private DetailedState mState;

    public boolean selected = false;
    public Proxy proxy;

    static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    private static int getSecurity(ScanResult result) {
        if (result.capabilities.contains("WEP")) {
            return SECURITY_WEP;
        } else if (result.capabilities.contains("PSK")) {
            return SECURITY_PSK;
        } else if (result.capabilities.contains("EAP")) {
            return SECURITY_EAP;
        }
        return SECURITY_NONE;
    }

    public String getSecurityString(boolean concise) {
        Context context = mContext;
        switch(security) {
            case SECURITY_EAP:
                return concise ? context.getString(R.string.wifi_security_short_eap) :
                    context.getString(R.string.wifi_security_eap);
            case SECURITY_PSK:
                switch (pskType) {
                    case WPA:
                        return concise ? context.getString(R.string.wifi_security_short_wpa) :
                            context.getString(R.string.wifi_security_wpa);
                    case WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa2) :
                            context.getString(R.string.wifi_security_wpa2);
                    case WPA_WPA2:
                        return concise ? context.getString(R.string.wifi_security_short_wpa_wpa2) :
                            context.getString(R.string.wifi_security_wpa_wpa2);
                    case UNKNOWN:
                    default:
                        return concise ? context.getString(R.string.wifi_security_short_psk_generic)
                                : context.getString(R.string.wifi_security_psk_generic);
                }
            case SECURITY_WEP:
                return concise ? context.getString(R.string.wifi_security_short_wep) :
                    context.getString(R.string.wifi_security_wep);
            case SECURITY_NONE:
            default:
                return concise ? "" : context.getString(R.string.wifi_security_none);
        }
    }

    private static PskType getPskType(ScanResult result) {
        boolean wpa = result.capabilities.contains("WPA-PSK");
        boolean wpa2 = result.capabilities.contains("WPA2-PSK");
        if (wpa2 && wpa) {
            return PskType.WPA_WPA2;
        } else if (wpa2) {
            return PskType.WPA2;
        } else if (wpa) {
            return PskType.WPA;
        } else {
            Log.w(TAG, "Received abnormal flag string: " + result.capabilities);
            return PskType.UNKNOWN;
        }
    }

    public AccessPoint(Context context, WifiConfiguration config) {
        mContext = context;
        loadConfig(config);
        refresh();
    }

    public AccessPoint(Context context, ScanResult result) {
        mContext = context;
        loadResult(result);
        refresh();
    }

    public AccessPoint(Context context, Bundle savedState) {
        mContext = context;
        mConfig = savedState.getParcelable(KEY_CONFIG);
        if (mConfig != null) {
            loadConfig(mConfig);
            refresh();
        }
        mScanResult = (ScanResult) savedState.getParcelable(KEY_SCANRESULT);
        if (mScanResult != null) {
            // if config and scanresult both exist, that ap is loaded by config and updated by scanresult.
            // ( some values are only available from scanresult )
            if (mConfig == null) {
                loadResult(mScanResult);
                refresh();
            } else {
                update(mScanResult);
            }
        }
        mInfo = (WifiInfo) savedState.getParcelable(KEY_WIFIINFO);
        if (savedState.containsKey(KEY_DETAILEDSTATE)) {
            mState = DetailedState.valueOf(savedState.getString(KEY_DETAILEDSTATE));
        }
        update(mInfo, mState);
    }

    public void saveWifiState(Bundle savedState) {
        savedState.putParcelable(KEY_CONFIG, mConfig);
        savedState.putParcelable(KEY_SCANRESULT, mScanResult);
        savedState.putParcelable(KEY_WIFIINFO, mInfo);
        if (mState != null) {
            savedState.putString(KEY_DETAILEDSTATE, mState.toString());
        }
    }

    private void loadConfig(WifiConfiguration config) {
        ssid = (config.SSID == null ? "" : removeDoubleQuotes(config.SSID));
        bssid = config.BSSID;
        security = getSecurity(config);
        networkId = config.networkId;
        mRssi = Integer.MAX_VALUE;
        mConfig = config;

        ProxyDB proxyDB = ProxyDB.getInstance(mContext);
        proxy = proxyDB.getProxy(config.SSID);
    }

    private void loadResult(ScanResult result) {
        ssid = result.SSID;
        bssid = result.BSSID;
        security = getSecurity(result);
        wpsAvailable = security != SECURITY_EAP && result.capabilities.contains("WPS");
        if (security == SECURITY_PSK)
            pskType = getPskType(result);
        networkId = -1;
        mRssi = result.level;
        mScanResult = result;

        ProxyDB proxyDB = ProxyDB.getInstance(mContext);
        proxy = proxyDB.getProxy(AccessPoint.convertToQuotedString(ssid));
    }

//    @Override
//    protected void onBindView(View view) {
//        super.onBindView(view);
//        ImageView signal = (ImageView) view.findViewById(R.id.signal);
//        if (mRssi == Integer.MAX_VALUE) {
//            signal.setImageDrawable(null);
//        } else {
//            signal.setImageLevel(getLevel());
//            signal.setImageResource(R.drawable.wifi_signal);
//            signal.setImageState((security != SECURITY_NONE) ?
//                    STATE_SECURED : STATE_NONE, true);
//        }
//    }

    @Override
    public int compareTo(Object o) {
        AccessPoint other = (AccessPoint) o;
        // Active one goes first.
        if (mInfo != null && other.mInfo == null) return -1;
        if (mInfo == null && other.mInfo != null) return 1;

        // Reachable one goes before unreachable one.
        if (mRssi != Integer.MAX_VALUE && other.mRssi == Integer.MAX_VALUE) return -1;
        if (mRssi == Integer.MAX_VALUE && other.mRssi != Integer.MAX_VALUE) return 1;

        // Configured one goes before unconfigured one.
        if (networkId != INVALID_NETWORK_ID
                && other.networkId == INVALID_NETWORK_ID) return -1;
        if (networkId == INVALID_NETWORK_ID
                && other.networkId != INVALID_NETWORK_ID) return 1;

        // Sort by signal strength.
        int difference = WifiManager.compareSignalLevel(other.mRssi, mRssi);
        if (difference != 0) {
            return difference;
        }
        // Sort by ssid.
        return ssid.compareToIgnoreCase(other.ssid);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccessPoint)) return false;
        return (this.compareTo((AccessPoint) other) == 0);
    }

    @Override
    public int hashCode() {
        int result = 0;
        if (mInfo != null) result += 13 * mInfo.hashCode();
        result += 19 * mRssi;
        result += 23 * networkId;
        result += 29 * ssid.hashCode();
        return result;
    }

    public boolean update(ScanResult result) {
        if (ssid.equals(result.SSID) && security == getSecurity(result)) {
            if (WifiManager.compareSignalLevel(result.level, mRssi) > 0) {
                int oldLevel = getLevel();
                mRssi = result.level;
//                if (getLevel() != oldLevel) {
//                    notifyChanged();
//                }
            }
            // This flag only comes from scans, is not easily saved in config
            if (security == SECURITY_PSK) {
                pskType = getPskType(result);
            }
            mScanResult = result;
            refresh();
            return true;
        }
        return false;
    }

    public void update(WifiInfo info, DetailedState state) {
        boolean reorder = false;
        if (info != null && networkId != INVALID_NETWORK_ID
                && networkId == info.getNetworkId()) {
            reorder = (mInfo == null);
            mRssi = info.getRssi();
            mInfo = info;
            mState = state;
            refresh();
        } else if (mInfo != null) {
            reorder = true;
            mInfo = null;
            mState = null;
            refresh();
        }
//        if (reorder) {
//            notifyHierarchyChanged();
//        }
    }

    public int getLevel() {
        if (mRssi == Integer.MAX_VALUE) {
            return -1;
        }
        return WifiManager.calculateSignalLevel(mRssi, 4);
    }

    public WifiConfiguration getConfig() {
        return mConfig;
    }

    WifiInfo getInfo() {
        return mInfo;
    }

    public DetailedState getState() {
        return mState;
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    public static String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    private void setTitle(String ssid) {
        mSSID = ssid;
    }

    private void setSummary(String summary) {
        mSummary = summary;
    }

    public String getTitle() {
        return mSSID;
    }

    public String getSummary() {
        return mSummary;
    }

    /** Updates the title and summary; may indirectly call notifyChanged()  */
    private void refresh() {
        setTitle(ssid);

        Context context = mContext;
        if (mConfig != null && mConfig.status == WifiConfiguration.Status.DISABLED) {
            switch (Wifi.getWifiConfigurationHelper().getDisableReason(mConfig)) {
                case WifiConfigurationHelper.DISABLED_AUTH_FAILURE:
                    setSummary(context.getString(R.string.wifi_disabled_password_failure));
                    break;
                case WifiConfigurationHelper.DISABLED_DHCP_FAILURE:
                case WifiConfigurationHelper.DISABLED_DNS_FAILURE:
                    setSummary(context.getString(R.string.wifi_disabled_network_failure));
                    break;
                case WifiConfigurationHelper.DISABLED_UNKNOWN_REASON:
                    setSummary(context.getString(R.string.wifi_disabled_generic));
            }
        } else if (mRssi == Integer.MAX_VALUE) { // Wifi out of range
            setSummary(context.getString(R.string.wifi_not_in_range));
        } else if (mState != null) { // This is the active connection
            // Check proxy and latest connect internet status
            if (mState == DetailedState.CONNECTED && proxy != null) {
                switch(proxy.getStatus()) {
                    case 1:
                        setSummary(Summary.get(context, mState));
                        break;

                    case 0:
                        setSummary(context.getString(R.string.check_proxy));
                        break;
                }
            } else {
                setSummary(Summary.get(context, mState));
            }
        } else { // In range, not disabled.
            StringBuilder summary = new StringBuilder();
            if (mConfig != null) { // Is saved network
                summary.append(context.getString(R.string.wifi_remembered));
            }

            if (security != SECURITY_NONE) {
                String securityStrFormat;
                if (summary.length() == 0) {
                    securityStrFormat = context.getString(R.string.wifi_secured_first_item);
                } else {
                    securityStrFormat = context.getString(R.string.wifi_secured_second_item);
                }
                summary.append(String.format(securityStrFormat, getSecurityString(true)));
            }

            if (mConfig == null && wpsAvailable) { // Only list WPS available for unsaved networks
                if (summary.length() == 0) {
                    summary.append(context.getString(R.string.wifi_wps_available_first_item));
                } else {
                    summary.append(context.getString(R.string.wifi_wps_available_second_item));
                }
            }
            setSummary(summary.toString());
        }
    }

    /**
     * Generate and save a default wifiConfiguration with common values.
     * Can only be called for unsecured networks.
     */
    public void generateOpenNetworkConfig() {
        if (security != SECURITY_NONE)
            throw new IllegalStateException();
        if (mConfig != null)
            return;
        mConfig = new WifiConfiguration();
        mConfig.SSID = AccessPoint.convertToQuotedString(ssid);
        mConfig.allowedKeyManagement.set(KeyMgmt.NONE);

        List<java.net.Proxy> l = ProxySelector.getDefault().select(URI.create("Sdfsf"));
    }
}
