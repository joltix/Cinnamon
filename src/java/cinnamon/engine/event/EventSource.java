package cinnamon.engine.event;

/**
 * <p>Designates a class that outputs a stream of events.</p>
 *
 * @param <T> type of event.
 */
public interface EventSource<T extends Event>
{
    /**
     * <p>Removes the next available event.</p>
     *
     * @return next event or null if none are available.
     */
    T pollEvent();
}
