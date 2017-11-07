package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.MockCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;

/**
 * <p>These tests check {@code Window's} static methods that deal with all {@code Window} instances. Each test
 * works with more than 2 {@code Windows}. After all {@code Windows} are opened, there is a delay before the test
 * continues; this allows inspection of a {@code Window} if need be.</p>
 *
 * <p><b>Note:</b> An exception access violation occasionally occurs when running these tests with 12 or more windows.
 * Until a fix is found, it is recommended to keep the window count smaller as the exception has not yet been
 * witnessed at these numbers.</p>
 */
public class WindowMultiInstanceTest
{
    private static final int SCREEN_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int SCREEN_HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private static final int WINDOWS_PER_ROW = 3;
    private static final int WINDOWS_PER_COLUMN = 2;

    // Window positioning
    private static final int GRID_COLUMN_WIDTH = SCREEN_WIDTH / WINDOWS_PER_ROW;
    private static final int GRID_ROW_HEIGHT = SCREEN_HEIGHT / (WINDOWS_PER_COLUMN + 1);
    private static final int GRID_VERTICAL_OFFSET = GRID_ROW_HEIGHT / 2;

    // In ms
    private static final long TEST_DELAY = 3000L;
    private static final long TEST_DURATION = 6000L;

    private static final int WINDOW_COUNT = 6;

    // Track created windows
    private Window[] mWindows;

    @Before
    public void setUp()
    {
        mWindows = new Window[WINDOW_COUNT];

        // Create windows spaced out across the screen
        for (int i = 0, x = 0, y = 0; i < mWindows.length; i++) {

            final Window window = new Window(new MockCanvas(), "#" + (i + 1) + " of " + mWindows.length);
            mWindows[i] = window;

            window.setSize(GRID_COLUMN_WIDTH, GRID_ROW_HEIGHT);
            window.setPosition(GRID_COLUMN_WIDTH * x, (GRID_ROW_HEIGHT * y) + GRID_VERTICAL_OFFSET);

            // Space windows left -> right, top -> bottom
            if (x + 1 < WINDOWS_PER_ROW) {
                x++;
            } else {
                x = 0;
                y++;
            }
        }
    }

    @After
    public void tearDown()
    {
        for (final Window window : mWindows) {
            window.destroy();
        }
    }

    @Test
    public void testOpenAll()
    {
        Window.openAll();

        WindowTestSuite.keepOpenAndExecute(null, TEST_DELAY, TEST_DURATION);

        for (final Window window : mWindows) {
            Assert.assertTrue(window.isOpen());
        }
    }

    @Test
    public void testCloseAll()
    {
        for (final Window window : mWindows) {
            window.open();
        }

        WindowTestSuite.keepOpenAndExecute(null, TEST_DELAY, TEST_DURATION);
        Window.closeAll();

        for (final Window window : mWindows) {
            Assert.assertFalse(window.isOpen());
        }
    }

    @Test
    public void testDestroyAll()
    {
        for (final Window window : mWindows) {
            window.open();
        }

        WindowTestSuite.keepOpenAndExecute(null, TEST_DELAY, TEST_DURATION);
        Window.destroyAll();

        for (final Window window : mWindows) {
            Assert.assertTrue(window.isDestroyed());
        }

        Assert.assertEquals(Window.getWindowCount(), 0);
    }

    @Test
    public void testWindowCount()
    {
        Assert.assertEquals(mWindows.length, Window.getWindowCount());
    }
}
