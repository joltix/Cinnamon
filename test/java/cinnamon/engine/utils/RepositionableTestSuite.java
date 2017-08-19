package cinnamon.engine.utils;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.util.Random;

/**
 * <p>Provides a set of (x,y,z) with all permutations of different types of floating point values to test against.</p>
 *
 * <p>This set covers the following types: zeros (positive and negative zeros are not differentiated), +- infinity,
 * NaN, and finite non-zero positive and negative numbers. Unlike the other types of values, when a finite non-zero
 * positive or negative number is needed in a permutation, the number is randomly chosen.</p>
 *
 * <p>Tests wanting to cover these values should call {@link #generatePermutations(Repositionable, Runnable, Random)}
 * and pass the actual test's operations in as a <tt>Runnable</tt>.</p>
 */
@RunWith(Suite.class)
@SuiteClasses({PointTest.class, VectorTest.class})
public class RepositionableTestSuite
{
    // Number of different kinds of input values (-inf, NaN, etc)
    public static final int VALUE_TYPE_COUNT = 5;

    private static final int TYPE_ZERO = 0;
    private static final int TYPE_POSITIVE_INFINITY = 1;
    private static final int TYPE_NEGATIVE_INFINITY = 2;
    private static final int TYPE_NAN = 3;
    private static final int TYPE_NORMAL = 4;

    // Maximum abs TYPE_NORMAL value
    private static final int RNG_CEILING = 1_000_000;

    // Minimum factor when computing a non-zero, non-NaN, finite value
    private static final float RNG_FLOOR = 0.01f;

    /**
     * <p>Executes the Runnable multiple times with each execution having a different combination of different
     * kinds of x, y, and z values (+0, infinity, etc) for the given Repositionable. The test method should be
     * called in {@link Runnable#run()} and expect that the given Repositionable's values are changed between
     * executions of the Runnable.</p>
     *
     *  @param container to test.
     * @param runnable testing operation.
     * @param rng random number generator.
     * @throws NullPointerException if either container, runnable, or rng is null.
     */
    public static void generatePermutations(Repositionable container, Runnable runnable, Random rng)
    {
        if (container == null) {
            throw new NullPointerException("Permutations cannot be stored when container is null");
        }
        if (runnable == null) {
            throw new NullPointerException("Runnable cannot be null");
        }
        if (rng == null) {
            throw new NullPointerException("Random number generator cannot be null");
        }

        // For each component's possible value type, set container and execute ops
        for (int a = 0; a < VALUE_TYPE_COUNT; a++) {
            for (int b = 0; b < VALUE_TYPE_COUNT; b++) {
                for (int c = 0; c < VALUE_TYPE_COUNT; c++) {

                    container.setX(getValueOfType(a, rng));
                    container.setY(getValueOfType(b, rng));
                    container.setZ(getValueOfType(c, rng));

                    runnable.run();
                }
            }
        }
    }

    /**
     * <p>Gets the value corresponding with the given range [0,{@link #VALUE_TYPE_COUNT}]. Value identifiers are as
     * follows.</p>
     *
     * <ul>
     *     <li>0 : +-0</li>
     *     <li>1 : {@link Float#POSITIVE_INFINITY}</li>
     *     <li>2 : {@link Float#NEGATIVE_INFINITY}</li>
     *     <li>3 : {@link Float#NaN}</li>
     *     <li>4 : all other values within the test case's range</li>
     * </ul>
     *
     * @param type value identifier
     * @param rng random number generator.
     * @return value.
     * @throws NullPointerException if random number generator is null.
     */
    public static float getValueOfType(int type, Random rng)
    {
        if (rng == null) {
            throw new NullPointerException("Random number generator cannot be null");
        }

        switch (type) {
            case TYPE_ZERO:
                return 0f;

            case TYPE_POSITIVE_INFINITY:
                return Float.POSITIVE_INFINITY;

            case TYPE_NEGATIVE_INFINITY:
                return Float.NEGATIVE_INFINITY;

            case TYPE_NAN:
                return Float.NaN;

            case TYPE_NORMAL:
                // Initial non-zero amount
                final float body = rng.nextInt(RNG_CEILING) + 1;
                float factor = rng.nextFloat();

                // Prevent zeroing out
                factor = (factor <= 0f) ? RNG_FLOOR : factor;

                return body * factor * ((rng.nextBoolean()) ? -1f : 1f);

            default: throw new IllegalArgumentException("Unrecognized value type: " + type);
        }
    }

    /**
     * <p>Copies the position values from one <tt>Repositionable</tt> to another.</p>
     *
     * @param from source.
     * @param to target.
     * @throws NullPointerException if either from or to is null.
     */
    public static void copyPositionFromTo(Repositionable from, Repositionable to)
    {
        if (from == null) {
            throw new NullPointerException("Position copy source cannot be null");
        }
        if (to == null) {
            throw new NullPointerException("Position copy destination cannot be null");
        }

        to.setX(from.getX());
        to.setY(from.getY());
        to.setZ(from.getZ());
    }

    /**
     * <p>Asserts that the given actual <tt>Repositionable</tt> has the exact same values (floating point delta of 0)
     * as the expected one. This method cannot be used to assert that two Repositionals are both null.</p>
     *
     * @param expected expected.
     * @param actual actual.
     * @throws NullPointerException if either expected or actual is null.
     */
    public static void assertEquals(Repositionable expected, Repositionable actual)
    {
        if (expected == null) {
            throw new NullPointerException("Expected cannot be null");
        }
        if (actual == null) {
            throw new NullPointerException("Actual cannot be null");
        }

        Assert.assertEquals(expected.getX(), actual.getX(), 0f);
        Assert.assertEquals(expected.getY(), actual.getY(), 0f);
        Assert.assertEquals(expected.getZ(), actual.getZ(), 0f);
    }
}
