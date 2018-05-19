package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * <p>These tests focus on expected exceptions for the various callbacks returned by {@link IntegratableInput}.</p>
 */
public class IntegratableInputCallbackExceptionsTest
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

    @Test (expected = NullPointerException.class)
    public void testGamepadConnectionCallbackNPEName()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, null);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnButtonsUpdateNPEConnection()
    {
        fakeXboxGamepadConnection();

        final ByteBuffer buffer = pressAllButtons(XB1.Button.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onButtonsUpdate(null, buffer);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnButtonsUpdateNPEBuffer()
    {
        fakeXboxGamepadConnection();

        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();
        callback.onButtonsUpdate(Connection.PAD_1, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGamepadUpdateCallbackOnButtonsUpdateIAEBufferLimitTooSmall()
    {
        fakeXboxGamepadConnection();

        final int cap = XB1.GAMEPAD_PROFILE.getButtonCount() - 1;
        final ByteBuffer buffer = pressAllButtons(cap);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onButtonsUpdate(Connection.PAD_1, buffer);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnAxesUpdateNPEConnection()
    {
        fakeXboxGamepadConnection();

        final FloatBuffer buffer = moveAllAxes(Axis.values().length);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(null, buffer);
    }

    @Test (expected = NullPointerException.class)
    public void testGamepadUpdateCallbackOnAxesUpdateNPEBuffer()
    {
        fakeXboxGamepadConnection();

        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(Connection.PAD_1, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGamepadUpdateCallbackOnAxesUpdateIAEBufferLimitTooSmall()
    {
        fakeXboxGamepadConnection();

        final int cap = XB1.GAMEPAD_PROFILE.getAxisCount() - 1;
        final FloatBuffer buffer = moveAllAxes(cap);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        callback.onAxesUpdate(Connection.PAD_1, buffer);
    }

    private void fakeXboxGamepadConnection()
    {
        final GamepadConnectionCallback connCallback = mInput.getGamepadConnectionCallback();
        connCallback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, XB1.GAMEPAD_NAME);
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
