package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.Axis;
import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;
import cinnamon.engine.event.Input.InputHistories;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.Table;

import java.util.*;

/**
 * <p>Implementation of {@code Controls} wrapping around an {@code EventSource} where each call to {@link #execute()}
 * empties the source of all waiting events while attempting to carry out set bindings.</p>
 *
 * <h3>Execution</h3>
 * <p>Each polled event will cause an examination of associated bindings and attempts to execute on them.</p>
 *
 * <p>All events returned by {@link EventSource#pollEvent()} must exist within the provided {@link InputHistories} in
 * the polled order. If an unrecorded event is polled, {@link #execute()}'s effects are undefined. This event instance
 * does not have to be the same as that in the corresponding history.</p>
 */
public final class InputControls implements Controls
{
    // Represents gamepad's in-use profile when no gamepad is actually available
    private static final PadProfile NULL_PROFILE;

    static
    {
        // Resting value has no meaning; needed for valid profile
        final Map<Axis, Float> resting = new HashMap<>();
        resting.put(Axis.AXIS_0, 0f);

        NULL_PROFILE = new PadProfile(XB1.Button.class, XB1.Stick.class, resting) { };
    }

    // Empty map representing lack of button bindings
    private static final Map<String, ButtonRule<?, PadEvent>> NULL_BUTTONS = new HashMap<>();

    // Empty map representing lack of axis bindings
    private static final Map<String, AxisRule<?, PadEvent>> NULL_AXES = new HashMap<>();

    // Executes keyboard bindings
    private final KeyboardHandler mKeyboard;

    // Executes mouse bindings
    private final MouseHandler mMouse;

    // Current keyboard binding
    private Map<String, ButtonRule<Key, KeyEvent>> mKeyboardMapping = new HashMap<>();

    // Current mouse button binding
    private Map<String, ButtonRule<Button, MouseEvent>> mMouseButtonMapping = new HashMap<>();

    // Current mouse axis binding
    private Map<String, AxisRule<Button, MouseEvent>> mMouseAxisMapping = new HashMap<>();

    // Available profiles read from Input during construction
    private final Map<String, PadProfile> mPadProfiles;

    // Current and all available bindings per gamepad
    private final Map<Connection, ConnectionEntry> mConnectionEntries = new EnumMap<>(Connection.class);

    // Provides events
    private final EventSource<InputEvent> mSource;

    // Routes events to each device's handler for executing on bindings
    private final InputEventVisitor mExecutionDispatch = new ExecutionEventVisitor();

    /**
     * <p>Constructs an {@code InputControls}.</p>
     *
     * @param input input devices.
     * @param source event source.
     * @param histories event histories.
     * @throws NullPointerException if input, source, or histories is null.
     */
    public InputControls(Input input, EventSource<InputEvent> source, InputHistories histories)
    {
        checkNull(input);
        checkNull(source);
        checkNull(histories);

        mSource = source;

        mKeyboard = createHandlerForKeyboard(histories);
        mMouse = createHandlerForMouse(histories);

        mPadProfiles = input.getGamepadProfiles();
        createMetadataEntriesPerConnection();

        associateEntriesWithConnectedGamepads(input, histories);
        watchForGamepadChanges(input, histories);
    }

    /**
     * <p>Empties the event source while attempting to execute the appropriate bindings.</p>
     *
     * <p><b>note</b> This method may take a longer than desirable time if the source has buffered too many events.</p>
     */
    public void execute()
    {
        InputEvent event;
        while ((event = mSource.pollEvent()) != null) {
            event.accept(mExecutionDispatch);
        }
    }

    @Override
    public Map<String, ButtonRule<Key, KeyEvent>> getKeyboardKeys()
    {
        return new HashMap<>(mKeyboardMapping);
    }

    @Override
    public void setKeyboardKeys(Map<String, ButtonRule<Key, KeyEvent>> bindings)
    {
        checkNull(bindings);

        // Set aside a copy to copy from during retrieval
        mKeyboardMapping = new HashMap<>(bindings);
        mKeyboard.setMappings(mKeyboardMapping);
    }

    @Override
    public Map<String, ButtonRule<Button, MouseEvent>> getMouseButtons()
    {
        return new HashMap<>(mMouseButtonMapping);
    }

    @Override
    public void setMouseButtons(Map<String, ButtonRule<Button, MouseEvent>> bindings)
    {
        checkNull(bindings);

        // Set aside a copy to copy from during retrieval
        mMouseButtonMapping = new HashMap<>(bindings);
        mMouse.mButtonHandler.setMappings(mMouseButtonMapping);
    }

    @Override
    public Map<String, AxisRule<Button, MouseEvent>> getMouseScrolls()
    {
        return new HashMap<>(mMouseAxisMapping);
    }

