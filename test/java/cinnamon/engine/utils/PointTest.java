package cinnamon.engine.utils;

import org.junit.*;

/**
 * Individual getters and setters are presumed correct.
 */
public class PointTest
{
    private static final float EPSILON = 0.01f;

    private static final float X = 0.42f;

    private static final float Y = 0.343f;

    private static final float Z = 0.7f;

    private Point mPointA;

    private Point mPointB;

    @Before
    public void setUp()
    {
        mPointA = new Point(X, Y, Z);
        mPointB = new Point(X, Y, Z);
    }

    @After
    public void tearDown()
    {
        mPointA = null;
        mPointB = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPESource()
    {
        new Point(null);
    }

    @Test
    public void testCopyCopiesAllCoordinates()
    {
        final Point copy = new Point();
        copy.copy(mPointA);

        Assert.assertEquals(X, copy.getX(), 0f);
        Assert.assertEquals(Y, copy.getY(), 0f);
        Assert.assertEquals(Z, copy.getZ(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNPESource()
    {
        mPointA.copy(null);
    }

    @Test
    public void testSetSetsAllCoordinates()
    {
        final float x = mPointA.getX();
        final float y = mPointA.getY();
        final float z = mPointA.getZ();

        mPointA.set(-x, -y, -z);

        Assert.assertEquals(-x, mPointA.getX(), 0f);
        Assert.assertEquals(-y, mPointA.getY(), 0f);
        Assert.assertEquals(-z, mPointA.getZ(), 0f);
    }

    @Test
    public void testShiftTranslatesAllCoordinates()
    {
        final float x = mPointA.getX();
        final float y = mPointA.getY();
        final float z = mPointA.getZ();

        mPointA.shift(1f, 1f, 1f);

        Assert.assertEquals(x + 1, mPointA.getX(), 0f);
        Assert.assertEquals(y + 1, mPointA.getY(), 0f);
        Assert.assertEquals(z + 1, mPointA.getZ(), 0f);
    }

    @Test
    public void testEqualsSymmetric()
    {
        Assert.assertEquals(mPointA, mPointB);
        Assert.assertEquals(mPointB, mPointA);
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertEquals(mPointA, mPointA);
    }

    @Test
    public void testEqualsTransitive()
    {
        final Point pointC = new Point(X, Y, Z);

        Assert.assertEquals(mPointB, pointC);
        Assert.assertEquals(mPointA, pointC);
        Assert.assertEquals(mPointA, mPointB);
    }

    @Test
    public void testEqualsReturnsFalseWhenACoordinateIsDifferent()
    {
        mPointB.setX(-mPointB.getX());

        Assert.assertNotEquals(mPointA, mPointB);
    }

    @Test
    public void testEqualsReturnsFalseWhenObjectIsNotAPoint()
    {
        Assert.assertNotEquals(mPointA, new Object());
    }

    @Test
    public void testEqualsReturnsFalseWhenNullObject()
    {
        Assert.assertNotEquals(mPointA, null);
    }

    @Test
    public void testHashCodeIsSameWhenPointsAreEqual()
    {
        final int expected = mPointA.hashCode();
        final int actual = mPointB.hashCode();

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testStaticDistanceBetweenReturnsZeroWhenSameCoordinates()
    {
        Assert.assertEquals(0f, Point.distanceBetween(mPointA, mPointB), 0f);
    }

    @Test
    public void testStaticDistanceBetweenReturnsNonZeroWhenDifferingCoordinates()
    {
        final float expectedDistance = distance(mPointA, mPointB) + X;

        mPointA.setX(mPointA.getX() + X);

        Assert.assertEquals(expectedDistance, Point.distanceBetween(mPointA, mPointB), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testStaticDistanceBetweenNPEPointA()
    {
        Point.distanceBetween(null, mPointB);
    }

    @Test (expected = NullPointerException.class)
    public void testStaticDistanceBetweenNPEPointB()
    {
        Point.distanceBetween(mPointA, null);
    }

    @Test
    public void testStaticIsEqualReturnsTrueWhenZeroDifferenceWithZeroEpsilon()
    {
        Assert.assertTrue(Point.isEqual(mPointA, mPointB, 0f));
    }

    @Test
    public void testStaticIsEqualReturnsTrueWhenZeroDifferenceWithNonZeroEpsilon()
    {
        Assert.assertTrue(Point.isEqual(mPointA, mPointB, EPSILON));
    }

    @Test
    public void testStaticIsEqualReturnsTrueWhenNonZeroDifferenceIsLessThanEpsilonButNotExact()
    {
        mPointA.setX(mPointA.getX() + (EPSILON / 2f));

        Assert.assertTrue(Point.isEqual(mPointA, mPointB, EPSILON));
    }

    @Test
    public void testStaticIsEqualReturnsFalseWhenNonZeroDifferenceIsGreaterThanOrEqualToEpsilon()
    {
        mPointA.setX(mPointA.getX() + (EPSILON * 2f));

        Assert.assertFalse(Point.isEqual(mPointA, mPointB, EPSILON));
    }

    @Test (expected = NullPointerException.class)
    public void testStaticIsEqualNPEPointA()
    {
        Point.isEqual(null, mPointB, EPSILON);
    }

    @Test (expected = NullPointerException.class)
    public void testStaticIsEqualNPEPointB()
    {
        Point.isEqual(mPointA, null, EPSILON);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStaticIsEqualIAENaNEpsilon()
    {
        Point.isEqual(mPointA, mPointB, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStaticIsEqualIAEPositivelyInfiniteEpsilon()
    {
        Point.isEqual(mPointA, mPointB, Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStaticIsEqualIAENegativelyInfiniteEpsilon()
    {
        Point.isEqual(mPointA, mPointB, Float.NEGATIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStaticIsEqualIAENegativeEpsilon()
    {
        Point.isEqual(mPointA, mPointB, -1f);
    }

    private float distance(Point a, Point b)
    {
        final float x = a.getX() - b.getX();
        final float y = a.getY() - b.getY();
        final float z = a.getZ() - b.getZ();

        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }
}
