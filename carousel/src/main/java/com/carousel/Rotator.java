package com.carousel;

import android.view.animation.AnimationUtils;

/**
 * This class encapsulates rotation. The duration of the rotation can be passed in the constructor
 * and specifies the maximum time that the rotation animation should take. Past this time, the
 * rotation is automatically moved to its final stage and computeRotationOffset() will always return
 * false to indicate that scrolling is over.
 * 
 * @author Carousel View
 */

class Rotator {
    private int mMode;

    private float mStartAngleDeg;
    private float mCurrAngleDeg;
    private float mDeltaAngleDeg;

    private long mStartMillis;
    private long mDurationMillis;

    private boolean mFinished;

    private float mVelocity;
    private final float mCoeffVelocity = 0.05f;

    private static final float DECELERATION = 240.0f;
    private static final int SCROLL_MODE = 0;
    private static final int FLING_MODE = 1;

    /**
     * Create a Scroller with the specified interpolator. If the interpolator is null, the default
     * (viscous) interpolator will be used.
     */
    Rotator() {
        mFinished = true;
    }

    /**
     * @return True if finished scrolling, false otherwise.
     */
    final boolean isFinished() {
        return mFinished;
    }

    /**
     * Force the finished field to a particular value.
     * 
     * @param finished
     *            The new finished value.
     */
    final void forceFinished(boolean finished) {
        mFinished = finished;
    }

    /**
     * Returns how long the scroll event will take, in milliseconds.
     * 
     * @return The duration of the scroll in milliseconds.
     */
    final long getDuration() {
        return mDurationMillis;
    }

    /**
     * Returns the current X offset in the scroll.
     * 
     * @return The new X offset as an absolute distance from the origin.
     */
    final float getCurrAngle() {
        return mCurrAngleDeg;
    }

    /**
     * @hide Returns the current velocity.
     * 
     * @return The original velocity less the deceleration. Result may be negative.
     */
    float getCurrVelocity() {
        return mCoeffVelocity * mVelocity - DECELERATION * elapsedMillis();
    }

    /**
     * Returns the start X offset in the scroll.
     * 
     * @return The start X offset as an absolute distance from the origin.
     */
    final float getStartAngle() {
        return mStartAngleDeg;
    }

    /**
     * Return the elapsed time since beginning of the scrolling.
     * 
     * @return The elapsed time in milliseconds.
     */
    int elapsedMillis() {
        return (int) (AnimationUtils.currentAnimationTimeMillis() - mStartMillis);
    }

    /**
     * Call this when you want to know the new location. If it returns true, the animation is not
     * yet finished. loc will be altered to provide the new location.
     */
    boolean computeAngleOffset() {
        if (mFinished) {
            return false;
        }

        long systemClock = AnimationUtils.currentAnimationTimeMillis();
        long timePassed = systemClock - mStartMillis;

        if (timePassed < mDurationMillis) {
            switch (mMode) {
            case SCROLL_MODE:

                float sc = (float) timePassed / mDurationMillis;
                mCurrAngleDeg = mStartAngleDeg + Math.round(mDeltaAngleDeg * sc);
                break;

            case FLING_MODE:

                float timePassedSeconds = timePassed / 1000.0f;
                float distance = mCoeffVelocity * mVelocity * timePassedSeconds
                        - (DECELERATION * timePassedSeconds * timePassedSeconds / 2.0f);
                mCurrAngleDeg = mStartAngleDeg + Math.round(distance);
                break;
            }
            return true; // return true because animation still running
        } else {
            mFinished = true;
            return false;
        }
    }

    /**
     * Start scrolling by providing a starting point and the distance to travel.
     * 
     * @param startAngleDeg
     *            Starting angle in degrees.
     * @param dAngleDeg
     *            Delta angle to rotate, must be >= 1 degree because rotation engine rounds step
     *            size.
     * @param durationMillis
     *            Duration of the scroll in milliseconds.
     */
    void startRotate(float startAngleDeg, float dAngleDeg, int durationMillis) {
        mMode = SCROLL_MODE;
        mFinished = false;
        mDurationMillis = durationMillis;
        mStartMillis = AnimationUtils.currentAnimationTimeMillis();
        mStartAngleDeg = startAngleDeg;
        mDeltaAngleDeg = dAngleDeg;
    }

    /**
     * Start scrolling based on a fling gesture. The distance traveled will depend on the initial
     * velocity of the fling.
     * 
     * @param velocityAngle
     *            Initial velocity of the fling (X) measured in pixels per second.
     */
    void fling(float velocityAngle) {
        mMode = FLING_MODE;
        mFinished = false;

        float velocity = velocityAngle;

        mVelocity = velocity;
        mDurationMillis = (int) (250.0f * Math.sqrt(2.0f * mCoeffVelocity * Math.abs(velocity)
                / DECELERATION));

        mStartMillis = AnimationUtils.currentAnimationTimeMillis();
    }
}
