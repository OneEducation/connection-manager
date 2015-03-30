package org.oneedu.uikit.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.oneedu.uikit.R;

/**
 * Created by dongseok0 on 27/03/15.
 */
public class ProgressBar extends FrameLayout {
    View mProgress;
    ImageView mIV;

    public ProgressBar(Context context) {
        super(context);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }

    public void init() {
        mProgress = findViewWithTag("Progress");
        mIV = (ImageView)findViewWithTag("ImageView");
        mIV.setAlpha(0.0f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        init();
    }

    public void done() {
        mIV.setImageResource(R.drawable.check);
        mProgress.animate().alpha(0.0f).setDuration(300).start();
        mIV.animate().alpha(1.0f).setDuration(300).setStartDelay(100).start();
    }

    public void fail() {
        mIV.setImageResource(R.drawable.fail);
        mProgress.animate().alpha(0.0f).setDuration(300).start();
        mIV.animate().alpha(1.0f).setDuration(300).setStartDelay(100).start();
    }
}
