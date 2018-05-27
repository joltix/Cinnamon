package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.Input.OnConnectionChangeListener;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.PadProfileTest.CustomPadProfile;
import cinnamon.engine.event.PadProfileTest.CustomPadProfile.Button;
import cinnamon.engine.event.PadProfileTest.CustomPadProfile.Axis;
import cinnamon.engine.utils.Point;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

/**
 * <p>These tests focus on the {@code IntegratableInput}'s state and expected exceptions. The exceptions for
 * callbacks such as {@code getMouseButtonCallback()} are tested separately in
 * {@link IntegratableInputCallbackExceptionsTest}.</p>
 */
public class IntegratableInputTest
{
    // PadProfile name for adding test-only profile
    private static final String TEST_PROFILE_NAME = "Test Controller";

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
    public void testSubmitKeyEvent()
    {
        mInput.submit(new KeyEvent(System.nanoTime(), Key.KEY_A, true));
    }

    @Test (expected = NullPointerException.class)
    public void testSubmitKeyEventNPE()
    {
        final KeyEvent event = null;
        mInput.submit(event);
    }

    @Test
    public void testSubmitMouseEvent()
    {
        final long time = System.nanoTime();
        final Mouse.Button button = Mouse.Button.RIGHT;

        mInput.submit(new MouseEvent(time, new Point(), button, true));
    }

    @Test (expected = NullPointerException.class)
    public void testSubmitMouseEventNPE()
    {
        final MouseEvent event = null;
        mInput.submit(event);
    }

    @Test
    public void testSubmitPadEvent()
    {
        final long time = System.nanoTime();
        final Connection connection = Connection.PAD_1;

        mInput.submit(new PadEvent(time, connection, XB1.Button.A, true));
    }

    @Test (expected = NullPointerException.class)
    public void testSubmitPadEventNPE()
    {
        final PadEvent event = null;
        mInput.submit(event);
    }

    @Test
    public void testPollReturnsKeyEvent()
    {
        // Key press
        final int key = GLFW.GLFW_KEY_SPACE;
        final int action = GLFW.GLFW_PRESS;
        mInput.getKeyboardKeyCallback().invoke(0L, key, 0, action, 0);

        final InputEvent event = mInput.pollEvent();
        Assert.assertEquals(KeyEvent.class, event.getClass());
    }

    @Test
    public void testPollReturnsMouseEvent()
    {
        // Mouse button press
        final int button = GLFW.GLFW_MOUSE_BUTTON_LEFT;
        final int action = GLFW.GLFW_PRESS;
        mInput.getMouseButtonCallback().onButtonUpdate(button, action, 0d, 0d);

        final InputEvent event = mInput.pollEvent();
        Assert.assertEquals(MouseEvent.class, event.getClass());
    }

    @Test
    public void testPollReturnsPadEvent()
    {
        fakeXboxGamepadConnection();

        // Gamepad button press
        final ByteBuffer states = createButtonState(XB1.Button.A.button(), true);
        mInput.getGamepadUpdateCallback().onButtonsUpdate(Connection.PAD_1, states);

        final InputEvent event = mInput.pollEvent();
        Assert.assertEquals(PadEvent.class, event.getClass());
    }

    @Test
    public void testPollReturnsNull()
    {
        Assert.assertNull(mInput.pollEvent());
    }

    @Test
    public void testPollReturnsInOrder()
    {
        // Scroll
        mInput.getMouseScrollCallback().onScrollUpdate(25d, 50d, 0d, 0d);

        // Key press
        final int key = GLFW.GLFW_KEY_SPACE;
        final int event = GLFW.GLFW_PRESS;
        mInput.getKeyboardKeyCallback().invoke(0L, key, 0, event, 0);

        // Mouse button press
        final int button = GLFW.GLFW_MOUSE_BUTTON_LEFT;
        mInput.getMouseButtonCallback().onButtonUpdate(button, GLFW.GLFW_PRESS, 0d, 0d);

        Assert.assertEquals(MouseEvent.class, mInput.pollEvent().getClass());
        Assert.assertEquals(KeyEvent.class, mInput.pollEvent().getClass());
        Assert.assertEquals(MouseEvent.class, mInput.pollEvent().getClass());
    }

