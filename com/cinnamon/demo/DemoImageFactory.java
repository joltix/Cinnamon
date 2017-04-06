package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.system.Config;
import com.cinnamon.utils.Comparison;
import com.cinnamon.utils.Merge;

/**
 * <p>
 *     Demo {@link ImageFactory}.
 * </p>
 */
public class DemoImageFactory extends ImageFactory
{
    private static final int LOAD = 100;
    private static final float GROWTH = 0.15f;

    public DemoImageFactory(ShaderFactory factory)
    {
        super(factory, new Merge<ImageComponent>(new PainterOrder()), LOAD,
                GROWTH);
    }

    @Override
    protected Config<ImageComponent, ShaderFactory> createDefaultConfig()
    {
        return new DefaultConfig();
    }

    @Override
    protected void onLoad(ShaderFactory factory)
    {
        addConfig("character", new CharacterConfig());
        addConfig("rock", new RockConfig());
    }

    private class CharacterConfig implements Config<ImageComponent, ShaderFactory>
    {
        @Override
        public void configure(ImageComponent object, ShaderFactory resource)
        {
            final Texture tex = resource.getTexture("demo_character.png");
            object.setTexture(tex.getId());
            object.setWidth(0.5f);
            object.setHeight(1.8f);
        }
    }

    private class RockConfig implements Config<ImageComponent, ShaderFactory>
    {
        @Override
        public void configure(ImageComponent object, ShaderFactory resource)
        {
            final Texture tex = resource.getTexture("demo_rock.png");
            object.setTexture(tex.getId());
            object.setWidth(14f);
            object.setHeight(21f);
        }
    }

    private class DefaultConfig implements Config<ImageComponent, ShaderFactory>
    {
        @Override
        public void configure(ImageComponent image, ShaderFactory resource)
        {
            image.moveTo(0f, 0f);
            image.setWidth(1f);
            image.setHeight(1f);
            image.setVisible(true);
            image.rotateTo(0f);
            image.setFlipHorizontally(false);
            image.setFlipVertically(false);
            image.setTexture(Texture.NULL);
            image.setTint(1f, 1f, 1f);
            image.setTransparency(1f);
        }
    }

    /**
     * <p>Comparison to be used when sorting an array of GObjects for drawing. The comparison is made with "Painter's
     * Algorithm" in mind, except with objects further from the screen being represented with smaller z values.
     * Further, GObjects are grouped by texture ids and GObjects without RenderComponents are associated with greater
     * values.</p>
     */
    private static class PainterOrder implements Comparison<ImageComponent>
    {
        @Override
        public int compare(ImageComponent obj0, ImageComponent obj1)
        {
            // Determine whether or not the L and R objs can be drawn
            final boolean drawable0 = obj0.isVisible() && obj0.getTransparency() > 0f;
            final boolean drawable1 = obj1.isVisible() && obj1.getTransparency() > 0f;

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
