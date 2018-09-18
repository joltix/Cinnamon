package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.ColorFadeCanvas;
import cinnamon.engine.utils.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * These tests are meant to confirm usability of the expected window management schemes.
 */
public class WindowLoopStyleTest
{
    // How long to hold window open
    private static final long DURATION = 4_000L * TestUtils.NANO_PER_MILLI;

    // Normalized color change
    private static final double COLOR_FADE_SPEED = 0.5d;

    private Window mWindow;

    @Before
    public void setUp()
    {
        mWindow = new Window(new ColorFadeCanvas(COLOR_FADE_SPEED));
    }

    @After
    public void tearDown()
    {
        Window.terminate();
        mWindow = null;
    }

    @Test
    public void testWindowWithCloseFlagControlledLoop()
    {
        mWindow.open();

        final long start = System.nanoTime();
        long now = start;

        while (!mWindow.shouldClose()) {
            Window.pollEvents();

            if ((now = System.nanoTime()) - start > DURATION) {
                mWindow.setShouldClose(true);
            }
        }

        // In case shouldClose returns true due to something else
        Assert.assertTrue(now - start > DURATION);
    }

    @Test
    public void testWindowWithIndependentlyControlledLoop()
    {
        mWindow.open();

        final long start = System.nanoTime();

        while (System.nanoTime() - start <= DURATION) {
            Window.pollEvents();
        }
    }
}
