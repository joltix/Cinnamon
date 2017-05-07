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
    /**
     * Boundary wall properties
     */

    // Minimum dimension, either width or height depending on which wall
    private static final float WALL_THICKNESS = 500f;

    // Infinite mass prevents motion from collisions
    private static final float WALL_MASS = 0f;

    // COR
    private static final float WALL_RESTITUTION = 0.8f;

    // COF
    private static final float WALL_FRICTION = 0.4f;

    /**
     * General members
     */

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
        setupBackgroundImage(resources.getImageFactory(), background);

        // Setup boundary walls
        setupWalls(resources);
    }

    /**
     * <p>Creates and attaches the {@link ImageComponent} to be used as the Room's background image.</p>
     *
     * @param factory factory to produce ImageComponent.
     * @param background texture id.
     */
    private void setupBackgroundImage(ImageFactory factory, int background)
    {
        // Attach background image to Room
        final ImageComponent image = factory.get();
        mHost.setImageComponent(image);

        // Setup background image
        image.setWidth(mBounds.getWidth());
        image.setHeight(mBounds.getHeight());
        image.setTexture(background);
        image.setTint(0f, 0f, 0f);
        image.setTransparency(1f);

        // Place image at (0,0,max) for background img to be drawn behind all
        image.moveTo(0f, 0f, Float.MAX_VALUE);
    }

    /**
     * <p>Sets up four walls around the Room: north, south, east, and west.</p>
     *
     * <p>Each wall is a {@link GObject} whose {@link BodyComponent} has been configured as an invisible immovable
     * game object.</p>
     *
     * @param resources resources for assembling the walls' game objects.
     */
    private void setupWalls(Game.Resources resources)
    {
        // Unwrap for factories
        final GObjectFactory gobjFactory = resources.getGObjectFactory();
        final BodyFactory bodyFactory = resources.getBodyFactory();
        final ImageFactory imageFactory = resources.getImageFactory();

        // Get sizing info
        final float cornerX = getWidth();
        final float cornerY = getHeight();
        final float width = mBounds.getWidth();
        final float height = mBounds.getHeight();
        final float overlap = 2f * WALL_THICKNESS;

        // Create northern wall
        final GObject wallN = gobjFactory.get();
        final BodyComponent bodyN = bodyFactory.get(WALL_MASS);
        final ImageComponent imageN = imageFactory.get();
        createWall(wallN, bodyN, imageN, -WALL_THICKNESS, cornerY, width + overlap, WALL_THICKNESS);

        // Create southern wall
        final GObject wallS = gobjFactory.get();
        final BodyComponent bodyS = bodyFactory.get(WALL_MASS);
        final ImageComponent imageS = imageFactory.get();
        createWall(wallS, bodyS, imageS, -WALL_THICKNESS, -WALL_THICKNESS, width + overlap, WALL_THICKNESS);

        // Create eastern wall
        final GObject wallE = gobjFactory.get();
        final BodyComponent bodyE = bodyFactory.get(WALL_MASS);
        final ImageComponent imageE = imageFactory.get();
        createWall(wallE, bodyE, imageE, cornerX, -WALL_THICKNESS, WALL_THICKNESS, height + overlap);

        // Create western wall
        final GObject wallW = gobjFactory.get();
        final BodyComponent bodyW = bodyFactory.get(WALL_MASS);
        final ImageComponent imageW = imageFactory.get();
        createWall(wallW, bodyW, imageW, -WALL_THICKNESS, -WALL_THICKNESS, WALL_THICKNESS, height + overlap);
    }

    /**
     * <p>Creates a wall of a given size at a given position.</p>
     *
     * <p>The given {@link BodyComponent} and {@link ImageComponent} are associated with the given {@link GObject}
     * and the desired width, height, and (x,y) position is applied.</p>
     *
     * @param object object.
     * @param body body.
     * @param image image.
     * @param x x.
     * @param y y.
     * @param width width.
     * @param height height.
     */
    private void createWall(GObject object, BodyComponent body, ImageComponent image, float x, float y, float width,
                            float height)
    {
        object.setBodyComponent(body);
        object.setImageComponent(image);

        // Move to position and resize
        object.moveTo(x, y);
        object.setWidth(width);
        object.setHeight(height);

        // Set physics properties
        body.setIgnoreGObjectParent(false);
        body.setCollidable(true);
        body.setRestitution(WALL_RESTITUTION);
        body.setFriction(WALL_FRICTION);

        // Set visual to invisible walls
        image.setTexture(Texture.NULL);
        image.setTransparency(0f);
        image.setTint(0f, 0f, 0f);
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