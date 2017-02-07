package com.cinnamon.demo;

import com.cinnamon.object.BodyComponent;
import com.cinnamon.object.BodyFactory;
import com.cinnamon.utils.Shape;

/**
 * <p>
 *     Demo {@link BodyFactory}.
 * </p>
 *
 *
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
    protected void onRequisition(BodyComponent component)
    {

    }

    @Override
    protected void makeDefault(BodyComponent component)
    {

    }

    @Override
    protected void onLoad(Object resource)
    {
        addConfig("actor", new ActorConfig());
        addConfig("background", new BackgroundConfig());
    }

    @Override
    protected BodyComponent createComponent()
    {
        return new DemoBodyComponent();
    }

    private class ActorConfig implements BodyConfig
    {
        @Override
        public void configure(BodyComponent object, Object resource)
        {
            final float w = 100f;
            final float h = 100f;

            object.setWidth(100f);
            object.setHeight(100f);
            object.setOffset(0f, 0f);
            object.setShape(new Shape(Shape.Type.RECTANGLE, w, w));
        }
    }

    private class BackgroundConfig implements BodyConfig
    {
        @Override
        public void configure(BodyComponent object, Object resource)
        {
            final float w = 3840f;
            final float h = 2160f;

            object.setWidth(w);
            object.setHeight(h);
            object.setOffset(0f, 0f);
            object.setShape(new Shape(Shape.Type.RECTANGLE, w, h));
        }
    }
}
