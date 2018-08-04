package cinnamon.engine.core;

import cinnamon.engine.core.GameTestSuite.TestCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.NoSuchElementException;

import static cinnamon.engine.core.GameTestSuite.*;

public class GameTest
{
    // How long game should run, in milliseconds
    private static final long GAME_DURATION = 4000L;

    private static final int TEST_SPECIFIC_TICK_RATE = 70;

    private Game mGame;

    @Before
    public void setUp()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        properties.put(Game.TICK_RATE, TEST_SPECIFIC_TICK_RATE);

        mGame = new AutoStopGame(new TestCanvas(), properties, GAME_DURATION);
    }

    @After
    public void tearDown()
    {
        mGame.stop();
        mGame = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPECanvas()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        new AutoStopGame(null, properties);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEProperties()
    {
        new AutoStopGame(new TestCanvas(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEUnexpectedValueType()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        properties.put(Game.TITLE, 42f);

        new AutoStopGame(new TestCanvas(), properties);
    }

    @Test (expected = NoSuchElementException.class)
    public void testConstructorNSEEPropertyHasMissingValue()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        properties.put(Game.TITLE, null);

        new AutoStopGame(new TestCanvas(), properties);
    }

    @Test
    public void testGetTitleReturnsExpectedTitle()
    {
        Assert.assertEquals(GameTestSuite.TITLE, mGame.getStringProperty(Game.TITLE));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetTitleIAEUnmodifiableProperty()
    {
        mGame.setStringProperty(Game.TITLE, "Game");
    }

    @Test
    public void testGetDeveloperReturnsExpectedDeveloper()
    {
        Assert.assertEquals(GameTestSuite.DEVELOPER, mGame.getStringProperty(Game.DEVELOPER));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeveloperIAEUnmodifiableProperty()
    {
        mGame.setStringProperty(Game.DEVELOPER, "Developer");
    }

    @Test
    public void testGetBuildReturnsExpectedBuild()
    {
        Assert.assertEquals(GameTestSuite.BUILD, mGame.getDoubleProperty(Game.BUILD), 0d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBuildIAEUnmodifiableProperty()
    {
        mGame.setDoubleProperty(Game.BUILD, 343.343d);
    }

    @Test
    public void testGetTickRateReturnsExpectedTickRate()
    {
        Assert.assertEquals(TEST_SPECIFIC_TICK_RATE, mGame.getIntegerProperty(Game.TICK_RATE));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetTickRateIAEUnmodifiableProperty()
    {
        mGame.setIntegerProperty(Game.TICK_RATE, 343);
    }

    @Test
    public void testGetMeasuredTickRate()
    {
        mGame.start();

        final int diff = Math.abs(mGame.getMeasuredTickRate() - TEST_SPECIFIC_TICK_RATE);

        Assert.assertTrue(diff >= 0 && diff < 2);
    }

    @Test
    public void testIsRunning()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        mGame = new AutoStopGame(new TestCanvas(), properties, GAME_DURATION / 4, () ->
        {
            Assert.assertTrue(mGame.isRunning());
        });

        mGame.start();
    }

    @Test
    public void testIsRunningReturnsFalseBeforeStart()
    {
        Assert.assertFalse(mGame.isRunning());
    }

    @Test
    public void testIsRunningReturnsFalseAfterStop()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        mGame = new AutoStopGame(new TestCanvas(), properties, 0L, () ->
        {
            mGame.stop();
            Assert.assertFalse(mGame.isRunning());
        });

        mGame.start();
    }
}
