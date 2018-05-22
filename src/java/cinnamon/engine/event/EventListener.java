package cinnamon.engine.event;

/**
 * <p>Listener to be called when an {@code Event} occurs.</p>
 *
 * @param <T> type of event.
 */
public interface EventListener<T extends Event>
{
    /**
     * <p>Called when the event occurs.</p>
     *
     * @param event event.
     */
    void onEvent(T event);
}
