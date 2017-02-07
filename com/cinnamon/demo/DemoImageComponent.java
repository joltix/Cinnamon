package com.cinnamon.demo;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;

/**
 * <p>
 *     Demo subclass of {@link ImageComponent}.
 * </p>
 *
 *
 */
public class DemoImageComponent extends ImageComponent
{
    /**
     * <p>Constructor for an {@link ImageComponent}.</p>
     *
     * @param width width.
     * @param height height.
     * @param texture {@link Texture} id of image.
     */
    public DemoImageComponent(float width, float height, int texture)
    {
        setWidth(width);
        setHeight(height);
        setTexture(texture);
    }
}
