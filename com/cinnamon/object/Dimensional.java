package com.cinnamon.object;

/**
 * Describes an object which has an inherent width and height. Whether the width or height can be publicly set is up
 * to implementing classes.
 */
public interface Dimensional
{
    /**
     * <p>Gets the width.</p>
     *
     * @return width.
     */
    float getWidth();

    /**
     * <p>Gets the height.</p>
     *
     * @return height.
     */
    float getHeight();

    /**
     * <p>Gets the center's x coordinate.</p>
     *
     * @return center x.
     */
    float getCenterX();

    /**
     * <p>Gets the center's y coordinate.</p>
     *
     * @return center y.
     */
    float getCenterY();
}
