package cinnamon.engine.core;

import cinnamon.engine.core.GameTestSuite.AutoStopGame;
import cinnamon.engine.core.GameTestSuite.TestCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.util.Map;

/**
 * Tests {@code Game}'s properties when given minimal initial properties. Most tests focus on optional properties and
 * the value returned when unspecified at construction.
 */
public class GameOptionalPropertiesTest
{
    private static final int DISPLAY_WIDTH;

    private static final int DISPLAY_HEIGHT;

    // Read display's size
    static
    {
        final GraphicsDevice GFX = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        DISPLAY_WIDTH = GFX.getDisplayMode().getWidth();
        DISPLAY_HEIGHT = GFX.getDisplayMode().getHeight();
    }

    private Game mGame;

    @Before
    public void setUp()
    {
        final Map<String, Object> properties = GameTestSuite.createMinimalProperties();
        mGame = new AutoStopGame(new TestCanvas(), properties);
    }

    @After
    public void tearDown()
    {
        mGame.stop();
        mGame = null;
    }

    @Test
    public void testGetWindowTitleReturnsGameTitleWhenUnspecified()
    {
        Assert.assertEquals(GameTestSuite.TITLE, mGame.getStringProperty(Game.TITLE));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowTitleIAEUnmodifiableProperty()
    {
        mGame.setStringProperty(Game.WINDOW_TITLE, "Some Title");
    }

    @Test
    public void testGetWindowWidthReturnsDisplayWidthWhenUnspecified()
    {
        Assert.assertEquals(DISPLAY_WIDTH, mGame.getIntegerProperty(Game.WINDOW_WIDTH));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowWidthIAEUnmodifiableProperty()
    {
        mGame.setIntegerProperty(Game.WINDOW_WIDTH, 100);
    }

    @Test
    public void testGetWindowHeightReturnsDisplayHeightWhenUnspecified()
    {
        Assert.assertEquals(DISPLAY_HEIGHT, mGame.getIntegerProperty(Game.WINDOW_HEIGHT));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowHeightIAEUnmodifiableProperty()
    {
        mGame.setIntegerProperty(Game.WINDOW_HEIGHT, 100);
    }

    @Test
    public void testGetWindowFullscreenReturnsTrueWhenUnspecified()
    {
        Assert.assertTrue(mGame.getBooleanProperty(Game.WINDOW_FULLSCREEN));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowFullscreenIAEUnmodifiableProperty()
    {
        mGame.setBooleanProperty(Game.WINDOW_FULLSCREEN, false);
    }

    @Test
    public void testGetWindowBorderlessReturnsFalseWhenUnspecified()
    {
        Assert.assertFalse(mGame.getBooleanProperty(Game.WINDOW_BORDERLESS));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowBorderlessIAEUnmodifiableProperty()
    {
        mGame.setBooleanProperty(Game.WINDOW_BORDERLESS, true);
    }

    @Test
    public void testGetWindowHiddenReturnsFalseWhenUnspecified()
    {
        Assert.assertFalse(mGame.getBooleanProperty(Game.WINDOW_HIDDEN));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetWindowHiddenIAEUnmodifiableProperty()
    {
        mGame.setBooleanProperty(Game.WINDOW_HIDDEN, true);
    }
}
