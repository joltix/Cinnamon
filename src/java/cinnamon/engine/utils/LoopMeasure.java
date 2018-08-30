package cinnamon.engine.utils;

/**
 * Measures the rate and iteration duration of looping behavior.
 *
 * <p>Since a looping construct's runtime can be retrieved through {@code endTime - startTime}, this class
 * forgoes this measurement. The term 'duration' then refers to the time between the beginning and ending points of
 * an iteration {@code i}, before {@code i + 1} begins.</p>
 *
 * <h3>Usage</h3>
 * <p>While the following example calls {@link #markLoopBegins(long)}, this method may be omitted when duration is the
 * only desired metric. This is not reciprocal; the other marking methods are always needed.</p>
 * <pre>
 *     <code>
 *
 *         LoopMeasure measure = new LoopMeasure(2);
 *         measure.markLoopBegins(System.nanotime());
 *
 *         while (true) {
 *             measure.markIterationBegins(System.nanotime());
 *
 *             // Do work here
 *
 *             measure.markIterationEnds(System.nanotime());
 *         }
 *     </code>
 * </pre>
 */
public final class LoopMeasure
{
    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    // Loop begins
    private long mLoopingStartTime = Long.MIN_VALUE;

    // Iteration begins
    private long mIterationStartTime = Long.MIN_VALUE;

    private boolean mIterationStarted = false;

    // Temporary iteration counter for under 1 second
    private int mIterationsSoFar;

    // Iterations per second
    private int mLoopRate;

    // Average iteration duration
    private double mAverageDur;

    // Shortest duration
    private double mMinDur;

    // Longest duration
    private double mMaxDur;

    // Iteration durations
    private final long[] mDurationSamples;

    // Next available iteration duration
    private int mSampleIndex = 0;

    // Loop rate measurement can only be started once
    private boolean mLoopStarted = false;

    // Cached average must be recomputed
    private boolean mDirtyAverage = false;

    // Cached min must be recomputed
    private boolean mDirtyMinDur = false;

    // Cached max must be recomputed
    private boolean mDirtyMaxDur = false;

    /**
     * Constructs a {@code LoopMeasure}.
     *
     * @param samples number of loops to use in data set.
     * @throws IllegalArgumentException if samples {@literal <} 1.
     */
    public LoopMeasure(int samples)
    {
        if (samples < 1) {
            final String format = "Number of samples must be >= 1, given: %d";
            throw new IllegalArgumentException(String.format(format, samples));
        }

        mDurationSamples = new long[samples];
    }

    /**
     * Marks the loop's start time for rate measurement.
     *
     * <p>This should only be called once at the beginning, before any call to {@link #markIterationBegins(long)} and
     * work begins. While this loop marks the start of looping behavior, a call to this method should still be
     * followed by {@code markIterationBegins(long)}.</p>
     *
     * <p>The given timestamp can be the same as the first iteration's start time.</p>
     *
     * @param start time in nanoseconds.
     * @throws IllegalStateException if called more than once.
     */
    public void markLoopBegins(long start)
    {
        if (mLoopStarted) {
            throw new IllegalStateException("Rate measurement already started - this should only be called once");
        }

        mLoopingStartTime = start;
        mLoopStarted = true;
    }

    /**
     * Marks an iteration's start time.
     *
     * <p>This should be called before any of an iteration's work has completed.</p>
     *
     * @param start time in nanoseconds.
     * @throws IllegalArgumentException if start {@literal <} either the start time from the previous call to this
     * method or the start time given to {@link #markLoopBegins(long)}.
     * @see #markIterationEnds(long)
     */
    public void markIterationBegins(long start)
    {
        if (start < mIterationStartTime) {
            throw new IllegalArgumentException("Iteration start time must be >= the previous iteration's start time");
        }
        if (start < mLoopingStartTime) {
            throw new IllegalArgumentException("Iteration start time must be >= the iteration's start time");
        }

        mIterationStartTime = start;
        mIterationStarted = true;
    }

    /**
     * Marks an iteration's end time.
     *
     * <p>This should be called after all of an iteration's work has completed.</p>
     *
     * @param stop time in nanoseconds.
     * @throws IllegalArgumentException if stop {@literal <} the start time from the most recent call to
     * {@code markIterationBegins(long)}.
     * @throws IllegalStateException if the iteration's start time was not previously marked.
     * @see #markIterationBegins(long)
     */
    public void markIterationEnds(long stop)
    {
        if (stop < mIterationStartTime) {
            throw new IllegalArgumentException("Iteration stop time must be >= the iteration's start");
        }
        if (!mIterationStarted) {
            throw new IllegalStateException("Iteration's beginning should be marked prior to end");
        }

        mIterationsSoFar++;
        recordLoopDuration(stop - mIterationStartTime);

        // At least 1 second has passed since last measurement
        if (stop - mLoopingStartTime >= NANOSECONDS_PER_SECOND) {

            mLoopRate = mIterationsSoFar;
            mIterationsSoFar = 0;
            mLoopingStartTime = stop;
        }

        mIterationStarted = false;
    }

    /**
     * Gets the average loop duration.
     *
     * @return average in milliseconds.
     */
    public double getAverageDuration()
    {
        if (mDirtyAverage) {
            computeAverageDuration();
            mDirtyAverage = false;
        }

        return mAverageDur;
    }

    /**
     * Gets the minimum loop duration.
     *
     * @return minimum in milliseconds.
     */
    public double getMinimumDuration()
    {
        if (mDirtyMinDur) {
            computeMinimumDuration();
            mDirtyMinDur = false;
        }

        return mMinDur;
    }

    /**
     * Gets the maximum loop duration.
     *
     * @return maximum in milliseconds.
     */
    public double getMaximumDuration()
    {
        if (mDirtyMaxDur) {
            computeMaximumDuration();
            mDirtyMaxDur = false;
        }

        return mMaxDur;
    }

    /**
     * Gets the most recent loop rate measurement.
     *
     * @return loop rate.
     */
    public int getRate()
    {
        return mLoopRate;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void recordLoopDuration(long duration)
    {
        // Wrap around and begin overwriting oldest samples
        if (mSampleIndex >= mDurationSamples.length) {
            mSampleIndex = mSampleIndex % mDurationSamples.length;
        }

        mDurationSamples[mSampleIndex++] = duration;
        mDirtyAverage = true;
        mDirtyMinDur = true;
        mDirtyMaxDur = true;
    }

    private void computeAverageDuration()
    {
        long sum = 0;
        for (int i = 0; i < mDurationSamples.length; i++) {
            sum += mDurationSamples[i];
        }

        mAverageDur = ((double) sum / mDurationSamples.length) / NANOSECONDS_PER_MILLISECOND;
    }

    private void computeMinimumDuration()
    {
        long min = Long.MAX_VALUE;
        for (int i = 0; i < mDurationSamples.length; i++) {
            final long sample = mDurationSamples[i];

            if (sample < min) {
                min = sample;
            }
        }

        mMinDur = (double) min / NANOSECONDS_PER_MILLISECOND;
    }

    private void computeMaximumDuration()
    {
        long max = Long.MIN_VALUE;
        for (int i = 0; i < mDurationSamples.length; i++) {
            final long sample = mDurationSamples[i];

            if (sample > max) {
                max = sample;
            }
        }

        mMaxDur = (double) max / NANOSECONDS_PER_MILLISECOND;
    }
}
