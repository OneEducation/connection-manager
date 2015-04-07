package org.oneedu.connection.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import org.oneedu.connection.R;

/**
 * Created by dongseok0 on 1/04/15.
 */
public class AccessPointTitleLayout extends RelativeLayout {
    private boolean connected;
    private static final int[] CONNECTED_STATE = new int[] { R.attr.state_connected };

    public AccessPointTitleLayout(Context context) {
        super(context);
    }

    public AccessPointTitleLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessPointTitleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AccessPointTitleLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        int[] state = super.onCreateDrawableState(extraSpace + 1);
        if (connected) {
            mergeDrawableStates(state, CONNECTED_STATE);
        }
        return state;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        refreshDrawableState();
    }
}
