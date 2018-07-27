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

    // Name for test system A
    private static final String NAME_A = "system_A";

    // Name for test system B
    private static final String NAME_B = "system_B";

    // System sort order
    private static final Comparator<BaseSystem> COMPARATOR = Comparator.comparingInt(BaseSystem::getPriority);

    private TestSystem mSystemA;

    private TestSystem mSystemB;

    private Domain<TestSystem> mDomain;

    @Before
    public void setUp()
    {
        mDomain = new Domain<>(COMPARATOR);
        mSystemA = new TestSystem(SYSTEM_PRIORITY);
        mSystemB = new TestSystem(SYSTEM_PRIORITY);
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
        mDomain.addSystem(NAME_A, mSystemA);
    }

    @Test (expected = NullPointerException.class)
    public void testAddSystemNPEName()
    {
        mDomain.addSystem(null, mSystemA);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddSystemIAENameInUse()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.addSystem(NAME_A, mSystemB);
    }

    @Test (expected = NullPointerException.class)
    public void testAddSystemNPESystem()
    {
        mDomain.addSystem(NAME_A, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddSystemIAESystemInUse()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.addSystem(NAME_B, mSystemA);
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromStartSystems()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromStopSystems()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
        mDomain.stopSystems();
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromPauseSystems()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromResumeSystems()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
        mDomain.resumeSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromPauseSystem()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromResumeSystem()
    {
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISETriedToCallFromCallWithSystems()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.callWithSystems((system) ->
        {
            mDomain.addSystem(NAME_B, mSystemB);
        });
    }

    @Test
    public void testRemoveSystem()
    {
        mDomain.addSystem(NAME_A, mSystemA);

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

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromStartSystems()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromStopSystems()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
        mDomain.stopSystems();
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromPauseSystems()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromResumeSystems()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
        mDomain.resumeSystems(REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromPauseSystem()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromResumeSystem()
    {
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.addSystem(NAME_A, new TestSystem(SYSTEM_PRIORITY, () ->
        {
            mDomain.removeSystem(NAME_B);
        }));

        mDomain.startSystems();
        mDomain.pauseSystems(REASON);
        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISETriedToCallFromCallWithSystems()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.callWithSystems((system) ->
        {
            mDomain.removeSystem(NAME_A);
        });
    }

    @Test
    public void testGetSystem()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        Assert.assertEquals(mSystemA, mDomain.getSystem(NAME_A));
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
        mDomain.addSystem(NAME_A, mSystemA);
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
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.startSystems();

        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testPauseSystemISESystemsNotYetStarted()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.pauseSystem(NAME_A, REASON);
    }

    @Test
    public void testPauseSystemCallsOnPause()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();

        mDomain.pauseSystem(NAME_A, REASON);

        Assert.assertEquals(1, mSystemA.mPauseCount);
    }

    @Test
    public void testPauseSystemDoesNotCallOnPauseIfNotPausable()
    {
        mDomain.addSystem(NAME_A, new TestSystem(0)
        {
            @Override
            protected boolean isPausable()
            {
                return false;
            }
        });

        mDomain.startSystems();
        mDomain.pauseSystem(NAME_A, REASON);

        Assert.assertEquals(0, mSystemA.mPauseCount);
    }

    @Test
    public void testPauseSystemDoesNotCallOnPauseIfAlreadyPaused()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.startSystems();
        mDomain.pauseSystem(NAME_A, REASON);
        mDomain.pauseSystem(NAME_A, REASON);

        Assert.assertEquals(1, mSystemA.mPauseCount);
    }

    @Test
    public void testResumeSystem()
    {
        mDomain.addSystem(NAME_A, mSystemA);
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
        mDomain.addSystem(NAME_B, mSystemA);
        mDomain.startSystems();

        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test (expected = IllegalStateException.class)
    public void testResumeSystemISESystemsNotYetStarted()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.resumeSystem(NAME_A, REASON);
    }

    @Test
    public void testResumeSystemCallsOnResume()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.resumeSystem(NAME_A, REASON);

        Assert.assertEquals(1, mSystemA.mResumeCount);
    }

    @Test
    public void testResumeSystemDoesNotCallOnResumeIfNotPaused()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();

        mDomain.resumeSystem(NAME_A, REASON);

        Assert.assertEquals(0, mSystemA.mResumeCount);
    }

    @Test
    public void testStartSystems()
    {
        mDomain.startSystems();
    }

    @Test
    public void testStartSystemsCallsOnStart()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);

        mDomain.startSystems();

        Assert.assertEquals(1, mSystemA.mStartCount);
        Assert.assertEquals(1, mSystemB.mStartCount);
    }

    @Test (expected = IllegalStateException.class)
    public void testStartSystemsISESystemsAlreadyStarted()
    {
        mDomain.addSystem(NAME_A, mSystemA);

        mDomain.startSystems();

        mDomain.startSystems();
    }

    @Test
    public void testPauseSystems()
    {
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
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);

        Assert.assertEquals(1, mSystemA.mPauseCount);
        Assert.assertEquals(1, mSystemB.mPauseCount);
    }

    @Test
    public void testPauseSystemsDoesNothingIfAlreadyPaused()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.pauseSystems(REASON);

        Assert.assertEquals(1, mSystemA.mPauseCount);
        Assert.assertEquals(1, mSystemB.mPauseCount);
    }

    @Test
    public void testResumeSystems()
    {
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
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.startSystems();

        mDomain.pauseSystems(REASON);
        mDomain.resumeSystems(REASON);

        Assert.assertEquals(1, mSystemA.mResumeCount);
        Assert.assertEquals(1, mSystemB.mResumeCount);
    }

    @Test
    public void testResumeSystemsDoesNothingIfNotPaused()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);
        mDomain.startSystems();

        mDomain.resumeSystems(REASON);

        Assert.assertEquals(0, mSystemA.mResumeCount);
        Assert.assertEquals(0, mSystemB.mResumeCount);
    }

    @Test
    public void testStopSystems()
    {
        mDomain.startSystems();

        mDomain.stopSystems();
    }

    @Test
    public void testStopSystemsCallsOnStop()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.addSystem(NAME_B, mSystemB);

        mDomain.startSystems();
        mDomain.stopSystems();

        Assert.assertEquals(1, mSystemA.mStopCount);
        Assert.assertEquals(1, mSystemB.mStopCount);
    }

    @Test (expected = IllegalStateException.class)
    public void testStopSystemsISESystemsHaventStarted()
    {
        mDomain.stopSystems();
    }

    @Test
    public void testCallWithSystems()
    {
        mDomain.callWithSystems((system) -> { });
    }

    @Test (expected = NullPointerException.class)
    public void testCallWithSystemsNPEAction()
    {
        mDomain.callWithSystems(null);
    }

    @Test
    public void testCallWithSystemsAffectsSystemOnce()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();

        mDomain.callWithSystems((system) ->
        {
            system.mCallCount++;
        });

        Assert.assertEquals(1, mSystemA.mCallCount);
    }

    @Test
    public void testCallWithUnpausedSystems()
    {
        mDomain.callWithUnpausedSystems((system) -> { });
    }

    @Test (expected = NullPointerException.class)
    public void testCallWithUnpausedSystemsNPEAction()
    {
        mDomain.callWithUnpausedSystems(null);
    }

    @Test
    public void testCallWithUnpausedSystemsAffectsSystemOnce()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();

        mDomain.callWithUnpausedSystems((system) ->
        {
            system.mCallCount++;
        });

        Assert.assertEquals(1, mSystemA.mCallCount);
    }

    @Test
    public void testCallWithUnpausedSystemsSkipsPausedSystem()
    {
        mDomain.addSystem(NAME_A, mSystemA);
        mDomain.startSystems();
        mDomain.pauseSystem(NAME_A, REASON);

        mDomain.callWithUnpausedSystems((system) ->
        {
           system.mCallCount++;
        });

        Assert.assertEquals(0, mSystemA.mCallCount);
    }

    /**
     * This {@code BaseSystem} subclass records the number of times {@code onStart()}, {@code onPause(int)},
     * {@code onResume(int)}, and {@code onStop()} are called.
     */
    static class TestSystem extends BaseSystem
    {
        private final Runnable mAction;

        private int mStartCount = 0;

        private int mPauseCount = 0;

        private int mResumeCount = 0;

        private int mStopCount = 0;

        private int mCallCount = 0;

        protected TestSystem(int priority)
        {
            this(priority, () -> {});
        }

        protected TestSystem(int priority, Runnable action)
        {
            super(priority);
            mAction = action;
        }

        @Override
        protected void onStart()
        {
            mStartCount++;

            if (mAction != null) {
                mAction.run();
            }
        }

        @Override
        protected void onPause(int reason)
        {
            mPauseCount++;

            if (mAction != null) {
                mAction.run();
            }
        }

        @Override
        protected void onResume(int reason)
        {
            mResumeCount++;

            if (mAction != null) {
                mAction.run();
            }
        }

        @Override
        protected void onStop()
        {
            mStopCount++;

            if (mAction != null) {
                mAction.run();
            }
        }
    }
}
