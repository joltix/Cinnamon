package cinnamon.engine.utils;

import org.junit.*;

import java.util.Random;

/**
 * <p><tt>Point</tt>'s getters and setters are not explicitly tested and are presumed correct.</p>
 *
 * <p>Each test performs its operations on points of all combinations of the following value types
 * (positive and negative zeros are not differentiated).</p>
 *
 * <ul>
 *     <li>0</li>
 *     <li>+infinity</li>
 *     <li>-infinity</li>
 *     <li>NaN</li>
 *     <li>non-zero finite positive and negative numbers</li>
 * </ul>
 */
public class PointTest
{
    private static final float DELTA = 0.00001f;

    private Random mRNG;

    private Point mPointA;
    private Point mPointB;

    // Setting aside Point A's values
    private Point mPointCopyA;

    @Before
    public void setUp()
    {
        mRNG = new Random(System.nanoTime());
        mPointA = new Point();
        mPointB = new Point();
        mPointCopyA = new Point();
    }

    @After
    public void tearDown()
    {
        mRNG = null;
        mPointA = null;
        mPointB = null;
        mPointCopyA = null;
    }

    @Test
    public void testCopy()
    {
        RepositionableTestSuite.generatePermutations(mPointA, () -> {
            mPointCopyA.copy(mPointA);

            RepositionableTestSuite.assertEquals(mPointA, mPointCopyA);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNullPointerException()
    {
        mPointA.copy(null);
    }

    @Test
    public void testHashCode()
    {
        RepositionableTestSuite.generatePermutations(mPointA, () -> {

            RepositionableTestSuite.copyPositionFromTo(mPointA, mPointB);

            // Test affirmative
            Assert.assertEquals(mPointA.hashCode(), mPointB.hashCode());
            Assert.assertEquals(mPointA.hashCode(), mPointA.hashCode());

            // Permutations will not use max so should not be equivalent
            mPointB.setX(Float.MAX_VALUE);
            mPointB.setY(Float.MAX_VALUE);
            mPointB.setZ(Float.MAX_VALUE);

            // Test negative
            Assert.assertNotEquals(mPointA.hashCode(), mPointB.hashCode());
            Assert.assertNotEquals(mPointA.hashCode(), new Object().hashCode());
        }, mRNG);
    }

    @Test
    public void testEquals()
    {
        RepositionableTestSuite.generatePermutations(mPointA, () -> {

            RepositionableTestSuite.copyPositionFromTo(mPointA, mPointCopyA);
            RepositionableTestSuite.copyPositionFromTo(mPointA, mPointB);

            // Symmetric
            Assert.assertTrue(mPointA.equals(mPointB));
            Assert.assertTrue(mPointB.equals(mPointA));

            // Reflexive
            Assert.assertTrue(mPointA.equals(mPointA));

            // Transitive
            Assert.assertTrue(mPointB.equals(mPointCopyA));
            Assert.assertTrue(mPointA.equals(mPointCopyA));

            // Permutations will not use max so should not be equivalent
            mPointB.setX(Float.MAX_VALUE);
            mPointB.setY(Float.MAX_VALUE);
            mPointB.setZ(Float.MAX_VALUE);

            Assert.assertFalse(mPointA.equals(mPointB));
            Assert.assertFalse(mPointA.equals(null));
            Assert.assertFalse(mPointA.equals(new Object()));
        }, mRNG);
    }

    @Test
    public void testStaticDistanceBetween()
    {
        // Short aliases
        final Point pA = mPointA;
        final Point pB = mPointB;

        RepositionableTestSuite.generatePermutations(mPointA, () -> {
            RepositionableTestSuite.generatePermutations(mPointB, () -> {

                final float expDist = distance(pA.getX(), pA.getY(), pA.getZ(), pB.getX(), pB.getY(), pB.getZ());
                final float actualDist = Point.distanceBetween(pA, pB);

                Assert.assertEquals(expDist, actualDist, 0f);

            }, mRNG);
        }, mRNG);
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
    public void testStaticIsEqual()
    {
        // Test special case values
        RepositionableTestSuite.generatePermutations(mPointA, () -> {
            RepositionableTestSuite.generatePermutations(mPointB, () -> {

                Assert.assertEquals(coordinatesEqual(mPointA, mPointB), Point.isEqual(mPointA, mPointB, DELTA));

            }, mRNG);
        }, mRNG);

        // Diff < DELTA should be eq
        mPointA.setPosition(1f, 1f, 1f);
        mPointB.setPosition(1f, 1f, 1f);
        mPointB.addX(DELTA / 2f);
        mPointB.addY(DELTA / 2f);
        mPointB.addZ(DELTA / 2f);

        Assert.assertTrue(coordinatesEqual(mPointA, mPointB));

        // Diff now > DELTA so not eq
        mPointB.addX(DELTA * 2f);
        mPointB.addY(DELTA * 2f);
        mPointB.addZ(DELTA * 2f);

        Assert.assertFalse(coordinatesEqual(mPointA, mPointB));
    }

    @Test (expected = NullPointerException.class)
    public void testStaticIsEqualNPEPointA()
    {
        Point.isEqual(null, mPointB, DELTA);
    }

    @Test (expected = NullPointerException.class)
    public void testStaticIsEqualNPEPointB()
    {
        Point.isEqual(mPointA, null, DELTA);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testStaticIsEqualIAEDelta()
    {
        Point.isEqual(mPointA, mPointB, -1f);
    }

    /**
     * <p>Computes the distance between two sets of coordinates.</p>
     *
     * @param x0 first x.
     * @param y0 first y.
     * @param z0 first z.
     * @param x1 second x.
     * @param y1 second y.
     * @param z1 second z.
     * @return distance.
     */
    private float distance(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        final float x = x0 - x1;
        final float y = y0 - y1;
        final float z = z0 - z1;

        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    /**
     * <p>Checks if two points can be considered equal with a delta of {@link #DELTA}. NaN values are considered
     * equal.</p>
     *
     * @param pointA first point.
     * @param pointB second point.
     * @return true if equal.
     * @throws NullPointerException if either point is null.
     */
    private boolean coordinatesEqual(Point pointA, Point pointB)
    {
        if (pointA == null) {
            throw new NullPointerException("Point A cannot be null");
        }
        if (pointB == null) {
            throw new NullPointerException("Point B cannot be null");
        }

        boolean expEq = coordinatesEqual(mPointA.getX(), mPointB.getX());
        expEq = expEq && coordinatesEqual(mPointA.getY(), mPointB.getY());
        return expEq && coordinatesEqual(mPointA.getZ(), mPointB.getZ());
    }

    /**
     * <p>Checks if two floats can be considered equal with a delta of {@link #DELTA}. NaN values are considered
     * equal.</p>
     *
     * @param valueA first float.
     * @param valueB second float.
     * @return true if equal.
     */
    private boolean coordinatesEqual(float valueA, float valueB)
    {
        return (Float.isNaN(valueA) && Float.isNaN(valueB)) || Math.abs(valueA - valueB) <= DELTA;
    }
}
