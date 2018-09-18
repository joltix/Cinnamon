package cinnamon.engine.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LoopMeasureTest
{
    // Number of iteration durations to account for during computations
    private static final int SAMPLE_COUNT = 2;

    // How long a loop should last, in milliseconds
    private static final long LOOP_DURATION = 2_000L;

    // Time spent per iteration, represented as a fraction of the number of milliseconds in a second
    private static final long ITERATION_LOAD = 750L;

    // Iterations per second
    private static final int RATE = 30;

    // This rate should not be feasible on the testing machine
    private static final int UNREASONABLE_RATE = 100_000_000;

    private LoopMeasure mMeasure;

    @Before
    public void setUp()
    {
        mMeasure = new LoopMeasure(SAMPLE_COUNT);
    }

    @After
    public void tearDown()
    {
        mMeasure = null;
    }

    @Test
    public void testConstructor()
    {
        new LoopMeasure(SAMPLE_COUNT);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEInvalidSampleCount()
    {
        new LoopMeasure(0);
    }

    @Test
    public void testMarkLoopBegins()
    {
        mMeasure.markLoopBegins(0L);
    }

    @Test (expected = IllegalStateException.class)
    public void testMarkLoopBeginsISECalledMoreThanOnce()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markLoopBegins(0L);
    }

    @Test
    public void testMarkIterationBegins()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markIterationBegins(0L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMarkIterationBeginsIAEIterationStartOccursBeforeLoopStart()
    {
        mMeasure.markLoopBegins(1L);
        mMeasure.markIterationBegins(0L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMarkIterationBeginsIAEIterationStartOccursBeforePreviousIterationStart()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markIterationBegins(1L);
        mMeasure.markIterationBegins(0L);
    }

    @Test
    public void testMarkIterationEnds()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markIterationBegins(0L);
        mMeasure.markIterationEnds(0L);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMarkIterationEndsIAEIterationStopOccursBeforeIterationStart()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markIterationBegins(1L);
        mMeasure.markIterationEnds(0L);
    }

    @Test (expected = IllegalStateException.class)
    public void testMarkIterationEndsISEIterationStartWasNotMarkedPriorToStop()
    {
        mMeasure.markLoopBegins(0L);
        mMeasure.markIterationEnds(0L);
    }

    @Test
    public void testGetRateReturnsExpectedMeasurement()
    {
        mMeasure.markLoopBegins(System.nanoTime());

        TestUtils.loopAtRate(LOOP_DURATION, () ->
        {
            mMeasure.markIterationBegins(System.nanoTime());
            mMeasure.markIterationEnds(System.nanoTime());
        }, RATE);

        final int measured = mMeasure.getRate();
        final String format = "Expected rate(%d) +1/-1 but measured %d instead";
        final String msg = String.format(format, RATE, measured);

        Assert.assertTrue(msg, Math.abs(RATE - mMeasure.getRate()) <= 1);
    }

    @Test
    public void testGetRateReturnsMuchSlowerRateThanUnreasonablyHighTarget()
    {
        mMeasure.markLoopBegins(System.nanoTime());

        TestUtils.loopAtRate(LOOP_DURATION, () ->
        {
            mMeasure.markIterationBegins(System.nanoTime());
            mMeasure.markIterationEnds(System.nanoTime());
        }, UNREASONABLE_RATE);

        final int measured = mMeasure.getRate();
        final String format = "Expected rate slower than %d iterations per second but instead measured %d";
        final String msg = String.format(format, UNREASONABLE_RATE, measured);

        Assert.assertTrue(msg, measured < UNREASONABLE_RATE);
    }

    @Test
    public void testGetAverageDurationReturnsMeasurementWithinBounds()
    {
        loopAndMeasure();

        final double measured = mMeasure.getAverageDuration();
        final String format = "Expected average duration < %f ms but instead measured %f";
        final double expectedDuration = 1_000d / RATE;
        final String msg = String.format(format, expectedDuration, measured);

        Assert.assertTrue(msg, measured < expectedDuration);
    }

    @Test
    public void testGetMinimumDurationReturnsPositiveDuration()
    {
        loopAndMeasure();

        final double measured = mMeasure.getAverageDuration();
        final String format = "Expected positive minimum duration but instead measured %f ms";
        final String msg = String.format(format, measured);

        Assert.assertTrue(msg, measured > 0d);
    }

    @Test
    public void testGetMaximumDurationReturnsPositiveDuration()
    {
        loopAndMeasure();

        final double measured = mMeasure.getAverageDuration();
        final String format = "Expected positive minimum duration but instead measured %f ms";
        final String msg = String.format(format, measured);

        Assert.assertTrue(msg, measured > 0d);
    }

    private void loopAndMeasure()
    {
        mMeasure.markLoopBegins(System.nanoTime());

        TestUtils.loopAtRate(LOOP_DURATION, () ->
        {
            mMeasure.markIterationBegins(System.nanoTime());

            try {
                Thread.sleep(ITERATION_LOAD / RATE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            mMeasure.markIterationEnds(System.nanoTime());
        }, RATE);
    }
}
