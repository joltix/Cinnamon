package com.cinnamon.system;

import com.cinnamon.gfx.Drawable;
import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.object.*;
import com.cinnamon.utils.*;

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
 *
 * <p>
 *     There are three kinds of ways to translate the View: instantaneous, focusing, and interpolation.
 *     Instantaneous translation is simply using the standard set of move methods like {@link #moveTo(float, float)}
 *     and {@link #moveToCenter(float, float)}; when these methods finish executing, the View's position has been
 *     updated as desired. Focusing, by using {@link #setFocus(GObject)}, is similar in this regard. However, the
 *     View is constantly moved to center on the focused {@link GObject} automatically in each game update, before
 *     the call to {@link Game#onUpdate()}. The last option, movement over time, designates a destination for the
 *     View to reach in a specified period. This is done with {@link #moveToCenter(float, float, long)}.
 * </p>
 *
 * <p>
 *     Scaling, on the other hand, can only be performed instantaneously or through interpolation through
 *     {@link #setScale(float)} and {@link #setScale(float, long)}, respectively.
 * </p>
 *
 * <p>
 *     All movement methods are mutually exclusive. All scaling methods are mutually exclusive. Using one type of
 *     movement or scaling will cancel all other types (motion cancels motion, scaling cancels scaling; they do not
 *     cross). If a limit is reached while interpolating scale, either because of room constraint or an explicit
 *     scale limit, scale interpolation is cancelled. To manually cancel an operation over time, {@link #stop()} can be
 *     called. After this method completes, the View's state remains as it was when stop() was called and the target
 *     values for interpolation are not achieved.
 * </p>
 *
 * <p>
 *     Interpolation behavior can be adjusted with {@link #setMoveInterpolator(TimeInterpolator)} and
 *     {@link #setScaleInterpolator(TimeInterpolator)}. By default, interpolated motion slows when approaching the
 *     destination. Interpolated scaling not only slows as the target value gets closer but also begins slowly as well.
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

    // Initial horizontal edge motion area 10% (5% off of left and right)
    private static final float DEFAULT_INACTIVE_HORIZONTAL = 0.1f;

    // Initial vertical edge motion area 10% (5% off of top and bottom)
    private static final float DEFAULT_INACTIVE_VERTICAL = 0.1f;

    // Float precision when comparing scale values
    private static final float SCALE_FP_PRECISION = 0.00001f;


    /**
     * Scaling values for zooming in and out
     */

    // Minimum scale allowed through setScale() (maximum zoom out)
    private float mMinScale;

    // Maximum scale allowed through setScale() (minimum zoom in)
    private float mMaxScale;

    // Drawing scale (# of pixels per unit)
    private float mPxPerUnit = 1f;

    /**
     * Edge motion; moving View by moving cursor to View's edges
     */

    // Sub rectangle centered on View; if cursor's outside this rect, edge motion takes effect
    private final Rect2D mInactiveZone = new AxisAlignedRect(1f, 1f, 0f, 0f);

    // Percentage of width to set aside for detecting cursor (10% = 10% off bottom, 10% off top)
    private float mMotionBoundsH = DEFAULT_INACTIVE_HORIZONTAL;

    // Percentage of height to set aside for detecting cursor (10% = 10% off left, 10% off right)
    private float mMotionBoundsV = DEFAULT_INACTIVE_VERTICAL;

    // Number of horizontal pixels reserved for inactive zone
    private float mMotionBoundsPxH;

    // Number of vertical pixels reserved for inactive zone
    private float mMotionBoundsPxV;

    // Speed used during edge motion (meters per second)
    private float mEdgeSpeed = 0.1f;

    // Source for getting mouse position
    private final Game mGame;

    // Toggle to enable edge motion
    private boolean mEdgeMotion = false;

    /**
     * Move interpolation
     */

    // TimeInterpolator used in moving to pos over time
    private TimeInterpolator mMoveInterpolator = new DecelInterpolator();

    // Destination pos for move over time
    private Point2F mTargetPos = new Point2F(0f, 0f);

    // Pos when move over time was started
    private Point2F mStartPos = new Point2F(0f, 0f);

    // Vector representing direction towards target pos when interpolating move
    private final Vector2F mMoveDirection = new Vector2F();

    // True = View is interpolating motion to a target pos
    private boolean mMovingToTarget = false;

    /**
     * Scale interpolation
     */

    // TimeInterpolator used in scaling over time
    private TimeInterpolator mScaleInterpolator = new EaseInterpolator();

    // True = View is interpolating towards a scale
    private boolean mScalingToTarget = false;

    /**
     * General View properties like position in world, move interpolator, boundary, and more
     */

    // View bounding box
    private final Rect2D mBoundary = new AxisAlignedRect(1f, 1f, 0f, 0f);

    // X position in world units
    private float mWorldX;

    // Y position in world units
    private float mWorldY;

    // GObject to center on in each game update
    private GObject mFocus;

    // Containing area for view
    private Room mRoom;

    // Whether or not allowed to leave Room
    private boolean mConstrained = false;

    /**
     * <p>Constructor for a View whose dimensions fill the {@link Window} and present {@link Drawable}s at a specific
     * zoom level. The View's dimensions will match that of the Window.</p>
     *
     * @param game Game.
     * @param pixelsPerUnit visual scale.
     */
    View(Game game, float pixelsPerUnit)
    {
        mGame = game;
        final Window window = game.getCanvas().getWindow();

        // Initialize size to fill Window
        init(window.getWidth(), window.getHeight(), pixelsPerUnit);
    }

    /**
     * <p>Constructor for a View whose dimensions only show a part of the overall {@link Window} at a specific zoom
     * level.</p>
     *
     * @param game Game.
     * @param width desired section width.
     * @param height desired section height.
     * @param pixelsPerUnit visual scale.
     * @throws IllegalArgumentException if the width < {@link #MINIMUM_WIDTH}, height < {@link #MINIMUM_HEIGHT}, or
     * if either is greater than the Window's current width and height, or if pixelsPerUnit <= 0.
     */
    public View(Game game, int width, int height, float pixelsPerUnit)
    {
        mGame = game;
        final Window window = game.getCanvas().getWindow();

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

        // Initialize size, scaling, and edge motion boundaries
        init(width, height, pixelsPerUnit);
    }

    /**
     * <p>Initializes the View's dimensions, scaling, and edge motion parameters.</p>
     *
     * @param width desired width.
     * @param height desired height.
     * @param pixelsPerUnit number of pixels per world unit.
     * @throws IllegalArgumentException if pixelsPerUnit <= 0.
     */
    private void init(float width, float height, float pixelsPerUnit)
    {
        // Ensure pixels per unit > 0
        if (pixelsPerUnit < 0f || isEqual(pixelsPerUnit, 0f)) {
            throw new IllegalArgumentException("pixelsPerUnit must be > 0: " + pixelsPerUnit);
        }

        // Size View's bounding box
        mBoundary.setWidth(width);
        mBoundary.setHeight(height);

        // Allow 75% smaller
        mMinScale = pixelsPerUnit * (1f - DEFAULT_SCALE_RANGE);

        // Allow 75% bigger
        mMaxScale = pixelsPerUnit * (1f + DEFAULT_SCALE_RANGE);

        // Set visual scale when drawing
        setScale(pixelsPerUnit);

        // Size and center inactive zone inside boundary
        setEdgeMotionBounds(DEFAULT_INACTIVE_HORIZONTAL, DEFAULT_INACTIVE_VERTICAL);
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

        sizeInactiveZone();
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

        sizeInactiveZone();
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
        return (ratioW > ratioH || isEqual(ratioW, ratioH)) ? ratioW : ratioH;
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
     * @return true if the desired scale was successfully reached and neither room constraint nor scale limits
     * changed the scale.
     */
    public boolean setScale(float pixelsPerUnit)
    {
        // Cancel interpolated scaling
        mScalingToTarget = false;

        return setScaleNoCancel(pixelsPerUnit);
    }

    /**
     * <p>Scales the View to a specific scale value over a period of time. If the given scale value is the current
     * scale, this method does nothing.</p>
     *
     * <p>Calling {@link #setScale(float)} will cancel the interpolation caused by this method.</p>
     *
     * <p>If the View is room constrained, this method will scale the View as close as it can to the target scale.</p>
     *
     * @param pixelsPerUnit pixels per unit.
     * @param duration time to take in milliseconds.
     * @throws IllegalArgumentException if duration <= 0.
     */
    public void setScale(float pixelsPerUnit, long duration)
    {
        // Ensure duration > 0
        if (duration <= 0) {
            throw new IllegalArgumentException("Scaling over time requires duration > 0: " + duration);
        }

        // No need to interpolate if already at scale
        if (isEqual(pixelsPerUnit, mPxPerUnit)) {
            return;
        }

        // Flag View as moving over time and setup interpolator
        mScalingToTarget = true;
        mScaleInterpolator.start(duration, mPxPerUnit, pixelsPerUnit);
    }

    /**
     * <p>Version of {@link #setScale(float)} without cancelling interpolated scaling caused by
     * {@link #setScale(float, long)}.</p>
     *
     * @param pixelsPerUnit pixels per unit.
     * @return true if the desired scale was successfully reached and neither room constraint nor scale limits
     * changed the scale.
     */
    private boolean setScaleNoCancel(float pixelsPerUnit)
    {
        boolean limited = false;

        // Keep scale >= minimum
        if (pixelsPerUnit < mMinScale) {
            pixelsPerUnit = mMinScale;
            limited = true;

        } else if (pixelsPerUnit > mMaxScale) {
            // Keep scale <= maximum
            pixelsPerUnit = mMaxScale;
            limited = true;
        }

        // Change pixels per unit to a scale that'd keep the View within the Room
        if (mConstrained) {
            final float idealScale = constrainScaleToRoom(pixelsPerUnit);

            // Limit scale (pixels per unit) to >= ideal scale to show only room
            if (pixelsPerUnit < idealScale) {
                pixelsPerUnit = idealScale;
                limited = true;

                // If new scale < min, shift min/max limits to accommodate new scale while preserving scale range
                if (pixelsPerUnit < mMinScale) {
                    final float range = mMaxScale - mMinScale;
                    mMinScale = pixelsPerUnit;
                    mMaxScale = mMinScale + range;
                }
            }
        }

        // Save scale and apply
        scaleOnFocus(pixelsPerUnit);
        return !limited;
    }

    /**
     * <p>Moves the {@link View} to center on the focus then applies the given scale. If no focus has been set, the
     * View is re-centered on the previous scale's center point adjusted for the new scaling factor.</p>
     *
     * @param pixelsPerUnit scale.
     */
    private void scaleOnFocus(float pixelsPerUnit)
    {
        // Get current center position in world
        final float centerX = getCenterX();
        final float centerY = getCenterY();

        // Save scale then update position with new scale
        mPxPerUnit = pixelsPerUnit;

        if (mFocus == null) {
            // Default to scaling on center position of old scale value
            moveToCenterNoCancel(centerX, centerY);

        } else {
            // Scale on to target if available
            moveToCenterNoCancel(mFocus.getCenterX(), mFocus.getCenterY());
        }
    }

    /**
     * <p>Checks if the View is interpolating towards a specific scale.</p>
     *
     * @return true if scaling over time.
     */
    public boolean isScaling()
    {
        return mScalingToTarget;
    }

    /**
     * <p>Gets the x position in world coordinates.</p>
     *
     * @return x.
     */
    public float getX()
    {
        return mWorldX;
    }

    /**
     * <p>Gets the y position in world coordinates.</p>
     *
     * @return y.
     */
    public float getY()
    {
        return mWorldY;
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
        return new Point3F(mWorldX, mWorldY, 0f);
    }

    /**
     * <p>Attempts to move the View to a specific (x,y) point.</p>
     *
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
     *
     * @param x x.
     * @param y y.
     */
    @Override
    public void moveTo(float x, float y)
    {
        // Cancel move order for motion over time
        mMovingToTarget = false;

        // Cancel focusing
        mFocus = null;

        moveToNoCancel(x, y);
    }

    /**
     * <p>Attempts to move the View to a specific (x,y) point.</p>
     *
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
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
     * <p>Attempts to move the View by an x and y amount.</p>
     *
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
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
     * <p>Attempts to move the View by an x and y amount.</p>
     *
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
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
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
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
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
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
     * <p>Moves the View to a specific (x,y) point over a period of time. If the given position is the current
     * position, this method does nothing.</p>
     *
     * <p>Moving to a position using this method will prevent edge motion until the target position has been reached
     * . However, calling any of the other move variants like {@link #moveTo(float, float)} and
     * {@link #moveBy(float, float)} will cancel the motion caused by this method.</p>
     *
     * <p>If the View is room constrained, this method will position the View as close as it can to the target point
     * .</p>
     *
     *  @param x x.
     * @param y y.
     * @param duration time to take in milliseconds.
     * @throws IllegalArgumentException if duration <= 0.
     */
    public void moveToCenter(float x, float y, long duration)
    {
        // Ensure duration > 0
        if (duration <= 0) {
            throw new IllegalArgumentException("Moving over time requires duration > 0: " + duration);
        }

        // No need to interpolate if already at position
        if (isEqual(x, mWorldX) && isEqual(y, mWorldY)) {
            return;
        }

        // Cancel focusing
        mFocus = null;

        // Save starting pos and stopping pos
        mStartPos.set(getCenterX(), getCenterY());
        mTargetPos.set(x, y);

        // Compute direction for motion
        mMoveDirection.set(mTargetPos.getX() - mStartPos.getX(), mTargetPos.getY() - mStartPos.getY());
        mMoveDirection.normalize();

        // Flag View as moving over time and setup interpolator
        mMovingToTarget = true;
        mMoveInterpolator.start(duration, 0f, (float) Point2F.distanceBetween(x, y, getCenterX(), getCenterY()));
    }

    /**
     * <p>Version of {@link #moveToCenter(float, float)} without cancelling interpolated motion caused by
     * {@link #moveToCenter(float, float, long)}, unlike the public move methods.</p>
     *
     * @param x x.
     * @param y y.
     */
    private void moveToCenterNoCancel(float x, float y)
    {
        final float halfW = (mBoundary.getWidth() / mPxPerUnit) / 2f;
        final float halfH = (mBoundary.getHeight() / mPxPerUnit) / 2f;
        moveToNoCancel(x - halfW, y - halfH);
    }

    /**
     * <p>Version of {@link #moveTo(float, float)} without cancelling interpolated motion caused by
     * {@link #moveToCenter(float, float, long)}.</p>
     *
     * @param x x.
     * @param y y.
     */
    private void moveToNoCancel(float x, float y)
    {
        // Adjust position if room constrained before actually moving
        if (mConstrained) {
            moveToConstrained(x, y);
        } else {
            applyMove(x, y);
        }
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
        applyMove(x, y);
    }

    /**
     * <p>Sets the View's new position in world space, updates the representation in scaled screen space, and moves
     * the inactive zone along with the View.</p>
     *
     * @param x x in world space.
     * @param y y in world space.
     */
    private void applyMove(float x, float y)
    {
        // Update world position
        mWorldX = x;
        mWorldY = y;

        // Update scaled position
        mBoundary.moveTo(x * mPxPerUnit, y * mPxPerUnit);

        // Center inactive within boundary
        mInactiveZone.moveToCenter(mBoundary.getCenterX(), mBoundary.getCenterY());
    }

    /**
     * <p>Checks if the View is interpolating towards a specific position.</p>
     *
     * @return true if moving over time.
     */
    public boolean isMoving()
    {
        return mMovingToTarget;
    }

    /**
     * <p>Cancels all operations being performed over time. This includes following a focused {@link GObject} as well
     * as scaling or moving to a specific value or position over time.</p>
     */
    public void stop()
    {
        mMovingToTarget = false;
        mFocus = null;
        mScalingToTarget = false;
    }

    /**
     * <p>Sets the {@link TimeInterpolator} to use when moving over time towards a destination.</p>
     *
     * @param interpolator interpolator.
     */
    public void setMoveInterpolator(TimeInterpolator interpolator)
    {
        mMoveInterpolator = interpolator;
    }

    /**
     * <p>Sets the {@link TimeInterpolator} to use when scaling over time towards a target value.</p>
     *
     * @param interpolator interpolator.
     */
    public void setScaleInterpolator(TimeInterpolator interpolator)
    {
        mScaleInterpolator = interpolator;
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
     * <p>Checks if there is a {@link GObject} the View is following.</p>
     *
     * @return true if the View is following something.
     */
    public boolean isFocusing()
    {
        return mFocus != null;
    }

    /**
     * <p>Gets the {@link GObject} the View follows in each game update.</p>
     *
     * @return GObject being followed.
     */
    public GObject getFocus()
    {
        return mFocus;
    }

    /**
     * <p>Sets the {@link GObject} for the View to follow.</p>
     *
     * @param focus GObject to follow.
     */
    public void setFocus(GObject focus)
    {
        mMovingToTarget = false;
        mFocus = focus;
    }

    /**
     * <p>Updates the state to advance operations over time such as interpolating towards a position or a scale or
     * following a focused {@link GObject}.</p>
     */
    void update()
    {
        // Center on focused gobj if one is set
        if (isFocusing()) {
            moveToCenterNoCancel(mFocus.getCenterX(), mFocus.getCenterY());

            // Move to interpolated position on way to target pos
        } else if (isMoving()) {
            moveInterpolated();

            // Check if should move by nearing cursor to edges
        } else if (isEdgeMotionEnabled()) {
            moveTowardsCursor();
        }

        // Interpolate scale if needed
        if (isScaling()) {
            // Cancel scaling if finished or interpolated scale hit a limit
            if (mScaleInterpolator.isFinished() || !setScaleNoCancel(mScaleInterpolator.get())) {
                mScalingToTarget = false;
            }
        }
    }

    /**
     * <p>Moves the View toward a target position designated from the move order from
     * {@link #moveToCenter(float, float, long)}. The position set is interpolated between the View's position when
     * the move order was made and the target destination over a period of time.</p>
     */
    private void moveInterpolated()
    {
        if (!mMoveInterpolator.isFinished()) {

            // Compute position based on progress to target position
            final float progress = mMoveInterpolator.get();
            final float x = mStartPos.getX() + (mMoveDirection.getX() * progress);
            final float y = mStartPos.getY() + (mMoveDirection.getY() * progress);

            // Center on position without effects caused by public move methods
            moveToCenterNoCancel(x, y);

        } else {
            mMovingToTarget = false;
        }
    }

    /**
     * <p>Moves the View by the value given by {@link #getSpeed()} along the x and y axes towards the edge(s) of the
     * inactive zone.</p>
     */
    private void moveTowardsCursor()
    {
        // Get mouse's position scaled to View
        final Point2F mousePos = mGame.getMousePosition();
        translateToWorld(mousePos);
        final float x = mousePos.getX() * mPxPerUnit;
        final float y = mousePos.getY() * mPxPerUnit;

        // Don't move if mouse is beyond View's edges
        if (!mBoundary.contains(x, y)) {
            return;
        }

        float shiftX = 0f;
        float shiftY = 0f;

        // Figure if there should be a horizontal translation
        if (x > mInactiveZone.getCornerX()) {
            shiftX += mEdgeSpeed;
        } else if (x < mInactiveZone.getX()) {
            shiftX -= mEdgeSpeed;
        }

        // Figure if there should be a vertical translation
        if (y > mInactiveZone.getCornerY()) {
            shiftY += mEdgeSpeed;
        } else if (y < mInactiveZone.getY()) {
            shiftY -= mEdgeSpeed;
        }

        // Translate View according to shift
        moveBy(shiftX, shiftY);
    }

    /**
     * <p>Checks if the View may be moved by moving the cursor towards the View's edges.</p>
     *
     * @return true if the View can be moved by its edges.
     */
    public boolean isEdgeMotionEnabled()
    {
        return mEdgeMotion;
    }

    /**
     * <p>Sets if the View may be moved by moving the cursor towards the View's edges.</p>
     *
     * @param enable true to allow motion by its edges.
     */
    public void setEdgeMotionEnabled(boolean enable)
    {
        mEdgeMotion = enable;
    }

    /**
     * <p>Gets the percentage of the View's width being used for edge motion. This value is split between the left
     * and right sides (e.g. 0.1 horizontally means 0.05 off the left and 0.05 off the right).</p>
     *
     * @return percentage of width set aside for edge motion.
     */
    public float getEdgeMotionBoundsHorizontal()
    {
        return mMotionBoundsH;
    }

    /**
     * <p>Gets the percentage of the View's height being used for edge motion. This value is split between the top
     * and bottom sides (e.g. 0.1 vertically means 0.05 off the top and 0.05 off the bottom).</p>
     *
     * @return percentage of height set aside for edge motion.
     */
    public float getEdgeMotionBoundsVertical()
    {
        return mMotionBoundsV;
    }

    /**
     * <p>Sets a percentage of the View's width and height to form an area around the edges to be used as a
     * boundary for following the cursor. If the cursor's position ends up in this edge area, the View
     * will move towards the cursor's position at the speed returned by {@link #getSpeed()}.</p>
     *
     * @param horizontal 0 to 1 (exclusive).
     * @param vertical 0 to 1 (exclusive).
     */
    public void setEdgeMotionBounds(float horizontal, float vertical)
    {
        // Check horizontal should be >= 0f
        if (horizontal < 0f) {
            throw new IllegalArgumentException("Horizontal percentage must be >= 0: " + horizontal);
        }

        // Check horizontal should be < 1f
        if (horizontal > 1f || isEqual(horizontal, 1f)) {
            throw new IllegalArgumentException("Horizontal percentage must be < 1: " + horizontal);
        }

        // Check vertical should be >= 0f
        if (vertical < 0f) {
            throw new IllegalArgumentException("Vertical percentage must be >= 0: " + vertical);
        }

        // Check vertical should be < 1f
        if (vertical > 1f || isEqual(vertical, 1f)) {
            throw new IllegalArgumentException("Vertical percentage must be < 1: " + vertical);
        }

        // Save percentages for getters
        mMotionBoundsH = horizontal;
        mMotionBoundsV = vertical;

        // Save actual pixel amounts for computations
        mMotionBoundsPxH = mBoundary.getWidth() * horizontal;
        mMotionBoundsPxV = mBoundary.getHeight() * vertical;

        sizeInactiveZone();
    }

    /**
     * <p>Computes and sets the size for the inactive zone based off of the boundary's current size and edge motion
     * bounds percentages. The inactive zone is also centered on the boundary.</p>
     */
    private void sizeInactiveZone()
    {
        // Compute inactive zone's new dimensions in scaled world space
        final float width = mBoundary.getWidth() - mMotionBoundsPxH;
        final float height = mBoundary.getHeight() - mMotionBoundsPxV;

        // Apply new size
        mInactiveZone.setWidth(width);
        mInactiveZone.setHeight(height);

        // Center inactive within boundary
        mInactiveZone.moveToCenter(mBoundary.getCenterX(), mBoundary.getCenterY());
    }

    /**
     * <p>Gets the speed used when the View moves due to edge motion.</p>
     *
     * @return speed in meters per second.
     */
    public float getSpeed()
    {
        return mEdgeSpeed;
    }

    /**
     * <p>Sets the speed to use when the View moves due to edge motion.</p>
     *
     * @param speed in meters per second.
     */
    public void setSpeed(float speed)
    {
        mEdgeSpeed = speed;
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
        return "View[@(" + mBoundary.getX() + "," + mBoundary.getY() + "), w(" + mBoundary.getWidth() + "),h("
                + mBoundary.getHeight() + "), edgeH(" + mMotionBoundsH + "), edgeV(" + mMotionBoundsV + ")]";
    }
}
