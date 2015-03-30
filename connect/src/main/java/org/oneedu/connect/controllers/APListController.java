package org.oneedu.connect.controllers;

import android.app.Activity;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.widget.TextView;

import org.oneedu.connect.R;
import org.oneedu.connect.ResizeHeightAnimation;
import org.oneedu.connect.WifiAdapter;
import org.oneedu.connect.fragments.APListFragment;
import org.oneedu.connect.fragments.WifiConnectingFragment;
import org.oneedu.connect.fragments.WifiDialogFragment;
import org.oneedu.connection.*;

/**
 * Created by dongseok0 on 26/03/15.
 */
public class APListController implements WifiAdapter.OnItemClickListener, DialogInterface.OnClickListener, View.OnClickListener {

    private final Activity mContext;
    private final APListFragment mFragment;
    private AccessPoint_ mSelectedAccessPoint;
    private WifiService mWifiService;
    private ProxyService mProxyService;

    private static final int MENU_ID_ADD_NETWORK = Menu.FIRST + 3;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private WifiDialogFragment mDialog;

    public APListController(APListFragment fragment, WifiService service, ProxyService proxy) {
        mFragment = fragment;
        mContext = fragment.getActivity();
        mWifiService = service;
        mProxyService = proxy;
    }

    @Override
    public void onItemClick(View view, AccessPoint_ ap, int position) {
        final TextView b1 = (TextView)view.findViewById(R.id.button1);
        final TextView b2 = (TextView)view.findViewById(R.id.button2);
        final TextView b3 = (TextView)view.findViewById(R.id.button3);
        final View spacer = view.findViewById(R.id.spacer);

        if (b1.getVisibility() == View.VISIBLE) {
            mSelectedAccessPoint = null;
            int height = mContext.getResources().getDimensionPixelSize(R.dimen.access_point_main_height);
            ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(view, height);
            resizeAnimation.setDuration(400);
            view.startAnimation(resizeAnimation);
            //view.findViewById(R.id.buttons).setVisibility(View.GONE);
            b1.setVisibility(View.GONE);
            b2.setVisibility(View.GONE);
            b3.setVisibility(View.GONE);
            spacer.setVisibility(View.GONE);
        } else {
            mSelectedAccessPoint = ap;

            if (mSelectedAccessPoint.getConfig() == null) {         // not saved network
                if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    mWifiService.connect(mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(view, false);
                }
                return;
            }

            if (mSelectedAccessPoint.networkId != -1 ) { // not INVALID_NETWORK_ID) {
                b2.setTag(MENU_ID_FORGET);
                b2.setVisibility(View.VISIBLE);

                if (mSelectedAccessPoint.getState() != null &&
                        mSelectedAccessPoint.getState().ordinal() == 5) {  // connected
                    b1.setTag(MENU_ID_MODIFY);
                    b1.setVisibility(View.VISIBLE);
                } else {
                    b1.setTag(MENU_ID_MODIFY);
                    b1.setVisibility(View.VISIBLE);

                    spacer.setVisibility(View.VISIBLE);
                    b3.setTag(MENU_ID_CONNECT);
                    b3.setVisibility(View.VISIBLE);
                }
            }

            int height = mContext.getResources().getDimensionPixelSize(R.dimen.access_point_height_extend);
            ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(view, height);
            resizeAnimation.setDuration(400);
            view.startAnimation(resizeAnimation);
        }
    }

    @Override
    public void onItemButtonClick(View view) {

        final View parent = (View) view.getTag(R.id.parent_card);

        final TextView b1 = (TextView)parent.findViewById(R.id.button1);
        final TextView b2 = (TextView)parent.findViewById(R.id.button2);
        final TextView b3 = (TextView)parent.findViewById(R.id.button3);
        final View spacer = parent.findViewById(R.id.spacer);

        b1.setVisibility(View.GONE);
        b2.setVisibility(View.GONE);
        b3.setVisibility(View.GONE);
        spacer.setVisibility(View.GONE);

        final int which = (Integer)view.getTag();

        int height = mContext.getResources().getDimensionPixelSize(R.dimen.access_point_main_height);
        ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(parent, height);
        resizeAnimation.setDuration(400);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                switch(which) {
                    case MENU_ID_CONNECT:
                        if (mSelectedAccessPoint.networkId != -1) {     //saved network
                            mWifiService.connect(mSelectedAccessPoint.networkId);
                            showConnectingDialog(parent);
                        } else if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
                            /** Bypass dialog for unsecured networks */
                            mSelectedAccessPoint.generateOpenNetworkConfig();
                            mWifiService.connect(mSelectedAccessPoint.getConfig());
                            showConnectingDialog(parent);

                        } else {
                            showDialog(parent, false);
                        }
                        break;

                    case MENU_ID_FORGET:
                        mWifiService.forget(mSelectedAccessPoint.networkId);
                        break;

                    case MENU_ID_MODIFY:
                        showDialog(parent, true);
                        break;

                    default:
                        break;
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        parent.startAnimation(resizeAnimation);
    }

