package com.cinnamon.utils;

import com.cinnamon.object.BodyComponent;
import com.cinnamon.system.ComponentFactory;

import static com.cinnamon.utils.Shape.Type.FREEFORM;

/**
 * <p>Shapes define polygons to be used in building {@link BodyComponent}s
 * for collision operations.</p>
 *
 *
 */
public class Shape extends ComponentFactory.Component
{
    /**
     * <p>Categories for {@link Shape}s to help respond accordingly when
     * different pairs of Shapes interact.</p>
     */
    public enum Type
    {
        /**
         * <p>Describes {@link Shape} as a rectangle.</p>
         */
        RECTANGLE,

        /**
         * <p>Describes {@link Shape} as a circle.</p>
         */
        CIRCLE,

        /**
         * <p>Describes {@link Shape} as a custom polygon.</p>
         */
        FREEFORM
    }

    // Shape points details
    private static final int FLOATS_PER_POINT = 2;
    private static final int POINTS_COUNT_RECTANGLE = 4;
    private static final int POINTS_COUNT_CIRCLE = 1;

    // Shape type
    private final Type mType;
    // Shape edit status
    private boolean mLocked = false;

    // Points defining edges
    private final float[] mPoints;
    // Point index when using setPoint();
    private int mPtIndex = 0;

    /**
     * <p>Constructs a Shape of type {@link Type#FREEFORM} prepared to
     * define its polygon with a specified number of points.</p>
     *
     * @param pointCount number of points defining the Shape.
     */
    public Shape(int pointCount)
    {
        mType = FREEFORM;
        mPoints = new float[pointCount];
    }

    /**
     * <p>Constructs a Shape of a given {@link Type} and dimensions.</p>
     *
     * @param type either {@link Type#RECTANGLE}, {@link Type#CIRCLE}, or
     * {@link Type#FREEFORM}.
     * @param width width.
     * @param height height.
     */
    public Shape(Type type, float width, float height)
    {
        mType = type;

        switch (type) {
            case RECTANGLE:
                mPoints = new float[POINTS_COUNT_RECTANGLE * FLOATS_PER_POINT];
                buildRectangle(width, height);
                mLocked = true;
                break;

            case CIRCLE:
                mPoints = new float[POINTS_COUNT_CIRCLE * FLOATS_PER_POINT];
                buildCircle(width);
                mLocked = true;
                break;

            case FREEFORM:
                throw new IllegalArgumentException("Use Shape(int) for " +
                        "freeform shapes");

            default: throw new IllegalArgumentException("Unrecognized shape " +
                    "type: " + type);
        }
    }

    /**
     * <p>Constructs a Shape based off of another Shape. All points,
     * {@link Type}, and lock status are copied.</p>
     *
     * @param shape other Shape.
     */
    public Shape(Shape shape)
    {
        mType = shape.mType;
        mPoints = shape.mPoints.clone();
        mLocked = shape.mLocked;
    }

    /**
     * <p>Defines the first point as the top left corner of the rectangle.
     * The rest of the points are defined clockwise from the first.</p>
     *
     * @param width width.
     * @param height height.
     */
    private void buildRectangle(float width, float height)
    {
        // Top left
        mPoints[0] = 0f;
        mPoints[1] = 0f;

        // Top right
        mPoints[2] = width;
        mPoints[3] = 0;

        // Bottom right
        mPoints[4] = width;
        mPoints[5] = height;

        // Bottom left
        mPoints[6] = 0.0f;
        mPoints[7] = height;
    }

    /**
     * <p>Defines the outer edge of the circle as the first point.</p>
     *
     * @param diameter width of the circle.
     */
    private void buildCircle(float diameter)
    {
        mPoints[0] = diameter;
        mPoints[1] = 0.0f;
    }

    /**
     * <p>Gets the x and y coordinates of a point and stores them in a given
     * array.</p>
     *
     * @param index point index.
     * @param point coordinates container of at least length == 2.
     */
    public void getPoint(int index, float[] point)
    {
        final int adjusted = index * 2;
        point[0] = mPoints[adjusted];
        point[1] = mPoints[adjusted + 1];
    }

    /**
     * <p>Adds a point to the Shape using the method's previous call's point
     * index + 1.</p>
     *
     * @param x x.
     * @param y y.
     * @throws IllegalStateException if the method's shape cannot be changed.
     * E.g. {@link #isLocked()} returns true.
     * @throws IllegalArgumentException if the given point's index is < 0 or
     * greater than the Shape's point capacity defined at construction.
     */
    public void addPoint(float x, float y)
    {
        setPoint(x, y, mPtIndex++);
    }

    /**
     * <p>Sets the ending point of a specific edge.</p>
     *
     * @param x x.
     * @param y y.
     * @param index edge.
     * @throws IllegalStateException if the method's shape cannot be changed.
     * E.g. {@link #isLocked()} returns true.
     * @throws IllegalArgumentException if the given point's index is < 0 or
     * greater than the Shape's point capacity defined at construction.
     */
    public void setPoint(float x, float y, int index)
    {
        if (mLocked) {
            throw new IllegalStateException("Shape may not be changed after " +
                    "finalized");
        }

        if ((index + 1) >= mPoints.length) {
            throw new IllegalArgumentException("Shape point capacity reached: "
                    + (mPoints.length / 2));
        }

        if (index < 0) {
            throw new IllegalArgumentException("Point index must be > 0");
        }

        final int pt = index * FLOATS_PER_POINT;
        mPoints[pt] = x;
        mPoints[pt + 1] = y;
    }

    /**
     * <p>Moves the Shape's points by an x and y value.</p>
     *
     * @param x x axis shift.
     * @param y y axis shift.
     */
    public void translateBy(float x, float y)
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

    /**
     * <p>Checks if a given point is contained within the defined polygon.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if the point is contained.
     */
    public boolean contains(float x, float y)
    {
        // Check each line clockwise if pt lies outside the shape
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
     * <p>Calculates the magnitude of the cross product between a line
     * segment and a given point. The line will be translated such that its
     * first point will be based from origin. The cross product is then
     * performed between the shifted second point and the given third point.</p>
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
     * <p>Checks whether or not {@link #addPoint(float, float)} and
     * {@link #setPoint(float, float, int)} may be used to change the Shape.</p>
     *
     * @return true if the Shape cannot be changed.
     */
    public boolean isLocked()
    {
        return mLocked;
    }

    /**
     * <p>Locks and prevents the Shape from changing its polygon through.</p>
     *
     * <p>After this method is called, point altering methods such as
     * {@link #setPoint(float, float, int)} will throw
     * {@link IllegalStateException}.</p>
     */
    public void lock()
    {
        mLocked = true;
    }

    /**
     * <p>Gets the Shape {@link Type}.</p>
     *
     * @return either {@link Type#RECTANGLE}, {@link Type#CIRCLE}, or
     * {@link Type#FREEFORM}.
     */
    public Type getType()
    {
        return mType;
    }

    /**
     * <p>Checks whether or not the Shape is of the specified Type.</p>
     *
     * @param type either {@link Type#RECTANGLE}, {@link Type#CIRCLE}, or
     * {@link Type#FREEFORM}.
     * @return true if the Shape is of the Type.
     */
    public boolean isType(Type type)
    {
        return mType == type;
    }

    /**
     * <p>Gets the number of points that define the Shape.</p>
     *
     * @return number of points.
     */
    public int getPointCount()
    {
        return mPoints.length / 2;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor " +
                "instead");
    }

    @Override
    public String toString()
    {
        return "[" + mType.name() + "," + (mPoints.length / 2)
                + " points,locked(" + mLocked + ")]";
    }
}
