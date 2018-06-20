package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.MockCanvas;
import cinnamon.engine.utils.Assets;
import cinnamon.engine.utils.Size;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.stb.STBImage;

import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Random;

/**
 * <p>These tests do not assert the underlying GLFW library and instead focus on the {@code Window} class' reported
 * state, with some requiring visual confirmation. Some methods, such as
 * {@code addOnFramebufferSizeChangeListener(Window.OnSizeChangeListener)} are not tested.</p>
 *
 * <p>Unless specified otherwise by the individual test, the {@code Window} test subject is expected to appear as a
 * black screen.</p>
 */
public class WindowTest
{
    private static final float DELTA = 0.01f;

    private static final long TEST_DURATION = 4000L;
    // Delay always at least 1 second regardless of duration
    @SuppressWarnings("ConstantConditions")
    private static final long TEST_DELAY = (TEST_DURATION * 0.5f < 1000L) ? 1000L : (long) (TEST_DURATION * 0.5f);

    // For testing icon change
    private static final String ICON_PATH = "cinnamon/engine/gfx/test_icon.jpg";

    private static final int EXPECTED_SCREEN_WIDTH = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    private static final int EXPECTED_SCREEN_HEIGHT = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 720;
    private static final String TITLE_TEST = "Window Title Changed";

    private static final String mWinTitle = WindowTest.class.getSimpleName();
    private Window mWindow;

    @Before
    public void setUp()
    {
        GLFW.glfwPollEvents();
        mWindow = new Window(new MockCanvas(), mWinTitle);
    }

    @After
    public void tearDown()
    {
        mWindow.destroy();
        mWindow = null;
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorCanvasNPE()
    {
        mWindow = new Window(null, mWinTitle);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorTitleNPE()
    {
        mWindow = new Window(new MockCanvas(), null);
    }

    @Test
    public void testOpen()
    {
        Assert.assertTrue(!mWindow.isOpen());
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() -> {
            Assert.assertFalse(!mWindow.isOpen());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testClose()
    {
        mWindow.open();
        Assert.assertTrue(mWindow.isOpen());

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            mWindow.close();
            Assert.assertFalse(mWindow.isOpen());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetPosition()
    {
        final Random random = new Random(System.nanoTime());

        // Random location on primary display
        final int x = random.nextInt(EXPECTED_SCREEN_WIDTH + 1 - WINDOW_WIDTH);
        final int y = random.nextInt(EXPECTED_SCREEN_HEIGHT + 1 - WINDOW_HEIGHT);

        mWindow.setFullscreen(false);
        mWindow.open();
        WindowTestSuite.keepOpenAndExecute(() ->
        {

            mWindow.setPosition(x, y);
            Assert.assertEquals(x, mWindow.getX(), 0f);
            Assert.assertEquals(y, mWindow.getY(), 0f);

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetPositionCenter()
    {
        final float expectedX = (EXPECTED_SCREEN_WIDTH / 2f) - (mWindow.getWidth() / 2f);
        final float expectedY = (EXPECTED_SCREEN_HEIGHT / 2f) - (mWindow.getHeight() / 2f);

        mWindow.open();
        mWindow.setFullscreen(false);

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            Assert.assertNotEquals(expectedX, mWindow.getX(), DELTA);
            Assert.assertNotEquals(expectedY, mWindow.getY(), DELTA);

            mWindow.setPositionCenter();

            Assert.assertEquals(expectedX, mWindow.getX(), DELTA);
            Assert.assertEquals(expectedY, mWindow.getY(), DELTA);

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetSize()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            Assert.assertEquals(Window.MINIMUM_WIDTH, mWindow.getWidth(), 0f);
            Assert.assertEquals(Window.MINIMUM_HEIGHT, mWindow.getHeight(), 0f);

            mWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            Window.pollEvents();

            Assert.assertEquals(WINDOW_WIDTH, mWindow.getWidth(), 0f);
            Assert.assertEquals(WINDOW_HEIGHT, mWindow.getHeight(), 0f);

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetSizeClampsWidthToMinimumWhenTooSmall()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            mWindow.setSize(-WINDOW_WIDTH, WINDOW_HEIGHT);
        }, TEST_DELAY, TEST_DURATION);

        Assert.assertEquals(Window.MINIMUM_WIDTH, mWindow.getWidth());
    }

    @Test
    public void testSetSizeClampsHeightToMinimumWhenTooSmall()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            mWindow.setSize(WINDOW_WIDTH, -WINDOW_HEIGHT);
        }, TEST_DELAY, TEST_DURATION);

        Assert.assertEquals(Window.MINIMUM_HEIGHT, mWindow.getHeight());
    }

    @Test
    public void testGetPrimaryDisplayResolution()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            final Size primarySize = mWindow.getPrimaryDisplayResolution();
            Assert.assertEquals(EXPECTED_SCREEN_WIDTH, primarySize.getWidth(), 0f);
            Assert.assertEquals(EXPECTED_SCREEN_HEIGHT, primarySize.getHeight(), 0f);

        }, TEST_DELAY, TEST_DURATION);
    }

