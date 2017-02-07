package com.cinnamon.demo;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.system.Game;
import com.cinnamon.utils.Shape;

/**
 * <p>Demo {@link GObjectFactory}.</p>
 *
 *
 */
public class DemoGObjectFactory extends GObjectFactory
{
    private static final int LOAD = 100;
    private static final float GROWTH = 0.15f;

    public DemoGObjectFactory(Game.Resources directory)
    {
        super(directory, LOAD, GROWTH);
    }

    @Override
    protected void onLoad(Game.Resources directory)
    {
        addConfiguration("character", new CharacterConfig());
        addConfiguration(GObjectFactory.CONFIG_ROOM, new RoomConfig());
    }

    @Override
    protected void onRequisition(GObject object)
    {

    }

    @Override
    protected void onRemove(GObject object)
    {

    }

    @Override
    protected void makeDefault(GObject object)
    {

    }

    private class CharacterConfig implements GObjectConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            final ImageFactory imgFact = resource.getImageFactory();
            final ImageComponent img = imgFact.getComponent("character");
            object.setImageComponent(img);
            object.setBodyComponent(new DemoBodyComponent(new Shape
                    (Shape.Type.RECTANGLE, 100, 100)));
        }
    }

    private class RoomConfig implements GObjectConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            final ImageFactory imgFact = resource.getImageFactory();
            final ImageComponent img = imgFact.getComponent(ImageFactory.CONFIG_ROOM);
            object.setImageComponent(img);
        }
    }
}
