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

package org.oneedu.connection.controllers;

import android.content.Context;
import android.content.res.Resources;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.dongseok0.library.wifi.Wifi;
import org.dongseok0.library.wifi.android.Credentials;
import org.dongseok0.library.wifi.android.IpAssignment;
import org.dongseok0.library.wifi.android.NetworkUtils;
import org.dongseok0.library.wifi.android.ProxySettings;
import org.json.JSONException;
import org.json.JSONObject;
import org.oneedu.connection.R;
import org.oneedu.connection.data.AccessPoint;
import org.oneedu.connection.data.Proxy;
import org.oneedu.connection.interfaces.WifiConfigUiBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WifiDialogController implements TextWatcher,
        View.OnClickListener, AdapterView.OnItemSelectedListener,
        ViewTreeObserver.OnGlobalFocusChangeListener
{
    private static final int INVALID_NETWORK_ID = -1;
    private final WifiConfigUiBase mConfigUi;
    private final View mView;
    private final AccessPoint mAccessPoint;

    private boolean mEdit;

    private TextView mSsidView;

    // e.g. AccessPoint.SECURITY_NONE
    private int mAccessPointSecurity;
    private TextView mPasswordView;
    private CompoundButton mShowPassword;

    private String unspecifiedCert = "unspecified";
    private static final int unspecifiedCertIndex = 0;

    /* Phase2 methods supported by PEAP are limited */
    private final ArrayAdapter<String> PHASE2_PEAP_ADAPTER;
    /* Full list of phase2 methods */
    private final ArrayAdapter<String> PHASE2_FULL_ADAPTER;

    private Spinner mSecuritySpinner;
    private Spinner mEapMethodSpinner;
    private Spinner mEapCaCertSpinner;
    private Spinner mPhase2Spinner;
    // Associated with mPhase2Spinner, one of PHASE2_FULL_ADAPTER or PHASE2_PEAP_ADAPTER
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mEapUserCertSpinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;

    /* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE 	    = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2 	= 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;

    private static final String TAG = "WifiConfigController";

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    //private Spinner mProxySettingsSpinner;
    private final CompoundButton mProxySB;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private TextView mProxyUsername;
    private TextView mProxyPassword;

//    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
//    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
//    private LinkProperties mLinkProperties = new LinkProperties();

    private final Handler mTextViewChangedHandler;

    private Proxy mProxy;

    private JSONObject mJsonConfig = new JSONObject();
    private Object mKeyStore;
    private Method mKeyStoreSaw;

    public WifiDialogController(
            WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit) {
        mConfigUi = parent;

        mView = view;
        mAccessPoint = accessPoint;
        mAccessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE :
                accessPoint.security;
        mEdit = edit;
        mProxy = accessPoint == null ? null : accessPoint.proxy;

        mTextViewChangedHandler = new Handler();
        final Context context = mConfigUi.getContext();
        final Resources resources = context.getResources();

        PHASE2_PEAP_ADAPTER = new ArrayAdapter<String>(
            context, android.R.layout.simple_spinner_item,
            context.getResources().getStringArray(R.array.wifi_peap_phase2_entries));
        PHASE2_PEAP_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        PHASE2_FULL_ADAPTER = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item,
                context.getResources().getStringArray(R.array.wifi_phase2_entries));
        PHASE2_FULL_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        unspecifiedCert = context.getString(R.string.wifi_unspecified);
        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);

        //mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        //mProxySettingsSpinner.setOnItemSelectedListener(this);
        mProxySB = (CompoundButton) mView.findViewById(R.id.sb_proxy_enable);
        mProxySB.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                View focusView = showProxyFields();
                enableSubmitIfAppropriate();

                if (compoundButton.getTag() != null && (Boolean)compoundButton.getTag() == b) {
                    compoundButton.setTag(null);
                } else if (focusView != null) {
                    focusView.requestFocus();
                }
            }
        });

        if (mAccessPoint == null) { // new network
            mConfigUi.setTitle(R.string.wifi_add_network);
            mConfigUi.setSignal(mAccessPoint);
            mSsidView = (TextView) mView.findViewById(R.id.ssid);
            mSsidView.addTextChangedListener(this);
            mSecuritySpinner = ((Spinner) mView.findViewById(R.id.security));
            mSecuritySpinner.setOnItemSelectedListener(this);

            mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);

            setInitialPos(mProxySB, true);
            //showIpConfigFields();
            showProxyFields();

            mConfigUi.setSubmitButton(context.getString(R.string.wifi_save));
        } else {
            mConfigUi.setTitle(mAccessPoint.ssid);
            mConfigUi.setSummary(mAccessPoint.getSummary());
            mConfigUi.setSignal(mAccessPoint);

            DetailedState state = mAccessPoint.getState();
            int level = mAccessPoint.getLevel();

            boolean showAdvancedFields = true;
            if (mAccessPoint.networkId != INVALID_NETWORK_ID) {
                WifiConfiguration config = mAccessPoint.getConfig();
                if (Wifi.getWifiConfigurationHelper().getIpAssignment(config) == IpAssignment.STATIC) {
                    //mIpSettingsSpinner.setSelection(STATIC_IP);
                    setInitialPos(mIpSettingsSpinner, STATIC_IP);
                    showAdvancedFields = true;
                } else {
                    //mIpSettingsSpinner.setSelection(DHCP);
                    setInitialPos(mIpSettingsSpinner, DHCP);
                }
                //Display IP addresses
                /*for(InetAddress a : config.linkProperties.getAddresses()) {
                    addRow(group, R.string.wifi_ip_address, a.getHostAddress());
                }*/


                if (Wifi.getWifiConfigurationHelper().getProxySettings(config) == ProxySettings.STATIC) {
                    //mProxySettingsSpinner.setSelection(PROXY_STATIC);
                    setInitialPos(mProxySB, true);
                    //showAdvancedFields = true;
                } else {
                    //mProxySettingsSpinner.setSelection(PROXY_NONE);
                    setInitialPos(mProxySB, false);
                    //showAdvancedFields = false;
                }
            }

            if (mAccessPoint.networkId == INVALID_NETWORK_ID) {
                setInitialPos(mProxySB, true);
            }

            if (mAccessPoint.networkId == INVALID_NETWORK_ID || mEdit) {
                showSecurityFields(true);
                //showIpConfigFields();
                showProxyFields();
                //mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.wifi_advanced_togglebox).setOnClickListener(this);
                if (showAdvancedFields) {
                    ((CheckBox) mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
                }
            }

            if (mEdit) {
                mConfigUi.setSubmitButton(context.getString(R.string.wifi_save));
            } else {
                if (state == null && level != -1) {
                    mConfigUi.setSubmitButton(context.getString(R.string.wifi_connect));
                } else {
                    mView.findViewById(R.id.ip_fields).setVisibility(View.GONE);
                }
                if (mAccessPoint.networkId != INVALID_NETWORK_ID) {
                    mConfigUi.setForgetButton(context.getString(R.string.wifi_forget));
                }
            }
        }


        mConfigUi.setCancelButton(context.getString(R.string.wifi_cancel));
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    /* show submit button if password, ip and proxy settings are valid */
    public void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        boolean enabled = true;
        boolean passwordInvalid = false;

        if (mPasswordView != null &&
            ((mAccessPointSecurity == AccessPoint.SECURITY_WEP && mPasswordView.length() == 0) ||
            (mAccessPointSecurity == AccessPoint.SECURITY_PSK && mPasswordView.length() < 8))) {
            passwordInvalid = true;
        }

        if ((mSsidView != null && mSsidView.length() == 0) ||
            ((mAccessPoint == null || mAccessPoint.networkId == INVALID_NETWORK_ID) &&
            passwordInvalid)) {
            enabled = false;
        }

        if (!enabled && passwordInvalid) {
            mConfigUi.setPasswordError(mPasswordView.length() == 0 ? R.string.require_password : R.string.invalid_password);
        } else {
            mConfigUi.setPasswordError(0);
        }

        boolean ipAndProxyValid = ipAndProxyFieldsAreValid();
        submit.setEnabled(enabled && ipAndProxyValid);
    }

    public WifiConfiguration getConfig() {
        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID && !mEdit) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mSsidView.getText().toString());
            // If the user adds a network manually, assume that it is hidden.
            config.hiddenSSID = true;
        } else if (mAccessPoint.networkId == INVALID_NETWORK_ID) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.ssid);
        } else {
            config.networkId = mAccessPoint.networkId;
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.ssid);
        }

        switch (mAccessPointSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPasswordView.length() != 0) {
                    int length = mPasswordView.length();
                    String password = mPasswordView.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
                config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = mEapMethodSpinner.getSelectedItemPosition();
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the PHASE2_PEAP_ADAPTER to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        // The default index from PHASE2_FULL_ADAPTER maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                Wifi.getWifiConfigurationHelper().setCaCertificateAlias(config, caCert);
                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(unspecifiedCert)) clientCert = "";
                Wifi.getWifiConfigurationHelper().setClientCertificateAlias(config, clientCert);
                config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(
                        mEapAnonymousView.getText().toString());

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;
            default:
                return null;
        }

