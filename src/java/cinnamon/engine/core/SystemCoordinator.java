package cinnamon.engine.core;

import java.util.NoSuchElementException;

/**
 * Allows external pausing and resuming of systems.
 *
 * @param <E> type of system.
 */
public interface SystemCoordinator<E extends BaseSystem> extends SystemDirectory<E>
{

    /**
     * Pauses a pausable yet unpaused system.
     *
     * @param name name.
     * @param reason reason.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     */
    void pauseSystem(String name, int reason);

    /**
     * Resumes a paused system.
     *
     * @param name name.
     * @param reason reason.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     */
    void resumeSystem(String name, int reason);
}
