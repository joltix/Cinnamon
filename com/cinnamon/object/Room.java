package com.cinnamon.object;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.gfx.Texture;
import com.cinnamon.system.Game;
import com.cinnamon.utils.AxisAlignedRect;
import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.Rect2D;

/**
 * <p>Defines a game area with a background image.</p>
 */
public class Room implements Dimensional
{
    // GObject host allows background ImageComponent drawing
    private final GObject mHost;

    // Room's bounding box (boundary)
    private final Rect2D mBounds;

    // Room name
    private String mName;

    /**
     * <p>Constructor for a Room.</p>
     *
     * @param resources resources.
     * @param width Room's width.
     * @param height Room's height.
     * @param background texture id.
     * @param name identifying name.
     * @throws IllegalArgumentException if width or height <= 0 or the given name is null, length() == 0, or is
     * only whitespace.
     */
    public Room(Game.Resources resources, float width, float height, int background, String name)
    {
        // Check width should be > 0f
        if (width < 0f || Point2F.isEqual(width, 0f)) {
            throw new IllegalArgumentException("Room width must be > 0: " + width);
        }

        // Check height should be > 0f
        if (height < 0f || Point2F.isEqual(height, 0f)) {
            throw new IllegalArgumentException("Room height must be > 0: " + height);
        }

        // Check name exists
        if (name == null || name.isEmpty() || name.matches("\\s+")) {
            throw new IllegalArgumentException("Room name may not be null or empty: " + name);
        }

        mHost = resources.getGObjectFactory().get();
        mBounds = new AxisAlignedRect(width, height);
        mName = name.trim();

        // Create background
        setupBackgroundImage(resources.getImageFactory(), width, height, background);
    }

    /**
     * <p>Creates and attaches the {@link ImageComponent} to be used as the Room's background image.</p>
     *
     * @param factory factory to produce ImageComponent.
     * @param width background's width.
     * @param height background's height.
     * @param background texture id.
     */
    private void setupBackgroundImage(ImageFactory factory, float width, float height, int background)
    {
        // Attach background image to Room
        final ImageComponent image = factory.get();
        mHost.setImageComponent(image);

        // Setup background image
        image.setWidth(width);
        image.setHeight(height);
        image.setTexture(background);
        image.setTint(0f, 0f, 0f);
        image.setTransparency(1f);

        // Place image at (0,0,max) for background img to be drawn behind all
        image.moveTo(0f, 0f, Float.MAX_VALUE);
    }

    /**
     * <p>Gets the Room's name.</p>
     *
     * @return name.
     */
    public final String getName()
    {
        return mName;
    }

    /**
     * <p>Sets the Room's name.</p>
     *
     * @param name name.
     */
    public final void setName(String name)
    {
        mName = name;
    }

    /**
     * <p>Gets the {@link Texture} to draw as the Room's background.</p>
     *
     * @return ImageComponent background.
     */
    public final int getBackground()
    {
        return mHost.getImageComponent().getTexture();
    }

    /**
     * <p>Sets the {@link Texture} id to use as a background image.</p>
     *
     * @param texture texture id.
     */
    public final void setBackground(int texture)
    {
        mHost.getImageComponent().setTexture(texture);
    }

    /**
     * <p>Sets the color tint for the background image.</p>
     *
     * @param r red.
     * @param g green.
     * @param b blue.
     */
    public final void setBackgroundTint(float r, float g, float b)
    {
        mHost.getImageComponent().setTint(r, g, b);
    }

    @Override
    public final float getWidth()
    {
        return mBounds.getWidth();
    }

    @Override
    public final float getHeight()
    {
        return mBounds.getHeight();
    }

    @Override
    public final float getCenterX()
    {
        return mBounds.getCenterX();
    }

    @Override
    public final float getCenterY()
    {
        return mBounds.getCenterY();
    }

    /**
     * <p>Checks whether or not a {@link GObject}'s bounding box is completely inside the Room.</p>
     *
     * @param object GObject.
     * @return true if the GObject is fully inside the Room.
     */
    public final boolean contains(GObject object)
    {
        final BodyComponent body = object.getBodyComponent();

        // Can't check if no body so assume not contained
        if (body == null) {
            return false;
        }

        // Check bounding boxes
        return mBounds.contains(body.getBounds());
    }
}