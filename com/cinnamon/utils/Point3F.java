package com.cinnamon.utils;

/**
 * <p>
 *     Represents a point in space of three dimensions and facilitates passing around coordinates.
 * </p>
 */
public class Point3F extends Point2F
{
    // Z depth coord
    private float mZ;

    /**
     * <p>Constructor for a three dimensional point.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public Point3F(float x, float y, float z)
    {
        super(x, y);
        mZ = z;
    }

    /**
     * <p>Constructor for copying a three dimensional point.</p>
     *
     * @param point point to copy.
     */
    public Point3F(Point3F point)
    {
        this(point.getX(), point.getY(), point.getZ());
    }

    /**
     * <p>Gets the z coordinate.</p>
     *
     * @return z.
     */
    public final float getZ()
    {
        return mZ;
    }

    /**
     * <p>Sets the z coordinate.</p>
     *
     * @param z z.
     */
    public final void setZ(float z)
    {
        mZ = z;
    }

    /**
     * <p>Translates the point by some x, y, and z amount.</p>
     *
     * @param x x to add.
     * @param y y to add.
     * @param z z to add.
     */
    public void translateBy(float x, float y, float z)
    {
        super.translateBy(x, y);
        mZ += z;
    }

    /**
     * <p>Sets the x, y, and z coordinates.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public final void set(float x, float y, float z)
    {
        super.set(x, y);
        mZ = z;
    }

    /**
     * <p>Computes the distance to another Point3F.</p>
     *
     * @param point other Point3F.
     * @return distance.
     */
    public final double distanceTo(Point3F point)
    {
        return Point3F.distanceBetween(getX(), getY(), mZ, point.getX(), point.getY(), point.mZ);
    }

    /**
     * <p>Computes the distance between two (x,y,z) points.</p>
     *
     * @param x0 x.
     * @param y0 y.
     * @param z0 z.
     * @param x1 other x.
     * @param y1 other y.
     * @param z1 other z.
     * @return distance between.
     */
    public static final double distanceBetween(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        final double x = Math.abs(x0 - x1);
        final double y = Math.abs(y0 - y1);
        final double z = Math.abs(z0 - z1);
        return Math.sqrt((x * x) + (y * y) + (z * z));
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }

    @Override
    public String toString()
    {
        return "(" + getX() + "," + getY() + "," + mZ + ")";
    }
}
