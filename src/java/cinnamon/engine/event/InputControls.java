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
    // Attempts to execute keyboard mappings from an event
    private final KeyboardHandler mKeyboard;

    // Attempts to execute mouse mappings from an event
    private final MouseHandler mMouse;

    // Attempts to execute gamepad mappings from an event
    private final Map<Connection, GamepadHandler> mPadControls = new EnumMap<>(Connection.class);

    // Latest set keyboard mapping
    private Map<String, ButtonRule<Key, KeyEvent>> mKeyboardMapping = new HashMap<>();

    // Latest set mouse button mapping
    private Map<String, ButtonRule<Button, MouseEvent>> mMouseButtonMapping = new HashMap<>();

    // Latest set mouse scroll mapping
    private Map<String, AxisRule<Button, MouseEvent>> mMouseAxisMapping = new HashMap<>();

    // Latest set button and axis mappings per gamepad
    private final Map<Connection, PadInfo> mPadMeta = new EnumMap<>(Connection.class);

    // Releases events to trigger a mapping
    private final EventSource<InputEvent> mSource;

    // Routes events to each device's handler for triggering mappings
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

        createHandlersForConnectedGamepads(input, histories);
        watchInputForGamepadChanges(input, histories);
    }

    /**
     * <p>Empties the event source while attempting to execute the appropriate mappings.</p>
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
    public void setKeyboardKeys(Map<String, ButtonRule<Key, KeyEvent>> mappings)
    {
        checkNull(mappings);

        // Set aside a copy to copy from during retrieval
        mKeyboardMapping = new HashMap<>(mappings);
        mKeyboard.setMappings(mKeyboardMapping);
    }

    @Override
    public Map<String, ButtonRule<Button, MouseEvent>> getMouseButtons()
    {
        return new HashMap<>(mMouseButtonMapping);
    }

    @Override
    public void setMouseButtons(Map<String, ButtonRule<Button, MouseEvent>> mappings)
    {
        checkNull(mappings);

        // Set aside a copy to copy from during retrieval
        mMouseButtonMapping = new HashMap<>(mappings);
        mMouse.mButtonHandler.setMappings(mMouseButtonMapping);
    }

    @Override
    public Map<String, AxisRule<Button, MouseEvent>> getMouseScrolls()
    {
        return new HashMap<>(mMouseAxisMapping);
    }

    @Override
    public void setMouseScrolls(Map<String, AxisRule<Button, MouseEvent>> mappings)
    {
        checkNull(mappings);

        // Check each rule if they map to middle mouse button
        for (final AxisRule<Button, MouseEvent> rule : mappings.values()) {
            final Button constant = rule.getConstants().get(0);

            if (constant != Button.MIDDLE) {
                final String format = "Scroll wheel rules must map to \"%s\", actual: %s";
                throw new IllegalArgumentException(String.format(format, Button.MIDDLE, constant));
            }
        }

        // Set aside a copy to copy from during retrieval
        mMouseAxisMapping = new HashMap<>(mappings);
        mMouse.mAxisHandler.setMappings(mMouseAxisMapping);
    }

    @Override
    public <T extends Enum<T> & ButtonWrapper> Map<String, ButtonRule<T, PadEvent>> getGamepadButtons
            (Connection connection, Class<T> cls)
    {
        checkNull(connection);
        checkNull(cls);

        checkGamepadAvailability(connection, mPadControls.get(connection));

        final PadInfo info = mPadMeta.get(connection);
        checkGamepadConstantClasses(info.mExpectedButtonClass, cls);

        @SuppressWarnings("unchecked")
        final Map<String, ButtonRule<T, PadEvent>> mappings = info.mButtonMappings;
        return new HashMap<>(mappings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & ButtonWrapper> void setGamepadButtons(Connection connection,
                                                                      Map<String, ButtonRule<T, PadEvent>> mappings)
    {
        checkNull(connection);
        checkNull(mappings);

        final GamepadHandler ctrl = mPadControls.get(connection);
        checkGamepadAvailability(connection, ctrl);

        // Check if gamepad button constant is as expected
        if (!mappings.isEmpty()) {
            final Class expected = mPadMeta.get(connection).mExpectedButtonClass;
            final Class actual = mappings.values().iterator().next().getConstants().get(0).getDeclaringClass();

            checkGamepadConstantClasses(expected, actual);
        }

        // Set aside a copy to copy from during retrieval
        final PadInfo info = mPadMeta.get(connection);
        info.mButtonMappings = new HashMap<>(mappings);

        ctrl.mButtonCtrl.setMappings(info.mButtonMappings);
    }

    @Override
    public <T extends Enum<T> & AxisWrapper> Map<String, AxisRule<T, PadEvent>> getGamepadAxes
            (Connection connection, Class<T> cls)
    {
        checkNull(connection);
        checkNull(cls);

        checkGamepadAvailability(connection, mPadControls.get(connection));

        final PadInfo info = mPadMeta.get(connection);
        checkGamepadConstantClasses(info.mExpectedAxisClass, cls);

        @SuppressWarnings("unchecked")
        final Map<String, AxisRule<T, PadEvent>> mappings = info.mAxisMappings;
        return new HashMap<>(mappings);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Enum<T> & AxisWrapper> void setGamepadAxes(Connection connection,
                                                                 Map<String, AxisRule<T, PadEvent>> mappings)
    {
        checkNull(connection);
        checkNull(mappings);

        final GamepadHandler ctrl = mPadControls.get(connection);
        checkGamepadAvailability(connection, ctrl);

        // Check if gamepad axis constant is as expected
        if (!mappings.isEmpty()) {
            final Class expected = mPadMeta.get(connection).mExpectedAxisClass;
            final Class actual = mappings.values().iterator().next().getConstants().get(0).getDeclaringClass();

            checkGamepadConstantClasses(expected, actual);
        }

        // Set aside a copy to copy from during retrieval
        final PadInfo info = mPadMeta.get(connection);
        info.mAxisMappings = new HashMap<>(mappings);

        ctrl.mAxisCtrl.setMappings(info.mAxisMappings);
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

    private void createHandlersForConnectedGamepads(Input input, InputHistories histories)
    {
        for (final Gamepad gamepad : input.getGamepads().values()) {
            createGamepadHandler(gamepad, histories);
        }
    }

    private void watchInputForGamepadChanges(Input input, InputHistories histories)
    {
        input.addGamepadOnConnectionChangeListener((gamepad) ->
        {
            if (gamepad.isConnected()) {
                createGamepadHandler(gamepad, histories);
            } else {
                destroyGamepadHandler(gamepad);
            }
        });
    }

    /**
     * <p>Creates a handler for the given gamepad and sets aside its desired button and axis classes. The gamepad
     * will have no set button or axis mappings.</p>
     *
     * @param gamepad gamepad.
     * @param histories event histories.
     */
    private void createGamepadHandler(Gamepad gamepad, InputHistories histories)
    {
        final Connection connection = gamepad.getConnection();
        final Table<PadEvent>[] buttonHistory = histories.getGamepadButtonHistory(connection);
        final Table<PadEvent> axisHistory = histories.getGamepadMotionHistory(connection);

        mPadControls.put(connection, new GamepadHandler(buttonHistory[0], buttonHistory[1], axisHistory));

        // Create meta data
        final PadProfile profile = gamepad.getProfile();
        final PadInfo info = new PadInfo();

        info.mExpectedButtonClass = profile.getButtonClass();
        info.mExpectedAxisClass = profile.getAxisClass();
        info.mButtonMappings = new HashMap<>();
        info.mAxisMappings = new HashMap<>();

        mPadMeta.put(connection, info);
    }

    /**
     * <p>Destroys the handler for the given gamepad.</p>
     *
     * @param gamepad gamepad.
     */
    private void destroyGamepadHandler(Gamepad gamepad)
    {
        final Connection connection = gamepad.getConnection();
        mPadMeta.remove(connection);
        mPadControls.remove(connection);
    }

    private void checkGamepadAvailability(Connection connection, GamepadHandler ctrl)
    {
        if (ctrl == null) {
            throw new IllegalStateException("No available gamepad for " + connection);
        }
    }

    private void checkGamepadConstantClasses(Class expected, Class actual)
    {
        if (expected != actual) {
            final String format = "Gamepad is configured to use \"%s\" constants but was given \"%s\"";
            throw new IllegalArgumentException(String.format(format, expected.getSimpleName(), actual.getSimpleName()));
        }
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Carries a gamepad's expected button and axis classes as well as its button and axis mappings.</p>
     *
     * @param <ButtonType> expected button.
     * @param <AxisType> expected axis.
     */
    private class PadInfo<
            ButtonType extends Enum<ButtonType> & ButtonWrapper,
            AxisType extends Enum<AxisType> & AxisWrapper
            >
    {
        private Map<String, ButtonRule<ButtonType, PadEvent>> mButtonMappings;
        private Map<String, AxisRule<AxisType, PadEvent>> mAxisMappings;

        private Class<ButtonType> mExpectedButtonClass;
        private Class<AxisType> mExpectedAxisClass;
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
            final GamepadHandler ctrl = mPadControls.get(event.getSource());
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
            mButtonHandler = new ButtonHandler<Button, Button, MouseEvent>(Button.class, pressHistory, releaseHistory)
            {
                @Override
                protected Button extractConstantFrom(MouseEvent event)
                {
                    return event.getButton();
                }
            };

            mAxisHandler = new AxisHandler<Button, Button, MouseEvent>(Button.class, axisHistory)
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
            mButtonCtrl = new ButtonHandler<ButtonType, Gamepad.Button, PadEvent>(Gamepad.Button.class, presses,
                    releases)
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

            mAxisCtrl = new AxisHandler<AxisType, Gamepad.Axis, PadEvent>(Gamepad.Axis.class, axisHistory)
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