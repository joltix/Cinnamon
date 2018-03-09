package cinnamon.engine.utils;

/**
 * <p>Describes a method of reading elements in the style of a 2D array.</p>
 *
 * @param <E> element type.
 */
public interface Table<E>
{
    /**
     * <p>Gets the element at the intersection of a column and row.</p>
     *
     * @param column column.
     * @param row row.
     * @return element.
     * @throws IndexOutOfBoundsException if column {@literal <} 0, column {@literal >}= width, row {@literal <} 0, or
     * row {@literal >}= height.
     */
    E get(int column, int row);

    /**
     * <p>Gets the number of columns.</p>
     *
     * @return column count.
     */
    int width();

    /**
     * <p>Gets the number of rows.</p>
     *
     * @return row count.
     */
    int height();
}
