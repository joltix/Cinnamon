package cinnamon.engine.utils;

import cinnamon.engine.utils.IntMap.IntWrapper;

/**
 * <p>Acts as a lookup with {@code int} keys. This interface should be implemented by those associating instances of
 * a class of elements with unique integer values and is useful for bridging a legacy code base's {@code int} constants
 * with constructs like {@code Enum}s.</p>
 *
 * <p>Elements must implement the accompanying {@code IntWrapper} interface to guarantee an externally visible
 * {@code int} value. The method {@code toInt()} must return a unique integer for each instance of an element type
 * stored in the {@code IntMap}.</p><br>
 *
 * <b>Map comparison</b>
 * <p>Unlike {@code Map} with {@code Integer} keys, this interface does not impose autoboxing and more cleanly allows
 * read-only behavior by not mandating write methods. In exchange, elements must implement {@code IntMap.IntWrapper}
 * .</p><br>
 *
 * @param <E> element type.
 */
public interface IntMap<E extends IntWrapper>
{
    /**
     * <p>Gets the element mapped to the given {@code int}.</p>
     *
     * @param value value.
     * @return element, or null if no element has the same internal {@code int}.
     */
    E get(int value);

    /**
     * <p>Gets the number of elements.</p>
     *
     * @return element count.
     */
    int size();

    /**
     * <p>This interface is meant to be implemented by classes serving as an alias for integer constants and as
     * elements for {@code IntMap}.</p>
     */
    interface IntWrapper
    {
        /**
         * <p>Gets the integer equivalent.</p>
         *
         * @return integer equivalent.
         */
        int toInt();
    }
}
