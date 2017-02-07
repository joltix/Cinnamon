package com.cinnamon.utils;

import java.util.Arrays;

/**
 * <p>
 *     Recursive merge sort.
 * </p>
 *
 *
 */
public class Merge<E> extends Sort<E>
{
    /**
     * <p>Constructs a Merge with a {@link Comparison} for establishing an
     * ordering between two objects while sorting.</p>
     *
     * @param comparison Comparison.
     * @throws IllegalArgumentException if the given Comparison is null.
     */
    public Merge(Comparison<E> comparison)
    {
        super(comparison);
    }

    /**
     * <p>Sorts an array of Objects.</p>
     *
     * @param objects array.
     */
    @Override
    public void sort(E[] objects)
    {
        sort(objects, Arrays.copyOf(objects, objects.length));
    }

    /**
     * <p>Sorts an array of Objects using a secondary <i>"swap"</i>array for an
     * increase in speed for sorting implementation.</p>
     *
     * <p>The swap array must be a clone of the array to be sorted. That is,
     * both arrays must be of the same length and the same values in the same
     * indices. However, the two arrays must not be the same instance.</p>
     *
     * <p>This method does not check if the contents of the two arrays are
     * equivalent. In such a case, sorting may fail.</p>
     *
     * @param objects array to sort.
     * @param swap secondary array.
     * @throws IllegalArgumentException if both arrays are the same instance
     * or if the lengths are not the same.
     */
    public void sort(E[] objects, E[] swap)
    {
        sort(objects, swap, 0, objects.length);
    }

    /**
     * <p>Sorts a subsection of an array of objects using a secondary
     * <i>"swap"</i>array for a speed increase in the sorting implementation
     * .</p>
     *
     * <p>The swap array must be a clone of the array to be sorted. That is,
     * both arrays must be of the same length and the same values in the same
     * indices. However, both arrays must not be the same instance.</p>
     *
     * <p>This method does not check if the contents of the two arrays are
     * equivalent. In such a case, sorting may fail.</p>
     *
     * @param objects array to sort.
     * @param swap secondary array.
     * @param start index to begin sorting.
     * @param end index to stop sorting.
     * @throws IllegalArgumentException if both arrays are the same instance
     * or if the lengths are not the same, or the start and end indices are
     * beyond the array's range or the start index >= end index.
     */
    public void sort(E[] objects, E[] swap, int start, int end)
    {
        // Make sure both arrays are different instances
        if (objects == swap) {
            throw new IllegalArgumentException("Swap and objects arrays must " +
                    "not be the same instance");
        }

        // Make sure the swap is big enough
        if (swap.length != objects.length) {
            throw new IllegalArgumentException("Swap array length must be >= " +
                    "(objects.length / 2)");
        }

        // Confirm start index is within the array's range
        if (start < 0 || start >= objects.length) {
            throw new IllegalArgumentException("Start index must be >= 0 and " +
                    "< the length of the array");
        }

        // Confirm end index is within the array's range
        if (end > objects.length || end < 1) {
            throw new IllegalArgumentException("End index must be >= 1 and <=" +
                    " the length of the array");
        }

        // Make sure start < end
        if (start >= end) {
            throw new IllegalArgumentException("Start index must be < end " +
                    "index");
        }

        final Comparison<E> cmp = getComparison();
        recurse(swap, objects, start, end, ((end - start) / 2) + start, cmp);
    }

    /**
     * <p>Recursively divides an array in half, sorting and merging halves
     * until the original array is fully sorted.</p>
     *
     * @param objects array to be sorted.
     * @param swap cloned array to swap objects between.
     * @param start inclusive start index of subsection.
     * @param end exclusive end index of subsection.
     * @param mid inclusive start index of second array.
     * @param cmp Comparison for judging a less than, equal to, and greater
     *            than order between objects.
     */
    private void recurse(E[] objects, E[] swap, int start, int end,
                         int mid, Comparison<E> cmp)
    {
        // Sorting the Drawables as base case when segment == 2
        if ((end - start) <= 2) {
            final E obj0 = objects[start];
            final E obj1 = objects[end - 1];
            if (cmp.compare(obj0, obj1) > 0) {
                swap[end - 1] = obj0;
                swap[start] = obj1;
            }
            return;
        }

        // Recursively sort right and left halves, respectively
        recurse(objects, swap, mid, end, ((end - mid) / 2) + mid, cmp);
        recurse(swap, objects, start, mid, ((mid - start) / 2) + start, cmp);

        // "Merge" (while sorting)
        merge(objects, swap, start, end, mid, cmp);
    }

    /**
     * <p>Merges a sorted subsection from one array to another. Sorting
     * takes place during the merge and so the "right" array contains a
     * sorted combination of the two halves.</p>
     *
     * @param left array containing a sorted subsection.
     * @param right array to be merged into containing a sorted subsection.
     * @param start start index.
     * @param end end index.
     * @param mid halfway index dividing array.
     * @param cmp Comparison for judging a less than, equal to, and greater
     *            than order between objects.
     */
    private void merge(E[] left, E[] right, int start, int end, int mid,
                       Comparison<E> cmp)
    {
        for (int i = start, x = start, y = mid; i < end; i++) {

            // If one half has been used up, fill remaining with other half
            if (x >= mid) {
                copy(right, y, right, i, end);
                break;
            } else if (y >= end) {
                copy(left, x, right, i, mid);
                break;
            } else {

                // Decide which half to take from
                final E objL = left[x];
                final E objR = right[y];
                if (cmp.compare(objL, objR) <= 0) {
                    right[i] = objL;
                    x++;
                } else {
                    right[i] = objR;
                    y++;
                }
            }
        }
    }

    /**
     * <p>Copies objects from one array to another.</p>
     *
     * @param from source array.
     * @param cursor0 source starting index.
     * @param to destination array.
     * @param cursor1 destination starting index.
     * @param length number of objects to copy.
     */
    protected void copy(E[] from, int cursor0, E[] to, int cursor1, int length)
    {
        for (int x = cursor0, y = cursor1; x < length; x++, y++) {
            to[y] = from[x];
        }
    }
}
