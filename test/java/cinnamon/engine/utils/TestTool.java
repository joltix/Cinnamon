package cinnamon.engine.utils;

import java.util.Random;

/**
 * <p>Utility functions to support test classes.</p>
 */
final class TestTool
{
    private static final Random mRNG = new Random(System.nanoTime());

    private TestTool() {}

    /**
     * <p>Chooses a random {@code int} between {@code min} (inclusive) and {@code max} (exclusive). If {@code true}
     * is passed for {@code allowNegation}, the random value chosen in the specified range has a chance of being
     * multiplied by -1.</p>
     *
     * @param min minimum.
     * @param max maximum.
     * @param allowNegation true to potentially negate the possible value.
     * @return int.
     * @throws IllegalArgumentException if min {@literal >} max or max - min = 0.
     */
    public static int generateInteger(int min, int max, boolean allowNegation)
    {
        if (min > max) {
            throw new IllegalArgumentException("Min must be <= max, min: " + min + ", max: " + max);
        }
        if (max - min <= 0) {
            throw new IllegalArgumentException("Given range (max - min) must be > 0, min: " + min + ", max: " + max);
        }

        return (mRNG.nextInt(max - min) + min) * ((allowNegation) ? -1 : 1);
    }

    /**
     * <p>Creates an array of distinct randomly indexed {@code ints} between {@code min} (inclusive) and {@code max}
     * (exclusive).</p>
     *
     * @param min minimum.
     * @param max maximum.
     * @param count number of returned ints.
     * @return ints.
     * @throws IllegalArgumentException if min {@literal >} max, count {@literal <} 1, or (max - min) {@literal <}
     * count.
     */
    public static int[] generateDistinctIntegers(int min, int max, int count)
    {
        if (min > max) {
            throw new IllegalArgumentException("Min must be <= max, min: " + min + ", max: " + max);
        }
        if (count < 1) {
            throw new IllegalArgumentException("Count must be > 0, count: " + count);
        }

        final int range = max - min;
        if (count > range) {
            throw new IllegalArgumentException("Range (max - min) must be <= count, min: " + min + ", max: " + max +
                    ", count: " + count);
        }

        // Create array holding all possible values
        final Integer[] possibleValues = new Integer[range];
        for (int i = 0, cursor = min; i < possibleValues.length; i++, cursor++) {
            possibleValues[i] = cursor;
        }

        shuffle(possibleValues);

        // Take first 'count' values
        final int[] out = new int[count];
        for (int i = 0; i < out.length; i++) {
            out[i] = possibleValues[i];
        }

        return out;
    }

    /**
     * <p>Creates an array of distinct randomly indexed {@code floats}.</p>
     *
     * @param min minimum float (inclusive).
     * @param max maximum float (exclusive).
     * @param count number of returned floats.
     * @return floats.
     * @throws IllegalArgumentException if min {@literal >} max or count {@literal <} 1.
     */
    public static float[] generateDistinctFloats(float min, float max, int count)
    {
        if (min > max) {
            throw new IllegalArgumentException("Min must be <= max, min: " + min + ", max: " + max);
        }
        if (count < 1) {
            throw new IllegalArgumentException("Count must be > 0, count: " + count);
        }
        final float range = max - min;
        if (range <= 0f) {
            throw new IllegalArgumentException("Given range (max - min) must be > 0, min: " + min + ", max: " + max);
        }

        // Create array holding possible values separated by an interval
        final float interval = range / count;
        final int numOfValues = (int) Math.floor(range / interval);
        final Float[] possibleValues = new Float[(numOfValues > count) ? numOfValues : count];

        float cursor = min;
        for (int i = 0; i < possibleValues.length; i++, cursor += interval) {
            possibleValues[i] = cursor;
        }

        shuffle(possibleValues);

        // Take first 'count' values while fudging each value by a fraction
        final float[] out = new float[count];
        for (int i = 0; i < out.length; i++) {
            out[i] = possibleValues[i] + mRNG.nextFloat() * interval;
            assert(!Float.isNaN(out[i]) && !Float.isInfinite(out[i]));
        }

        return out;
    }

    /**
     * <p>Shuffles all elements in the given array. This method takes O(n) in the length of the array.</p>
     *
     * @param array to shuffle.
     * @param <E> type to shuffle.
     * @throws NullPointerException if array is null.
     */
    public static <E> void shuffle(E[] array)
    {
        if (array == null) {
            throw new NullPointerException();
        }

        for (int i = 0; i < array.length; i++) {
            final int swapIndex = mRNG.nextInt(array.length);
            final E swap = array[swapIndex];

            array[swapIndex] = array[i];
            array[i] = swap;
        }
    }
}
