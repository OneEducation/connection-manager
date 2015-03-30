package org.oneedu.connect.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import org.oneedu.connect.R;
import org.oneedu.connect.ResizeHeightAnimation;
import org.oneedu.connect.controllers.WifiConnectingController;
import org.oneedu.connection.AccessPoint_;

/**
 * Created by dongseok0 on 27/03/15.
 */
public class WifiConnectingFragment extends Fragment {
    private View mView;
    private int mStartWidth;
    private int mStartHeight;
    private WifiConnectingController mController;
    private int mLeftDelta;
    private int mTopDelta;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wifi_connecting, null);
        mView = view.findViewById(R.id.cardView);

        Bundle bundle = getArguments();
        final int thumbnailTop = bundle.getInt(".top");
        final int thumbnailLeft = bundle.getInt(".left");
        mStartWidth = bundle.getInt(".width");
        mStartHeight = bundle.getInt(".height");

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

        final int targetHeight = mView.getHeight();
        //mView.getLayoutParams().width = mStartWidth;
        mView.getLayoutParams().height = mStartHeight;

        mView.setTranslationX(mLeftDelta);
        mView.setTranslationY(mTopDelta);

        mView.requestLayout();

        mView.post(new Runnable() {
            @Override
            public void run() {
                mView.animate().translationY(0).setDuration(mTopDelta > 0 ? 400 : 0)
                        .setStartDelay(500)
                        .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(mView, targetHeight);
                        resizeAnimation.setDuration(400);
                        resizeAnimation.setStartOffset(400);
                        mView.startAnimation(resizeAnimation);
                    }
                });
            }
        });
    }

    public void setTitle(AccessPoint_ ap) {
        ((TextView)mView.findViewById(R.id.title)).setText(ap.ssid);
        ((TextView)mView.findViewById(R.id.summary)).setText(ap.getSummary());
        ImageView mSignal = (ImageView)mView.findViewById(R.id.signal);

        if (ap.mRssi == Integer.MAX_VALUE) {
            mSignal.setImageDrawable(null);
        } else {
            mSignal.setImageLevel(ap.getLevel());
            mSignal.setImageResource(R.drawable.wifi_signal);
            int[] state = new int[2];
            if (ap.getState() != null && ap.getState().ordinal() == 5) {

                state[0] = R.attr.state_connected;

                if(ap.security != AccessPoint_.SECURITY_NONE) {
                    state[1] = R.attr.state_encrypted;
                }
            }
            mSignal.setImageState(state, true);
        }
    }
}
