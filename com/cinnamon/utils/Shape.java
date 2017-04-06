package com.cinnamon.utils;

import com.cinnamon.object.Positional;


/**
 * <p>Shapes define 2D polygons.</p>
 */
public final class Shape implements Positional, Rotatable
{
    // Number of coordinates per point
    private static final int FLOATS_PER_POINT = 2;

    // Expected number of coordinates for a rectangular Shape
    private static final int POINTS_SIZE_RECTANGLE = FLOATS_PER_POINT * 4;

    // Bounding box
    private final Rect2D mRect = new AxisAlignedRect(1, 1);

    private double mAngle;

    // Arr to hold point when scaling
    private final float[] mPt = new float[2];

    // Points defining edges
    private final float[] mPoints;

    // Number of coordinates forming polygon
    private int mSize;

    // Shape edit status
    private boolean mLocked = false;

    /**
     * <p>Constructor for a Shape prepared to define its polygon with a specific number of points.</p>
     *
     * @param pointCount number of points defining the Shape.
     */
    public Shape(int pointCount)
    {
        mPoints = new float[pointCount * FLOATS_PER_POINT];
    }

    /**
     * <p>Constructor for an axis aligned rectangular Shape.</p>
     *
     * @param width width.
     * @param height height.
     */
    public Shape(float width, float height)
    {
        mPoints = new float[POINTS_SIZE_RECTANGLE];
        mSize = 4;
        buildRectangle(width, height);

        // Prevent new points
        lock();
    }

    /**
     * <p>Constructs a Shape based off of another Shape. All points and lock status are copied.</p>
     *
     * @param shape other Shape.
     */
    public Shape(Shape shape)
    {
        mPoints = shape.mPoints.clone();
        mSize = shape.mSize;
        mLocked = shape.mLocked;

        // Resize bounding box to contain the new polygon
        updateBounds();
    }

    /**
     * <p>Defines the first point as the bottom left corner of the rectangle. The rest of the points are defined
     * counter-clockwise.</p>
     *
     * @param width width.
     * @param height height.
     */
    private void buildRectangle(float width, float height)
    {
        // Bottom left
        mPoints[0] = 0f;
        mPoints[1] = 0f;

        // Bottom right
        mPoints[2] = width;
        mPoints[3] = 0;

        // Top right
        mPoints[4] = width;
        mPoints[5] = height;

        // Top left
        mPoints[6] = 0.0f;
        mPoints[7] = height;
    }

    /**
     * <p>Updates the bounding box to encompass the Shape's points.</p>
     */
    private void updateBounds()
    {
        final float[] point = new float[2];

        // Iterate over all points to get the bounding box corners
        float originX = Float.MAX_VALUE;
        float originY = Float.MAX_VALUE;
        float cornerX = -Float.MAX_VALUE;
        float cornerY = -Float.MAX_VALUE;
        for (int i = 0, sz = getPointCount(); i < sz; i++) {
            getPoint(i, point);

            // Find top left corner
            originX = Math.min(point[0], originX);
            originY = Math.min(point[1], originY);

            // Find bottom right corner
            cornerX = Math.max(point[0], cornerX);
            cornerY = Math.max(point[1], cornerY);
        }

        // Move bounding box origin to match
        mRect.moveTo(originX, originY);

        // Calc box corner point
        mRect.setWidth(cornerX - originX);
        mRect.setHeight(cornerY - originY);
    }

    /**
     * <p>Gets the coordinates of a point and stores them in a given array where [0] = x and [1] = y.</p>
     *
     * @param index point index.
     * @param point coordinates container of at least length == 2.
     */
    public void getPoint(int index, float[] point)
    {
        // Compute index of point's x
        final int adjusted = index * 2;

        point[0] = mPoints[adjusted];
        point[1] = mPoints[adjusted + 1];
    }

    /**
     * <p>Gets the coordinates of a point and stores them in a given {@link Vector2F}.</p>
     *
     * @param index point index.
     * @param vector coordinates container.
     */
    public void getPoint(int index, Vector2F vector)
    {
        // Compute index of point's x
        final int adjusted = index * 2;

        vector.set(mPoints[adjusted], mPoints[adjusted + 1]);
    }

    /**
     * <p>Adds a point to the Shape using the index of the previous call to this method + 1. Successively adding
     * points with this method is one of the ways of building a polygon.</p>
     *
     * @param x x.
     * @param y y.
     * @throws IllegalStateException if the method's shape cannot be changed, e.g. {@link #isLocked()} returns true.
     */
    public void addPoint(float x, float y)
    {
        if (mLocked) {
            throw new IllegalStateException("Points may not be added after lock()");
        }

        setPoint(mSize++, x, y);
    }

