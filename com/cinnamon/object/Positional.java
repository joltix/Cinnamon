package com.cinnamon.object;

import com.cinnamon.utils.Point2F;

/**
 * <p>
 *     Positional describes an object in 3D space through access and
 *     manipulation of an (x,y,z) point.
 * </p>
 *
 * <p>
 *     A secondary point is maintained as offset values to be added to the
 *     Positional's true position through
 *     {@link #setOffset(float, float, float)}. Position getter methods that
 *     return a value and not a {@link Point2F} are treated with the offset
 *     automatically applied. Those returning Point2Fs return the true positions
 *     and have not had the offsets applied.
 * </p>
 *
 *
 */
public interface Positional
{
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
     * <p>Moves the position to a specific (x,y,z) point.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    void moveTo(float x, float y, float z);

    /**
     * <p>Moves the position to a specific (x,y) point while leaving the z
     * dimension as-is.</p>
     *
     * @param x x.
     * @param y y;
     */
    void moveTo(float x, float y);

    /**
     * <p>Sets offset values for the x, y, and z coordinates.</p>
     *
     * <p>These values are added to target destinations for methods such as
     * {@link #moveTo(float, float, float)}.</p>
     *
     * @param x x offset.
     * @param y y offset.
     * @param z z offset.
     */
    void setOffset(float x, float y, float z);

    /**
     * <p>Sets offset values for the x and y coordinates.</p>
     *
     * <p>These values are added to target destinations for methods such as
     * {@link #moveTo(float, float)}.</p>
     *
     * @param x x offset.
     * @param y y offset.
     */
    void setOffset(float x, float y);

    /**
     * <p>Gets the x offset.</p>
     *
     * @return x offset.
     */
    float getOffsetX();

    /**
     * <p>Gets the y offset.</p>
     *
     * @return y offset.
     */
    float getOffsetY();

    /**
     * <p>Gets the z offset.</p>
     *
     * @return z offset.
     */
    float getOffsetZ();
}