//        config.proxySettings = mProxySettings;
//        config.ipAssignment = mIpAssignment;
//        config.linkProperties = new LinkProperties(mLinkProperties);
        Wifi.getWifiConfigurationHelper().setIpProxy(config, mJsonConfig);

        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        mJsonConfig = new JSONObject();

        IpAssignment ipAssignment = (mIpSettingsSpinner != null &&
                mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) ?
                IpAssignment.STATIC : IpAssignment.DHCP;

        if (ipAssignment == IpAssignment.STATIC) {
            int result = 0;
            try {
                result = validateIpConfigFields();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (result != 0) {
                return false;
            }
        }

//        mProxySettings = (mProxySettingsSpinner != null &&
//                mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) ?
//                ProxySettings.STATIC : ProxySettings.NONE;

        ProxySettings proxySettings = mProxySB.isChecked() && mProxyHostView.getText().length() > 0 ? ProxySettings.STATIC : ProxySettings.NONE;

        if (proxySettings == ProxySettings.STATIC && mProxyHostView != null) {
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int result = validate(host, portStr, exclusionList);

            if (result == 0) {
                try {
                    mJsonConfig.put("proxy_host", host);
                    mJsonConfig.put("proxy_port", portStr);
                    mJsonConfig.put("exclusion_list", exclusionList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                return false;
            }
        }
        return true;
    }

    // Allows underscore char to supports proxies that do not
    // follow the spec
    private static final String HC = "a-zA-Z0-9\\_";

    // Matches blank input, ips, and domain names
    private static final String HOSTNAME_REGEXP =
            "^$|^[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN;
    private static final String EXCLUSION_REGEXP =
            "$|^(\\*)?\\.?[" + HC + "]+(\\-[" + HC + "]+)*(\\.[" + HC + "]+(\\-[" + HC + "]+)*)*$";
    private static final Pattern EXCLUSION_PATTERN;
    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLUSION_PATTERN = Pattern.compile(EXCLUSION_REGEXP);
    }

    /**
     * validate syntax of hostname and port entries
     * @return 0 on success, string resource ID on failure
     */
    public int validate(String hostname, String port, String exclList) {
        Matcher match = HOSTNAME_PATTERN.matcher(hostname);
        String exclListArray[] = exclList.split(",");

        if (!match.matches()) {
            mConfigUi.setProxyHostError(R.string.proxy_error_invalid_host);
            return R.string.proxy_error_invalid_host;
        } else {
            mConfigUi.setProxyHostError(0);
        }

        for (String excl : exclListArray) {
            Matcher m = EXCLUSION_PATTERN.matcher(excl);
            if (!m.matches()) return R.string.proxy_error_invalid_exclusion_list;
        }

        int portError = validateProxyPort(hostname, port);
        mConfigUi.setProxyPortError(portError);
        return portError;
    }

    private int validateProxyPort(String hostname, String port) {
        if (hostname.length() > 0 && port.length() == 0) {
            return R.string.proxy_error_empty_port;
        }

        if (port.length() > 0) {
            if (hostname.length() == 0) {
                return R.string.proxy_error_empty_host_set_port;
            }
            int portVal = -1;
            try {
                portVal = Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return R.string.proxy_error_invalid_port;
            }
            if (portVal <= 0 || portVal > 0xFFFF) {
                return R.string.proxy_error_invalid_port;
            }
        }
        return 0;
    }

    private int validateIpConfigFields() throws JSONException {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.wifi_ip_settings_invalid_ip_address;

        InetAddress inetAddr = null;
        try {
            inetAddr = NetworkUtils.numericToInetAddress(ipAddr);
        } catch (IllegalArgumentException e) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }
        mJsonConfig.put("ip", ipAddr);

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
            mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                    R.string.wifi_network_prefix_length_hint));
        }
        mJsonConfig.put("prefix_length", networkPrefixLength);

        String gateway = mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length-1] = 1;
                mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = null;
            try {
                gatewayAddr = NetworkUtils.numericToInetAddress(gateway);
            } catch (IllegalArgumentException e) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            mJsonConfig.put("gateway", gateway);
        }

        String dns = mDns1View.getText().toString();
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
            mDns1View.setText(mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns);
            } catch (IllegalArgumentException e) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            mJsonConfig.put("dns1", dns);
        }

        if (mDns2View.length() > 0) {
            dns = mDns2View.getText().toString();
            try {
                dnsAddr = NetworkUtils.numericToInetAddress(dns);
            } catch (IllegalArgumentException e) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            mJsonConfig.put("dns2", dns);
        }
        return 0;
    }

    private View showSecurityFields(boolean init) {
        View ret = null;
//        if (mInXlSetupWizard) {
//            // Note: XL SetupWizard won't hide "EAP" settings here.
//            if (!((WifiSettingsForSetupWizardXL)mConfigUi.getContext()).initSecurityFields(mView,
//                        mAccessPointSecurity)) {
//                return ret;
//            }
//        }
        if (mAccessPointSecurity == AccessPoint.SECURITY_NONE) {
            mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
            return ret;
        }
        mView.findViewById(R.id.security_fields).setVisibility(View.VISIBLE);
        mConfigUi.setMinPasswordLength(mAccessPointSecurity == AccessPoint.SECURITY_PSK ? 8 : 0);
        if (mPasswordView == null) {
            ret = mPasswordView = (TextView) mView.findViewById(R.id.password);
            mPasswordView.addTextChangedListener(this);
            mShowPassword = (CheckBox) mView.findViewById(R.id.show_password);
            mShowPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    updatePasswordVisibility(mPasswordView, isChecked);
                }
            });
            if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }

        if (mAccessPointSecurity != AccessPoint.SECURITY_EAP) {
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            return ret;
        }
        mView.findViewById(R.id.eap).setVisibility(View.VISIBLE);

        if (mEapMethodSpinner == null) {
            mEapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
            mEapMethodSpinner.setOnItemSelectedListener(this);
            mPhase2Spinner = (Spinner) mView.findViewById(R.id.phase2);
            mEapCaCertSpinner = (Spinner) mView.findViewById(R.id.ca_cert);
            mEapUserCertSpinner = (Spinner) mView.findViewById(R.id.user_cert);
            ret = mEapIdentityView = (TextView) mView.findViewById(R.id.identity);
            mEapAnonymousView = (TextView) mView.findViewById(R.id.anonymous);

            loadCertificates(mEapCaCertSpinner, Credentials.CA_CERTIFICATE);
            loadCertificates(mEapUserCertSpinner, Credentials.USER_PRIVATE_KEY);

            // Modifying an existing network
            if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                WifiEnterpriseConfig enterpriseConfig = mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                if (init) {
                    setInitialPos(mEapMethodSpinner, eapMethod);
                } else {
                    mEapMethodSpinner.setSelection(eapMethod);
                }

                showEapFieldsByMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        switch (phase2Method) {
                            case Phase2.NONE:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_NONE);
                                break;
                            case Phase2.MSCHAPV2:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_MSCHAPV2);
                                break;
                            case Phase2.GTC:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_GTC);
                                break;
                            default:
                                Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                                break;
                        }
                        break;
                    default:
                        mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                setSelection(mEapCaCertSpinner, Wifi.getWifiConfigurationHelper().getCaCertificateAlias(enterpriseConfig));
                setSelection(mEapUserCertSpinner, Wifi.getWifiConfigurationHelper().getClientCertificateAlias(enterpriseConfig));
                mEapIdentityView.setText(enterpriseConfig.getIdentity());
                mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
            } else {
                // Choose a default for a new network and show only appropriate
                // fields
                if (init) {
                    setInitialPos(mEapMethodSpinner, Eap.PEAP);
                } else {
                    mEapMethodSpinner.setSelection(Eap.PEAP);
                }
                showEapFieldsByMethod(Eap.PEAP);
            }
        } else {
            showEapFieldsByMethod(mEapMethodSpinner.getSelectedItemPosition());
        }

        return ret;
    }

    /**
     * EAP-PWD valid fields include
     *   identity
     *   password
     * EAP-PEAP valid fields include
     *   phase2: MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     * EAP-TLS valid fields include
     *   user_cert
     *   ca_cert
     *   identity
     * EAP-TTLS valid fields include
     *   phase2: PAP, MSCHAP, MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     */
    private void showEapFieldsByMethod(int eapMethod) {
        // Common defaults
        mView.findViewById(R.id.l_method).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);

        // Defaults for most of the EAP methods and over-riden by
        // by certain EAP methods
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);

        Context context = mConfigUi.getContext();
        switch (eapMethod) {
            case WIFI_EAP_METHOD_PWD:
                setPhase2Invisible();
                setCaCertInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TLS:
                mView.findViewById(R.id.l_user_cert).setVisibility(View.VISIBLE);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case WIFI_EAP_METHOD_PEAP:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_PEAP_ADAPTER) {
                    mPhase2Adapter = PHASE2_PEAP_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TTLS:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_FULL_ADAPTER) {
                    mPhase2Adapter = PHASE2_FULL_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                break;
        }
    }

    private void setPhase2Invisible() {
        mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setCaCertInvisible() {
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
        mEapCaCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setUserCertInvisible() {
        mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
        mEapUserCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setAnonymousIdentInvisible() {
        mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        mPasswordView.setText("");
        mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
    }

//    private View showIpConfigFields() {
//        WifiConfiguration config = null;
//        View ret = null;
//
//        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);
//
//        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
//            config = mAccessPoint.getConfig();
//        }
//
//        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
//            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
//            if (mIpAddressView == null) {
//                ret = mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
//                mIpAddressView.addTextChangedListener(this);
//                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
//                mGatewayView.addTextChangedListener(this);
//                mNetworkPrefixLengthView = (TextView) mView.findViewById(
//                        R.id.network_prefix_length);
//                mNetworkPrefixLengthView.addTextChangedListener(this);
//                mDns1View = (TextView) mView.findViewById(R.id.dns1);
//                mDns1View.addTextChangedListener(this);
//                mDns2View = (TextView) mView.findViewById(R.id.dns2);
//                mDns2View.addTextChangedListener(this);
//            }
//            if (config != null) {
//                LinkProperties linkProperties = config.linkProperties;
//                Iterator<LinkAddress> iterator = linkProperties.getLinkAddresses().iterator();
//                if (iterator.hasNext()) {
//                    LinkAddress linkAddress = iterator.next();
//                    mIpAddressView.setText(linkAddress.getAddress().getHostAddress());
//                    mNetworkPrefixLengthView.setText(Integer.toString(linkAddress
//                            .getNetworkPrefixLength()));
//                }
//
//                for (RouteInfo route : linkProperties.getRoutes()) {
//                    if (route.isDefaultRoute()) {
//                        mGatewayView.setText(route.getGateway().getHostAddress());
//                        break;
//                    }
//                }
//
//                Iterator<InetAddress> dnsIterator = linkProperties.getDnses().iterator();
//                if (dnsIterator.hasNext()) {
//                    mDns1View.setText(dnsIterator.next().getHostAddress());
//                }
//                if (dnsIterator.hasNext()) {
//                    mDns2View.setText(dnsIterator.next().getHostAddress());
//                }
//            }
//        } else {
//            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
//        }
//        return ret;
//    }

    private View showProxyFields() {
        WifiConfiguration config = null;
        View ret = null;

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
            config = (WifiConfiguration) mAccessPoint.getConfig();
        }

        //if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
        if (mProxySB.isChecked()) {

            //mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(View.VISIBLE);
            View proxyFields = mView.findViewById(R.id.proxy_fields);
            proxyFields.setVisibility(View.VISIBLE);
            ret = proxyFields;

            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);

                mProxyUsername = (TextView) mView.findViewById(R.id.proxy_username);
                mProxyPassword = (TextView) mView.findViewById(R.id.proxy_password);

                final CheckBox showProxyPassword = (CheckBox) mView.findViewById(R.id.show_proxy_password);
                showProxyPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        updatePasswordVisibility(mProxyPassword, isChecked);
                    }
                });
            }
            if (config != null) {
                String[] proxy = Wifi.getWifiConfigurationHelper().getProxyFields(config);
                if (proxy != null) {
                    mProxyHostView.setText(proxy[0]);
                    mProxyPortView.setText(proxy[1]);
                    mProxyExclusionListView.setText(proxy[2]);
                }

                if (mProxy != null) {
                    mProxyHostView.setText(mProxy.getHost());
                    mProxyPortView.setText(Integer.toString(mProxy.getPort()));
                    mProxyUsername.setText(mProxy.getUsername());

                    if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                        mProxyPassword.setHint(R.string.proxy_pw_unchanged);
                    }
                }
            }
        } else {
            //mView.findViewById(R.id.proxy_warning_limited_support).setVisibility(View.GONE);
            mView.findViewById(R.id.proxy_fields).setVisibility(View.GONE);
        }
        return ret;
    }



    private void loadCertificates(Spinner spinner, String prefix) {
        final Context context = mConfigUi.getContext();

        String[] certs = null;

        try {
            if (mKeyStore == null) {
                mKeyStore = Class.forName("android.security.KeyStore").getDeclaredMethod("getInstance").invoke(null);
            }
            if (mKeyStoreSaw == null) {
                mKeyStoreSaw = mKeyStore.getClass().getDeclaredMethod("saw", String.class, int.class);
            }
            certs = (String[]) mKeyStoreSaw.invoke(mKeyStore, prefix, 1010);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }


        if (certs == null || certs.length == 0) {
            certs = new String[] {unspecifiedCert};
        } else {
            final String[] array = new String[certs.length + 1];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public void afterTextChanged(Editable s) {
        mTextViewChangedHandler.post(new Runnable() {
                public void run() {
                    enableSubmitIfAppropriate();
                }
            });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.wifi_advanced_togglebox) {

            if (((CheckBox) view).isChecked()) {
                final View advancedFields = mView.findViewById(R.id.wifi_advanced_fields);
                advancedFields.setVisibility(View.VISIBLE);

                advancedFields.post(new Runnable() {
                    @Override
                    public void run() {
                        //if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_NONE &&
                        if (!mProxySB.isChecked() &&
                                mIpSettingsSpinner.getSelectedItemPosition() == DHCP) {

                            // Hack for prevent auto-scroll to edittext on top
                            ((ScrollView) mView).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                            ((ScrollView) mView).pageScroll(View.FOCUS_DOWN);
                            mView.post(new Runnable() {
                                @Override
                                public void run() {
                                    ((ScrollView) mView).setDescendantFocusability(ViewGroup.FOCUS_BEFORE_DESCENDANTS);
                                }
                            });
                        } else {
                            advancedFields.requestFocus();
                        }
                    }
                });
            } else {
                mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.GONE);
            }
        }
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        View focusView = null;
        if (parent == mSecuritySpinner) {
            mAccessPointSecurity = position;
            focusView = showSecurityFields(false);
        } else if (parent == mEapMethodSpinner) {
            focusView = showSecurityFields(false);
        } /*else if (parent == mProxySettingsSpinner) {
            focusView = showProxyFields();
        }*/ else {
            //focusView = showIpConfigFields();
        }
        enableSubmitIfAppropriate();

        // Log.d("Test", "" + parent.getTag() + " / " + position);
        // Check whether it's called during initialising.
        if (parent.getTag() != null && (Integer)parent.getTag() == position) {
            parent.setTag(null);
        } else if (focusView != null) {
            focusView.requestFocus();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Make the characters of the password visible if show_password is checked.
     */
    private void updatePasswordVisibility(TextView view, boolean checked) {
        int pos = view.getSelectionEnd();
        view.setInputType(
                InputType.TYPE_CLASS_TEXT | (checked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_TEXT_VARIATION_PASSWORD));
        if (pos >= 0) {
            ((EditText)view).setSelection(pos);
        }
    }

    public String getProxyUsername() {
        return mProxyUsername.getText().toString();
    }

    public String getProxyPassword() {
        if (mProxyPassword.length() > 0) {
            return mProxyPassword.getText().toString();
        } else {
            return mProxy.getPassword();
        }
    }

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
        if (mView == null || newFocus == null || oldFocus == newFocus || mView.findViewById(newFocus.getId()) == null) {
            return;
        }
        scrollViewToCenter(newFocus);
    }

    private void setInitialPos(Spinner spinner, int pos) {
        spinner.setTag(pos);
        spinner.setSelection(pos, false);
    }

    private void setInitialPos(CompoundButton btn, boolean val) {
        btn.setTag(val);
        btn.setChecked(val);
    }

    private void scrollViewToCenter(final View v) {

        mView.post(new Runnable() {
            @Override
            public void run() {
                // Determine where to set the scroll-to to by measuring the distance from the top of the scroll view
                // to the control to focus on by summing the "top" position of each view in the hierarchy.
                int yDistanceToControlsView = 0;
                View parentView = (View) v.getParent();
                while (true)
                {
                    if (parentView.equals(mView))
                    {
                        break;
                    }
                    yDistanceToControlsView += parentView.getTop();
                    parentView = (View) parentView.getParent();
                }

                // Compute the final position value for the top and bottom of the control in the scroll view.
                final int topInScrollView = yDistanceToControlsView + v.getTop();
                final int bottomInScrollView = yDistanceToControlsView + v.getBottom();
                final int middleY = (mView.getScrollY() + mView.getHeight()/2);

                //Log.d("Test", "scrollY : " + mView.getScrollY() + " / " + mView.getHeight() +" / " + "focusY : " + topInScrollView);
                // Post the scroll action to happen on the scrollView with the UI thread.
                mView.post(new Runnable() {
                    @Override
                    public void run() {
                        ((ScrollView)mView).smoothScrollBy(0, topInScrollView - middleY);
                    }
                });
            }
        });
    }
}
