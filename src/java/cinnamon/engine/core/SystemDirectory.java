package cinnamon.engine.core;

import java.util.NoSuchElementException;

/**
 * Declares the ability to retrieve a system by name.
 *
 * @param <E> type of system.
 */
public interface SystemDirectory<E extends GameSystem>
{
    /**
     * Gets a system.
     *
     * @param name name.
     * @return system.
     * @throws NullPointerException if name is null.
     * @throws NoSuchElementException if name does not refer to a system.
     */
    E getSystem(String name);
}
