package com.cinnamon.utils;

import com.cinnamon.object.Dimensional;
import com.cinnamon.object.Positional;

/**
 * <p>
 *     Rect2D designates a 2D rectangle formed by two {@link Point2F}s
 *     defining opposite corners.
 * </p>
 *
 *
 */
public interface Rect2D extends Positional, Dimensional
{
    /**
     * <p>Gets the {@link Point2F} representing the position.</p>
     *
     * @return position.
     */
    Point2F getOrigin();

    /**
     * <p>Gets the {@link Point2F} representing the corner opposite that of
     * the origin position.</p>
     *
     * @return corner.
     */
    Point2F getCorner();

    /**
     * <p>Checks whether or not an (x,y) point is contained within the
     * Rect2D.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if contained.
     */
    boolean contains(float x, float y);

    /**
     * <p>Checks whether or not another Rect2D contains.</p>
     *
     * @param rect other Rect2D.
     * @return true if the Rect2D contains.
     */
    boolean intersects(Rect2D rect);

    /**
     * <p>Checks whether or not an axis aligned rectangle formed by the
     * origin point (originX,originY) and corner point (cornerX,cornerY)
     * contains the calling Rect2D.</p>
     *
     * @param originX origin x.
     * @param originY origin y.
     * @param cornerX corner x.
     * @param cornerY corner y.
     * @return true if the two Rect2Ds intersect.
     */
    boolean intersects(float originX, float originY, float cornerX,
                       float cornerY);

    /**
     * <p>This method will always return 0.</p>
     *
     * @return 0.
     */
    @Override
    float getZ();

    /**
     * <p>Moves the position to a specific (x,y) point. This method will
     * always treat z as 0 and ignore any given z value.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    @Override
    void moveTo(float x, float y, float z);

    /**
     * <p>Sets offset values for the x and y coordinates. This method will
     * always set z to 0 and ignore any given z value.</p>
     *
     * <p>These values are added to target destinations for methods such as
     * {@link #moveTo(float, float, float)}.</p>
     *
     * @param x x offset.
     * @param y y offset.
     */
    @Override
    void setOffset(float x, float y, float z);

    /**
     * <p>This method will always return 0.</p>
     *
     * @return 0.
     */
    @Override
    float getOffsetZ();
}
