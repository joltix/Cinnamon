package cinnamon.engine.event;

/**
 * <p>This interface is implemented by event sources that can suppress the creation of new related events.</p>
 */
public interface EventSilenceable
{
    /**
     * <p>Checks if events are being suppressed.</p>
     *
     * @return true if events are suppressed.
     */
    boolean isMuted();

    /**
     * <p>Suppresses event creation.</p>
     */
    void mute();

    /**
     * <p>Allows new events to be created.</p>
     */
    void unmute();
}
