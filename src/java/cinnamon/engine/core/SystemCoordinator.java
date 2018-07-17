package cinnamon.engine.core;

import java.util.NoSuchElementException;

/**
 * Allows external pausing and resuming of systems.
 *
 * @param <E> type of system.
 */
public interface SystemCoordinator<E extends GameSystem> extends SystemDirectory<E>
{

    /**
     * Pauses a running system. If the system is already paused, this method does nothing.
     *
     * @param name name.
     * @param reason reason.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     */
    void pauseSystem(String name, int reason);

    /**
     * Resumes a paused system. If the system is not paused, this method does nothing.
     *
     * @param name name.
     * @param reason reason.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     */
    void resumeSystem(String name, int reason);
}
