package cinnamon.engine.event;

import cinnamon.engine.event.EventVisitor.Visitable;

/**
 * <p>Base class for events.</p>
 *
 * <h3>Time</h3>
 * <p>The framework's current timing uses {@link System#nanoTime()} for provided event families e.g.
 * {@code InputEvent}s. Subclasses expecting to interact with provided systems or comparing ordering with
 * provided events should be aware that the creation timestamp during construction is expected to be <i>monotically
 * increasing</i> and is allowed to be negative.</p>
 */
public abstract class Event implements Visitable
{
    // Creation timestamp
    private long mTime;

    /**
     * <p>Constructs an {@code Event} with a creation timestamp.</p>
     *
     * @param time creation timestamp in nanoseconds.
     */
    protected Event(long time)
    {
        mTime = time;
    }

    /**
     * <p>Gets the creation timestamp.</p>
     *
     * @return creation time.
     */
    public final long getTime()
    {
        return mTime;
    }
}
