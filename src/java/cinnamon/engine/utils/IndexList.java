package cinnamon.engine.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>Array-backed list that chooses an element's index upon adding. Calling {@code get(int)} with an index that is
 * larger than the {@code IndexList} returns null to signal that the index is unused. This class is designed for
 * elements that need an id as well as storage for lookup.</p>
 *
 * <p>While adding an element returns the index it was assigned, sometimes the index is required during the element's
 * construction. In such cases, the index to-be-assigned can first be retrieved through {@code reserve()}.</p>
 *
 * <b>Recycled Indices</b>
 * <p>When an element is removed from the {@code IndexList}, its index is set aside and prioritized for reuse in
 * future calls to {@code add(E)}. This helps keep elements packed to avoid having to skip too many null entries
 * during iteration. However, it should be noted that removing large numbers of elements without adding can still
 * result in many corresponding null entries, affecting iteration performance.</p>
 *
 * @param <E> elements to store.
 */
public final class IndexList<E> implements Iterable<E>
{
    private static final int DEFAULT_CAPACITY = 10;

    private static final float DEFAULT_GROWTH = 0.85f;

    // Reusable ids
    private final IntStack mIndexPool = new IntStack();

    // Growth factor when increasing array capacity (0f - 1f)
    private final float mGrowth;

    // Lookup array and obj count
    private E[] mElements;
    private int mSize = 0;

    // Next index that has never been used
    private int mNextAvailableIndex = 0;

    /**
     * <p>Constructs an {@code IndexList}.</p>
     */
    public IndexList()
    {
        this(DEFAULT_CAPACITY, DEFAULT_GROWTH);
    }

    /**
     * <p>Constructs an {@code IndexList} with a specific initial capacity and a normalized growth factor.</p>
     *
     * @param capacity initial capacity.
     * @param growth growth factor.
     * @throws IllegalArgumentException if capacity {@literal <}= 0 or growth is {@literal <}= 0 or growth is
     * {@literal >} 1.
     */
    @SuppressWarnings("unchecked")
    public IndexList(int capacity, float growth)
    {
        // Validate capacity
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        // Validate growth
        if (growth < 0f || growth > 1f) {
            throw new IllegalArgumentException("Growth factor must be within range 0 - 1: " + growth);
        }

        mElements = (E[]) new Object[capacity];
        mGrowth = growth;
    }

    /**
     * <p>Constructs an {@code IndexList} with a specific initial capacity, a normalized growth factor, and starting
     * elements.</p>
     *
     * @param capacity initial capacity.
     * @param growth growth factor.
     * @param elements starting content.
     * @throws IllegalArgumentException if capacity {@literal <}= 0 or growth is {@literal <}= 0 or growth is
     * {@literal >} 1.
     */
    @SuppressWarnings("unchecked")
    public IndexList(int capacity, float growth, E...elements)
    {
        // Validate capacity
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0, capacity: " + capacity);
        }
        // Validate growth
        if (growth <= 0f || growth > 1f) {
            throw new IllegalArgumentException("Growth factor must be > 0 and <= 1, growth: " + growth);
        }

        mElements = (E[]) new Object[(capacity < elements.length) ? elements.length : capacity];
        mGrowth = growth;

