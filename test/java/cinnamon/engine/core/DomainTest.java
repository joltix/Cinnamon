package cinnamon.engine.core;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;
import java.util.NoSuchElementException;

public class DomainTest
{
    // Priority per system
    private static final int SYSTEM_PRIORITY = 343;

    // Code given for all resumes and pauses
    private static final int REASON = 42;

    // Name for dummy A
    private static final String NAME_A = "system_A";

    // Name for dummy B
    private static final String NAME_B = "system_B";

    // System sort order
    private static final Comparator<GameSystem> COMPARATOR = Comparator.comparingInt(GameSystem::getPriority);

    private DummySystem mDummyA;

    private DummySystem mDummyB;

    private Domain<DummySystem> mDomain;

    @Before
    public void setUp()
    {
        mDomain = new Domain<>(COMPARATOR);
        mDummyA = new DummySystem(SYSTEM_PRIORITY);
        mDummyB = new DummySystem(SYSTEM_PRIORITY);
    }

    @After
    public void tearDown()
    {
        mDomain = null;
    }

    @Test
    public void testConstructor()
    {
        new Domain(COMPARATOR);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorComparatorNPE()
    {
        new Domain(null);
    }

    @Test
    public void testAddSystem()
    {
        mDomain.addSystem(NAME_A, mDummyA);
    }

    @Test (expected = NullPointerException.class)
    public void testAddSystemNPEName()
    {
        mDomain.addSystem(null, mDummyA);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddSystemIAENameInUse()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.addSystem(NAME_A, mDummyB);
    }

    @Test (expected = NullPointerException.class)
    public void testAddSystemNPESystem()
    {
        mDomain.addSystem(NAME_A, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddSystemIAESystemInUse()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.addSystem(NAME_B, mDummyA);
    }

    @Test
    public void testRemoveSystem()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.removeSystem(NAME_A);
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveSystemNPEName()
    {
        mDomain.removeSystem(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testRemoveSystemNSEE()
    {
        mDomain.removeSystem(NAME_A);
    }

    @Test
    public void testGetSystem()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        Assert.assertEquals(mDummyA, mDomain.getSystem(NAME_A));
    }

    @Test (expected = NullPointerException.class)
    public void testGetSystemNPEName()
    {
        mDomain.getSystem(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetSystemNSEE()
    {
        mDomain.getSystem(NAME_A);
    }

    @Test
    public void testPauseSystem()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test (expected = NullPointerException.class)
    public void testPauseSystemNPEName()
    {
        mDomain.pauseSystem(null, REASON);
    }

    @Test (expected = NoSuchElementException.class)
    public void testPauseSystemNSEE()
    {
        mDomain.addSystem(NAME_B, mDummyB);
        mDomain.startSystems();

        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testPauseSystemISESystemsNotYetStarted()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test
    public void testPauseSystemCallsOnPause()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.pauseSystem(NAME_A, REASON);

        Assert.assertEquals(1, mDummyA.mPauseCount);
    }

    @Test
    public void testPauseSystemDoesNothingIfAlreadyPaused()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        Assert.assertEquals(0, mDummyA.mPauseCount);
    }

    @Test
    public void testResumeSystem()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test (expected = NullPointerException.class)
    public void testResumeSystemNPEName()
    {
        mDomain.resumeSystem(null, REASON);
    }

    @Test (expected = NoSuchElementException.class)
    public void testResumeSystemNSEEName()
    {
        mDomain.addSystem(NAME_B, mDummyA);
        mDomain.startSystems();

        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testResumeSystemISESystemsNotYetStarted()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test
    public void testResumeSystemCallsOnResume()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.resumeSystem(NAME_A, REASON);

        Assert.assertEquals(1, mDummyA.mResumeCount);
    }

    @Test
    public void testResumeSystemDoesNothingIfNotPaused()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.resumeSystem(NAME_A, REASON);

        Assert.assertEquals(0, mDummyA.mResumeCount);
    }

    @Test
    public void testStartSystems()
    {
        mDomain.startSystems();
    }

    @Test
    public void testStartSystemsCallsOnStart()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);

        mDomain.startSystems();

        Assert.assertEquals(1, mDummyA.mStartCount);
        Assert.assertEquals(1, mDummyB.mStartCount);
    }

    @Test (expected = IllegalStateException.class)
    public void testStartSystemsISESystemsAlreadyStarted()
    {
        mDomain.addSystem(NAME_A, mDummyA);

        mDomain.startSystems();

        mDomain.startSystems();
    }

    @Test
    public void testPauseSystems()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testPauseSystemsISESystemsNotYetStarted()
    {
        mDomain.pauseSystems(REASON);
    }

    @Test
    public void testPauseSystemsCallsOnPause()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);

        Assert.assertEquals(1, mDummyA.mPauseCount);
        Assert.assertEquals(1, mDummyB.mPauseCount);
    }

    @Test
    public void testPauseSystemsDoesNothingIfAlreadyPaused()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.pauseSystems(REASON);

        Assert.assertEquals(1, mDummyA.mPauseCount);
        Assert.assertEquals(1, mDummyB.mPauseCount);
    }

    @Test
    public void testResumeSystems()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.resumeSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testResumeSystemsISESystemsNotYetStarted()
    {
        mDomain.resumeSystems(REASON);
    }

    @Test
    public void testResumeSystemsCallsOnResume()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.resumeSystems(REASON);

        Assert.assertEquals(1, mDummyA.mResumeCount);
        Assert.assertEquals(1, mDummyB.mResumeCount);
    }

    @Test
    public void testResumeSystemsDoesNothingIfNotPaused()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);
        mDomain.startSystems();

        mDomain.resumeSystems(REASON);

        Assert.assertEquals(0, mDummyA.mResumeCount);
        Assert.assertEquals(0, mDummyB.mResumeCount);
    }

    @Test
    public void testStopSystems()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.startSystems();

        mDomain.stopSystems();
    }

    @Test
    public void testStopSystemsCallsOnStop()
    {
        mDomain.addSystem(NAME_A, mDummyA);
        mDomain.addSystem(NAME_B, mDummyB);

        mDomain.startSystems();
        mDomain.stopSystems();

        Assert.assertEquals(1, mDummyA.mStopCount);
        Assert.assertEquals(1, mDummyB.mStopCount);
    }

    @Test (expected = IllegalStateException.class)
    public void testStopSystemsISESystemsHaventStarted()
    {
        mDomain.stopSystems();
        mDomain.stopSystems();
    }

    @Test
    public void testRunPerSystem()
    {
        mDomain.callWithSystems((system) -> {});
    }

    @Test (expected = NullPointerException.class)
    public void testRunPerSystemNPEAction()
    {
        mDomain.callWithSystems(null);
    }

    /**
     * This {@code GameSystem} subclass records the number of times {@code onStart()}, {@code onPause(int)},
     * {@code onResume(int)}, and {@code onStop()} are called.
     */
    static class DummySystem extends GameSystem
    {
        private int mStartCount = 0;

        private int mPauseCount = 0;

        private int mResumeCount = 0;

        private int mStopCount = 0;

        protected DummySystem(int priority)
        {
            super(priority);
        }

        @Override
        protected void onStart()
        {
            mStartCount++;
        }

        @Override
        protected void onPause(int reason)
        {
            mPauseCount++;
        }

        @Override
        protected void onResume(int reason)
        {
            mResumeCount++;
        }

        @Override
        protected void onStop()
        {
            mStopCount++;
        }
    }
}
