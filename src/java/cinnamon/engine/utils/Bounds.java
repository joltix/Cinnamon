package cinnamon.engine.utils;

/**
 * <p>Defines a 3D shape with methods for testing containment and intersection against other {@code Bounds} or the
 * spherical variant {@code SphereBounds}. The default implementations for containment and intersection tests operate
 * on the {@code Bounds} as an axis-aligned box. Subclasses representing a different shape should provide their own
 * implementations for the following methods.</p>
 *
 * <ul>
 *     <li>{@code float getVolume()}</li>
 *     <li>{@code float getSurfaceArea()}</li>
 *     <li>{@code boolean contains(Bounds)}</li>
 *     <li>{@code boolean contains(SphereBounds)}</li>
 *     <li>{@code boolean contains(Point)}</li>
 *     <li>{@code boolean intersects(Bounds)}</li>
 *     <li>{@code boolean intersects(SphereBounds)}</li>
 * </ul>
 * <br>
 *
 * <b>Implicit box</b>
 * <p>No matter the shape, a {@code Bounds} maintains an implicit minimal axis-aligned box defined by two points:
 * minimum and maximum. Each point represents a corner of the shape's tight-fitting box, forming a diagonal that cuts
 * across the volume in three dimensions. It should be noted that it is possible for either or both minimum and
 * maximum points to fall outside the shape but not the implicit box (e.g. in the case of a sphere).</p>
 *
 * <b>Specificity</b>
 * <p>Care should be taken when testing a {@code Bounds} for containment or intersection since a {@code Bounds} of
 * another shape, such as {@code SphereBounds}, could be treated as a box if passed as an argument of type
 * {@code Bounds}, as shown below.</p>
 *
 * <pre>
 *     <code>
 *
 *         Bounds bounds = new MutableBounds();
 *         SphereBounds sphere = new MutableSphereBounds();
 *
 *         // Is sphere inside box
 *         boolean contained = bounds.contains(sphere);
 *
 *         // Is box inside box
 *         Bounds sphereAsBounds = sphere;
 *         contained = bounds.contains(sphereAsBounds);
 *     </code>
 * </pre>
 */
public abstract class Bounds implements Position, Size
{
    /**
     * <p>Index when not being used by an exclusive locking {@code SpatialPartition}.</p>
     */
    static final int UNPARTITIONED = -1;

    // Index position for tree node; constant access
    private int mNodeIndex = UNPARTITIONED;

    protected Bounds() { }

    /**
     * <p>Checks if a {@code Bounds} is contained within. This method tests containment using the given
     * {@code Bounds}' implicit box.</p>
     *
     * @param bounds bounds.
     * @return true if contained.
     * @throws NullPointerException if bounds is null.
     */
    public boolean contains(Bounds bounds)
    {
        checkNull(bounds);
        return isBoxContainedInBox(this, bounds);
    }

