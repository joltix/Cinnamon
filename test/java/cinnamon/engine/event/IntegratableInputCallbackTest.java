package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.InputEvent.Action;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import cinnamon.engine.event.IntegratableInput.MouseButtonCallback;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.Point;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallbackI;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * <p>These tests focus on how feeding input data to the callbacks affect the {@code IntegratableInput}'s output
 * in addition to expected exceptions.</p>
 */
public class IntegratableInputCallbackTest
{
    private IntegratableInput mInput;

    @Before
    public void setUp()
    {
        mInput = new IntegratableInput();
    }

    @After
    public void tearDown()
    {
        mInput = null;
    }

    @Test
    public void testGetKeyboardKeyCallback()
    {
        final GLFWKeyCallbackI callback = mInput.getKeyboardKeyCallback();
        callback.invoke(0, GLFW.GLFW_KEY_SPACE, 0, GLFW.GLFW_PRESS, 0);

        final InputEvent event = mInput.poll();
        Assert.assertNotNull(event);
        Assert.assertSame(KeyEvent.class, event.getClass());
        Assert.assertTrue(event.getAction() == Action.PRESS);
        Assert.assertSame(Key.KEY_SPACE, ((KeyEvent) event).getKey());
    }

    @Test
    public void testKeyboardKeyCallbackDoesNothingMuted()
    {
        mInput.getKeyboard().mute();

        final GLFWKeyCallbackI callback = mInput.getKeyboardKeyCallback();
        callback.invoke(0, GLFW.GLFW_KEY_SPACE, 0, GLFW.GLFW_PRESS, 0);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testKeyboardKeyCallbackDoesNothingUnknownKey()
    {
        final GLFWKeyCallbackI callback = mInput.getKeyboardKeyCallback();
        callback.invoke(0, GLFW.GLFW_KEY_UNKNOWN, 0, GLFW.GLFW_PRESS, 0);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testKeyboardKeyCallbackDoesNothingRepeatAction()
    {
        final GLFWKeyCallbackI callback = mInput.getKeyboardKeyCallback();
        callback.invoke(0, GLFW.GLFW_KEY_SPACE, 0, GLFW.GLFW_REPEAT, 0);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testGetMouseButtonCallback()
    {
        final MouseButtonCallback callback = mInput.getMouseButtonCallback();
        callback.onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0, 0d, 0d);

        final InputEvent event = mInput.poll();
        Assert.assertNotNull(event);
        Assert.assertSame(MouseEvent.class, event.getClass());
        Assert.assertTrue(event.getAction() == Action.PRESS);
        Assert.assertSame(Button.LEFT, ((MouseEvent) event).getButton());
    }

    @Test
    public void testMouseButtonCallbackDoesNothingMuted()
    {
        mInput.getMouse().mute();

        final MouseButtonCallback callback = mInput.getMouseButtonCallback();
        callback.onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0, 0d, 0d);

        Assert.assertNull(mInput.poll());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMouseButtonCallbackIAEUnrecognizedButton()
    {
        final MouseButtonCallback callback = mInput.getMouseButtonCallback();
        callback.onButtonUpdate(-42, GLFW.GLFW_PRESS, 0, 0d, 0d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMouseButtonCallbackIAEUnrecognizedAction()
    {
        final MouseButtonCallback callback = mInput.getMouseButtonCallback();
        callback.onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_LEFT, -42, 0, 0d, 0d);
    }

    @Test
    public void testMouseButtonCallbackDoesNothingRepeatAction()
    {
        final MouseButtonCallback callback = mInput.getMouseButtonCallback();
        callback.onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_REPEAT, 0, 0d, 0d);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testGamepadConnectionCallbackGamepadConnected()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, XboxPadProfile.NAME);

        Assert.assertNotNull(mInput.getGamepad(Connection.PAD_1));
    }

    @Test
    public void testGamepadConnectionCallbackGamepadDisconnected()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();

        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, XboxPadProfile.NAME);
        Assert.assertNotNull(mInput.getGamepad(Connection.PAD_1));

        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_DISCONNECTED, XboxPadProfile.NAME);
        Assert.assertNull(mInput.getGamepad(Connection.PAD_1));
    }

    @Test
    public void testGamepadConnectionCallbackDoesNothing()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(-42, GLFW.GLFW_CONNECTED, XboxPadProfile.NAME);

        Assert.assertNull(mInput.getGamepad(Connection.PAD_1));
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadConnectionCallbackNPEName()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGamepadConnectionCallbackIAEUnrecognizedEvent()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(Connection.PAD_1.toInt(), -42, XboxPadProfile.NAME);
    }

    @Test
    public void testGamepadUpdateCallbackOnButtonUse()
    {
        fakeXboxGamepadConnection();

        final ByteBuffer buffer = pressAllButtons(XboxPadProfile.Button.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onButtonsUpdate(Connection.PAD_1, buffer);

        // Check all are pressed
        final Gamepad gamepad = mInput.getGamepad(Connection.PAD_1);
        for (final XboxPadProfile.Button button : XboxPadProfile.Button.values()) {
            Assert.assertTrue(gamepad.isPressed(button));
        }
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnButtonUseNPEConnection()
    {
        fakeXboxGamepadConnection();

        final ByteBuffer buffer = pressAllButtons(XboxPadProfile.Button.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onButtonsUpdate(null, buffer);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnButtonUseNPEBuffer()
    {
        fakeXboxGamepadConnection();

        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();
        callback.onButtonsUpdate(Connection.PAD_1, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGamepadUpdateCallbackOnButtonUseIAEBufferCapacityTooSmall()
    {
        fakeXboxGamepadConnection();

        final ByteBuffer buffer = pressAllButtons(XboxPadProfile.Button.values().length - 1);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onButtonsUpdate(Connection.PAD_1, buffer);
    }

    @Test
    public void testGamepadUpdateCallbackOnAxisUse()
    {
        fakeXboxGamepadConnection();

        final FloatBuffer buffer = moveAllAxes(Axis.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(Connection.PAD_1, buffer);

        // Check all moved to 1f
        final Gamepad gamepad = mInput.getGamepad(Connection.PAD_1);
        for (final XboxPadProfile.Stick stick : XboxPadProfile.Stick.values()) {
            final Point pt = gamepad.getAxisPosition(stick);

            Assert.assertEquals(buffer.get(stick.getVertical().toInt()), pt.getY(), 0f);

            // Triggers don't have horizontal axes
            if (stick.getHorizontal() != null) {
                Assert.assertEquals(buffer.get(stick.getHorizontal().toInt()), pt.getX(), 0f);
            }
        }
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnAxisUseNPEConnection()
    {
        fakeXboxGamepadConnection();

        final FloatBuffer buffer = moveAllAxes(Axis.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(null, buffer);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnAxisUseNPEBuffer()
    {
        fakeXboxGamepadConnection();

        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(Connection.PAD_1, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGamepadUpdateCallbackOnAxisUseIAEBufferCapacityTooSmall()
    {
        fakeXboxGamepadConnection();

        final int cap = new XboxPadProfile().getAxisRange() - 1;
        final FloatBuffer buffer = moveAllAxes(cap);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(Connection.PAD_1, buffer);
    }

    private void fakeXboxGamepadConnection()
    {
        final GamepadConnectionCallback connCallback = mInput.getGamepadConnectionCallback();
        connCallback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, XboxPadProfile.NAME);
    }

    /**
     * <p>Creates a {@code ByteBuffer} where all buttons' states are pressed.</p>
     *
     * @param buttonCount number of buttons.
     * @return buffer.
     */
    private ByteBuffer pressAllButtons(int buttonCount)
    {
        final ByteBuffer buffer = BufferUtils.createByteBuffer(buttonCount);

        for (int i = 0; i < buttonCount; i++) {
            buffer.put((byte) 0x1);
        }

        return buffer;
    }

    /**
     * <p>Creates a {@code FloatBuffer} where all axes' states have been moved to the maximal value of {@code 1}.</p>
     *
     * @param axisCount number of axes.
     * @return buffer.
     */
    private FloatBuffer moveAllAxes(int axisCount)
    {
        final FloatBuffer buffer = BufferUtils.createFloatBuffer(axisCount);

        for (int i = 0; i < axisCount; i++) {
            buffer.put(1f);
        }

        return buffer;
    }
}
