package com.cinnamon.object;

import com.cinnamon.system.ComponentFactory;
import com.cinnamon.utils.AxisAlignedRect;
import com.cinnamon.utils.Rect2D;
import com.cinnamon.utils.Shape;
import com.cinnamon.utils.Vector2F;

/**
 * <p>
 *     BodyComponent represents a 2D collision polygon {@link GObject}.
 * </p>
 *
 * <p>
 *     Collision detection implementation consists of the separating axis
 *     theorem for fine-grain collision checking and an axis aligned bounding
 *     box for broad checks.
 * </p>
 *
 *
 */
public abstract class BodyComponent extends ComponentFactory.Component
        implements Positional, Dimensional
{
    // Polygon for fine-grain collision checking
    private Shape mShape;

    // Bounding box
    private final Rect2D mBoundary = new AxisAlignedRect(0, 0);

    // Position offset
    private float mOffsetX = 0f;
    private float mOffsetY = 0f;

    // Toggle to allow selection
    private boolean mSelectable = true;

    /**
     * <p>Checks whether or not another BodyComponent is touching.</p>
     *
     * @param component BodyComponent.
     * @return true if the BodyComponents touch.
     */
    public boolean contains(BodyComponent component)
    {
        if (!mBoundary.intersects(component.getBounds())) {
            return false;
        }

        return BodyComponent.contains(mShape, component.getShape());
    }
    /**
     * <p>Performs collision checking between two {@link Shape}s using the
     * separating axis theorem.</p>
     *
     * @param shape Shape.
     * @param other other Shape.
     * @return true if the two Shapes intersect.
     */
    private static boolean contains(Shape shape, Shape other)
    {
        final Vector2F axis = new Vector2F();
        final float[] pt0 = new float[2];
        final float[] pt1 = new float[2];

        float[] minMax0;
        float[] minMax1;

        // Find min/max projected points of first shape
        for (int i = 0, sz = shape.getPointCount() - 1; i < sz; i += 2) {
            shape.getPoint(i, pt0);
            shape.getPoint(i + 1, pt1);

            // Compute separation axis
            axis.moveTo(pt1[0] - pt0[0], pt1[1] - pt0[1]);

            // Make it a unit vector and transform perpendicular
            axis.normalize();
            axis.normal(false);

            // Find the min/max projections of each shape onto the axis
            minMax0 = BodyComponent.findMinMaxProjections(shape, axis);
            minMax1 = BodyComponent.findMinMaxProjections(other, axis);

            // Compare shape min/max boundaries if not overlapping
            if (minMax0[0] > minMax1[1] || minMax0[1] < minMax1[0]) {
                return false;
            }
        }

        // Could not find gap so shapes intersect
        return true;
    }

    /**
     * <p>Finds the minimum and maximum magnitudes of projected vectors of a
     * given {@link Shape} when the Shape is projected onto a
     * {@link Vector2F}.</p>
     *
     * @param shape Shape to project.
     * @param vector axis to project on to.
     * @return float[] of size 2.
     */
    private static float[] findMinMaxProjections(Shape shape, Vector2F vector)
    {
        final Vector2F tempPt0 = new Vector2F();
        final Vector2F tempPt1 = new Vector2F();
        final float[] pt = new float[2];

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        // Project each point onto the vector
        for (int i = 0, sz = shape.getPointCount(); i < sz; i++) {

            // Convert point to vector2D for methods
            shape.getPoint(i, pt);
            tempPt0.moveTo(pt[0], pt[1]);

            // Compute projection constant and track min/max
            final float constant = vector.dotProduct(tempPt0);
            if (constant <= min) {
                min = constant;
            }
            if (constant >= max) {
                max = constant;
            }
        }

        return new float[] {min, max};
    }

    /**
     * <p>Checks whether or not an (x,y) point is found within the
     * BodyComponent.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if the point exists inside.
     */
    public boolean contains(float x, float y)
    {
        // Check if point does not exist within the bounding box
        if (!mBoundary.contains(x, y)) {
            return false;
        }

        // Check if point is within shape
        return mShape.contains(x, y);
    }
    /**
     * <p>Updates the bounding box to encompass the BodyComponent's Shape.</p>
     *
     * TODO: Update Rect2D bounding box to contain circle shape
     */
    private void updateBounds()
    {
        final float[] point = new float[2];

        // Circle shapes need custom bound format since no edges
        if (mShape.getType() == Shape.Type.CIRCLE) {

            // TODO: Decide on a format for defining circle types before
            // measuring bounding boxes

        } else {
            // Iterate over all points to get the bounding box corners

            float originX = 0;
            float originY = 0;
            float cornerX = 0;
            float cornerY = 0;
            for (int i = 0, count = mShape.getPointCount(); i < count; i++) {
                mShape.getPoint(i, point);

                // Find top left corner
                originX = Math.min(point[0], originX);
                originY = Math.min(point[1], originY);

                // Find bottom right corner
                cornerX = Math.max(point[0], cornerX);
                cornerY = Math.max(point[1], cornerY);
            }

            // Move bounding box origin to match
            mBoundary.moveTo(originX, originY);

            // Calc box corner point
            mBoundary.setWidth(cornerX - originX);
            mBoundary.setHeight(cornerY - originY);
        }
    }

    /**
     * <p>Gets the {@link Shape} used for fine-grain collision detection.</p>
     *
     * @return Shape.
     */
    public Shape getShape()
    {
        return mShape;
    }

    /**
     * <p>Sets the {@link Shape} to use for fine-grain collision detection.</p>
     *
     * @param shape Shape.
     */
    public void setShape(Shape shape)
    {
        mShape = new Shape(shape);
        mShape.lock();
        updateBounds();
    }
    /**
     * <p>Gets the {@link Rect2D} to use as a bounding box for broad
     * collision detection.</p>
     *
     * @return bounding box.
     */
    public Rect2D getBounds()
    {
        return mBoundary;
    }

    @Override
    public float getWidth()
    {
        return mBoundary.getWidth();
    }

    @Override
    public void setWidth(float width)
    {
        mBoundary.setWidth(width);
    }

    @Override
    public float getHeight()
    {
        return mBoundary.getHeight();
    }

    @Override
    public void setHeight(float height)
    {
        mBoundary.setHeight(height);
    }

    @Override
    public void setOffset(float x, float y, float z)
    {
        mOffsetX = x;
        mOffsetY = y;
    }

    @Override
    public float getOffsetX()
    {
        return mOffsetX;
    }

    @Override
    public float getOffsetY()
    {
        return mOffsetY;
    }

    /**
     * <p>This method always returns 0.</p>
     *
     * @return 0.
     */
    @Override
    public float getOffsetZ()
    {
        return 0;
    }

    @Override
    public void setOffset(float x, float y)
    {
        setOffset(x, y, 0);
    }

    @Override
    public float getX()
    {
        return mBoundary.getX();
    }

    @Override
    public float getY()
    {
        return mBoundary.getY();
    }

    @Override
    public float getZ()
    {
        return mBoundary.getZ();
    }

    @Override
    public void moveTo(float x, float y, float z)
    {
        // Compute coord shift for all points
        float xShift = x - mBoundary.getX() + mOffsetX;
        float yShift = y - mBoundary.getY() + mOffsetY;

        // Move bounding box and shape to match instance's position
        mBoundary.moveTo(x, y);
        mShape.translateBy(xShift, yShift);
    }

    @Override
    public void moveTo(float x, float y)
    {
        moveTo(x, y, mBoundary.getZ());
    }

    /**
     * <p>Checks whether or not the BodyComponent can be selected.</p>
     *
     * @return true if selection is allowed.
     */
    public boolean isSelectable()
    {
        return mSelectable;
    }

    /**
     * <p>Sets whether or not the BodyComponent can be selected.</p>
     *
     * @param enable true to allow selection.
     */
    public void setSelectable(boolean enable)
    {
        mSelectable = enable;
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
        return "[" + mShape + ",(" + mBoundary.getX() + "," + mBoundary.getY() +
                "),(" + mBoundary.getX() + mBoundary.getWidth() + "," + mBoundary.getY
                () +
                mBoundary
                        .getHeight() + ")]";
    }
}
