package com.cinnamon.utils;

import com.cinnamon.object.Positional;

/**
 * <p>Representation of a two dimension vector of floats.</p>
 *
 *
 */
public class Vector2F implements Positional
{
    // Number of elements
    private static final int SIZE = 2;

    // Element indices
    private static final int X = 0;
    private static final int Y = 1;

    // Elements
    private final float[] mValues = new float[SIZE];

    /**
     * <p>Constructor for a Vector2F.</p>
     */
    public Vector2F()
    {

    }

    /**
     * <p>Constructor for copying another Vector2F.</p>
     *
     * @param vector vector to copy.
     */
    public Vector2F(Vector2F vector)
    {
        mValues[X] = vector.mValues[X];
        mValues[Y] = vector.mValues[Y];
    }

    /**
     * <p>Copies another Vector2F's values.</p>
     *
     * @param vector vector to copy.
     */
    public void copy(Vector2F vector)
    {
        mValues[X] = vector.mValues[X];
        mValues[Y] = vector.mValues[Y];
    }

    @Override
    public float getX() {
        return mValues[X];
    }

    @Override
    public float getY() {
        return mValues[Y];
    }

    @Override
    public float getZ() {
        return 0;
    }

    @Override
    public void moveTo(float x, float y, float z) {
        moveTo(x, y, 0);
    }

    @Override
    public void moveTo(float x, float y) {
        mValues[X] = x;
        mValues[Y] = y;
    }

    /**
     * <p>This method has no effect.</p>
     *
     * @param x x offset.
     * @param y y offset.
     */
    @Override
    public void setOffset(float x, float y) {

    }

    /**
     * <p>This method has no effect.</p>
     *
     * @param x x offset.
     * @param y y offset.
     * @param z z offset.
     */
    @Override
    public void setOffset(float x, float y, float z)
    {

    }

    /**
     * <p>This method always returns 0.</p>
     *
     * @return 0.
     */
    @Override
    public float getOffsetX() {
        return 0;
    }

    /**
     * <p>This method always returns 0.</p>
     *
     * @return 0.
     */
    @Override
    public float getOffsetY() {
        return 0;
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

    /**
     * <p>Performs the dot product operation with another Vector2F.</p>
     *
     * @param vector other Vector2F.
     * @return dot product.
     */
    public float dotProduct(Vector2F vector)
    {
        // Multiply corresponding elements and sum all
        float dotProd = 0f;
        for (int i = 0, len = size(); i < len; i++) {
            dotProd += (mValues[i] * vector.mValues[i]);
        }

        return dotProd;
    }

    /**
     * <p>Multiplies the Vector2F with a scalar.</p>
     *
     * @param scalar scalar.
     */
    public void multiply(float scalar)
    {
        // Multiply scalar against each element
        for (int i = 0; i < mValues.length; i++) {
            mValues[i] *= scalar;
        }
    }

    /**
     * <p>Gets a unit vector representing the direction of the Vector2F.</p>
     *
     * @return unit vector.
     */
    public Vector2F getUnit()
    {
        return computeUnitVector(new Vector2F(this));
    }

    /**
     * <p>Transforms a given Vector2F into a unit vector representing the
     * direction of the calling vector.</p>
     *
     * @param container Vector2F to transform to unit vector.
     * @return unit vector.
     */
    public Vector2F getUnit(Vector2F container)
    {
        container.copy(this);
        return computeUnitVector(container);
    }

    /**
     * <p>Transforms the calling vector into a unit vector.</p>
     */
    public void normalize()
    {
        computeUnitVector(this);
    }

    /**
     * <p>Transforms a Vector2F into a unit vector.</p>
     * @param unitVector
     * @return
     */
    private Vector2F computeUnitVector(Vector2F unitVector)
    {
        final float magnitude = magnitude();
        unitVector.multiply((1 / magnitude));
        return unitVector;
    }

    /**
     * <p>Gets a vector whose direction is perpendicular to the calling
     * vector.</p>
     *
     * @param right true to create a right-hand normal, false for left.
     * @return normal.
     */
    public Vector2F getNormal(boolean right)
    {
        return makePerpendicular(new Vector2F(this), right);
    }

    /**
     * <p>Transforms a given container vector whose direction is
     * perpendicular to the calling vector.</p>
     *
     * @param container vector to become perpendicular.
     * @param right true to create a right-hand normal, false for left.
     * @return normal.
     */
    public Vector2F getNormal(Vector2F container, boolean right)
    {
        return Vector2F.makePerpendicular(container, right);
    }

    /**
     * <p>Transforms the calling vector into a vector perpendicular to the
     * original direction.</p>
     *
     * @param right true to make a right-hand normal, false for left.
     */
    public void normal(boolean right)
    {
        Vector2F.makePerpendicular(this, right);
    }

    /**
     * <p>Transforms the calling vector into a vector perpendicular to the
     * original direction.</p>
     *
     * @param container vector to become perpendicular.
     * @param right true to make a right-hand normal, false for left.
     */
    private static Vector2F makePerpendicular(Vector2F container, boolean right)
    {
        // Pull vector point
        final float x = container.mValues[X];
        final float y = container.mValues[Y];

        // Flip to the right
        if (right) {
            container.mValues[X] = -y;
            container.mValues[Y] = x;
        } else {

            // Flip to left perpendicular
            container.mValues[X] = y;
            container.mValues[Y] = -x;
        }

        return container;
    }

    /**
     * <p>Computes the magnitude.</p>
     *
     * @return magnitude.
     */
    public float magnitude()
    {
        // Square each element and sum
        double toSqrt = 0d;
        for (int i = 0; i < mValues.length; i++) {
            final double val = mValues[i];
            toSqrt += (val * val);
        }

        // Final square root op for magnitude
        return (float) Math.sqrt(toSqrt);
    }

    /**
     * <p>Gets the number of elements.</p>
     *
     * @return element count.
     */
    public int size()
    {
        return SIZE;
    }

    /**
     * <p>Computes a vector that is the projection of the caller onto
     * another vector.</p>
     *
     * @param vector vector to project on to.
     * @return projected vector.
     */
    public Vector2F projectOn(Vector2F vector)
    {
        return projectOn(new Vector2F(vector), new Vector2F());
    }

    /**
     * <p>Computes a vector that is the projection of the caller onto
     * another vector.</p>
     *
     * @param vector vector to project on to.
     * @param container vector to use as projected vector.
     * @return projected vector.
     */
    public Vector2F projectOn(Vector2F vector, Vector2F container)
    {
        container.copy(vector);

        // Dot product against itself
        final float selfDot = container.dotProduct(container);

        // Dot product against calling vector
        final float dot = dotProduct(container) / selfDot;
        container.multiply(dot);

        return container;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (double val : mValues) {
            builder.append(val);
            builder.append(",");
        }
        return builder.substring(0, builder.length() - 1) + ") [mag=" + magnitude() + "]";
    }
}
