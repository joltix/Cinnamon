package com.cinnamon.system;

/**
 * <p>
 *     Measures average rates per second when given a sample size and {@link #log()} is called each loop. An average of
 *     <i>x</i> samples is computed as a loop rate and may be retrieved with {@link #getRate()}.
 * </p>
 *
 * <p>
 *     {@link #start()} is an optional method meant to provide a more accurate initial timestamp if a lengthy operation
 *     occurs between the RateLogger's instantiation and the first call to log(). {@link #RateLogger(int)} will also
 *     record the initial timestamp and will act the same as {@link #start()} if no operation stands between it and
 *     the ideal starting point for measurement.
 * </p>
 *
 * <p>
 *      Sample rates are recorded with nanosecond accuracy, as provided by {@link System#nanoTime()}.
 * </p>
 */
public class RateLogger
{
    // Nanoseconds in a second
    private static final long NANO_IN_SEC = 1000000000L;

    // Nanosecond stamp of last log
    private long mEnd;

    // Number of loops so far
    private int mLoopCount;

    // Most recent computed average loop rate
    private int mLoopRate;

    // Nanosecond sum counter to check when to average loops
    private long mTimeAccum;

    // Samples storage
    private long[] mSamples;

    // Index of current sample to measure
    private int mSampleIndex;

    /**
     * <p>Constructor for a RateLogger.</p>
     *
     * @param samples number of samples required before averaging.
     * @throws IllegalArgumentException if the sample size < 1.
     */
    public RateLogger(int samples)
    {
        if (samples < 1) {
            throw new IllegalArgumentException("There must be at least 1 sample");
        }

        mSamples = new long[samples];

        // Record initial time
        start();
    }

    /**
     * <p>Records the time as an initial timestamp in order to measure the duration of the first loop after the first
     * call to {@link #log()}.</p>
     *
     * <p>This method is meant to be called only once prior to the first {@link #log()} of the looping behavior to
     * measure. Calling again while looping may produce inaccurate rates.</p>
     */
    public final void start()
    {
        mEnd = System.nanoTime();
    }

    /**
     * <p>Records the time relative to the last call to this method. This method should be executed at the end of each
     * loop in order to measure its per second rate.</p>
     */
    public final void log()
    {
        // Time of current log
        final long current = System.nanoTime();

        // Add up how much time since last log
        mTimeAccum += (current - mEnd);

        // Update last log time as current in anticipation of next log
        mEnd = current;

        // For (at least) each second
        if (mTimeAccum >= NANO_IN_SEC) {
            // Record the number of loops that occurred if still have samples left
            if (mSampleIndex < mSamples.length) {
                mSamples[mSampleIndex++] = mLoopCount;
            } else {
                // Compute and update current loop rate
                mLoopRate = average();
            }

            // Reset for next batch of samples
            reset();
        } else {
            // Count loop
            mLoopCount++;
        }
    }

    /**
     * <p>Gets the latest rate. This rate is an average of a number of recorded rates, which is determined by
     * {@link #getSampleSize()}.</p>
     *
     * @return latest rate.
     */
    public final int getRate()
    {
        return mLoopRate;
    }

    /**
     * <p>Gets the number of samples used to compute an average loop rate.</p>
     *
     * @return sample size.
     */
    public final int getSampleSize()
    {
        return mSamples.length;
    }

    /**
     * <p>Computes the average number of loops from all the recorded loop rate samples.</p>
     *
     * @return average loop rate.
     */
    private int average()
    {
        // Sum all sample durations
        long sum = 0;
        for (int i = 0; i < mSamples.length; i++) {
            sum += mSamples[i];
        }

        // Compute average rounded up
        return Math.round((float) sum / mSamples.length);
    }

    /**
     * <p>Resets the number of nanoseconds and loops being recorded per second.</p>
     */
    private void reset()
    {
        mTimeAccum = 0;
        mLoopCount = 0;
    }
}