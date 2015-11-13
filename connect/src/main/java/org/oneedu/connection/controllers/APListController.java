package org.oneedu.connection.controllers;

import android.app.Activity;
import android.graphics.Point;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.view.View;

import org.oneedu.connection.data.AccessPoint;
import org.oneedu.connection.R;
import org.oneedu.connection.WifiAdapter;
import org.oneedu.connection.fragments.APListFragment;
import org.oneedu.connection.fragments.WifiConnectingFragment;
import org.oneedu.connection.fragments.WifiDialogFragment;
import org.oneedu.connection.services.ProxyService;
import org.oneedu.connection.services.WifiService;

import at.markushi.ui.RevealColorView;

/**
 * Created by dongseok0 on 26/03/15.
 */
public class APListController implements WifiAdapter.OnItemClickListener, View.OnClickListener {

    private final Activity mContext;
    private final APListFragment mFragment;
    private AccessPoint mSelectedAccessPoint;
    private WifiService mWifiService;
    private ProxyService mProxyService;

    private WifiDialogFragment mDialog;
    private RevealColorView mRevealColorView;
    private View mRevealPHView;
    private View mRootView;

    public APListController(APListFragment fragment, View rootView, WifiService service, ProxyService proxy) {
        mFragment = fragment;
        mContext = fragment.getActivity();
        mWifiService = service;
        mProxyService = proxy;
        mRootView = rootView;
        mRevealColorView = (RevealColorView) mRootView.findViewById(R.id.reveal);
        mRevealPHView = mRootView.findViewById(R.id.reveal_ph);
    }

    @Override
    public void onItemClick(View view, AccessPoint ap, int position) {
        mSelectedAccessPoint = ap;

        if (mSelectedAccessPoint.getConfig() == null) {         // not saved network
            if (mSelectedAccessPoint.security == AccessPoint.SECURITY_NONE) {
                /** Bypass dialog for unsecured networks */
                showConnectingDialog(view, mSelectedAccessPoint, null);
            } else {
                showDialog(view, false);
            }
            return;
        }
    }

