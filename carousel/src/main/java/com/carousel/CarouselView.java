package com.carousel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;
import android.widget.BaseAdapter;


/**
 * Implements {@link CarouselSpinner}.
 * 
 * @author Carousel View
 */
public class CarouselView extends CarouselSpinner implements GestureDetector.OnGestureListener {

    /**
     * The info for adapter context menu
     */
    private AdapterContextMenuInfo mContextMenuInfo;

    /**
     * Carousel adapter.
     */
    private CarouselItemAdapter mAdapter;

    private List<CarouselItemHolder> mCarouselItems;

    /**
     * The position of the item that received the user's down touch.
     */
    private int mDownTouchPosition;

    /**
     * The view of the item that received the user's down touch.
     */
    private View mDownTouchView;

    /**
     * Executes the delta rotations from a fling or scroll movement.
     */
    private FlingRotateRunnable mFlingRunnable = new FlingRotateRunnable();

    /**
     * Helper for detecting touch gestures.
     */
    private GestureDetector mGestureDetector;

    /**
     * If <code>true</code>, which scroll is enable in horizontal state,
     * otherwise in vertical state.
     */
    private boolean mIsHorizontalScroll;

    /**
     * The currently selected item's child.
     */
    private View mSelectedChild;

    /**
     * When fling runnable runs, it resets this to false. Any method along the
     * path until the end of its run() can set this to true to abort any
     * remaining fling. For example, if we've reached either the leftmost or
     * rightmost item, we will set this to true.
     */
    private boolean mShouldStopFling;

    /**
     * Holds boolean value, which indicates whether it is "single tap" mode now.
     */
    private boolean mIsSingleTapUp;

    /**
     * Defines configuration for this {@link CarouselView}.
     */
    private CarouselOptions mCarouselOptions;

    private ViewCoefficientHolder mViewCoefficientHolder;

    private int mCarouselDiameter = 100; // Default value

    private boolean mIsCarouselPanelsDrawingInProgress = false;

    private int mSnapshotElementsCount;

    private CarouselScrollListener mCarouselScrollListener;

    private boolean mRelayoutAllowed = true;
    private boolean mOnFlingStarted = false;
    
    private OnClickListener mEmptyClickListener = new OnClickListener() {
        public void onClick(View v) {
            // Do nothing
        }
    };

    // Constructors
    public CarouselView(Context context) {
        this(context, null, 0);
    }

    public CarouselView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarouselView(Context context, CarouselOptions carouselOptions) {
        super(context);

        mCarouselOptions = carouselOptions;
        initCarouselView();
    }

