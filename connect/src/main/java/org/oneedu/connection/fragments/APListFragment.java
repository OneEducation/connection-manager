package org.oneedu.connection.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oneedu.connection.controllers.APListController;
import org.oneedu.connection.data.AccessPoint;
import org.oneedu.connection.MainActivity;
import org.oneedu.connection.R;
import org.oneedu.connection.WifiAdapter;
import org.oneedu.connection.services.WifiService;

import java.util.ArrayList;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class APListFragment extends Fragment {

    private RecyclerView mApListView;
    private APListController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.access_point_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getActivity();
        mApListView = (RecyclerView)view.findViewById(R.id.ap_list);

        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mApListView.setLayoutManager(llm);

        // Default animator with change animation enabled from v22.0 (fade in/out)
        RecyclerView.ItemAnimator animator = mApListView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mController = new APListController(APListFragment.this, view, ((MainActivity)context).mWifiService, ((MainActivity)context).mProxyService);
        view.findViewById(R.id.fab_add_network).setOnClickListener(mController);

        ((MainActivity) getActivity()).mWifiService.setOnUpdateAccessPointListener(new WifiService.OnUpdateAccessPointListener() {
            @Override
            public void onUpdateAPListener(ArrayList<AccessPoint> apns) {

                Log.d("onUpdateAPListener", "" + apns.size());
                if (apns.size() == 0) {
                    return;
                }

                WifiAdapter wifiAdapter = (WifiAdapter) mApListView.getAdapter();

                if (wifiAdapter == null) {
                    wifiAdapter = new WifiAdapter(context, apns);
                    wifiAdapter.setOnItemClickListener(mController);
                    mApListView.setAdapter(wifiAdapter);
                } else {
                    wifiAdapter.set(apns);
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)getActivity()).mWifiService.setOnUpdateAccessPointListener(null);
    }
}
