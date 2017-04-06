package com.cinnamon.object;

import com.cinnamon.utils.Point3F;

/**
 * <p>
 *     Positional describes a 2D object in 3D space. This also adds dimension setting methods to the
 *     {@link Dimensional} interface, which allow only reading of an object's width and height.
 * </p>
 */
public interface Positional extends Dimensional
{
    /**
     * <p>Gets the position. Changes to the {@link Point3F} will not affect the Positional's position.</p>
     *
     * @return position.
     */
    Point3F getPosition();

    /**
     * <p>Gets the x coordinate.</p>
     *
     * @return x.
     */
    float getX();

    /**
     * <p>Gets the y coordinate.</p>
     *
     * @return y.
     */
    float getY();

    /**
     * <p>Gets the z coordinate.</p>
     *
     * @return z.
     */
    float getZ();

    /**
     * <p>Moves the position to a specific (x,y) point while leaving the z dimension as-is.</p>
     *
     * @param x x.
     * @param y y;
     */
    void moveTo(float x, float y);

    /**
     * <p>Moves the position to a specific (x,y,z) point.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    void moveTo(float x, float y, float z);

    /**
     * <p>Moves the position by an x and y amount. The values are added to the respective coordinates.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     */
    void moveBy(float x, float y);

    /**
     * <p>Moves the position by an x, y, and z amount. The values are added to the respective coordinates.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     * @param z amount along z.
     */
    void moveBy(float x, float y, float z);

    /**
     * <p>Moves the position to center on a specific (x,y) point.</p>
     *
     * @param x x.
     * @param y y.
     */
    void moveToCenter(float x, float y);

    /**
     * <p>Sets the width.</p>
     *
     * @param width width.
     */
    void setWidth(float width);

    /**
     * <p>Sets the height.</p>
     *
     * @param height height.
     */
    void setHeight(float height);
}
