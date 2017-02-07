package com.cinnamon.object;

import com.cinnamon.demo.DemoBodyComponent;
import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.system.Game;
import com.cinnamon.utils.Shape;

/**
 * Simple implementation of a {@link Room}.
 *
 *
 */
public class Room2D implements Room
{
    // Used to represent background when sorting draw order
    private final GObject mHost;

    // Know boundary and drawing info independent of host, as backup
    private BodyComponent mCollComp;
    private ImageComponent mGFXComp;

    // Room dimensions
    private final float mWidth;
    private final float mHeight;

    /**
     * <p>Constructor for a Room2D.</p>
     *
     * @param factory factory to produce a host {@link GObject}.
     * @param width boundary width.
     * @param height boundary height.
     */
    public Room2D(GObjectFactory factory, float width, float height)
    {
        // Calc room dimensions
        mWidth = width;
        mHeight = height;

        // Create host GObject and adjust for
        mHost = factory.getGObject(GObjectFactory.CONFIG_ROOM);

        // Build room collision
        final Shape box = new Shape(Shape.Type.RECTANGLE, width, height);
        mCollComp = new DemoBodyComponent(box);

        // No need for host to access collision
        mHost.setBodyComponent(null);

        // Attach background drawing component
        mGFXComp = mHost.getImageComponent();
        mGFXComp.setWidth(mWidth);
        mGFXComp.setHeight(mHeight);
        mGFXComp.setTransparency(0.4f);
        mHost.setImageComponent(mGFXComp);
    }

    @Override
    public ImageComponent getBackground()
    {
        return mGFXComp;
    }

    @Override
    public void setBackgroundImage(int texture)
    {
        if (mGFXComp != null) {
            mGFXComp.setTexture(texture);
        }
    }

    @Override
    public float getWidth()
    {
        return mWidth;
    }

    @Override
    public float getHeight()
    {
        return mHeight;
    }

    @Override
    public boolean contains(GObject object)
    {
        final BodyComponent coll = object.getBodyComponent();
        return coll != null && mCollComp.contains(coll);
    }
}
