package com.cinnamon.system;

/**
 * <p>
 *     A hub for input {@link Event}s to be distributed when the appropriate hardware is triggered (an early control
 *     mapping system).
 * </p>
 *
 * <p>
 *     {@link KeyEvent}s and {@link MouseEvent}s to react to should be added through {@link #addEvent(KeyEvent)} or
 *     {@link #addEvent(MouseEvent)} while actions to execute are linked to hardware constants through the <i>attach
 *     (hardware, handler)</i> methods. Calling {@link #fire()} will execute the attached {@link EventHandler}s when
 *     their firing conditions have been met.
 * </p>
 *
 * <p>
 *     Each hardware constant has a desired firing condition which must be met before its attached
 *     {@link KeyEventHandler} or {@link MouseEventHandler} can be triggered. The desired condition can be set
 *     through {@link #setMode(KeyEvent.Key, boolean, boolean)} and
 *     {@link #setMode(MouseEvent.Button, boolean, boolean)} to specify firing on a {@link InputEvent.Action#PRESS},
 *     {@link InputEvent.Action#RELEASE}, or in the case of {@link MouseEvent.Button}s, even
 *     {@link InputEvent.Action#SCROLL_FORWARD} or {@link InputEvent.Action#SCROLL_BACKWARD}.
 * </p>
 *
 * <p>
 *     There are no guarantees about the order in which {@link KeyEventHandler}s and {@link MouseEventHandler}s are
 *     executed during {@link #fire()}; any order decision is left to implementations.
 * </p>
 */
public interface ControlMap
{
    /**
     * <p>Adds a {@link KeyEvent} to update the state of the corresponding {@link KeyEvent.Key}. A
     * {@link KeyEventHandler} attached to the {@link KeyEvent.Key} may be executed on the next call to
     * {@link #fire()}, depending on the Key's fire mode.</p>
     *
     * @param event KeyEvent.
     */
    void addEvent(KeyEvent event);

    /**
     * <p>Adds a {@link MouseEvent} to update the state of the corresponding {@link MouseEvent.Button}. A
     * {@link MouseEventHandler} attached to the {@link MouseEvent.Button} may be executed on the next call to
     * {@link #fire()}, depending on the Button's fire mode.</p>
     *
     * @param event MouseEvent.
     */
    void addEvent(MouseEvent event);

    /**
     * <p>Sets a {@link KeyEvent.Key} to fire on {@link InputEvent.Action#PRESS} or {@link InputEvent.Action#RELEASE}
     * and whether PRESS events for the Key should generate subsequent {@link InputEvent.Action#REPEAT} events.</p>
     *
     * @param key keyboard key.
     * @param fireOnPress true to fire {@link KeyEventHandler}s on PRESS events, false for RELEASE.
     * @param repeatable true to generate REPEAT events after a PRESS.
     */
    void setMode(KeyEvent.Key key, boolean fireOnPress, boolean repeatable);

    /**
     * <p>Sets a {@link MouseEvent.Button} to fire on {@link InputEvent.Action#PRESS} or
     * {@link InputEvent.Action#RELEASE} and whether PRESS events for the Button should generate subsequent
     * {@link InputEvent.Action#REPEAT} events.</p>
     *
     * @param button mouse button.
     * @param fireOnPress true to fire {@link MouseEventHandler}s on PRESS events, false for RELEASE.
     * @param repeatable true to generate REPEAT events after a PRESS.
     */
    void setMode(MouseEvent.Button button, boolean fireOnPress, boolean repeatable);

    /**
     * <p>Executes all attached {@link EventHandler}s whose desired firing state has been satisfied by the most
     * recent {@link InputEvent}s set through either {@link #addEvent(KeyEvent)} or {@link #addEvent(MouseEvent)}.</p>
     *
     * <p>If a {@link KeyEvent.Key} or {@link MouseEvent.Button} is set as repeatable, and  it's current
     * {@link InputEvent.Action} is {@link InputEvent.Action#PRESS}, then this method will generate a corresponding
     * {@link InputEvent.Action#REPEAT} {@link KeyEvent}.</p>
     */
    void fire();

    /**
     * <p>Attaches a {@link KeyEventHandler} to a {@link KeyEvent.Key} to be executed the next time the
     * appropriate {@link KeyEvent} is set when {@link #fire()} is called.</p>
     *
     * @param key keyboard key.
     * @param handler KeyEventHandler to fire.
     * @return previously set KeyEventHandler, or null if none was set.
     */
    KeyEventHandler attach(KeyEvent.Key key, KeyEventHandler handler);

    /**
     * <p>Attaches a {@link MouseEventHandler} to a {@link MouseEvent.Button} to be executed the next time the
     * appropriate {@link MouseEvent} is set when {@link #fire()} is called.</p>
     *
     * @param button mouse button.
     * @param handler MouseEventHandler to fire.
     * @return previously set MouseEventhandler, or null if none was set.
     */
    MouseEventHandler attach(MouseEvent.Button button, MouseEventHandler handler);

    /**
     * <p>Detaches a {@link KeyEventHandler} from the given {@link KeyEvent.Key}.</p>
     *
     * @param key keyboard key.
     * @return removed KeyEventHandler.
     */
    KeyEventHandler detach(KeyEvent.Key key);

    /**
     * <p>Detaches an {@link MouseEventHandler} from the given {@link MouseEvent.Button}.</p>
     *
     * @param button mouse button.
     * @return removed MouseEventHandler.
     */
    MouseEventHandler detach(MouseEvent.Button button);

    /**
     * <p>Gets the most recent {@link KeyEvent}.</p>
     *
     * @return KeyEvent.
     */
    KeyEvent getKey();

    /**
     * <p>Gets the most recent {@link MouseEvent}.</p>
     *
     * @return MouseEvent.
     */
    MouseEvent getMouse();
}
