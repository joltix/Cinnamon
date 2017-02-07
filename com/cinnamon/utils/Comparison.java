package com.cinnamon.utils;

/**
 */
/**
 * <p>
 *    Declares a compare method for judging whether or not an object is
 *    less than, equal to, or greater than another object.
 * </p>
 *
 * <p>
 *     The difference between Comparison and older classes for comparing
 *     objects is that classes do not need to implement the comparison within
 *     their respective classes. Instead, classes that require comparisons
 *     between objects can instantiate and swap Comparison implementations
 *     dynamically, allowing different orderings.
 * </p>
 *
 * @param <E> type of objects to compare.
 *
 *
 */
public interface Comparison<E>
{
    /**
     * <p>Compares two objects to discern a less than, equal to, or greater
     * than relationship. The value this method returns is always a
     * comparison relative to the first given object.</p>
     *
     * @param obj0 one object.
     * @param obj1 other object.
     * @return a negative integer if the first object is considered less than
     * the second, zero if both are equal, and a positive integer if the
     * first is greater than the second.
     */
    int compare(E obj0, E obj1);
}