package com.cinnamon.demo;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.system.Config;
import com.cinnamon.system.Game;

/**
 * <p>Demo {@link GObjectFactory}.</p>
 */
public class DemoGObjectFactory extends GObjectFactory<GObject>
{
    private static final int LOAD = 100;
    private static final float GROWTH = 0.15f;

    public DemoGObjectFactory(Game.Resources directory)
    {
        super(directory, LOAD, GROWTH);
    }

    @Override
    protected Config<GObject, Game.Resources> createDefaultConfig()
    {
        return new DefaultConfig();
    }

    @Override
    protected void onRemove(GObject object)
    {

    }

    @Override
    protected void onLoad(Game.Resources directory)
    {
        addConfig("char", new CharacterConfig());
        addConfig("red_char", new RedCharacterConfig());
        addConfig("green_char", new GreenCharacterConfig());
        addConfig("blue_char", new BlueCharacterConfig());
        addConfig("rock", new RockConfig());
    }

    @Override
    protected GObject createIdentifiable()
    {
        return new GObject();
    }

    @Override
    protected void onRequisition(GObject object)
    {

    }

    private class DefaultConfig implements Config<GObject, Game.Resources>
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            object.setBodyComponent(null);
            object.setImageComponent(null);
            object.setHeight(1f);
            object.setWidth(1f);
            object.moveTo(0f, 0f, 0f);
            object.rotateTo(0f);
            object.setOnClickListener(null);
        }
    }

    private class CharacterConfig implements Config<GObject, Game.Resources>
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            final ImageFactory imgFact = resource.getImageFactory();
            final ImageComponent img = imgFact.get("character");
            object.setImageComponent(img);

            object.setBodyComponent(resource.getBodyFactory().get("character"));
            object.setWidth(1.5f);
            object.setHeight(2f);
            object.rotateTo(0f);
        }
    }

    private class RedCharacterConfig extends CharacterConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            super.configure(object, resource);
            object.getImageComponent().setTint(1f, 0f, 0f);
        }
    }

    private class GreenCharacterConfig extends CharacterConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            super.configure(object, resource);
            object.getImageComponent().setTint(0f, 1f, 0f);
        }
    }

    private class BlueCharacterConfig extends CharacterConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            super.configure(object, resource);
            object.getImageComponent().setTint(0f, 0f, 1f);
        }
    }

    private class RockConfig implements Config<GObject, Game.Resources>
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            object.setImageComponent(resource.getImageFactory().get("rock"));
            object.setBodyComponent(resource.getBodyFactory().get("rock"));
            object.setWidth(10f);
            object.setHeight(10f);
        }
    }
}
