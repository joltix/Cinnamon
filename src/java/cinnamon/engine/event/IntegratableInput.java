package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.*;
import cinnamon.engine.event.Input.BufferedInput;
import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.Table;
import org.lwjgl.glfw.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * <p>This implementation offers callbacks for reporting new input data and is designed for use with the GLFW input
 * API, though GLFW integration is not required in order to use this class. This decoupling has the effect of having
 * many of the callbacks unrecognizable by GLFW's callback setters. These can be easily wrapped, as shown below.</p>
 *
 * <pre>
 *     <code>
 *
 *         // Forward the call from GLFW callback
 *         GLFW.glfwSetMouseButtonCallback(firstWindow, (window, button, action, mods) {@literal ->}
 *         {
 *             GLFW.glfwGetCursorPos(window, x, y);
 *             callback.onButtonUpdate(button, action, mods, x[0], y[0]);
 *         });
 *     </code>
 * </pre>
 *
 * <p>For methods returning a GLFW callback which has a window handle parameter, any value may be passed in. This
 * parameter is currently ignored.</p><br>
 *
 * <b>Keyboard and Mouse</b>
 * <p>The keyboard's key states are reported through {@link GLFWKeyCallbackI} while the mouse's button and scroll
 * states, as well as position, is delivered through {@code MouseButtonCallback}, {@code MouseScrollCallback}, and
 * {@link GLFWCursorPosCallbackI}.</p><br>
 *
 * <b>Gamepads</b>
 * <p>The button and stick states of all {@code Gamepad}s are updated with each call to both
 * {@code GamepadUpdateCallback.onButtonsUpdate(Connection, ByteBuffer)} and
 * {@code GamepadUpdateCallback.onAxesUpdate(Connection, FloatBuffer)}. The list of available {@code Gamepad}s are
 * only updated when the callback returned by {@code getGamepadConnectionCallback()} is notified.</p>
 */
public final class IntegratableInput implements BufferedInput
{
    // Indices into arrays from history creation
    private static final int PRESS_HISTORY = 0;
    private static final int RELEASE_HISTORY = 1;

    // Initial event load
    private static final int BUFFER_SIZE = 20;

    // How far back to track button events
    private static final int BUTTON_GENS = 4;
    private static final int AXIS_GENS = 2;

    private final List<OnConnectionChangeListener> mConnListeners = new ArrayList<>();

    private final Map<String, PadProfile> mGamepadProfiles = new HashMap<>();

    // All input placed and consumed here, in order
    private final Queue<InputEvent> mBuffer = new ArrayDeque<>(BUFFER_SIZE);

    // Keyboard button history
    private final FixedQueueArray<KeyEvent> mKeyboardPresses;
    private final FixedQueueArray<KeyEvent> mKeyboardReleases;

    // Mouse button history
    private final FixedQueueArray<MouseEvent> mMousePresses;
    private final FixedQueueArray<MouseEvent> mMouseReleases;

    private final Keyboard mKeyboard;
    private final Mouse mMouse;
    private final Mouse.State mMouseState;

    // Gamepad entries
    private final Map<Connection, PadData> mPads = new EnumMap<>(Connection.class);

