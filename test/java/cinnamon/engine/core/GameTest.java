package cinnamon.engine.core;

import cinnamon.engine.core.Game.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static cinnamon.engine.core.GameTestSuite.*;

public class GameTest
{
    // How long game should run, in milliseconds
    private static final long GAME_DURATION = 2_000L;

    private AutoStopGame mGame;

    @Before
    public void setUp()
    {
        final Configuration.Builder builder = GameTestSuite.createMinimalConfigurationBuilder();

        mGame = new AutoStopGame(builder.build(), GAME_DURATION);
    }

    @After
    public void tearDown()
    {
        mGame.stop();
        mGame = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEConfiguration()
    {
        new AutoStopGame(null);
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
    public void testGetVersionReturnsExpectedVersion()
    {
        Assert.assertEquals(GameTestSuite.VERSION, mGame.getStringProperty(Game.VERSION));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetVersionIAEUnmodifiableProperty()
    {
        mGame.setStringProperty(Game.VERSION, "343");
    }

    @Test
    public void testGetBuildReturnsExpectedBuild()
    {
        Assert.assertEquals(GameTestSuite.BUILD, mGame.getIntegerProperty(Game.BUILD));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetBuildIAEUnmodifiableProperty()
    {
        mGame.setIntegerProperty(Game.BUILD, 343);
    }

    @Test
    public void testGetTickRateReturnsExpectedTickRate()
    {
        Assert.assertEquals(GameTestSuite.TICK_RATE, mGame.getIntegerProperty(Game.TICK_RATE));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetTickRateIAEUnmodifiableProperty()
    {
        mGame.setIntegerProperty(Game.TICK_RATE, 343);
    }

    @Test
    public void testGetTickRate()
    {
        mGame.start();

        final int diff = Math.abs(mGame.getTickRate() - GameTestSuite.TICK_RATE);

        Assert.assertTrue(diff >= 0 && diff < 2);
    }

    @Test
    public void testGetTickDuration()
    {
        mGame.setTickAction(() ->
        {
            // Fake work to lengthen out each tick's duration
            try {
                Thread.sleep(1_000L / GameTestSuite.TICK_RATE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        mGame.start();

        final double diff = Math.abs(mGame.getTickDuration() - (1_000d / GameTestSuite.TICK_RATE));

        Assert.assertTrue(diff >=  0d && diff < 2d);
    }

    @Test
    public void testIsRunningReturnsTrue()
    {
        final boolean[] trace = {false};

        mGame.setTickAction(() ->
        {
            trace[0] = mGame.isRunning();
        });

        mGame.start();

        Assert.assertTrue(trace[0]);
    }

    @Test
    public void testIsRunningReturnsFalseBeforeStart()
    {
        Assert.assertFalse(mGame.isRunning());
    }

    @Test
    public void testIsRunningReturnsFalseAfterStop()
    {
        final boolean[] trace = {true};

        mGame.setTickAction(() ->
        {
            mGame.stop();
            trace[0] = mGame.isRunning();
        });

        mGame.start();

        Assert.assertFalse(trace[0]);
    }
}
