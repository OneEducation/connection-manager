package org.oneedu.connection.views;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;

import org.oneedu.connection.R;

/**
 * Created by dongseok0 on 1/04/15.
 */
public class AccessPointView extends CardView {
    private boolean connected;
    private static final int[] CONNECTED_STATE = new int[] { R.attr.state_connected };

    public AccessPointView(Context context) {
        super(context);
    }

    public AccessPointView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AccessPointView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
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
