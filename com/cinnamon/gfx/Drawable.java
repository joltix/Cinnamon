package com.cinnamon.gfx;


/**
 * <p>
 *     A lightweight version of {@link ImageComponent}, Drawables are meant
 *     to relay drawing information as copies of fuller ImageComponents.
 * </p>
 *
 *
 */
public interface Drawable
{
    /**
     * <p>Gets the {@link Texture} id of the image to draw.</p>
     *
     * @return Texture id.
     */
    int getTexture();

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
}
