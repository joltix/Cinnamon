package com.cinnamon.system;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.object.BodyComponent;
import com.cinnamon.object.GObject;
import com.cinnamon.object.Room;
import com.cinnamon.utils.AxisAlignedRect;
import com.cinnamon.utils.Rect2D;

/**
 * <p>
 *     A View defines the visible area to be drawn on screen. As such, the View's size cannot be larger than the
 *     {@link Window} that hosts it.
 * </p>
 *
 * <p>
 *     While the View itself moves in world coordinates, it can convert between screen and world coordinates, as
 *     shown through {@link #translateToWorld(MouseEvent)}.
 * </p>
 */
public final class View
{
    // Minimum width and height allowed
    private static final int MINIMUM_WIDTH = 1;
    private static final int MINIMUM_HEIGHT = 1;

    // View's shape (and boundaries)
    private final Rect2D mBoundary;

    // Containing area for view
    private Room mRoom;

    // Whether or not allowed to leave Room
    private boolean mConstrained = false;

    /**
     * <p>Constructor for a View whose dimensions fill the {@link Window}. The View's dimensions will match that of
     * the Window.</p>
     *
     * @param window Window to fill.
     */
    View(Window window)
    {
        this(window, window.getWidth(), window.getHeight());
    }

    /**
     * <p>Constructor for a View whose dimensions only show a part of the overall {@link Window}.</p>
     *
     * @param window Window to represent a section of.
     * @param width desired section width.
     * @param height desired section height.
     * @throws IllegalArgumentException if the width < {@link #MINIMUM_WIDTH}, height < {@link #MINIMUM_HEIGHT}, or
     * if either is greater than the Window's current width and height.
     */
    public View(Window window, int width, int height)
    {
        // Check if width is valid
        if (width < MINIMUM_WIDTH) {
            throw new IllegalArgumentException("Width must be at least " + MINIMUM_WIDTH);
        }
        if (width > window.getWidth()) {
            throw new IllegalArgumentException("Width cannot be greater than Window's width: " + window.getWidth());
        }

        // Check if height is valid
        if (height < MINIMUM_HEIGHT) {
            throw new IllegalArgumentException("Height must be at least " + MINIMUM_HEIGHT);
        }
        if (height > window.getHeight()) {
            throw new IllegalArgumentException("Height cannot be greater than Window's height: " + window.getHeight());
        }

        // Create View's bounding box
        mBoundary = new AxisAlignedRect(width, height);
    }

    /**
     * <p>Sets the {@link Room} within which the View moves.</p>
     *
     * @param room Room.
     */
    void setRoom(Room room)
    {
        mRoom = room;
    }

    /**
     * <P>Gets the width.</P>
     *
     * @return width.
     */
    public float getWidth()
    {
        return mBoundary.getWidth();
    }

    /**
     * <p>Gets the height.</p>
     *
     * @return height.
     */
    public float getHeight()
    {
        return mBoundary.getHeight();
    }

    /**
     * <p>Changes the View's size.</p>
     *
     * @param width width.
     * @param height height.
     */
    public void setSize(float width, float height)
    {
        mBoundary.setWidth(width);
        mBoundary.setHeight(height);
    }

    /**
     * <p>Gets the x position.</p>
     *
     * @return x.
     */
    public float getX()
    {
        return mBoundary.getX();
    }

    /**
     * <p>Gets the y position.</p>
     *
     * @return y.
     */
    public float getY()
    {
        return mBoundary.getY();
    }

    /**
     * <p>Attempts to move the View to a specific (x,y) point. If the View is room constrained, this method will
     * position the View as close as it can to the target point.</p>
     *
     * @param x x.
     * @param y y.
     */
    public void moveTo(float x, float y)
    {
        if (mConstrained) {
            moveToConstrained(x, y);
        } else {
            mBoundary.moveTo(x, y);
        }
    }

