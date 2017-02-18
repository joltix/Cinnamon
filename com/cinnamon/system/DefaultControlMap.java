package com.cinnamon.system;

import com.cinnamon.utils.PooledQueue;

import java.util.EnumMap;

/**
 * <p>
 *     This {@link ControlMap} implementation assumes one {@link KeyEvent} and one {@link MouseEvent} is
 *     submitted per game tick. Furthermore, only one {@link InputEvent.Action} is allowed to be handled per hardware
 *     constant, whether {@link KeyEvent.Key} or {@link MouseEvent.Button}.
 * </p>
 */
public class DefaultControlMap implements ControlMap
{
    // Represents no action mapped to key
    private final KeyEventHandler NULL_KEY = new KeyAction();

    // Represents no action mapped to mouse
    private final MouseEventHandler NULL_MOUSE = new MouseAction();

    // Object pool for repeating key events
    private final PooledQueue<KeyEvent> mKeyRepeatPool = new PooledQueue<KeyEvent>();

    // Object pool for repeating mouse events
    private final PooledQueue<MouseEvent> mMouseRepeatPool = new PooledQueue<MouseEvent>();

    // Keyboard state mappings
    private final EnumMap<KeyEvent.Key, KeyState> mKeyStates;

    // Mouse state mappings
    private final EnumMap<MouseEvent.Button, MouseState> mMouseStates;

    // Most recently added Key event (non repeating)
    private KeyEvent mRecentKey;

    // Most recently added Mouse event (non repeating)
    private MouseEvent mRecentMouse;

    /**
     * <p>Constructor for a DefaultControlMap.</p>
     */
    public DefaultControlMap()
    {
        // Create hardware state maps
        mKeyStates = new EnumMap<KeyEvent.Key, KeyState>(KeyEvent.Key.class);
        mMouseStates = new EnumMap<MouseEvent.Button, MouseState>(MouseEvent.Button.class);

        // Setup keyboard state objects
        for (KeyEvent.Key key : KeyEvent.Key.values()) {
            mKeyStates.put(key, new KeyState(NULL_KEY, key));
        }

        // Setup mouse state objects
        for (MouseEvent.Button button : MouseEvent.Button.values()) {
            mMouseStates.put(button, new MouseState(NULL_MOUSE, button));
        }
    }

    @Override
    public void addEvent(KeyEvent event)
    {
        // Mark event as latest and store event in state
        mRecentKey = event;

        if (event != null && !event.isRepeat()) {
            mKeyStates.get(event.getKey()).update(event);
        }
    }

    @Override
    public void addEvent(MouseEvent event)
    {
        // Mark event as latest and store event in state
        mRecentMouse = event;

        if (event != null && !event.isRepeat()) {
            mMouseStates.get(event.getButton()).update(event);
        }
    }

    @Override
    public void setMode(KeyEvent.Key key, boolean fireOnPress, boolean repeatable)
    {
        final KeyState state = mKeyStates.get(key);
        state.mFireOnPress = fireOnPress;
        state.mRepeat = repeatable;
    }

    @Override
    public void setMode(MouseEvent.Button button, boolean fireOnPress, boolean repeatable)
    {
        final MouseState state = mMouseStates.get(button);
        state.mFireOnPress = fireOnPress;
        state.mRepeat = repeatable;
    }

    @Override
    public void fire()
    {
        fireKeys();
        fireButtons();
    }

    /**
     * <p>Fires all keys which have their firing conditions satisfied and have not yet fired their most recently
     * added {@link KeyEvent}.</p>
     */
    private void fireKeys()
    {
        for (final KeyEvent.Key key : mKeyStates.keySet()) {
            final KeyState state = mKeyStates.get(key);
            final KeyEvent event = state.mRecentEvent;

            // Can't do anything with a key without any Event
            if (event == null) {
                continue;
            }

            // Check if firing conditions are met
            if (allowedToFire(state)) {

                // Process event
                state.mHandler.handle(event);
                state.mHasFired = true;
            } else if (state.mRepeat && event.isPress()) {
                // Generate repeat event if conditions allow and repeat permitted
                state.mHandler.handle(repeat(event));
            }
        }
    }

