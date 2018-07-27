package cinnamon.engine.core;

import java.util.NoSuchElementException;

/**
 * Gives a {@code SystemDirectory} the ability to add and remove systems.
 *
 * @param <E> type of system.
 */
public interface WritableSystemDirectory<E extends BaseSystem> extends SystemDirectory<E>
{
    /**
     * Adds a system with the given name.
     *
     * @param name name.
     * @param system system.
     * @throws NullPointerException if name or system is null.
     * @throws IllegalArgumentException if either name or system is already in use.
     * @throws IllegalStateException if adding systems is not currently allowed.
     */
    void addSystem(String name, E system);

    /**
     * Removes a system.
     *
     * @param name name.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     * @throws IllegalStateException if removing systems is not currently allowed.
     */
    void removeSystem(String name);
}
