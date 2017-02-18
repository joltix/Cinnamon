package com.cinnamon.system;

/**
 * <p>
 *     EventDispatcher registers {@link EventHandler}s and {@link EventFilter}s to be notified of specific
 *     {@link Event}s when the appropriate <i>process(Event)</i> methods are called.
 * </p>
 *
 * <p>
 *     The order in which EventHandlers are notified depend on the priority specified when the EventHandler was added
 *     . Though values in-between are permitted, priority is categorized as follows: {@link #PRIORITY_MIN},
 *     {@link #PRIORITY_LOW}, {@link #PRIORITY_MEDIUM}, {@link #PRIORITY_HIGH}, and {@link #PRIORITY_MAX}.
 * </p>
 */
public interface EventDispatcher
{
    /**
     * <p>Lowest possible {@link EventHandler} execution priority. An EventHandler with this priority can
     * only be succeeded by another PRIORITY_MIN.</p>
     */
    public static final int PRIORITY_MIN = Integer.MIN_VALUE;

    /**
     * <p>Second lowest {@link EventHandler} execution priority.</p>
     */
    public static final int PRIORITY_LOW = Integer.MIN_VALUE / 2;

    /**
     * <p>Neutral {@link EventHandler} execution priority.</p>
     */
    public static final int PRIORITY_MEDIUM = 0;

    /**
     * <p>Second highest {@link EventHandler} execution priority.</p>
     */
    public static final int PRIORITY_HIGH = Integer.MAX_VALUE / 2;

    /**
     * <p>Highest {@link EventHandler} execution priority. And EventHandler with this priority can only be succeeded
     * by another PRIORITY_MAXIMUM.</p>
     */
    public static final int PRIORITY_MAX = Integer.MAX_VALUE;

    /**
     * <p>Adds a {@link KeyEventHandler} for processing {@link KeyEvent}s.</p>
     *
     * @param handler KeyEventHandler.
     * @param filter EventFilter for ignoring undesired {@link Event}s.
     * @param priority value determining event handling order (higher is earlier).
     */
    void addHandler(KeyEventHandler handler, EventFilter<KeyEvent> filter, int priority);

    /**
     * <p>Removes a {@link KeyEventHandler} and its associated {@link EventFilter}.</p>
     *
     * @param handler KeyEventHandler.
     */
    void removeHandler(KeyEventHandler handler);

    /**
     * <p>Adds a {@link MouseEventHandler} for processing {@link MouseEvent}s.</p>
     *
     * @param handler MouseEventHandler.
     * @param filter EventFilter for ignoring undesired {@link Event}s.
     * @param priority value determining event handling order (higher is earlier).
     */
    void addHandler(MouseEventHandler handler, EventFilter<MouseEvent> filter, int priority);

    /**
     * <p>Removes a {@link MouseEventHandler} and its associated {@link EventFilter}.</p>
     *
     * @param handler MouseEventHandler.
     */
    void removeHandler(MouseEventHandler handler);

    /**
     * <p>Adds a {@link CreateEventHandler} for processing {@link CreateEvent}s.</p>
     *
     * @param handler CreateEventHandler.
     * @param filter EventFilter for ignoring undesired {@link Event}s.
     * @param priority value determining event handling order (higher is earlier).
     */
    void addHandler(CreateEventHandler handler, EventFilter<CreateEvent> filter, int priority);

    /**
     * <p>Removes a {@link CreateEventHandler} and its associated {@link EventFilter}.</p>
     *
     * @param handler CreateEventHandler.
     */
    void removeHandler(CreateEventHandler handler);

    /**
     * <p>Adds a {@link DestroyEventHandler} for processing {@link DestroyEvent}s.</p>
     *
     * @param handler DestroyEventHandler.
     * @param filter EventFilter for ignoring undesired {@link Event}s.
     * @param priority value determining event handling order (higher is earlier).
     */
    void addHandler(DestroyEventHandler handler, EventFilter<DestroyEvent> filter, int priority);

    /**
     * <p>Removes a {@link DestroyEventHandler} and its associated {@link EventFilter}.</p>
     *
     * @param handler DestroyEventHandler.
     */
    void removeHandler(DestroyEventHandler handler);

    /**
     * <p>Notifies any registered {@link KeyEventHandler}s and their {@link EventFilter}s of the targeted {@link Event}
     * .</p>
     *
     * @param event {@link KeyEvent}.
     */
    void process(KeyEvent event);

    /**
     * <p>Notifies any registered {@link MouseEventHandler}s and their {@link EventFilter}s of the targeted
     * {@link Event}.</p>
     *
     * @param event {@link MouseEvent}.
     */
    void process(MouseEvent event);

    /**
     * <p>Notifies any registered {@link CreateEventHandler}s and their {@link EventFilter}s of the targeted
     * {@link Event}.</p>
     *
     * @param event {@link CreateEvent}.
     */
    void process(CreateEvent event);

    /**
     * <p>Notifies any registered {@link DestroyEventHandler}s and their {@link EventFilter}s of the targeted
     * {@link Event}.</p>
     *
     * @param event {@link DestroyEvent}.
     */
    void process(DestroyEvent event);

    /**
     * <p>Removes all registered {@link EventHandler}s and their associated {@link EventFilter}s.</p>
     */
    void clearHandlers();

    /**
     * <p>
     *     Decides whether or not a given {@link Event} is a desired target Event to be processed in an
     *     {@link EventDispatcher}.
     * </p>
     *
     * @param <E> Event type.
     */
    interface EventFilter<E extends Event>
    {
        /**
         * <p>Checks an {@link Event} and decides whether or not it should be processed by the EventFilter's partner
         * {@link EventHandler}.</p>
         *
         * <p>If this method returns true, the given Event is passed to the EventHandler.</p>
         *
         * @param event
         * @return true to allow Event.
         */
        boolean filter(E event);
    }
}