    /**
     * <p>Checks if a {@code SphereBounds} is contained within.</p>
     *
     * @param sphere sphere.
     * @return true if contained.
     * @throws NullPointerException if sphere is null.
     */
    public boolean contains(SphereBounds sphere)
    {
        checkNull(sphere);

        // Bounds' box
        final float minX = getX();
        final float minY = getY();
        final float minZ = getZ();
        final float maxX = minX + getWidth();
        final float maxY = minY + getHeight();
        final float maxZ = minZ + getDepth();

        // Treat sphere as box
        final float minXB = sphere.getX();
        final float minYB = sphere.getY();
        final float minZB = sphere.getZ();
        final float maxXB = minXB + sphere.getWidth();
        final float maxYB = minYB + sphere.getHeight();
        final float maxZB = minZB + sphere.getDepth();

        return isBoxContainedInBox(minXB, minYB, minZB, maxXB, maxYB, maxZB, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * <p>Checks if a {@code Point} is contained within.</p>
     *
     * @param point point.
     * @return true if contained.
     * @throws NullPointerException if point is null.
     */
    public boolean contains(Point point)
    {
        checkNull(point);
        return isPointInsideBounds(point);
    }

    /**
     * <p>Checks if a {@code Bounds} intersects. This method tests intersection using the given {@code Bounds}'
     * implicit box.</p>
     *
     * @param bounds bounds.
     * @return true if intersects.
     * @throws NullPointerException if bounds is null.
     */
    public boolean intersects(Bounds bounds)
    {
        checkNull(bounds);
        return isBoxIntersectingBox(this, bounds);
    }

    /**
     * <p>Checks if a {@code SphereBounds} intersects.</p>
     *
     * @param sphere sphere.
     * @return true if intersects.
     * @throws NullPointerException if sphere is null.
     */
    public boolean intersects(SphereBounds sphere)
    {
        checkNull(sphere);
        return doesBoxHaveACornerInSphere(this, sphere);
    }

    /**
     * <p>Gets the volume.</p>
     *
     * @return volume.
     */
    public float getVolume()
    {
        return getWidth() * getHeight() * getDepth();
    }

    /**
     * <p>Gets the surface area.</p>
     *
     * @return surface area.
     */
    public float getSurfaceArea()
    {
        final float w = getWidth();
        final float h = getHeight();
        final float d = getDepth();

        // Opposite face is same so no need for bottom, right, or back
        final float areaTop = w * d;
        final float areaLeft = h * d;
        final float areaFront = w * h;

        return 2f * areaTop + 2f * areaLeft + 2f * areaFront;
    }

    /**
     * <p>Gets the center. Changes to the returned point do not affect the actual center.</p>
     *
     * @return center.
     */
    public Point getCenter()
    {
        final float centerX = getX() + getWidth() / 2f;
        final float centerY = getY() + getHeight() / 2f;
        final float centerZ = getZ() + getDepth() / 2f;

        return new Point(centerX, centerY, centerZ);
    }

    /**
     * <p>Gets the smallest x of all points in the implicit box.</p>
     *
     * @return smallest x.
     */
    @Override
    public abstract float getX();

    /**
     * <p>Gets the smallest y of all points in the implicit box.</p>
     *
     * @return smallest y.
     */
    @Override
    public abstract float getY();

    /**
     * <p>Gets the smallest z of all points in the implicit box.</p>
     *
     * @return smallest z.
     */
    @Override
    public abstract float getZ();

    /**
     * <p>Gets the largest x of all points in the implicit box.</p>
     *
     * @return largest x.
     */
    public float getMaximumX()
    {
        return getX() + getWidth();
    }

    /**
     * <p>Gets the largest y of all points in the implicit box.</p>
     *
     * @return largest y.
     */
    public float getMaximumY()
    {
        return getY() + getHeight();
    }

    /**
     * <p>Gets the largest z of all points in the implicit box.</p>
     *
     * @return largest z.
     */
    public float getMaximumZ()
    {
        return getZ() + getDepth();
    }

    /**
     * <p>Checks if a partitioning structure has locked use of the {@code Bounds} from other locking partitioning
     * implementations.</p>
     *
     * @return true if in use by a partition that prevents use by other locking partitions.
     */
    public final boolean isExclusivelyPartitioned()
    {
        return mNodeIndex != UNPARTITIONED;
    }

    /**
     * <p>Gets the index into the locking partition.</p>
     *
     * @return partition index, or {@link #UNPARTITIONED} if not locked.
     */
    int getIndex()
    {
        return mNodeIndex;
    }

    /**
     * <p>Sets the index into the locking partition.</p>
     *
     * @param index partition index.
     */
    void setIndex(int index)
    {
        mNodeIndex = index;
    }

    /**
     * <p>Tests if a {@code Point} is within the {@code Bounds}' implicit box.</p>
     *
     * @param point to test.
     * @return true if contained in box.
     */
    private boolean isPointInsideBounds(Point point)
    {
        final float x = point.getX();
        final float y = point.getY();
        final float z = point.getZ();

        final float minX = getX();
        final float minY = getY();
        final float minZ = getZ();

        return inRange(x, minX, minX + getWidth())
                && inRange(y, minY, minY + getHeight())
                && inRange(z, minZ, minZ + getDepth());
    }

    /**
     * <p>Throws a {@code NullPointerException} if the given object is null.</p>
     *
     * @param object to check.
     * @throws NullPointerException if object is null.
     */
    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Tests if at least one corner of the specified {@code Bounds}' implicit box is inside a given
     * {@code SphereBounds}.</p>
     *
     * @param box box.
     * @param sphere sphere.
     * @return true if one of the box's corners is inside the sphere.
     */
    static boolean doesBoxHaveACornerInSphere(Bounds box, SphereBounds sphere)
    {
        // Treat sphere as a box
        final float sphereMinX = sphere.getX();
        final float sphereMinY = sphere.getY();
        final float sphereMinZ = sphere.getZ();
        final float diameter = sphere.getWidth();
        final float sphereMaxX = sphereMinX + diameter;
        final float sphereMaxY = sphereMinY + diameter;
        final float sphereMaxZ = sphereMinZ + diameter;

        final float minX = box.getX();
        final float minY = box.getY();
        final float minZ = box.getZ();
        final float maxX = minX + box.getWidth();
        final float maxY = minY + box.getHeight();
        final float maxZ = minZ + box.getDepth();

        // If boxes don't intersect, then sphere won't
        final boolean boxIntersects = isBoxIntersectingBox(sphereMinX, sphereMinY, sphereMinZ, sphereMaxX,
                sphereMaxY, sphereMaxZ, minX, minY, minZ, maxX, maxY, maxZ);

        return boxIntersects && isSphereIntersectingAnEdge(sphere, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * <p>Checks if the box formed by one set of min/max points are contained within the box formed by
     * another set of min/max points.</p>
     *
     * @param minXA minimum x of inner box.
     * @param minYA minimum y of inner box.
     * @param minZA minimum z of inner box.
     * @param maxXA maximum x of inner box.
     * @param maxYA maximum y of inner box.
     * @param maxZA maximum z of inner box.
     * @param minXB min x of outer box.
     * @param minYB min y of outer box.
     * @param minZB min z of outer box.
     * @param maxXB max x of outer box.
     * @param maxYB max y of outer box.
     * @param maxZB max z of outer box.
     * @return true if first box is contained within the second.
     */
    static boolean isBoxContainedInBox(float minXA, float minYA, float minZA, float maxXA, float maxYA, float maxZA,
                                       float minXB, float minYB, float minZB, float maxXB, float maxYB, float maxZB)
    {
        return inRange(minXA, minXB, maxXB) && inRange(maxXA, minXB, maxXB)
                && inRange(minYA, minYB, maxYB) && inRange(maxYA, minYB, maxYB)
                && inRange(minZA, minZB, maxZB) && inRange(maxZA, minZB, maxZB);
    }

    /**
     * <p>Tests if the boxes defined by two {@code Bounds} intersects.</p>
     *
     * @param boxA first.
     * @param boxB second.
     * @return true if intersecting.
     */
    private static boolean isBoxIntersectingBox(Bounds boxA, Bounds boxB)
    {
        // Get first box
        final float minX = boxA.getX();
        final float minY = boxA.getY();
        final float minZ = boxA.getZ();
        final float maxX = minX + boxA.getWidth();
        final float maxY = minY + boxA.getHeight();
        final float maxZ = minZ + boxA.getDepth();

        // Get second box
        final float otherMinX = boxB.getX();
        final float otherMinY = boxB.getY();
        final float otherMinZ = boxB.getZ();
        final float otherMaxX = otherMinX + boxB.getWidth();
        final float otherMaxY = otherMinY + boxB.getHeight();
        final float otherMaxZ = otherMinZ + boxB.getDepth();

        return isBoxIntersectingBox(minX, minY, minZ, maxX, maxY, maxZ,
                otherMinX, otherMinY, otherMinZ, otherMaxX, otherMaxY, otherMaxZ);
    }

    /**
     * <p>Tests if the given {@code SphereBounds} intersects an edge of the  box formed by the given minimum and
     * maximum points.</p>
     *
     * @param sphere sphere.
     * @param minX minimum x.
     * @param minY minimum y.
     * @param minZ minimum z.
     * @param maxX maximum x.
     * @param maxY maximum y.
     * @param maxZ maximum z.
     * @return true if the sphere intersects at least one edge of the box.
     */
    private static boolean isSphereIntersectingAnEdge(SphereBounds sphere, float minX, float minY, float minZ,
                                                      float maxX, float maxY, float maxZ)
    {
        return isSphereIntersectingLine(sphere, minX, minY, minZ, minX, minY, maxZ)
                || isSphereIntersectingLine(sphere, minX, minY, maxZ, maxX, minY, maxZ)
                || isSphereIntersectingLine(sphere, maxX, minY, maxZ, maxX, minY, minZ)
                || isSphereIntersectingLine(sphere, maxX, minY, minZ, minX, minY, minZ)
                || isSphereIntersectingLine(sphere, minX, minY, minZ, minX, maxY, minZ)
                || isSphereIntersectingLine(sphere, minX, maxY, minZ, minX, maxY, maxZ)
                || isSphereIntersectingLine(sphere, minX, maxY, maxZ, maxX, maxY, maxZ)
                || isSphereIntersectingLine(sphere, maxX, maxY, maxZ, maxX, maxY, minZ)
                || isSphereIntersectingLine(sphere, maxX, maxY, minZ, minX, maxY, minZ)
                || isSphereIntersectingLine(sphere, maxX, maxY, minZ, maxX, minY, minZ)
                || isSphereIntersectingLine(sphere, minX, maxY, maxZ, minX, minY, maxZ)
                || isSphereIntersectingLine(sphere, maxX, maxY, maxZ, maxX, minY, maxZ);
    }

    /**
     * <p>Tests if a given sphere intersects the line segment formed by two specified points.</p>
     *
     * @param sphere sphere.
     * @param x0 first x.
     * @param y0 first y.
     * @param z0 first z.
     * @param x1 second x.
     * @param y1 second y.
     * @param z1 second z.
     * @return true if sphere intersects the line.
     */
    private static boolean isSphereIntersectingLine(SphereBounds sphere, float x0, float y0, float z0,
                                                    float x1, float y1, float z1)
    {
        // Compute vector between second and first points
        final float lineX = x1 - x0;
        final float lineY = y1 - y0;
        final float lineZ = z1 - z0;

        // Distance squared to avoid square root
        final float edgeLen = (lineX * lineX) + (lineY * lineY) + (lineZ * lineZ);

        final Point center = sphere.getCenter();
        final float centerX = center.getX();
        final float centerY = center.getY();
        final float centerZ = center.getZ();
        float x3;
        float y3;
        float z3;
        if (edgeLen == 0f) {
            // Edge is effectively a single point
            x3 = x0;
            y3 = y0;
            z3 = z0;

        } else {
            // Compute vector between sphere's center and edge's first point
            final float x2 = center.getX() - x0;
            final float y2 = center.getY() - y0;
            final float z2 = center.getZ() - z0;

            // Project sphere's center onto line segment for point of intersection with sphere
            final float dot = ((lineX * x2) + (lineY * y2) + (lineZ * z2)) / edgeLen;
            x3 = x0 + dot * lineX;
            y3 = y0 + dot * lineY;
            z3 = y0 + dot * lineZ;
        }

        // Test distance from sphere center to projection on edge
        final float projLineX = centerX - x3;
        final float projLineY = centerY - y3;
        final float projLineZ = centerZ - z3;
        final float finalDist = (projLineX * projLineX) + (projLineY * projLineY) + (projLineZ * projLineZ);
        return finalDist <= sphere.getRadius() * sphere.getRadius();
    }

    /**
     * <p>Tests if a box defined by a {@code Bounds} is contained within another.</p>
     *
     * @param outer containing box.
     * @param inner box to test.
     * @return true if inner is contained within outer.
     */
    private static boolean isBoxContainedInBox(Bounds outer, Bounds inner)
    {
        // Compute inner box
        final float targetMinX = inner.getX();
        final float targetMinY = inner.getY();
        final float targetMinZ = inner.getZ();
        final float targetMaxX = targetMinX + inner.getWidth();
        final float targetMaxY = targetMinY + inner.getHeight();
        final float targetMaxZ = targetMinZ + inner.getDepth();

        // Compute outer box
        final float minX = outer.getX();
        final float minY = outer.getY();
        final float minZ = outer.getZ();
        final float maxX = minX + outer.getWidth();
        final float maxY = minY + outer.getHeight();
        final float maxZ = minZ + outer.getDepth();

        // Check target box's min/max points if in this box's min/max
        return isBoxContainedInBox(targetMinX, targetMinY, targetMinZ, targetMaxX, targetMaxY, targetMaxZ,
                minX, minY, minZ, maxX, maxY, maxZ);

    }

    /**
     * <p>Tests if a given value is {@literal >=} {@code min} and {@literal <=} {@code max}.</p>
     *
     * @param value value to test.
     * @param min inclusive minimum.
     * @param max inclusive maximum.
     * @return true if between min and max.
     */
    private static boolean inRange(float value, float min, float max)
    {
        return value >= min && value <= max;
    }

    /**
     * <p>Tests if the box defined by two specified points intersects the box of another pair of points.</p>
     *
     * @param minX first box's min x.
     * @param minY first box's min y.
     * @param minZ first box's min z.
     * @param maxX first box's max x.
     * @param maxY first box's max y.
     * @param maxZ first box's max z.
     * @param otherMinX second box's min x.
     * @param otherMinY second box's min y.
     * @param otherMinZ second box's min z.
     * @param otherMaxX second box's max x.
     * @param otherMaxY second box's max y.
     * @param otherMaxZ second box's max z.
     * @return true if boxes intersect.
     */
    private static boolean isBoxIntersectingBox(float minX, float minY, float minZ,
                                                float maxX, float maxY, float maxZ,
                                                float otherMinX, float otherMinY, float otherMinZ,
                                                float otherMaxX, float otherMaxY, float otherMaxZ)
    {
        // Test each min corner's beyond the other's max; one true means no intersection
        return !(minX > otherMaxX || otherMinX > maxX
                || minY > otherMaxY || otherMinY > maxY
                || minZ > otherMaxZ || otherMinZ > maxZ);
    }
}
