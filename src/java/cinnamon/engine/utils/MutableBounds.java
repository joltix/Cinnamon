package cinnamon.engine.utils;

/**
 * <p>{@code Bounds} maintaining its box shape while allowing changes to size and position.</p>
 */
public final class MutableBounds extends Bounds implements DefinableBounds, Copier<Bounds>
{
    // Forms diagonal crossing box in 3D
    private final Point mMinPt = new Point(0f, 0f, 0f);
    private final Point mMaxPt = new Point(0f, 0f, 0f);

    /**
     * <p>Constructs a {@code MutableBounds} representing a box at (0,0,0) whose width, height, and depth is 1.</p>
     */
    public MutableBounds()
    {
        mMinPt.setPosition(-0.5f, -0.5f, -0.5f);
        mMaxPt.setPosition(0.5f, 0.5f, 0.5f);
    }

    /**
     * <p>Constructs a {@code MutableBounds} with the same position and size as another {@code Bounds}.</p>
     *
     * @param bounds to copy.
     * @throws NullPointerException if bounds is null.
     */
    public MutableBounds(Bounds bounds)
    {
        copy(bounds);
    }

    /**
     * <p>Constructs a {@code MutableBounds} with the specified dimensions and the given minimum point.</p>
     *
     * @param width width.
     * @param height height.
     * @param depth depth.
     * @param point point.
     * @throws NullPointerException if point is null.
     */
    public MutableBounds(float width, float height, float depth, Point point)
    {
        this(width, height, depth, point.getX(), point.getY(), point.getZ());
    }

    /**
     * <p>Constructs a {@code MutableBounds} with the specified dimensions and the given minimum point.</p>
     *
     * @param width width.
     * @param height height.
     * @param depth depth.
     * @param x minimum x.
     * @param y minimum y.
     * @param z minimum z.
     */
    public MutableBounds(float width, float height, float depth, float x, float y, float z)
    {
        mMinPt.setPosition(x, y, z);
        mMaxPt.setPosition(x + width, y + height, z + depth);
    }

    @Override
    public void encompass(float x, float y, float z)
    {
        // Change corners to include point
        mMinPt.setX(Math.min(x, mMinPt.getX()));
        mMinPt.setY(Math.min(y, mMinPt.getY()));
        mMinPt.setZ(Math.min(z, mMinPt.getZ()));
        mMaxPt.setX(Math.max(x, mMaxPt.getX()));
        mMaxPt.setY(Math.max(y, mMaxPt.getY()));
        mMaxPt.setZ(Math.max(z, mMaxPt.getZ()));
    }

    @Override
    public void encompass(float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        assert(minX <= maxX && minY <= maxY && minZ <= maxZ);

        mMinPt.setPosition(minX, minY, minZ);
        mMaxPt.setPosition(maxX, maxY, maxZ);
    }

    @Override
    public void centerOn(float x, float y, float z)
    {
        final float halfW = getWidth() / 2f;
        final float halfH = getHeight() / 2f;
        final float halfD = getDepth() / 2f;

        mMinPt.setPosition(x - halfW, y - halfH, z - halfD);
        mMaxPt.setPosition(x + halfW, y + halfH, z + halfD);
    }

    @Override
    public float getWidth()
    {
        return mMaxPt.getX() - mMinPt.getX();
    }

    @Override
    public float getHeight()
    {
        return mMaxPt.getY() - mMinPt.getY();
    }

    @Override
    public float getDepth()
    {
        return mMaxPt.getZ() - mMinPt.getZ();
    }

    @Override
    public float getX()
    {
        return mMinPt.getX();
    }

    @Override
    public float getY()
    {
        return mMinPt.getY();
    }

    @Override
    public float getZ()
    {
        return mMinPt.getZ();
    }

    @Override
    public float getMaximumX()
    {
        return mMaxPt.getX();
    }

    @Override
    public float getMaximumY()
    {
        return mMaxPt.getY();
    }

    @Override
    public float getMaximumZ()
    {
        return mMaxPt.getZ();
    }

    @Override
    public void copy(Bounds object)
    {
        if (object == null) {
            throw new NullPointerException();
        }

        final float x = object.getX();
        final float y = object.getY();
        final float z = object.getZ();

        mMinPt.setPosition(x, y, z);
        mMaxPt.setPosition(x + object.getWidth(), y+ object.getHeight(), z + object.getDepth());
    }

    @Override
    public int hashCode()
    {
        return (17 * 31 + mMinPt.hashCode()) * 31 + mMaxPt.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final MutableBounds bounds = (MutableBounds) obj;
        return bounds.mMinPt.equals(mMinPt) && bounds.mMaxPt.equals(mMaxPt);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }
}