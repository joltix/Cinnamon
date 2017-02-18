package com.cinnamon.system;

/**
 * <p>
 *     Handles {@link Event}s submitted to the {@link EventHub}. EventHandlers are callbacks to be registered with
 *     the EventHub's {@link EventDispatcher} which will notify the handler when the desired Event is processed.
 * </p>
 *
 * @param <E> Event type.
 */
public interface EventHandler<E extends Event>
{
    /**
     * <p>Performs operations based off of an {@link Event} provided by the {@link EventDispatcher}.</p>
     *
     * @param event Event.
     */
    void handle(E event);
}