        // Copy elements over
        for (int i = 0; i < elements.length; i++) {
            mElements[i] = elements[i];
            mSize++;
        }
    }

    /**
     * <p>Gets the element at a given index.</p>
     *
     * @param index index.
     * @return element, or null if index is unused.
     * @throws IndexOutOfBoundsException if index {@literal <} 0.
     */
    public E get(int index)
    {
        // Prevent IndexOutOfBoundsException due to end-of-list
        if (index >= mElements.length) {
            return null;
        }

        return mElements[index];
    }

    /**
     * <p>Adds an element and returns its index. Null is not permitted.</p>
     *
     * @param element to add.
     * @return index.
     * @throws NullPointerException if element is null.
     */
    public int add(E element)
    {
        if (element == null) {
            throw new NullPointerException();
        }

        final int index = getAvailableIndex();
        mElements[index] = element;

        mSize++;
        return index;
    }

    /**
     * <p>Removes the element at a given index.</p>
     *
     * <p>After removing the element at the specified index, the index is made available for future calls to
     * {@link #add(Object)}.</p>
     *
     * @param index index.
     * @return element, or null if index was unused.
     * @throws IndexOutOfBoundsException if index {@literal <} 0.
     */
    public E remove(int index)
    {
        // Index referred to empty space
        final E element = get(index);
        if (element == null) {
            return null;
        }

        mElements[index] = null;
        mIndexPool.push(index);
        mSize--;

        return element;
    }

    /**
     * <p>Removes all elements and makes all indices available.</p>
     */
    public void clear()
    {
        // Remove all elements
        for (int i = 0, sz = size(); i < sz; i++) {
            mElements[i] = null;
        }
        mSize = 0;

        // Make all indices available
        mIndexPool.clear();
        mNextAvailableIndex = 0;
    }

    /**
     * <p>Sets aside an index to be used during the next call to {@link #add(Object)})}.</p>
     *
     * <p>This method is provided for elements whose index is needed prior to being added. The index
     * returned by this method will change after subsequent calls to {@link #add(Object)}. Furthermore, the
     * reserved index will be the same as that returned by the next call to {@link #add(Object)}.</p>
     *
     * @return future index.
     */
    public int reserve()
    {
        return readAvailableIndex();
    }

    /**
     * <p>The returned {@code Iterator} supports {@link Iterator#remove()}.</p>
     *
     * @return an {@code Iterator}.
     */
    @Override
    public Iterator<E> iterator()
    {
        return new IndexIterator();
    }

    /**
     * <p>Gets the number of elements.</p>
     *
     * @return element count.
     */
    public int size()
    {
        return mSize;
    }

    /**
     * <p>Checks if there are no elements.</p>
     *
     * @return true if size == 0.
     */
    public boolean isEmpty()
    {
        return mSize == 0;
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + mIndexPool.hashCode();
        hash = 31 * hash + Arrays.hashCode(mElements);
        hash = 31 * hash + mSize;
        return hash;
    }

    /**
     * <p>Returns true if either the given object is the same {@code IndexList} or the following conditions are met.</p>
     *
     * <ul>
     *     <li>The given object is also an {@code IndexList}.</li>
     *     <li>Both {@code IndexLists} have the same size.</li>
     *     <li>Each element is equivalent to an element in the other {@code IndexList} through the element's
     *     {@code equals(Object)} implementation.</li>
     *     <li>Both elements in an equivalent pair have the same indices in their respective {@code IndexLists}.</li>
     * </ul>
     *
     * @param object to test for equality.
     * @return true if equal to this {@code IndexList}.
     */
    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != IndexList.class) {
            return false;
        } else if (object == this) {
            return true;
        }

        final IndexList index = (IndexList) object;
        if (index.mSize != mSize) {
            return false;
        }

        // Compare content
        final int sz = Math.min(mElements.length, index.mElements.length);
        for (int i = 0; i < sz; i++) {
            if (mElements[i] != index.mElements[i]) {
                return false;
            }
        }

        // Compare indices waiting to be reused
        return index.mIndexPool.equals(mIndexPool);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Retrieves an index that is currently not in use. This method prioritizes recycling previously used indices
     * before returning indices that have never been used.</p>
     *
     * <p>This method runs in amortized O(1)+ time. Dwindling array space may require allocating a larger array for
     * O(n) where n is the number of stored elements.</p>
     *
     * @return index.
     */
    private int getAvailableIndex()
    {
        // Grow the lookup area if out of space
        if (mSize >= mElements.length) {
            increaseCapacity();
        }

        // Choose new index or reuse an old one if possible
        return (mIndexPool.isEmpty()) ? mNextAvailableIndex++ : mIndexPool.pop();
    }

    /**
     * <p>Reads the index to be assigned during the next call to {@link #add(Object)}. Unlike
     * {@link #getAvailableIndex()}, calling this method does not change the index returned from subsequent calls.</p>
     *
     * @return next available index.
     */
    private int readAvailableIndex()
    {
        return (mIndexPool.isEmpty()) ? mNextAvailableIndex : mIndexPool.peek();
    }

    /**
     * <p>Creates a new array with a length of (1 + growth) times the current array length and copies the old array's
     * contents.</p>
     */
    @SuppressWarnings("unchecked")
    private void increaseCapacity()
    {
        // Create larger array and copy over all refs
        final int newSize = (int) ((1 + mGrowth) * mElements.length);
        final E[] largerArr = (E[]) new Object[newSize];
        System.arraycopy(mElements, 0, largerArr, 0, mSize);

        // Replace old lookup with larger one
        mElements = largerArr;
    }

    /**
     * <p>{@code Iterator} for all non-null elements in the {@code IndexList}. If there are any elements yet to be
     * visited, calling {@code next()} will skip indices referring to null in the {@code IndexList} and will return
     * the next non-null element. {@code remove()} is supported.
     */
    private class IndexIterator implements Iterator<E>
    {
        // Expected number of objs to iterate through
        private int mUnseen = IndexList.this.size();

        private boolean mNextCalled = false;
        private boolean mRemoveCalled = false;

        // Current index
        private int mCursor = -1;

        @Override
        public boolean hasNext()
        {
            return mUnseen > 0;
        }

        @Override
        public E next()
        {
            if (mUnseen <= 0) {
                throw new NoSuchElementException();
            }

            // Seek next non-null entry
            while (mElements[++mCursor] == null) { }

            mUnseen--;
            mNextCalled = true;
            mRemoveCalled = false;
            return mElements[mCursor];
        }

        @Override
        public void remove()
        {
            if (!mNextCalled) {
                throw new IllegalStateException("Iterator's next() must be called before remove()");
            }
            if (mRemoveCalled) {
                throw new IllegalStateException("Can only call remove() once per call to Iterator's next()");
            }

            mNextCalled = false;
            mRemoveCalled = true;

            // Remove
            IndexList.this.mIndexPool.push(mCursor);
            mElements[mCursor] = null;
            mSize--;
        }
    }
}
