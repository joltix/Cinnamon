package com.cinnamon.system;

import org.lwjgl.glfw.GLFW;

/**
 * <p>
 *     Describes the {@link Action} input that forms a common basis for a {@link KeyEvent} and {@link MouseEvent} by
 *     describing each InputEvent with an Action such as {@link Action#PRESS} or {@link Action#RELEASE}.
 * </p>
 *
 * <p>
 *     Only MouseEvents whose {@link MouseEvent.Button} is {@link MouseEvent.Button#MIDDLE} may have
 *     {@link Action#SCROLL_FORWARD} or {@link Action#SCROLL_BACKWARD} as an Action.
 * </p>
 *
 * <p>
 *     {@link Action#REPEAT} events are copies of an older PRESS event. These are generated so long as the original
 *     PRESS has no matching RELEASE.
 * </p>
 */
public abstract class InputEvent extends Event
{
    /**
     * <p>
     *     Constants describing an {@link InputEvent}'s nature: either a press, release, repeat, scroll forward, or
     *     scroll backward.
     * </p>
     */
    public enum Action
    {
        /**
         * <p>Used to describe a press.</p>
         */
        PRESS,
        /**
         * <p>Used to describe a release after a press.</p>
         */
        RELEASE,
        /**
         * <p>Used to describe a press without yet a release.</p>
         */
        REPEAT,

        /**
         * <p>Used to describe scrolling forward the middle mouse button.</p>
         */
        SCROLL_FORWARD,

        /**
         * <p>Used to describe scrolling backward the middle mouse button.</p>
         */
        SCROLL_BACKWARD
    }

    // Input Action such as press, release, repeat
    private Action mAction;

    /**
     * <p>Constructor for an InputEvent.</p>
     *
     * @param action either {@link Action#PRESS}, {@link Action#RELEASE}, or {@link Action#REPEAT}.
     */
    public InputEvent(Action action)
    {
        setAction(action);
    }

    /**
     * <p>Gets the {@link Action} describing the InputEvent.</p>
     *
     * @return either {@link Action#PRESS}, {@link Action#RELEASE}, or {@link Action#REPEAT}.
     */
    public final Action getAction()
    {
        return mAction;
    }

    /**
     * <p>Sets the {@link Action} describing the InputEvent.</p>
     *
     * @param action either {@link Action#PRESS}, {@link Action#RELEASE}, or {@link Action#REPEAT}.
     */
    protected final void setAction(Action action)
    {
        mAction = action;
    }

    /**
     * <p>Checks whether or not the InputEvent is described by {@link Action#PRESS}.</p>
     *
     * @return true if PRESS event.
     */
    public final boolean isPress()
    {
        return mAction == Action.PRESS;
    }

    /**
     * <p>Checks whether or not the InputEvent is described by {@link Action#RELEASE}.</p>
     *
     * @return true if RELEASE event.
     */
    public final boolean isRelease()
    {
        return mAction == Action.RELEASE;
    }

    /**
     * <p>Checks whether or not the InputEvent is described by {@link Action#REPEAT}.</p>
     *
     * @return true if REPEAT event.
     */
    public final boolean isRepeat()
    {
        return mAction == Action.REPEAT;
    }

    /**
     * <p>Gets the {@link Action} form of a GLFW action constant.</p>
     *
     * @param system system constant.
     * @return Action, or null if the system constant is unrecognized.
     */
    static final Action systemActionToAction(int system)
    {
        switch (system) {
            case GLFW.GLFW_PRESS:
                return Action.PRESS;
            case GLFW.GLFW_RELEASE:
                return Action.RELEASE;
            default: return null;
        }
    }
}
