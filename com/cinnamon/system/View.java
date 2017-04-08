package com.cinnamon.system;

import com.cinnamon.gfx.Drawable;
import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.object.*;
import com.cinnamon.utils.AxisAlignedRect;
import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.Point3F;
import com.cinnamon.utils.Rect2D;

/**
 * <p>
 *     A View defines the visible area to be drawn on screen. As such, the View's size cannot be larger than the
 *     {@link Window} that hosts it.
 * </p>
 *
 * <p>
 *     While the View's position moves in world coordinates, its width and height are measured in pixels, scaled
 *     according to its pixels per unit conversion factor. The View can also convert to and from screen coordinates
 *     as well, as shown through {@link #translateToWorld(Point2F)}.
 * </p>
 *
 * <p>
 *     The View's ability to zoom is a range of its scale defined by {@link #setScaleLimits(float, float)} where the
 *     closer the scale is to the minimum the smaller the game world appears. These limits can change, however, if
 *     constraining through {@link #setRoomConstrained(boolean)} after the limits have been set. For example, if the
 *     View is zoomed out enough so that the area beyond the {@link Room} is visible and then the View is
 *     constrained, the scale limits will be shifted such that the maximum zoom-out no longer shows beyond the Room.
 *     If this occurs, the scale limits' range will be maintained; if the range between minimum and maximum is 10 and
 *     the limits change due to being constrained, the range will still remain as 10.
 * </p>
 */
public final class View implements Positional
{
    /**
     * <p>Minimum View width allowed.</p>
     */
    public static final int MINIMUM_WIDTH = Window.MINIMUM_WIDTH;

    /**
     * <p>Minimum View height allowed.</p>
     */
    public static final int MINIMUM_HEIGHT = Window.MINIMUM_HEIGHT;

    // 75% in both directions from scale value (zoom level: 25% - 175%)
    private static final float DEFAULT_SCALE_RANGE = 0.75f;

    // Float precision when comparing scale values
    private static final float SCALE_FP_PRECISION = 0.00001f;

    // Minimum scale allowed through setScale() (maximum zoom out)
    private float mMinScale;

    // Maximum scale allowed through setScale() (minimum zoom in)
    private float mMaxScale;

    // Drawing scale (# of pixels per unit)
    private float mPxPerUnit;

    // View bounding box
    private final Rect2D mBoundary;

    // Containing area for view
    private Room mRoom;

    // Whether or not allowed to leave Room
    private boolean mConstrained = false;

    /**
     * <p>Constructor for a View whose dimensions fill the {@link Window} and present {@link Drawable}s at a specific
     * zoom level. The View's dimensions will match that of the Window.</p>
     *
     * @param window Window to fill.
     * @param pixelsPerUnit visual scale.
     */
    View(Window window, float pixelsPerUnit)
    {
        this(window, window.getWidth(), window.getHeight(), pixelsPerUnit);
    }

    /**
     * <p>Constructor for a View whose dimensions only show a part of the overall {@link Window} at a specific zoom
     * level.</p>
     *
     * @param window Window to represent a section of.
     * @param width desired section width.
     * @param height desired section height.
     * @param pixelsPerUnit visual scale.
     * @throws IllegalArgumentException if the width < {@link #MINIMUM_WIDTH}, height < {@link #MINIMUM_HEIGHT}, or
     * if either is greater than the Window's current width and height.
     */
    public View(Window window, int width, int height, float pixelsPerUnit)
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

        // Allow 50% smaller
        mMinScale = pixelsPerUnit * (1f - DEFAULT_SCALE_RANGE);

        // Allow 50% bigger
        mMaxScale = pixelsPerUnit * (1f + DEFAULT_SCALE_RANGE);

