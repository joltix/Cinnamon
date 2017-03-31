package com.cinnamon.utils;

/**
 * <p>
 *     Represents a point in space of two dimensions and facilitates passing around coordinates.
 * </p>
 */
public class Point2F
{
    // Precision to use for floating point equivalence comparison
    public static final float PRECISION = 0.000001f;

    // Coords
    private float mX;
    private float mY;

    /**
     * <p>Constructor for a two dimensional point.</p>
     *
     * @param x x.
     * @param y y.
     */
    public Point2F(float x, float y)
    {
        mX = x;
        mY = y;
    }

    /**
     * <p>Constructor for copying a two dimensional point.</p>
     *
     * @param point point to copy.
     */
    public Point2F(Point2F point)
    {
        mX = point.mX;
        mY = point.mY;
    }

    /**
     * <p>Gets the x coordinate.</p>
     *
     * @return x.
     */
    public final float getX()
    {
        return mX;
    }

    /**
     * <p>Sets the x coordinate.</p>
     *
     * @param x x.
     */
    public final void setX(float x)
    {
        mX = x;
    }

    /**
     * <p>Gets the y coordinate.</p>
     *
     * @return y.
     */
    public final float getY()
    {
        return mY;
    }

    /**
     * <p>Sets the y coordinate.</p>
     *
     * @param y y.
     */
    public final void setY(float y)
    {
        mY = y;
    }

    /**
     * <p>Sets the x and y coordinates.</p>
     *
     * @param x x.
     * @param y y.
     */
    public final void set(float x, float y)
    {
        mX = x;
        mY = y;
    }

    /**
     * <p>Translates the point by some x and y amount.</p>
     *
     * @param x x to add.
     * @param y y to add.
     */
    public void translateBy(float x, float y)
    {
        mX += x;
        mY += y;
    }

    /**
     * <p>Computes the distance to another Point2F.</p>
     *
     * @param point other Point2F.
     * @return distance.
     */
    public final double distanceTo(Point2F point)
    {
        return Point2F.distanceBetween(mX, mY, point.mX, point.mY);
    }

    /**
     * <p>Computes the distance between two (x,y) points.</p>
     *
     * @param x0 x.
     * @param y0 y.
     * @param x1 other x.
     * @param y1 other y.
     * @return distance between.
     */
    public static final double distanceBetween(double x0, double y0, double x1, double y1)
    {
        final double xDiff = Math.abs(x1 - x0);
        final double yDiff = Math.abs(y1 - y0);

        return Math.sqrt((xDiff * xDiff) + (yDiff * yDiff));
    }

    /**
     * <p>Checks if two coordinates are roughly equivalent with a precision up to {@link #PRECISION}.</p>
     *
     * @param val0 one coordinate.
     * @param val1 other coordinate.
     * @return true if coordinates can be considered the same.
     */
    public static final boolean isEqual(float val0, float val1)
    {
        return Math.abs(val0 - val1) < Point2F.PRECISION;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }

    @Override
    public String toString()
    {
        return "(" + mX + "," + mY + ")";
    }
}
