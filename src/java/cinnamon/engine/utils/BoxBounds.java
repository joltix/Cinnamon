package cinnamon.engine.utils;

import static java.util.Objects.requireNonNull;

/**
 * An axis-aligned rectangular bounding volume, also known as an AABB (Axis-Aligned Bounding Box).
 */
public final class BoxBounds extends Bounds
{
    // Minimum X
    private float mX;

    // Minimum Y
    private float mY;

    // Minimum Z
    private float mZ;

    private float mWidth;

    private float mHeight;

    private float mDepth;

    /**
     * Constructs a {@code BoxBounds} to contain another {@code Bounds}.
     * 
     * <p>If the given {@code Bounds} is a {@code BoxBounds}, the new {@code BoxBounds} will be a copy.</p>
     * 
     * @param bounds to copy.
     * @throws NullPointerException if {@code bounds} is {@code null}.
     */
    public BoxBounds(Bounds bounds)
    {
        requireNonNull(bounds);

        mX = bounds.getMinimumX();
        mY = bounds.getMinimumY();
        mZ = bounds.getMinimumZ();

        mWidth = bounds.getMaximumX() - mX;
        mHeight = bounds.getMaximumY() - mY;
        mDepth = bounds.getMaximumZ() - mZ;
    }

    /**
     * Constructs a {@code BoxBounds} centered at (0,0,0).
     * 
     * @param size size.
     * @throws NullPointerException if {@code size} is {@code null}.
     * @throws IllegalArgumentException if {@code size} has a dimension whose value is {@literal <} 0,
     * {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public BoxBounds(Size size)
    {
        this(new Point(), size);

        // Shift backwards so center is (0,0,0)
        mX = -mWidth / 2f;
        mY = -mHeight / 2f;
        mZ = -mDepth / 2f;
    }

    /**
     * Constructs a {@code BoxBounds}.
     * 
     * @param minimum minimum point.
     * @param size size.
     * @throws NullPointerException if {@code minimum} or {@code size} is {@code null}.
     * @throws IllegalArgumentException if either {@code minimum} has a coordinate or
     * {@code size} has a dimension whose value is {@literal <} 0, {@code Float.NaN}, {@code Float.POSITIVE_INFINITY},
     * or {@code Float.NEGATIVE_INFINITY}.
     */
    public BoxBounds(Point minimum, Size size)
    {
        requireNonNull(minimum);
        requireNonNull(size);

        checkValueNotNaN("Minimum x", minimum.getX());
        checkValueNotNaN("Minimum y", minimum.getY());
        checkValueNotNaN("Minimum z", minimum.getZ());

        checkValueNotInfinite("Minimum x", minimum.getX());
        checkValueNotInfinite("Minimum y", minimum.getY());
        checkValueNotInfinite("Minimum z", minimum.getZ());

        checkValueNotNaN("Width", size.getWidth());
        checkValueNotNaN("Height", size.getHeight());
        checkValueNotNaN("Depth", size.getDepth());

        checkValueNotInfinite("Width", size.getWidth());
        checkValueNotInfinite("Height", size.getHeight());
        checkValueNotInfinite("Depth", size.getDepth());

        checkDimensionNotNegative("Width", size.getWidth());
        checkDimensionNotNegative("Height", size.getHeight());
        checkDimensionNotNegative("Depth", size.getDepth());

        mX = minimum.getX();
        mY = minimum.getY();
        mZ = minimum.getZ();

        mWidth = size.getWidth();
        mHeight = size.getHeight();
        mDepth = size.getDepth();
    }

