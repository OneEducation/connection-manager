package org.oneedu.connection.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.oneedu.connection.R;
import org.oneedu.connection.ResizeHeightAnimation;
import org.oneedu.connection.views.AccessPointTitleLayout;
import org.oneedu.connectservice.AccessPoint;
import org.oneedu.connectservice.Proxy;
import org.oneedu.connectservice.WifiDialogController;
import org.oneedu.connectservice.WifiConfigUiBase;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class WifiDialogFragment extends Fragment implements WifiConfigUiBase {

    private boolean mEdit;
    private View.OnClickListener mListener;
    private AccessPoint mAccessPoint;
    private Proxy mProxy;

    public View mView;
    private WifiDialogController mController;

    Button mPositive;
    Button mNeutral;
    Button mNegative;
    private ImageView mSignal;
    public View mRootView;
    private int mLeftDelta;
    private int mTopDelta;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = getLayoutInflater().inflate(R.layout.wifi_dialog2, null);

        mView = mRootView.findViewById(R.id.cardView);
        mPositive = (Button)mView.findViewById(R.id.positive);
        mNeutral = (Button)mView.findViewById(R.id.neutral);
        mNegative = (Button)mView.findViewById(R.id.negative);
        mSignal = (ImageView)mView.findViewById(R.id.signal);

        if (mAccessPoint != null) {
            AccessPointTitleLayout l_title = (AccessPointTitleLayout) mView.findViewById(R.id.main);
            l_title.setConnected(mAccessPoint.getState() != null && mAccessPoint.getState().ordinal() == 5);
        }

        mController = new WifiDialogController(this, mView.findViewById(R.id.scrollView), mAccessPoint, mEdit, mProxy);
        /* During creation, the submit button can be unavailable to determine
         * visibility. Right after creation, update button visibility */
        mController.enableSubmitIfAppropriate();


        mView.getViewTreeObserver().addOnGlobalFocusChangeListener(mController);

        Bundle bundle = getArguments();
        final int thumbnailTop = bundle.getInt(".top");
        final int thumbnailLeft = bundle.getInt(".left");
        final int thumbnailWidth = bundle.getInt(".width");
        final int thumbnailHeight = bundle.getInt(".height");
        //mOriginalOrientation = bundle.getInt(".orientation");

        // Only run the animation if we're coming from the parent activity, not if
        // we're recreated automatically by the window manager (e.g., device rotation)
        if (savedInstanceState == null) {
            ViewTreeObserver observer = mView.getViewTreeObserver();
            observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

                @Override
                public boolean onPreDraw() {
                    mView.getViewTreeObserver().removeOnPreDrawListener(this);

                    // Figure out where the thumbnail and full size versions are, relative
                    // to the screen and each other
                    int[] screenLocation = new int[2];
                    mView.getLocationOnScreen(screenLocation);
                    mLeftDelta = thumbnailLeft - screenLocation[0];
                    mTopDelta = thumbnailTop - screenLocation[1];
//
//                    // Scale factors to make the large version the same size as the thumbnail
//                    mWidthScale = (float) thumbnailWidth / mImageView.getWidth();
//                    mHeightScale = (float) thumbnailHeight / mImageView.getHeight();

                    runEnterAnimation();

                    return true;
                }
            });
        }

        return mRootView;
    }

    public void runEnterAnimation() {
        mView.setTranslationX(mLeftDelta);
        mView.setTranslationY(mTopDelta);
        mView.animate().translationY(0).setDuration(400)
                //.setStartDelay(500)
                .withEndAction(new Runnable() {
            @Override
            public void run() {
                int height = mRootView.getHeight();
                ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(mView, height);
                resizeAnimation.setDuration(400);
                mView.startAnimation(resizeAnimation);
            }
        });
    }

    public void setArgs(View.OnClickListener listener,
                              AccessPoint accessPoint, boolean edit, Proxy proxy) {
        mEdit = edit;
        mListener = listener;
        mAccessPoint = accessPoint;
        mProxy = proxy;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView.getViewTreeObserver().removeOnGlobalFocusChangeListener(mController);
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public WifiDialogController getController() {
        return mController;
    }

    @Override
    public LayoutInflater getLayoutInflater() {
        return getActivity().getLayoutInflater();
    }

    @Override
    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public void setTitle(int id) {
        ((TextView)mView.findViewById(R.id.title)).setText(id);
    }

    @Override
    public void setTitle(CharSequence title) {
        ((TextView)mView.findViewById(R.id.title)).setText(title);
    }

    @Override
    public void setSummary(String summary) {
        ((TextView)mView.findViewById(R.id.summary)).setText(summary);
    }

    @Override
    public void setSignal(AccessPoint ap) {
        if (ap == null || ap.mRssi == Integer.MAX_VALUE) {
            mSignal.setImageDrawable(null);
        } else {
            mSignal.setImageLevel(ap.getLevel());
            mSignal.setImageResource(R.drawable.wifi_signal);
            int[] state = new int[2];
            if (ap.getState() != null && ap.getState().ordinal() == 5) {

                state[0] = R.attr.state_connected;

                if(ap.security != AccessPoint.SECURITY_NONE) {
                    state[1] = R.attr.state_encrypted;
                }
            }
            mSignal.setImageState(state, true);
        }
    }

    @Override
    public void setSubmitButton(CharSequence text) {
        mNegative.setVisibility(View.VISIBLE);
        mPositive.setText(text);
        mPositive.setOnClickListener(mListener);
    }

    @Override
    public void setForgetButton(CharSequence text) {
        mNegative.setVisibility(View.VISIBLE);
        mNeutral.setText(text);
        mNeutral.setOnClickListener(mListener);
    }

    @Override
    public void setCancelButton(CharSequence text) {
        mNegative.setVisibility(View.VISIBLE);
        mNegative.setOnClickListener(mListener);
        mNegative.setText(text);
    }

    @Override
    public Button getSubmitButton() {
        return mPositive;
    }

    @Override
    public Button getForgetButton() {
        return mNeutral;
    }

    @Override
    public Button getCancelButton() {
        return mNegative;
    }
}
