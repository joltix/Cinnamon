package com.cinnamon.system;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.Texture;
import com.cinnamon.object.BodyComponent;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;

/**
 * <p>
 *     Config is a small set-and-forget object meant to define configurations
 *     of objects such as {@link GObject}s and {@link ImageComponent}s and
 *     apply such configurations when needed automatically. In the case of
 *     the GObject, a Config definition may choose the appropriate
 *     {@link Texture} and {@link BodyComponent} to create a standard game
 *     actor that can easily be reproduced by the {@link GObjectFactory}
 *     simply by submitting the Config's name.
 * </p>
 *
 *
 */
public interface Config<E, U>
{
    /**
     * <p>Applies instructions to configure a {@link E} given a {@link U}
     * resource.</p>
     *
     * @param object object.
     * @param resource resource.
     */
    void configure(E object, U resource);
}
