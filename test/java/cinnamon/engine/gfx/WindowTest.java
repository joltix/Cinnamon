package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.DummyCanvas;
import cinnamon.engine.utils.Assets;
import cinnamon.engine.utils.TestUtils;
import org.junit.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.stb.STBImage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

public class WindowTest
{
    // Window.pollEvents() loop duration, in milliseconds
    private static final long DURATION = 4_000L;

    private static final String ICON_PATH = "/cinnamon/engine/gfx/test_icon.jpg";

    private static final Monitor UNCONNECTED_MONITOR = new Monitor();

    private static final int WINDOW_WIDTH = Window.MINIMUM_WIDTH * 2;

    private static final int WINDOW_HEIGHT = Window.MINIMUM_HEIGHT * 2;

    // Monitor to go fullscreen on
    private Monitor mMonitor;

    private Window mWindow;

    @Before
    public void setUp()
    {
        mWindow = new Window(new DummyCanvas());
        mMonitor = Monitor.getPrimaryMonitor();
    }

    @After
    public void tearDown()
    {
        Window.terminate();

        mWindow = null;
        mMonitor = null;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorCanvasNPE()
    {
        mWindow.destroy();

        mWindow = new Window(null);
    }

    @Test
    public void testOpen()
    {
        mWindow.open();

        Assert.assertTrue(mWindow.isOpen());
    }

    @Test (expected = IllegalStateException.class)
    public void testOpenISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.open();
    }

    @Test
    public void testClose()
    {
        mWindow.open();
        mWindow.close();

        Assert.assertFalse(mWindow.isOpen());
    }

