package com.carouselview.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.carouselview.R;

/**
 * Created by CarouselView on 7/7/16.
 */
public class ImagePanel extends BasePanel {

    private ImageView mImageViewHolder;

    //Constructors
    public ImagePanel(Context context) {
        this(context, null);
    }

    public ImagePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImagePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initImagePanel();
    }

    public void setImageResId(int resId) {
        mImageViewHolder.setImageResource(resId);
    }

    /* ***************************************************************************** */
    /* ******************************** Utility API ******************************** */
    /* ***************************************************************************** */

    private void initImagePanel() {
        mImageViewHolder = new ImageView(getContext());
        mImageViewHolder.setScaleType(ImageView.ScaleType.FIT_XY);
        mImageViewHolder.setImageResource(R.mipmap.ic_launcher);
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mImageViewHolder.setLayoutParams(lp);
        mPanelContainer.addView(mImageViewHolder);
    }
}