        // Set visual scale when drawing
        setScale(pixelsPerUnit);
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
     * {@inheritDoc}
     *
     * <P>The returned width is in pixels.</P>
     *
     * @return width.
     */
    @Override
    public float getWidth()
    {
        return mBoundary.getWidth();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned height is in pixels.</p>
     *
     * @return height.
     */
    @Override
    public float getHeight()
    {
        return mBoundary.getHeight();
    }

    /**
     * {@inheritDoc}
     *
     * <p>The given width should be in pixels.</p>
     *
     * @param width width.
     */
    @Override
    public void setWidth(float width)
    {
        mBoundary.setWidth(width);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The given height should be in pixels.</p>
     *
     * @param height height.
     */
    @Override
    public void setHeight(float height)
    {
        mBoundary.setHeight(height);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned coordinate is in world space.</p>
     *
     * @return center x.
     */
    @Override
    public float getCenterX()
    {
        return (mBoundary.getX() + (getWidth() / 2f)) / mPxPerUnit;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned coordinate is in world space.</p>
     *
     * @return center y.
     */
    @Override
    public float getCenterY()
    {
        return (mBoundary.getY() + (getHeight() / 2f)) / mPxPerUnit;
    }

    /**
     * <p>Gets the minimum scale factor allowed through {@link #setScale(float)}.</p>
     *
     * @return minimum number of pixels per unit.
     */
    public float getMinimumScale()
    {
        return mMinScale;
    }

    /**
     * <p>Gets the maximum scale factor allowed through {@link #setScale(float)}.</p>
     *
     * @return maximum number of pixels per unit.
     */
    public float getMaximumScale()
    {
        return mMaxScale;
    }

    /**
     * <p>Sets the minimum and maximum scale factors allowed through {@link #setScale(float)}.</p>
     *
     * @param min minimum number of pixels per unit.
     * @param max maximum number of pixels per unit.
     * @throws IllegalArgumentException if min >= max min <= 0, or max <= min.
     */
    public void setScaleLimits(float min, float max)
    {
        // Ensure min < max
        if (min > max || isEqual(min, max)) {
            throw new IllegalArgumentException("Min " + min + " must be < max " + max);
        }

        // Ensure min > 0
        if (min < 0f || isEqual(min, 0f)) {
            throw new IllegalArgumentException("Min " + min + " must be > 0");
        }

        // Ensure max > min
        if (max < min || isEqual(max, min)) {
            throw new IllegalArgumentException("Max " + max + " must be > min " + min);
        }

        mMinScale = min;
        mMaxScale = max;
    }

    /**
     * <p>Computes the ideal scale value to fit the View within the set {@link Room}. If the given scale value
     * already places the View within the Room, then the given scale is returned.</p>
     *
     * @param scale default scale.
     * @return ideal scale to fit inside Room, or given scale if already fits.
     */
    private float constrainScaleToRoom(float scale)
    {
        final float viewW = mBoundary.getWidth();
        final float viewH = mBoundary.getHeight();

        final float roomW = mRoom.getWidth();
        final float roomH = mRoom.getHeight();

        // No need to cap minimum scale if view is completely within the room
        if (viewW < roomW && Point2F.isEqual(viewW, roomW)
                && viewH < roomH && Point2F.isEqual(viewH, roomH)) {
            return scale;
        }

        // Compute ratios between view and room; one of these is ideal to scale the view
        final float ratioW = viewW / roomW;
        final float ratioH = viewH / roomH;

        // Larger ratio decides restricting dimension
        return (ratioW > ratioH || Point2F.isEqual(ratioW, ratioH)) ? ratioW : ratioH;
    }

    /**
     * <p>Checks if two floats are close enough to be considered equal using {@link #SCALE_FP_PRECISION} as the epsilon./p>
     *
     * @param f0 one float.
     * @param f1 other float.
     * @return true if can be considered equal.
     */
    private boolean isEqual(float f0, float f1)
    {
        return Math.abs(f0 - f1) < SCALE_FP_PRECISION;
    }

    /**
     * <p>Gets the visual scale used when comparing {@link Drawable}s to the View's bounds. The returned value is
     * the number of pixels that represent 1 unit.</p>
     *
     * @return pixels per unit.
     */
    public float getScale()
    {
        return mPxPerUnit;
    }

    /**
     * <p>Sets the visual scale used when interacting with {@link Drawable}s. This is the factor for converting
     * between world units and screen coordinates.</p>
     *
     * <p>If the given scaling factor is below the minimum set through {@link #setScaleLimits(float, float)}, the scale
     * becomes the value returned by {@link #getMinimumScale()}; similar occurs for the maximum value and
     * its corresponding methods.</p>
     *
     * @param pixelsPerUnit pixels per unit.
     */
    public void setScale(float pixelsPerUnit)
    {
        // Keep scale >= minimum
        if (pixelsPerUnit < mMinScale) {
            pixelsPerUnit = mMinScale;

        } else if (pixelsPerUnit > mMaxScale) {
            // Keep scale <= maximum
            pixelsPerUnit = mMaxScale;
        }

        // Change pixels per unit to a scale that'd keep the View within the Room
        if (mConstrained) {
            final float idealScale = constrainScaleToRoom(pixelsPerUnit);

            // Limit scale (pixels per unit) to >= ideal scale showing only room
            if (pixelsPerUnit < idealScale) {
                pixelsPerUnit = idealScale;

                // If new scale < min, shift min/max limits to accommodate new scale while preserving scale range
                if (pixelsPerUnit < mMinScale) {
                    final float range = mMaxScale - mMinScale;
                    mMinScale = pixelsPerUnit;
                    mMaxScale = mMinScale + range;
                }
            }
        }

        // Save scale
        mPxPerUnit = pixelsPerUnit;
    }

    /**
     * <p>Gets the x position in world coordinates.</p>
     *
     * @return x.
     */
    public float getX()
    {
        return mBoundary.getX() / mPxPerUnit;
    }

    /**
     * <p>Gets the y position in world coordinates.</p>
     *
     * @return y.
     */
    public float getY()
    {
        return mBoundary.getY() / mPxPerUnit;
    }

    /**
     * <p>This method will always return 0.</p>
     *
     * @return 0.
     */
    @Override
    public float getZ()
    {
        return 0f;
    }

    @Override
    public Point3F getPosition()
    {
        return new Point3F(getX(), getY(), getZ());
    }

    /**
     * <p>Attempts to move the View to a specific (x,y) point. If the View is room constrained, this method will
     * position the View as close as it can to the target point.</p>
     *
     * @param x x.
     * @param y y.
     */
    @Override
    public void moveTo(float x, float y)
    {
        if (mConstrained) {
            moveToConstrained(x, y);
        } else {
            mBoundary.moveTo(x * mPxPerUnit, y * mPxPerUnit);
        }
    }

    /**
     * <p>Attempts to move the View to a specific (x,y) point. If the View is room constrained, this method will
     * position the View as close as it can to the target point.</p>
     *
     * <p>The z value is ignored.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    @Override
    public void moveTo(float x, float y, float z)
    {
        moveTo(x, y);
    }

    /**
     * <p>Attempts to move the View by an x and y amount. If the View is room constrained, this method will position
     * the View as close as it can to the target point.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     */
    @Override
    public void moveBy(float x, float y)
    {
        moveTo(getX() + x, getY() + y);
    }

    /**
     * <p>Attempts to move the View by an x and y amount. If the View is room constrained, this method will position
     * the View as close as it can to the target point.</p>
     *
     * <p>The z value is ignored.</p>
     *
     * @param x amount along x.
     * @param y amount along y.
     * @param z amount along z.
     */
    @Override
    public void moveBy(float x, float y, float z)
    {
        moveTo(getX() + x, getY() + y);
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
        moveToCenter(object.getCenterX(), object.getCenterY());
    }

    /**
     * <p>Moves the View to center on an (x,y) point.</p>
     *
     * @param x x.
     * @param y y.
     */
    @Override
    public void moveToCenter(float x, float y)
    {
        final float halfW = (mBoundary.getWidth() / mPxPerUnit) / 2f;
        final float halfH = (mBoundary.getHeight() / mPxPerUnit) / 2f;
        moveTo(x - halfW, y - halfH);
    }

    /**
     * <p>Moves the View to a specific (x,y) point if doing so would not move the View's area beyond the set
     * {@link Room}. If executing a move would place the View beyond the Room, this method will only partially apply
     * the new position in that either of the offending x or y coordinates will be set to the Room's boundary.</p>
     *
     * @param x x.
     * @param y y.
     */
    private void moveToConstrained(float x, float y)
    {
        // Prevent position past room's left edge
        if (x < 0f) {
            x = 0f;
        }

        // Prevent position past room's bottom edge
        if (y < 0f) {
            y = 0f;
        }

        // Prevent position past room's right edge
        final float projectedEdgeX = x + (mBoundary.getWidth() / mPxPerUnit);
        final float roomW = mRoom.getWidth();
        if (projectedEdgeX > roomW) {
            x -= (projectedEdgeX - roomW);
        }

        // Prevent position past room's top edge
        final float projectedEdgeY = y + (mBoundary.getHeight() / mPxPerUnit);
        final float roomH = mRoom.getHeight();
        if (projectedEdgeY > roomH) {
            y -= (projectedEdgeY - roomH);
        }

        // Execute move with adjusted coords
        mBoundary.moveTo(x * mPxPerUnit, y * mPxPerUnit);
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
        final float worldX = screenToWorldX(event.getX());
        final float worldY = screenToWorldY(event.getY());

        // Update event with translated coords
        final MouseEvent.Button button = event.getButton();
        final InputEvent.Action action = event.getAction();
        event.update(button, action, worldX, worldY);
    }

    /**
     * <p>Converts a given {@link Point2F} from the traditional top left origin coordinate system to game world
     * coordinates (bottom left origin).</p>
     *
     * @param point Point2F.
     */
    public void translateToWorld(Point2F point)
    {
        // Compute world-based coordinates
        final float worldX = screenToWorldX(point.getX());
        final float worldY = screenToWorldY(point.getY());

        // Update with translated coords
        point.set(worldX, worldY);
    }

    /**
     * <p>Converts an x coordinate from screen space to world space while taking into account the View's pixels per
     * unit.</p>
     *
     * @param x screen x.
     * @return world x.
     */
    private float screenToWorldX(float x)
    {
        return (x + mBoundary.getX()) / mPxPerUnit;
    }

    /**
     * <p>Converts a y coordinate from screen space to world space while taking into account the View's pixels per unit
     * .</p>
     *
     * @param y screen y.
     * @return world y.
     */
    private float screenToWorldY(float y)
    {
        return (mBoundary.getHeight() - y + mBoundary.getY()) / mPxPerUnit;
    }

    /**
     * <p>Checks if at least a part of a {@link Drawable} is within the View's bounds and is therefore visible,
     * barring factors such as the component's transparency. The given Drawable is expected to represent the game
     * object in world space, without modifications from {@link #transform(Drawable)}.</p>
     *
     * @param drawable Drawable.
     * @return true if the Drawable intersects the View's rectangle.
     */
    public boolean intersects(Drawable drawable)
    {
        // Compute position in pixels
        final float x = drawable.getX() * mPxPerUnit;
        final float y = drawable.getY() * mPxPerUnit;

        // Compute dimensions in pixels
        final float cornerX = x + (drawable.getWidth() * mPxPerUnit);
        final float cornerY = y + (drawable.getHeight() * mPxPerUnit);

        // Test for intersection
        return mBoundary.intersects(x, y, cornerX, cornerY);
    }

    /**
     * <p>Changes a {@link Drawable}'s position and size relative to the View's position and scale. Although the
     * Drawable will be placed and sized to suit the View, there is no guarantee that the Drawable will be within the
     * View's boundaries.</p>
     *
     * @param drawable drawable.
     */
    public void transform(Drawable drawable)
    {
        // Compute drawable's position scaled and shifted according to view
        final float x = (drawable.getX() * mPxPerUnit) - mBoundary.getX();
        final float y = (drawable.getY() * mPxPerUnit) - mBoundary.getY();

        // Scale drawable according to view's zoom
        drawable.setWidth(drawable.getWidth() * mPxPerUnit);
        drawable.setHeight(drawable.getHeight() * mPxPerUnit);

        // Move to place
        drawable.moveTo(x, y);
    }

    /**
     * <p>Checks if the View should not be allowed to leave the set {@link Room}.</p>
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
        return "View@(" + mBoundary.getX() + "," + mBoundary.getY() + ")[w(" + mBoundary.getWidth() + "),h("
                + mBoundary.getHeight() + ")]";
    }
}