    /**
     * <p>Tests for containment while treating the given {@code Bounds} as a {@code BoxBounds} where the bounds' size
     * is implied by subtracting its minimum from its maximum. This method is identical to
     * {@link #contains(BoxBounds)}.</p>
     */
    @Override
    public boolean contains(Bounds bounds)
    {
        requireNonNull(bounds);

        return containsBox(bounds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sides that lie on each other are considered to be within the boundary.</p>
     */
    @Override
    public boolean contains(BoxBounds bounds)
    {
        requireNonNull(bounds);

        return containsBox(bounds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>A point that lies on a side is considered to be within the boundary.</p>
     */
    @Override
    public boolean contains(Point point)
    {
        requireNonNull(point);

        checkValueNotNaN("X", point.getX());
        checkValueNotNaN("Y", point.getY());
        checkValueNotNaN("Z", point.getZ());
        checkValueNotInfinite("X", point.getX());
        checkValueNotInfinite("Y", point.getY());
        checkValueNotInfinite("Z", point.getZ());

        final float maxX = getMaximumX();
        final float maxY = getMaximumY();
        final float maxZ = getMaximumZ();

        final float pX = point.getX();
        final float pY = point.getY();
        final float pZ = point.getZ();
        
        // Point can be thought of as a box whose min/max are the same
        return BoxBounds.isRectInsideRect(pX, pY, pZ, pX, pY, pZ, mX, mY, mZ, maxX, maxY, maxZ);
    }

    /**
     * <p>Tests for intersection while treating the given {@code Bounds} as a {@code BoxBounds} where the bounds' size
     * is implied by subtracting its minimum from its maximum. This method is identical to
     * {@link #intersects(BoxBounds)}.</p>
     */
    @Override
    public boolean intersects(Bounds bounds)
    {
        requireNonNull(bounds);

        return intersectsBox(bounds);
    }

    @Override
    public boolean intersects(BoxBounds bounds)
    {
        requireNonNull(bounds);

        return intersectsBox(bounds);
    }

    /**
     * Sets the size.
     *
     * @param width width.
     * @param height height.
     * @param depth depth.
     * @throws IllegalArgumentException if {@code width}, {@code height}, or {@code depth} is {@literal <} 0,
     * {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setSize(float width, float height, float depth)
    {
        checkValueNotNaN("Width", width);
        checkValueNotNaN("Height", height);
        checkValueNotNaN("Depth", depth);

        checkValueNotInfinite("Width", width);
        checkValueNotInfinite("Height", height);
        checkValueNotInfinite("Depth", depth);

        checkDimensionNotNegative("Width", width);
        checkDimensionNotNegative("Height", height);
        checkDimensionNotNegative("Depth", depth);

        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    @Override
    public Point getCenter()
    {
        return new Point(mWidth / 2f + mX, mHeight / 2f + mY, mDepth / 2f + mZ);
    }

    /**
     * Sets the minimum point.
     *
     * @param x minimum x.
     * @param y minimum y.
     * @param z minimum z.
     * @throws IllegalArgumentException if {@code x}, {@code y}, or {@code z} is {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setMinimum(float x, float y, float z)
    {
        checkValueNotNaN("X", x);
        checkValueNotNaN("Y", y);
        checkValueNotNaN("Z", z);

        checkValueNotInfinite("X", x);
        checkValueNotInfinite("Y", y);
        checkValueNotInfinite("Z", z);

        mX = x;
        mY = y;
        mZ = z;
    }

    /**
     * Gets the width.
     *
     * @return width.
     */
    public float getWidth()
    {
        return mWidth;
    }

    /**
     * Gets the height.
     *
     * @return height.
     */
    public float getHeight()
    {
        return mHeight;
    }

    /**
     * Gets the depth.
     *
     * @return depth.
     */
    public float getDepth()
    {
        return mDepth;
    }

    @Override
    public float getMinimumX()
    {
        return mX;
    }

    @Override
    public float getMinimumY()
    {
        return mY;
    }

    @Override
    public float getMinimumZ()
    {
        return mZ;
    }

    @Override
    public float getMaximumX()
    {
        return mX + mWidth;
    }

    @Override
    public float getMaximumY()
    {
        return mY + mHeight;
    }

    @Override
    public float getMaximumZ()
    {
        return mZ + mDepth;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != BoxBounds.class) {
            return false;
        } else if (object == this) {
            return true;
        }

        final BoxBounds other = (BoxBounds) object;

        return mX == other.mX && mY == other.mY && mZ == other.mY && 
            mWidth == other.mWidth && mHeight == other.mHeight && mDepth == other.mDepth; 
    }

    @Override
    public int hashCode()
    {
        int hash = 31 * 17;

        hash = 31 * hash + Float.hashCode(mX);
        hash = 31 * hash + Float.hashCode(mY);
        hash = 31 * hash + Float.hashCode(mZ);
        hash = 31 * hash + Float.hashCode(mWidth);
        hash = 31 * hash + Float.hashCode(mHeight);

        return 31 * hash + Float.hashCode(mDepth);
    }

    /**
     * Tests for containment while treating the given {@code Bounds} as a {@code BoxBounds}.
     *
     * @param bounds bounds.
     * @return {@code true} if {@code bounds} is completely inside.
     */
    private boolean containsBox(Bounds bounds)
    {
        // Max point
        final float maxX = getMaximumX();
        final float maxY = getMaximumY();
        final float maxZ = getMaximumZ();

        // Other bounds' min point
        final float otherMinX = bounds.getMinimumX();
        final float otherMinY = bounds.getMinimumY();
        final float otherMinZ = bounds.getMinimumZ();

        // Other bounds' max point
        final float otherMaxX = bounds.getMaximumX();
        final float otherMaxY = bounds.getMaximumY();
        final float otherMaxZ = bounds.getMaximumZ();

        return BoxBounds.isRectInsideRect(otherMinX, otherMinY, otherMinZ, otherMaxX, otherMaxY, otherMaxZ, mX, mY, mZ, maxX, maxY, maxZ);
    }

    /**
     * Tests for intersection while treating the given {@code Bounds} as a {@code BoxBounds}.
     *
     * @param bounds bounds.
     * @return {@code true} if {@code bounds} intersects.
     */
    private boolean intersectsBox(Bounds bounds)
    {
        // Max point
        final float maxX = mX + mWidth;
        final float maxY = mY + mHeight;
        final float maxZ = mZ + mDepth;

        // Other bounds' min point
        final float otherMinX = bounds.getMinimumX();
        final float otherMinY = bounds.getMinimumY();
        final float otherMinZ = bounds.getMinimumZ();

        // Other bounds' max point
        final float otherMaxX = bounds.getMaximumX();
        final float otherMaxY = bounds.getMaximumY();
        final float otherMaxZ = bounds.getMaximumZ();

        return !(mX > otherMaxX || mY > otherMaxY || mZ > otherMaxZ ||
                maxX < otherMinX || maxY < otherMinY || maxZ < otherMinZ);
    }

    private void checkDimensionNotNegative(String dimension, float value)
    {
        if (value <= 0f) {
            final String format = "%s cannot be negative, given: %f";
            throw new IllegalArgumentException(String.format(format, dimension, value));
        }
    }

    private void checkValueNotInfinite(String name, float value)
    {
        if (Float.isInfinite(value)) {
            final String format = "%s cannot be infinite, given: %f";
            throw new IllegalArgumentException(String.format(format, name, value));
        }
    }

    private void checkValueNotNaN(String name, float value)
    {
        if (Float.isNaN(value)) {
            final String format = "%s cannot be NaN";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    /**
     * Returns {@code true} if the rectangle defined by the first set of minimum and maximum points (inner) is inside
     * the rectangle defined by the second set of points (outer).
     *
     * <p>Overlapping sides are considered "inside".</p>
     *
     * @param minX0 inner minimum x.
     * @param minY0 inner minimum y.
     * @param minZ0 inner minimum z.
     * @param maxX0 inner maximum x.
     * @param maxY0 inner maximum y.
     * @param maxZ0 inner maximum z.
     *
     * @param minX1 outer minimum x.
     * @param minY1 outer minimum y.
     * @param minZ1 outer minimum z.
     * @param maxX1 outer maximum x.
     * @param maxY1 outer maximum y.
     * @param maxZ1 outer maximum z.
     */
    private static boolean isRectInsideRect(float minX0, float minY0, float minZ0,
                                            float maxX0, float maxY0, float maxZ0,
                                            float minX1, float minY1, float minZ1,
                                            float maxX1, float maxY1, float maxZ1)
    {
        return minX1 <= minX0 && minY1 <= minY0 && minZ1 <= minZ0 &&
            maxX1 >= maxX0 && maxY1 >= maxY0 && maxZ1 >= maxZ0;
    }
}
