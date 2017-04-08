package com.cinnamon.gfx;


import com.cinnamon.object.Positional;
import com.cinnamon.utils.Rotatable;

/**
 * <p>
 *     Drawables carry information needed for something to be drawn such as a {@link Texture}'s id, the size to draw
 *     with, and more.
 * </p>
 */
public interface Drawable extends Positional, Rotatable
{
    /**
     * <p>Gets the {@link Texture} id of the image to draw.</p>
     *
     * @return Texture id.
     */
    int getTexture();

    /**
     * <p>Checks if the texture should be flipped horizontally.</p>
     *
     * @return true to flip.
     */
    boolean isFlippedHorizontally();

    /**
     * <p>Checks if the texture should be flipped vertically.</p>
     *
     * @return true to flip.
     */
    boolean isFlippedVertically();

    /**
     * <p>Gets the red color value.</p>
     *
     * @return red.
     */
    float getRed();

    /**
     * <p>Gets the green color value.</p>
     *
     * @return green.
     */
    float getGreen();

    /**
     * <p>Gets the blue color value.</p>
     *
     * @return blue.
     */
    float getBlue();

    /**
     * <p>Gets the transparency, or alpha, amount.</p>
     *
     * @return transparency.
     */
    float getTransparency();
}
