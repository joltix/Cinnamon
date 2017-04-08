package com.cinnamon.utils;

/**
 * <p>Axis-aligned implementation of {@link Rect2D}.</p>
 */
public final class AxisAlignedRect implements Rect2D
{
    // Origin point
    private final Point3F mPosition;

    // Corner point forming rectangle with origin
    private final Point3F mCorner;

    /**
     * <p>Constructor to copy another {@link Rect2D}.</p>
     *
     * @param rect Rect2D to copy.
     */
    public AxisAlignedRect(Rect2D rect)
    {
        mPosition = new Point3F(rect.getPosition());
        mCorner = new Point3F(rect.getCorner());
    }

    /**
     * <p>Constructor for an AxisAlignedRect of a specific width and height positioned at (0,0).</p>
     *
     * @param width width.
     * @param height height.
     */
    public AxisAlignedRect(float width, float height)
    {
        this(width, height, 0f, 0f);
    }

    /**
     * <p>Constructor for an AxisAlignedRect of a specific width and height positioned at a specific (x,y) point.</p>
     *
     * @param width width.
     * @param height height.
     * @param x x.
     * @param y y.
     */
    public AxisAlignedRect(float width, float height, float x, float y)
    {
        mPosition = new Point3F(x, y, 0f);
        mCorner = new Point3F(x + width, y + height, 0f);
    }

    @Override
    public boolean contains(float x, float y)
    {
        // Test if point is outside rectangle
        return !(x < getX() || y < getY() || x > getCornerX() || y > getCornerY());
    }

    @Override
    public boolean contains(float x, float y, float z)
    {
        // Z must be the same for containment
        if (Point2F.isEqual(getZ(), z)) {
            return false;
        }

        return contains(x, y);
    }

    @Override
    public boolean contains(Rect2D rect)
    {
        return contains(rect.getX(), rect.getY(), rect.getCornerX(), rect.getCornerY());
    }

    @Override
    public boolean contains(float x, float y, float cornerX, float cornerY)
    {
        // Check if origin point is within AxisAlignedRect
        final boolean originContained = x >= getX() && y >= getY();

        // Check if corner point is within as well
        return originContained && cornerX <= getCornerX() && cornerY <= getCornerY();
    }

    @Override
    public boolean intersects(Rect2D rect)
    {
        return intersects(rect.getX(), rect.getY(), rect.getCornerX(), rect.getCornerY());
    }

    @Override
    public boolean intersects(float x, float y, float cornerX, float cornerY)
    {
        // Adjust calling Rect2D's corner with offset
        final float cornerXA = getCornerX();
        final float cornerYA = getCornerY();

        // Test if corners are separated
        return !(cornerX < getX() || cornerY < getY() || cornerXA < x || cornerYA < y);
    }

    @Override
    public Point3F getPosition()
    {
        return new Point3F(mPosition);
    }

    @Override
    public Point3F getCorner()
    {
        return new Point3F(mCorner);
    }

    @Override
    public float getX()
    {
        return mPosition.getX();
    }

    @Override
    public float getY()
    {
        return mPosition.getY();
    }

    @Override
    public float getZ()
    {
        return mPosition.getZ();
    }

    @Override
    public float getCornerX()
    {
        return mCorner.getX();
    }

    @Override
    public float getCornerY()
    {
        return mCorner.getY();
    }

    @Override
    public float getCornerZ()
    {
        return mCorner.getZ();
    }

    @Override
    public void moveTo(float x, float y)
    {
        moveTo(x, y, mPosition.getZ());
    }

    @Override
    public void moveTo(float x, float y, float z)
    {
        mCorner.translateBy(x - mPosition.getX(), y - mPosition.getY(), z - mPosition.getZ());
        mPosition.set(x, y, z);
    }

    @Override
    public void moveBy(float x, float y)
    {
        moveBy(x, y, 0f);
    }

    @Override
    public void moveBy(float x, float y, float z)
    {
        mPosition.set(mPosition.getX() + x, mPosition.getY() + y, mPosition.getZ() + z);
        mCorner.set(mCorner.getX() + x, mCorner.getY() + y, mCorner.getZ() + z);
    }

    @Override
    public void moveToCenter(float x, float y)
    {
        moveTo(x - (getWidth() / 2f), y - (getHeight() / 2f));
    }

    @Override
    public float getWidth()
    {
        return mCorner.getX() - mPosition.getX();
    }

    @Override
    public void setWidth(float width)
    {
        mCorner.setX(mPosition.getX() + width);
    }

    @Override
    public float getHeight()
    {
        return mCorner.getY() - mPosition.getY();
    }

    @Override
    public void setHeight(float height)
    {
        mCorner.setY(mPosition.getY() + height);
    }

    @Override
    public float getCenterX()
    {
        return getX() + (getWidth() / 2f);
    }

    @Override
    public float getCenterY()
    {
        return getY() + (getHeight() / 2f);
    }
}
