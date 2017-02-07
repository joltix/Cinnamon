package com.cinnamon.utils;

/**
 * <p>
 *     Base class for comparison-based sorting algorithms. This class provides a
 *     swappable the {@link Comparison} to establish an ordering between
 *     objects as well as an ordering validation method
 *     {@link #isOrdered(Object[], boolean)}.
 * </p>
 *
 *
 */
public abstract class Sort<E>
{
    // Ordering decider between two objects
    private Comparison<E> mCmp;

    /**
     * <p>Constructor with a {@link Comparison} for establishing an
     * ordering between two objects while sorting.</p>
     *
     * @param comparison Comparison.
     * @throws IllegalArgumentException if the given Comparison is null.
     */
    protected Sort(Comparison<E> comparison)
    {
        setComparison(comparison);
    }

    /**
     * <p>Sorts an array of Objects.</p>
     *
     * @param objects array.
     */
    public abstract void sort(E[] objects);

    /**
     * <p>Checks whether or not an array's objects are sorted.</p>
     *
     * @param values array to examine.
     * @param ascending true to check for an ascending order.
     * @return true if the array is in the desired order.
     */
    public boolean isOrdered(E[] values, boolean ascending)
    {
        // Check each value if it is logically greater/less than the last,
        // depending on desired order
        E last = values[0];
        for (int i = 1; i < values.length; i++) {
            final E obj = values[i];
            final int cmp = mCmp.compare(obj, last);

            // Check if there's an order violation between current and previous
            if ((ascending && cmp < 0) || (!ascending && cmp > 0)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Gets the {@link Comparison} used to discern a less than, equal to,
     * or greater than relationship between two objects.</p>
     *
     * @return Comparison.
     */
    protected final Comparison<E> getComparison()
    {
        return mCmp;
    }

    /**
     * <p>Sets a {@link Comparison} to judge whether or not an object is
     * less than, equal to, or greater than another object during the sorting
     * process.</p>
     *
     * @param comparison Comparison.
     * @throws IllegalArgumentException if the given Comparison is null.
     */
    public final void setComparison(Comparison comparison)
    {
        if (comparison == null) {
            throw new IllegalArgumentException("Comparison cannot be null");
        }

        mCmp = comparison;
    }
}
