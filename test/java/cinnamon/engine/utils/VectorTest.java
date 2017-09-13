package cinnamon.engine.utils;

import org.junit.*;

import java.util.Random;

/**
 * <p>{@code Vector}'s getters and setters are not explicitly tested and are presumed correct.</p>
 *
 * <p>Each test performs its testing on vectors with components of all combinations of the following value types
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
public class VectorTest
{
    // FP comparison
    private static final float DELTA = 0.00001f;

    private Random mRNG;

    private Vector mVectorA;
    private Vector mVectorB;

    // Backup corresponding vectors' components
    private Vector mVectorCopyA;
    private Vector mVectorCopyB;

    @Before
    public void setUp()
    {
        mRNG = new Random(System.nanoTime());
        mVectorA = new Vector();
        mVectorB = new Vector();
        mVectorCopyA = new Vector();
        mVectorCopyB = new Vector();
    }

    @After
    public void tearDown()
    {
        mRNG = null;
        mVectorA = null;
        mVectorB = null;
        mVectorCopyA = null;
        mVectorCopyB = null;
    }

    @Test
    public void testCopy()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            mVectorCopyA.copy(mVectorA);

            PositionableTestSuite.assertEquals(mVectorA, mVectorCopyA);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNullPointerException()
    {
        mVectorA.copy(null);
    }

    @Test
    public void testDot()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);

            PositionableTestSuite.generatePermutations(mVectorB, () -> {
                PositionableTestSuite.copyPositionFromTo(mVectorB, mVectorCopyB);

                final float dotA2B = mVectorA.dot(mVectorB);
                final float dotB2A = mVectorB.dot(mVectorA);

                final float vecAX = mVectorA.getX();
                final float vecAY = mVectorA.getY();
                final float vecAZ = mVectorA.getZ();

                final float vecBX = mVectorB.getX();
                final float vecBY = mVectorB.getY();
                final float vecBZ = mVectorB.getZ();

                final float expectedDot = (vecAX * vecBX) + (vecAY * vecBY) + (vecAZ * vecBZ);

                // Test dot product result
                Assert.assertEquals(expectedDot, dotA2B, DELTA);
                Assert.assertEquals(expectedDot, dotB2A, DELTA);

                // Test vector's components remain changed
                PositionableTestSuite.assertEquals(mVectorCopyA, mVectorA);
                PositionableTestSuite.assertEquals(mVectorCopyB, mVectorB);
            }, mRNG);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testDotNullPointerException()
    {
        mVectorA.dot(null);
    }

    @Test
    public void testCross()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.generatePermutations(mVectorB, () -> {

                PositionableTestSuite.copyPositionFromTo(mVectorB, mVectorCopyB);

                final float xA = mVectorA.getX();
                final float yA = mVectorA.getY();
                final float zA = mVectorA.getZ();

                final float xB = mVectorB.getX();
                final float yB = mVectorB.getY();
                final float zB = mVectorB.getZ();

                final float expCrossX = (yA * zB) - (zA * yB);
                final float expCrossY = (zA * xB) - (xA * zB);
                final float expCrossZ = (xA * yB) - (yA * xB);
                final Vector returnedVector = mVectorA.cross(mVectorB);

                // Test resulting normal
                Assert.assertEquals(expCrossX, mVectorA.getX(), DELTA);
                Assert.assertEquals(expCrossY, mVectorA.getY(), DELTA);
                Assert.assertEquals(expCrossZ, mVectorA.getZ(), DELTA);

                // Test second vector for unchanged components
                PositionableTestSuite.assertEquals(mVectorCopyB, mVectorB);

                // Test returned is caller
                Assert.assertTrue(returnedVector == mVectorA);
            }, mRNG);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testCrossNullPointerException()
    {
        mVectorA.cross(null);
    }

    @Test
    public void testMultiply()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);

            for (int i = 0; i < PositionableTestSuite.VALUE_TYPE_COUNT; i++) {
                // Reset components back to permutation
                PositionableTestSuite.copyPositionFromTo(mVectorCopyA, mVectorA);

                // Compute expected and actual
                final float factor = PositionableTestSuite.getValueOfType(i, mRNG);
                final float expX = mVectorA.getX() * factor;
                final float expY = mVectorA.getY() * factor;
                final float expZ = mVectorA.getZ() * factor;
                final Vector returnedVector = mVectorA.multiply(factor);

                Assert.assertEquals(expX, mVectorA.getX(), DELTA);
                Assert.assertEquals(expY, mVectorA.getY(), DELTA);
                Assert.assertEquals(expZ, mVectorA.getZ(), DELTA);
                Assert.assertTrue(returnedVector == mVectorA);
            }
        }, mRNG);
    }

    @Test
    public void testDivide()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);

            for (int i = 0; i < PositionableTestSuite.VALUE_TYPE_COUNT; i++) {
                // Reset components back to permutation
                PositionableTestSuite.copyPositionFromTo(mVectorCopyA, mVectorA);

                // Compute expected and actual
                final float factor = PositionableTestSuite.getValueOfType(i, mRNG);
                final float expX = mVectorA.getX() / factor;
                final float expY = mVectorA.getY() / factor;
                final float expZ = mVectorA.getZ() / factor;
                final Vector returnedVector = mVectorA.divide(factor);

                Assert.assertEquals(expX, mVectorA.getX(), DELTA);
                Assert.assertEquals(expY, mVectorA.getY(), DELTA);
                Assert.assertEquals(expZ, mVectorA.getZ(), DELTA);
                Assert.assertTrue(returnedVector == mVectorA);
            }
        }, mRNG);
    }

    @Test
    public void testAdd()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);

            PositionableTestSuite.generatePermutations(mVectorB, () -> {
                PositionableTestSuite.copyPositionFromTo(mVectorB, mVectorCopyB);

                final float vecAX = mVectorA.getX();
                final float vecAY = mVectorA.getY();
                final float vecAZ = mVectorA.getZ();

                final float vecBX = mVectorB.getX();
                final float vecBY= mVectorB.getY();
                final float vecBZ = mVectorB.getZ();

                // Compute expected components for vector A
                final float expSumX = vecAX + vecBX;
                final float expSumY = vecAY + vecBY;
                final float expSumZ = vecAZ + vecBZ;
                final Vector returnedVector = mVectorA.add(mVectorB);

                Assert.assertEquals(expSumX, mVectorA.getX(), DELTA);
                Assert.assertEquals(expSumY, mVectorA.getY(), DELTA);
                Assert.assertEquals(expSumZ, mVectorA.getZ(), DELTA);
                Assert.assertTrue(returnedVector == mVectorA);

                // Vector B should remain unchanged
                PositionableTestSuite.assertEquals(mVectorCopyB, mVectorB);
            }, mRNG);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testAddNullPointerException()
    {
        mVectorA.add(null);
    }

    @Test
    public void testSubtract()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);

            PositionableTestSuite.generatePermutations(mVectorB, () -> {
                PositionableTestSuite.copyPositionFromTo(mVectorB, mVectorCopyB);

                final float vecAX = mVectorA.getX();
                final float vecAY = mVectorA.getY();
                final float vecAZ = mVectorA.getZ();

                final float vecBX = mVectorB.getX();
                final float vecBY= mVectorB.getY();
                final float vecBZ = mVectorB.getZ();

                // Compute expected components for vector A
                final float expSumX = vecAX - vecBX;
                final float expSumY = vecAY - vecBY;
                final float expSumZ = vecAZ - vecBZ;
                final Vector returnedVector = mVectorA.subtract(mVectorB);

                Assert.assertEquals(expSumX, mVectorA.getX(), DELTA);
                Assert.assertEquals(expSumY, mVectorA.getY(), DELTA);
                Assert.assertEquals(expSumZ, mVectorA.getZ(), DELTA);
                Assert.assertTrue(returnedVector == mVectorA);

                // Vector B should remain unchanged
                PositionableTestSuite.assertEquals(mVectorCopyB, mVectorB);
            }, mRNG);
        }, mRNG);
    }

    @Test (expected = NullPointerException.class)
    public void testSubtractNullPointerException()
    {
        mVectorA.subtract(null);
    }

    @Test
    public void testNormalize()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            final float x = mVectorA.getX();
            final float y = mVectorA.getY();
            final float z = mVectorA.getZ();
            final Vector returnedVector = mVectorA.normalize();

            // Compute expected magnitude after normalization
            float magnitude = computeDistanceTo(x, y, z);
            magnitude = (magnitude == 0f) ? 1f : magnitude;
            final float expNormX = x / magnitude;
            final float expNormY = y / magnitude;
            final float expNormZ = z / magnitude;
            final float expNormMag = computeDistanceTo(expNormX, expNormY, expNormZ);

            // Compute actual magnitude (w/o relying on vector.magnitude())
            final float actualNormX = mVectorA.getX();
            final float actualNormY = mVectorA.getY();
            final float actualNormZ = mVectorA.getZ();
            final float actualNormMag = computeDistanceTo(actualNormX, actualNormY, actualNormZ);

            Assert.assertEquals(expNormMag, actualNormMag, DELTA);
            Assert.assertTrue(returnedVector == mVectorA);
        }, mRNG);
    }

    @Test
    public void testNegate()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            final float expX = -mVectorA.getX();
            final float expY = -mVectorA.getY();
            final float expZ = -mVectorA.getZ();

            final Vector returnedVector = mVectorA.negate();
            final float actualX = mVectorA.getX();
            final float actualY = mVectorA.getY();
            final float actualZ = mVectorA.getZ();

            Assert.assertEquals(expX, actualX, DELTA);
            Assert.assertEquals(expY, actualY, DELTA);
            Assert.assertEquals(expZ, actualZ, DELTA);
            Assert.assertTrue(returnedVector == mVectorA);
        }, mRNG);
    }

    @Test
    public void testMagnitude()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            final float expMag = computeDistanceTo(mVectorA.getX(), mVectorA.getY(), mVectorA.getZ());
            final float actualMag = mVectorA.magnitude();

            Assert.assertEquals(expMag, actualMag, DELTA);
        }, mRNG);
    }

    @Test
    public void testIsZero()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            final float x = mVectorA.getX();
            final float y = mVectorA.getY();
            final float z = mVectorA.getZ();

            Assert.assertEquals(x == 0f && y == 0f && z == 0f, mVectorA.isZero());
        }, mRNG);
    }

    @Test
    public void testHashCode()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorB);

            // Test affirmative
            Assert.assertEquals(mVectorA.hashCode(), mVectorB.hashCode());
            Assert.assertEquals(mVectorA.hashCode(), mVectorA.hashCode());

            // Permutations will not use max so should not be equivalent
            mVectorB.setX(Float.MAX_VALUE);
            mVectorB.setY(Float.MAX_VALUE);
            mVectorB.setZ(Float.MAX_VALUE);

            // Test negative
            Assert.assertNotEquals(mVectorA.hashCode(), mVectorB.hashCode());
            Assert.assertNotEquals(mVectorA.hashCode(), new Object().hashCode());
        }, mRNG);
    }

    @Test
    public void testEquals()
    {
        PositionableTestSuite.generatePermutations(mVectorA, () -> {

            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorCopyA);
            PositionableTestSuite.copyPositionFromTo(mVectorA, mVectorB);

            // Symmetric
            Assert.assertTrue(mVectorA.equals(mVectorB));
            Assert.assertTrue(mVectorB.equals(mVectorA));

            // Reflexive
            Assert.assertTrue(mVectorA.equals(mVectorA));

            // Transitive
            Assert.assertTrue(mVectorB.equals(mVectorCopyA));
            Assert.assertTrue(mVectorA.equals(mVectorCopyA));

            // Permutations will not use max so should not be equivalent
            mVectorB.setX(Float.MAX_VALUE);
            mVectorB.setY(Float.MAX_VALUE);
            mVectorB.setZ(Float.MAX_VALUE);

            Assert.assertFalse(mVectorA.equals(mVectorB));
            Assert.assertFalse(mVectorA.equals(null));
            Assert.assertFalse(mVectorA.equals(new Object()));
        }, mRNG);
    }

    /**
     * <p>Computes the distance from (0,0,0) to the given (x,y,z).</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     * @return distance.
     */
    private float computeDistanceTo(float x, float y, float z)
    {
        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }
}