    private void showDialog(View v, boolean edit) {
        org.oneedu.connection.Proxy proxy = null;
        if(mSelectedAccessPoint != null) {
            proxy = mProxyService.getProxy(AccessPoint_.convertToQuotedString(mSelectedAccessPoint.ssid));
        }

        mDialog = new WifiDialogFragment();
        mDialog.setArgs(this, mSelectedAccessPoint, edit, proxy);

        int[] screenLocation = new int[2];
        v.getLocationOnScreen(screenLocation);
        int orientation = mContext.getResources().getConfiguration().orientation;

        Bundle extras = new Bundle();
        extras.putInt(".orientation", orientation);
        extras.putInt(".left", screenLocation[0]);
        extras.putInt(".top", screenLocation[1]);
        extras.putInt(".width", v.getWidth());
        extras.putInt(".height", v.getHeight());
        //extras.putString(".description", info.description);
        mDialog.setArguments(extras);

        mContext.getFragmentManager().beginTransaction()
                .addToBackStack("WifiDialog")
                .setCustomAnimations(0, R.anim.slide_out_down, 0, R.anim.slide_out_down)
                //.add(R.id.container, mDialog)
                .replace(R.id.container, mDialog)
                .commit();

        //mFragment.runHideAnimation(null);


    }

    @Deprecated
    @Override
    public void onClick(DialogInterface dialog, int button) {
        if (button == WifiDialogFragment.BUTTON_FORGET && mSelectedAccessPoint != null) {
//            forget();
        } else if (button == WifiDialogFragment.BUTTON_SUBMIT) {
            if (mDialog != null) {
                submit(mDialog.getController());
            }
        }
    }

    void submit(final org.oneedu.connection.WifiDialogController configController) {

        WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null && mSelectedAccessPoint.networkId != -1) {
                mWifiService.connect(mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != -1) {
            if (mSelectedAccessPoint != null) {
                WifiConfiguration con = mProxyService.addOrUpdateProxy(configController);
                mWifiService.save(con);
                mContext.getFragmentManager().popBackStack();
            }
        } else {
            WifiConfiguration con = mProxyService.addOrUpdateProxy(configController);

            if (configController.isEdit()) {
                mWifiService.save(con);
                mContext.getFragmentManager().popBackStack();
            } else {
                mWifiService.connect(con);
                showConnectingDialog(mDialog.getView());
            }
        }
    }

    private void showConnectingDialog(View v) {

        int[] screenLocation = new int[2];
        v.getLocationOnScreen(screenLocation);
        int orientation = mContext.getResources().getConfiguration().orientation;

        Bundle extras = new Bundle();
        extras.putInt(".orientation", orientation);
        extras.putInt(".left", screenLocation[0]);
        extras.putInt(".top", screenLocation[1]);
        extras.putInt(".width", v.getWidth());
        extras.putInt(".height", v.getHeight());
        //extras.putString(".description", info.description);

        WifiConnectingFragment fragment = new WifiConnectingFragment();
        fragment.setArguments(extras);
        fragment.setController(new WifiConnectingController(fragment, mSelectedAccessPoint, mWifiService));

        mContext.getFragmentManager().beginTransaction()
                .addToBackStack("Connecting")
                .setCustomAnimations(0, R.anim.slide_out_down, 0, R.anim.slide_out_down)
                .replace(R.id.container, fragment)
                .commit();

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.neutral:
                mWifiService.forget(mSelectedAccessPoint.networkId);
                mContext.getFragmentManager().popBackStack();
                break;

            case R.id.positive:
                if (mDialog != null) {
                    submit(mDialog.getController());
                }
                break;

            case R.id.negative:
                mContext.getFragmentManager().popBackStack();
                break;
        }
    }


}
