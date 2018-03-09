package cinnamon.engine.utils;

import cinnamon.engine.utils.FixedQueueArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Random;

public class FixedQueueArrayTest
{
    // Elements
    private static final String[] ELEMENTS = {"bear", "otter", "badger"};

    // Width and height
    private static final int QUEUE_CAPACITY = ELEMENTS.length;
    private static final int QUEUE_COUNT = 10;

    private FixedQueueArray<String> mQueueArray;

    @Before
    public void setUp()
    {
        mQueueArray = new FixedQueueArray<>(QUEUE_COUNT, QUEUE_CAPACITY);
    }

    @After
    public void tearDown()
    {
        mQueueArray = null;
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEQueueCount()
    {
        new FixedQueueArray<String>(-1, QUEUE_CAPACITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEQueueCapacity()
    {
        new FixedQueueArray<String>(QUEUE_COUNT, -1);
    }

    @Test
    public void testConstructorCopyTable()
    {
        final int queue = new Random().nextInt(QUEUE_COUNT);
        for (final String element : ELEMENTS) {
            mQueueArray.add(queue, element);
        }

        final FixedQueueArray<String> copy = new FixedQueueArray<>(mQueueArray);

        Assert.assertTrue(mQueueArray.equals(copy));
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorCopyNPE()
    {
        new FixedQueueArray<String>(null);
    }

    @Test
    public void testAdd()
    {
        Assert.assertNull(mQueueArray.add(0, ELEMENTS[0]));
    }

    @Test
    public void testAddReturnsOldestElement()
    {
        // Completely fill one of the queues
        final int queue = new Random().nextInt(QUEUE_COUNT);
        for (final String element : ELEMENTS) {
            mQueueArray.add(queue, element);
        }

        Assert.assertEquals(ELEMENTS[0], mQueueArray.add(queue, null));
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testAddIOBEMinimum()
    {
        mQueueArray.add(-1, ELEMENTS[0]);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testAddIOBEMaximum()
    {
        mQueueArray.add(QUEUE_COUNT, ELEMENTS[0]);
    }

    @Test
    public void testGet()
    {
        Assert.assertNull(mQueueArray.get(0, 0));
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testGetIOBEMinimumColumn()
    {
        mQueueArray.get(-1, 0);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testGetIOBEMaximumColumn()
    {
        mQueueArray.get(QUEUE_CAPACITY, 0);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testGetIOBEMinimumRow()
    {
        mQueueArray.get(0, -1);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testGetIOBEMaximumRow()
    {
        mQueueArray.get(0, QUEUE_COUNT);
    }

    @Test
    public void testClearRow()
    {
        // Completely fill a random queue
        final int queue = new Random().nextInt(QUEUE_COUNT);
        for (final String element : ELEMENTS) {
            mQueueArray.add(queue, element);
        }

        mQueueArray.clear(queue);
        for (int col = 0; col < QUEUE_CAPACITY; col++) {
            mQueueArray.get(col, queue);
        }
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testClearRowIOBEMinimum()
    {
        mQueueArray.clear(-1);
    }

    @Test (expected = IndexOutOfBoundsException.class)
    public void testClearRowIOBEMaximum()
    {
        mQueueArray.clear(QUEUE_COUNT);
    }

    @Test
    public void testClear()
    {
        // Completely fill a random queue
        final Random rng = new Random();
        for (final String element : ELEMENTS) {
            mQueueArray.add(rng.nextInt(QUEUE_COUNT), element);
        }

        mQueueArray.clear();

        for (int col = 0; col < QUEUE_CAPACITY; col++) {
            for (int row = 0; row < QUEUE_COUNT; row++) {
                Assert.assertNull(mQueueArray.get(col, row));
            }
        }
    }

    @Test
    public void testWidth()
    {
        Assert.assertEquals(QUEUE_CAPACITY, mQueueArray.width());
    }

    @Test
    public void testHeight()
    {
        Assert.assertEquals(QUEUE_COUNT, mQueueArray.height());
    }

    @Test
    public void testHashCode()
    {
        final FixedQueueArray<String> copy = new FixedQueueArray<>(mQueueArray.height(), mQueueArray.width());

        Assert.assertEquals(mQueueArray.hashCode(), copy.hashCode());
    }

    @Test
    public void testHashCodeNotEqual()
    {
        final int w = mQueueArray.width();
        final int h = mQueueArray.height();
        final FixedQueueArray<String> differ = new FixedQueueArray<>(h + 1, w + 1);

        Assert.assertNotEquals(mQueueArray.hashCode(), differ.hashCode());
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        final FixedQueueArray<String> different = new FixedQueueArray<>(mQueueArray.height(), mQueueArray.width());
        different.add(0, ELEMENTS[1]);

        Assert.assertFalse(mQueueArray.equals(different));
    }

    @Test
    public void testEqualsNull()
    {
        Assert.assertFalse(mQueueArray.equals(null));
    }

    @Test
    public void testEqualsTransitive()
    {
        final FixedQueueArray<String> copyA = new FixedQueueArray<>(mQueueArray.height(), mQueueArray.width());
        final FixedQueueArray<String> copyB = new FixedQueueArray<>(mQueueArray.height(), mQueueArray.width());

        Assert.assertTrue(mQueueArray.equals(copyA) && copyA.equals(copyB) && mQueueArray.equals(copyB));
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertTrue(mQueueArray.equals(mQueueArray));
    }

    @Test
    public void testEqualsSymmetric()
    {
        final FixedQueueArray<String> copy = new FixedQueueArray<>(mQueueArray.height(), mQueueArray.width());

        Assert.assertTrue(mQueueArray.equals(copy) && copy.equals(mQueueArray));
    }
}
