package cinnamon.engine.utils;

/**
 * <p>Declares the minimum mutators for changing a {@code Bounds}' size and position.</p>
 */
public interface DefinableBounds
{
    /**
     * <p>Resizes to include the given point.</p>
     *
     * <p>Unlike {@link #encompass(Point, Point)}, this overload does not make any previously contained point
     * uncontained, i.e. the shape <i>stretches</i> to include the point.</p>
     *
     * @param point point.
     * @throws NullPointerException if point is null.
     */
    default void encompass(Point point)
    {
        if (point == null) {
            throw new NullPointerException();
        }
        encompass(point.getX(), point.getY(), point.getZ());
    }

    /**
     * <p>Resizes to include the given point.</p>
     *
     * <p>Unlike {@link #encompass(Point, Point)}, this overload does not make any previously contained point
     * uncontained, i.e. the shape <i>stretches</i> to include the point.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    void encompass(float x, float y, float z);

    /**
     * <p>Moves and resizes to include all points within the rectangle defined by the two given points.</p>
     *
     * @param min minimum point.
     * @param max maximum point.
     * @throws NullPointerException if either min or max is null.
     */
    default void encompass(Point min, Point max)
    {
        if (min == null) {
            throw new NullPointerException();
        }
        if (max == null) {
            throw new NullPointerException();
        }

        encompass(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    /**
     * <p>Moves and resizes to include all points within the rectangle defined by the two given points.</p>
     *
     * @param minX minimum x.
     * @param minY minimum y.
     * @param minZ minimum z.
     * @param maxX maximum x.
     * @param maxY maximum y.
     * @param maxZ maximum z.
     */
    void encompass(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);

    /**
     * <p>Centers on the given point.</p>
     *
     * @param point point.
     * @throws NullPointerException if point is null.
     */
    default void centerOn(Point point)
    {
        if (point == null) {
            throw new NullPointerException();
        }
        centerOn(point.getX(), point.getY(), point.getZ());
    }

    /**
     * <p>Centers on the given point.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    void centerOn(float x, float y, float z);
}
