package com.cinnamon.demo;

import com.cinnamon.object.BodyComponent;
import com.cinnamon.object.BodyFactory;
import com.cinnamon.system.Config;
import com.cinnamon.utils.Shape;

/**
 * <p>
 *     Demo {@link BodyFactory}.
 * </p>
 */
public class DemoBodyFactory extends BodyFactory
{
    private static final int LOAD = 1000;
    private static final float GROWTH = 0.1f;

    protected DemoBodyFactory()
    {
        super(new Object(), LOAD, GROWTH);
    }

    @Override
    protected Config<BodyComponent, Object> createDefaultConfig()
    {
        return new DefaultConfig();
    }

    @Override
    protected void onRequisition(BodyComponent object)
    {

    }

    @Override
    protected void onRemove(BodyComponent object)
    {

    }

    @Override
    protected void onLoad(Object resource)
    {
        addConfig("rock", new RockConfig());
        addConfig("character", new CharacterConfig());
    }

    @Override
    protected BodyComponent createIdentifiable()
    {
        return new BodyComponent(new Shape(10, 10));
    }

    private class CharacterConfig implements Config<BodyComponent, Object>
    {
        @Override
        public void configure(BodyComponent object, Object resource)
        {
            object.setWidth(1.5f);
            object.setHeight(2f);
            object.setMass(70f);
            object.setFriction(0.7f);
            object.setRestitution(0.6f);
            object.setCollidable(true);
            object.setStatic(false);
        }
    }

    private class RockConfig implements Config<BodyComponent, Object>
    {
        @Override
        public void configure(BodyComponent object, Object resource)
        {
            object.setShape(new Shape(10f, 10f));
            object.setMass(700f);
            object.setFriction(0.9f);
            object.setRestitution(0.1f);
            object.setCollidable(true);
            object.setStatic(false);
        }
    }

    private class DefaultConfig implements Config<BodyComponent, Object>
    {
        @Override
        public void configure(BodyComponent body, Object resource)
        {
            body.setWidth(1f);
            body.setHeight(1f);
            body.setAcceleration(null);
            body.setVelocity(null);
            body.moveTo(0f, 0f, 0f);
            body.rotateTo(0f);
            body.setStatic(false);
            body.setMass(1f);
            body.setFriction(0.6f);
            body.setRestitution(0.4f);
        }
    }
}
