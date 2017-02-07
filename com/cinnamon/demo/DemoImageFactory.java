package com.cinnamon.demo;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.gfx.ShaderFactory;
import com.cinnamon.gfx.Texture;
import com.cinnamon.utils.Comparison;
import com.cinnamon.utils.Merge;

/**
 * <p>
 *     Demo {@link ImageFactory}.
 * </p>
 *
 *
 */
public class DemoImageFactory extends ImageFactory
{
    private static final int LOAD = 100;
    private static final float GROWTH = 0.15f;

    private static final Float DEFAULT_WIDTH = 100f;
    private static final Float DEFAULT_HEIGHT = 100f;
    private static final int DEFAULT_TEXTURE = 0;

    public DemoImageFactory(ShaderFactory factory)
    {
        super(factory, new Merge<ImageComponent>(new PainterOrder()), LOAD,
                GROWTH);
    }

    @Override
    protected void onLoad(ShaderFactory factory)
    {
        addConfig("character", new CharacterConfig());
        addConfig(ImageFactory.CONFIG_ROOM, new RoomConfig());
    }

    @Override
    protected ImageComponent createComponent()
    {
        return new DemoImageComponent(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_TEXTURE);
    }

    @Override
    protected void makeDefault(ImageComponent component)
    {

    }

    private class CharacterConfig implements ImageConfig
    {
        @Override
        public void configure(ImageComponent object, ShaderFactory resource)
        {
            final Texture tex = resource.getTexture("demo_character.png");
            object.setTexture(tex.getId());
            object.setWidth(tex.getWidth());
            object.setHeight(tex.getHeight());
        }
    }

    private class RoomConfig implements ImageConfig
    {
        @Override
        public void configure(ImageComponent object, ShaderFactory resource)
        {
            object.moveTo(0, 0, -Float.MAX_VALUE);
        }
    }

    /**
     * <p>Comparison to be used when sorting an array of GObjects for
     * drawing. The comparison is made with "Painter's Algorithm" in mind,
     * except with objects further from the screen being represented with
     * smaller z values. Further, GObjects are grouped by texture ids and
     * GObjects without RenderComponents are associated with greater values.</p>
     */
    private static class PainterOrder implements Comparison<ImageComponent>
    {
        @Override
        public int compare(ImageComponent obj0, ImageComponent obj1)
        {
            // Determine whether or not the L and R objs can be drawn
            final boolean drawable0 = obj0 != null && obj0.isVisible()
                    && obj0.getTransparency() > 0f;
            final boolean drawable1 = obj1 != null && obj1.isVisible()
                    && obj1.getTransparency() > 0f;

            // Compare 'z' values and texture ids if both can be drawn
            if (drawable0 && drawable1) {
                final float z0 = obj0.getZ();
                final float z1 = obj1.getZ();
                if (z0 < z1) {
                    return -1;
                } else if (z0 > z1) {
                    return 1;
                } else {

                    // Retrieve obj texture ids for comparison
                    final int tex0 = obj0.getTexture();
                    final int tex1 = obj1.getTexture();

                    // Order textures from least to greatest
                    if (tex0 < tex1) {
                        return -1;
                    } else if (tex0 > tex1) {
                        return 1;
                    } else {
                        return 0;
                    }
                }

            } else if (!drawable0 && drawable1) {
                // Left can't be drawn but right can; '>'
                return 1;
            } else if (drawable0 && !drawable1) {
                // Right can't be drawn but left can; '<'
                return -1;
            } else {
                // Incapable of drawing either obj; "equivalent"
                return 0;
            }
        }
    }
}