    /**
     * <p>Constructs an {@code IntegratableInput}.</p>
     */
    public IntegratableInput()
    {
        final FixedQueueArray<KeyEvent>[] keyboardHistories = createDefaultKeyboardHistory();
        mKeyboardPresses = keyboardHistories[PRESS_HISTORY];
        mKeyboardReleases = keyboardHistories[RELEASE_HISTORY];

        final FixedQueueArray<MouseEvent>[] mouseHistories = createDefaultMouseHistory();
        mMousePresses = mouseHistories[PRESS_HISTORY];
        mMouseReleases = mouseHistories[RELEASE_HISTORY];

        mKeyboard = new Keyboard(mKeyboardPresses, mKeyboardReleases);
        mMouseState = new Mouse.State(mMousePresses, mMouseReleases);
        mMouse = new Mouse(mMouseState);

        // Support Xbox controllers
        mGamepadProfiles.put(XboxPadProfile.NAME, new XboxPadProfile());
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public InputEvent poll()
    {
        return mBuffer.poll();
    }

    /**
     * {@inheritDoc}
     * @param event event.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void submit(KeyEvent event)
    {
        checkNull(event);

        addButtonEventToHistory(event.getKey(), event, mKeyboardPresses, mKeyboardReleases);
        mBuffer.add(event);
    }

    /**
     * {@inheritDoc}
     * @param event event.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void submit(MouseEvent event)
    {
        checkNull(event);

        addButtonEventToHistory(event.getButton(), event, mMousePresses, mMouseReleases);
        mBuffer.add(event);
    }

    /**
     * {@inheritDoc}
     * @param event event.
     * @throws NullPointerException {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void submit(PadEvent event)
    {
        checkNull(event);

        final PadData data = mPads.get(event.getSource());
        if (data == null) {
            return;
        }

        if (event.isAxis()) {
            final Axis axis = ((AxisWrapper) event.getWrapper()).getVertical();
            data.mAxisHistory.add(axis.ordinal(), event);

        } else {
            final Gamepad.Button button = ((ButtonWrapper) event.getWrapper()).toButton();
            addButtonEventToHistory(button, event, data.mPresses, data.mReleases);
        }
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Keyboard getKeyboard()
    {
        return mKeyboard;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Mouse getMouse()
    {
        return mMouse;
    }

    /**
     * {@inheritDoc}
     * @param connection connection.
     * @return {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public Gamepad getGamepad(Connection connection)
    {
        checkNull(connection);

        final PadData data = mPads.get(connection);
        if (data == null) {
            return null;
        }

        return data.mGamepad;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Map<Connection, Gamepad> getGamepads()
    {
        final Map<Connection, Gamepad> gamepads = new EnumMap<>(Connection.class);

        for (final Connection connection : Connection.values()) {
            final PadData data = mPads.get(connection);
            if (data != null) {
                gamepads.put(connection, data.mGamepad);
            }
        }

        return gamepads;
    }

    /**
     * <p>Gets the callback to produce {@code KeyEvent}s.</p>
     *
     * <p>If the keyboard is muted or the callback is executed with the {@link GLFW#GLFW_KEY_UNKNOWN} key or
     * {@link GLFW#GLFW_REPEAT} action, then this callback does nothing. The {@code window} parameter is ignored and
     * any value may be passed in.</p>
     *
     * @return callback.
     */
    public GLFWKeyCallbackI getKeyboardKeyCallback()
    {
        return (window, key, scancode, action, mods) ->
        {
            if (mKeyboard.isMuted() || key == GLFW.GLFW_KEY_UNKNOWN ||
                    action == GLFW.GLFW_REPEAT) {
                return;
            }

            final boolean press = action == GLFW.GLFW_PRESS;
            final KeyEvent event = KeyEvent.createForKey(Key.from(key), press);

            addButtonEventToHistory(event.getKey(), event, mKeyboardPresses, mKeyboardReleases);
            mBuffer.add(event);
        };
    }

    /**
     * <p>Gets the callback to update the {@code Mouse}'s position. This callback is not affected by the
     * {@code Mouse}'s mute state.</p>
     *
     * @return callback.
     */
    public GLFWCursorPosCallbackI getMousePositionCallback()
    {
        return (window, x, y) ->
        {
            mMouseState.setPosition((float) x, (float) y);
        };
    }

    /**
     * <p>Gets the callback to produce {@code MouseEvent}s for mouse button activity.</p>
     *
     * <p>If the mouse is muted or the callback is executed with the {@link GLFW#GLFW_REPEAT} action, then this
     * method does nothing.</p>
     *
     * @return callback.
     */
    public MouseButtonCallback getMouseButtonCallback()
    {
        return (button, action, mods, x, y) ->
        {
            if (mMouse.isMuted() || action == GLFW.GLFW_REPEAT) {
                return;
            }

            final boolean press;
            if (action == GLFW.GLFW_PRESS) {
                press = true;
            } else if (action == GLFW.GLFW_RELEASE) {
                press = false;
            } else {
                throw new IllegalArgumentException("Unrecognized action, given: " + action);
            }

            final Button constant = Button.from(button);
            if (constant == null) {
                throw new IllegalArgumentException("Unrecognized mouse button: " + button);
            }

            final float fX = (float) x;
            final float fY = (float) y;
            mMouseState.setPosition(fX, fY);

            final MouseEvent event = MouseEvent.createForButton(constant, press, fX, fY);
            addButtonEventToHistory(constant, event, mMousePresses, mMouseReleases);
            mBuffer.add(event);
        };
    }

    /**
     * <p>Gets a callback to produce {@code MouseEvent}s for mouse scroll activity.</p>
     *
     * <p>If the mouse is muted, then this method does nothing.</p>
     *
     * @return callback.
     */
    public MouseScrollCallback getMouseScrollCallback()
    {
        return (xOffset, yOffset, x, y) ->
        {
            if (mMouse.isMuted()) {
                return;
            }

            final float fX = (float) x;
            final float fY = (float) y;
            mMouseState.setPosition(fX, fY);

            final float offsetX = (float) xOffset;
            final float offsetY = (float) yOffset;
            mMouseState.setHorizontalScrollOffset(offsetX);
            mMouseState.setVerticalScrollOffset(offsetY);

            mBuffer.add(MouseEvent.createForScroll(offsetX, offsetY, fX, fY));
        };
    }

    /**
     * <p>Gets a callback to produce {@code PadEvent}s for button and axis activity.</p>
     *
     * <p>If the gamepad is unavailable or muted, then both {@code onButtonsUpdate(Connection, ByteBuffer)} and
     * {@code onAxesUpdate(Connection, FloatBuffer)} does nothing.</p>
     *
     * @return callback.
     */
    public GamepadUpdateCallback getGamepadUpdateCallback()
    {
        return new ImplGamepadUpdateCallback();
    }

    /**
     * <p>Gets a callback to update the list of available {@code Gamepad}s.</p>
     *
     * @return callback.
     */
    public GamepadConnectionCallback getGamepadConnectionCallback()
    {
        return (joystick, event, name) ->
        {
            checkNull(name);

            final Connection conn = Gamepad.Connection.from(joystick);
            if (conn == null) {
                return;
            }

            final Gamepad gamepad;
            final boolean connected;

            if (event == GLFW.GLFW_CONNECTED) {

                // Ignore unrecognized (no profile)
                final PadProfile profile = mGamepadProfiles.get(name);
                if (profile == null) {
                    return;
                }

                gamepad = createGamepad(conn, profile);
                connected = true;

            } else if (event == GLFW.GLFW_DISCONNECTED) {

                // Discard gamepad data
                final PadData data = mPads.remove(conn);
                gamepad = data.mGamepad;
                data.mState.setConnected(false);
                connected = false;

            } else {
                throw new IllegalArgumentException("Unrecognized GLFW event: " + event);
            }

            for (final OnConnectionChangeListener listener : mConnListeners) {
                listener.onChange(gamepad, connected);
            }
        };
    }

    /**
     * {@inheritDoc}
     * @param name name.
     * @return {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean containsGamepadProfile(String name)
    {
        checkNull(name);

        return mGamepadProfiles.containsKey(name);
    }

    /**
     * {@inheritDoc}
     * @param name profile name.
     * @param profile profile.
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public void addGamepadProfile(String name, PadProfile profile)
    {
        checkNull(name);
        checkNull(profile);

        if (mGamepadProfiles.containsKey(name)) {
            throw new IllegalArgumentException("PadProfile name \'" + name + "\' is already in use");
        }

        mGamepadProfiles.put(name, profile);
    }

    /**
     * {@inheritDoc}
     * @param listener listener.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void addGamepadOnConnectionChangeListener(OnConnectionChangeListener listener)
    {
        checkNull(listener);

        mConnListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * @param listener listener.
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public void removeGamepadOnConnectionChangeListener(OnConnectionChangeListener listener)
    {
        checkNull(listener);

        mConnListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Table<KeyEvent>[] getKeyboardHistory()
    {
        final Table<KeyEvent>[] histories = (Table<KeyEvent>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<KeyEvent>()
        {
            @Override
            public KeyEvent get(int x, int y)
            {
                return mKeyboardPresses.get(x, y);
            }

            @Override
            public int width()
            {
                return mKeyboardPresses.width();
            }

            @Override
            public int height()
            {
                return mKeyboardPresses.height();
            }
        };

        histories[RELEASE_HISTORY] = new Table<KeyEvent>()
        {
            @Override
            public KeyEvent get(int x, int y)
            {
                return mKeyboardReleases.get(x, y);
            }

            @Override
            public int width()
            {
                return mKeyboardReleases.width();
            }

            @Override
            public int height()
            {
                return mKeyboardReleases.height();
            }
        };

        return histories;
    }

    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Table<MouseEvent>[] getMouseHistory()
    {
        final Table<MouseEvent>[] histories = (Table<MouseEvent>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<MouseEvent>()
        {
            @Override
            public MouseEvent get(int x, int y)
            {
                return mMousePresses.get(x, y);
            }

            @Override
            public int width()
            {
                return mMousePresses.width();
            }

            @Override
            public int height()
            {
                return mMousePresses.height();
            }
        };

        histories[RELEASE_HISTORY] = new Table<MouseEvent>()
        {
            @Override
            public MouseEvent get(int x, int y)
            {
                return mMouseReleases.get(x, y);
            }

            @Override
            public int width()
            {
                return mMouseReleases.width();
            }

            @Override
            public int height()
            {
                return mMouseReleases.height();
            }
        };

        return histories;
    }

    /**
     * {@inheritDoc}
     * @param connection connection.
     * @return {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Table<PadEvent<ButtonWrapper>>[] getGamepadButtonHistory(Connection connection)
    {
        checkNull(connection);

        final PadData data = mPads.get(connection);
        if (data == null) {
            return null;
        }

        final Table<PadEvent<ButtonWrapper>>[] histories = (Table<PadEvent<ButtonWrapper>>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<PadEvent<ButtonWrapper>>()
        {
            private final Table<PadEvent<ButtonWrapper>> mActual = data.mPresses;

            @Override
            public PadEvent<ButtonWrapper> get(int x, int y)
            {
                return mActual.get(x, y);
            }

            @Override
            public int width()
            {
                return mActual.width();
            }

            @Override
            public int height()
            {
                return mActual.height();
            }
        };

        histories[RELEASE_HISTORY] = new Table<PadEvent<ButtonWrapper>>()
        {
            private final Table<PadEvent<ButtonWrapper>> mActual = data.mReleases;

            @Override
            public PadEvent<ButtonWrapper> get(int x, int y)
            {
                return mActual.get(x, y);
            }

            @Override
            public int width()
            {
                return mActual.width();
            }

            @Override
            public int height()
            {
                return mActual.height();
            }
        };

        return histories;
    }

    /**
     * {@inheritDoc}
     * @param connection connection.
     * @return {@inheritDoc}
     */
    @Override
    public Table<PadEvent<AxisWrapper>> getGamepadAxisHistory(Connection connection)
    {
        checkNull(connection);

        final PadData data = mPads.get(connection);
        if (data == null) {
            return null;
        }

        return new Table<PadEvent<AxisWrapper>>()
        {
            @Override
            public PadEvent<AxisWrapper> get(int x, int y)
            {
                return data.mAxisHistory.get(x, y);
            }

            @Override
            public int width()
            {
                return data.mAxisHistory.width();
            }

            @Override
            public int height()
            {
                return data.mAxisHistory.height();
            }
        };
    }

    /**
     * <p>Creates the button history for the {@code Keyboard}. Entries are made for two clicks (four events: two
     * presses and two releases).</p>
     *
     * @return button history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<KeyEvent>[] createDefaultKeyboardHistory()
    {
        final Key[] keys = Key.values();
        final FixedQueueArray presses = new FixedQueueArray<>(keys.length, BUTTON_GENS);
        final FixedQueueArray releases = new FixedQueueArray<>(keys.length, BUTTON_GENS);

        for (final Key key : keys) {
            final int ord = key.ordinal();

            presses.add(ord, KeyEvent.createForKey(key, true));
            releases.add(ord, KeyEvent.createForKey(key, false));

            presses.add(ord, KeyEvent.createForKey(key, true));
            releases.add(ord, KeyEvent.createForKey(key, false));
        }

        final FixedQueueArray<KeyEvent>[] histories = (FixedQueueArray<KeyEvent>[]) new FixedQueueArray[2];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        return histories;
    }

    /**
     * <p>Creates the button history for the {@code Mouse}. Entries are made for two clicks (four events: two presses
     * and two releases). All events return a position of (0,0).</p>
     *
     * @return button history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<MouseEvent>[] createDefaultMouseHistory()
    {
        final Button[] buttons = Button.values();
        final FixedQueueArray presses = new FixedQueueArray<>(buttons.length, BUTTON_GENS);
        final FixedQueueArray releases = new FixedQueueArray<>(buttons.length, BUTTON_GENS);

        for (final Button button : buttons) {
            final int ord = button.ordinal();

            presses.add(ord, MouseEvent.createForButton(button, true, 0f, 0f));
            releases.add(ord, MouseEvent.createForButton(button, false, 0f, 0f));

            presses.add(ord, MouseEvent.createForButton(button, true, 0f, 0f));
            releases.add(ord, MouseEvent.createForButton(button, false, 0f, 0f));
        }

        final FixedQueueArray<MouseEvent>[] histories = (FixedQueueArray<MouseEvent>[]) new FixedQueueArray[2];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        return histories;
    }

    /**
     * <p>Creates the button history for the {@code Gamepad} of the given {@code Connection}. Entries are made for two
     * clicks (four events: two presses and two releases).</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return button history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<PadEvent<ButtonWrapper>>[] createGamepadButtonHistory(Connection connection,
                                                                                  PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final ButtonWrapper[] wrappers = profile.getButtonClass().getEnumConstants();
        final FixedQueueArray<PadEvent<ButtonWrapper>> presses = new FixedQueueArray<>(wrappers.length, BUTTON_GENS);
        final FixedQueueArray<PadEvent<ButtonWrapper>> releases = new FixedQueueArray<>(wrappers.length, BUTTON_GENS);

        for (final ButtonWrapper wrapper : wrappers) {
            final int ord = wrapper.toButton().ordinal();

            presses.add(ord, PadEvent.createForButton(connection, wrapper, true));
            releases.add(ord, PadEvent.createForButton(connection, wrapper, false));

            presses.add(ord, PadEvent.createForButton(connection, wrapper, true));
            releases.add(ord, PadEvent.createForButton(connection, wrapper, false));
        }

        final FixedQueueArray<PadEvent<ButtonWrapper>>[] histories = (FixedQueueArray<PadEvent<ButtonWrapper>>[]) new
                FixedQueueArray[2];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        return histories;
    }

    /**
     * <p>Creates the axis history for the {@code Gamepad} of the given {@code Connection}. All entries report the
     * default resting position as returned by the {@code Gamepad}'s {@code PadProfile}.</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return axis history.
     */
    private FixedQueueArray<PadEvent<AxisWrapper>> createGamepadAxisHistory(Connection connection, PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final Map<Axis, Float> resting = profile.getRestingAxisValues();
        final AxisWrapper[] axes = profile.getAxisClass().getEnumConstants();
        final int axisCount = Axis.values().length;
        final FixedQueueArray<PadEvent<AxisWrapper>> history = new FixedQueueArray<>(axisCount,  AXIS_GENS);

        // Write entries for events at resting positions
        for (final AxisWrapper wrapper : axes) {

            final float vertical = resting.get(wrapper.getVertical());
            final Axis secondary = wrapper.getHorizontal();
            final float horizontal = (secondary != null) ? resting.get(secondary) : 0f;

            final int ord = wrapper.getVertical().ordinal();
            history.add(ord, PadEvent.createForAxis(connection, wrapper, horizontal, vertical));
            history.add(ord, PadEvent.createForAxis(connection, wrapper, horizontal, vertical));
        }

        return history;
    }

    /**
     * <p>Creates the dead zone history for the {@code Gamepad} of the given {@code Connection}. All entries report
     * true (i.e. the axis is presumed to be at the default resting position defined by the {@code Gamepad}'s
     * {@code PadProfile}).</p>
     *
     * @param profile profile.
     * @return dead zone history.
     */
    private FixedQueueArray<Boolean> createGamepadDeadZoneHistory(PadProfile profile)
    {
        assert (profile != null);

        final AxisWrapper[] axes = profile.getAxisClass().getEnumConstants();
        final int axisCount = Axis.values().length;
        final FixedQueueArray<Boolean> history = new FixedQueueArray<>(axisCount, BUTTON_GENS);

        for (final AxisWrapper wrapper : axes) {
            final int ord = wrapper.getVertical().ordinal();

            history.add(ord, true);
            history.add(ord, true);

            history.add(ord, true);
            history.add(ord, true);
        }

        return history;
    }

    /**
     * <p>Instantiates a {@code Gamepad} with default event histories and sets up the the internal {@code PadData}
     * wrapper. When this method returns, {@link Gamepad#isConnected()} will return true.</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return gamepad.
     */
    private Gamepad createGamepad(Connection connection, PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final FixedQueueArray<PadEvent<ButtonWrapper>>[] buttons = createGamepadButtonHistory(connection, profile);
        final FixedQueueArray<PadEvent<AxisWrapper>> axes = createGamepadAxisHistory(connection, profile);
        final FixedQueueArray<Boolean> zones = createGamepadDeadZoneHistory(profile);

        final State state = new State(buttons[PRESS_HISTORY], buttons[RELEASE_HISTORY], axes, zones);
        final Gamepad gamepad = new Gamepad(connection, profile, state);

        state.setConnected(true);
        mPads.put(connection, new PadData(state, gamepad, buttons, axes, zones));

        return gamepad;
    }

    /**
     * <p>Clamps the given axis value between -1 and 1. If the given value is outside this range, either -1 or 1 is
     * returned - whichever is closer.</p>
     *
     * @param value value.
     * @return clamped value.
     */
    private float clampAxis(float value)
    {
        return (value < -1f) ? -1f : (value > 1f) ? 1f : value;
    }

    private <T extends ButtonEvent> void addButtonEventToHistory(Enum constant, T event,
                                                                 FixedQueueArray<T> pressHistory,
                                                                 FixedQueueArray<T> releaseHistory)
    {
        final int ord = constant.ordinal();
        if (event.isPress()) {
            pressHistory.add(ord, event);
        } else {
            releaseHistory.add(ord, event);
        }
    }

    private <T extends InputEvent> boolean isRecentPress(int ordinal, Table<T> pressHistory, Table<T> releaseHistory)
    {
        final T press = pressHistory.get(0, ordinal);
        final T release = releaseHistory.get(0, ordinal);

        return press.getTime() > release.getTime();
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Wrapper for holding related gamepad data.</p>
     */
    private class PadData
    {
        private final int mExpectedButtonBufferCap;
        private final int mExpectedAxisBufferCap;

        private final ButtonWrapper[] mButtons;
        private final AxisWrapper[] mAxes;

        private final Gamepad mGamepad;
        private final State mState;

        private final FixedQueueArray<PadEvent<ButtonWrapper>> mPresses;
        private final FixedQueueArray<PadEvent<ButtonWrapper>> mReleases;
        private final FixedQueueArray<PadEvent<AxisWrapper>> mAxisHistory;
        private final FixedQueueArray<Boolean> mZoneHistory;

        private PadData(State state, Gamepad gamepad, FixedQueueArray<PadEvent<ButtonWrapper>>[] buttonHistory,
                        FixedQueueArray<PadEvent<AxisWrapper>> axisHistory, FixedQueueArray<Boolean> zoneHistory)
        {
            final PadProfile profile = gamepad.getProfile();
            mButtons = profile.getButtonClass().getEnumConstants();
            mAxes = profile.getAxisClass().getEnumConstants();

            mPresses = buttonHistory[PRESS_HISTORY];
            mReleases = buttonHistory[RELEASE_HISTORY];
            mAxisHistory = axisHistory;
            mZoneHistory = zoneHistory;

            mState = state;
            mGamepad = gamepad;

            mExpectedButtonBufferCap = profile.getButtonRange();
            mExpectedAxisBufferCap = profile.getAxisRange();
        }
    }

    private class ImplGamepadUpdateCallback implements GamepadUpdateCallback
    {
        @Override
        public void onButtonsUpdate(Connection connection, ByteBuffer buffer)
        {
            checkNull(connection);
            checkNull(buffer);

            final PadData data = mPads.get(connection);
            if (data == null || data.mGamepad.isMuted()) {
                return;
            }

            if (buffer.capacity() < data.mExpectedButtonBufferCap) {
                throw new IllegalArgumentException("Buffer capacity is too small, expected: " +
                        data.mExpectedButtonBufferCap + " actual: " + buffer.capacity());
            }

            for (ButtonWrapper wrapper : data.mButtons) {

                final Gamepad.Button rawButton = wrapper.toButton();
                final int ord = rawButton.ordinal();

                // Choose history for new event
                final boolean pressed;
                final FixedQueueArray<PadEvent<ButtonWrapper>> history;
                if (pressed = (buffer.get(rawButton.toInt()) == 0x1)) {
                    history = data.mPresses;
                } else {
                    history = data.mReleases;
                }

                // No state change means no event
                if (pressed != isRecentPress(ord, data.mPresses, data.mReleases)) {

                    final PadEvent<ButtonWrapper> event = PadEvent.createForButton(connection, wrapper, pressed);
                    history.add(ord, event);
                    mBuffer.add(event);
                }
            }
        }

        @Override
        public void onAxesUpdate(Connection connection, FloatBuffer buffer)
        {
            checkNull(connection);
            checkNull(buffer);

            final PadData data = mPads.get(connection);
            if (data == null || data.mGamepad.isMuted()) {
                return;
            }

            if (buffer.capacity() < data.mExpectedAxisBufferCap) {
                throw new IllegalArgumentException("Buffer capacity is too small, expected: " +
                        data.mExpectedAxisBufferCap + " actual: " + buffer.capacity());
            }

            final Gamepad gamepad = data.mGamepad;
            final FixedQueueArray<PadEvent<AxisWrapper>> axisHistory = data.mAxisHistory;
            final FixedQueueArray<Boolean> zoneHistory = data.mZoneHistory;

            for (final AxisWrapper axis : data.mAxes) {

                final Axis vertical = axis.getVertical();
                final Axis horizontal = axis.getHorizontal();
                final PadEvent<AxisWrapper> lastEvent = axisHistory.get(0, vertical.ordinal());

                final float v = clampAxis(buffer.get(vertical.toInt()));
                final float h = (horizontal == null) ? 0f : clampAxis(buffer.get(horizontal.toInt()));

                // Only create event if position(s) are different than previous
                if (lastEvent.getVertical() != v ||
                        lastEvent.getHorizontal() != h) {

                    final int ord = axis.getVertical().ordinal();
                    final boolean inside = gamepad.isInsideDeadZone(axis, v, h);
                    zoneHistory.add(ord, inside);

                    // Ignore when inside dead zone for > 1 axis update
                    if (!gamepad.isInsideDeadZone(axis, v, h)) {

                        final PadEvent<AxisWrapper> next = PadEvent.createForAxis(connection, axis, h, v);
                        axisHistory.add(ord, next);
                        mBuffer.add(next);
                    }
                }
            }
        }
    }

    /**
     * <p>This callback should be notified when a mouse button's state has changed.</p>
     */
    public interface MouseButtonCallback
    {
        /**
         * <p>Called when a mouse button's state has changed from a press to a release and vice versa.</p>
         *
         * <p>If action is {@link GLFW#GLFW_REPEAT}, then this method does nothing.</p>
         *
         * @param button button.
         * @param action action.
         * @param mods mods.
         * @param x x.
         * @param y y.
         * @throws IllegalArgumentException if button does not refer to a {@link Button} or if action is neither
         * {@code GLFW_PRESS}, {@code GLFW_RELEASE}, nor {@code GLFW_REPEAT}.
         */
        void onButtonUpdate(int button, int action, int mods, double x, double y);
    }

    /**
     * <p>This callback should be notified when a mouse scroll has occurred.</p>
     */
    public interface MouseScrollCallback
    {
        /**
         * <p>Called when a mouse wheel has been moved.</p>
         *
         * @param xOffset horizontal scroll.
         * @param yOffset vertical scroll.
         * @param x x.
         * @param y y.
         */
        void onScrollUpdate(double xOffset, double yOffset, double x, double y);
    }

    /**
     * <p>This callback should be notified when a gamepad has either connected or disconnected.</p>
     */
    public interface GamepadConnectionCallback
    {
        /**
         * <p>Called when a gamepad's connection state has changed.</p>
         *
         * <p>If the given joystick index does not match the {@code int} value of any {@code Connection} through
         * {@link Connection#toInt()}, then this method does nothing.</p>
         *
         * @param joystick joystick.
         * @param event event.
         * @param name joystick name.
         * @throws NullPointerException if name is null.
         * @throws IllegalArgumentException if event is neither {@code GLFW_CONNECTED} nor {@code GLFW_DISCONNECTED}.
         */
        void onConnectionUpdate(int joystick, int event, String name);
    }

    /**
     * <p>This callback should be notified when a gamepad's button and axis states need to be updated.</p>
     */
    public interface GamepadUpdateCallback
    {
        /**
         * <p>Called when a gamepad's button states need to be updated.</p>
         *
         * <p>Button states should be given through the {@code buffer} in true-false form as bytes (0x1 for the
         * pressed state and 0x0 for release). These states should be indexed according to each {@code Button}'s
         * internal {@code int} value, as returned by {@link Button#toInt()}.</p>
         *
         * <p>If the given {@code Connection} is not associated with an available {@code Gamepad}, then this method
         * does nothing.</p>
         *
         * @param connection connection.
         * @param buffer new button states.
         * @throws NullPointerException if connection or buffer is null.
         * @throws IllegalArgumentException if buffer capacity {@literal <} the number of expected buttons.
         */
        void onButtonsUpdate(Connection connection, ByteBuffer buffer);

        /**
         * <p>Called when a gamepad's axis states need to be updated.</p>
         *
         * <p>Axis states should be given through the {@code buffer} with each value clamped between -1 and 1,
         * inclusive. These states should be indexed according to each {@code Axis}' internal {@code int} value, as
         * returned by {@link Axis#toInt()}.</p>
         *
         * @param connection connection.
         * @param buffer new axis states.
         * @throws NullPointerException if connection or buffer is null.
         * @throws IllegalArgumentException if buffer capacity {@literal <} the number of expected axes.
         */
        void onAxesUpdate(Connection connection, FloatBuffer buffer);
    }
}