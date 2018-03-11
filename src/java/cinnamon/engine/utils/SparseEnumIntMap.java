package cinnamon.engine.utils;


import cinnamon.engine.utils.IntMap.IntWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Read-only {@code IntMap} implementation storing a desired {@code Enum}'s values for O(1) lookup. This class is
 * best for when the {@code int} constants to map are not close to the 0 index but are relatively close to each other
 * (e.g. 343 vs 350).</p>
 *
 * <b>Runtime</b>
 * <p>This class trades memory for speed, providing constant time for {@code get(int)} whilst memory rises
 * proportional to the number of unused {@code int} values in-between those mapped. In the simplest case, for a
 * {@code SparseEnumIntMap} with two {@code Enum}s represented by 10 and 80, more memory is used than a
 * {@code SparseEnumIntMap} whose two {@code Enum}s are mapped to 34 and 37.</p>
 *
 * @param <T> enum type.
 */
public final class SparseEnumIntMap<T extends Enum<T> & IntWrapper> implements IntMap<T>
{
    private final T[] mConstants;
    private final int[] mRedirect;
    private final int mOffset;

    /**
     * <p>Constructs a {@code SparseEnumIntMap} from the {@code Class} object of the target {@code Enum}.</p>
     *
     * @param cls enum's class.
     * @throws NullPointerException if cls is null.
     * @throws IllegalArgumentException if at least two enum values share the same internal int.
     */
    @SuppressWarnings("unchecked")
    public SparseEnumIntMap(Class<T> cls)
    {
        if (cls == null) {
            throw new NullPointerException();
        }

        // Leave last index empty
        final T[] constants = cls.getEnumConstants();
        mConstants = (T[]) new Enum[constants.length + 1];
        System.arraycopy(constants, 0, mConstants, 0, constants.length);

        final Map<Integer, T> seen = new HashMap<>(constants.length);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        // Measure index range and check for enums with the same ints
        for (final T key : constants) {

            final int i = key.toInt();
            min = (i < min) ? i : min;
            max = (i > max) ? i : max;

            // Make sure all enums have unique mappings
            if (seen.containsKey(i)) {
                throw new IllegalArgumentException("Enum \'" + key + "\' shares the same int as \'" +
                        seen.get(i) + "\'");
            } else {
                seen.put(i, key);
            }
        }

        mOffset = -min;
        mRedirect = new int[max - min + 1];
        mapRedirectValues(constants);
    }

    @Override
    public T get(int value)
    {
        final int index = value + mOffset;

        if (index < 0 || index >= mRedirect.length) {
            return null;
        }

        return mConstants[mRedirect[index]];
    }

    @Override
    public int size()
    {
        return mConstants.length - 1;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Populates the sparse array to point to each {@code Enum} value's internal {@code int}. Unused indices in
     * the sparse array will point to the last slot in the dense array (which should be null).</p>
     *
     * @param constants packed constants.
     */
    private void mapRedirectValues(T[] constants)
    {
        mRedirect[mRedirect.length - 1] = 0;

        // Unused indices map to null slot
        final int unused = mConstants.length - 1;
        for (int i = 0; i < mRedirect.length; i++) {
            mRedirect[i] = unused;
        }

        // Map internal value to ordinals
        for (final T key : constants) {
            mRedirect[key.toInt() + mOffset] = key.ordinal();
        }
    }
}
