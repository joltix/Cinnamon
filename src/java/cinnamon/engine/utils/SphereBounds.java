package cinnamon.engine.utils;

/**
 * <p>Implementation of {@code Bounds} whose containment and intersection tests have been overridden to perform
 * against a sphere instead of the implicit box defined by {@code Bounds}.</p>
 */
public abstract class SphereBounds extends Bounds
{
    // Cached to skip casting each volume/surface area computation
    private static final float PI = (float) Math.PI;

    protected SphereBounds() { }

    @Override
    public boolean contains(Bounds bounds)
    {
        return SphereBounds.doesBoxHaveAllCornersInCircle(bounds, this);
    }

    @Override
    public boolean contains(SphereBounds sphere)
    {
        // Can't be contained if other's bigger
        if (sphere.getRadius() > getRadius()) {
            return false;
        }

        // Treat given sphere as box
        final float otherDiameter = sphere.getWidth();
        final float otherMinX = sphere.getX();
        final float otherMinY = sphere.getY();
        final float otherMinZ = sphere.getZ();
        final float otherMaxX = otherMinX + otherDiameter;
        final float otherMaxY = otherMinY + otherDiameter;
        final float otherMaxZ = otherMinZ + otherDiameter;

        // Treat self as box
        final float diameter = getWidth();
        final float minX = getX();
        final float minY = getY();
        final float minZ = getZ();
        final float maxX = minX + diameter;
        final float maxY = minY + diameter;
        final float maxZ = minZ + diameter;

        // Sphere containment test reduces to box containment
        return MutableBounds.isBoxContainedInBox(otherMinX, otherMinY, otherMinZ, otherMaxX, otherMaxY, otherMaxZ,
                minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean contains(Point point)
    {
        return isSphereIntersectingSphere(this, point.getX(), point.getY(), point.getZ(), 0f);
    }

    @Override
    public boolean intersects(Bounds bounds)
    {
        return Bounds.doesBoxHaveACornerInSphere(bounds, this);
    }

    @Override
    public boolean intersects(SphereBounds sphere)
    {
        return isSphereIntersectingSphere(this, sphere);
    }

    /**
     * <p>Gets the radius. This is always half of the width, height, and depth.</p>
     *
     * @return radius.
     */
    public abstract float getRadius();

    @Override
    public float getVolume()
    {
        final float r = getRadius();
        return 4f * PI * r * r * r / 3f;
    }

    @Override
    public float getSurfaceArea()
    {
        final float r = getRadius();
        return 4f * PI * r * r;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is equivalent to {@code 2x radius}, or diameter.</p>
     *
     * @return diameter.
     */
    @Override
    public float getWidth()
    {
        return getRadius() * 2f;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is equivalent to {@code 2x radius}, or diameter.</p>
     *
     * @return diameter.
     */
    @Override
    public float getHeight()
    {
        return getRadius() * 2f;
    }

    /**
     * {@inheritDoc}
     *
     * <p>This is equivalent to {@code 2x radius}, or diameter.</p>
     *
     * @return diameter.
     */
    @Override
    public float getDepth()
    {
        return getRadius() * 2f;
    }

    /**
     * <p>Tests if two spheres intersect.</p>
     *
     * @param sphere first sphere.
     * @param other second sphere.
     * @return true if intersecting.
     */
    private static boolean isSphereIntersectingSphere(SphereBounds sphere, SphereBounds other)
    {
        // Compute radius and center point of other sphere
        final float otherRadius = other.getRadius();
        final float otherCenterX = other.getX() + otherRadius;
        final float otherCenterY = other.getY() + otherRadius;
        final float otherCenterZ = other.getZ() + otherRadius;

        return SphereBounds.isSphereIntersectingSphere(sphere, otherCenterX, otherCenterY, otherCenterZ, otherRadius);
    }

    /**
     * <p>Tests if two spheres intersect.</p>
     *
     * @param sphere sphere.
     * @param centerX other sphere's center x.
     * @param centerY other sphere's center y.
     * @param centerZ other sphere's center z.
     * @param radius other sphere's radius.
     * @return true if intersecting.
     */
    private static boolean isSphereIntersectingSphere(SphereBounds sphere, float centerX, float centerY, float centerZ,
                                               float radius)
    {
        // Compute center position
        final float circleRadius = sphere.getRadius();
        final float circleCenterX = sphere.getX() + circleRadius;
        final float circleCenterY = sphere.getY() + circleRadius;
        final float circleCenterZ = sphere.getZ() + circleRadius;

        // Compute slope between both center points
        final float xDiff = centerX - circleCenterX;
        final float yDiff = centerY - circleCenterY;
        final float zDiff = centerZ - circleCenterZ;

        // Compute relative distance (to avoid square root from computing actual distance)
        final float relativeDist = (xDiff * xDiff) + (yDiff * yDiff) + (zDiff * zDiff);

        // Min dist for intersection
        final float combinedRadii = circleRadius + radius;
        return relativeDist <= (combinedRadii * combinedRadii);
    }

    /**
     * <p>Tests if a box has all its corners inside a sphere.</p>
     *
     * @param box box.
     * @param sphere sphere.
     * @return true if all the box's corners are inside the sphere.
     */
    private static boolean doesBoxHaveAllCornersInCircle(Bounds box, SphereBounds sphere)
    {
        final float minX = box.getX();
        final float minY = box.getY();
        final float minZ = box.getZ();

        final float maxX = minX + box.getWidth();
        final float maxY = minY + box.getHeight();
        final float maxZ = minZ + box.getDepth();

        // Check bottom left point -> counter clockwise if in sphere
        return (SphereBounds.isSphereIntersectingSphere(sphere, minX, minY, minZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, minX, minY, maxZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, maxX, minY, maxZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, maxX, minY, minZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, minX, maxY, minZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, minX, maxY, maxZ, 0f)
                && SphereBounds.isSphereIntersectingSphere(sphere, maxX, maxY, maxZ, 0f))
                && SphereBounds.isSphereIntersectingSphere(sphere, maxX, maxY, minZ, 0f);
    }
}
