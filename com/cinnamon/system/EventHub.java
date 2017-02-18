package com.cinnamon.system;

import java.util.Queue;

/**
 * <p>
 *     DefaultEventHub is a centralized depot for {@link Event}s to be stored for later propagation. Events are
 *     processed and propagated by a call to {@link #broadcast()} with Event handling deferred to the
 *     {@link EventDispatcher}, which can define default Event reactions as well as notify registered
 *     {@link EventHandler}s.
 * </p>
 */
public abstract class EventHub
{
    // Visitor for processing Events
    private EventDispatcher mDispatcher;

    // Event queue
    private Queue<Event> mEventQueue = createEventQueue();

    /**
     * <p>Constructor for an EventHub.</p>
     *
     * @param dispatcher EventDispatcher for handling {@link Event}s.
     */
    protected EventHub(EventDispatcher dispatcher)
    {
        mDispatcher = dispatcher;
    }

    /**
     * <p>Adds an {@link Event} to be handled.</p>
     *
     * @param event Events such as {@link CreateEvent}.
     */
    public abstract void add(Event event);

    /**
     * <p>Removes the oldest queued(s) {@link Event} and gives it to the set {@link EventDispatcher} to be processed.
     * </p>
     */
    public abstract void broadcast();

    /**
     * <p>Instantiates a {@link Queue} to serve as the event queue.</p>
     *
     * @return event queue.
     */
    protected abstract Queue<Event> createEventQueue();

    /**
     * <p>Gets the event queue.</p>
     *
     * @return event queue.
     */
    protected final Queue<Event> getEventQueue()
    {
        return mEventQueue;
    }

    /**
     * <p>Gets the {@link EventDispatcher} used to propagate {@link Event}s from the event queue.</p>
     *
     * @return EventDispatcher.
     */
    protected final EventDispatcher getDispatcher()
    {
        return mDispatcher;
    }
}