    /**
     * <p>Sets a point's x and y values.</p>
     *
     * @param index point index.
     * @param x x.
     * @param y y.
     * @throws IllegalArgumentException if the index refers to a point beyond the actual number of points.
     */
    public void setPoint(int index, float x, float y)
    {
        // Check if index is beyond valid points
        if (index >= mSize) {
            throw new IllegalArgumentException("Index " + index + " refers beyond size: " + mSize);
        }

        // Compute indices of point's x and y values
        final int pt = index * FLOATS_PER_POINT;
        mPoints[pt] = x;
        mPoints[pt + 1] = y;

        // Move rect origin to encompass point
        final Point3F origin = mRect.getPosition();
        origin.set(Math.min(origin.getX(), x), Math.min(origin.getY(), y));

        // Move rect corner to encompass point
        final Point3F corner = mRect.getCorner();
        origin.set(Math.max(corner.getX(), x), Math.max(corner.getY(), y));
    }

    @Override
    public Point3F getPosition()
    {
        return mRect.getPosition();
    }

    @Override
    public float getX()
    {
        return mRect.getX();
    }

    @Override
    public float getY()
    {
        return mRect.getY();
    }

    @Override
    public float getZ()
    {
        return mRect.getZ();
    }

    @Override
    public void moveTo(float x, float y)
    {
        moveTo(x, y, getZ());
    }

    @Override
    public void moveTo(float x, float y, float z)
    {
        // Move each point in the Shape along
        translatePointsBy(x - getX(), y - getY());

        mRect.moveTo(x, y, z);
    }

    @Override
    public void moveBy(float x, float y)
    {
        moveBy(x, y, 0);
    }

    @Override
    public void moveBy(float x, float y, float z)
    {
        // Move each point in the Shape along
        translatePointsBy(x, y);

        mRect.moveBy(x, y, z);
    }

    @Override
    public void moveToCenter(float x, float y)
    {
        moveTo(x - (getWidth() / 2f), y - (getHeight() / 2f));
    }

    /**
     * <p>Adds an x and y value to each point.</p>
     *
     * @param x x axis shift.
     * @param y y axis shift.
     */
    private void translatePointsBy(float x, float y)
    {
        // Add the translation values to each point's x and y
        final int len = mPoints.length / 2;
        for (int pt = 0; pt < len; pt++) {
            final int xIndex = pt * 2;

            // Update values
            mPoints[xIndex] += x;
            mPoints[xIndex + 1] += y;
        }
    }

    @Override
    public float getWidth()
    {
        return mRect.getWidth();
    }

    @Override
    public void setWidth(float width)
    {
        // Apply new width to points
        scalePoints(width / mRect.getWidth(), 1f, false);
    }

    @Override
    public float getHeight()
    {
        return mRect.getHeight();
    }

    @Override
    public void setHeight(float height)
    {
        // Apply new height to points
        scalePoints(1f, height / mRect.getHeight(), false);
    }

    @Override
    public float getCenterX()
    {
        return getX() + (getWidth() / 2f);
    }

    @Override
    public float getCenterY()
    {
        return getY() + (getHeight() / 2f);
    }

    /**
     * <p>Scales each point's coordinates.</p>
     *
     * @param scaleX scale along x.
     * @param scaleY scale along y.
     */
    private void scalePoints(float scaleX, float scaleY, boolean fromCenter)
    {
        // Get dimensions
        final float w = getWidth();
        final float h = getHeight();

        // Compute half width and height pre scaling
        final float halfWPre = w / 2f;
        final float halfHPre = h / 2f;

        // Compute half width and height post scaling
        final float halfWPost = w * scaleX / 2f;
        final float halfHPost = h * scaleX / 2f;

        // Get origin coords for shifting before scaling
        final float originX = mRect.getX();
        final float originY = mRect.getY();

        float originXPost = Float.MAX_VALUE;
        float originYPost = Float.MAX_VALUE;

        for (int i = 0, sz = getPointCount(); i < sz; i++) {
            getPoint(i, mPt);

            // Shift point towards origin
            float translatedX = mPt[0] - originX;
            float translatedY = mPt[1] - originY;

            // Apply scaling and shift back to old origin
            translatedX = (translatedX * scaleX) + originX;
            translatedY = (translatedY * scaleY) + originY;

            // If desired, shift to same center as before
            if (fromCenter) {
                translatedX -= halfWPost - halfWPre;
                translatedY -= halfHPost - halfHPre;
            }

            setPoint(i, translatedX, translatedY);

            // Track min point for new local origin
            originXPost = Math.min(originXPost, translatedX);
            originYPost = Math.min(originYPost, translatedY);
        }

        // Update bounding box
        mRect.moveTo(originXPost, originYPost);
        mRect.setWidth(w * scaleX);
        mRect.setHeight(h * scaleY);
    }

