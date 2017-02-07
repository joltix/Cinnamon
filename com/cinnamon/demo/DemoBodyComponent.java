package com.cinnamon.demo;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.object.BodyComponent;
import com.cinnamon.utils.Shape;

/**
 * <p>
 *     Demo subclass of {@link BodyComponent}.
 * </p>
 *
 *
 */
public class DemoBodyComponent extends BodyComponent
{
    /**
     * <p>Constructs a blank DemoBodyComponent. The
     * DemoBodyComponent will have no {@link Shape} or bounding box</p>
     */
    public DemoBodyComponent()
    {

    }

    /**
     * <p>Constructs a DemoBodyComponent object based off of a specified
     * {@link Shape}.</p>
     *
     * @param shape polygon to represent edges.
     */
    public DemoBodyComponent(Shape shape)
    {
        setShape(shape);
    }

    /**
     * <p>Constructs a DemoBodyComponent based off of another
     * {@link ImageComponent}. The new DemoBodyComponent will use the same
     * {@link Shape} and bounding box as the given ImageComponent.</p>
     *
     * @param component ImageComponent to copy.
     */
    public DemoBodyComponent(BodyComponent component)
    {
        this(component.getShape());
    }
}