    @Override
    public void setMouseScrolls(Map<String, AxisRule<Button, MouseEvent>> bindings)
    {
        checkNull(bindings);

        // Check each rule if they map to middle mouse button
        for (final AxisRule<Button, MouseEvent> rule : bindings.values()) {
            final Button constant = rule.getConstants().get(0);

            if (constant != Button.MIDDLE) {
                final String format = "Scroll wheel rules must map to \"%s\", actual: %s";
                throw new IllegalArgumentException(String.format(format, Button.MIDDLE, constant));
            }
        }

        // Set aside a copy to copy from during retrieval
        mMouseAxisMapping = new HashMap<>(bindings);
        mMouse.mAxisHandler.setMappings(mMouseAxisMapping);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & ButtonWrapper> Map<String, ButtonRule<T, PadEvent>> getGamepadButtons
            (Connection connection, Class<T> cls)
    {
        checkNull(connection);
        checkNull(cls);

        final ConnectionEntry entry = mConnectionEntries.get(connection);
        final PadProfile profile = entry.mActiveProfile;

        checkGamepadConstantClasses(cls, profile.getButtonClass());

        return (Map) entry.mProfileButtonMaps.get(profile);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & ButtonWrapper> Map<String, ButtonRule<T, PadEvent>> getGamepadButtons
            (Connection connection, String profile, Class<T> cls)
    {
        checkNull(connection);
        checkNull(profile);
        checkNull(cls);

        final PadProfile padProfile = mPadProfiles.get(profile);

        if (padProfile == null) {
            final String format = "Gamepad profile \"%s\" is unrecognized";
            throw new NoSuchElementException(String.format(format, profile));
        }

        checkGamepadConstantClasses(cls, padProfile.getButtonClass());

        final ConnectionEntry entry = mConnectionEntries.get(connection);
        return (Map) entry.mProfileButtonMaps.get(padProfile);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & ButtonWrapper> void setGamepadButtons(Connection connection, String profile,
                                                                      Map<String, ButtonRule<T, PadEvent>> bindings)
    {
        checkNull(connection);
        checkNull(profile);
        checkNull(bindings);

        final PadProfile padProfile = mPadProfiles.get(profile);

        if (padProfile == null) {
            final String format = "Gamepad profile \"%s\" is unrecognized";
            throw new NoSuchElementException(String.format(format, profile));
        }

        Map bindingsCopy;

        // Check if button constant matches profile
        if (!bindings.isEmpty()) {
            final Class expected = padProfile.getButtonClass();
            final Class actual = bindings.values().iterator().next().getConstants().get(0).getDeclaringClass();
            checkGamepadConstantClasses(expected, actual);

            // Prevent direct edits
            bindingsCopy = new HashMap(bindings);
        } else {
            // Even though just empty, this guarantees implementation
            bindingsCopy = NULL_BUTTONS;
        }

        // Update bindings for the specified profile
        final ConnectionEntry entry = mConnectionEntries.get(connection);
        entry.mProfileButtonMaps.put(padProfile, bindingsCopy);

        // Replace active bindings if same profile
        if (entry.mActiveProfile.equals(padProfile)) {
            if (entry.mHandler != null) {
                entry.mHandler.mButtonCtrl.setMappings(bindings);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & AxisWrapper> Map<String, AxisRule<T, PadEvent>> getGamepadAxes
            (Connection connection, Class<T> cls)
    {
        checkNull(connection);
        checkNull(cls);

        final ConnectionEntry entry = mConnectionEntries.get(connection);
        final PadProfile profile = entry.mActiveProfile;

        checkGamepadConstantClasses(cls, profile.getAxisClass());

        return (Map) entry.mProfileAxisMaps.get(profile);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & AxisWrapper> Map<String, AxisRule<T, PadEvent>> getGamepadAxes(Connection connection,
                                                                                               String profile,
                                                                                               Class<T> cls)
    {
        checkNull(connection);
        checkNull(profile);
        checkNull(cls);

        final PadProfile padProfile = mPadProfiles.get(profile);

        if (padProfile == null) {
            final String format = "Gamepad profile \"%s\" is unrecognized";
            throw new NoSuchElementException(String.format(format, profile));
        }

        checkGamepadConstantClasses(cls, padProfile.getAxisClass());

        final ConnectionEntry entry = mConnectionEntries.get(connection);
        return (Map) entry.mProfileAxisMaps.get(entry.mActiveProfile);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & AxisWrapper> void setGamepadAxes(Connection connection, String profile,
                                                                 Map<String, AxisRule<T, PadEvent>> bindings)
    {
        checkNull(connection);
        checkNull(profile);
        checkNull(bindings);

        final PadProfile padProfile = mPadProfiles.get(profile);

        if (padProfile == null) {
            final String format = "Gamepad profile \"%s\" is unrecognized";
            throw new NoSuchElementException(String.format(format, profile));
        }

        Map bindingsCopy;

        // Check if button constant matches profile
        if (!bindings.isEmpty()) {
            final Class expected = padProfile.getAxisClass();
            final Class actual = bindings.values().iterator().next().getConstants().get(0).getDeclaringClass();
            checkGamepadConstantClasses(expected, actual);

            // Prevent direct edits
            bindingsCopy = new HashMap(bindings);
        } else {
            // Even though just empty, this guarantees implementation
            bindingsCopy = NULL_AXES;
        }

        // Update bindings for the specified profile
        final ConnectionEntry entry = mConnectionEntries.get(connection);
        entry.mProfileAxisMaps.put(padProfile, bindingsCopy);

        // Replace active bindings if same profile
        if (entry.mActiveProfile.equals(padProfile)) {
            if (entry.mHandler != null) {
                entry.mHandler.mAxisCtrl.setMappings(bindings);
            }
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private KeyboardHandler createHandlerForKeyboard(InputHistories histories)
    {
        final Table<KeyEvent>[] keyboardHistories = histories.getKeyboardHistory();

        return new KeyboardHandler(keyboardHistories[0], keyboardHistories[1]);
    }

    private MouseHandler createHandlerForMouse(InputHistories histories)
    {
        final Table<MouseEvent>[] mouseButtonHistories = histories.getMouseButtonHistory();
        final Table<MouseEvent> mouseAxisHistory = histories.getMouseScrollHistory();

        return new MouseHandler(mouseButtonHistories[0], mouseButtonHistories[1], mouseAxisHistory);
    }

    /**
     * <p>Creates a metadata entry for each supported gamepad connection. Each entry will be initialized with empty
     * button and axis bindings. Active bindings are also empty.</p>
     */
    @SuppressWarnings("unchecked")
    private void createMetadataEntriesPerConnection()
    {
        final Iterable<PadProfile> profiles = mPadProfiles.values();

        for (final Connection connection : Connection.values()) {
            final ConnectionEntry entry = new ConnectionEntry();
            entry.mActiveProfile = NULL_PROFILE;

            // Init each supported profile to an empty map
            for (final PadProfile profile : profiles) {
                entry.mProfileButtonMaps.put(profile, NULL_BUTTONS);
                entry.mProfileAxisMaps.put(profile, NULL_AXES);
            }

            // Empty profile points to empty bindings for when user submits an empty map
            entry.mProfileButtonMaps.put(NULL_PROFILE, NULL_BUTTONS);
            entry.mProfileAxisMaps.put(NULL_PROFILE, NULL_AXES);

            mConnectionEntries.put(connection, entry);
        }
    }

    private void associateEntriesWithConnectedGamepads(Input input, InputHistories histories)
    {
        for (final Gamepad gamepad : input.getGamepads().values()) {
            associateEntryWithGamepad(gamepad, histories);
        }
    }

    @SuppressWarnings("unchecked")
    private void watchForGamepadChanges(Input input, InputHistories histories)
    {
        // Add new profile to each gamepad
        input.addOnGamepadProfileAddListener((name, profile) ->
        {
            for (final Connection connection : Connection.values()) {

                mPadProfiles.put(name, profile);

                final ConnectionEntry entry = mConnectionEntries.get(connection);
                entry.mProfileButtonMaps.put(profile, NULL_BUTTONS);
                entry.mProfileAxisMaps.put(profile, NULL_AXES);
            }
        });

        input.addGamepadOnConnectionChangeListener((gamepad) ->
        {
            if (gamepad.isConnected()) {
                associateEntryWithGamepad(gamepad, histories);
            } else {
                disassociateEntryFromGamepad(gamepad.getConnection());
            }
        });
    }

    /**
     * <p>Selects the gamepad's profile as the in-use profile and creates a gamepad handler with the in-use profile's
     * associated bindings.</p>
     *
     * @param gamepad gamepad.
     * @param histories event histories.
     */
    @SuppressWarnings("unchecked")
    private void associateEntryWithGamepad(Gamepad gamepad, InputHistories histories)
    {
        final Connection connection = gamepad.getConnection();
        final Table<PadEvent>[] buttonHistory = histories.getGamepadButtonHistory(connection);
        final Table<PadEvent> axisHistory = histories.getGamepadMotionHistory(connection);

        final ConnectionEntry entry = mConnectionEntries.get(connection);
        final PadProfile profile = gamepad.getProfile();

        entry.mActiveProfile = profile;
        entry.mHandler = new GamepadHandler(buttonHistory[0], buttonHistory[1], axisHistory);

        // Set bindings according to profile
        entry.mHandler.mButtonCtrl.setMappings((Map) entry.mProfileButtonMaps.get(profile));
        entry.mHandler.mAxisCtrl.setMappings((Map) entry.mProfileAxisMaps.get(profile));
    }

    /**
     * <p>Resets the connection entry's in-use profile and discards the entry's associated gamepad handler.</p>
     *
     * @param connection connection.
     */
    private void disassociateEntryFromGamepad(Connection connection)
    {
        final ConnectionEntry entry = mConnectionEntries.get(connection);
        entry.mActiveProfile = NULL_PROFILE;
        entry.mHandler = null;
    }

    private void checkGamepadConstantClasses(Class expected, Class actual)
    {
        if (actual != expected) {
            final String format = "Gamepad is configured to use \"%s\" constants but was given \"%s\"";
            throw new IllegalArgumentException(String.format(format, actual.getSimpleName(), expected.getSimpleName()));
        }
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Carries a connection's in-use profile, available bindings, and the bindings' executor.</p>
     */
    private class ConnectionEntry
    {
        // Available axis bindings per profile
        private final Map mProfileButtonMaps = new HashMap<>();

        // Available axis bindings per profile
        private final Map mProfileAxisMaps = new HashMap<>();

        // Profile currently being used
        private PadProfile mActiveProfile;

        // Executes on current bindings
        private GamepadHandler mHandler;
    }

    /**
     * <p>Routes events to the correct handlers.</p>
     */
    private class ExecutionEventVisitor implements InputEventVisitor
    {
        @Override
        public void visit(KeyEvent event)
        {
            mKeyboard.submit(event);
        }

        @Override
        public void visit(MouseEvent event)
        {
            ((event.isScroll()) ? mMouse.mAxisHandler : mMouse.mButtonHandler).submit(event);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void visit(PadEvent event)
        {
            final GamepadHandler ctrl = mConnectionEntries.get(event.getSource()).mHandler;
            ((event.isButton()) ? ctrl.mButtonCtrl : ctrl.mAxisCtrl).submit(event);
        }
    }

    private class KeyboardHandler extends ButtonHandler<Key, Key, KeyEvent>
    {
        KeyboardHandler(Table<KeyEvent> pressHistory, Table<KeyEvent> releaseHistory)
        {
            super(Key.class, pressHistory, releaseHistory);
        }

        @Override
        protected Key extractConstantFrom(KeyEvent event)
        {
            return event.getKey();
        }
    }

    private class MouseHandler
    {
        // Executes on button events
        private final ButtonHandler<Button, Button, MouseEvent> mButtonHandler;

        // Executes on axis events
        private final AxisHandler<Button, Button, MouseEvent> mAxisHandler;

        private MouseHandler(Table<MouseEvent> pressHistory, Table<MouseEvent> releaseHistory,
                            Table<MouseEvent> axisHistory)
        {
            mButtonHandler = new ButtonHandler<>(Button.class, pressHistory, releaseHistory)
            {
                @Override
                protected Button extractConstantFrom(MouseEvent event)
                {
                    return event.getButton();
                }
            };

            mAxisHandler = new AxisHandler<>(Button.class, axisHistory)
            {
                @Override
                protected Button extractConstantFrom(MouseEvent event)
                {
                    return event.getButton();
                }
            };
        }
    }

    private class GamepadHandler<ButtonType extends Enum<ButtonType> & ButtonWrapper,
            AxisType extends Enum<AxisType> & AxisWrapper>
    {
        // Executes on button events
        private final ButtonHandler<ButtonType, Gamepad.Button, PadEvent> mButtonCtrl;

        // Executes on axis events
        private final AxisHandler<AxisType, Gamepad.Axis, PadEvent> mAxisCtrl;

        GamepadHandler(Table<PadEvent> presses, Table<PadEvent> releases, Table<PadEvent> axisHistory)
        {
            mButtonCtrl = new ButtonHandler<>(Gamepad.Button.class, presses, releases)
            {
                @Override
                protected Gamepad.Button extractConstantFrom(PadEvent event)
                {
                    return event.getButton().button();
                }

                @Override
                protected Enum[] unwrap(List<? extends Enum> constants)
                {
                    final Gamepad.Button[] buttons = new Gamepad.Button[constants.size()];

                    // Return the raw buttons
                    for (int i = 0; i < buttons.length; i++) {
                        buttons[i] = ((ButtonWrapper) constants.get(i)).button();
                    }

                    return buttons;
                }
            };

            mAxisCtrl = new AxisHandler<>(Gamepad.Axis.class, axisHistory)
            {
                @Override
                protected Axis extractConstantFrom(PadEvent event)
                {
                    return event.getAxis().vertical();
                }

                @Override
                protected Enum[] unwrap(List<? extends Enum> constants)
                {
                    final Gamepad.Axis[] axes = new Gamepad.Axis[constants.size()];

                    // Return the raw axes
                    for (int i = 0; i < axes.length; i++) {
                        axes[i] = ((AxisWrapper) constants.get(i)).vertical();
                    }

                    return axes;
                }
            };
        }
    }
}