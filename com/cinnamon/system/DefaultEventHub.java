package com.cinnamon.system;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * <p>
 *     This {@link EventHub} implementation is used when no user implementation is provided. All {@link Event}s
 *     except {@link InputEvent}s can be added from any thread through {@link #add(Event)}. This class assumes
 *     {@link Window.Input} as the only source of InputEvents and so all provided InputEvents must be added from the
 *     main thread. {@link #add(InputEvent)} is not thread-safe.
 * </p>
 */
public final class DefaultEventHub extends EventHub
{
    // Number of InputEvents to process per broadcast
    private static final int BROADCAST_INPUT_CAPACITY = 5;

    // Number of general Events to process per broadcast
    private static final int BROADCAST_CAPACITY = 100;

    // Initial queue size for general Events
    private static final int EVENT_LOAD = 90;

    // KeyEvent and MouseEvent queue
    private final Queue<Event> mInputQueue = new LinkedList<Event>();

    /**
     * <p>Constructor for a DefaultEventHub using a {@link DefaultEventDispatcher}.</p>
     */
    protected DefaultEventHub()
    {
        super(new DefaultEventDispatcher());
    }

    @Override
    protected Queue<Event> createEventQueue()
    {
        return new ArrayBlockingQueue<Event>(EVENT_LOAD);
    }

    /**
     * {@inheritDoc} This method is thread-safe.
     */
    @Override
    public void add(Event event)
    {
        getEventQueue().add(event);
    }

    /**
     * <p>Adds an {@link InputEvent} to be handled.</p>
     *
     * <p>This method should not be called from any thread other than main without external synchronization.</p>
     *
     * @param event {@link KeyEvent} or {@link MouseEvent}.
     */
    public void add(InputEvent event)
    {
        mInputQueue.add(event);
    }

    @Override
    public void broadcast()
    {
        // Process InputEvents first
        broadcast(mInputQueue, BROADCAST_INPUT_CAPACITY);

        // Process general Events
        broadcast(getEventQueue(), BROADCAST_CAPACITY);
    }

    /**
     * <p>Polls a given {@link Queue} a <i>cap</i> amount of {@link Event} and passes them to the currently set
     * {@link EventDispatcher}.</p>
     *
     * @param queue Queue of Events.
     * @param cap number of Events to poll.
     * @param <E> Event type.
     */
    private <E extends Event> void broadcast(Queue<E> queue, int cap)
    {
        final EventDispatcher dispatcher = getDispatcher();

        for (int i = 0; i < cap; i++) {
            final E event = queue.poll();
            if (event != null) {
                event.handle(dispatcher);
            }
        }
    }
}
