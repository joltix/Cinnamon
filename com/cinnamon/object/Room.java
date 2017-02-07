package com.cinnamon.object;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;

/**
 * <p>Defines a boundary area with a background image where {@link GObject}s
 * move around.</p>
 *
 *
 */
public interface Room
{
    /**
     * <p>Gets the {@link ImageComponent} to represent the Room's background
     * .</p>
     *
     * @return ImageComponent background.
     */
    ImageComponent getBackground();

    /**
     * <p>Sets the {@link Texture} id to use as a background image.</p>
     *
     * @param texture texture id.
     */
    void setBackgroundImage(int texture);

    /**
     * <p>Gets the width.</p>
     *
     * @return Room width.
     */
    float getWidth();

    /**
     * <p>Gets the height.</p>
     *
     * @return Room height.
     */
    float getHeight();

    /**
     * <p>Checks whether or not the {@link GObject}'s {@link BodyComponent}
     * is within the Room.</p>
     *
     * @param object GObject.
     * @return true if the GObject is inside the Room.
     */
    boolean contains(GObject object);
}
