package cinnamon.engine.core;

import java.util.function.Consumer;

/**
 *
 *
 * @param <E> type of system.
 */
public interface SystemGroupCoordinator<E extends GameSystem>
{
    /**
     * Reason code used when pausing and resuming systems en mass.
     */
    int REASON = 0;

    /**
     * Starts all systems.
     *
     * @throws IllegalStateException if systems have already been started.
     */
    void startSystems();

    /**
     * Stops all systems.
     *
     * @throws IllegalStateException if systems have not yet started.
     */
    void stopSystems();

    /**
     * Pauses all running systems. Any already paused system will not have {@link GameSystem#onPause(int)} called.
     *
     * @param reason reason.
     * @throws IllegalStateException if systems have not yet started.
     */
    void pauseSystems(int reason);

    /**
     * Resumes all paused systems. Any already running system will not have {@link GameSystem#onResume(int)} called.
     *
     * @param reason reason.
     * @throws IllegalStateException if systems have not yet started.
     */
    void resumeSystems(int reason);

    /**
     * Calls {@link Consumer#accept(Object)} with every system.
     *
     * @param action action.
     * @throws NullPointerException if action is null.
     */
    void callWithSystems(Consumer<E> action);
}