    @Test
    public void testPollDropsPadEventWithDisconnectedGamepad()
    {
        fakeXboxGamepadConnection();

        mInput.submit(new PadEvent(0L, Connection.PAD_1, XB1.Button.A, true));

        fakeXboxGamepadDisconnection();

        Assert.assertNull(mInput.pollEvent());
    }

    @Test
    public void testPollReturnsNullWhenKeyboardIsMuted()
    {
        final Keyboard keyboard = mInput.getKeyboard();

        keyboard.mute();

        // Key release
        final int key = GLFW.GLFW_KEY_ENTER;
        final int event = GLFW.GLFW_RELEASE;
        mInput.getKeyboardKeyCallback().invoke(0L, key, 0, event, 0);

        Assert.assertNull(mInput.pollEvent());
    }

    @Test
    public void testPollReturnsNullWhenMouseIsMuted()
    {
        final Mouse mouse = mInput.getMouse();

        mouse.mute();

        // Button release
        final int button = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
        final int event = GLFW.GLFW_RELEASE;
        mInput.getMouseButtonCallback().onButtonUpdate(button, event, 0d, 0d);

        Assert.assertNull(mInput.pollEvent());
    }

    @Test
    public void testPollReturnsNullWhenGamepadIsMuted()
    {
        fakeXboxGamepadConnection();

        mInput.getGamepad(Connection.PAD_1).mute();

        // Report new button states
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();
        final ByteBuffer states = createButtonState(XB1.Button.A.button(), true);
        callback.onButtonsUpdate(Connection.PAD_1, states);

        Assert.assertNull(mInput.pollEvent());
    }

    @Test
    public void testAddGamepadProfile()
    {
        mInput.addGamepadProfile(TEST_PROFILE_NAME, new CustomPadProfile(Button.class, Axis.class));
    }

    @Test (expected = NullPointerException.class)
    public void testAddGamepadProfileNPEName()
    {
        mInput.addGamepadProfile(null, new CustomPadProfile(Button.class, Axis.class));
    }

