package com.cinnamon.utils;

/**
 * <p>Base class for Events created and sent around the engine as messages.</p>
 *
 *
 */
public abstract class Event
{
    /**
     * <p>Types describe {@link Event}s' source.</p>
     */
    public enum Type
    {
        /**
         * <p>Defines the {@link Event} as coming from the keyboard.</p>
         */
        KEYBOARD,

        /**
         * <p>Defines the {@link Event} as coming from the mouse.</p>
         */
        MOUSE
    }

    /**
     * <p>Actions describe {@link Event}s' reason for creation.</p>
     */
    public enum Action
    {
        /**
         * <p>Represents a press from an input device.</p>
         */
        PRESS,

        /**
         * <p>Represents a repeated {@link #PRESS} event.</p>
         */
        REPEAT,

        /**
         * <p>Defines an input device returning to its natural state, and the
         * conclusion of a press-release event pair..</p>
         */
        RELEASE,

        /**
         * <p>Categorizes the event as caused by a wheel rolling forward.</p>
         */
        SCROLL_FORWARD,

        /**
         * <p>Categorizes the event as caused by a wheel rolling backward.</p>
         */
        SCROLL_BACKWARD
    }

    // Source
    private Type mType;

    // Reason for existing
    private Action mAction;

    // Time in nanosec of last event data update
    private long mTime;

    /**
     * <p>Constructs an Event of a {@link Type} and {@link Action}.</p>
     *
     * @param type either {@link Type#KEYBOARD} or {@link Type#MOUSE}.
     * @param action either {@link Action#PRESS},
     * {@link Action#RELEASE}, {@link Action#SCROLL_FORWARD}, or
     * {@link Action#SCROLL_BACKWARD}.
     */
    protected Event(Type type, Action action)
    {
        mType = type;
        mAction = action;
        mTime = System.nanoTime();
    }

    /**
     * <p>Copies the {@link Type} and {@link Action} of another Event.
     * {@link #getTime()} will reflect the time of the copy
     * constructor.</p>
     *
     * @param event Event to copy.
     */
    protected Event(Event event)
    {
        mType = event.mType;
        mAction = event.mAction;
        System.nanoTime();
    }

    /**
     * <p>Changes the {@link Type} and {@link Action}.</p>
     *
     * <p>Timestamp is not copied but updated to the time of the update.</p>
     *
     * @param type either {@link Type#KEYBOARD} or {@link Type#MOUSE}.
     * @param action either {@link Action#PRESS} or {@link Action#RELEASE}.
     */
    public void update(Type type, Action action)
    {
        mType = type;
        mAction = action;
        mTime = System.nanoTime();
    }

    /**
     * <p>Gets the Event {@link Type} category.</p>
     *
     * @return type constant such as {@link Type#KEYBOARD} or
     * {@link Type#MOUSE}.
     */
    public final Type getType()
    {
        return mType;
    }

    /**
     * <p>Gets the {@link Action} specifier describing the reason for the Event
     * .</p>
     *
     * @return action constant such as {@link Action#PRESS} or
     * {@link Action#SCROLL_FORWARD}.
     */
    public final Action getAction()
    {
        return mAction;
    }

    /**
     * <p>Checks whether or not the Event's {@link Action} matches a
     * specified one.</p>
     *
     * @param action Action constant such as {@link Action#PRESS}.
     * @return true if the Action describes the Event.
     */
    public final boolean isAction(Action action)
    {
        return mAction == action;
    }

    /**
     * <p>Gets the time of the Event's creation.</p>
     *
     * @return time in nanoseconds.
     */
    public final long getTime()
    {
        return mTime;
    }

    @Override
    public String toString()
    {
        return "[" + mType.name() + "," + mAction.name() + "]";
    }
}
