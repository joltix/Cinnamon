package cinnamon.engine.utils;

import java.util.List;

/**
 * <p>Organizes elements by a corresponding {@code Bounds}' location and volume to perform containment and
 * intersection queries. Added {@code Bounds}-element pairs must be updated should changes to a {@code Bounds}' size or
 * position invalidate its place in the tree.</p>
 *
 * <p>Containment and intersection queries are provided in two flavors to return either a {@code List} or an array.
 * The latter variant is intended for queries where iteration over the results is the main operation and it is
 * expected to occur frequently.</p>
 *
 * <p>Implementations disallowing a {@code Bounds}-element pair from being added to the tree may throw an
 * {@code IllegalArgumentException}.</p>
 *
 * @param <E> element to partition.
 */
public interface SpacePartitioningTree<E>
{
    /**
     * <p>Adds to a {@code List} each element whose associated {@code Bounds} is contained by the given {@code Bounds}
     * through {@code capture.contains(associated)}, up to {@code limit} instances.</p>
     *
     * @param results list of contained elements.
     * @param capture containing bounds.
     * @param limit max number of elements.
     * @return results.
     * @throws NullPointerException if results or capture is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0.
     */
    List<E> getContained(List<E> results, Bounds capture, int limit);

    /**
     * <p>Array version of {@link #getContained(List, Bounds, int)}.</p>
     *
     * @param results array of contained elements.
     * @param capture containing bounds.
     * @param limit max number of elements.
     * @return results.
     * @throws NullPointerException if results or capture is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0 or the given array's length is {@literal <} limit.
     */
    E[] getContained(E[] results, Bounds capture, int limit);

    /**
     * <p>Adds to a {@code List} each element whose associated {@code Bounds} intersects with the given {@code Bounds}
     * through {@code capture.intersects(associated)}, up to {@code limit} instances.</p>
     *
     * @param results list of intersecting elements.
     * @param capture intersecting bounds.
     * @param limit max number of partitionables.
     * @return results.
     * @throws NullPointerException if results or capture is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0.
     */
    List<E> getIntersections(List<E> results, Bounds capture, int limit);

    /**
     * <p>Array version of {@link #getIntersections(List, Bounds, int)}.</p>
     *
     * @param results array of intersecting elements.
     * @param capture intersecting bounds.
     * @param limit max number of elements.
     * @return results.
     * @throws NullPointerException if results or capture is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0 or the given array's length is {@literal <} limit.
     */
    E[] getIntersections(E[] results, Bounds capture, int limit);

    /**
     * <p>Adds to a {@code List} each element whose associated {@code Bounds} intersects the given {@code Point}
     * through {@code bounds.contains(point)}, up to {@code limit} instances.</p>
     *
     * @param results list of intersecting elements.
     * @param point intersecting point.
     * @param limit max number of elements.
     * @return results.
     * @throws NullPointerException if results or point is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0.
     */
    List<E> getIntersections(List<E> results, Point point, int limit);

    /**
     * <p>Array version of {@link #getIntersections(List, Point, int)}.</p>
     *
     * @param results array of intersecting elements.
     * @param point intersecting point.
     * @param limit max number of elements.
     * @return results.
     * @throws NullPointerException if results or point is null.
     * @throws IllegalArgumentException if limit {@literal <}= 0 or the given array's length is {@literal <} limit.
     */
    E[] getIntersections(E[] results, Point point, int limit);

    /**
     * <p>Adds a {@code Bounds}-element pair.</p>
     *
     * @param bounds element's simplified volume.
     * @param element to partition.
     * @return true if the pair was successfully added, false if already in the partition.
     * @throws NullPointerException if bounds or element is null.
     * @throws IllegalArgumentException if the bounds-element pair is disallowed by the partition.
     */
    boolean add(Bounds bounds, E element);

    /**
     * <p>Gets the element paired with the given {@code Bounds}.</p>
     *
     * @param bounds element's representative volume.
     * @return element, or null if bounds was not in the tree.
     * @throws NullPointerException if bounds is null.
     */
    E getElement(Bounds bounds);

    /**
     * <p>Removes a {@code Bounds} and its element.</p>
     *
     * @param bounds element's simplified volume.
     * @return element associated with bounds, or null if nothing was removed.
     * @throws NullPointerException if bounds is null.
     */
    E remove(Bounds bounds);

    /**
     * <p>Updates a {@code Bounds}' position in the hierarchy should its position be invalid.</p>
     *
     * @param bounds element's simplified volume.
     * @return true if corrective operations were done.
     * @throws NullPointerException if bounds is null.
     */
    boolean update(Bounds bounds);

    /**
     * <p>Updates all {@code Bounds}' positions in the hierarchy should their positions be invalid.</p>
     *
     * @return true if any corrective operations were done.
     */
    boolean update();

    /**
     * <p>Checks if there is a {@code Bounds} {@code b} in the partition such that the given {@code Bounds}
     * {@code g} is equivalent in the expression {@code b.equals(g)}.</p>
     *
     * @param bounds element's simplified volume.
     * @return true if there is an equivalent bounds in the partition.
     * @throws NullPointerException if bounds is null.
     */
    boolean contains(Bounds bounds);

    /**
     * <p>Checks if there is an element {@code e0} in the partition such that the given element {@code e1} is
     * equivalent in the expression {@code e0.equals(e1)}.</p>
     *
     * @param element to search for.
     * @return true if there is an equivalent element in the partition.
     * @throws NullPointerException if element is null.
     */
    boolean containsElement(E element);

    /**
     * <p>Removes all {@code Bounds} elements.</p>
     */
    void clear();

    /**
     * <p>Gets the number of {@link Bounds}-element pairs in the partition.</p>
     *
     * @return count.
     */
    int size();

    /**
     * <p>Checks if the {@link Bounds}-element pairs count is 0.</p>
     *
     * @return true if {@code size()} == 0.
     */
    default boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * <p>Returns true if either the given object is the same tree instance or the following conditions are met.</p>
     *
     * <ul>
     *     <li>The given object is also a {@code SpacePartitioningTree} and both trees have the same size.</li>
     *     <li>Each {@code Bounds}-element pair has a corresponding pair in the other tree whose {@code Bounds} and
     *     element are equivalent in the following expression
     *     {@code bounds.equals(otherBounds) && element.equals(otherElement)}.</li>
     * </ul>
     *
     * @param object to test for equality.
     * @return true if equal to this tree.
     */
    @Override
    boolean equals(Object object);

    /**
     * <p>The hash code is computed as if by the following.</p>
     *
     * <pre>
     *     <code>
     *         for (pair : content) {
     *             int pairHash = initial * 31 + pair.bounds.hashCode();
     *             pairHash = 31 * pairHash + pair.element.hashCode();
     *
     *             treeHash = 31 * treeHash + pairHash;
     *         }
     *     </code>
     * </pre>
     *
     * @return hash code for the tree.
     */
    @Override
    int hashCode();
}
