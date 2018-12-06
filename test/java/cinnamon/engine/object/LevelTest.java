package cinnamon.engine.object;

import cinnamon.engine.object.Level.WorldSystem;
import cinnamon.engine.utils.Size;

import org.junit.Assert;
import org.junit.Test;

public class LevelTest
{
    private static final String LEVEL_NAME = "dummy_level";

    private static final String SYSTEM_NAME = "dummy_level";

    @Test
    public void testConstructor()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPESize()
    {
        new Level(LEVEL_NAME, null)
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEWidthNotANumber()
    {
        new Level(LEVEL_NAME, createSize(Float.NaN, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHeightNotANumber()
    {
        new Level(LEVEL_NAME, createSize(1f, Float.NaN, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEDepthNotANumber()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, Float.NaN))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEWidthPositiveInfinity()
    {
        new Level(LEVEL_NAME, createSize(Float.POSITIVE_INFINITY, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEWidthNegativeInfinity()
    {
        new Level(LEVEL_NAME, createSize(Float.NEGATIVE_INFINITY, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHeightPositiveInfinity()
    {
        new Level(LEVEL_NAME, createSize(1f, Float.POSITIVE_INFINITY, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHeightNegativeInfinity()
    {
        new Level(LEVEL_NAME, createSize(1f, Float.NEGATIVE_INFINITY, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEDepthPositiveInfinity()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, Float.POSITIVE_INFINITY))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEDepthNegativeInfinity()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, Float.NEGATIVE_INFINITY))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEWidthNotPositive()
    {
        new Level(LEVEL_NAME, createSize(0f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEWidthLargerThanMaximumSize()
    {
        new Level(LEVEL_NAME, createSize(Level.MAX_SIZE + 1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHeightNotPositive()
    {
        new Level(LEVEL_NAME, createSize(1f, 0f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEHeightLargerThanMaximumSize()
    {
        new Level(LEVEL_NAME, createSize(1f, Level.MAX_SIZE + 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEDepthNotPositive()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, 0f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEDepthLargerThanMaximumSize()
    {
        new Level(LEVEL_NAME, createSize(1f, 1f, Level.MAX_SIZE + 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };
    }

    @Test
    public void testPauseSystemPausesWhenLevelIsLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);
        level.load();

        level.pauseSystem(SYSTEM_NAME, 0);

        Assert.assertTrue(system.isPaused());
    }

    @Test
    public void testPauseSystemDoesNotPauseWhenLevelNotLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);
        level.pauseSystem(SYSTEM_NAME, 0);

        Assert.assertFalse(system.isPaused());
    }

    /**
     * resumeSystem(String, int)'s effect on a system is not tested in the case where the level is not loaded because
     * the system is not pausable when the level is unloaded. If the level is unloaded after the system is paused,
     * the system reverts to an unpaused state.
     */
    @Test
    public void testResumeSystemResumesWhenLevelIsLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);
        level.load();
        level.pauseSystem(SYSTEM_NAME, 0);

        level.resumeSystem(SYSTEM_NAME, 0);

        Assert.assertFalse(system.isPaused());
    }

    @Test
    public void testAddSystemAddsWhenLevelNotLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);

        Assert.assertSame(system, level.getSystem(SYSTEM_NAME));
    }

    @Test (expected = IllegalStateException.class)
    public void testAddSystemISELevelLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.load();

        level.addSystem(SYSTEM_NAME, system);
    }

    @Test
    public void testRemoveSystemRemovesWhenLevelNotLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);

        level.removeSystem(SYSTEM_NAME);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveSystemISELevelLoaded()
    {
        final Level level = new Level(LEVEL_NAME, createSize(1f, 1f, 1f))
        {
            @Override
            protected void onLoad() { }

            @Override
            protected void onUnload() { }
        };

        final WorldSystem system = new DummyWorldSystem(0);
        level.addSystem(SYSTEM_NAME, system);
        level.load();

        level.removeSystem(SYSTEM_NAME);
    }

    private Size createSize(float width, float height, float depth)
    {
        return new Size()
        {
            @Override
            public float getWidth()
            {
                return width;
            }

            @Override
            public float getHeight()
            {
                return height;
            }

            @Override
            public float getDepth()
            {
                return depth;
            }
        };
    }

    static class DummyWorldSystem extends WorldSystem
    {
        protected DummyWorldSystem(int priority)
        {
            super(priority);
        }

        @Override
        protected void onUpdate() { }

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