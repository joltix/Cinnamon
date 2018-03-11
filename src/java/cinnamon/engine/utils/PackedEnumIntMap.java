package cinnamon.engine.utils;

import cinnamon.engine.utils.IntMap.IntWrapper;

import java.util.*;

/**
 * <p>Read-only {@code IntMap} implementation storing a desired {@code Enum}'s values for O(log N) lookup. This class is
 * best for when the {@code int} constants to map are relatively far apart (e.g. 3 vs 7,721).</p>
 *
 * <b>Runtime</b>
 * <p>This class trades speed for memory, providing logarithmic time for {@code get(int)} whilst memory rises
 * proportional to the number of {@code enum} values.</p>
 *
 * @param <T> enum type.
 */
public final class PackedEnumIntMap<T extends Enum<T> & IntWrapper> implements IntMap<T>
{
    private final T[] mConstants;

    /**
     * <p>Constructs a {@code PackedEnumIntMap} from the {@code Class} object of the target {@code Enum}.</p>
     *
     * @param cls enum's class.
     * @throws NullPointerException if cls is null.
     * @throws IllegalArgumentException if at least two enum values share the same internal int.
     */
    @SuppressWarnings("unchecked")
    public PackedEnumIntMap(Class<T> cls)
    {
        if (cls == null) {
            throw new NullPointerException();
        }

        mConstants = cls.getEnumConstants();
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        // Measure index range
        for (final T value : mConstants) {
            final int internal = value.toInt();
            min = (min < internal) ? min : internal;
            max = (max > internal) ? max : internal;
        }

        final int offset = -min;
        final T[] seen = (T[]) new Enum[mConstants.length];
        for (final T value : mConstants) {

            final int i = value.toInt() + offset;
            final T old = seen[i];

            // Make sure all enums have unique mappings
            if (old == null) {
                seen[i] = value;
            } else {
                assert (old != value);

                throw new IllegalArgumentException("Enum \'" + value + "\' shares the same int as \'" + old + "\'");
            }
        }

        // Order by internal int
        Arrays.sort(mConstants, (o1, o2) ->
        {
            return Integer.compare(o1.toInt(), o2.toInt());
        });
    }

    @Override
    public T get(int value)
    {
        int begin = 0;
        int end = mConstants.length;
        int i = mConstants.length / 2;

        // Binary search
        while (true) {

            final T key = mConstants[i];
            final int internal = key.toInt();

            if (value == internal) {
                return key;

                // Reached leaf and no match
            } else if (end - begin == 0) {
                return null;

                // Search left subtree
            } else if (value < internal) {

                end = i;
                i = (end - begin) / 2;

                // Search right subtree
            } else {

                begin = i;
                i += (end - begin) / 2;
            }
        }
    }

    @Override
    public int size()
    {
        return mConstants.length;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
}
