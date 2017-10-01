package cinnamon.engine.utils;

/**
 * <p>Classes implementing the {@code Partitionable} interface declares that each instance is represented in a 3D space
 * by a simplified volume.</p>
 *
 * @param <T> type of boundary.
 */
public interface Partitionable<T extends Bounds>
{
    /**
     * <p>Gets the encompassing volume.</p>
     *
     * @return bounds.
     */
    T getBounds();
}
