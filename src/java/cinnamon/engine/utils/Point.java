package cinnamon.engine.utils;

import static java.util.Objects.requireNonNull;

/**
 * A movable position in three dimensions.
 */
public final class Point implements Position, Copier<Point>
{
    private float mX;

    private float mY;

    private float mZ;

    /**
     * Constructs a {@code Point} whose coordinates are (0,0,0).
     */
    public Point()
    {
        this(0f, 0f, 0f);
    }

    /**
     * Constructs a {@code Point} with the same coordinates as another.
     *
     * @param source to copy.
     * @throws NullPointerException if point is null.
     */
    public Point(Point source)
    {
        copy(source);
    }

    /**
     * Constructs a {@code Point} from the given coordinates.
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public Point(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    /**
     * Copies all coordinates from a given {@code Point}.
     *
     * @param source to copy.
     * @throws NullPointerException if point is null.
     */
    @Override
    public void copy(Point source)
    {
        requireNonNull(source);

        mX = source.getX();
        mY = source.getY();
        mZ = source.getZ();
    }

    /**
     * Sets the position.
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public void set(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    /**
     * Shifts the position.
     *
     * @param dx change in x.
     * @param dy change in y.
     * @param dz change in z.
     */
    public void shift(float dx, float dy, float dz)
    {
        mX += dx;
        mY += dy;
        mZ += dz;
    }

    @Override
    public float getX()
    {
        return mX;
    }

    /**
     * Sets the x position.
     *
     * @param x x.
     */
    public void setX(float x)
    {
        mX = x;
    }

    @Override
    public float getY()
    {
        return mY;
    }

    /**
     * Sets the y position.
     *
     * @param y y.
     */
    public void setY(float y)
    {
        mY = y;
    }

    @Override
    public float getZ()
    {
        return mZ;
    }

    /**
     * Sets the z position.
     *
     * @param z z.
     */
    public void setZ(float z)
    {
        mZ = z;
    }

    /**
     * If the given object is also a {@code Point}, coordinate comparison follows {@link Float#compareTo(Float)},
     * i.e. equality is tested <i>exactly</i> (but with {@code NaN} equal to itself).
     *
     * <p>For testing equality with an epsilon, see {@link Point#isEqual(Point, Point, float)}.</p>
     *
     * @param obj object to compare with.
     * @return true if {@code obj} is a {@code Point} and both refer to the same position.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        final Point other = (Point) obj;

        return Float.compare(mX, other.mX) == 0 &&
                Float.compare(mY, other.mY) == 0 &&
                Float.compare(mZ, other.mZ) == 0;
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + Float.hashCode(mX);
        hash = 31 * hash + Float.hashCode(mY);
        return 31 * hash + Float.hashCode(mZ);
    }


    @Override
    public String toString()
    {
        return String.format("(%.4f,%.4f,%.4f)", mX, mY, mZ);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Computes the distance between two points.
     *
     * @param a first point.
     * @param b second point.
     * @return distance between.
     * @throws NullPointerException if either of the given points are null.
     */
    public static float distanceBetween(Point a, Point b)
    {
        requireNonNull(a, "Point A cannot be null");
        requireNonNull(b, "Point B cannot be null");

        // Compute diffs
        final float x = a.mX - b.mX;
        final float y = a.mY - b.mY;
        final float z = a.mZ - b.mZ;

        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    /**
     * Returns true if two positions are roughly equivalent within a given error.
     *
     * <p>This test will adjust {@code epsilon} when either of the coordinates is {@literal >} 1 to compensate for
     * larger coordinate values.</p>
     *
     * <p>If corresponding coordinates are both {@code NaN}, they are considered equal.</p>
     *
     * @param a first point.
     * @param b second point.
     * @param epsilon distance between coordinates before they are no longer considered equal.
     * @return true if points can be considered equal.
     * @throws NullPointerException if either of the given points are null.
     * @throws IllegalArgumentException if epsilon is {@link Float#NaN}, {@link Float#POSITIVE_INFINITY},
     * {@link Float#NEGATIVE_INFINITY}, or {@literal <} 0.
     */
    public static boolean isEqual(Point a, Point b, float epsilon)
    {
        requireNonNull(a, "Point A cannot be null");
        requireNonNull(a, "Point B cannot be null");

        if (Float.isNaN(epsilon)) {
            throw new IllegalArgumentException("Epsilon cannot be NaN");
        }
        if (Float.isInfinite(epsilon)) {
            throw new IllegalArgumentException("Epsilon cannot be infinite");
        }
        if (epsilon < 0f) {
            throw new IllegalArgumentException("Epsilon must be >= 0");
        }

        return isRoughlyEqual(a.mX, b.mX, epsilon) &&
                isRoughlyEqual(a.mY, b.mY, epsilon) &&
                isRoughlyEqual(a.mZ, b.mZ, epsilon);
    }

    /**
     * Returns {@code true} if two values are roughly equivalent (within a given epsilon). The specified epsilon is
     * adjusted when at least one of the given {@code float} values is {@literal >} 1.
     *
     * <p>Corresponding {@code NaN} coordinates are considered equal.</p>
     *
     * <p>This comparison is given at Christer Ericson's blog
     * <a href="http://realtimecollisiondetection.net/blog/?p=89">Realtime Collision Detection</a>
     * and also briefly from his GDC 2007 presentation "Physics for Game Programmers: Numerical Robustness".</p>
     *
     * @param a first value.
     * @param b second value.
     * @param epsilon exclusive difference before which values are considered equal.
     * @return true if values are roughly equal.
     */
    private static boolean isRoughlyEqual(float a, float b, float epsilon)
    {
        // Exact equality for normal values as well as NaNs and infinities
        if (Float.compare(a, b) == 0) {
            return true;
        }

        final float largerInput = Math.max(Math.abs(a), Math.abs(b));

        // When |input| < 1, epsilon is unchanged, otherwise scale to input
        final float scale = Math.max(1f, largerInput);

        // Ignore relatively small errors
        return Math.abs(a - b) < epsilon * scale;
    }
}
