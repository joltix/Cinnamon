package cinnamon.engine.utils;

/**
 * Marks a class as having a priority value to describe its importance, often for sorting purposes. Unlike
 * {@link Comparable}, this interface conceptually does not imply a natural ordering amongst objects as implementing
 * classes may be completely unrelated. Practically, using {@code Prioritizable} shifts the responsibility for
 * testing difference and equality to the sorting code.
 *
 * <p>This interface should be used either when a class has conceptually no natural ordering, or instances are
 * compared with those of an unrelated class.</p>
 */
public interface Prioritizable
{
    /**
     * Gets the integer value used during sorting.
     *
     * @return priority.
     */
    int getPriority();
}