    /**
     * <p>Fires all buttons which have their firing conditions satisfied and have not yet fired their most recently
     * added {@link MouseEvent}.</p>
     */
    private void fireButtons()
    {
        for (final MouseEvent.Button button : mMouseStates.keySet()) {
            final MouseState state = mMouseStates.get(button);
            final MouseEvent event = state.mRecentEvent;

            // Can't do anything with a key without any Event
            if (event == null) {
                continue;
            }

            // Check if firing conditions are met
            if (allowedToFire(state)) {

                // Process event
                state.mHandler.handle(event);
                state.mHasFired = true;
            } else if (state.mRepeat && event.isPress()) {
                // Generate repeat event if conditions allow and repeat permitted
                state.mHandler.handle(repeat(event));
            }
        }
    }

    /**
     * <p>Checks whether or not a {@link State}'s firing conditions have been satisfied and its most recent
     * {@link KeyEvent} or {@link MouseEvent} is ready to be fired.</p>
     *
     * <p>This method must only be called when the State's most recent event is not null.</p>
     *
     * @param state KeyEvent or MouseEvent.
     * @return true to allow the State's InputEvent to be fired.
     */
    private boolean allowedToFire(State state)
    {
        assert (state.mRecentEvent != null);

        // Can't fire if Event was already fired
        if (state.mHasFired) {
            return false;
        }

        // Decide firing permission depending on InputEvent.Action
        final InputEvent.Action action = state.mRecentEvent.getAction();
        switch (action) {
            case PRESS:
                // Only fire if firing mode wants firing on press
                return state.mFireOnPress;

            case RELEASE:
                // Fire if firing mode desires firing on release and hasn't fired yet
                return !state.mFireOnPress;

            case SCROLL_FORWARD:
                return true;

            case SCROLL_BACKWARD:
                return true;

            default: throw new IllegalStateException("Unrecognized input action: " + action);
        }
    }

    /**
     * Gets a {@link InputEvent.Action#REPEAT} {@link KeyEvent}.
     *
     * @param event KeyEvent to repeat.
     * @return repeated KeyEvent.
     */
    private KeyEvent repeat(KeyEvent event)
    {
        KeyEvent repeat = mKeyRepeatPool.poll();
        if (repeat == null) {
            repeat = new KeyEvent(event);
        } else {
            repeat.update(event.getKey(), event.getAction());
        }

        return repeat;
    }

    /**
     * Gets a {@link InputEvent.Action#REPEAT} {@link MouseEvent}.
     *
     * @param event MouseEvent to repeat.
     * @return repeated MouseEvent.
     */
    private MouseEvent repeat(MouseEvent event)
    {
        MouseEvent repeat = mMouseRepeatPool.poll();
        if (repeat == null) {
            repeat = new MouseEvent(event);
        } else {
            repeat.update(event.getButton(), event.getAction(), event.getX(), event.getY());
        }

        return repeat;
    }

    @Override
    public KeyEventHandler attach(KeyEvent.Key key, KeyEventHandler handler)
    {
        return attach(key, handler, mKeyStates, NULL_KEY);
    }

    @Override
    public MouseEventHandler attach(MouseEvent.Button button, MouseEventHandler handler)
    {
        return attach(button, handler, mMouseStates, NULL_MOUSE);
    }

