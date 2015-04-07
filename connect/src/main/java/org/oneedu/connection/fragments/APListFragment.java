package org.oneedu.connection.fragments;

import android.app.Fragment;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oneedu.connection.controllers.APListController;
import org.oneedu.connectservice.AccessPoint;
import org.oneedu.connection.MainActivity;
import org.oneedu.connection.R;
import org.oneedu.connection.WifiAdapter;
import org.oneedu.connectservice.WifiService;

import java.util.ArrayList;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class APListFragment extends Fragment {

    private RecyclerView mApListView;
    private WifiAdapter mWifiAdapter;
    private APListController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.access_point_list, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = getActivity();
        mApListView = (RecyclerView)view.findViewById(R.id.ap_list);

        mApListView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mApListView.setLayoutManager(llm);


        //((MainActivity)getActivity()).mWifiService.updateAccessPoints();
        mWifiAdapter = new WifiAdapter(context, ((MainActivity)context).mWifiService.getAPList());
        mApListView.setAdapter(mWifiAdapter);

        mController = new APListController(this, ((MainActivity)context).mWifiService, ((MainActivity)context).mProxyService);
        mWifiAdapter.setOnItemClickListener(mController);

        view.findViewById(R.id.fab_add_network).setOnClickListener(mController);

        ((MainActivity)getActivity()).mWifiService.setOnUpdateAccessPointListener(new WifiService.OnUpdateAccessPointListener() {
            @Override
            public void onUpdateAPListener(ArrayList<AccessPoint> apns) {

                Log.d("APList", ""+apns.size());
                mWifiAdapter.set(apns);
                mWifiAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUpdateConnectionState(WifiInfo wifiInfo, NetworkInfo.DetailedState state) {

            }
        });
    }
}