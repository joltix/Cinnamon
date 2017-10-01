package cinnamon.engine.utils;

import org.junit.*;

import java.util.Iterator;

/**
 * <p>Constructors and the following methods are not explicitly tested.</p>
 * <ul>
 *     <li>{@code peek()}</li>
 *     <li>{@code compact()}</li>
 *     <li>{@code size()}</li>
 *     <li>{@code isEmpty()}</li>
 *     <li>{@code forEach(Consumer<? super Integer>)}</li>
 *     <li>{@code spliterator()}</li>
 *     <li>{@code iterator().forEachRemaining(Consumer<? super Integer>)}</li>
 * </ul>
 *
 * <p>The {@code Iterator} returned from {@code iterator()} tests most of its methods for typical use in
 * {@code testIterator()}. The test for {@code clone()} merely checks that {@code CloneNotSupportedException} is
 * thrown.</p>
 */
public class IntStackTest
{
    // Random number generator ceiling
    private static final int MAX = 1_000_000;
    private static final int SET_SIZE = 1_000;

    // Values added to stack during set up
    private int[] mIntegers;

    private IntStack mStack;

    @Before
    public void setUp()
    {
        mIntegers = TestTool.generateDistinctIntegers(-MAX, MAX, SET_SIZE);
        mStack = new IntStack();

        // Fill stack
        for (final int val : mIntegers) {
            mStack.push(val);
        }
    }

    @After
    public void tearDown()
    {
        mStack = null;
    }

    @Test
    public void testPush()
    {
        final int top = mStack.peek();

        // Choose new int different than stack's top
        int trace;
        while ((trace = TestTool.generateInteger(0, MAX, true)) == top) { }

        mStack.push(trace);
        Assert.assertEquals(trace, mStack.peek());
    }

    @Test (expected = IllegalStateException.class)
    public void testPeekIllegalStateException()
    {
        final IntStack stack = new IntStack();
        stack.peek();
    }

    @Test
    public void testPop()
    {
        final int top = mStack.peek();
        final int popped = mStack.pop();

        Assert.assertEquals(top, popped);
        Assert.assertNotEquals(top, mStack.peek());
    }

    @Test (expected = IllegalStateException.class)
    public void testPopIllegalStateException()
    {
        final IntStack stack = new IntStack();
        stack.pop();
    }

    @Test
    public void testClear()
    {
        Assert.assertFalse(mStack.isEmpty());
        mStack.clear();
        Assert.assertTrue(mStack.isEmpty());
    }

    @Test
    public void testCopy()
    {
        final IntStack stack = new IntStack();
        stack.copy(mStack);

        Assert.assertEquals(mStack.size(), stack.size());
        Assert.assertEquals(mStack.peek(), stack.peek());
        Assert.assertEquals(mStack, stack);
        Assert.assertEquals(mStack.hashCode(), stack.hashCode());
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNPE()
    {
        mStack.copy(null);
    }

    @Test
    public void testIterator()
    {
        final Iterator<Integer> iter = mStack.iterator();
        int iterated = 0;

        // Each value must be same as reference
        for (int i = mIntegers.length - 1; i >= 0 && iter.hasNext(); i--, iterated++) {
            Assert.assertEquals((Integer) mIntegers[i], iter.next());
        }

        // Make sure amount of values iterated through matches all added
        Assert.assertEquals(mIntegers.length, iterated);
    }

    @Test (expected = UnsupportedOperationException.class)
    public void testIteratorRemoveUnsupportedOperationException()
    {
        mStack.iterator().remove();
    }

    @Test
    public void testForEach()
    {
        // For marking ints as accepted
        final boolean[] accepts = new boolean[mIntegers.length];

        mStack.forEach((Integer obj) -> {
            for (int i = 0; i < mIntegers.length; i++) {
                if (mIntegers[i] == obj) {
                    accepts[i] = true;
                }
            }
        });

        // Confirm all accepted
        for (int i = 0; i < mIntegers.length; i++) {
            Assert.assertTrue(accepts[i]);
        }
    }

    @Test (expected = NullPointerException.class)
    public void testForEachNPE()
    {
        mStack.forEach(null);
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        final IntStack duplicate = new IntStack();
        for (int i = 1; i < mIntegers.length; i++) {
            duplicate.push(mIntegers[i]);
        }
        duplicate.push(mIntegers[0]);

        Assert.assertFalse(mStack.equals(duplicate));
    }

    @Test
    public void testEqualsSymmetric()
    {
        final IntStack duplicate = new IntStack(10, 0.5f, mIntegers);
        Assert.assertTrue(mStack.equals(duplicate));
        Assert.assertTrue(duplicate.equals(mStack));
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertTrue(mStack.equals(mStack));
    }

    @Test
    public void testEqualsTransitive()
    {
        final IntStack duplicateA = new IntStack(10, 0.5f, mIntegers);
        final IntStack duplicateB = new IntStack(10, 0.5f, mIntegers);

        Assert.assertTrue(mStack.equals(duplicateA));
        Assert.assertTrue(duplicateA.equals(duplicateB));
        Assert.assertTrue(mStack.equals(duplicateB));
    }

    @Test
    public void testEqualsNull()
    {
        //noinspection ConstantConditions
        Assert.assertFalse(mStack.equals(null));
    }

    @Test
    public void testEqualsDifferentClass()
    {
        Assert.assertFalse(mStack.equals(new Object()));
    }

    @Test
    public void testHashCodeSame()
    {
        final IntStack duplicate = new IntStack(10, 0.5f, mIntegers);
        Assert.assertEquals(mStack.hashCode(), duplicate.hashCode());
    }

    @Test
    public void testHashCodeDifferent()
    {
        // Change the 6th value
        if (mIntegers[5] == Integer.MAX_VALUE) {
            mIntegers[5]--;
        } else {
            mIntegers[5]++;
        }

        final IntStack duplicate = new IntStack(10, 0.5f, mIntegers);
        Assert.assertNotEquals(mStack.hashCode(), duplicate.hashCode());
    }

    @Test (expected = CloneNotSupportedException.class)
    public void testCloneCloneNotSupportedException() throws CloneNotSupportedException
    {
        mStack.clone();
    }
}
