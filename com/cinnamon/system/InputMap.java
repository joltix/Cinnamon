package com.cinnamon.system;

import com.cinnamon.utils.Event;
import com.cinnamon.utils.KeyEvent;
import com.cinnamon.utils.MouseEvent;
import com.cinnamon.utils.PooledQueue;

/**
 * <p>
 *     Associates {@link Action}s to keys or mouse buttons and executes them as
 *     needed. InputMap allows instructions to run as soon as needed by
 *     providing the related input {@link Event} and signalling the need with
 *     {@link #executeActions()}. Actions are to be attached for execution
 *     through methods like {@link #attachKey(int, Action)}.</p>
 * <p>
 *
 *
 */
public class InputMap implements ControlMap
{
    // Represents no action mapped to key
    private final ControlMap.Action<KeyEvent> NULL_KEY = new KeyAction();

    // Represents no action mapped to mouse
    private final ControlMap.Action<MouseEvent> NULL_MOUSE = new MouseAction();


    // Object pool for repeating key events
    private final PooledQueue<KeyEvent> mKeyRepeatPool = new
            PooledQueue<KeyEvent>();

    // Object pool for repeating mouse events
    private final PooledQueue<MouseEvent> mMouseRepeatPool = new
            PooledQueue<MouseEvent>();


    // Keyboard mappings
    private KeyState[] mKeyStates = new KeyState[KeyEvent.KEY_COUNT];

    // Mouse mappings
    private MouseState[] mMouseStates = new MouseState[MouseEvent.BUTTON_COUNT];

    // Most recently added Key event (non repeating)
    private KeyEvent mRecentKey;

    // Most recently added Mouse event (non repeating)
    private MouseEvent mRecentMouse;

    /**
     * <p>Constructor for an empty InputMap.</p>
     */
    public InputMap()
    {
        for (int i = 0; i < mKeyStates.length; i++) {
            mKeyStates[i] = new KeyState(NULL_KEY, NULL_KEY, null);
        }

        for (int i = 0; i < mMouseStates.length; i++) {
            mMouseStates[i] = new MouseState(NULL_MOUSE, NULL_MOUSE, null);
        }
    }

    @Override
    public void setEvent(KeyEvent event)
    {
        // Mark event as latest and store event in state
        mRecentKey = event;

        if (event != null && !event.isAction(Event.Action.REPEAT)) {
            mKeyStates[event.getKey()].setEvent(event);
        }
    }

    @Override
    public void setEvent(MouseEvent event)
    {
        // Mark event as latest and store event in state
        mRecentMouse = event;

        if (event != null && !event.isAction(Event.Action.REPEAT)) {
            mMouseStates[event.getButton()].setEvent(event);
        }
    }

    /**
     * <p>Execute {@link Action} associated with the most recent
     * {@link Event}s as well as keyboard keys in a state of
     * {@link Event.Action#REPEAT}.</p>
     */
    @Override
    public void executeActions()
    {
        int skipKey = -1;
        if (mRecentKey != null && mRecentKey.isAction(Event.Action.PRESS)) {

            // Run system installed action first, then client
            final KeyState state = mKeyStates[mRecentKey.getKey()];
            state.getSystemAction().execute(mRecentKey);
            state.getAction().execute(mRecentKey);

            // Mark recent key index to prevent repeating
            skipKey = state.getKey();
        }

        // Execute repeating keys
        executeRepeatingKeys(skipKey);

        if (mRecentMouse != null && mRecentMouse.isAction(Event.Action.RELEASE)) {

            // Run system installed action first, then client
            final MouseState state = mMouseStates[mRecentMouse.getButton()];
            state.getSystemAction().execute(mRecentMouse);
            state.getAction().execute(mRecentMouse);
        }
    }

