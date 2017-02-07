package com.cinnamon.utils;

/**
 * <p>
 *     Represents a point in space of two dimensions and facilitates passing
 *     around points from one object to another.
 * </p>
 *
 *
 */
public class Point2F
{
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
     * <p>Computes the distance to another Point2F.</p>
     *
     * @param point other Point2F.
     * @return distance.
     */
    public final float distanceTo(Point2F point)
    {
        float x = (float) Math.abs(Math.pow(mX - point.mX, 2));
        float y = (float) Math.abs(Math.pow(mY - point.mY, 2));
        return (float) Math.sqrt(x + y);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor " +
                "instead");
    }

    @Override
    public String toString()
    {
        return "(" + mX + "," + mY + ")";
    }
}