    /**
     * <p>This test can only be visually confirmed and has no assertions. The expected runtime is as follows.</p>
     *
     * <p>A window will open. Shortly after, the window will fill the screen completely and no title bar or widgets
     * are visible.</p>
     */
    @Test
    public void testSetFullscreen()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            mWindow.setFullscreen(true);

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetDecorated()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            Assert.assertTrue(mWindow.isDecorated());
            mWindow.setDecorated(false);
            Assert.assertFalse(mWindow.isDecorated());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetResizable()
    {
        mWindow.open();

        Assert.assertFalse(mWindow.isResizable());

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            mWindow.setResizable(true);
            Assert.assertTrue(mWindow.isResizable());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetTitle()
    {
        mWindow.open();
        mWindow.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);

        Assert.assertEquals(WindowTest.class.getSimpleName(), mWindow.getTitle());

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            mWindow.setTitle(TITLE_TEST);
            Assert.assertEquals(TITLE_TEST, mWindow.getTitle());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testSetIcon()
    {
        mWindow.open();

        WindowTestSuite.keepOpenAndExecute(() ->
        {
            // Load icon
            final byte[] bytes = Assets.loadBytesFromResource(ICON_PATH, 4096);
            final ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            // Get colors
            final IntBuffer width = BufferUtils.createIntBuffer(1);
            final IntBuffer height = BufferUtils.createIntBuffer(1);
            final IntBuffer channels = BufferUtils.createIntBuffer(1);
            final ByteBuffer pulled = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);
            if (pulled == null) {
                throw new IllegalArgumentException("Bytes couldn't be extracted");
            }

            final String failure = STBImage.stbi_failure_reason();
            Assert.assertNull("Failed to extract colors from icon(s): " + failure, failure);

            mWindow.setIcon(pulled);

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test (expected = NullPointerException.class)
    public void testSetIconNPE()
    {
        final ByteBuffer[] icons = null;
        mWindow.setIcon(icons);
    }

    @Test (expected = NullPointerException.class)
    public void testSetIconNPESpecificBuffer()
    {
        mWindow.setIcon(BufferUtils.createByteBuffer(1024), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIconIllegalArgumentExceptionSizeNotMultipleOf16()
    {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(20);
        buffer.flip();

        mWindow.setIcon(buffer);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetIconIllegalArgumentExceptionIconTooSmall()
    {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(0);
        buffer.flip();

        mWindow.setIcon(buffer);
    }

    @Test
    public void testMinimize()
    {
        mWindow.open();
        Assert.assertFalse(mWindow.isMinimized());

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            mWindow.minimize();
            Assert.assertTrue(mWindow.isMinimized());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testMaximize()
    {
        mWindow.open();
        Assert.assertFalse(mWindow.isMaximized());

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            mWindow.maximize();
            Assert.assertTrue(mWindow.isMaximized());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testRestore()
    {
        mWindow.open();
        mWindow.minimize();
        Assert.assertTrue(mWindow.isMinimized());

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            mWindow.restore();
            Assert.assertFalse(mWindow.isMinimized());

        }, TEST_DELAY, TEST_DURATION);
    }

    @Test
    public void testFocus()
    {
        mWindow.open();

        // Create another window to take focus away from the test window
        final Window secondary = new Window(new MockCanvas(), "focus stealer");
        secondary.open();
        assert (secondary.isFocused());

        WindowTestSuite.keepOpenAndExecute(() ->
        {

            Assert.assertFalse(mWindow.isFocused());
            mWindow.focus();

            Assert.assertTrue(mWindow.isFocused());
            Assert.assertFalse(secondary.isFocused());

        }, TEST_DELAY, TEST_DURATION);

        secondary.destroy();
    }
}
