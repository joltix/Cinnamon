package cinnamon.engine.utils;

import java.util.Arrays;

/**
 * <p>Stores elements as an array of queues with matching sizes. This class is meant for keeping track of an order
 * of elements.</p>
 *
 * <p>Unlike traditional queues, each element added removes its oldest and each queue has the same fixed capacity.
 * Because of this, {@code FixedQueueArray}s may not be space-efficient when the capacity is large and queue indices
 * are unused.</p>
 *
 * @param <V> element type.
 */
public final class FixedQueueArray<V> implements Table<V>
{
    private final V[] mElements;
    private final int mQueueCapacity;
    private final int mQueueCount;

    private int mHash;
    private boolean mUpdateHash = true;

    /**
     * <p>Constructs a {@code FixedQueueArray} from a {@code Table}. Element queues and orders are preserved.</p>
     *
     * @param table to copy.
     * @throws NullPointerException if table is null.
     */
    public FixedQueueArray(Table<V> table)
    {
        this(table.height(), table.width());

        // Copy all elements over
        for (int y = table.height() - 1; y >= 0; y--) {
            for (int x = table.width() - 1; x >= 0; x--) {

                final V element = table.get(x, y);
                if (element != null) {
                    add(y, element);
                }
            }
        }
    }

    /**
     * <p>Constructs a {@code FixedQueueArray} to track a {@code queueSize} number of elements per queue.</p>
     *
     * @param queueCount number of queues.
     * @param queueCapacity max size per queue.
     * @throws IllegalArgumentException if queueCount {@literal < 1} or queueSize {@literal < 1}.
     */
    @SuppressWarnings("unchecked")
    public FixedQueueArray(int queueCount, int queueCapacity)
    {
        if (queueCount < 1) {
            throw new IllegalArgumentException("Number of queues cannot be < 1, given: " + queueCount);
        }
        if (queueCapacity < 1) {
            throw new IllegalArgumentException("Queue capacity cannot be < 1, given: " + queueCapacity);
        }

        mElements = (V[]) new Object[queueCount * queueCapacity];
        mQueueCapacity = queueCapacity;
        mQueueCount = queueCount;
    }

    /**
     * <p>Adds an element to a queue and removes the oldest.</p>
     *
     * @param row queue index.
     * @param element element.
     * @return oldest element.
     * @throws IndexOutOfBoundsException if index {@literal < 0} or {@literal >=} the number of queues.
     */
    public V add(int row, V element)
    {
        if (row < 0 || row >= mElements.length / mQueueCapacity) {
            throw new IndexOutOfBoundsException("Row must be >= 0 and < queue count, given: " + row);
        }

        final int ord = row * mQueueCapacity;
        final V removed = mElements[ord + mQueueCapacity - 1];

        // Shift elements down
        System.arraycopy(mElements, ord, mElements, ord + 1, mQueueCapacity - 1);
        mElements[ord] = element;

        mUpdateHash = true;
        return removed;
    }

    @Override
    public V get(int column, int row)
    {
        if (column < 0 || column >= width()) {
            throw new IndexOutOfBoundsException("Column must be >= 0 and < width, actual: " + column);
        }
        if (row < 0 || row >= height()) {
            throw new IndexOutOfBoundsException("Row must be >= 0 and < height, actual: " + row);
        }

        return mElements[row * mQueueCapacity + column];
    }

    /**
     * <p>Removes all elements.</p>
     */
    public void clear()
    {
        for (int i = 0; i < mElements.length; i++) {
            mElements[i] = null;
        }
    }

    /**
     * <p>Removes all elements from the row.</p>
     *
     * @param row row.
     * @throws IndexOutOfBoundsException if row {@literal <} 0 or row {@literal >}= height.
     */
    public void clear(int row)
    {
        if (row < 0 || row >= height()) {
            throw new IndexOutOfBoundsException("Row must be >= 0 and < height, given: " + row);
        }

        final int start = mQueueCapacity * row;
        final int end = start + mQueueCapacity;

        for (int i = start; i < end; i++) {
            mElements[i] = null;
        }
    }

    @Override
    public int width()
    {
        return mQueueCapacity;
    }

    @Override
    public int height()
    {
        return mQueueCount;
    }

    @Override
    public int hashCode()
    {
        if (mUpdateHash) {
            int hash = 17 * 31 + Arrays.deepHashCode(mElements);
            hash = 31 * hash + Integer.hashCode(width());
            mHash = 31 * hash + Integer.hashCode(height());

            mUpdateHash = false;
        }

        return mHash;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final FixedQueueArray<V> other = (FixedQueueArray<V>) obj;

        // Compare each element
        for (int i = 0; i < mElements.length; i++) {
            final V element = mElements[i];
            final V otherElement = other.mElements[i];

            if (element == null || otherElement == null) {
                if (element != otherElement) {
                    return false;
                }

            } else if (!element.equals(otherElement)) {
                return false;
            }
        }

        return mQueueCapacity == other.mQueueCapacity &&
                mElements.length == other.mElements.length;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
}
