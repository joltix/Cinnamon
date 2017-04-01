package com.cinnamon.utils;

/**
 * <p>
 *     Array-backed list that chooses an object's index when added and reuses indices as they become available
 *     (e.g. when objects are removed). Unlike the usual list, objects cannot be inserted to a specific index and no
 *     particular order is guaranteed.
 * </p>
 *
 * @param <E> Objects to store.
 */
public class IndexList<E>
{
    // Object pool for indices
    private final PooledQueue<Integer> mIndexPool = new PooledQueue<Integer>();

    // Growth factor when increasing array capacity (0f - 1f)
    private final float mGrowth;

    // Lookup array and obj count
    private E[] mObjs;
    private int mSize;

    /**
     * <p>Constructor for an IndexList with an initial capacity and a normalized growth factor (0.0 - 1.0).</p>
     *
     * @param load initial capacity.
     * @param growth growth factor.
     */
    @SuppressWarnings("unchecked")
    public IndexList(int load, float growth)
    {
        mObjs = (E[]) new Object[load];
        mGrowth = growth;
    }

    /**
     * <p>Gets the object at a given index.</p>
     *
     * @param index index.
     * @return object.
     */
    public final E get(int index)
    {
        return mObjs[index];
    }

    /**
     * <p>Adds an object to the IndexList and returns the index for lookup.</p>
     *
     * @param object object.
     * @return object index.
     */
    public int add(E object)
    {
        final int index = getAvailableIndex();
        mObjs[index] = object;
        return index;
    }

    /**
     * <p>Removes the object at a given index.</p>
     *
     * @param index index.
     * @return object.
     */
    public final E remove(int index)
    {
        // Remove object from lookup
        final E obj = mObjs[index];
        mObjs[index] = null;

        // Set index aside for reuse and update size
        mIndexPool.add(index);
        mSize--;
        return obj;
    }

    /**
     * <p>Removes all stored objects and any pending index assignments.</p>
     */
    public final void clear()
    {
        for (int i = 0, sz = size(); i < sz; i++) {
            mObjs[i] = null;
        }

        mSize = 0;
        mIndexPool.clear();
    }

    /**
     * <p>Retrieves an index which corresponds to a null entry.</p>
     *
     * <p>This method runs in amortized O(1)+ time. Dwindling array space may require allocating a larger array for O
     * (n) where n is the number of stored objects (size).</p>
     *
     * @return index.
     */
    protected final int getAvailableIndex()
    {
        // Grow the lookup area if out of space
        if (mSize >= mObjs.length) {
            increaseCapacity();
        }

        // Choose new index or reuse an old one if possible
        return (mIndexPool.isEmpty()) ? mSize++ : mIndexPool.poll();
    }

    /**
     * <p>Creates a new E[] with a size (1 + growth) times the current E[] length and copies the old array's contents
     * .</p>
     */
    @SuppressWarnings("unchecked")
    private void increaseCapacity()
    {
        // Create larger array and copy over all refs
        final int newSize = (int) ((1 + mGrowth) * mObjs.length);
        final E[] largerArr = (E[]) new Object[newSize];
        System.arraycopy(mObjs, 0, largerArr, 0, mSize);

        // Replace old lookup with larger one
        mObjs = largerArr;
    }

    /**
     * <p>Gets the number of indexed objects.</p>
     *
     * @return object count.
     */
    public final int size()
    {
        return mSize;
    }

    /**
     * <p>Checks if size is 0.</p>
     *
     * @return true if size == 0.
     */
    public final boolean isEmpty()
    {
        return mSize == 0;
    }
}