    /**
     * <p>Attaches an {@link EventHandler} to a hardware constant in preparation for later event firing.</p>
     *
     * @param constant {@link KeyEvent.Key} or {@link MouseEvent.Button}.
     * @param handler {@link KeyEventHandler} or {@link MouseEventHandler}.
     * @param states map of {@link KeyState}s or {@link MouseState}s.
     * @param nullConstant {@link #NULL_KEY} or {@link #NULL_MOUSE}.
     * @param <A> {@link Event} type.
     * @param <B> hardware constant.
     * @param <C> EventHandler.
     * @param <D> either {@link KeyState} or {@link MouseState}.
     * @param <E> map of D {@link State}s to hardware constants.
     * @return
     */
    private <A extends InputEvent, B extends Enum<B>, C extends EventHandler<A>, D extends State<A, C>,
        E extends EnumMap<B, D>> C attach(B constant, C handler, E states, C nullConstant)
    {
        final D state = states.get(constant);

        // Replace old handler with new
        final C oldHandler = state.mHandler;
        state.mHandler = handler;

        // Return null instead of null handler obj
        return (oldHandler == nullConstant) ? null : oldHandler;
    }

    @Override
    public KeyEventHandler detach(KeyEvent.Key key)
    {
        return attach(key, NULL_KEY);
    }

    @Override
    public MouseEventHandler detach(MouseEvent.Button button)
    {
        return attach(button, NULL_MOUSE);
    }

    @Override
    public KeyEvent getKey()
    {
        return mRecentKey;
    }

    @Override
    public MouseEvent getMouse()
    {
        return mRecentMouse;
    }

    /**
     * <p>
     *     Wrapper for {@link KeyEvent} type {@link State}.
     * </p>
     */
    private class KeyState extends State<KeyEvent, KeyEventHandler>
    {
        // Keyboard key description
        private KeyEvent.Key mKey;

        /**
         * <p>Constructor for a KeyState.</p>
         *
         * @param handler {@link KeyEventHandler} to fire.
         * @param key {@link KeyEvent.Key} description.
         */
        private KeyState(KeyEventHandler handler, KeyEvent.Key key)
        {
            super(handler);
            mKey = key;
        }
    }

    /**
     * <p>
     *     Wrapper for {@link MouseEvent} type {@link State}.
     * </p>
     */
    private class MouseState extends State<MouseEvent, MouseEventHandler>
    {
        // Mouse button description
        private MouseEvent.Button mButton;

        /**
         * <p>Constructor for a MouseState.</p>
         *
         * @param handler {@link MouseEventHandler} to fire.
         * @param button {@link MouseEvent.Button} description.
         */
        private MouseState(MouseEventHandler handler, MouseEvent.Button button)
        {
            super(handler);
            mButton = button;
        }
    }

    /**
     * <p>
     *     Tracks the last {@link Event} of an input and references the {@link EventHandler} set to fire when needed.
     * </p>
     *
     * @param <E> {@link KeyEvent} or {@link MouseEvent}
     */
    private abstract class State<E extends InputEvent, U extends EventHandler<E>>
    {
        // User-installed Action
        protected U mHandler;

        // Last Event for the State
        protected E mRecentEvent;
        protected E mPrevEvent;

        // Whether or not to fire on PRESS Events
        protected boolean mFireOnPress = true;

        // Whether or not the State's most recent Event has already fired
        protected boolean mHasFired = false;

        // Whether or not to repeat PRESS Events
        protected boolean mRepeat = true;

        /**
         * <p>Constructor for a State.</p>
         *
         * @param handler EventHandler to fire.
         */
        private State(U handler)
        {
            mHandler = handler;
        }

        protected void update(E event)
        {
            // Move old Event back and set new recent Event
            mPrevEvent = mRecentEvent;
            mRecentEvent = event;

            // Reset fired status for new recent Event
            mHasFired = false;
        }
    }

    /**
     * <p>
     *     Wrapper for a {@link KeyEventHandler} that does nothing. This is meant to act as <i>null</i> for
     *     {@link KeyEvent}s.
     * </p>
     */
    private class KeyAction implements KeyEventHandler
    {
        @Override
        public void handle(KeyEvent event)
        {

        }
    }

    /**
     * <p>
     *     Wrapper for a {@link MouseEventHandler} that does nothing. This is meant to act as <i>null</i> for
     *     {@link MouseEvent}s.
     * </p>
     */
    private class MouseAction implements MouseEventHandler
    {
        @Override
        public void handle(MouseEvent event)
        {

        }
    }
}
