package cinnamon.engine.core;

import cinnamon.engine.core.BaseSystem.OnPauseListener;
import cinnamon.engine.core.BaseSystem.OnResumeListener;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Comparator;

public class BaseSystemTest
{
    // Index for primary test listener
    private static final int LISTENER_A_INDEX = 0;

    // Index for secondary test listener
    private static final int LISTENER_B_INDEX = 1;

    // System priority
    private static final int PRIORITY = 343;

    // Pause/resume reason code
    private static final int REASON = 42;

    private BaseSystem mSystem;

    @Before
    public void setUp()
    {
        mSystem = new TestSystem(PRIORITY);
    }

    @After
    public void tearDown()
    {
        mSystem = null;
    }

    @Test
    public void testStart()
    {
        mSystem.start();
    }

    @Test
    public void testStop()
    {
        mSystem.stop();
    }

    @Test
    public void testPause()
    {
        mSystem.pause(REASON);
    }

    @Test
    public void testPauseNotifiesEachListenerOnce()
    {
        final int[] numberOfCalls = new int[2];

        final OnPauseListener listenerA = (system, reason) ->
        {
            numberOfCalls[LISTENER_A_INDEX]++;
        };

        final OnPauseListener listenerB = (system, reason) ->
        {
            numberOfCalls[LISTENER_B_INDEX]++;
        };

        mSystem.addOnPauseListener(listenerA);
        mSystem.addOnPauseListener(listenerB);
        mSystem.pause(REASON);

        for (final int callCount : numberOfCalls) {
            Assert.assertEquals(1, callCount);
        }
    }

    @Test
    public void testResume()
    {
        mSystem.resume(REASON);
    }

    @Test
    public void testResumeNotifiesEachListenerOnce()
    {
        final int[] listenerCalls = new int[2];

        // Record number of times listener A is notified
        final OnResumeListener listenerA = (system, reason) ->
        {
            listenerCalls[LISTENER_A_INDEX]++;
        };

        // Record number of times listener B is notified
        final OnResumeListener listenerB = (system, reason) ->
        {
            listenerCalls[LISTENER_B_INDEX]++;
        };

        mSystem.addOnResumeListener(listenerA);
        mSystem.addOnResumeListener(listenerB);
        mSystem.resume(REASON);

        for (final int callCount : listenerCalls) {
            Assert.assertEquals(1, callCount);
        }
    }

    @Test
    public void testGetPriorityMatchesConstructorArgument()
    {
        Assert.assertEquals(PRIORITY, mSystem.getPriority());
    }

    @Test
    public void testIsPaused()
    {
        mSystem.pause(REASON);

        Assert.assertTrue(mSystem.isPaused());
    }

    @Test
    public void testIsPausedReturnsFalse()
    {
        Assert.assertFalse(mSystem.isPaused());
    }

    @Test
    public void testAddOnPauseListener()
    {
        mSystem.addOnPauseListener((system, reason) -> {});
    }

    @Test (expected = NullPointerException.class)
    public void testAddOnPauseListenerNPEListener()
    {
        mSystem.addOnPauseListener(null);
    }

    @Test
    public void testRemoveOnPauseListener()
    {
        mSystem.removeOnPauseListener((system, reason) -> {});
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveOnPauseListenerNPEListener()
    {
        mSystem.removeOnPauseListener(null);
    }

    @Test
    public void testAddOnResumeListener()
    {
        mSystem.addOnResumeListener((system, reason) -> {});
    }

    @Test (expected = NullPointerException.class)
    public void testAddOnResumeListenerNPE()
    {
        mSystem.addOnResumeListener(null);
    }

    @Test
    public void testRemoveOnResumeListener()
    {
        mSystem.removeOnResumeListener((system, reason) -> {});
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveOnResumeListenerNPEListener()
    {
        mSystem.removeOnResumeListener(null);
    }

    @Test
    public void testSetCoordinator()
    {
        mSystem.setCoordinator(new Domain(Comparator.comparingInt(BaseSystem::getPriority)));
    }

    @Test
    public void testSetCoordinatorToNull()
    {
        mSystem.setCoordinator(null);
    }

    @Test
    public void testGetCoordinatorReturnsNull()
    {
        Assert.assertNull(mSystem.getCoordinator());
    }

    @Test
    public void testGetCoordinatorReturnsCoordinatorFromSetCoordinator()
    {
        final Domain<TestSystem> domain = new Domain<>(Comparator.comparingInt(BaseSystem::getPriority));
        mSystem.setCoordinator(domain);

        Assert.assertSame(domain, mSystem.getCoordinator());
    }

    private class TestSystem extends BaseSystem
    {
        protected TestSystem(int priority)
        {
            super(priority);
        }

        @Override
        protected void onStart() { }

        @Override
        protected void onPause(int reason) { }

        @Override
        protected void onResume(int reason) { }

        @Override
        protected void onStop() { }
    }
}
