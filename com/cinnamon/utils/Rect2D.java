package com.cinnamon.utils;

import com.cinnamon.object.Dimensional;
import com.cinnamon.object.Positional;

/**
 * <p>
 *     Rect2D describes a rectangular plane in 3D space. The rectangle is defined by two points forming the shape's
 *     diagonal; both can be retrieved with {@link #getPosition()} and {@link #getCorner()}. Both corners will always
 *     have the same z value.
 * </p>
 */
public interface Rect2D extends Positional
{
    /**
     * <p>Gets the corner as a {@link Point3F}. Changes to the Point3F do not affect the Rect2D's corner.</p>
     *
     * @return corner.
     */
    Point3F getCorner();

    /**
     * <p>Checks whether or not an (x,y) point is contained within the Rect2D.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if contained.
     */
    boolean contains(float x, float y);

    /**
     * <p>Checks whether or not an (x,y,z) point is contained within the Rect2D.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if contained.
     */
    boolean contains(float x, float y, float z);

    /**
     * <p>Checks whether or not another Rect2D is fully contained within the Rect2D's boundaries.</p>
     *
     * @param rect other Rect2D.
     * @return true if Rect2D is fully contained.
     */
    boolean contains(Rect2D rect);

    /**
     * <p>Checks whether or not the rectangle formed by two (x,y) points is fully contained within the Rect2D's
     * boundaries.</p>
     *
     * @param x x.
     * @param y y.
     * @param cornerX corner x.
     * @param cornerY corner y.
     * @return true if rectangle is fully contained.
     */
    boolean contains(float x, float y, float cornerX, float cornerY);

    /**
     * <p>Checks whether or not another Rect2D intersects the Rect2D.</p>
     *
     * @param rect other Rect2D.
     * @return true if the two Rect2Ds intersect.
     */
    boolean intersects(Rect2D rect);

    /**
     * <p>Checks whether or not the rectangle formed by two (x,y) points is fully contained within the Rect2D's
     * boundaries.</p>
     *
     * @param x x.
     * @param y y.
     * @param cornerX corner x.
     * @param cornerY corner y.
     * @return true if the two Rect2Ds intersect.
     */
    boolean intersects(float x, float y, float cornerX, float cornerY);

    /**
     * <p>Gets the corner x coordinate.</p>
     *
     * @return x.
     */
    float getCornerX();

    /**
     * <p>Gets the corner y coordinate.</p>
     *
     * @return y.
     */
    float getCornerY();

    /**
     * <p>Gets the corner z coordinate.</p>
     *
     * @return z.
     */
    float getCornerZ();
}
