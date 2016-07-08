package com.carousel;

import android.content.Context;
import android.graphics.Matrix;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


/**
 * Carousel item holder. Contains specific information about carousel item.
 *
 * @author Carousel view
 */
class CarouselItemHolder extends FrameLayout implements Comparable<CarouselItemHolder> {

    private int mIndex;
    private float mCurrentAngle;
    private boolean mDrawn;
    private boolean mIsDispatchTouchEventEnable;

    private View mContentView;

    // Item's coordinates in carousel view
    private float mItemX;
    private float mItemY;
    private float mItemZ;
    private float mScale;

    private float mlApha;
    // It's needed to find screen coordinates
    private Matrix mCIMatrix;
    private OnClickListener mOnItemClickListener;

    private static final LayoutParams CHILD_PARAMS = new LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

    CarouselItemHolder(Context context, View childView) {
        super(context);
        initWidget(context, childView);
    }

    @Override
    public void offsetLeftAndRight(int offset) {
        super.offsetLeftAndRight(offset);
        /* DON'T REMOVE THIS!!! NEEDED FOR CAROUSEL APPROPRIATE WORK */
        invalidate();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mIsDispatchTouchEventEnable ? super.dispatchTouchEvent(ev) : false;
    }

    @Override
    public float getAlpha() {
        return mlApha;
    }

    /* ******************************************************************** */
    /* ***************************** Comparable  ************************** */
    /* ******************************************************************** */

    @Override
    public int compareTo(CarouselItemHolder another) {
        return (int) (another.getItemZ() - mItemZ);
    }

    /* ***************************************************************************** */
    /* ******************************** Utility API ******************************** */
    /* ***************************************************************************** */

    View getContentView() {
        return mContentView;
    }

    OnClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    void setOnItemClickListener(OnClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    boolean isEnable() {
        return mIsDispatchTouchEventEnable;
    }

    void setDispatchTouchEventEnable(boolean isEnable) {
        mIsDispatchTouchEventEnable = isEnable;
    }

    int getIndex() {
        return mIndex;
    }

    float getCurrentAngle() {
        return mCurrentAngle;
    }

    float getItemX() {
        return mItemX;
    }

    float getItemY() {
        return mItemY;
    }

    float getItemScale() {
        return mScale;
    }

    void setItemZ(float z) {
        mItemZ = z;
    }

    float getItemZ() {
        return mItemZ;
    }

    boolean isDrawn() {
        return mDrawn;
    }

    Matrix getCIMatrix() {
        return mCIMatrix;
    }

    void setIndex(int index) {
        mIndex = index;
    }

    void setCurrentAngle(float currentAngle) {
        mCurrentAngle = currentAngle;
    }

    void setItemX(float x) {
        mItemX = x;
    }

    void setItemY(float y) {
        mItemY = y;
    }

    void setItemScale(float scale) {
        mScale = scale;
    }

    void setDrawn(boolean drawn) {
        mDrawn = drawn;
    }

    void setCIMatrix(Matrix mMatrix) {
        mCIMatrix = mMatrix;
    }

    void setItemAlpha(float angleDeg, int minAlpha) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            double percent = 1.0 - Math.sin(Math.toRadians(angleDeg / 2.0));
            percent = Math.pow(percent, 2.0);
            double alpha = Math.max(percent, minAlpha / 255.0);
            mlApha = (float) alpha;
            setAlpha(mlApha);
        }
    }

    private void initWidget(Context context, View childView) {
        mContentView = childView;
        final LayoutParams params = new LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        this.setLayoutParams(params);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.carousel_item_holder, this, true);
        final FrameLayout container = (FrameLayout) view.findViewById(R.id.carousel_item_container);
        container.addView(childView, CHILD_PARAMS);
    }
}