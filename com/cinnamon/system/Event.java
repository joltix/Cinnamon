package com.cinnamon.system;

/**
 * <p>
 *     Base class for Events created and sent around the engine to notify and request.
 * </p>
 */
public abstract class Event
{
    // Time in nanosec of last event data update
    private long mTime;

    /**
     * <p>Constructor for an Event.</p>
     */
    protected Event()
    {
        timestamp();
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

    /**
     * <p>Updates the Event's timestamp. THis method should only be called when held data is changed.</p>
     */
    protected void timestamp()
    {
        mTime = System.nanoTime();
    }

    /**
     * <p>Calls on an {@link EventDispatcher} to process the Event.</p>
     *
     * @param distributor EventDispatcher.
     */
    protected abstract void handle(EventDispatcher distributor);
}
