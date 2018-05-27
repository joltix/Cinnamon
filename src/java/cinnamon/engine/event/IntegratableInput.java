package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.*;
import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import cinnamon.engine.utils.Table;
import org.lwjgl.glfw.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * <p>Creates {@link InputEvent}s to populate device views (e.g. {@code Keyboard}) while allowing read-access to each
 * view's event histories. This class is designed for use with the GLFW input API but does not require any GLFW
 * state, including whether GLFW is initialized. Interaction with GLFW occurs externally and passes the necessary
 * data to this class' callbacks which are either styled similar to GLFW's callbacks or actually are GLFW's
 * callbacks . This decoupling has the effect of having many of the callbacks unrecognizable by GLFW's callback
 * setters but these can be easily wrapped, as shown below.</p>
 *
 * <pre>
 *     <code>
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
 * parameter is currently ignored.</p>
 *
 * <h3>Keyboard and Mouse</h3>
 * <p>The keyboard's key states are reported through {@link GLFWKeyCallbackI} while the mouse's button and scroll
 * states, as well as position, is delivered through {@code MouseButtonCallback}, {@code MouseScrollCallback}, and
 * {@link GLFWCursorPosCallbackI}.</p>
 *
 * <h3>Gamepads</h3>
 * <p>The button and axis states of all {@code Gamepad}s are updated with each call to both
 * {@code GamepadUpdateCallback.onButtonsUpdate(Connection, ByteBuffer)} and
 * {@code GamepadUpdateCallback.onAxesUpdate(Connection, FloatBuffer)}.</p>
 * <p>Available {@code Gamepad}s are only updated when the callback returned by
 * {@code getGamepadConnectionCallback()} is notified.</p>
 *
 * <h3>Event histories</h3>
 * <p>Each device view is backed by a handful of read-only event histories and can be retrieved through getters like
 * {@link #getKeyboardHistory()}. Unlike directly querying {@code Keyboard}, {@code Mouse}, and {@code Gamepad},
 * these histories allow reading of events further back in time than the most recent. New events are only written to
 * the appropriate history once the events are returned by {@link #pollEvent()}.</p>
 *
 * <h3>Dropped events</h3>
 * <p>While most events reported to {@code IntegratableInput} are passed out through {@link #pollEvent()}, there are
 * a few cases where events may be discarded.</p>
 *
 * <ul>
 *     <li>for event callbacks, the corresponding device requested no events be made (e.g. {@link Keyboard#isMuted()})
 *     </li>
 *     <li>one of the arguments passed to an event-generating callback was unrecognized (see the callback's
 *     documentation)</li>
 *     <li>for {@code PadEvent}s, the corresponding gamepad was either not available when the event callback was
 *     notified or the gamepad was disconnected prior to the event being polled</li>
 * </ul>
 *
 * <p>Dropped events are not written to their device's history and are effectively ignored.</p>
 */
public final class IntegratableInput implements Input, InputHistories, EventSource<InputEvent>
{
    // Indices into arrays when event histories are passed around
    private static final int PRESS_HISTORY = 0;
    private static final int RELEASE_HISTORY = 1;
    private static final int SCROLL_HISTORY = 2;

    // Initial event load
    private static final int BUFFER_SIZE = 20;

    // Number of button events to track per history
    private static final int BUTTON_GENS = 2;

    // Number of axis events to track per history
    private static final int AXIS_GENS = 2;

    // Ordinal for middle mouse button
    private static final int MOUSE_MIDDLE_ORD = Button.MIDDLE.ordinal();

    // Written into history before each new scroll event
    private static final MouseEvent NO_SCROLL_EVENT = new MouseEvent(0L, new Point(), new Point());

    private final InputEventVisitor mHistoryWriter = new InputEventVisitor()
    {
        @Override
        public void visit(KeyEvent event)
        {
            addButtonEventToHistory(event.getKey(), event, mKeyboardPresses, mKeyboardReleases);
            mEventWritten = true;
        }

        @Override
        public void visit(MouseEvent event)
        {
            // Write event to scroll history with NO_SCROLL_EVENT to represent a cease in scrolling
            if (event.isScroll()) {
                final FixedQueueArray<MouseEvent> history = mMouseState.getScrollHistory();
                history.add(MOUSE_MIDDLE_ORD, NO_SCROLL_EVENT);
                history.add(MOUSE_MIDDLE_ORD, event);

            } else {
                assert (event.getButton() != null);

                // Write to button history
                final FixedQueueArray<MouseEvent> pressHistory = mMouseState.getPressHistory();
                final FixedQueueArray<MouseEvent> releasesHistory = mMouseState.getReleaseHistory();
                addButtonEventToHistory(event.getButton(), event, pressHistory, releasesHistory);
            }

            mEventWritten = true;
        }

        @Override
        public void visit(PadEvent event)
        {
            final PadInfo info = mPads.get(event.getSource());

            // Gamepad isn't connected so ignore event
            if (info == null) {
                mEventWritten = false;
                return;
            }

            // Write to button history
            if (event.isButton()) {
                final FixedQueueArray<PadEvent> presses = info.mState.getPressHistory();
                final FixedQueueArray<PadEvent> releases = info.mState.getReleaseHistory();
                addButtonEventToHistory(event.getButton().button(), event, presses, releases);

            } else {
                assert (event.getAxis() != null);

                // Sensor based event -> write to motion history
                info.mState.getMotionHistory().add(event.getAxis().vertical().ordinal(), event);
            }
            mEventWritten = true;
        }
    };

    // True if the last visit to the history writer actually wrote to history
    private boolean mEventWritten = false;

    // Listeners to notify whenever a gamepad connects/disconnects
    private final List<OnConnectionChangeListener> mConnListeners = new ArrayList<>();

    // All recognized gamepad setups
    private final Map<String, PadProfile> mGamepadProfiles = new HashMap<>();

    // All input placed and consumed here, in order
    private final Queue<InputEvent> mBuffer = new ArrayDeque<>(BUFFER_SIZE);

    // Keyboard press history
    private final FixedQueueArray<KeyEvent> mKeyboardPresses;

    // Keyboard release history
    private final FixedQueueArray<KeyEvent> mKeyboardReleases;

    private final Keyboard mKeyboard;

    // Write access to mouse' state
    private final Mouse.State mMouseState;

    private final Mouse mMouse;

    // Gamepad entries
    private final Map<Connection, PadInfo> mPads = new EnumMap<>(Connection.class);

    /**
     * <p>Constructs an {@code IntegratableInput}.</p>
     */
    public IntegratableInput()
    {
        // Set up keyboard
        final FixedQueueArray<KeyEvent>[] keyboardHistories = createDefaultKeyboardHistory();
        mKeyboardPresses = keyboardHistories[PRESS_HISTORY];
        mKeyboardReleases = keyboardHistories[RELEASE_HISTORY];
        mKeyboard = new Keyboard(mKeyboardPresses, mKeyboardReleases);

        // Set up mouse
        mMouseState = createMouseState();
        mMouse = new Mouse(mMouseState);

        // Support console controllers
        addGamepadProfile(XB1.GAMEPAD_NAME, XB1.GAMEPAD_PROFILE);
        addGamepadProfile(PS4.GAMEPAD_NAME, PS4.GAMEPAD_PROFILE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Each polled event is written to the appropriate event history prior to returning.</p>
     */
    @Override
    public InputEvent pollEvent()
    {
        InputEvent event;
        while ((event = mBuffer.poll()) != null) {
            event.accept(mHistoryWriter);

            // Should ignore dropped events
            if (mEventWritten) {
                break;
            }
        }

        return event;
    }

    @Override
    public void submit(KeyEvent event)
    {
        checkNull(event);

        mBuffer.add(event);
    }

    @Override
    public void submit(MouseEvent event)
    {
        checkNull(event);

        mBuffer.add(event);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void submit(PadEvent event)
    {
        checkNull(event);

        // Ignore events for unavailable gamepads
        if (mPads.get(event.getSource()) != null) {
            mBuffer.add(event);
        }
    }

    @Override
    public Keyboard getKeyboard()
    {
        return mKeyboard;
    }

    @Override
    public Mouse getMouse()
    {
        return mMouse;
    }

    @Override
    public Gamepad getGamepad(Connection connection)
    {
        checkNull(connection);

        final PadInfo data = mPads.get(connection);
        return (data == null) ? null : data.mGamepad;
    }

    @Override
    public Map<Connection, Gamepad> getGamepads()
    {
        final Map<Connection, Gamepad> gamepads = new EnumMap<>(Connection.class);

        for (final PadInfo info : mPads.values()) {
            gamepads.put(info.mGamepad.getConnection(), info.mGamepad);
        }

        return gamepads;
    }

    /**
     * <p>Gets the callback to produce {@code KeyEvent}s.</p>
     *
     * <p>If this callback is notified with any {@code key} where {@link Key#from(int)} would return null, if
     * {@code action} is neither {@link GLFW#GLFW_PRESS} nor {@link GLFW#GLFW_RELEASE}, or if
     * {@link Keyboard#isMuted()} returns {@code true}, then this callback does nothing. The callback's {@code window}
     * parameter is ignored and any value may be passed in.</p>
     *
     * @return callback.
     */
    public GLFWKeyCallbackI getKeyboardKeyCallback()
    {
        return (window, key, scanCode, action, mods) ->
        {
            // Check if keyboard wants silence
            if (mKeyboard.isMuted()) {
                return;
            }

            // Unrecognized action -> ignore
            if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
                return;
            }

            final Key constant = Key.from(key);

            // Unrecognized key -> ignore
            if (constant == null) {
                return;
            }

            mBuffer.add(new KeyEvent(System.nanoTime(), constant, action == GLFW.GLFW_PRESS));
        };
    }

    /**
     * <p>Gets the callback to update the {@code Mouse}'s position.</p>
     *
     * <p>This callback ignores the {@code Mouse}'s mute state and its {@code window} parameter.</p>
     *
     * @return callback.
     */
    public GLFWCursorPosCallbackI getMousePositionCallback()
    {
        return (window, x, y) ->
        {
            mMouseState.getPosition().setPosition((float) x, (float) y, 0f);
        };
    }

    /**
     * <p>Gets the callback to produce {@code MouseEvent}s for press-release activity.</p>
     *
     * @return callback.
     */
    public MouseButtonCallback getMouseButtonCallback()
    {
        return (button, action, x, y) ->
        {
            // Check if mouse wants silence
            if (mMouse.isMuted()) {
                return;
            }

            // Ignore anything but press-release activity
            if (action != GLFW.GLFW_PRESS && action != GLFW.GLFW_RELEASE) {
                return;
            }

            final Button constant = Button.from(button);

            // Unrecognized button -> ignore
            if (constant == null) {
                return;
            }

            final Point position = mMouseState.getPosition();
            position.setPosition((float) x, (float) y, 0f);

            final boolean press = action == GLFW.GLFW_PRESS;
            mBuffer.add(new MouseEvent(System.nanoTime(), position, constant, press));
        };
    }

    /**
     * <p>Gets a callback to produce {@code MouseEvent}s for scroll activity.</p>
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

            // Update mouse's position
            final Point position = new Point((float) x, (float) y, 0f);
            mMouseState.getPosition().copy(position);

            // Ensure offsets are between -1 and +1, inclusive
            final Point offsets = new Point(clampScroll(xOffset), clampScroll(yOffset), 0f);

            mBuffer.add(new MouseEvent(System.nanoTime(), position, offsets));
        };
    }

    /**
     * <p>Gets a callback to produce {@code PadEvent}s for button and axis-based activity.</p>
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

            // Unsupported gamepad -> ignore
            if (conn == null) {
                return;
            }

            // Only handle connected/disconnected events
            if (event != GLFW.GLFW_CONNECTED && event != GLFW.GLFW_DISCONNECTED) {
                return;
            }

            final Gamepad gamepad;

            if (event == GLFW.GLFW_CONNECTED) {
                final PadProfile profile = mGamepadProfiles.get(name);

                // Ignore unrecognized gamepads
                if (profile == null) {
                    return;
                }

                gamepad = createGamepad(conn, profile);

            } else if (mPads.containsKey(conn)) {

                // Discard gamepad data
                final PadInfo data = mPads.remove(conn);
                gamepad = data.mGamepad;
                data.mState.setConnected(false);

            } else {
                // Can't disconnect an unavailable gamepad
                return;
            }

            // Notify about connection change
            for (final OnConnectionChangeListener listener : mConnListeners) {
                listener.onChange(gamepad);
            }
        };
    }

    @Override
    public boolean containsGamepadProfile(String name)
    {
        checkNull(name);

        return mGamepadProfiles.containsKey(name);
    }

    @Override
    public void addGamepadProfile(String name, PadProfile profile)
    {
        checkNull(name);
        checkNull(profile);

        // Check name if name is unavailable
        if (mGamepadProfiles.containsKey(name)) {
            final String format = "%s name \"%s\" is already in use";
            final String profileCls = PadProfile.class.getSimpleName();
            throw new IllegalArgumentException(String.format(format, profileCls, name));
        }

        mGamepadProfiles.put(name, profile);
    }

    @Override
    public void addGamepadOnConnectionChangeListener(OnConnectionChangeListener listener)
    {
        checkNull(listener);

        mConnListeners.add(listener);
    }

    @Override
    public void removeGamepadOnConnectionChangeListener(OnConnectionChangeListener listener)
    {
        checkNull(listener);

        mConnListeners.remove(listener);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Table<KeyEvent>[] getKeyboardHistory()
    {
        final Table<KeyEvent>[] histories = (Table<KeyEvent>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<KeyEvent>()
        {
            @Override
            public KeyEvent get(int column, int row)
            {
                return mKeyboardPresses.get(column, row);
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
            public KeyEvent get(int column, int row)
            {
                return mKeyboardReleases.get(column, row);
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

    @Override
    @SuppressWarnings("unchecked")
    public Table<MouseEvent>[] getMouseButtonHistory()
    {
        final Table<MouseEvent>[] histories = (Table<MouseEvent>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<MouseEvent>()
        {
            private final Table<MouseEvent> mActual = mMouseState.getPressHistory();

            @Override
            public MouseEvent get(int column, int row)
            {
                return mActual.get(column, row);
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

        histories[RELEASE_HISTORY] = new Table<MouseEvent>()
        {
            private final Table<MouseEvent> mActual = mMouseState.getReleaseHistory();

            @Override
            public MouseEvent get(int column, int row)
            {
                return mActual.get(column, row);
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

    @Override
    public Table<MouseEvent> getMouseScrollHistory()
    {
        return new Table<MouseEvent>()
        {
            private final Table<MouseEvent> mActual = mMouseState.getScrollHistory();

            @Override
            public MouseEvent get(int column, int row)
            {
                return mActual.get(column, row);
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
    }

    @Override
    @SuppressWarnings("unchecked")
    public Table<PadEvent>[] getGamepadButtonHistory(Connection connection)
    {
        checkNull(connection);

        final PadInfo info = mPads.get(connection);

        // Ignore unconnected gamepad
        if (info == null) {
            return null;
        }

        final Table<PadEvent>[] histories = (Table<PadEvent>[]) new Table[2];

        histories[PRESS_HISTORY] = new Table<PadEvent>()
        {
            private final Table<PadEvent> mActual = info.mState.getPressHistory();

            @Override
            public PadEvent get(int column, int row)
            {
                return mActual.get(column, row);
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

        histories[RELEASE_HISTORY] = new Table<PadEvent>()
        {
            private final Table<PadEvent> mActual = info.mState.getReleaseHistory();

            @Override
            public PadEvent get(int column, int row)
            {
                return mActual.get(column, row);
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

    @Override
    public Table<PadEvent> getGamepadMotionHistory(Connection connection)
    {
        checkNull(connection);

        final PadInfo info = mPads.get(connection);

        // Ignore unconnected gamepads
        if (info == null) {
            return null;
        }

        return new Table<PadEvent>()
        {
            private final Table<PadEvent> mActual = info.mState.getMotionHistory();

            @Override
            public PadEvent get(int column, int row)
            {
                return mActual.get(column, row);
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
    }

    /**
     * <p>Creates the write-access object for the {@code Mouse}.</p>
     *
     * @return mouse state.
     */
    private Mouse.State createMouseState()
    {
        final FixedQueueArray<MouseEvent>[] mouseHistories = createDefaultMouseHistory();

        return Mouse.State.builder()
                .pressHistory(mouseHistories[PRESS_HISTORY])
                .releaseHistory(mouseHistories[RELEASE_HISTORY])
                .scrollHistory(mouseHistories[SCROLL_HISTORY])
                .position(new Point()).build();
    }

    /**
     * <p>Creates the button history for the {@code Keyboard}. Entries are made for two clicks (four events: two
     * presses and two releases) and all events have the same creation time. Index 0 contains the press history and
     * index 1 contains the release history.</p>
     *
     * @return button history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<KeyEvent>[] createDefaultKeyboardHistory()
    {
        final Key[] keys = Key.values();
        final FixedQueueArray presses = new FixedQueueArray<>(keys.length, BUTTON_GENS);
        final FixedQueueArray releases = new FixedQueueArray<>(keys.length, BUTTON_GENS);
        final long time = System.nanoTime();

        for (final Key key : keys) {
            final int ord = key.ordinal();

            presses.add(ord, new KeyEvent(time, key, true));
            releases.add(ord, new KeyEvent(time, key, false));

            presses.add(ord, new KeyEvent(time, key, true));
            releases.add(ord, new KeyEvent(time, key, false));
        }

        final FixedQueueArray<KeyEvent>[] histories = (FixedQueueArray<KeyEvent>[]) new FixedQueueArray[2];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        return histories;
    }

    /**
     * <p>Creates the event histories for the {@code Mouse}. For press-release events, entries are made for two clicks
     * (four events: two presses and two releases). All events have the same creation time and return a position
     * of (0,0). Index 0 contains the press history, index 1 contains the release history, and index 2 contains the
     * scroll history.</p>
     *
     * @return event histories.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<MouseEvent>[] createDefaultMouseHistory()
    {
        final Button[] buttons = Button.values();
        final FixedQueueArray presses = new FixedQueueArray<>(buttons.length, BUTTON_GENS);
        final FixedQueueArray releases = new FixedQueueArray<>(buttons.length, BUTTON_GENS);
        final FixedQueueArray scrolls = new FixedQueueArray<>(buttons.length, BUTTON_GENS);

        final Point pt = new Point(0f, 0f, 0f);
        final long time = System.nanoTime();

        for (int i = 0; i < BUTTON_GENS; i++) {

            for (final Button button : buttons) {
                final int ord = button.ordinal();
                presses.add(ord, new MouseEvent(time, pt, button, true));
                releases.add(ord, new MouseEvent(time, pt, button, false));
            }

            scrolls.add(MOUSE_MIDDLE_ORD, NO_SCROLL_EVENT);
        }

        final FixedQueueArray<MouseEvent>[] histories = (FixedQueueArray<MouseEvent>[]) new FixedQueueArray[3];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        histories[SCROLL_HISTORY] = scrolls;
        return histories;
    }

    /**
     * <p>Creates the button history for the {@code Gamepad} of the given {@code Connection}. Entries are made for two
     * clicks (four events: two presses and two releases) and all events have the same creation time. Index 0
     * contains the press history and index 1 contains the release history.</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return button history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<PadEvent>[] createGamepadButtonHistory(Connection connection, PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final ButtonWrapper[] wrappers = (ButtonWrapper[]) profile.getButtonClass().getEnumConstants();
        final FixedQueueArray<PadEvent> presses = new FixedQueueArray<>(wrappers.length, BUTTON_GENS);
        final FixedQueueArray<PadEvent> releases = new FixedQueueArray<>(wrappers.length, BUTTON_GENS);
        final long time = System.nanoTime();

        for (final ButtonWrapper wrapper : wrappers) {
            final int ord = wrapper.button().ordinal();

            presses.add(ord, new PadEvent(time, connection, wrapper, true));
            releases.add(ord, new PadEvent(time, connection, wrapper, false));

            presses.add(ord, new PadEvent(time, connection, wrapper, true));
            releases.add(ord, new PadEvent(time, connection, wrapper, false));
        }

        final FixedQueueArray<PadEvent>[] histories = (FixedQueueArray<PadEvent>[]) new FixedQueueArray[2];
        histories[PRESS_HISTORY] = presses;
        histories[RELEASE_HISTORY] = releases;
        return histories;
    }

    /**
     * <p>Creates the motion history for the {@code Gamepad} of the given {@code Connection}. All entries report the
     * default resting position as returned by the {@code Gamepad}'s {@code PadProfile}.</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return axis history.
     */
    private FixedQueueArray<PadEvent> createGamepadAxisHistory(Connection connection, PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final Map<Axis, Float> resting = profile.getRestingAxisValues();
        final AxisWrapper[] axes = (AxisWrapper[]) profile.getAxisClass().getEnumConstants();
        final int axisCount = Axis.values().length;
        final FixedQueueArray<PadEvent> history = new FixedQueueArray<>(axisCount,  AXIS_GENS);
        final long time = System.nanoTime();

        // Write entries for events at resting positions
        for (final AxisWrapper wrapper : axes) {

            final float vertical = resting.get(wrapper.vertical());
            final Axis secondary = wrapper.horizontal();
            final float horizontal = (secondary != null) ? resting.get(secondary) : 0f;

            final int ord = wrapper.vertical().ordinal();
            history.add(ord, new PadEvent(time, connection, wrapper, new Point(horizontal, vertical, 0f)));
            history.add(ord, new PadEvent(time, connection, wrapper, new Point(horizontal, vertical, 0f)));
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

        final AxisWrapper[] axes = (AxisWrapper[]) profile.getAxisClass().getEnumConstants();
        final int axisCount = Axis.values().length;
        final FixedQueueArray<Boolean> history = new FixedQueueArray<>(axisCount, BUTTON_GENS);

        for (final AxisWrapper wrapper : axes) {
            final int ord = wrapper.vertical().ordinal();

            history.add(ord, true);
            history.add(ord, true);

            history.add(ord, true);
            history.add(ord, true);
        }

        return history;
    }

    /**
     * <p>Instantiates a {@code Gamepad} with event histories populated by false events and sets up the the internal
     * {@code PadData} wrapper. When this method returns, {@link Gamepad#isConnected()} will return true.</p>
     *
     * @param connection connection.
     * @param profile profile.
     * @return gamepad.
     */
    private Gamepad createGamepad(Connection connection, PadProfile profile)
    {
        assert (connection != null);
        assert (profile != null);

        final FixedQueueArray<PadEvent>[] buttons = createGamepadButtonHistory(connection, profile);
        final FixedQueueArray<PadEvent> axes = createGamepadAxisHistory(connection, profile);
        final FixedQueueArray<Boolean> zones = createGamepadDeadZoneHistory(profile);

        final State state = State.builder()
                .pressHistory(buttons[PRESS_HISTORY])
                .releaseHistory(buttons[RELEASE_HISTORY])
                .motionHistory(axes)
                .deadZoneHistory(zones)
                .build();

        final Gamepad gamepad = new Gamepad(connection, profile, state);

        state.setConnected(true);
        mPads.put(connection, new PadInfo(state, gamepad));

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

    /**
     * <p>Adds {@code event} to either {@code pressHistory} or {@code releaseHistory}, as appropriate.</p>
     *
     * @param button button.
     * @param event event.
     * @param pressHistory press history.
     * @param releaseHistory release history.
     * @param <T> type of button event.
     */
    private <T extends ButtonEvent> void addButtonEventToHistory(Enum button, T event,
                                                                 FixedQueueArray<T> pressHistory,
                                                                 FixedQueueArray<T> releaseHistory)
    {
        final int ord = button.ordinal();
        ((event.isPress()) ? pressHistory : releaseHistory).add(ord, event);
    }

    /**
     * <p>Checks if {@code button} is currently pressed.</p>
     *
     * @param button button.
     * @param info gamepad info.
     * @return true if the button is pressed.
     */
    private boolean isCurrentlyPressed(Enum button, PadInfo info)
    {
        final FixedQueueArray<PadEvent> presses = info.mState.getPressHistory();
        final FixedQueueArray<PadEvent> releases = info.mState.getReleaseHistory();

        return PressChecker.isPressed(button, presses, releases);
    }

    private float clampScroll(double offset)
    {
        return (float) Math.min(1d, Math.max(offset, -1d));
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
    private class PadInfo
    {
        private final int mExpectedButtonCount;
        private final int mExpectedAxisCount;

        private final ButtonWrapper[] mButtons;
        private final AxisWrapper[] mAxes;

        private final Map<Axis, Float> mRestOffsets;

        private final Gamepad mGamepad;
        private final State mState;

        private PadInfo(State state, Gamepad gamepad)
        {
            final PadProfile profile = gamepad.getProfile();
            mButtons = (ButtonWrapper[]) profile.getButtonClass().getEnumConstants();
            mAxes = (AxisWrapper[]) profile.getAxisClass().getEnumConstants();

            mRestOffsets = profile.getRestingAxisValues();

            mState = state;
            mGamepad = gamepad;

            mExpectedButtonCount = profile.getButtonCount();
            mExpectedAxisCount = profile.getAxisCount();
        }
    }

    private class ImplGamepadUpdateCallback implements GamepadUpdateCallback
    {
        @Override
        public void onButtonsUpdate(Connection connection, ByteBuffer buffer)
        {
            checkNull(connection);
            checkNull(buffer);

            final PadInfo info = mPads.get(connection);

            // Ignore unconnected gamepads
            if (info == null) {
                return;
            }

            // Gamepad wants silence
            if (info.mGamepad.isMuted()) {
                return;
            }

            // Check buffer's readable size
            if (buffer.limit() < info.mExpectedButtonCount) {
                final String format = "Buffer limit is too small, expected at least: %d, actual: %d";
                final int limit = buffer.limit();
                final int expected = info.mExpectedButtonCount;
                throw new IllegalArgumentException(String.format(format, expected, limit));
            }

            for (ButtonWrapper wrapper : info.mButtons) {

                final Gamepad.Button button = wrapper.button();
                final boolean pressed = (buffer.get(button.toInt()) == 0x1);

                // No state change means no event
                if (pressed != isCurrentlyPressed(button, info)) {

                    mBuffer.add(new PadEvent(System.nanoTime(), connection, wrapper, pressed));
                }
            }
        }

        @Override
        public void onAxesUpdate(Connection connection, FloatBuffer buffer)
        {
            checkNull(connection);
            checkNull(buffer);

            final PadInfo info = mPads.get(connection);

            // Ignore unconnected gamepads
            if (info == null) {
                return;
            }

            // Gamepad wants silence
            if (info.mGamepad.isMuted()) {
                return;
            }

            // Check buffer's readable size
            if (buffer.limit() < info.mExpectedAxisCount) {
                final String format = "Buffer limit is too small, expected: %d, actual %d";
                final int limit = buffer.limit();
                final int expected = info.mExpectedAxisCount;
                throw new IllegalArgumentException(String.format(format, expected, limit));
            }

            createMotionEvents(info, buffer);
        }

        /**
         * <p>Creates a {@code PadEvent} for each {@code AxisWrapper} constant whose new offsets are different
         * than previous. No event will be created if the new offset</p>
         *
         * @param info gamepad info.
         * @param buffer new offsets.
         */
        private void createMotionEvents(PadInfo info, FloatBuffer buffer)
        {
            final Gamepad gamepad = info.mGamepad;
            final FixedQueueArray<PadEvent> axisHistory = info.mState.getMotionHistory();
            final FixedQueueArray<Boolean> zoneHistory = info.mState.getDeadZoneHistory();

            // Check each axis for motion
            for (final AxisWrapper axis : info.mAxes) {

                final Axis vertical = axis.vertical();
                final Axis horizontal = axis.horizontal();
                final PadEvent lastEvent = axisHistory.get(0, vertical.ordinal());

                final float v;
                final float h;

                if (horizontal == null) {
                    // Convert trigger's value from -1/+1 range to 0/+1
                    v = (buffer.get(vertical.toInt()) + 1f) / 2f;
                    h = 0f;

                } else {
                    v = clampAxis(buffer.get(vertical.toInt()));
                    h = clampAxis(buffer.get(horizontal.toInt()));
                }

                // Only create event if offsets are different than previous
                if (lastEvent.getVertical() != v || lastEvent.getHorizontal() != h) {

                    final float restingY = info.mRestOffsets.get(axis.vertical());
                    final double zone = gamepad.getDeadZone(axis);
                    final int ord = axis.vertical().ordinal();

                    // Check if offsets are inside dead zone
                    final boolean inside = isInsideCircle(h, v, zone, restingY);

                    // Create event if either not in dead zone or just crossed into it
                    if (!inside || !zoneHistory.get(0, ord)) {

                        final long time = System.nanoTime();
                        final Connection connection = gamepad.getConnection();
                        final Point offsets = new Point(h, v, 0f);

                        mBuffer.add(new PadEvent(time, connection, axis, offsets));
                    }

                    // Cache dead zone presence
                    zoneHistory.add(ord, inside);
                }
            }
        }

        /**
         * <p>Creates a {@code Point} whose x and y components are the resting (x,y) for the given
         * {@code AxisWrapper}.</p>
         *
         * @param info gamepad info.
         * @param wrapper wrapper.
         * @return offsets at rest.
         */
        private Point createOffsetsAtRest(PadInfo info, AxisWrapper wrapper)
        {
            final PadProfile profile = info.mGamepad.getProfile();
            final Map<Axis, Float> values = profile.getRestingAxisValues();

            return new Point(0f, values.get(wrapper.vertical()), 0f);
        }

        /**
         * <p>Checks if the position at {@code x} and {@code y} is within the radius centered on the given vertical
         * offset. Horizontal offset is unneeded because all single-axis sensors returning a range of values are
         * treated as vertical-only.</p>
         *
         * @param x x.
         * @param y y.
         * @param radius radius.
         * @param offsetY center y offset.
         * @return true if position is inside the circle.
         */
        private boolean isInsideCircle(float x, float y, double radius, float offsetY)
        {
            // Radius is how much of possible dead zone (possible radius of 2)
            final float scaledRadius = (float) radius * (1f + Math.abs(offsetY));

            y -= offsetY;

            return (x * x) + (y * y) <= scaledRadius * scaledRadius;
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
         * <p>If this callback is notified with any {@code button} where {@link Button#from(int)} would return null, if
         * {@code action} is neither {@link GLFW#GLFW_PRESS} nor {@link GLFW#GLFW_RELEASE}, or if
         * {@link Mouse#isMuted()} returns {@code true}, then this callback does nothing.</p>
         * *
         * @param button button.
         * @param action action.
         * @param x x.
         * @param y y.
         */
        void onButtonUpdate(int button, int action, double x, double y);
    }

    /**
     * <p>This callback should be notified when a mouse scroll has occurred.</p>
     */
    public interface MouseScrollCallback
    {
        /**
         * <p>Called when the mouse wheel has been moved.</p>
         *
         * <p>The given scroll offsets will be clamped to the range -1/+1, inclusive. If {@link Mouse#isMuted()}
         * returns {@code true}, then this method does nothing.</p>
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
         * pressed state and 0x0 for release). These states should be indexed according to each {@code Gamepad.Button}'s
         * internal {@code int} value, as returned by {@link Button#toInt()}.</p>
         *
         * <p>If the given {@code Connection} does not refer to an available {@code Gamepad}, this method does
         * nothing.</p>
         *
         * @param connection connection.
         * @param buffer new button states.
         * @throws NullPointerException if connection or buffer is null.
         * @throws IllegalArgumentException if buffer limit {@literal <} the number of expected buttons.
         */
        void onButtonsUpdate(Connection connection, ByteBuffer buffer);

        /**
         * <p>Called when a gamepad's axis states need to be updated.</p>
         *
         * <p>Axis states should be given through the {@code buffer} with each value clamped between -1 and 1,
         * inclusive. These states should be indexed according to each {@code Gamepad.Axis}' internal {@code int}
         * value, as returned by {@link Axis#toInt()}. Events will only be created if the new offsets indicate
         * motion i.e. previous offsets != new offsets.</p>
         *
         * <p>If the given {@code Connection} does not refer to an an available {@code Gamepad} this method does
         * nothing. If an axis' offset value is within the associated {@link Gamepad}'s dead zone, the offset value
         * is treated as the axis' resting offset (typically 0 for analog sticks).</p>
         *
         * @param connection connection.
         * @param buffer new axis states.
         * @throws NullPointerException if connection or buffer is null.
         * @throws IllegalArgumentException if buffer limit {@literal <} the number of expected axes.
         */
        void onAxesUpdate(Connection connection, FloatBuffer buffer);
    }
}