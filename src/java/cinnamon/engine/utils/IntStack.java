package cinnamon.engine.utils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * <p>Array-backed stack of primitive {@code ints}. This class is meant to replace stacking {@code ints} with generic
 * classes when operations are expected to be frequent and repeated instantiation through autoboxing at high
 * frequency is to be avoided.</p>
 *
 * <p>For compatibility with the {@code Iterable} interface, the {@code Iterator} returned by this class'
 * {@code iterator()} method autoboxes.</p>
 *
 * <b>Growth</b>
 * <p>The {@code IntStack}'s internal array grows automatically when full and the amount of growth can be controlled by
 * the growth factor passed during construction. However, the array is not automatically shrunken. To do so,
 * {@code compact()} must manually be called in order to allow the potentially blocking operation to execute at
 * appropriate times.</p>
 */
public final class IntStack implements Copier<IntStack>, Iterable<Integer>
{
    private static final int DEFAULT_INITIAL_CAPACITY = 10;
    private static final float DEFAULT_GROWTH = 0.75f;

    private final float mGrowth;
    private int[] mValues;

    // Top grows towards right
    private int mTop = -1;
    private int mSize = 0;

    /**
     * <p>Constructs an {@code IntStack}.</p>
     */
    public IntStack()
    {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_GROWTH);
    }

    /**
     * <p>Constructs an {@code IntStack} with the same values and growth factor as another.</p>
     *
     * @param stack to copy.
     * @throws NullPointerException if stack is null.
     */
    public IntStack(IntStack stack)
    {
        copy(stack);
        mGrowth = stack.mGrowth;
    }

    /**
     * <p>Constructs an {@code IntStack} with a specific initial capacity and normalized growth factor.</p>
     *
     * @param initialCapacity initial capacity before growth.
     * @param growth factor to grow capacity by (must be {@literal >} 0 and {@literal <}= 1).
     * @throws IllegalArgumentException if initial capacity is {@literal <}= 0 or growth is {@literal <}= 0 or growth
     * is {@literal >} 1.
     */
    public IntStack(int initialCapacity, float growth)
    {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be > 0, initialCapacity: " + initialCapacity);
        }
        if (growth <= 0f || growth > 1f) {
            throw new IllegalArgumentException("Growth factor must be > 0 and <= 1, growth: " + growth);
        }

        mValues = new int[initialCapacity];
        mGrowth = growth;
    }

    /**
     * <p>Constructs an {@code IntStack} with a specific initial capacity, normalized growth factor, and values. If
     * the given initial capacity is smaller than the number of given values, the value count is instead taken as the
     * initial capacity.</p>
     *
     * @param initialCapacity initial capacity before growth.
     * @param growth factor to grow capacity by (must be {@literal >} 0 and {@literal <}= 1).
     * @param values starting stack content.
     * @throws IllegalArgumentException if initial capacity is {@literal <}= 0, growth is {@literal <}= 0 or
     * {@literal >} 1, or the number of starting values is 0.
     */
    public IntStack(int initialCapacity, float growth, int...values)
    {
        this((initialCapacity > values.length) ? initialCapacity : values.length, growth);

        if (values.length == 0) {
            throw new IllegalArgumentException("Initial value count must be > 0, values.length: " + values.length);
        }

        mSize = values.length;
        mTop = mSize - 1;

        System.arraycopy(values, 0, mValues, 0, mSize);
    }

    /**
     * <p>Adds an {@code int} to the top.</p>
     *
     * @param value value.
     */
    public void push(int value)
    {
        // Make sure there's space
        if (mValues.length == mSize) {
            growArray();
        }

        mSize++;
        mValues[++mTop] = value;
    }

    /**
     * <p>Gets the most recently pushed {@code int}.</p>
     *
     * @return top.
     * @throws IllegalStateException if called when the stack is empty.
     */
    public int peek()
    {
        if (mSize == 0) {
            throw new IllegalStateException("Cannot peek anything from an empty stack");
        }

        return mValues[mTop];
    }

    /**
     * <p>Removes the most recently pushed {@code int}.</p>
     *
     * @return top.
     * @throws IllegalStateException if called when the stack is empty.
     */
    public int pop()
    {
        if (mSize == 0) {
            throw new IllegalStateException("Cannot pop anything from an empty stack");
        }

        mSize--;
        return mValues[mTop--];
    }

    /**
     * <p>Checks if the stack has any {@code ints} left.</p>
     *
     * @return true if size is 0.
     */
    public boolean isEmpty()
    {
        return mSize == 0;
    }

    /**
     * <p>Gets the number of {@code ints} in the stack.</p>
     *
     * @return value count.
     */
    public int size()
    {
        return mSize;
    }

    /**
     * <p>Removes all {@code ints}.</p>
     */
    public void clear()
    {
        mTop = -1;
        mSize = 0;
    }

    /**
     * <p>Attempts to resize the internal array to tightly fit the stack's {@code ints}.</p>
     */
    public void compact()
    {
        if (mSize == mValues.length) {
            return;
        }

        final int[] compact = new int[mSize];
        System.arraycopy(mValues, 0, compact, 0, mSize);
        mValues = compact;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The given {@code IntStack}'s growth factor is not copied.</p>
     *
     * @param object to copy.
     */
    @Override
    public void copy(IntStack object)
    {
        if (object == null) {
            throw new NullPointerException();
        }

        mTop = object.mTop;
        mSize = object.mSize;

        // Copy values
        if (mValues.length < object.mValues.length) {
            mValues = Arrays.copyOfRange(object.mValues, 0, mSize);
        } else {
            System.arraycopy(object.mValues, 0, mValues, 0, mSize);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned {@code Iterator} will iterate from the top of the stack going to the bottom and does not support
     * removal. Calling its {@code remove()} method will throw an {@code UnsupportedOperationException}.</p>
     */
    @Override
    public Iterator<Integer> iterator()
    {
        return new StackIterator();
    }

    /**
     * <p>Performs the given {@code Consumer<? super Integer>} on each {@code int} starting at the top and going
     * down to the bottom.</p>
     *
     * @param action to perform on each integer.
     * @throws NullPointerException if action is null.
     */
    @Override
    public void forEach(Consumer<? super Integer> action)
    {
        if (action == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < mSize; i++) {
            action.accept(mValues[i]);
        }
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + ((Integer) mSize).hashCode();

        for (int i = 0; i < mSize; i++) {
            hash = 31 * hash + mValues[i];
        }

        return hash;
    }

    /**
     * <p>Returns true if either the given object is the same {@code IntStack} or the following conditions are met.</p>
     *
     * <ul>
     *     <li>The given object is also an {@code IntStack}.</li>
     *     <li>Both {@code IntStacks} have the same size.</li>
     *     <li>Each {@code int} {@code a} is equivalent to an {@code int} {@code b} in the other stack such that
     *     {@code a.equals(b)}.</li>
     *     <li>Both {@code ints} in an equivalent pair have the same positions in their respective stacks.</li>
     * </ul>
     *
     * @param obj to test for equality.
     * @return true if equal to this stack.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != IntStack.class) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final IntStack stack = (IntStack) obj;
        if (stack.mSize != mSize) {
            return false;
        }

        // Check each value for equality
        for (int i = 0; i < mSize; i++) {
            if (stack.mValues[i] != mValues[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }

    /**
     * <p>Increases the size of the {@code int} array by the growth factor provided to the constructor.</p>
     */
    private void growArray()
    {
        final int[] larger = new int[(int) (mSize * (1f + mGrowth))];
        System.arraycopy(mValues, 0, larger, 0, mSize);
        mValues = larger;
    }

    /**
     * <p>Iterates through all {@code ints} in the {@code IntStack} from the top going to the bottom.</p>
     */
    private class StackIterator implements Iterator<Integer>
    {
        private int mCursor = mTop;

        @Override
        public boolean hasNext()
        {
            return mCursor >= 0;
        }

        @Override
        public Integer next()
        {
            return IntStack.this.mValues[mCursor--];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
