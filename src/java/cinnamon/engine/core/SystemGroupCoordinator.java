package cinnamon.engine.core;

import java.util.function.Consumer;

public interface SystemGroupCoordinator<E extends BaseSystem>
{
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
     * Pauses all pausable yet unpaused systems.
     *
     * @param reason reason.
     * @throws IllegalStateException if systems have not yet started.
     */
    void pauseSystems(int reason);

    /**
     * Resumes all paused systems.
     *
     * @param reason reason.
     * @throws IllegalStateException if systems have not yet started.
     */
    void resumeSystems(int reason);

    /**
     * Calls {@link Consumer#accept(Object)} with every system. This method ignores whether or not a system is paused.
     *
     * <p>When notifying systems about an event, a pause state should be honored and a system ignored to maintain
     * expectations. For this purpose, {@link #callWithUnpausedSystems(Consumer)} should be used instead.</p>
     *
     * @param action action.
     * @throws NullPointerException if action is null.
     */
    void callWithSystems(Consumer<E> action);

    /**
     * Calls {@link Consumer#accept(Object)} with every pausable yet unpaused system.
     *
     * @param action action.
     * @throws NullPointerException if action is null.
     * @see #callWithSystems(Consumer)
     */
    void callWithUnpausedSystems(Consumer<E> action);
}
