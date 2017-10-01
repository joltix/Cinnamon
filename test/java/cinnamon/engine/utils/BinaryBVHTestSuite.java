package cinnamon.engine.utils;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * <p>Defines the test subject class {@code BoxPartitionable}, input set generation, and its parameters. Executing
 * this test suite will run the tests in {@link BinaryBVHTest} and {@link BinaryBVHPropertyTest}.</p>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({BinaryBVHTest.class, BinaryBVHPropertyTest.class})
public class BinaryBVHTestSuite
{
    // Minimum bounds size
    public static final int MIN_W = 0;
    public static final int MIN_H = 0;
    public static final int MIN_D = 0;

    // Maximum bounds size
    public static final int MAX_W = 1_000;
    public static final int MAX_H = 1_000;
    public static final int MAX_D = 1_000;

    // Minimum bounds' position
    public static final int MIN_X = -1_000;
    public static final int MIN_Y = MIN_X;
    public static final int MIN_Z = MIN_X;

    // Maximum bounds' position
    public static final int MAX_X = 1_000;
    public static final int MAX_Y = MAX_X;
    public static final int MAX_Z = MAX_X;

    /**
     * <p>Creates an array of {@code BoxPartitionables} of random sizes and positions according to the
     * {@code BinaryBVHTestSuite}'s testing parameters.</p>
     *
     * @param count number of instances.
     * @return array of {@code BoxPartitionables}.
     */
    public static BoxPartitionable[] generateDistinctBoxPartitionables(int count)
    {
        if (count < 1) {
            throw new IllegalArgumentException("Count must be >= 1, count: " + count);
        }

        // Create sets of random sizes and positions
        final BoxPartitionable[] partitionables = new BoxPartitionable[count];
        final float[] widths = TestTool.generateDistinctFloats(MIN_W, MAX_W, count);
        final float[] heights = TestTool.generateDistinctFloats(MIN_H, MAX_H, count);
        final float[] depths = TestTool.generateDistinctFloats(MIN_D, MAX_D, count);
        final float[] x = TestTool.generateDistinctFloats(MIN_X, MAX_X, count);
        final float[] y = TestTool.generateDistinctFloats(MIN_Y, MAX_Y, count);
        final float[] z = TestTool.generateDistinctFloats(MIN_Z, MAX_Z, count);

        for (int i = 0; i < partitionables.length; i++) {
            partitionables[i] = new BoxPartitionable(widths[i], heights[i], depths[i], x[i], y[i], z[i]);
        }

        return partitionables;
    }

    /**
     * <p>Element added to {@code BinaryBVH} during testing.</p>
     */
    public static class BoxPartitionable implements Partitionable
    {
        private MutableBounds mBounds = new MutableBounds();

        /**
         * <p>Constructs a {@code BoxPartitionable} whose {@code Bounds} has the same size and position as another
         * {@code BoxPartitionable}.</p>
         *
         * @param obj to copy.
         * @throws NullPointerException if obj is null.
         */
        public BoxPartitionable(BoxPartitionable obj)
        {
            mBounds.copy(obj.mBounds);
        }

        /**
         * <p>Constructs a {@code BoxPartitionable} with a {@code MutableBounds} of specific size and position.</p>
         *
         * @param width bounds' width.
         * @param height bounds' height.
         * @param depth bounds' depth.
         * @param x bounds' min x.
         * @param y bounds' min y.
         * @param z bounds' min z.
         * @throws IllegalArgumentException if either width, height, or depth is {@literal <} 0.
         */
        public BoxPartitionable(float width, float height, float depth, float x, float y, float z)
        {
            if (width < 0f) {
                throw new IllegalArgumentException("Width must be >= 0, width: " + width);
            }
            if (height < 0f) {
                throw new IllegalArgumentException("Height must be >= 0, height: " + height);
            }
            if (depth < 0f) {
                throw new IllegalArgumentException("Depth must be >= 0, depth: " + depth);
            }

            mBounds.encompass(x, y, z, x + width, y + height, z + depth);
        }

        /**
         * <p>Gets a mutable reference to the {@code Bounds}.</p>
         *
         * @return mutable bounds.
         */
        public MutableBounds getMutableBounds()
        {
            return mBounds;
        }

        @Override
        public Bounds getBounds()
        {
            return mBounds;
        }

        @Override
        public int hashCode()
        {
            return 17 * 31 + mBounds.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == null || !(obj instanceof BoxPartitionable)) {
                return false;
            } else if (obj == this) {
                return true;
            }

            return ((BoxPartitionable) obj).mBounds.equals(mBounds);
        }
    }
}