    public CarouselView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Initialize carousel options
        mCarouselOptions = new CarouselOptions(getContext(), attrs);
        initCarouselView();
    }


    /**
     * Implemented to handle touch screen motion events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!this.isEnabled())
            return false;
        
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDownTouchPosition = getDownTouchPosition(event);
        }

        if (mIsHorizontalScroll) {
            mGestureDetector.onTouchEvent(event);
        }

        restoreGestureDetectorState(event);

        invalidateChildren();
        if (getSelectedItemPosition() == mDownTouchPosition) {
            if (sendDispatchTouchEventToChild(event)) {
                mIsSingleTapUp = false;
                return true;
            } else if (mIsSingleTapUp) {
                onCarouselItemClick();
            }
        }

        return true;
    }
    
    /**
     * Sets carousel scroll listener.
     * 
     * @param carouselScrollListener
     *          instance of {@link CarouselScrollListener}
     */
    public void setCarouselScrollListener(CarouselScrollListener carouselScrollListener) {
        mCarouselScrollListener = carouselScrollListener;
    }

    /* ******************************************************************** */
    /* ************************* OnGestureListener ************************ */
    /* ******************************************************************** */

    @Override
    public boolean onDown(MotionEvent e) {
        // Kill any existing fling/scroll
        mFlingRunnable.stop(false);

        // /// Don't know yet what for it is
        // Get the item's view that was touched
        mDownTouchPosition = getDownTouchPosition(e);

        // Must return true to get matching events for this down event.
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mOnFlingStarted = true;
        mFlingRunnable.startUsingVelocity((int) (velocityX));
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        if (mDownTouchPosition < 0) {
            return;
        }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        long id = getItemIdAtPosition(mDownTouchPosition);
        dispatchLongPress(mDownTouchView, mDownTouchPosition, id);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        float deltaY = e2.getY() - e1.getY();
        float deltaX = e2.getX() - e1.getX();
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            mIsHorizontalScroll = true;
            /*
             * Cancels touch event from selected child view.
             */
            sendDispatchTouchEventToChild(cancelTouch(e1));
            /*
             * Now's a good time to tell our parent to stop intercepting our events! The user has
             * moved more than the slop amount, since GestureDetector ensures this before calling
             * this method. Also, if a parent is more interested in this touch's events than we are,
             * it would have intercepted them by now (for example, we can assume when a Gallery is
             * in the ListView, a vertical scroll would not end up in this method since a ListView
             * would have intercepted it by now).
             */
            getParent().requestDisallowInterceptTouchEvent(true);

            trackMotionScroll(getScrollDistance(distanceX));
            return true;
        } else {
            if (Math.abs(deltaY) > CarouselConfigInfo.SCROLLING_THRESHOLD) {
                mIsHorizontalScroll = false;
            }
        }
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (getSelectedItemPosition() == mDownTouchPosition) {
            if (0 != countItemToPosition((int) e.getX(), (int) e.getY())) {
                mIsSingleTapUp = true;
                return true;
            }
        } else {
            setSelection(e);
        }
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        // Do nothing
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Carousel view steals all key events
        return event.dispatch(this, null, null);
    }

    /**
     * Bring up the context menu for this view.
     */
    @Override
    public boolean showContextMenu() {
        if (isPressed() && mSelectedPosition >= 0) {
            int index = mSelectedPosition - mFirstPosition;
            View v = getChildAt(index);
            return dispatchLongPress(v, mSelectedPosition, mSelectedRowId);
        }
        return false;
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    public void dispatchSetSelected(boolean selected) {
        /*
         * We don't want to pass the selected state given from its parent to its children since this
         * widget itself has a selected state to give to its children.
         */
    }

    /**
     * This method add view to carousel view.
     * 
     * @param childView
     *            instance of view.
     * 
     * @throws NullPointerException
     *             Every time when argument <code>null</code>.
     */
    @Override
    public void addView(View childView) {
        if (null == childView) {
            throw new NullPointerException("Child view cannot be null.");
        }

        addView(childView, mEmptyClickListener);
    }

    /**
     * This method add view to carousel view and register a callback to be
     * invoked when this view is clicked.
     * 
     * @param childView
     *            instance of view.
     * @param action
     *            the callback that will run.
     * 
     * @throws NullPointerException
     *             every time when arguments <code>null</code>.
     */
    public void addView(View childView, OnClickListener action) {
        if ((null == childView) || (null == action)) {
            throw new NullPointerException("Child view cannot be null.");
        }

        addViewToList(childView, action);
    }

    /**
     * This method add view to carousel view by id.
     * 
     * @param childViewId
     *            id of view.
     * 
     * @throws IllegalArgumentException
     *             Every time when argument equals <code>-1</code>.
     */
    public void addView(int childViewId) {
        if (-1 == childViewId) {
            throw new IllegalArgumentException("Child view cannot be null.");
        }
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        addView(layoutInflater.inflate(childViewId, null), mEmptyClickListener);
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getPositionForView(originalView);
        if (longPressPosition < 0) {
            return false;
        }

        final long longPressId = mAdapter.getItemId(longPressPosition);
        return dispatchLongPress(originalView, longPressPosition, longPressId);
    }

    /**
     * Jump directly to a specific item in the adapter data.
     */
    public void setSelection(int position, boolean animate) {
        // Animate only if requested position is already on screen somewhere
        boolean shouldAnimate = animate && mFirstPosition <= position
                && position <= mFirstPosition + getChildCount() - 1;

        setSelectionInt(position, shouldAnimate);
    }

    /**
     * Notifies that data has been changed and any View reflecting the data set
     * should refresh itself.
     */
    public void notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged();
        setAdapter(mAdapter);
    }

    public void scrollToChild(int idx) {
        CarouselItemHolder view = (CarouselItemHolder) getAdapter().getView(idx, null, null);
        if(null == view) {
            return;
        }

        float angle = view.getCurrentAngle();

        if (angle == 0)
            return;

        if (angle > 180.0f)
            angle = 360.0f - angle;
        else
            angle = -angle;

        mFlingRunnable.startUsingDistance(angle);
    }

    /* ******************************************************************** */
    /* ************************* CarouselSpinner ************************** */
    /* ******************************************************************** */

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mIsCarouselPanelsDrawingInProgress) {
            drawCarouselPanels(canvas);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        /*
         * The gallery shows focus by focusing the selected item. So, give focus to our selected
         * item instead. We steal keys from our selected item elsewhere.
         */
        if (gainFocus && mSelectedChild != null) {
            mSelectedChild.requestFocus(direction);
        }
    }

    /**
     * Extra information about the item for which the context menu should be
     * shown.
     */
    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        return mContextMenuInfo;
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(LayoutParams p) {
        return p instanceof LayoutParams;
    }

    /**
     * Index of the child to draw for this iteration
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        // Sort Carousel items by z coordinate in reverse order
        ArrayList<CarouselItemHolder> sl = new ArrayList<CarouselItemHolder>();
        for (int j = 0; j < childCount; j++) {
            CarouselItemHolder view = (CarouselItemHolder) getAdapter().getView(j, null, null);
            if (i == 0)
                view.setDrawn(false);
            sl.add((CarouselItemHolder) getAdapter().getView(j, null, null));
        }

        Collections.sort(sl);

        // Get first undrawn item in array and get result index
        int idx = 0;

        for (CarouselItemHolder civ : sl) {
            if (!civ.isDrawn()) {
                civ.setDrawn(true);
                idx = civ.getIndex();
                break;
            }
        }
        return idx;
    }

    /**
     * Transform an item depending on it's coordinates
     */
    @Override
    protected boolean getChildStaticTransformation(final View child, Transformation transformation) {
        CarouselItemHolder item = ((CarouselItemHolder) child);
        setMatrixToCarouselHolder(item, getChildTransformationMatrix(item, transformation));

        /*
         * DON'T uncomment following line. It cause to routine calling of
         * getChildStaticTransformation() method and device overheating.
         */
        /* child.invalidate(); */
        return true;
    }

    /**
     * Setting up images
     */
    protected void layout(int delta, boolean animate) {
        if (mDataChanged) {
            handleDataChanged();
        }

        // Handle an empty gallery by removing all views.
        if (getCount() == 0) {
            resetList();
            return;
        }

        // Update to the new selected position.
        if (mNextSelectedPosition >= 0) {
            setSelectedPositionInt(mNextSelectedPosition);
        }

        // All views go in recycler while we are in layout
        recycleAllViews();

        /*
         * FIXME: THIS CAUSE INFINITE LOOP IN CASE YOU FILL VIEW COMPONENTS IN RUNTIME OR IN
         * onAttach() onDettach() METHODS
         */
        // detachAllViewsFromParent();

        mCarouselDiameter = getMeasuredWidth();

        CarouselItemHolder child = (CarouselItemHolder) getAdapter().getView(0, null, null);

        if (child.getWidth() > 0)
            mCarouselDiameter = Math.min(getMeasuredWidth(), child.getWidth() * 2);

        int count = getAdapter().getCount();
        float angleUnit = 360.0f / count;

        float angleOffset = mSelectedPosition * angleUnit;
        for (int i = 0; i < getAdapter().getCount(); i++) {
            float angle = angleUnit * i - angleOffset;
            if (angle < 0.0f)
                angle = 360.0f + angle;
            makeAndAddView(i, angle);
        }

        /*
         * FIXME: IN GENERAL IT DOESN'T MAKE SENCE IF detachAllViewsFromParent() IS COMMENTED.
         * 
         * NOTE: if you comment this call, you should comment call recycleAllViews(); (upper in the
         * method) too.
         * 
         * Flush any cached views that did not get reused above
         */
        mRecycler.clear();

        setNextSelectedPositionInt(mSelectedPosition);

        checkSelectionChanged();
        // mDataChanged = false;
        mNeedSync = false;

        updateSelectedItemMetadata();
    }

    /**
     * Setting up images after layout changed
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        /*
         * Remember that we are in layout to prevent more layout request from being generated.
         */
        mInLayout = true;
        layout(0, false);
        mInLayout = false;
    }

    @Override
    protected void setSelectedPositionInt(int position) {
        super.setSelectedPositionInt(position);
        super.setNextSelectedPositionInt(position);

        // Updates any metadata we keep about the selected item.
        updateSelectedItemMetadata();
    }

    /**
     * Compute the horizontal extent of the horizontal scrollbar's thumb within
     * the horizontal range. This value is used to compute the length of the
     * thumb within the scrollbar's track.
     */
    @Override
    protected int computeHorizontalScrollExtent() {
        // Only 1 item is considered to be selected
        return 1;
    }

    /**
     * Compute the horizontal offset of the horizontal scrollbar's thumb within
     * the horizontal range. This value is used to compute the position of the
     * thumb within the scrollbar's track.
     */
    @Override
    protected int computeHorizontalScrollOffset() {
        // Current scroll position is the same as the selected position
        return mSelectedPosition;
    }

    /**
     * Compute the horizontal range that the horizontal scrollbar represents.
     */
    @Override
    protected int computeHorizontalScrollRange() {
        // Scroll range is the same as the item count
        return mItemCount;
    }

    /* ***************************************************************************** */
    /* ******************************** Utility API ******************************** */
    /* ***************************************************************************** */

    /**
     * Rebuild internals - used when updating configuration and need object to
     * refresh using latest config changes.
     */
    public void refresh() {
        mViewCoefficientHolder = getViewCoefficient();
        invalidate();
    }

    private void onUp() {
        if (mFlingRunnable.mRotator.isFinished()) {
            scrollIntoSlots();
        }
    }

    /**
     * Complete scroll so front panel is perfectly centered.
     */
    private void scrollIntoSlots() {
        if ((getChildCount() == 0) || (null == mSelectedChild)) {
            return; // Nothing to do
        }

        // Find nearest item to the 0 degrees angle
        int smallestPos = 0;
        float smallestAngle = 180;
        float angle;
        for (int i = 0; i < getAdapter().getCount(); i++) {
            CarouselItemHolder item = (CarouselItemHolder) getAdapter().getView(i, null, null);
            angle = item.getCurrentAngle();
            if (angle > 180.0f)
                angle = (360.0f - angle);
            if (angle < smallestAngle) {
                smallestAngle = angle;
                smallestPos = i;
            }
        }

        CarouselItemHolder item = (CarouselItemHolder) getAdapter()
                .getView(smallestPos, null, null);
        angle = item.getCurrentAngle();

        // Make it minimum to rotate
        if (angle > 180.0f)
            angle = -(360.0f - angle);

        // Start rotation if still more than 1 degree to rotate
        if (Math.abs(angle) > 1) {
            mFlingRunnable.startUsingDistance(-angle);
        } else {
            int position = item.getIndex();
            setSelectedPositionInt(position);
            if (null != mCarouselScrollListener) {
                mCarouselScrollListener.onPositionChanged(position);
            }
            onFinishedMovement();
        }
    }

    /**
     * Helper for makeAndAddView to set the position of a view and fill out its
     * layout paramters.
     * 
     * @param child
     *            The view to position
     * @param index
     *            index-coordintate indicating where this view should be placed.
     *            This will either be the left or right edge of the view,
     *            depending on the fromLeft paramter
     * @param angleOffset
     *            Are we posiitoning views based on the left edge? (i.e.,
     *            building from left to right)?
     */
    private void setUpChild(CarouselItemHolder child, int index, float angleOffset) {
        /* Check whether child doesn't have parent */
        if (child.getParent() == null) {
            // Ignore any layout parameters for child, use wrap content
            addViewInLayout(child, -1 /* index */, generateDefaultLayoutParams());
        }

        child.setSelected(index == mSelectedPosition);

        int h;
        int w;

        if (mInLayout) {
            w = child.getMeasuredWidth();
            h = child.getMeasuredHeight();
        } else {
            w = child.getMeasuredWidth();
            h = child.getMeasuredHeight();
        }

        // Measure child
        child.measure(w, h);
        child.layout(0, 0, w, h);

        if (mRelayoutAllowed) {
            child.setCurrentAngle(angleOffset);
            calculateItemPosition(child, angleOffset);
        }
    }

    /**
     * Tracks a motion scroll. In reality, this is used to do just about any
     * movement to items (touch scroll, arrow-key scroll, set an item as
     * selected).
     * 
     * @param deltaAngle
     *            Change in X from the previous event.
     */
    private void trackMotionScroll(float deltaAngle) {
        if (getChildCount() == 0) {
            return;
        }

        int newPositionOfCurrentItem = mSelectedPosition;
        float lowestAngleOffset = Integer.MAX_VALUE;
        for (int i = 0; i < getAdapter().getCount(); i++) {
            CarouselItemHolder child = (CarouselItemHolder) getAdapter().getView(i, null, null);

            float angle = child.getCurrentAngle();
            angle += deltaAngle;

            while (angle > 360.0f)
                angle -= 360.0f;

            while (angle < 0.0f)
                angle += 360.0f;

            child.setCurrentAngle(angle);
            calculateItemPosition(child, angle);
            if (lowestAngleOffset == Integer.MAX_VALUE) {
                lowestAngleOffset = Math.min(angle, (360 - angle));
            } else {
                float curLowestAngleOffset = Math.min(angle, (360 - angle));
                float angleOfSelectedItem = ((CarouselItemHolder) getAdapter().getView(newPositionOfCurrentItem, null, null)).getCurrentAngle();
                float offsetOfCurrent = Math.min(angleOfSelectedItem, (360 - angleOfSelectedItem));
                if ((curLowestAngleOffset < lowestAngleOffset) && ((curLowestAngleOffset < offsetOfCurrent))) {
                    newPositionOfCurrentItem = i;
                }
            }
        }

        setSelectedPositionInt(newPositionOfCurrentItem);

        // Clear unused views
        mRecycler.clear();

        invalidate();
    }

    /**
     * Called when rotation is finished
     */
    private void onFinishedMovement() {
        checkSelectionChanged();
        invalidate();
    }

    private int getDownTouchPosition(MotionEvent e) {
        int downTouchPosition = pointToPosition((int) e.getX(), (int) e.getY());
        if (downTouchPosition >= 0) {
            mDownTouchView = getChildAt(downTouchPosition - mFirstPosition);
        }
        return downTouchPosition;
    }

    private void setSelection(MotionEvent event) {
        if (mFlingRunnable.mRotator.isFinished()) {
            if (getSelectedItemPosition() != mDownTouchPosition) {
                scrollToChild(mDownTouchPosition);
            }
        }
    }

    private void updateSelectedItemMetadata() {
        View oldSelectedChild = mSelectedChild;

        View child = mSelectedChild = getChildAt(mSelectedPosition - mFirstPosition);
        if (null == child) {
            return;
        }

        child.setSelected(true);
        child.setFocusable(true);

        if (hasFocus()) {
            child.requestFocus();
        }

        // We unfocus the old child down here so the above hasFocus check
        // returns true
        if (oldSelectedChild != null) {

            // Make sure its drawable state doesn't contain 'selected'
            oldSelectedChild.setSelected(false);

            // Make sure it is not focusable anymore, since otherwise arrow keys
            // can make this one be focused
            oldSelectedChild.setFocusable(false);
        }
    }

    private int getScrollDistance(final float distanceX) {
        int result;
        if (distanceX > 0) {
            result = (int) Math.min(distanceX, mCarouselOptions.getMaxScrollingDistance());
        } else {
            result = (int) Math.max(distanceX, -mCarouselOptions.getMaxScrollingDistance());
        }
        return result;
    }

    private int getScrollVelocity(final float initialVelocity) {
        int result = 0;
        if(getCount() != 0) {
            if (initialVelocity > 0) {
                result = (int) Math.min(initialVelocity, mCarouselOptions.getMaxScrollingVelocity() / getCount());
            } else {
                result = (int) Math.max(initialVelocity, -mCarouselOptions.getMaxScrollingVelocity() / getCount());
            }
        }
        return result;
    }

    private boolean sendDispatchTouchEventToChild(MotionEvent event) {
        boolean result;
        if (0 == mItemCount) {
            return false;
        }
        final CarouselItemHolder selectedView = (CarouselItemHolder) getSelectedView();

        if (null == selectedView) {
        	return false;
        }

        selectedView.setDispatchTouchEventEnable(true);

        if (DisplayMetrics.DENSITY_TV == getResources().getDisplayMetrics().densityDpi) {
            event.setLocation(event.getX() - (int) (selectedView.getItemX()),
                    (event.getY() - (Math.abs(selectedView.getItemY()))));
        } else {
            event.setLocation(event.getX() - (int) (selectedView.getItemX()),
                    ((event.getY() - (selectedView.getItemY()))));
        }
        result = selectedView.dispatchTouchEvent(event);
        selectedView.setDispatchTouchEventEnable(false);

        return result;
    }

    private final Matrix getChildTransformationMatrix(final CarouselItemHolder item,
            final Transformation transformation) {
        float scale = item.getItemScale();
        float scaleXOff = item.getWidth() / 2.0f * (1.0f - scale);

        float centerX = (float) getWidth() / 2;
        scaleXOff += (item.getItemX() + item.getWidth() / 2 - centerX)
                * mViewCoefficientHolder.mDiameterScale;

        final Matrix matrix = transformation.getMatrix();
        matrix.setTranslate(item.getItemX() + scaleXOff, item.getItemY());
        matrix.preScale(scale, scale);

        return matrix;
    }

    private void calculateItemPosition(final CarouselItemHolder child, float angleDegree) {
        int leftPadding = (getWidth() - mCarouselDiameter) / 2;
        int diameter = mCarouselDiameter - child.getWidth();
        float angleRadian = (float) Math.toRadians(angleDegree);
        float r = (float) (diameter / 2);
        float scale = calculateItemScale(angleRadian);

        float x = r * (1.0f - (float)Math.sin(angleRadian));
        float z = r * -(float)Math.cos(angleRadian); // z = -r..r

        float y = (getHeight() - child.getHeight()) / 2 + z * mViewCoefficientHolder.mTilt;
        y -= child.getHeight() / 4 * (1 - scale);

        child.setItemX(x + leftPadding);
        child.setItemY(y);
        child.setItemZ(z);
        child.setItemScale(scale); // Shrink object as it goes deeper away.
        child.setItemAlpha(angleDegree, mCarouselOptions.getMinAlpha());
    }

    private float calculateItemScale(float angleRadian) {
        float sinAngle = (float) Math.sin(angleRadian / 2f); // 0..1
        float scale = 1f - Math.min(1.0f, sinAngle * CarouselConfigInfo.DEPTH_SCALE); // 1..0
        scale = Math.max(CarouselConfigInfo.MIN_SCALE, scale);
        return scale;
    }

    // TODO need to found better way for supporting multi screen.
    /**
     * Provides {@link ViewCoefficientHolder}, which holds values which improve
     * displaying of carousel view items on different screen resolutions.
     * 
     * @return instance of {@link ViewCoefficientHolder}
     */
    private ViewCoefficientHolder getViewCoefficient() {
        int dpi = getResources().getDisplayMetrics().densityDpi;
        float dpiScale = (float) dpi / DisplayMetrics.DENSITY_HIGH;
        return new ViewCoefficientHolder(0.0f, CarouselConfigInfo.DIAMETER_SCALE * dpiScale,
                CarouselConfigInfo.TILT);
    }

    private boolean dispatchLongPress(View view, int position, long id) {
        boolean handled = false;

        if (!handled) {
            mContextMenuInfo = new AdapterContextMenuInfo(view, position, id);
            handled = super.showContextMenuForChild(this);
        }

        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }

        return handled;
    }

    private void makeAndAddView(int position, float angleOffset) {
        CarouselItemHolder child;

        if (!mDataChanged) {
            child = (CarouselItemHolder) mRecycler.get(position);
            if (child != null) {

                // Position the view
                setUpChild(child, child.getIndex(), angleOffset);
            } else {
                // Nothing found in the recycler -- ask the adapter for a view
                child = (CarouselItemHolder) mAdapter.getView(position, null, this);

                // Position the view
                setUpChild(child, child.getIndex(), angleOffset);
            }
            return;
        }

        // Nothing found in the recycler -- ask the adapter for a view
        child = (CarouselItemHolder) mAdapter.getView(position, null, this);

        // Position the view
        setUpChild(child, child.getIndex(), angleOffset);

    }

    private void initCarouselAdapter() {
        mCarouselItems = new ArrayList<>();
        mAdapter = new CarouselItemAdapter(mCarouselItems);
        setAdapter(mAdapter);
    }

    private void onCarouselItemClick() {
        if (null != mCarouselScrollListener) {
            mCarouselScrollListener.onPositionClicked(mDownTouchPosition);
        }
        mCarouselItems.get(mDownTouchPosition).getOnItemClickListener().onClick(getSelectedView());
        mIsSingleTapUp = false;
    }

    private void restoreGestureDetectorState(MotionEvent event) {
        mRelayoutAllowed = false;
        if ((event.getAction() == MotionEvent.ACTION_UP)
                || (event.getAction() == MotionEvent.ACTION_CANCEL)) {
            if (!mOnFlingStarted) {
                mIsHorizontalScroll = true;
                onUp();
                mRelayoutAllowed = true;
            } else {
                mOnFlingStarted = false;
            }
        }
    }

    private void addViewToList(View childView, OnClickListener onClickListener) {
        CarouselItemHolder carouselItem = new CarouselItemHolder(getContext(), childView);
        carouselItem.setIndex(mCarouselItems.size());
        carouselItem.setDispatchTouchEventEnable(false);
        carouselItem.setOnItemClickListener(onClickListener);

        mCarouselItems.add(carouselItem);
    }

    private void initCarouselView() {
        // It's needed to make items with greater value of
        // z coordinate to be behind items with lesser z-coordinate
        setChildrenDrawingOrderEnabled(true);
        mViewCoefficientHolder = getViewCoefficient();

        // Making user gestures available
        mGestureDetector = new GestureDetector(this.getContext(), this);
        mIsHorizontalScroll = true;

        // It's needed to apply 3D transforms to items
        // before they are drawn
        setStaticTransformationsEnabled(true);

        // Initialize image adapter
        initCarouselAdapter();
        initSelectedPosition();
    }

    private MotionEvent cancelTouch(MotionEvent ev) {
        MotionEvent cancel = MotionEvent.obtain(ev);
        cancel.setAction(MotionEvent.ACTION_CANCEL);

        return cancel;
    }

    private void initSelectedPosition() {
        if ((mCarouselOptions.getSelectedItem() < 0)
                || (mCarouselOptions.getSelectedItem() >= mAdapter.getCount())) {
            setNextSelectedPositionInt(0);
        } else {
            setNextSelectedPositionInt(mCarouselOptions.getSelectedItem());
        }
    }

    private void invalidateChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).invalidate();
        }
    }

    private final void setMatrixToCarouselHolder(final CarouselItemHolder item, final Matrix matrix) {
        Matrix mm = new Matrix(matrix);

        item.setCIMatrix(mm);
        Matrix outputMatrix = item.getCIMatrix();
        /*
         * Reuse matrices.
         */
        if (null == outputMatrix) {
            outputMatrix = new Matrix();
        }
        item.setCIMatrix(outputMatrix);
    }

    /**
     * Holds a values for carousel's view.
     * 
     * @author Nazar Ivanchuk
     * 
     */
    private static class ViewCoefficientHolder {

        private final float mDiameterScale;
        private final float mTilt;

        private ViewCoefficientHolder(float topPaddingCoefficient, float diameterScale, float tilt) {
            mDiameterScale = diameterScale;
            mTilt = tilt;
        }
    }

    /**
     * Carousel adapter class for the carousel items.
     * 
     * @author Nazar Ivanchuk
     */
    private class CarouselItemAdapter extends BaseAdapter {

        private List<CarouselItemHolder> mItems;

        public CarouselItemAdapter(List<CarouselItemHolder> items) {
            mItems = items;
        }

        public int getCount() {
            int count = 0;
            if (null != mItems) {
                count = mItems.size();
            }
            return count;
        }

        public CarouselItemHolder getItem(int position) {
            CarouselItemHolder carouselItemHolder = null;
            if (null != mItems) {
                carouselItemHolder = mItems.get(position);
            }
            return carouselItemHolder;
        }

        public long getItemId(int position) {
            long itemId = 0;
            if ((mItems != null) && (0 != mItems.size())) {
                itemId = mItems.get(position).getIndex();
            }
            return itemId;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if ((mItems != null) && isIndexExists(mItems, position)) {
                return mItems.get(position);
            } else {
                return convertView;
            }
        }
        
        private boolean isIndexExists(List<CarouselItemHolder> list, int index) {
            return (index >= 0) && (index < list.size());
        }
    }

    /**
     * Rotation class for the Carousel.
     * 
     * @author ???
     * @author Nazar Ivanchuk
     */
    private class FlingRotateRunnable implements Runnable {

        /**
         * Tracks the decay of a fling rotation
         */
        private Rotator mRotator;

        /**
         * Angle value reported by mRotator on the previous fling
         */
        private float mLastFlingAngle;

        /**
         * Constructor
         */
        private FlingRotateRunnable() {
            mRotator = new Rotator();
        }

        private void startCommon() {
            // Remove any pending flings
            removeCallbacks(this);
        }

        private void startUsingVelocity(float initialVelocity) {
            if (initialVelocity == 0)
                return;

            initialVelocity = getScrollVelocity(initialVelocity);
            startCommon();

            mLastFlingAngle = 0.0f;
            mRotator.fling(initialVelocity);

            post(this);
        }

        private void startUsingDistance(float deltaAngle) {
            if (Math.abs(deltaAngle) < 1)
                return; // Ignore rotation request if less than 1 degree.

            startCommon();

            mLastFlingAngle = 0;
            mRotator.startRotate(0.0f, -deltaAngle, mCarouselOptions.getAnimationDuration());
            post(this);
        }

        private void stop(boolean scrolling) {
            removeCallbacks(this);
            endFling(scrolling);
        }

        private void endFling(boolean scrolling) {
            /*
             * Force the scroller's status to finished (without setting its position to the end)
             */
            mRotator.forceFinished(true);

            if (scrolling)
                scrollIntoSlots();
        }

        @Override
        public void run() {
            invalidateChildren();
            if (CarouselView.this.getChildCount() == 0) {
                endFling(true);
                return;
            }

            mShouldStopFling = false;

            boolean more = mRotator.computeAngleOffset();
            float angle = mRotator.getCurrAngle();

            // Flip sign to convert finger direction to list items direction
            // (e.g. finger moving down means list is moving towards the top)
            float delta = mLastFlingAngle - angle;
            trackMotionScroll(delta);

            if (more && !mShouldStopFling) {
                mLastFlingAngle = angle;
                post(this);
            } else {
                mLastFlingAngle = 0.0f;
                endFling(true);
            }
        }
    }

    private void drawCarouselPanels(Canvas canvas) {
        for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
            View childView = getChildAt(childIndex);
            if (childIndex != getSelectedItemPosition()) {
                drawCarouselPanel(canvas, childView);
            }
        }

        View frontView = getChildAt(getSelectedItemPosition());
        if (frontView != null) {
            drawCarouselPanel(canvas, frontView);
        }
    }

    private void drawCarouselPanel(Canvas canvas, View carouselPanel) {
        carouselPanel.setDrawingCacheEnabled(true);
        Bitmap frontViewBitmap = carouselPanel.getDrawingCache();
        Paint paint = new Paint();
        paint.setAlpha((int) (((CarouselItemHolder) carouselPanel).getAlpha() * 255.0f));
        canvas.drawBitmap(frontViewBitmap, ((CarouselItemHolder) carouselPanel).getCIMatrix(),
                paint);
        carouselPanel.setDrawingCacheEnabled(false);
    }

    private void setCarouselDrawingPanelsEnabled(boolean enable) {
        mIsCarouselPanelsDrawingInProgress = enable;
        for (int childIndex = 0; childIndex < getChildCount(); childIndex++) {
            View childView = getChildAt(childIndex);
            childView.setVisibility(enable ? View.INVISIBLE : View.VISIBLE);
            childView.invalidate();
        }
    }

    /**
     * Callback interface intended to notify about the fact that selected
     * position was changed.
     * 
     * @author Carousel View
     * 
     */
    public interface CarouselScrollListener {

        /**
         * Notify about the fact that selected position was changed.
         * 
         * @param position
         *            selected position
         */
        void onPositionChanged(int position);
        
        /**
         * Notify when carousel position has been clicked to navigate away.
         * 
         * @param position
         *            selected position
         */
        void onPositionClicked(int position);
    }
}