    @Override
    public double getRotation()
    {
        return mAngle;
    }

    @Override
    public void rotateTo(double angle)
    {
        // Compute angle from 0
        final double angleDiff = angle - mAngle;
        mAngle += angleDiff;

        rotateByRads(angleDiff);
    }

    @Override
    public void rotateBy(double angle)
    {
        mAngle += angle;
        rotateByRads(angle);
    }

    /**
     * <p>Rotates the shape by some angle in radians starting at the current orientation. The bounding box is
     * updated to encompass the shape in its new orientation.</p>
     *
     * @param angle angle in radians.
     */
    private void rotateByRads(double angle)
    {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        // Compute angle trig
        final double sin = Math.sin(angle);
        final double cos = Math.cos(angle);

        // Find difference to get center origin
        final double centerX = (getWidth() / 2f) + getX();
        final double centerY = (getHeight() / 2f) + getY();

        for (int i = 0; i < mSize; i++) {
            getPoint(i, mPt);

            // Compute center position
            final double centeredX = mPt[0] - centerX;
            final double centeredY = mPt[1] - centerY;

            // Apply rotation about center and translate back to original origin
            final double rotatedX = ((centeredX * cos) + (centeredY * -sin)) + centerX;
            final double rotatedY = ((centeredX * sin) + (centeredY * cos)) + centerY;

            minX = Math.min(rotatedX, minX);
            minY = Math.min(rotatedY, minY);

            maxX = Math.max(rotatedX, maxX);
            maxY = Math.max(rotatedY, maxY);

            setPoint(i, (float) rotatedX, (float) rotatedY);
        }

        // Resize bounding box to contain rotated polygon
        mRect.moveTo((float) minX, (float) minY);
        mRect.setWidth((float) (maxX - minX));
        mRect.setHeight((float) (maxY - minY));
    }

    /**
     * <p>Gets the bounding box containing the Shape's polygon.</p>
     *
     * @return bounding box.
     */
    public Rect2D getBounds()
    {
        return mRect;
    }

    /**
     * <p>Checks if a given point is contained within the defined polygon.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if the point is contained.
     */
    public boolean contains(float x, float y)
    {
        // Check each line counter clockwise if pt lies outside the shape
        final int ptCount = getPointCount();
        for (int pt = 0; pt < ptCount; pt++) {
            final int pt0Index = pt * 2;

            // Line seg's 2nd point will be 1st point on final line seg
            final int pt1Index = (pt == ptCount - 1) ? 0 : pt0Index + 2;

            // Check if point exists "above" the line
            final float side = crossProdMag(pt0Index, pt1Index, x, y);
            if (side > 0) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>Calculates the magnitude of the cross product between a line segment and a given point. The line will be
     * translated such that its first point will be based from origin. The cross product is then performed between
     * the shifted second point and the given third point.</p>
     *
     * @param ptIndex0 line segment's first point.
     * @param ptIndex1 line segment's second point.
     * @param x point's x.
     * @param y point's y.
     * @return cross product's magnitude.
     */
    private float crossProdMag(int ptIndex0, int ptIndex1, float x, float y)
    {
        // Get first point
        float x0 = mPoints[ptIndex0];
        float y0 = mPoints[ptIndex0 + 1];

        // Shift second point to base back from origin
        float deltaX = mPoints[ptIndex1] - x0;
        float deltaY = mPoints[ptIndex1 + 1] - y0;

        // Calc z val of cross product between comparing point and line seg
        return (deltaX * (y - y0)) - (deltaY * (x - x0));
    }

    /**
     * <p>Checks if the polygon's structure has been finalized with {@link #lock()} and no more points may be added
     * with {@link #addPoint(float, float)}.</p>
     *
     * @return true if no more points can be added.
     */
    public boolean isLocked()
    {
        return mLocked;
    }

    /**
     * <p>Locks and prevents the Shape from adding more points. Though {@link #setPoint(int, float, float)} may
     * still be used to change existing point's coordinates, calling {@link #addPoint(float, float)} will throw an
     * {@link IllegalStateException}.</p>
     */
    public void lock()
    {
        // Ensure bounding box encompasses points
        if (!mLocked) {
            updateBounds();
        }

        mLocked = true;
    }

    /**
     * <p>Gets the number of points that define the polygon.</p>
     *
     * @return number of points.
     */
    public int getPointCount()
    {
        return mSize;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }
}