    @Override
    public void onItemButtonClick(View view) {

        final View parent = (View) view.getTag(R.id.parent_card);
        final int which = view.getId(); //(Integer)view.getTag();

        // wait for hiding animation done
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch(which) {
                    case R.id.button3: //MENU_ID_CONNECT:
                        showConnectingDialog(parent, mSelectedAccessPoint, null);
                        break;

                    case R.id.button2: //MENU_ID_FORGET:
                        mWifiService.forget(mSelectedAccessPoint.networkId);
                        break;

                    case R.id.button1: //MENU_ID_MODIFY:
                        showDialog(parent, true);
                        break;

                    default:
                        break;
                }
            }
        }, 400);
    }

    private void showDialog(View v, boolean edit) {
        mDialog = new WifiDialogFragment();
        mDialog.setArgs(this, mSelectedAccessPoint, edit);

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
                .setCustomAnimations(0, R.anim.fade_out, 0, R.anim.fade_out)
                //.add(R.id.container, mDialog)
                .replace(R.id.container, mDialog)
                .commit();

        //mFragment.runHideAnimation(null);


    }

    void submit(final WifiDialogController configController) {

        WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null && mSelectedAccessPoint.networkId != -1) {
                mWifiService.connect(mSelectedAccessPoint.networkId);
            }
        } else if (config.networkId != -1) {
            if (mSelectedAccessPoint != null) {
                WifiConfiguration con = mProxyService.addOrUpdateProxy(configController);

                final NetworkInfo.DetailedState state = mSelectedAccessPoint.getState();
                if (state != null && state.ordinal() == 5) {
                    // modifying connected network : update -> disable -> reconnect
                    showConnectingDialog(mDialog.mView, mSelectedAccessPoint, con);
                } else {
                    // just save
                    mWifiService.save(con);
                    mContext.getFragmentManager().popBackStack();
                }
            }
        } else {
            WifiConfiguration con = mProxyService.addOrUpdateProxy(configController);

            if (configController.isEdit()) {
                mWifiService.save(con);
                mContext.getFragmentManager().popBackStack();
            } else {
                AccessPoint accessPoint = mSelectedAccessPoint != null ?
                        mSelectedAccessPoint : new AccessPoint(mContext, con);
                showConnectingDialog(mDialog.mView, accessPoint, con);
            }
        }
    }

    private void showConnectingDialog(View v, AccessPoint accessPoint, WifiConfiguration config) {

        int[] screenLocation = new int[2];
        v.getLocationOnScreen(screenLocation);
        int orientation = mContext.getResources().getConfiguration().orientation;

        Bundle extras = new Bundle();
        accessPoint.saveWifiState(extras);
        if (config != null) {
            extras.putParcelable(".wificonfig", config);
        }

        extras.putInt(".orientation", orientation);
        extras.putInt(".left", screenLocation[0]);
        extras.putInt(".top", screenLocation[1]);
        extras.putInt(".width", v.getWidth());
        extras.putInt(".height", v.getHeight());
        //extras.putString(".description", info.description);

        WifiConnectingFragment fragment = new WifiConnectingFragment();
        fragment.setArguments(extras);

        if (mDialog != null) {
            fragment.setTargetFragment(mDialog, 0);
        }

        mContext.getFragmentManager().beginTransaction()
                .addToBackStack("Connecting")
                .setCustomAnimations(0, R.anim.fade_out, 0, R.anim.fade_out)
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

            case R.id.fab_add_network:
                showAddNetworkDialog(v);

                break;
        }


    }

    private void swingBtn(final View v, final Runnable runnable) {
        int[] targetLoc = new int[2];
        int[] srcLoc = new int[2];
        mRevealColorView.getLocationOnScreen(targetLoc);
        v.getLocationOnScreen(srcLoc);

        int diffY = targetLoc[1] - srcLoc[1] + mRevealColorView.getHeight() / 2;

        View container = (View)v.getParent();
        mRootView.findViewById(R.id.ap_list).animate().translationY(300).alpha(0.0f).setDuration(1000);
        mRootView.findViewById(R.id.screen_title).animate().translationY(300).alpha(0).setDuration(1000);

        container.animate().translationY(diffY).rotation(180).setDuration(500)//.setStartDelay(200)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.setAlpha(0);

                        Point p = getLocationInView(mRevealColorView, v);
                        int pink = mFragment.getResources().getColor(R.color.oneEduPink);

                        mRevealColorView.animate().alpha(0).setStartDelay(200).setDuration(300);
                        mRevealPHView.animate().alpha(1).setStartDelay(200).setDuration(300).withEndAction(runnable);

                        mRevealColorView.reveal(p.x, p.y, pink, v.getWidth() / 2, 300, null);
                    }
                }).start();
    }

    private void showAddNetworkDialog(final View v) {

        swingBtn(v, new Runnable() {
            @Override
            public void run() {
                mDialog = new WifiDialogFragment();
                mDialog.setArgs(APListController.this, null, false);

                int[] screenLocation = new int[2];
                mRevealColorView.getLocationOnScreen(screenLocation);
                int orientation = mContext.getResources().getConfiguration().orientation;

                Bundle extras = new Bundle();
                extras.putInt(".orientation", orientation);
                extras.putInt(".left", screenLocation[0]);
                extras.putInt(".top", screenLocation[1]);
                extras.putInt(".width", mRevealColorView.getWidth());
                extras.putInt(".height", mRevealColorView.getHeight());
                mDialog.setArguments(extras);

                mContext.getFragmentManager().beginTransaction()
                        .addToBackStack("AddNetwork")
                        .replace(R.id.container, mDialog)
                        .commit();
            }
        });


    }

    private Point getLocationInView(View src, View target) {
        final int[] l0 = new int[2];
        src.getLocationOnScreen(l0);

        final int[] l1 = new int[2];
        target.getLocationOnScreen(l1);

        l1[0] = l1[0] - l0[0] - target.getWidth() / 2;
        l1[1] = l1[1] - l0[1] - target.getHeight() / 2;

        return new Point(l1[0], l1[1]);
    }

}
