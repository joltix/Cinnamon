package cinnamon.engine.utils;

/**
 * <p>A movable point in three dimensions.</p>
 */
public final class Point implements Repositionable, Copier<Point>
{
    private float mX;
    private float mY;
    private float mZ;

    /**
     * <p>Constructs a <tt>Point</tt> whose coordinates are (0,0,0).</p>
     */
    public Point()
    {
        this(0f, 0f, 0f);
    }

    /**
     * <p>Constructs a <tt>Point</tt> from the given coordinates.</p>
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
     * <p>Constructs a <tt>Point</tt> with the same coordinates as another.</p>
     *
     * @param point to copy.
     * @throws NullPointerException if the given point is null.
     */
    public Point(Point point)
    {
        copy(point);
    }

    /**
     * <p>Copies all coordinates from the given <tt>Point</tt>.</p>
     *
     * @param point to copy.
     * @throws NullPointerException if the given point is null.
     */
    @Override
    public void copy(Point point)
    {
        if (point == null) {
            throw new NullPointerException("Cannot copy null point");
        }

        mX = point.mX;
        mY = point.mY;
        mZ = point.mZ;
    }

    @Override
    public void setPosition(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    @Override
    public void addX(float x)
    {
        mX += x;
    }

    @Override
    public void addY(float y)
    {
        mY += y;
    }

    @Override
    public void addZ(float z)
    {
        mZ += z;
    }

    @Override
    public float getX()
    {
        return mX;
    }

    @Override
    public void setX(float x)
    {
        mX = x;
    }

    @Override
    public float getY()
    {
        return mY;
    }

    @Override
    public void setY(float y)
    {
        mY = y;
    }

    @Override
    public float getZ()
    {
        return mZ;
    }

    @Override
    public void setZ(float z)
    {
        mZ = z;
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + ((Float) mX).hashCode();
        hash = 31 * hash + ((Float) mY).hashCode();
        return 31 * hash + ((Float) mZ).hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison between two coordinates of {@link Float#NaN} are treated as true and no distinction is made
     * between positive and negative zeros. Floating point comparison is exact (delta of 0).</p>
     *
     * @param obj the point with which to compare.
     * @return true if the given object is a point and both points refer to the exact same coordinates.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        final Point pt = (Point) obj;

        // Compare coordinates (account for corresponding being NaN)
        boolean sameComps = (mX == pt.mX || (Float.isNaN(mX) && Float.isNaN(pt.mX)));
        sameComps = sameComps && (mY == pt.mY || (Float.isNaN(mY) && Float.isNaN(pt.mY)));
        sameComps = sameComps && (mZ == pt.mZ || (Float.isNaN(mZ) && Float.isNaN(pt.mZ)));

        return sameComps;
    }

    @Override
    public String toString()
    {
        return "(" + mX + "," + mY + "," + mZ + ")";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }

    /**
     * <p>Computes the distance between two points.</p>
     *
     * @param pointA first point.
     * @param pointB second point.
     * @return distance between.
     * @throws NullPointerException if either of the given points are null.
     */
    public static float distanceBetween(Point pointA, Point pointB)
    {
        if (pointA == null) {
            throw new NullPointerException("Point A cannot be null");
        }
        if (pointB == null) {
            throw new NullPointerException("Point B cannot be null");
        }

        // Compute diffs
        final float x = pointA.mX - pointB.mX;
        final float y = pointA.mY - pointB.mY;
        final float z = pointA.mZ - pointB.mZ;

        return (float) Math.sqrt((x * x) + (y * y) + (z * z));
    }

    /**
     * <p>Checks if two Points are roughly equivalent using a given delta. If corresponding coordinates are NaN, they
     * are considered equal.</p>
     *
     * @param pointA first point.
     * @param pointB second point.
     * @param delta difference allowed between coordinates before they are no longer considered equal.
     * @return true if coordinates are roughly equivalent.
     * @throws NullPointerException if either of the given points are null.
     * @throws IllegalArgumentException if delta &lt; 0.
     */
    public static boolean isEqual(Point pointA, Point pointB, float delta)
    {
        if (pointA == null) {
            throw new NullPointerException("Point A cannot be null");
        }
        if (pointB == null) {
            throw new NullPointerException("Point B cannot be null");
        }
        if (delta < 0f) {
            throw new IllegalArgumentException("Delta must be >= 0");
        }

        boolean equal = isEqual(pointA.mX, pointB.mX, delta);
        equal = equal && isEqual(pointA.mY, pointB.mY, delta);
        return equal && isEqual(pointA.mZ, pointB.mZ, delta);
    }

    /**
     * <p>Checks if two coordinates are roughly equivalent using a given delta. If corresponding coordinates are NaN,
     * they are considered equal.</p>
     *
     * @param valueA first coordinate.
     * @param valueB second coordinate.
     * @param delta difference allowed between coordinates before they are no longer considered equal.
     * @return true if coordinates can be considered equal.
     */
    private static boolean isEqual(float valueA, float valueB, float delta)
    {
        return (Float.isNaN(valueA) && Float.isNaN(valueB)) || (Math.abs(valueA - valueB) < delta);
    }
}
