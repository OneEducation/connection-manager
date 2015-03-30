package org.oneedu.connect;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.oneedu.connection.AccessPoint_;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dongseok0 on 25/03/15.
 */
public class WifiAdapter extends RecyclerView.Adapter<WifiAdapter.ViewHolder> {

    public static final int[] STATE_SECURED_CONNECTED = {
            R.attr.state_encrypted,
            R.attr.state_connected
    };

    private Context mContext;
    private List<AccessPoint_> mDataset;
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemClickListener {
        abstract void onItemClick(View view, AccessPoint_ ap, int position);
        abstract void onItemButtonClick(View view);
    }

    public interface OnItemLongClickListener {
        abstract void onItemLongClick(View view, AccessPoint_ ap, int position);
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
    public class ViewHolder extends RecyclerView.ViewHolder {
        protected TextView mTitle;
        protected TextView mSummary;
        protected ImageView mSignal;

        // each data item is just a string in this case
        public ViewHolder(View v) {
            super(v);
            mTitle =  (TextView) v.findViewById(R.id.title);
            mSummary = (TextView)  v.findViewById(R.id.summary);
            mSignal = (ImageView)  v.findViewById(R.id.signal);

            View button1 = v.findViewById(R.id.button1);
            View button2 = v.findViewById(R.id.button2);
            View button3 = v.findViewById(R.id.button3);
            button1.setTag(R.id.parent_card, v);
            button2.setTag(R.id.parent_card,v);
            button3.setTag(R.id.parent_card,v);

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemClick(v, mDataset.get(getPosition()), getPosition());
                    }
                }
            });

            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemButtonClick(v);
                    }
                }
            });

            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemButtonClick(v);
                    }
                }
            });

            button3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onItemClickListener != null) {
                        onItemClickListener.onItemButtonClick(v);
                    }
                }
            });

            v.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (onItemLongClickListener != null) {
                        onItemLongClickListener.onItemLongClick(v, mDataset.get(getPosition()), getPosition());
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

        private void setSignal(AccessPoint_ ap) {

            if (ap.mRssi == Integer.MAX_VALUE) {
                mSignal.setImageDrawable(null);
            } else {
                mSignal.setImageLevel(ap.getLevel());
                mSignal.setImageResource(R.drawable.wifi_signal);

                if(ap.security != AccessPoint_.SECURITY_NONE) {
                    mSignal.setImageState(AccessPoint_.STATE_SECURED, true);
                }

//                int[] state = new int[2];
                if (ap.getState() != null && ap.getState().ordinal() == 5) {
                    mSignal.setImageState(AccessPoint_.STATE_CONNECTED, true);
                    //state[0] = R.attr.state_connected;
                }
            }

        }

        /** Updates the title and summary; may indirectly call notifyChanged()  */
        private void set(AccessPoint_ ap) {
            setTitle(ap.getTitle().toString());
            setSummary(ap.getSummary() == null ? "" : ap.getSummary().toString());
            setSignal(ap);

            int color = ap.getState() != null && ap.getState().ordinal() == 5 ? mContext.getResources().getColor(R.color.oneEduGreen) :  mContext.getResources().getColor(R.color.oneEduGrey);
            setColor(color);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WifiAdapter(Context context, ArrayList<AccessPoint_> myDataset) {
        mDataset = myDataset;
        mContext = context;
    }

    public WifiAdapter(Context context) {
        mContext = context;
        mDataset = new ArrayList<AccessPoint_>();
    }

    public void add(AccessPoint_ ap) {
        insert(ap, mDataset.size());
    }

    public void insert(AccessPoint_ ap, int position) {
        mDataset.add(position, ap);
        notifyItemInserted(position);
    }

    public void set(ArrayList<AccessPoint_> list) {
        mDataset = list;
        //notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public WifiAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.access_point, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        AccessPoint_ ap = mDataset.get(position);
        holder.set(ap);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
