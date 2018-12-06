package cinnamon.engine.object;

import cinnamon.engine.object.EntityManager.Tuner;
import cinnamon.engine.utils.Size;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

public class LevelManagerTest
{
    private static final String LEVEL_NAME = "Level 1";

    private static final float LEVEL_WIDTH = 1f;

    private static final float LEVEL_HEIGHT = 1f;

    private static final float LEVEL_DEPTH = 1f;

    private LevelManager mManager;

    @Before
    public void setUp()
    {
        mManager = new LevelManager(0, new Tuner(0).getManager());
    }

    @After
    public void tearDown()
    {
        mManager = null;
    }

    @Test
    public void testGetCurrentLevelReturnsNullWhenNoLevelLoaded()
    {
        Assert.assertNull(mManager.getCurrentLevel());
    }

    @Test
    public void testGetCurrentLevelReturnsLoadedLevel()
    {
        final Level level = createLevel();
        mManager.addLevel(level);
        mManager.loadLevel(LEVEL_NAME);

        // Force level load
        mManager.onTick();

        Assert.assertSame(level, mManager.getCurrentLevel());
    }

    @Test
    public void testGetLevelReturnsExpectedLevel()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        Assert.assertSame(level, mManager.getLevel(LEVEL_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testGetLevelNPEName()
    {
        mManager.getLevel(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testGetLevelNSEEUnrecognizedName()
    {
        mManager.getLevel(LEVEL_NAME);
    }

    @Test
    public void testLoadLevel()
    {
        mManager.addLevel(createLevel());

        mManager.loadLevel(LEVEL_NAME);
    }

    @Test (expected = NullPointerException.class)
    public void testLoadLevelNPEName()
    {
        mManager.loadLevel(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testLoadLevelNSEEUnrecognizedName()
    {
        mManager.loadLevel(LEVEL_NAME);
    }

    @Test (expected = IllegalStateException.class)
    public void testLoadLevelISELevelAlreadyLoaded()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        // Force level load
        mManager.loadLevel(LEVEL_NAME);
        mManager.onTick();

        mManager.loadLevel(LEVEL_NAME);
    }

    @Test
    public void testUnloadLevelMakesLevelNoLongerLoaded()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        // Force level load
        mManager.loadLevel(LEVEL_NAME);
        mManager.onTick();

        // Force level unload
        mManager.unloadLevel(LEVEL_NAME);
        mManager.onTick();

        Assert.assertFalse(level.isLoaded());
    }

    @Test
    public void testUnloadLevelCancelsMatchingPendingLoad()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        mManager.loadLevel(LEVEL_NAME);
        mManager.unloadLevel(LEVEL_NAME);

        // Attempt to force level load
        mManager.onTick();

        Assert.assertFalse(level.isLoaded());
        Assert.assertNotSame(level, mManager.getCurrentLevel());
    }

    @Test (expected = NullPointerException.class)
    public void testUnloadLevelNPEName()
    {
        mManager.unloadLevel(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testUnloadLevelNSEENameDoesNotMatchLoadedLevel()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        // Force level load
        mManager.loadLevel(LEVEL_NAME);
        mManager.onTick();

        mManager.unloadLevel(LEVEL_NAME + LEVEL_NAME);
    }

    @Test
    public void testIsNameInUseReturnsTrue()
    {
        mManager.addLevel(createLevel());

        Assert.assertTrue(mManager.isNameInUse(LEVEL_NAME));
    }

    @Test
    public void testIsNameInUseReturnsFalse()
    {
        Assert.assertFalse(mManager.isNameInUse(LEVEL_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testIsNameInUseNPEName()
    {
        mManager.isNameInUse(null);
    }

    @Test
    public void testAddLevel()
    {
        mManager.addLevel(createLevel());
    }

    @Test (expected = NullPointerException.class)
    public void testAddLevelNPELevel()
    {
        mManager.addLevel(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddLevelIAENameAlreadyInUse()
    {
        mManager.addLevel(createLevel());

        mManager.addLevel(createLevel());
    }

    @Test
    public void testRemoveLevelReturnsExpectedLevel()
    {
        final Level level = createLevel();
        mManager.addLevel(level);

        Assert.assertSame(level, mManager.removeLevel(LEVEL_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveLevelNPEName()
    {
        mManager.removeLevel(null);
    }

    @Test (expected = IllegalStateException.class)
    public void testRemoveLevelISELevelStillLoaded()
    {
        mManager.addLevel(createLevel());
        mManager.loadLevel(LEVEL_NAME);

        // Force level load
        mManager.onTick();

        mManager.removeLevel(LEVEL_NAME);
    }

    private Level createLevel()
    {
        return new Level(LEVEL_NAME, new Size()
            {
                @Override
                public float getWidth()
                {
                    return LEVEL_WIDTH;
                }

                @Override
                public float getHeight()
                {
                    return LEVEL_HEIGHT;
                }

                @Override
                public float getDepth()
                {
                    return LEVEL_DEPTH;
                }
            })
        {
            @Override
            public void onLoad() { }

            @Override
            public void onUnload() { }
        };
    }
}