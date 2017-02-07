package com.cinnamon.utils;

/**
 * <p>Axis-aligned implementation of {@link Rect2D}.</p>
 *
 *
 */
public final class AxisAlignedRect implements Rect2D
{
    // Offset point
    private final Point2F mOffset;

    // Origin and opposite corner point form rectangle
    private final Point2F mOrigin;
    private final Point2F mCorner;

    /**
     * <p>Constructor to copy another {@link Rect2D}.</p>
     *
     * @param rect Rect2D to copy.
     */
    public AxisAlignedRect(Rect2D rect)
    {
        mOffset = new Point2F(rect.getOffsetX(), rect.getOffsetY());
        mOrigin = new Point2F(rect.getOrigin());
        mCorner = new Point2F(rect.getCorner());
    }

    /**
     * <p>Constructor for an AxisAlignedRect of a specific width and height.</p>
     *
     * @param width width.
     * @param height height.
     */
    public AxisAlignedRect(float width, float height)
    {
        this(width, height, 0, 0);
    }

    /**
     * <p>Constructor for an AxisAlignedRect of a specific width and height
     * at a specific (x,y) position.</p>
     *
     * @param width width.
     * @param height height.
     * @param x x.
     * @param y y.
     */
    public AxisAlignedRect(float width, float height, float x, float y)
    {
        mOffset = new Point2F(0, 0);
        mOrigin = new Point2F(x, y);
        mCorner = new Point2F(x + width, y + height);
    }

    @Override
    public Point2F getOrigin()
    {
        return mOrigin;
    }

    @Override
    public Point2F getCorner()
    {
        return mCorner;
    }

    @Override
    public float getX()
    {
        return mOrigin.getX() + mOffset.getX();
    }

    @Override
    public float getWidth()
    {
        return mCorner.getX() - mOrigin.getX();
    }

    @Override
    public float getY()
    {
        return mOrigin.getY() + mOffset.getY();
    }

    @Override
    public void setWidth(float width)
    {
        mCorner.setX(getX() + width);
    }

    @Override
    public float getZ()
    {
        return 0;
    }

    @Override
    public float getHeight()
    {
        return mCorner.getY() - mOrigin.getY();
    }

    @Override
    public void setHeight(float height)
    {
        mCorner.setY(getY() + height);
    }

    @Override
    public void moveTo(float x, float y, float z)
    {
        mCorner.translateBy(x - mOrigin.getX(), y - mOrigin.getY());
        mOrigin.set(x, y);
    }

    @Override
    public void moveTo(float x, float y)
    {
        moveTo(x, y, 0);
    }

    @Override
    public boolean contains(float x, float y)
    {
        // Test if point is outside rectangle
        if (x < getX()
                || y < getY()
                || x > (mCorner.getX() + mOffset.getX())
                || y > (mCorner.getY() + mOffset.getY())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean intersects(Rect2D rect)
    {
        // Get other rect's corner, offset adjusted
        final Point2F corner = rect.getCorner();
        final float cornerX = corner.getX() + rect.getOffsetX();
        final float cornerY = corner.getY() + rect.getOffsetY();

        // Test for intersection
        return intersects(rect.getX(), getY(), cornerX, cornerY);
    }

    @Override
    public boolean intersects(float originX, float originY, float cornerX,
                              float cornerY)
    {
        // Adjust calling Rect2D's corner with offset
        final float cornerXA = mCorner.getX() + mOffset.getX();
        final float cornerYA = mCorner.getY() + mOffset.getY();

        // Test if corners are separated
        return !(cornerX < getX() || cornerY < getY())
                && !(cornerXA < originX || cornerYA < originY);
    }

    @Override
    public void setOffset(float x, float y)
    {
        setOffset(x, y, 0);
    }

    @Override
    public void setOffset(float x, float y, float z)
    {
        mOffset.set(x, y);
    }

    @Override
    public float getOffsetX()
    {
        return mOffset.getX();
    }

    @Override
    public float getOffsetY()
    {
        return mOffset.getY();
    }

    @Override
    public float getOffsetZ()
    {
        return 0;
    }
}
