package org.oneedu.connect.fragments;

import android.animation.Animator;
import android.app.Fragment;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oneedu.connect.controllers.APListController;
import org.oneedu.connection.AccessPoint_;
import org.oneedu.connect.MainActivity;
import org.oneedu.connect.R;
import org.oneedu.connect.WifiAdapter;
import org.oneedu.connection.WifiService;

import java.util.ArrayList;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class APListFragment extends Fragment {

    private RecyclerView mApListView;
    private WifiManager mWifiManager;
    private WifiAdapter mWifiAdapter;
    private APListController mController;

    public float getYFraction()
    {
        int height = mApListView.getHeight();
        return (height == 0) ? 0 : mApListView.getY() / (float) height;
    }

    public void setYFraction(float yFraction) {
        int height = mApListView.getHeight();
        mApListView.setY((height > 0) ? (yFraction * height) : 0);
    }

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
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        mApListView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(context);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mApListView.setLayoutManager(llm);


        ((MainActivity)getActivity()).mWifiService.updateAccessPoints();
        mWifiAdapter = new WifiAdapter(getActivity(), ((MainActivity)getActivity()).mWifiService.getAPList());
        mApListView.setAdapter(mWifiAdapter);

        mController = new APListController(this, ((MainActivity)getActivity()).mWifiService, ((MainActivity)getActivity()).mProxyService);
        mWifiAdapter.setOnItemClickListener(mController);

        ((MainActivity)getActivity()).mWifiService.setOnUpdateAccessPointListener(new WifiService.OnUpdateAccessPointListener() {
            @Override
            public void onUpdateAPListener(ArrayList<AccessPoint_> apns) {

                Log.d("APList", ""+apns.size());
                mWifiAdapter.set(apns);
                mWifiAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUpdateConnectionState(WifiInfo wifiInfo, NetworkInfo.DetailedState state) {

            }
        });
    }

    public void runHideAnimation(Animator.AnimatorListener listener) {
        mApListView.animate().alpha(0.0f).setDuration(500).setListener(listener).start();
    }

    public void runShowAnimation(Animator.AnimatorListener listener) {
        mApListView.animate().alpha(1.0f).setDuration(500).setListener(listener).start();
    }
}
