package com.carouselview.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.carouselview.R;

/**
 * Created by CarouselView on 7/8/16.
 */
public class ListLayoutPanel extends BasePanel {

    public interface OnScrollListener {
        void onScroll();
    }

    private OnScrollListener mOnScrollListener;

    private String[] values = new String[] { "Iron Man", "Thor", "Hulk",
            "Captain America", "Black Widow", "Hawkeye", "Quicksilver", "Scarlet Witch",
            "Vision", "Maria Hill", "Falcon", "Ultron"};

    //Constructors
    public ListLayoutPanel(Context context) {
        this(context, null);
    }

    public ListLayoutPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ListLayoutPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayoutPanel();
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListener = onScrollListener;
    }

    /* ***************************************************************************** */
    /* ******************************** Utility API ******************************** */
    /* ***************************************************************************** */

    private void initLayoutPanel() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_list_panel, null);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_list_item_1, values);
        final ListView listView = (ListView) view.findViewById(R.id.listView);
        listView.setAdapter(adapter);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(mOnScrollListener != null) {
                    mOnScrollListener.onScroll();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mOnScrollListener != null) {
                    mOnScrollListener.onScroll();
                }
            }
        });
        mPanelContainer.addView(view);
    }
}
