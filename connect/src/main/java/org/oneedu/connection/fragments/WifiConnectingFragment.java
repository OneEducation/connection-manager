package org.oneedu.connection.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.oneedu.connection.R;
import org.oneedu.connection.controllers.WifiConnectingController;
import org.oneedu.connection.views.AccessPointTitleLayout;
import org.oneedu.connectservice.AccessPoint;

/**
 * Created by dongseok0 on 27/03/15.
 */
public class WifiConnectingFragment extends Fragment {
    private View mView;
    private WifiConnectingController mController;
    private int mLeftDelta;
    private int mTopDelta;
    private AccessPointTitleLayout mTitleLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wifi_connecting, null);
        mView = view.findViewById(R.id.cardView);
        mTitleLayout = (AccessPointTitleLayout)view.findViewById(R.id.main);

        Bundle bundle = getArguments();
        final int thumbnailTop = bundle.getInt(".top");
        final int thumbnailLeft = bundle.getInt(".left");

        if (savedInstanceState == null) {
            ViewTreeObserver observer = mView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    mView.getViewTreeObserver().removeOnPreDrawListener(this);
                    int[] screenLocation = new int[2];
                    mView.getLocationOnScreen(screenLocation);
                    mLeftDelta = thumbnailLeft - screenLocation[0];
                    mTopDelta = thumbnailTop - screenLocation[1];

                    setTitle(mController.getAP());
                    runEnterAnimation();
                    return true;
                }
            });
        }

        return view;
    }

    public void setController(WifiConnectingController controller) {
        mController = controller;
    }

    public void runEnterAnimation() {
        mView.setTranslationX(mLeftDelta);
        mView.setTranslationY(mTopDelta);
        mView.animate().translationY(0).setDuration(mTopDelta > 0 ? 400 : 0)
            .setStartDelay(500)
            .withEndAction(new Runnable() {
                @Override
                public void run() {
                    mView.findViewById(R.id.l_connectNetwork).setVisibility(View.VISIBLE);
                }
            }).start();
    }

    public void setTitle(AccessPoint ap) {
        mTitleLayout.setConnected(ap.getState() != null && ap.getState().ordinal() == 5);
        ((TextView)mView.findViewById(R.id.title)).setText(ap.ssid);
        ((TextView)mView.findViewById(R.id.summary)).setText(ap.getSummary());
        ImageView mSignal = (ImageView)mView.findViewById(R.id.signal);

        if (ap.mRssi == Integer.MAX_VALUE) {
            mSignal.setImageDrawable(null);
        } else {
            mSignal.setImageLevel(ap.getLevel());
            mSignal.setImageResource(R.drawable.wifi_signal);

            if(ap.security != AccessPoint.SECURITY_NONE) {
                mSignal.setImageState(AccessPoint.STATE_SECURED, true);
            }
        }
    }
}