    /**
     * <p>Moves the View to center on a {@link GObject}.</p>
     *
     * <p>Centering will prioritize the position and dimensions of any {@link BodyComponent} over
     * {@link ImageComponent}.</p>
     *
     * @param object GObject.
     */
    public void moveToCenter(GObject object)
    {
        final float halfX = object.getX() + (object.getWidth() / 2);
        final float halfY = object.getY() + (object.getHeight() / 2);
        moveToCenter(halfX, halfY);
    }

    /**
     * <p>Moves the View to center on an (x,y) point.</p>
     *
     * @param x x.
     * @param y y.
     */
    public void moveToCenter(float x, float y)
    {
        moveTo(x - (getWidth() / 2), y - (getHeight() / 2));
    }

    /**
     * <p>Moves the View to a specific (x,y) point if doing so would not move the View's area beyond the set
     * {@link Room}. If executing a move would move the View beyond the Room, this method will only partially apply
     * the new position in that either of the offending x or y coordinates will be set to the Room's boundary.</p>
     *
     * @param x x.
     * @param y y.
     */
    private void moveToConstrained(float x, float y)
    {
        // Prevent position past room's left edge
        if (x < 0) {
            x = 0;
        }

        // Prevent position past room's bottom edge
        if (y < 0) {
            y = 0;
        }

        // Prevent position past room's right edge
        final float projectedEdgeX = x + mBoundary.getWidth();
        final float roomW = mRoom.getWidth();
        if (projectedEdgeX > roomW) {
            x -= (projectedEdgeX - roomW);
        }

        // Prevent position past room's top edge
        final float projectedEdgeY = y + mBoundary.getHeight();
        final float roomH = mRoom.getHeight();
        if (projectedEdgeY > roomH) {
            y -= (projectedEdgeY - roomH);
        }

        // Execute move with adjusted coords
        mBoundary.moveTo(x, y);
    }

    /**
     * <p>Converts a given {@link MouseEvent} from the traditional top left origin coordinate system to game world
     * coordinates (bottom left origin).</p>
     *
     * @param event MouseEvent.
     */
    public void translateToWorld(MouseEvent event)
    {
        // Compute world-based coordinates
        final float worldX = event.getX() + mBoundary.getX();
        final float worldY = mBoundary.getHeight() - event.getY() + mBoundary.getY();

        // Update event with translated coords
        final MouseEvent.Button button = event.getButton();
        final InputEvent.Action action = event.getAction();
        event.update(button, action, worldX, worldY);
    }

    /**
     * <p>Checks whether or not an {@link ImageComponent} contains the View and is therefore visible, barring factors
     * such as the component's set transparency.</p>
     *
     * @param component ImageComponent.
     * @return true if the ImageComponent contains the View's rectangle.
     */
    public boolean intersects(ImageComponent component)
    {
        // Get image position
        final float x = component.getX();
        final float y = component.getY();

        // Get image offset
        final float offX = component.getOffsetX();
        final float offY = component.getOffsetY();

        // Compute corner position with offset
        final float cornerAdjustX = x + component.getWidth() + offX;
        final float cornerAdjustY = y + component.getHeight() + offY;

        return mBoundary.intersects(x, y, cornerAdjustX, cornerAdjustY);
    }

    /**
     * <p>Checks whether or not the View should leave the set {@link Room}.</p>
     *
     * @return true if View should be fully contained.
     */
    public boolean isRoomConstrained()
    {
        return mConstrained;
    }

    /**
     * <p>Sets whether or not the View should be prevented from moving beyond the set {@link Room}.</p>
     *
     * @param enable true to keep the View from leaving the Room.
     */
    public void setRoomConstrained(boolean enable)
    {
        mConstrained = enable;
    }

    @Override
    public String toString()
    {
        return "View@(" + mBoundary.getX() + "," + mBoundary.getY() + ")[w("
                + mBoundary.getWidth() + "),h(" + mBoundary.getHeight() + ")]";
    }
}