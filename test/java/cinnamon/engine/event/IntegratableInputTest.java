package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.Input.OnConnectionChangeListener;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.PadProfileTest.MockPadProfile;
import cinnamon.engine.event.PadProfileTest.MockPadProfile.Button;
import cinnamon.engine.event.PadProfileTest.MockPadProfile.Axis;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import java.nio.ByteBuffer;

/**
 * <p>These tests focus on the {@code IntegratableInput}'s state and expected exceptions. Callbacks such as that
 * returned by {@code getMouseButtonCallback()} are tested in {@link IntegratableInputCallbackTest}.</p>
 */
public class IntegratableInputTest
{
    private static final String GAMEPAD_NAME = "Mock Controller";

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
        mInput.submit(KeyEvent.createForKey(Key.KEY_A, true));
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
        mInput.submit(MouseEvent.createForButton(Mouse.Button.RIGHT, true, 0f, 0f));
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
        mInput.submit(PadEvent.createForButton(Gamepad.Connection.PAD_1, XboxPadProfile.Button.A, true));
    }

    @Test (expected = NullPointerException.class)
    public void testSubmitPadEventNPE()
    {
        final PadEvent event = null;
        mInput.submit(event);
    }

    @Test
    public void testPollReturnsNull()
    {
        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testPollReturnsInOrder()
    {
        mInput.getMouseScrollCallback().onScrollUpdate(25d, 50d, 0d, 0d);
        mInput.getKeyboardKeyCallback().invoke(0L, GLFW.GLFW_KEY_SPACE, 0, GLFW.GLFW_PRESS, 0);
        mInput.getMouseButtonCallback().onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_LEFT, GLFW.GLFW_PRESS, 0, 0d, 0d);

        Assert.assertEquals(MouseEvent.class, mInput.poll().getClass());
        Assert.assertEquals(KeyEvent.class, mInput.poll().getClass());
        Assert.assertEquals(MouseEvent.class, mInput.poll().getClass());
    }

    @Test
    public void testXboxPadProfileExists()
    {
        Assert.assertTrue(mInput.containsGamepadProfile(XboxPadProfile.NAME));
    }

    @Test
    public void testPollReturnsNullWhenKeyboardIsMuted()
    {
        final Keyboard keyboard = mInput.getKeyboard();

        keyboard.mute();
        mInput.getKeyboardKeyCallback().invoke(0L, GLFW.GLFW_KEY_ENTER, 0, GLFW.GLFW_RELEASE, 0);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testPollReturnsNullWhenMouseIsMuted()
    {
        final Mouse mouse = mInput.getMouse();

        mouse.mute();
        mInput.getMouseButtonCallback().onButtonUpdate(GLFW.GLFW_MOUSE_BUTTON_RIGHT, GLFW.GLFW_RELEASE, 0, 0d, 0d);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testPollReturnsNullWhenGamepadIsMuted()
    {
        fakeGamepadConnection();

        final Gamepad gamepad = mInput.getGamepad(Connection.PAD_1);
        final GamepadUpdateCallback callback = mInput.getGamepadUpdateCallback();

        final ByteBuffer states = createButtonState(XboxPadProfile.Button.A.toButton(), true);
        gamepad.mute();
        callback.onButtonsUpdate(Connection.PAD_1, states);

        Assert.assertNull(mInput.poll());
    }

    @Test
    public void testAddGamepadProfile()
    {
        mInput.addGamepadProfile(GAMEPAD_NAME, new MockPadProfile(Button.class, Axis.class));
    }

    @Test (expected = NullPointerException.class)
    public void testAddGamepadProfileNPEName()
    {
        mInput.addGamepadProfile(null, new MockPadProfile(Button.class, Axis.class));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddGamepadProfileIAE()
    {
        mInput.addGamepadProfile(GAMEPAD_NAME, new MockPadProfile(Button.class, Axis.class));
        mInput.addGamepadProfile(GAMEPAD_NAME, new MockPadProfile(Button.class, Axis.class));
    }

    @Test
    public void testContainsGamepadProfile()
    {
        mInput.addGamepadProfile(GAMEPAD_NAME, new MockPadProfile(Button.class, Axis.class));

        Assert.assertTrue(mInput.containsGamepadProfile(GAMEPAD_NAME));
    }

    @Test
    public void testContainsGamepadProfileReturnsFalse()
    {
        Assert.assertFalse(mInput.containsGamepadProfile(GAMEPAD_NAME));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsGamepadProfileNPE()
    {
        mInput.containsGamepadProfile(null);
    }

    @Test
    public void testGetKeyboardNotNull()
    {
        Assert.assertNotNull(mInput.getKeyboard());
    }

    @Test
    public void testGetMouseNotNull()
    {
        Assert.assertNotNull(mInput.getMouse());
    }

    @Test
    public void testGetKeyboardCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getKeyboardKeyCallback());
    }

    @Test
    public void testGetMousePositionCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getMousePositionCallback());
    }

    @Test
    public void testGetMouseButtonCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getMouseButtonCallback());
    }

    @Test
    public void testGetMouseScrollCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getMouseScrollCallback());
    }

    @Test
    public void testGetGamepadConnectionCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getGamepadConnectionCallback());
    }

    @Test
    public void testGetGamepadUpdateCallbackNotNull()
    {
        Assert.assertNotNull(mInput.getGamepadUpdateCallback());
    }

    @Test
    public void testGetKeyboardHistoryNotNull()
    {
        Assert.assertNotNull(mInput.getKeyboardHistory());
    }

    @Test
    public void testGetMouseHistoryNotNull()
    {
        Assert.assertNotNull(mInput.getMouseHistory());
    }

    @Test
    public void testGetGamepadButtonHistoryReturnsNull()
    {
        Assert.assertNull(mInput.getGamepadButtonHistory(Gamepad.Connection.PAD_1));
    }

    @Test
    public void testGetGamepadAxisHistoryReturnsNull()
    {
        Assert.assertNull(mInput.getGamepadAxisHistory(Gamepad.Connection.PAD_1));
    }

    @Test
    public void testGetGamepadReturnsNull()
    {
        Assert.assertNull(mInput.getGamepad(Gamepad.Connection.PAD_1));
    }

    @Test
    public void testGetGamepadsReturnsEmpty()
    {
        Assert.assertTrue(mInput.getGamepads().isEmpty());
    }

    @Test
    public void testAddOnGamepadConnectionChangeListener()
    {
        mInput.addGamepadOnConnectionChangeListener((gamepad, connected) -> { });
    }

    @Test (expected = NullPointerException.class)
    public void testAddOnGamepadConnectionChangeListenerNPE()
    {
        mInput.addGamepadOnConnectionChangeListener(null);
    }

    @Test
    public void testRemoveOnGamepadConnectionChangeListener()
    {
        final OnConnectionChangeListener listener = (gamepad, connected) -> {};

        mInput.addGamepadOnConnectionChangeListener(listener);
        mInput.removeGamepadOnConnectionChangeListener(listener);
    }

    private void fakeGamepadConnection()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();
        callback.onConnectionUpdate(Connection.PAD_1.toInt(), GLFW.GLFW_CONNECTED, XboxPadProfile.NAME);
    }

    private ByteBuffer createButtonState(Gamepad.Button button, boolean press)
    {
        final Gamepad.Button[] buttons = Gamepad.Button.values();
        final ByteBuffer buffer = BufferUtils.createByteBuffer(buttons.length);

        for (final Gamepad.Button expected : buttons) {
            final byte b;
            if (button == expected) {
                b = (press) ? (byte) 0x1 : (byte) 0x0;
            } else {
                b = (byte) 0x0;
            }

            buffer.put(expected.toInt(), b);
        }

        return buffer;
    }
}
