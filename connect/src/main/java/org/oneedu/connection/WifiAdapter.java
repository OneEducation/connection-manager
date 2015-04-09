package org.oneedu.connection;

import android.content.Context;
import android.net.NetworkInfo;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import org.oneedu.connection.views.AccessPointTitleLayout;
import org.oneedu.connectservice.AccessPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.AccessPointViewHolder> {
    int mSelectedAPPosition = -1;
    private Context mContext;
    private List<AccessPoint> mDataSet;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemClickListener {
        abstract void onItemClick(View view, AccessPoint ap, int position);
        abstract void onItemButtonClick(View view);
    }

    public interface OnItemLongClickListener {
        abstract void onItemLongClick(View view, AccessPoint ap, int position);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class AccessPointViewHolder extends RecyclerView.ViewHolder {
        private final View  m_l_buttons;
        private final View  button1;
        private final View  button2;
        private final View  button3;
        private final View  spacer;
        private final View  mView;
        private TextView    mTitle;
        private TextView    mSummary;
        private ImageView   mSignal;
        private AccessPointTitleLayout  m_l_title;

        private final int   mHeightNormal;
        private final int   mHeightExtend;

        View.OnClickListener mButtonClickListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final int prev = mSelectedAPPosition;
                mSelectedAPPosition = -1;
                notifyItemChanged(prev);

                if (onItemClickListener != null) {
                    onItemClickListener.onItemButtonClick(v);
                }
            }
        };

        // each data item is just a string in this case
        public AccessPointViewHolder(View v) {
            super(v);
            mView = v;
            mTitle =  (TextView) v.findViewById(R.id.title);
            mSummary = (TextView)  v.findViewById(R.id.summary);
            mSignal = (ImageView)  v.findViewById(R.id.signal);
            m_l_title = (AccessPointTitleLayout) v.findViewById(R.id.main);
            m_l_buttons = v.findViewById(R.id.buttons);

            mHeightNormal = mContext.getResources().getDimensionPixelSize(R.dimen.access_point_main_height);
            mHeightExtend = mContext.getResources().getDimensionPixelSize(R.dimen.access_point_height_extend);

            button1 = v.findViewById(R.id.button1);
            button2 = v.findViewById(R.id.button2);
            button3 = v.findViewById(R.id.button3);
            spacer = v.findViewById(R.id.spacer);
            button1.setTag(R.id.parent_card, v);
            button2.setTag(R.id.parent_card, v);
            button3.setTag(R.id.parent_card, v);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final int currentPos = getPosition();

                    if (mSelectedAPPosition == currentPos) {
                        mSelectedAPPosition = -1;
                    } else {
                        final int prev = mSelectedAPPosition;
                        notifyItemChanged(prev);
                        mSelectedAPPosition = currentPos;
                    }
                    notifyItemChanged(currentPos);

                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, mDataSet.get(currentPos), currentPos);
                    }
                }
            });

            button1.setOnClickListener(mButtonClickListener);
            button2.setOnClickListener(mButtonClickListener);
            button3.setOnClickListener(mButtonClickListener);

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onItemLongClickListener != null) {
                        onItemLongClickListener.onItemLongClick(v, mDataSet.get(getPosition()), getPosition());
                    }
                    return true;
                }
            });
        }

        private void setTitle(String ssid) {
            mTitle.setText(ssid);
        }

        private void setSummary(String summary) {
            mSummary.setText(summary);
        }

        private void setColor(int color) {
            mTitle.setTextColor(color);
            mSummary.setTextColor(color);
        }

        private void setSignal(AccessPoint ap) {

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

        private void set(AccessPoint ap, int pos) {
            m_l_title.setConnected(ap.getState() != null && ap.getState().ordinal() == 5);
            setTitle(ap.getTitle().toString());
            setSummary(ap.getSummary() == null ? "" : ap.getSummary().toString());
            setSignal(ap);
            setButtons(ap);

            boolean selected = mSelectedAPPosition == pos;

            ResizeHeightAnimation resizeAnimation = new ResizeHeightAnimation(mView, selected ? mHeightExtend : mHeightNormal);
            resizeAnimation.setDuration(ap.selected == selected ? 0 : 400);
            mView.startAnimation(resizeAnimation);

            ap.selected = selected;

            m_l_buttons.setVisibility(selected ? View.VISIBLE : View.GONE);
        }

        private void setButtons(AccessPoint ap) {
            if (ap.networkId != -1 ) { // not INVALID_NETWORK_ID) {
                button2.setVisibility(View.VISIBLE);

                if (ap.getState() != null && ap.getState() == NetworkInfo.DetailedState.CONNECTED) {
                    button1.setVisibility(View.VISIBLE);

                    spacer.setVisibility(View.GONE);
                    button3.setVisibility(View.GONE);
                } else {
                    button1.setVisibility(View.VISIBLE);

                    spacer.setVisibility(View.VISIBLE);
                    button3.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onViewRecycled(AccessPointViewHolder holder) {
        super.onViewRecycled(holder);
//        holder.set
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WifiAdapter(Context context, ArrayList<AccessPoint> myDataset) {
        mDataSet = myDataset;
        mContext = context;
    }

    public WifiAdapter(Context context) {
        mContext = context;
        mDataSet = new ArrayList<AccessPoint>();
    }

    public void add(AccessPoint ap) {
        insert(ap, mDataSet.size());
    }

    public void insert(AccessPoint ap, int position) {
        mDataSet.add(position, ap);
        notifyItemInserted(position);
    }

    public void set(ArrayList<AccessPoint> list) {
        List<AccessPoint> oldList = mDataSet;
        mDataSet = list;

        for(int i = 0; i < Math.min(oldList.size(), mDataSet.size()); i++) {
            AccessPoint oldAP = oldList.get(i);
            AccessPoint newAP = mDataSet.get(i);
            if (!newAP.equals(oldAP)) {
                notifyItemChanged(i);
            }
        }

        if (oldList.size() > mDataSet.size()) {
            notifyItemRangeRemoved(mDataSet.size(), oldList.size() - mDataSet.size());
        } else {
            notifyItemRangeInserted(oldList.size(), mDataSet.size() - oldList.size());
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AccessPointViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.access_point, parent, false);
        // set the view's size, margins, paddings and layout parameters
        AccessPointViewHolder vh = new AccessPointViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(AccessPointViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        AccessPoint ap = mDataSet.get(position);
        holder.set(ap, position); //, mSelectedAPPosition == position);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
