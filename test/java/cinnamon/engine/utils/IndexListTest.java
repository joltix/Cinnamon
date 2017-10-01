package cinnamon.engine.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

/**
 * <p>The following methods are not explicitly tested. For {@code get(int)}, its throwing of
 * {@code IndexOutOfBoundsException} is checked.</p>
 * <ul>
 *     <li>{@code get(int)}</li>
 *     <li>{@code size()}</li>
 *     <li>{@code isEmpty()}</li>
 *     <li>{@code forEach(Consumer<? super E>)}</li>
 *     <li>{@code spliterator()}</li>
 *     <li>{@code iterator().forEachRemaining(Consumer<? super E>)}</li>
 * </ul>
 */
public class IndexListTest
{
    private static final int ELEMENT_COUNT = 100_000;

    private Object[] mElements;

    private IndexList<Object> mIndex;

    @Before
    public void setUp()
    {
        mElements = new Object[ELEMENT_COUNT];
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            mElements[i] = new Object();
        }

        mIndex = new IndexList<>(10, 0.5f, mElements);
    }

    @After
    public void tearDown()
    {
        mIndex = null;
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testGetIndexOutOfBoundsException()
    {
        mIndex.get(-42);
    }

    @Test
    public void testAdd()
    {
        final Object obj = new Object();
        final int i = mIndex.add(obj);

        Assert.assertTrue(mIndex.get(i) == obj);
    }

    @Test (expected = NullPointerException.class)
    public void testAddNPE()
    {
        mIndex.add(null);
    }

    @Test
    public void testRemove()
    {
        final Object first = mIndex.get(0);
        final int size = mIndex.size();
        final Object removed = mIndex.remove(0);

        Assert.assertSame(first, removed);
        Assert.assertEquals(mIndex.size(), size - 1);
        Assert.assertNotSame(removed, mIndex.get(0));
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testRemoveIndexOutOfBoundsException()
    {
        mIndex.remove(-2);
    }

    @Test
    public void testReserve()
    {
        final Object subject = new Object();
        final int reservation = mIndex.reserve();
        final int i = mIndex.add(subject);

        Assert.assertEquals(reservation, i);
    }

    @Test
    public void testClear()
    {
        mIndex.clear();
        Assert.assertTrue(mIndex.isEmpty());
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        final Object[] objs = new Object[mElements.length - 1];
        System.arraycopy(mElements, 0, objs, 0, objs.length);
        final IndexList<Object> otherIndex = new IndexList<>(10, 0.5f, objs);

        Assert.assertFalse(mIndex.equals(otherIndex));
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertTrue(mIndex.equals(mIndex));
    }

    @Test
    public void testEqualsSymmetric()
    {
        final IndexList<Object> duplicate = new IndexList<>(10, 0.5f, mElements);

        Assert.assertTrue(mIndex.equals(duplicate));
        Assert.assertTrue(duplicate.equals(mIndex));
    }

    @Test
    public void testEqualsTransitive()
    {
        final IndexList<Object> duplicateA = new IndexList<>(10, 0.5f, mElements);
        final IndexList<Object> duplicateB = new IndexList<>(20, 0.25f, mElements);

        Assert.assertTrue(duplicateA.equals(duplicateB));
        Assert.assertTrue(duplicateA.equals(mIndex));
        Assert.assertTrue(duplicateB.equals(mIndex));
    }

    @Test
    public void testEqualsNull()
    {
        Assert.assertFalse(mIndex.equals(null));
    }

    @Test
    public void testHashCodeSame()
    {
        final IndexList<Object> sameHash = new IndexList<>(10, 0.5f, mElements);
        Assert.assertEquals(mIndex.hashCode(), sameHash.hashCode());
    }

    @Test
    public void testHashCodeDifferent()
    {
        final IndexList<Object> diffHash = new IndexList<>(10, 0.5f, mElements[0], mElements[2]);
        Assert.assertNotEquals(mIndex.hashCode(), diffHash.hashCode());
    }

    @Test (expected = CloneNotSupportedException.class)
    public void testCloneCloneNotSupportedException() throws CloneNotSupportedException
    {
        mIndex.clone();
    }

    @Test
    public void testIterator()
    {
        // For marking element as seen by iterator
        final boolean[] visits = new boolean[mElements.length];

        final Iterator<Object> iterator = mIndex.iterator();
        while (iterator.hasNext()) {
            final Object obj = iterator.next();
            Assert.assertNotNull(obj);

            for (int i = 0; i < visits.length; i++) {
                if (mElements[i] == obj) {
                    visits[i] = true;
                }
            }
        }

        // Confirm all visited during iteration
        for (final boolean visited : visits) {
            Assert.assertTrue(visited);
        }
    }

    @Test
    public void testIteratorRemove()
    {
        int size = mIndex.size();
        final Iterator<Object> iterator = mIndex.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            Assert.assertEquals(--size, mIndex.size());
        }
    }

    @Test (expected = IllegalStateException.class)
    public void testIteratorRemoveIllegalStateExceptionNext()
    {
        final Iterator<Object> iterator = mIndex.iterator();
        while (iterator.hasNext()) {
            iterator.remove();
        }
    }

    @Test (expected = IllegalStateException.class)
    public void testIteratorRemoveIllegalStateExceptionRemoveAgain()
    {
        final Iterator<Object> iterator = mIndex.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
            iterator.remove();
        }
    }
}