    @Test (expected = NullPointerException.class)
    public void testAddGamepadProfileNPEProfile()
    {
        mInput.addGamepadProfile(TEST_PROFILE_NAME, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddGamepadProfileIAE()
    {
        final PadProfile profile = new CustomPadProfile(Button.class, Axis.class);

        mInput.addGamepadProfile(TEST_PROFILE_NAME, profile);
        mInput.addGamepadProfile(TEST_PROFILE_NAME, profile);
    }

    @Test
    public void testContainsGamepadProfile()
    {
        mInput.addGamepadProfile(TEST_PROFILE_NAME, new CustomPadProfile(Button.class, Axis.class));

        Assert.assertTrue(mInput.containsGamepadProfile(TEST_PROFILE_NAME));
    }

    @Test
    public void testContainsGamepadProfileReturnsFalse()
    {
        Assert.assertFalse(mInput.containsGamepadProfile(TEST_PROFILE_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsGamepadProfileNPE()
    {
        mInput.containsGamepadProfile(null);
    }

    @Test
    public void testContainsGamepadProfileForXboxReturnsTrue()
    {
        Assert.assertTrue(mInput.containsGamepadProfile(XB1.GAMEPAD_NAME));
    }

    @Test
    public void testContainsGamepadProfileForPlayStationReturnsTrue()
    {
        Assert.assertTrue(mInput.containsGamepadProfile(PS4.GAMEPAD_NAME));
    }

    @Test
    public void testGetKeyboard()
    {
        Assert.assertNotNull(mInput.getKeyboard());
    }

    @Test
    public void testGetMouse()
    {
        Assert.assertNotNull(mInput.getMouse());
    }

    @Test
    public void testGetKeyboardCallback()
    {
        Assert.assertNotNull(mInput.getKeyboardKeyCallback());
    }

    @Test
    public void testGetMousePositionCallback()
    {
        Assert.assertNotNull(mInput.getMousePositionCallback());
    }

    @Test
    public void testGetMouseButtonCallback()
    {
        Assert.assertNotNull(mInput.getMouseButtonCallback());
    }

    @Test
    public void testGetMouseScrollCallback()
    {
        Assert.assertNotNull(mInput.getMouseScrollCallback());
    }

    @Test
    public void testGetGamepadConnectionCallback()
    {
        Assert.assertNotNull(mInput.getGamepadConnectionCallback());
    }

    @Test
    public void testGetGamepadUpdateCallback()
    {
        Assert.assertNotNull(mInput.getGamepadUpdateCallback());
    }

    @Test
    public void testGetKeyboardHistory()
    {
        Assert.assertNotNull(mInput.getKeyboardHistory());
    }

    @Test
    public void testGetMouseHistory()
    {
        Assert.assertNotNull(mInput.getMouseButtonHistory());
    }

    @Test
    public void testGetGamepadButtonHistory()
    {
        fakeXboxGamepadConnection();

        Assert.assertNotNull(mInput.getGamepadButtonHistory(Connection.PAD_1));
    }

    @Test
    public void testGetGamepadButtonHistoryReturnsNull()
    {
        Assert.assertNull(mInput.getGamepadButtonHistory(Connection.PAD_1));
    }

    @Test
    public void testGetGamepadMotionHistory()
    {
        fakeXboxGamepadConnection();

        Assert.assertNotNull(mInput.getGamepadMotionHistory(Connection.PAD_1));
    }

    @Test
    public void testGetGamepadMotionHistoryReturnsNull()
    {
        Assert.assertNull(mInput.getGamepadMotionHistory(Connection.PAD_1));
    }

    @Test
    public void testGetGamepad()
    {
        fakeXboxGamepadConnection();

        Assert.assertNotNull(mInput.getGamepad(Connection.PAD_1));
    }

    @Test
    public void testGetGamepadReturnsNull()
    {
        Assert.assertNull(mInput.getGamepad(Connection.PAD_1));
    }

    @Test
    public void testGetGamepads()
    {
        fakeXboxGamepadConnection();

        Assert.assertFalse(mInput.getGamepads().isEmpty());
    }

    @Test
    public void testGetGamepadsReturnsEmpty()
    {
        Assert.assertTrue(mInput.getGamepads().isEmpty());
    }

    @Test
    public void testAddOnGamepadConnectionChangeListener()
    {
        mInput.addGamepadOnConnectionChangeListener((gamepad) -> { });
    }

    @Test (expected = NullPointerException.class)
    public void testAddOnGamepadConnectionChangeListenerNPE()
    {
        mInput.addGamepadOnConnectionChangeListener(null);
    }

    @Test
    public void testRemoveOnGamepadConnectionChangeListener()
    {
        final OnConnectionChangeListener listener = (gamepad) -> {};

        mInput.addGamepadOnConnectionChangeListener(listener);
        mInput.removeGamepadOnConnectionChangeListener(listener);
    }

    private void fakeXboxGamepadConnection()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        final int joystick = Connection.PAD_1.toInt();
        final int event = GLFW.GLFW_CONNECTED;

        callback.onConnectionUpdate(joystick, event, XB1.GAMEPAD_NAME);
    }

    private void fakeXboxGamepadDisconnection()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        final int joystick = Connection.PAD_1.toInt();
        final int event = GLFW.GLFW_DISCONNECTED;

        callback.onConnectionUpdate(joystick, event, XB1.GAMEPAD_NAME);
    }

    /**
     * <p>Creates a {@code ByteBuffer} containing the button state of all {@code Gamepad.Button}s. The specified
     * button's state will be set while all others will be in their release state.</p>
     *
     * @param button to press.
     * @param press true if press.
     * @return button states.
     */
    private ByteBuffer createButtonState(Gamepad.Button button, boolean press)
    {
        final Gamepad.Button[] buttons = Gamepad.Button.values();
        final ByteBuffer buffer = BufferUtils.createByteBuffer(buttons.length);

        // Set the state for each button
        for (final Gamepad.Button expected : buttons) {
            final byte b;

            if (button == expected) {
                b = (press) ? (byte) 0x1 : (byte) 0x0;
            } else {
                // All other buttons are released
                b = (byte) 0x0;
            }

            buffer.put(expected.toInt(), b);
        }

        return buffer;
    }
}
