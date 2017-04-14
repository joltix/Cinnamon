package com.cinnamon.utils;

/**
 * <p>
 *     Interpolates between two float values at a constant speed with progress dependent on system time.
 * </p>
 *
 * <p>
 *     To interpolate between two values, {@link #start(long, float, float)} should be called first to setup the
 *     interpolation parameters. Each call to {@link #get()} will retrieve the interpolated value at that point in
 *     time. If the desired time period has passed, subsequent calls to get() will return the target value before a
 *     new interpolation is set through start(long, float, float).
 * </p>
 */
public class TimeInterpolator
{
    // # of nanoseconds in a millisecond
    private static final long MILLI_TO_NANO = 1000000L;

    // Desired period to interpolate over
    private long mDuration;

    // Start time in nanoseconds
    private long mStartTime;

    // Initial value
    private float mFrom;

    // Target value
    private float mTo;

    // Flag marking interpolation's end
    private boolean mFinished = true;

    /**
     * <p>Sets up interpolation between two values to be completed within a period of time.</p>
     *
     * @param duration period in milliseconds.
     * @param from initial value.
     * @param to target value.
     */
    public final void start(long duration, float from, float to)
    {
        // Make sure duration > 0 ms
        if (duration <= 0L) {
            throw new IllegalArgumentException("Duration must be > 0 milliseconds: " + duration);
        }

        // Save interpolation params
        mDuration = duration * MILLI_TO_NANO;
        mFrom = from;
        mTo = to;

        // Keep track of starting time and reset finished flag
        mStartTime = System.nanoTime();
        mFinished = false;
    }

    /**
     * <p>Gets the next interpolated value. If interpolation is finished, further calls to this method will return
     * the target value originally given in {@link #start(long, float, float)}.</p>
     *
     * @return next value.
     */
    public final float get()
    {
        // Compute fraction representing current moment out of desired duration
        final float progress = (float) (System.nanoTime() - mStartTime) / mDuration;

        // progress < 1 means not done interpolating
        if (progress < 1f) {
            return interpolate(mFrom, mTo, progress);
        }

        // Reached time constraint
        mFinished = true;
        return mTo;
    }

    /**
     * <p>Interpolates a value between <i>from</i> and <i>to</i> at a percentage representing progress towards the
     * end of the expected duration.
     *
     * @param from initial value.
     * @param to target value.
     * @param progress fraction of duration.
     * @return value at <i>progress</i> between <i>from</i> and <i>to</i>.
     */
    protected float interpolate(float from, float to, float progress)
    {
        return from + ((to - from) * progress);
    }

    /**
     * <p>Checks if the interpolation duration has been reached.</p>
     *
     * @return true if done interpolating.
     */
    public final boolean isFinished()
    {
        return mFinished;
    }
}
