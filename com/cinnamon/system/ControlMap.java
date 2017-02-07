package com.cinnamon.system;

import com.cinnamon.utils.Event;
import com.cinnamon.utils.KeyEvent;
import com.cinnamon.utils.MouseEvent;

/**
 * <p>
 *     A hub for input {@link Event}s to be distributed when the appropriate
 *     hardware is triggered (an early control mapping system).
 * </p>
 *
 *
 */
public interface ControlMap
{
    /**
     * <p>Sets a {@link KeyEvent} to be sent on the next call to
     * {@link #executeActions()}.</p>
     *
     * @param event KeyEvent.
     */
    void setEvent(KeyEvent event);

    /**
     * <p>Sets a {@link MouseEvent} to be sent on the next call to
     * {@link #executeActions()}.</p>
     *
     * @param event MouseEvent.
     */
    void setEvent(MouseEvent event);

    /**
     * <p>Executes all attached {@link Action}s that have their
     * corresponding {@link Event}s set.</p>
     */
    void executeActions();

    /**
     * <p>Attaches an {@link Action} to a keyboard constant to be executed the
     * next time {@link #executeActions()} is called with a corresponding
     * {@link KeyEvent} set.</p>
     *
     * @param key keyboard key constant.
     * @param action Action to run.
     * @return
     */
    Action<KeyEvent> attachKey(int key, Action<KeyEvent> action);

    /**
     * <p>Attaches an {@link Action} to a mouse constant to be executed the
     * next time {@link #executeActions()} is called with a corresponding
     * {@link MouseEvent} set.</p>
     *
     * @param button mouse button constant.
     * @param action Action to run.
     * @return
     */
    Action<MouseEvent> attachMouse(int button, Action<MouseEvent> action);

    /**
     * <p>Detaches an {@link Action} from the given key constant.</p>
     *
     * @param key keyboard key constant.
     * @return removed Action.
     */
    Action<KeyEvent> detachKey(int key);

    /**
     * <p>Detaches an {@link Action} from the given mouse constant.</p>
     *
     * @param button mouse button constant.
     * @return removed Action.
     */
    Action<MouseEvent> detachMouse(int button);

    /**
     * <p>Gets the most recent KeyEvent.</p>
     *
     * @return KeyEvent.
     */
    KeyEvent getKey();

    /**
     * <p>Gets the most recent MouseEvent.</p>
     *
     * @return MouseEvent.
     */
    MouseEvent getMouse();

    /**
     * <p>Action to be attached and executed when an input {@link Event}
     * matching the desired input constant is submitted to the
     * {@link ControlMap}.</p>
     *
     * @param <E> {@link KeyEvent} or {@link MouseEvent}
     */
    interface Action<E extends Event>
    {
        void execute(E event);
    }
}
