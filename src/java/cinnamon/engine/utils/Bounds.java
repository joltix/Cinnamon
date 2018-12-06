package cinnamon.engine.utils;

import static java.util.Objects.requireNonNull;

/**
 * Bounding volume for simple collision testing.
 *
 * <p>Implementations must decide on the specific shape of their bounding volumes. Whatever shape is chosen, its
 * position(s) <i>must never be {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or
 * {@code Float.NEGATIVE_INFINITY}.</i></p>
 *
 * <p>While provided bounding volumes (e.g. {@code BoxBounds}) are explicitly supported with
 * {@code contains(BoxBounds)} and {@code intersects(BoxBounds)}, it is up to user implementations to detect and
 * handle other {@code Bounds} through the more general {@link #contains(Bounds)} and {@link #intersects(Bounds)}.</p>
 *
 * <p>For specifics on how external implementations are handled by those provided, see the provided's documentation.</p>
 */
public abstract class Bounds
{
    // Bounds is not an interface because it is expected to hold
    // state in a future update.

    /**
     * Returns {@code true} if this {@code Bounds} contains another.
     * 
     * <p>This is a fall-through method for user implementations not supported by the provided {@code Bounds} 
     * implementations. By default, this method returns {@code false}.</p>
     *
     * @param bounds bounds.
     * @return {@code true} if {@code bounds} is inside.
     * @throws NullPointerException if {@code bounds} is {@code null}.
     */
    public boolean contains(Bounds bounds)
    {
        requireNonNull(bounds);

        return false;
    }

    /**
     * Returns {@code true} if this {@code Bounds} contains the given {@code BoxBounds}.
     * 
     * @param bounds bounds.
     * @return {@code true} if {@code bounds} is inside.
     * @throws NullPointerException if {@code bounds} is {@code null}.
     */
    public abstract boolean contains(BoxBounds bounds);

    /**
     * Returns {@code true} if this {@code Bounds} contains the given {@code Point}.
     * 
     * @param point point.
     * @return {@code true} if {@code point} is inside.
     * @throws NullPointerException if {@code point} is {@code null}.
     * @throws IllegalArgumentException if the {@code point} consists of a {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public abstract boolean contains(Point point);

    /**
     * Returns {@code true} if this {@code Bounds} intersects another.
     * 
     * <p>This is a fall-through method for user implementations not supported by the provided {@code Bounds} 
     * implementations. By default, this method returns {@code false}.</p>
     *
     * @param bounds bounds.
     * @return {@code true} if intersects.
     * @throws NullPointerException if {@code bounds} is {@code null}.
     */
    public boolean intersects(Bounds bounds)
    {
        requireNonNull(bounds);

        return false;
    }

    /**
     * Returns {@code true} if this {@code Bounds} intersects the given {@code BoxBounds}.
     *
     * @param bounds bounds.
     * @return {@code true} if intersects.
     * @throws NullPointerException if {@code bounds} is {@code null}.
     */
    public abstract boolean intersects(BoxBounds bounds);

    /**
     * Gets the center point.
     * 
     * <p>Changes to the {@code Point} have no effect on this {@code Bounds}.</p>
     * 
     * @return center.
     */
    public abstract Point getCenter();

    /**
     * Gets the minimum x.
     * 
     * @return minimum x.
     */
    public abstract float getMinimumX();

    /**
     * Gets the maximum x.
     * 
     * @return maximum x.
     */
    public abstract float getMaximumX();

    /**
     * Gets the minimum y.
     * 
     * @return minimum y.
     */
    public abstract float getMinimumY();

    /**
     * Gets the maximum y.
     * 
     * @return maximum y.
     */
    public abstract float getMaximumY();

    /**
     * Gets the minimum z.
     * 
     * @return minimum z.
     */
    public abstract float getMinimumZ();

    /**
     * Gets the maximum z.
     * 
     * @return maximum z.
     */
    public abstract float getMaximumZ();

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
}