    /**
     * <p>Iterate through all keyboard keys and execute all {@link Action}s
     * whose last {@link KeyEvent} was a {@link Event.Action#PRESS}.</p>
     *
     * @param skipIndex index of recently pressed key.
     */
    private void executeRepeatingKeys(int skipIndex)
    {
        for (int i = 0; i < mKeyStates.length; i++) {
            // Skip the recently executed key
            if (i == skipIndex) {
                continue;
            }

            // Skip keys which are not pressed
            final KeyState state = mKeyStates[i];
            final KeyEvent event = state.getEvent();
            if (event == null || !event.isAction(Event.Action.PRESS)) {
                continue;
            }

            // Get repeat copy
            final KeyEvent repeatEvent = repeat(event);

            // Execute actions
            state.getSystemAction().execute(repeatEvent);
            state.getAction().execute(repeatEvent);

            // Put repeat event back in the pool for reuse
            mKeyRepeatPool.add(repeatEvent);
        }
    }

    /**
     * Gets a {@link Event.Action#REPEAT} action {@link Event}.
     *
     * @param event {@link KeyEvent} to repeat.
     * @return repeated Event.
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
     * <p>Attaches a keyboard key {@link Action} at the system level.</p>
     *
     * @param key keyboard key constant.
     * @param action Action.
     * @return previously set Action.
     */
    Action<KeyEvent> attachSystemKey(int key, Action<KeyEvent> action)
    {
        return attach(key, action, mKeyStates, NULL_KEY, true);
    }

    /**
     * <p>Attaches a mouse button {@link Action} at the system level.</p>
     *
     * @param button mouse button constant.
     * @param action Action.
     * @return previously set Action.
     */
    Action<MouseEvent> attachSystemMouse(int button, Action<MouseEvent> action)
    {
        return attach(button, action, mMouseStates, NULL_MOUSE, true);
    }

    /**
     * <p>Detaches a keyboard key {@link Action} set at the system level.</p>
     *
     * @param key keyboard key constant.
     * @return removed Action.
     */
    Action<KeyEvent> detachSystemKey(int key)
    {
        return attachSystemKey(key, NULL_KEY);
    }

    /**
     * <p>Detaches an mouse button {@link Action} set at the system level.</p>
     *
     * @param button mouse button constant.
     * @return removed Action.
     */
    Action<MouseEvent> detachSystemButton(int button)
    {
        return attachSystemMouse(button, NULL_MOUSE);
    }

    @Override
    public Action<KeyEvent> attachKey(int key, Action<KeyEvent> action)
    {
        return attach(key, action, mKeyStates, NULL_KEY, false);
    }

    @Override
    public Action<MouseEvent> attachMouse(int button, Action<MouseEvent> action)
    {
        return attach(button, action, mMouseStates, NULL_MOUSE, false);
    }

    /**
     * <p>Attaches an {@link Action} to an index corresponding to the key or
     * button constant.</p>
     *
     * @param index hardware input constant.
     * @param action {@link Action}.
     * @param states {@link KeyState}[] or {@link MouseState}[].
     * @param blank null Action; {@link KeyAction} or {@link MouseAction}.
     * @param system true if the Action should be stored as a system Action.
     * @param <E> {@link KeyEvent} or {@link MouseEvent}
     * @return the previously set Action.
     */
    private <E extends Event> Action<E> attach(int index, Action<E> action,
                                          State<E>[] states, Action<E> blank,
                                          boolean system)
    {
        final State<E> state = states[index];
        final Action<E> old;
        old = (system) ? state.getSystemAction() : state.getAction();

        if (system) {
            state.setSystemAction(action);
        } else {
            state.setAction(action);
        }

        return (old == blank) ? null : old;
    }

    @Override
    public Action<KeyEvent> detachKey(int key)
    {
        return attachKey(key, NULL_KEY);
    }