    @Test (expected = IllegalStateException.class)
    public void testCloseISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.close();
    }

    @Test
    public void testGetFrameExtentsReturnsFourValues()
    {
        Assert.assertEquals(4, mWindow.getFrameExtents().length);
    }

    @Test (expected = IllegalStateException.class)
    public void testGetFrameExtentsISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.getFrameExtents();
    }

    @Test
    public void testSetPosition()
    {
        final Random random = new Random(System.nanoTime());

        // Random location on primary display
        final int x = random.nextInt(mMonitor.getWidth() + 1 - WINDOW_WIDTH);
        final int y = random.nextInt(mMonitor.getHeight() + 1 - WINDOW_HEIGHT);

        mWindow.setPosition(x, y);

        Assert.assertEquals(x, mWindow.getX());
        Assert.assertEquals(y, mWindow.getY());
    }

    @Test (expected = IllegalStateException.class)
    public void testSetPositionISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setPosition(4, 2);
    }

    /**
     * Getting the position of a centered window appears to return a slightly different position than was set. As this
     * difference is negligible and no clear cause has yet been found, this test exists more for visual confirmation
     * apart from an exception-free execution - there is no position assertion.
     *
     * Note that the position discrepancy has been observed to occur at the earliest point where the position set does
     * not match an immediate subsequent position retrieval; this was examined with direct GLFW method calls. As
     * noted by {@link GLFW#glfwSetWindowPos(long, int, int)}, the window manager may be overriding the position.
     */
    @Test
    public void testSetPositionCenter()
    {
        mWindow.open();

        mWindow.setPositionCenter(mMonitor);
    }

    @Test (expected = NullPointerException.class)
    public void testSetPositionCenterNPEMonitor()
    {
        mWindow.setPositionCenter(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPositionCenterNPEMonitorNotConnected()
    {
        mWindow.setPositionCenter(UNCONNECTED_MONITOR);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetPositionCenterISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setPositionCenter(mMonitor);
    }

    @Test
    public void testSetSizeSetsWidthAndHeight()
    {
        mWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        Assert.assertEquals(WINDOW_WIDTH, mWindow.getWidth());
        Assert.assertEquals(WINDOW_HEIGHT, mWindow.getHeight());
    }

    @Test
    public void testSetSizeClampsWidthToMinimumWhenTooSmall()
    {
        mWindow.setSize(-WINDOW_WIDTH, WINDOW_HEIGHT);

        Assert.assertEquals(Window.MINIMUM_WIDTH, mWindow.getWidth());
    }

    @Test
    public void testSetSizeClampsHeightToMinimumWhenTooSmall()
    {
        mWindow.setSize(WINDOW_WIDTH, -WINDOW_HEIGHT);

        Assert.assertEquals(Window.MINIMUM_HEIGHT, mWindow.getHeight());
    }

    @Test (expected = IllegalStateException.class)
    public void testSetSizeISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    @Test
    public void testGetWidthReturnsMinimumWidthByDefault()
    {
        Assert.assertEquals(Window.MINIMUM_WIDTH, mWindow.getWidth());
    }

    @Test
    public void testGetWidthReturnsWidth()
    {
        mWindow.setWidth(WINDOW_WIDTH);

        Assert.assertEquals(WINDOW_WIDTH, mWindow.getWidth());
    }

    @Test (expected = IllegalStateException.class)
    public void testGetWidthISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setWidth(WINDOW_WIDTH);
    }

    @Test
    public void testSetWidthClampsWidthToMinimumWhenTooSmall()
    {
        mWindow.setWidth(-WINDOW_WIDTH);

        Assert.assertEquals(Window.MINIMUM_WIDTH, mWindow.getWidth());
    }

    @Test (expected = IllegalStateException.class)
    public void testSetWidthISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setWidth(WINDOW_WIDTH);
    }

    @Test
    public void testGetHeightReturnsMinimumHeightByDefault()
    {
        Assert.assertEquals(Window.MINIMUM_HEIGHT, mWindow.getHeight());
    }

    @Test
    public void testGetHeightReturnsHeight()
    {
        mWindow.setHeight(WINDOW_HEIGHT);

        Assert.assertEquals(WINDOW_HEIGHT, mWindow.getHeight());
    }

    @Test (expected = IllegalStateException.class)
    public void testGetHeightISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setHeight(WINDOW_HEIGHT);
    }

    @Test
    public void testSetHeightClampsHeightToMinimumWhenTooSmall()
    {
        mWindow.setHeight(-WINDOW_HEIGHT);

        Assert.assertEquals(Window.MINIMUM_HEIGHT, mWindow.getHeight());
    }

    @Test (expected = IllegalStateException.class)
    public void testSetHeightISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setHeight(WINDOW_HEIGHT);
    }

    @Test
    public void testIsResizableReturnsFalseByDefault()
    {
        Assert.assertFalse(mWindow.isResizable());
    }

    @Test
    public void testIsResizableReturnsTrue()
    {
        mWindow.setResizable(true);

        Assert.assertTrue(mWindow.isResizable());
    }

    @Test
    public void testIsResizableReturnsFalse()
    {
        mWindow.setResizable(false);

        Assert.assertFalse(mWindow.isResizable());
    }

    @Test (expected = IllegalStateException.class)
    public void testIsResizableISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.isResizable();
    }

    @Test
    public void testSetResizableExecutesCleanly()
    {
        mWindow.setResizable(true);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetResizableISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setResizable(true);
    }

    @Test
    public void testIsDecoratedReturnsTrueByDefault()
    {
        Assert.assertTrue(mWindow.isDecorated());
    }

    @Test
    public void testIsDecoratedReturnsTrue()
    {
        mWindow.setDecorated(true);

        Assert.assertTrue(mWindow.isDecorated());
    }

    @Test
    public void testIsDecoratedReturnsFalse()
    {
        mWindow.setDecorated(false);

        Assert.assertFalse(mWindow.isDecorated());
    }

    @Test (expected = IllegalStateException.class)
    public void testIsDecoratedISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.isDecorated();
    }

    @Test
    public void testSetDecoratedExecutesCleanly()
    {
        mWindow.setDecorated(false);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetDecoratedISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setDecorated(false);
    }

    @Test
    public void testIsVsyncEnabledReturnsTrueByDefault()
    {
        Assert.assertTrue(mWindow.isVsyncEnabled());
    }

    @Test
    public void testIsVsyncEnabledReturnsTrue()
    {
        mWindow.setVsync(true);

        Assert.assertTrue(mWindow.isVsyncEnabled());
    }

    @Test
    public void testIsVsyncEnabledReturnsFalse()
    {
        mWindow.setVsync(false);

        Assert.assertFalse(mWindow.isVsyncEnabled());
    }

    @Test (expected = IllegalStateException.class)
    public void testIsVsyncEnabledISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.isVsyncEnabled();
    }

    /**
     * This test assumes the test machine can loop on the main thread (with vsync disabled) at a rate higher than the
     * test monitor's current refresh rate.
     */
    @Test
    public void testSetVsyncTrueLimitsFrameRateForDisplay()
    {
        mWindow.open();

        final int rate;

        // Measure frame rate without vsync
        mWindow.setVsync(false);
        TestUtils.loop(DURATION, Window::pollEvents);
        rate = (mWindow.getCanvas().getFrameRate());

        // Measure frame rate with vsync enabled
        mWindow.setVsync(true);
        TestUtils.loop(DURATION, Window::pollEvents);

        final GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

        assert (mode != null);

        final int expected = mode.refreshRate();
        final int vsyncRate = mWindow.getCanvas().getFrameRate();
        final int diff = Math.abs(expected - vsyncRate);

        // Check vsync rate against display's rate
        Assert.assertTrue(diff == 0 || diff == 1);

        // Vsync rate should be lower than uncontrolled rate
        final String msg = String.format("Vsync(%d) should have been < actual rate(%d)\n", vsyncRate, rate);
        Assert.assertTrue(msg, vsyncRate < rate);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetVsyncISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setVsync(true);
    }

    @Test
    public void testSetIcon()
    {
        mWindow.open();

        // Load icon
        byte[] bytes;

        try {
            bytes = Assets.loadResource(ICON_PATH, Assets.BYTE_ARRAY);
        } catch (IOException e) {
            e.printStackTrace();
            bytes = new byte[4096];
        }

        final ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        // Make sure pixels are formatted correctly
        final IntBuffer width = BufferUtils.createIntBuffer(1);
        final IntBuffer height = BufferUtils.createIntBuffer(1);
        final IntBuffer channels = BufferUtils.createIntBuffer(1);
        final ByteBuffer pulled = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);

        assert (pulled != null);
        assert (STBImage.stbi_failure_reason() == null);

        TestUtils.loopWithOneShot(DURATION, Window::pollEvents, DURATION / 2, () ->
        {
            mWindow.setIcon(pulled);
        });
    }

    @Test (expected = NullPointerException.class)
    public void testSetIconNPESingleIcon()
    {
        mWindow.setIcon(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIconIAESizeNotMultipleOf16()
    {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(1025);
        buffer.put(new byte[buffer.capacity()]);
        buffer.flip();

        mWindow.setIcon(buffer);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIconIAEIconTooSmall()
    {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(0);
        buffer.flip();

        mWindow.setIcon(buffer);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetIconISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        final ByteBuffer buffer = BufferUtils.createByteBuffer(1025);
        buffer.put(new byte[buffer.capacity()]);
        buffer.flip();

        mWindow.setIcon(buffer);
    }

    @Test
    public void testMinimizeWithOpenedFloatingWindow()
    {
        mWindow.open();
        mWindow.minimize();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test
    public void testMinimizeWithOpenedMaximizedWindow()
    {
        mWindow.open();
        mWindow.maximize();
        mWindow.minimize();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test
    public void testMinimizeWithOpenedFullscreenWindow()
    {
        mWindow.open();
        mWindow.setFullscreen(mMonitor);
        mWindow.minimize();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test
    public void testMinimizeWithClosedFloatingWindowDoesNothingUntilOpened()
    {
        mWindow.minimize();

        Assert.assertFalse(mWindow.isMinimized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test
    public void testMinimizeWithClosedMaximizedWindowDoesNothingUntilOpened()
    {
        mWindow.maximize();
        mWindow.minimize();

        Assert.assertFalse(mWindow.isMinimized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test
    public void testMinimizeWithClosedFullscreenWindowDoesNothingUntilOpened()
    {
        mWindow.setFullscreen(mMonitor);
        mWindow.minimize();

        Assert.assertFalse(mWindow.isMinimized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMinimized());
    }

    @Test (expected = IllegalStateException.class)
    public void testMinimizeISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.minimize();
    }

    @Test
    public void testMaximizeWithOpenedFloatingWindow()
    {
        mWindow.open();
        mWindow.maximize();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test
    public void testMaximizeWithOpenedMinimizedWindow()
    {
        mWindow.open();
        mWindow.minimize();
        mWindow.maximize();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test
    public void testMaximizeWithOpenedFullscreenWindow()
    {
        mWindow.open();
        mWindow.setFullscreen(mMonitor);
        mWindow.maximize();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test
    public void testMaximizeWithClosedFloatingWindowDoesNothingUntilOpened()
    {
        mWindow.maximize();

        Assert.assertFalse(mWindow.isMaximized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test
    public void testMaximizeWithClosedMinimizedWindowDoesNothingUntilOpened()
    {
        mWindow.minimize();
        mWindow.maximize();

        Assert.assertFalse(mWindow.isMaximized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test
    public void testMaximizeWithClosedFullscreenWindowDoesNothingUntilOpened()
    {
        mWindow.setFullscreen(mMonitor);
        mWindow.maximize();

        Assert.assertFalse(mWindow.isMaximized());

        mWindow.open();

        Assert.assertTrue(mWindow.isMaximized());
    }

    @Test (expected = IllegalStateException.class)
    public void testMaximizeISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.maximize();
    }

    @Test
    public void testSetFullscreenWithOpenedFloatingWindow()
    {
        mWindow.open();
        mWindow.setFullscreen(mMonitor);

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test
    public void testSetFullscreenWithOpenedMinimizedWindow()
    {
        mWindow.open();
        mWindow.minimize();
        mWindow.setFullscreen(mMonitor);

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test
    public void testSetFullscreenWithOpenedMaximizedWindow()
    {
        mWindow.open();
        mWindow.maximize();
        mWindow.setFullscreen(mMonitor);

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test
    public void testSetFullscreenWithClosedFloatingWindowDoesNothingUntilOpened()
    {
        mWindow.setFullscreen(mMonitor);

        Assert.assertFalse(mWindow.isFullscreen());

        mWindow.open();

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test
    public void testSetFullscreenWithClosedMinimizedWindowDoesNothingUntilOpened()
    {
        mWindow.minimize();
        mWindow.setFullscreen(mMonitor);

        Assert.assertFalse(mWindow.isFullscreen());

        mWindow.open();

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test
    public void testSetFullscreenWithClosedMaximizedWindowDoesNothingUntilOpened()
    {
        mWindow.maximize();
        mWindow.setFullscreen(mMonitor);

        Assert.assertFalse(mWindow.isFullscreen());

        mWindow.open();

        Assert.assertTrue(mWindow.isFullscreen());
    }

    @Test (expected = NullPointerException.class)
    public void testSetFullscreenNPEMonitor()
    {
        mWindow.setFullscreen(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetFullscreenIAEMonitorNotConnected()
    {
        mWindow.setFullscreen(UNCONNECTED_MONITOR);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetFullscreenISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setFullscreen(mMonitor);
    }

    @Test
    public void testRestoreWithOpenedMinimizedWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.minimize();
            mWindow.open();

            Assert.assertTrue(mWindow.isMinimized());

            mWindow.restore();

            Assert.assertFalse(mWindow.isMinimized());
        });
    }

    @Test
    public void testRestoreWithOpenedMaximizedWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.maximize();
            mWindow.open();

            Assert.assertTrue(mWindow.isMaximized());

            mWindow.restore();

            Assert.assertFalse(mWindow.isMaximized());
        });
    }

    @Test
    public void testRestoreWithOpenedFullscreenWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.setFullscreen(mMonitor);
            mWindow.open();

            Assert.assertTrue(mWindow.isFullscreen());

            mWindow.restore();

            Assert.assertFalse(mWindow.isFullscreen());
        });
    }

    @Test
    public void testRestoreWithClosedMinimizedWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.minimize();
            mWindow.restore();
            mWindow.open();

            Assert.assertFalse(mWindow.isMinimized());
        });
    }

    @Test
    public void testRestoreWithClosedMaximizedWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.maximize();
            mWindow.restore();
            mWindow.open();

            Assert.assertFalse(mWindow.isMaximized());
        });
    }

    @Test
    public void testRestoreWithClosedFullscreenWindow()
    {
        assertSameSizeAndPositionBeforeAndAfter(() ->
        {
            mWindow.setFullscreen(mMonitor);
            mWindow.restore();
            mWindow.open();

            Assert.assertFalse(mWindow.isFullscreen());
        });
    }

    @Test (expected = IllegalStateException.class)
    public void testRestoreISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.restore();
    }

    @Test (expected = IllegalStateException.class)
    public void testIsFocusedISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.isFocused();
    }

    @Test
    public void testFocus()
    {
        final Window secondary = new Window(new DummyCanvas());
        mWindow.open();
        secondary.open();

        mWindow.focus();

        Assert.assertTrue(mWindow.isFocused());
        Assert.assertFalse(secondary.isFocused());

        secondary.destroy();
    }

    @Test (expected = IllegalStateException.class)
    public void testFocusISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.focus();
    }

    @Test
    public void getTitleReturnsExpectedTitle()
    {
        mWindow.setTitle(WindowTest.class.getSimpleName());

        Assert.assertEquals(WindowTest.class.getSimpleName(), mWindow.getTitle());
    }

    @Test (expected = IllegalStateException.class)
    public void getTitleISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.getTitle();
    }

    @Test
    public void setTitleExecutesCleanly()
    {
        mWindow.setTitle(WindowTest.class.getSimpleName());
    }

    @Test (expected = NullPointerException.class)
    public void setTitleNPETitle()
    {
        mWindow.setTitle(null);
    }

    @Test (expected = IllegalStateException.class)
    public void setTitleISEWindowAlreadyDestroyed()
    {
        mWindow.destroy();

        mWindow.setTitle("");
    }

    @Test
    public void testSetShouldClose()
    {
        mWindow.setShouldClose(true);
    }

    private void assertSameSizeAndPositionBeforeAndAfter(Runnable runnable)
    {
        final int w = mWindow.getWidth();
        final int h = mWindow.getHeight();
        final int x = mWindow.getX();
        final int y = mWindow.getY();

        runnable.run();

        Assert.assertEquals(w, mWindow.getWidth());
        Assert.assertEquals(h, mWindow.getHeight());
        Assert.assertEquals(x, mWindow.getX());
        Assert.assertEquals(y, mWindow.getY());
    }
}
