package cinnamon.engine.utils;

/**
 * <p>Provides methods for changing the size and position of a {@code SphereBounds}.</p>
 */
public final class MutableSphereBounds extends SphereBounds implements DefinableBounds, Copier<SphereBounds>
{
    private static final float PI = (float) Math.PI;

    private final Point mCenter = new Point();
    private float mRadius;

    /**
     * <p>Constructs a {@code MutableSphereBounds} at origin (0,0,0) whose radius is 1.</p>
     */
    public MutableSphereBounds()
    {
        mRadius = 1f;
    }

    /**
     * <p>Constructs a {@code MutableSphereBounds} from another {@code SphereBounds}.</p>
     *
     * @param sphere sphere.
     * @throws NullPointerException if sphere is null.
     */
    public MutableSphereBounds(SphereBounds sphere)
    {
        copy(sphere);
    }

    /**
     * <p>Constructs a {@code MutableSphereBounds} with the specified radius centered on the given point.</p>
     *
     * @param radius radius.
     * @param point center.
     * @throws NullPointerException if point is null.
     */
    public MutableSphereBounds(float radius, Point point)
    {
        this(radius, point.getX(), point.getY(), point.getZ());
    }

    /**
     * <p>Constructs a {@code MutableSphereBounds} with the specified radius centered on the given point.</p>
     *
     * @param radius radius.
     * @param x center x.
     * @param y center y.
     * @param z center z.
     */
    public MutableSphereBounds(float radius, float x, float y, float z)
    {
        mRadius = radius;
        mCenter.setPosition(x, y, z);
    }

    @Override
    public void encompass(float x, float y, float z)
    {
        setRadius((float) Math.sqrt((x * x) + (y * y) + (z * z)));
    }

    @Override
    public void encompass(float minX, float minY, float minZ, float maxX, float maxY, float maxZ)
    {
        final float w = maxX - minX;
        final float h = maxY - minY;
        final float d = maxZ - minZ;

        // Center the sphere on rect
        final float centerX = minX + w / 2f;
        final float centerY = minY + h / 2f;
        final float centerZ = minZ + d / 2f;
        mCenter.setPosition(centerX, centerY, centerZ);

        // Radius is half of rectangle's diag
        final float xDiff = centerX - minX;
        final float yDiff = centerY - minY;
        final float zDiff = centerZ - minZ;
        mRadius = (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
    }

    @Override
    public Point getCenter()
    {
        return new Point(mCenter);
    }

    @Override
    public float getX()
    {
        return mCenter.getX() - mRadius;
    }

    @Override
    public float getY()
    {
        return mCenter.getY() - mRadius;
    }

    @Override
    public float getZ()
    {
        return mCenter.getZ() - mRadius;
    }

    @Override
    public float getMaximumX()
    {
        return mCenter.getX() + mRadius;
    }

    @Override
    public float getMaximumY()
    {
        return mCenter.getY() + mRadius;
    }

    @Override
    public float getMaximumZ()
    {
        return mCenter.getZ() + mRadius;
    }

    /**
     * <p>Moves to center on the given coordinates.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    @Override
    public void centerOn(float x, float y, float z)
    {
        mCenter.setPosition(x, y, z);
    }

    @Override
    public float getWidth()
    {
        return mRadius * 2f;
    }

    @Override
    public float getHeight()
    {
        return getWidth();
    }

    @Override
    public float getDepth()
    {
        return getWidth();
    }

    /**
     * <p>Gets the radius.</p>
     *
     * @return radius.
     */
    public float getRadius()
    {
        return mRadius;
    }

    /**
     * <p>Sets the radius.</p>
     *
     * @param radius radius.
     * @throws IllegalArgumentException if radius is {@literal <} 0 or {@code NaN}.
     */
    public void setRadius(float radius)
    {
        if (radius < 0f || Float.isNaN(radius)) {
            throw new IllegalArgumentException("Radius cannot be < 0 or NaN");
        }

        mRadius = radius;
    }

    @Override
    public void copy(SphereBounds object)
    {
        if (object == null) {
            throw new NullPointerException();
        }

        mCenter.copy(object.getCenter());
        mRadius = object.getRadius();
    }

    @Override
    public int hashCode()
    {
        return (17 * 31 + mCenter.hashCode()) * 31 + ((Float) mRadius).hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final MutableSphereBounds sphere = (MutableSphereBounds) obj;
        return sphere.getX() == getX() && sphere.getY() == getY()
                && sphere.getZ() == getZ() && sphere.getRadius() == getRadius();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use copy constructor instead");
    }
}