    @Override
    public Action<MouseEvent> detachMouse(int button)
    {
        return attachMouse(button, NULL_MOUSE);
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
    private class KeyState extends State<KeyEvent>
    {
        // Keyboard key constant
        private int mKey;

        /**
         * <p>Constructor for a KeyState.</p>
         *
         * @param sysAction {@link Action} set b the system.
         * @param action Action set by the user.
         * @param event initial {@link KeyEvent}.
         */
        private KeyState(Action<KeyEvent> sysAction, Action<KeyEvent> action,
                           KeyEvent event)
        {
            super(sysAction, action, event);
        }

        /**
         * <p>Gets the keyboard key constant.</p>
         *
         * @return keyboard key.
         */
        public int getKey()
        {
            return mKey;
        }

        /**
         * <p>Sets the keyboard key constant.</p>
         *
         * @param key keyboard key.
         */
        public void setKey(int key)
        {
            mKey = key;
        }
    }

    /**
     * <p>
     *     Wrapper for {@link MouseEvent} type {@link State}.
     * </p>
     */
    private class MouseState extends State<MouseEvent>
    {
        // Mouse button constant
        private int mButton;

        /**
         * <p>Constructor for a MouseState.</p>
         *
         * @param sysAction {@link Action} set by the system.
         * @param action Action set by the user.
         * @param event initial {@link MouseEvent}.
         */
        private MouseState(Action<MouseEvent> sysAction, Action<MouseEvent>
                action, MouseEvent event)
        {
            super(sysAction, action, event);
        }

        /**
         * <p>Gets the mouse button constant.</p>
         *
         * @return mouse button.
         */
        public int getButton()
        {
            return mButton;
        }

        /**
         * <p>Sets the mouse button constant.</p>
         *
         * @param button mouse button.
         */
        public void setButton(int button)
        {
            mButton = button;
        }
    }

    /**
     * <p>
     *     Tracks the last {@link Event} of an input and references the
     *     {@link Action}s set to execute according to the right Event.
     * </p>
     *
     * @param <E> {@link KeyEvent} or {@link MouseEvent}
     */
    private abstract class State<E extends Event>
    {
        // System-installed Action
        private Action<E> mSystemAction;

        // User-installed Action
        private Action<E> mAction;

        // Last Event for the State
        private E mRecentEvent;

        /**
         * <p>Constructor for a State.</p>
         *
         * @param sysAction an {@link Action} installed by the engine.
         * @param action an Action installed by the user.
         * @param event initial {@link Event}.
         */
        private State(Action<E> sysAction, Action<E> action, E event)
        {
            mSystemAction = sysAction;
            mAction = action;
            mRecentEvent = event;
        }

        /**
         * <p>Gets the {@link Action} set by the system.</p>
         *
         * @return Action.
         */
        public final Action<E> getSystemAction()
        {
            return mSystemAction;
        }

        /**
         * <p>Sets an {@link Action} from the system.</p>
         *
         * @param action Action.
         */
        public final void setSystemAction(Action<E> action)
        {
            mSystemAction = action;
        }

        /**
         * <p>Gets the {@link Action} set by the user.</p>
         *
         * @return Action.
         */
        public final Action<E> getAction()
        {
            return mAction;
        }

        /**
         * <p>Sets an {@link Action} from the user.</p>
         *
         * @param action Action.
         */
        public final void setAction(Action<E> action)
        {
            mAction = action;
        }

        /**
         * <p>Gets the most recent {@link Event}.</p>
         *
         * @return Event.
         */
        public final E getEvent()
        {
            return mRecentEvent;
        }

        /**
         * <p>Sets the most recent {@link Event}.</p>
         *
         * @param event Event.
         */
        public final void setEvent(E event)
        {
            mRecentEvent = event;
        }
    }

    /**
     * <p>
     *     Wrapper for a {@link KeyEvent} {@link Action} that does nothing.
     *     This is meant as a null Action for KeyEvents.
     * </p>
     */
    private class KeyAction implements Action<KeyEvent>
    {
        @Override
        public void execute(KeyEvent event)
        {

        }
    }

    /**
     * <p>
     *     Wrapper for a {@link MouseEvent} {@link Action} that does nothing.
     *     This is meant as a null Action for MouseEvents.
     * </p>
     */
    private class MouseAction implements Action<MouseEvent>
    {
        @Override
        public void execute(MouseEvent event)
        {

        }
    }
}
