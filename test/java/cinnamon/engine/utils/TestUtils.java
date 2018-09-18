package cinnamon.engine.utils;

public final class TestUtils
{
    // Number of nanoseconds per second
    public static final long NANO_PER_SEC = 1_000_000_000L;

    // Number of nanoseconds per millisecond
    public static final long NANO_PER_MILLI = 1_000_000L;

    private TestUtils() { }

    /**
     * Loops an action for a specified duration at a given rate.
     *
     * <p>{@code action} will be executed at least once regardless of {@code rate}.</p>
     *
     *  @param duration loop duration in milliseconds.
     * @param action action to execute per loop.
     * @param rate iterations per second.
     */
    public static void loopAtRate(long duration, Runnable action, int rate)
    {
        assert (duration > 0L);
        assert (action != null);
        assert (rate > 0);

        final long durationInNano = duration * NANO_PER_MILLI;
        final long targetIterationDuration = NANO_PER_SEC / rate;
        final long start = System.nanoTime();

        long previousStart = System.nanoTime();
        long iterationDuration = 0L;

        boolean executed = false;

        while (System.nanoTime() - start < durationInNano) {

            final long iterationStop = System.nanoTime();
            iterationDuration += iterationStop - previousStart;
            previousStart = iterationStop;

            // Try to reach rate
            while (iterationDuration >= targetIterationDuration) {
                action.run();
                executed = true;
                iterationDuration -= targetIterationDuration;
            }
        }

        // In case delay is so little the loop didn't even iterate once
        if (!executed) {
            action.run();
        }
    }

    /**
     * Loops an action for a specified duration.
     *
     * <p>{@code action} will be executed at least once.</p>
     *
     * @param duration loop duration in milliseconds.
     * @param action action to execute per loop.
     */
    public static void loop(long duration, Runnable action)
    {
        assert (duration > 0L);
        assert (action != null);

        final long durationInNano = duration * NANO_PER_MILLI;
        final long start = System.nanoTime();

        boolean executed = false;

        while ((System.nanoTime() - start) < durationInNano) {
            action.run();
            executed = true;
        }

        // In case delay is so little the loop didn't even iterate once
        if (!executed) {
            action.run();
        }
    }

    /**
     * Loops an action for a specified duration with a delay before another action occurs.
     *
     * <p>While {@code action} will be executed at least once, {@code oneShot} will be executed <i>at most</i> once.
     * {@code oneShot} will always be executed after {@code action}.</p>
     *
     * @param duration loop duration in milliseconds.
     * @param action action to execute per loop.
     * @param delay minimum time in milliseconds before executing the second action.
     * @param oneShot delayed secondary action.
     */
    public static void loopWithOneShot(long duration, Runnable action, long delay, Runnable oneShot)
    {
        assert (duration > 0L);
        assert (action != null);
        assert (delay > 0L);
        assert (oneShot != null);

        final long durationInNano = duration * NANO_PER_MILLI;
        final long delayInNano = delay * NANO_PER_MILLI;
        final long start = System.nanoTime();

        boolean actionExecuted = false;
        long soFar;
        boolean oneShotExecuted = false;

        while ((soFar = System.nanoTime() - start) <= durationInNano) {
            action.run();
            actionExecuted = true;

            if (soFar > delayInNano && !oneShotExecuted) {
                oneShot.run();
                oneShotExecuted = true;
            }
        }

        // In case delay is so little the loop didn't even iterate once
        if (!actionExecuted) {
            action.run();
        }
        if (!oneShotExecuted) {
            oneShot.run();
        }
    }
}
