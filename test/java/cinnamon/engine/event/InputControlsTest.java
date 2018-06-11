package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;
import cinnamon.engine.event.Gamepad.State;
import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.Input.OnConnectionChangeListener;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.event.XB1.Stick;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import cinnamon.engine.utils.Table;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class InputControlsTest
{
    // Player 1
    private static final Connection CONNECTION = Connection.PAD_1;

    // Test gamepad: Xbox One controller
    private static final PadProfile PROFILE = XB1.GAMEPAD_PROFILE;

    private static final String RULE_NAME = "test_rule";

    // For mocking gamepad connection
    private OnConnectionChangeListener mConnectionListener;

    // Devices
    private Input mInput;

    // Device histories
    private InputHistories mHistories;

    private InputControls mCtrl;

    // Gamepad write-access
    private State mGamepadState;

    private Gamepad mGamepad;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp()
    {
        mHistories = mockInputHistories();
        mInput = mockInput();
        mCtrl = new InputControls(mInput, mock(EventSource.class), mHistories);
    }

    @After
    public void tearDown()
    {
        mConnectionListener = null;
        mHistories = null;
        mGamepadState = null;
        mGamepad = null;
    }

    @Test (expected = NullPointerException.class)
    @SuppressWarnings("unchecked")
    public void testConstructorNPEInput()
    {
        new InputControls(null, mock(EventSource.class), mHistories);
    }

    @Test (expected = NullPointerException.class)
    @SuppressWarnings("unchecked")
    public void testConstructorNPESource()
    {
        new InputControls(mInput, null, mHistories);
    }

    @Test (expected = NullPointerException.class)
    @SuppressWarnings("unchecked")
    public void testConstructorNPEHistories()
    {
        new InputControls(mInput, mock(EventSource.class), null);
    }

    @Test
    public void testGetKeyboardKeys()
    {
        Assert.assertNotNull(mCtrl.getKeyboardKeys());
    }

    @Test
    public void testGetKeyboardKeysReturnsEmpty()
    {
        Assert.assertTrue(mCtrl.getKeyboardKeys().isEmpty());
    }

    @Test
    public void testGetKeyboardKeysReturnsPreviouslySet()
    {
        final Map<String, ButtonRule<Key, KeyEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(Key.KEY_SPACE, (event) -> {}, ButtonPreferences.forRelease(), 0));

        mCtrl.setKeyboardKeys(b);

        Assert.assertEquals(b, mCtrl.getKeyboardKeys());
    }

    @Test
    public void testSetKeyboardKeys()
    {
        mCtrl.setKeyboardKeys(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetKeyboardKeysNPE()
    {
        mCtrl.setKeyboardKeys(null);
    }

    @Test
    public void testGetMouseButtons()
    {
        Assert.assertNotNull(mCtrl.getMouseButtons());
    }

    @Test
    public void testGetMouseButtonsReturnsEmpty()
    {
        Assert.assertTrue(mCtrl.getMouseButtons().isEmpty());
    }

    @Test
    public void testGetMouseButtonsReturnsPreviouslySet()
    {
        final Map<String, ButtonRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(Button.LEFT, (event) -> {}, ButtonPreferences.forRelease(), 0));

        mCtrl.setMouseButtons(b);

        Assert.assertEquals(b, mCtrl.getMouseButtons());
    }

    @Test
    public void testSetMouseButtons()
    {
        mCtrl.setMouseButtons(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetMouseButtonsNPE()
    {
        mCtrl.setMouseButtons(null);
    }

    @Test
    public void testGetMouseScrolls()
    {
        Assert.assertNotNull(mCtrl.getMouseScrolls());
    }

    @Test
    public void testGetMouseScrollsReturnsEmpty()
    {
        Assert.assertTrue(mCtrl.getMouseScrolls().isEmpty());
    }

    @Test
    public void testGetMouseScrollsReturnsPreviouslySet()
    {
        final HashMap<String, AxisRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Button.MIDDLE, (event) -> {}, MotionPreferences.forTranslation(), 0));

        mCtrl.setMouseScrolls(b);

        Assert.assertEquals(b, mCtrl.getMouseScrolls());
    }

    @Test
    public void testSetMouseScrolls()
    {
        mCtrl.setMouseScrolls(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetMouseScrollsNPE()
    {
        mCtrl.setMouseScrolls(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMouseScrollsIAERuleDoesNotUseMiddleButton()
    {
        final HashMap<String, AxisRule<Button, MouseEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Button.LEFT, (event) -> {}, MotionPreferences.forTranslation(), 0));

        mCtrl.setMouseScrolls(b);
    }

    @Test
    public void testGetGamepadButtons()
    {
        mockGamepadConnected(CONNECTION);

        Assert.assertNotNull(mCtrl.getGamepadButtons(CONNECTION, XB1.Button.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsNPEConnection()
    {
        mCtrl.getGamepadButtons(null, XB1.Button.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadButtonsNPEClass()
    {
        mCtrl.getGamepadButtons(CONNECTION, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadButtonsIAEWrongClass()
    {
        mockGamepadConnected(CONNECTION);

        mCtrl.setGamepadButtons(CONNECTION, createXboxButtonMap());
        mCtrl.getGamepadButtons(CONNECTION, MockButton.class);
    }

    @Test (expected = IllegalStateException.class)
    public void testGetGamepadButtonsISENoAvailableGamepad()
    {
        mockGamepadDisconnected(CONNECTION);

        mCtrl.getGamepadButtons(CONNECTION, XB1.Button.class);
    }

    @Test
    public void testGetGamepadButtonsReturnsEmpty()
    {
        mockGamepadConnected(CONNECTION);

        Assert.assertTrue(mCtrl.getGamepadButtons(CONNECTION, XB1.Button.class).isEmpty());
    }

    @Test
    public void testGetGamepadButtonsReturnsPreviouslySet()
    {
        mockGamepadConnected(CONNECTION);

        final Map<String, ButtonRule<XB1.Button, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(XB1.Button.A, (event) -> {}, ButtonPreferences.forRelease(), 0));

        mCtrl.setGamepadButtons(CONNECTION, b);

        Assert.assertEquals(b, mCtrl.getGamepadButtons(CONNECTION, XB1.Button.class));
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadButtonsNPEConnection()
    {
        mCtrl.setGamepadButtons(null, createXboxButtonMap());
    }

    @Test (expected = IllegalStateException.class)
    public void testSetGamepadButtonsISENoAvailableGamepad()
    {
        mockGamepadDisconnected(CONNECTION);

        mCtrl.setGamepadButtons(CONNECTION, createXboxButtonMap());
    }

    @Test
    public void testGetGamepadAxes()
    {
        mockGamepadConnected(CONNECTION);

        Assert.assertNotNull(mCtrl.getGamepadAxes(CONNECTION, Stick.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesNPEConnection()
    {
        mCtrl.getGamepadAxes(null, Stick.class);
    }

    @Test (expected = NullPointerException.class)
    public void testGetGamepadAxesNPEClass()
    {
        mCtrl.getGamepadAxes(CONNECTION, null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetGamepadAxesIAEWrongClass()
    {
        mockGamepadConnected(CONNECTION);

        mCtrl.setGamepadAxes(CONNECTION, createXboxAxisMap());
        mCtrl.getGamepadAxes(CONNECTION, MockAxis.class);
    }

    @Test (expected = IllegalStateException.class)
    public void testGetGamepadAxesISENoAvailableGamepad()
    {
        mCtrl.getGamepadAxes(CONNECTION, Stick.class);
    }

    @Test
    public void testGetGamepadAxesReturnsEmpty()
    {
        mockGamepadConnected(CONNECTION);

        Assert.assertTrue(mCtrl.getGamepadAxes(CONNECTION, Stick.class).isEmpty());
    }

    @Test
    public void testGetGamepadAxesReturnsPreviouslySet()
    {
        mockGamepadConnected(CONNECTION);

        final Map<String, AxisRule<Stick, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Stick.RIGHT_TRIGGER, (event) -> {}, MotionPreferences.forTranslation(), 0));

        mCtrl.setGamepadAxes(CONNECTION, b);

        Assert.assertEquals(b, mCtrl.getGamepadAxes(CONNECTION, Stick.class));
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadAxesNPEConnection()
    {
        mCtrl.setGamepadAxes(null, createXboxAxisMap());
    }

    @Test (expected = NullPointerException.class)
    public void testSetGamepadAxesNPEButtonBindings()
    {
        mCtrl.setGamepadAxes(CONNECTION, null);
    }

    @Test (expected = IllegalStateException.class)
    public void testSetGamepadAxesISENoAvailableGamepad()
    {
        mCtrl.setGamepadAxes(CONNECTION, createXboxAxisMap());
    }

    /**
     * <p>Returns an {@link Input} with initial event histories for keyboard and mouse. The
     * {@link OnConnectionChangeListener} set through
     * {@link Input#addGamepadOnConnectionChangeListener(OnConnectionChangeListener)} is captured.</p>
     *
     * @return input.
     */
    private Input mockInput()
    {
        final Table<KeyEvent> keyPressHistory = new FixedQueueArray<>(Key.COUNT, 4);
        final FixedQueueArray<KeyEvent> keyReleaseHistory = new FixedQueueArray<>(Key.COUNT, 4);
        final FixedQueueArray<MouseEvent> buttonPressHistory = new FixedQueueArray<>(Button.COUNT, 4);
        final FixedQueueArray<MouseEvent> buttonReleaseHistory = new FixedQueueArray<>(Button.COUNT, 4);
        final FixedQueueArray<MouseEvent> mouseScrollHistory = new FixedQueueArray<>(1, 4);

        final Input input = mock(Input.class);
        when(input.getKeyboard()).thenReturn(new Keyboard(keyPressHistory, keyReleaseHistory));
        when(input.getMouse()).thenReturn(new Mouse(createMouseState(buttonPressHistory, buttonReleaseHistory,
                mouseScrollHistory)));

        // Intercept listener for on-demand connection changes
        doAnswer((invocation) ->
        {
            mConnectionListener = (OnConnectionChangeListener) invocation.getArguments()[0];
            return null;

        }).when(input).addGamepadOnConnectionChangeListener(any(OnConnectionChangeListener.class));

        return input;
    }

    private Mouse.State createMouseState(FixedQueueArray<MouseEvent> pressHistory, FixedQueueArray<MouseEvent>
            releaseHistory, FixedQueueArray<MouseEvent> scrollHistory)
    {
        return Mouse.State.builder()
                .pressHistory(pressHistory)
                .releaseHistory(releaseHistory)
                .scrollHistory(scrollHistory)
                .position(new Point())
                .build();
    }

    @SuppressWarnings("unchecked")
    private InputHistories mockInputHistories()
    {
        final Table<KeyEvent>[] keyboardHistory = new Table[] {mock(Table.class), mock(Table.class)};
        final Table<MouseEvent>[] mouseHistory = new Table[] {mock(Table.class), mock(Table.class)};
        final InputHistories histories = mock(InputHistories.class);

        when(histories.getKeyboardHistory()).thenReturn(keyboardHistory);
        when(histories.getMouseButtonHistory()).thenReturn(mouseHistory);
        when(histories.getMouseScrollHistory()).thenReturn(mock(Table.class));

        return histories;
    }

    /**
     * <p>Creates a {@code Gamepad} configured for the specified {@code Connection} and {@code PadProfile}.</p>
     *
     * @param connection connection.
     * @param profile profile.
     */
    @SuppressWarnings("unchecked")
    private void createGamepad(Connection connection, PadProfile profile)
    {
        final FixedQueueArray<PadEvent>[] buttons = new FixedQueueArray[2];
        buttons[0] = new FixedQueueArray<>(XB1.Button.values().length, 4);
        buttons[1] = new FixedQueueArray<>(XB1.Button.values().length, 4);

        final FixedQueueArray<PadEvent> motions = new FixedQueueArray<>(Stick.values().length, 4);
        final FixedQueueArray<Boolean> zones = new FixedQueueArray<>(Stick.values().length, 4);

        mGamepadState = State.builder()
                .pressHistory(buttons[0])
                .releaseHistory(buttons[1])
                .motionHistory(motions)
                .deadZoneHistory(zones)
                .build();

        mGamepad = new Gamepad(connection, profile, mGamepadState);

        when(mHistories.getGamepadButtonHistory(connection)).thenReturn(buttons);
        when(mHistories.getGamepadMotionHistory(connection)).thenReturn(motions);
    }

    /**
     * <p>Imitates a gamepad connection.</p>
     */
    private void mockGamepadConnected(Connection connection)
    {
        createGamepad(connection, PROFILE);
        when(mInput.getGamepad(connection)).thenReturn(mGamepad);

        // Fake notification
        mGamepadState.setConnected(true);
        mConnectionListener.onChange(mGamepad);
    }

    /**
     * <p>Imitates a gamepad disconnection.</p>
     */
    private void mockGamepadDisconnected(Connection connection)
    {
        final Gamepad pad = mInput.getGamepad(connection);
        if (pad == null) {
            return;
        }

        // Fake notification
        mGamepadState.setConnected(false);
        mConnectionListener.onChange(mGamepad);

        when(mHistories.getGamepadButtonHistory(connection)).thenReturn(null);
        when(mHistories.getGamepadMotionHistory(connection)).thenReturn(null);
        when(mInput.getGamepad(connection)).thenReturn(null);
    }

    private Map<String, ButtonRule<XB1.Button, PadEvent>> createXboxButtonMap()
    {
        final Map<String, ButtonRule<XB1.Button, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new ButtonRule<>(XB1.Button.A, (event) -> {}, ButtonPreferences.forRelease(), 0));
        return b;
    }

    private Map<String, AxisRule<Stick, PadEvent>> createXboxAxisMap()
    {
        final Map<String, AxisRule<Stick, PadEvent>> b = new HashMap<>();
        b.put(RULE_NAME, new AxisRule<>(Stick.RIGHT_TRIGGER, (event) -> {}, MotionPreferences.forTranslation(), 0));
        return b;
    }

    /**
     * <p>Used to force an {@code IllegalArgumentException} since gamepad isn't configured for this button class.</p>
     */
    private enum MockButton implements ButtonWrapper
    {
        ;

        @Override
        public Gamepad.Button button()
        {
            return null;
        }
    }

    /**
     * <p>Used to force an {@code IllegalArgumentException} since gamepad isn't configured for this axis class.</p>
     */
    private enum MockAxis implements AxisWrapper
    {
        ;

        @Override
        public Axis vertical()
        {
            return null;
        }

        @Override
        public Axis horizontal()
        {
            return null;
        }
    }
}
