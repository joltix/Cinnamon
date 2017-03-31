package com.cinnamon.utils;


/**
 * <p>Representation of a two dimensional vector of floats.</p>
 */
public class Vector2F
{
    // Number of values
    private static final int SIZE = 2;

    // Values
    private float mX;
    private float mY;

    /**
     * <p>Constructor for a zero Vector2F.</p>
     */
    public Vector2F()
    {
        mX = 0f;
        mY = 0f;
    }

    /**
     * <p>Constructor for a Vector2F with specific x and y values.</p>
     *
     * @param x x.
     * @param y y.
     */
    public Vector2F(float x, float y)
    {
        mX = x;
        mY = y;
    }

    /**
     * <p>Constructor for copying another Vector2F.</p>
     *
     * @param vector vector to copy.
     */
    public Vector2F(Vector2F vector)
    {
        copy(vector);
    }

    /**
     * <p>Copies another vector's values.</p>
     *
     * @param vector vector to copy.
     */
    public void copy(Vector2F vector)
    {
        mX = vector.mX;
        mY = vector.mY;
    }

    /**
     * <p>Gets the x value.</p>
     *
     * @return x.
     */
    public final float getX() {
        return mX;
    }

    /**
     * <p>Sets the x value.</p>
     *
     * @param x x.
     */
    public final void setX(float x)
    {
        mX = x;
    }

    /**
     * <p>Gets the y value.</p>
     *
     * @return y.
     */
    public final float getY() {
        return mY;
    }

    /**
     * <p>Sets the y value.</p>
     *
     * @param y y.
     */
    public final void setY(float y)
    {
        mY = y;
    }

    /**
     * <p>Gets the vector's x and y values as a {@link Point2F}.</p>
     *
     * @return point.
     */
    public Point2F getPoint()
    {
        return new Point2F(mX, mY);
    }

    /**
     * <p>Sets the x and y values.</p>
     *
     * @param x x.
     * @param y y.
     */
    public final void set(float x, float y)
    {
        mX = x;
        mY = y;
    }

    /**
     * <p>Performs the dot product, also known as scalar product, operation with another vector.</p>
     *
     * @param vector other Vector2F.
     * @return dot product.
     */
    public float dotProduct(Vector2F vector)
    {
        return (mX * vector.mX) + (mY * vector.mY);
    }

    /**
     * <p>Multiplies the vector with a scalar.</p>
     *
     * @param scalar scalar.
     * @return vector.
     */
    public Vector2F multiply(float scalar)
    {
        mX *= scalar;
        mY *= scalar;
        return this;
    }

    /**
     * <p>Divides the vector with a scalar. This is equivalent to calling {@link #multiply(float)} and passing in
     * <i>"1f / scalar"</i>.</p>
     *
     * @param scalar scalar.
     * @return vector.
     */
    public Vector2F divide(float scalar)
    {
        mX /= scalar;
        mY /= scalar;
        return this;
    }

    /**
     * <p>Adds another vector.</p>
     *
     * @param vector other vector.
     * @return calling vector.
     */
    public Vector2F add(Vector2F vector)
    {
        mX += vector.mX;
        mY += vector.mY;
        return this;
    }

    /**
     * <p>Adds an x and y value.</p>
     *
     * @param x x.
     * @param y y.
     * @return vector.
     */
    public Vector2F add(float x, float y)
    {
        mX += x;
        mY += y;
        return this;
    }

    /**
     * <p>Subtracts another vector.</p>
     *
     * @param vector other vector.
     * @return calling vector.
     */
    public Vector2F subtract(Vector2F vector)
    {
        mX -= vector.mX;
        mY -= vector.mY;
        return this;
    }

    /**
     * <p>Subtracts an x and y value.</p>
     *
     * @param x x.
     * @param y y.
     * @return vector.
     */
    public Vector2F subtract(float x, float y)
    {
        mX -= x;
        mY -= y;
        return this;
    }

    /**
     * <p>Gets a unit vector representing the calling vector's direction.</p>
     *
     * @return unit vector.
     */
    public Vector2F getUnit()
    {
        return computeUnitVector(new Vector2F(this));
    }

    /**
     * <p>Transforms a given vector into a unit vector representing the direction of the calling vector.</p>
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
     *
     * @param unitVector
     * @return unit vector.
     */
    private Vector2F computeUnitVector(Vector2F unitVector)
    {
        final float magnitude = magnitude();
        if (Point2F.isEqual(magnitude, 0f)) {
            unitVector.set(0f, 0f);
        } else {
            unitVector.divide(magnitude);
        }
        return unitVector;
    }

    /**
     * <p>Negates the direction.</p>
     */
    public final void negate()
    {
        multiply(-1f);
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
     * <p>Transforms a given container vector whose direction is perpendicular to the calling vector.</p>
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
     * <p>Transforms the calling vector into a vector perpendicular to the original direction.</p>
     *
     * @param right true to make a right-hand normal, false for left.
     */
    public void normal(boolean right)
    {
        Vector2F.makePerpendicular(this, right);
    }

    /**
     * <p>Transforms the calling vector into a vector perpendicular to the original direction.</p>
     *
     * @param container vector to become perpendicular.
     * @param right true to make a right-hand normal, false for left.
     */
    private static Vector2F makePerpendicular(Vector2F container, boolean right)
    {
        // Pull vector point
        final float x = container.mX;
        final float y = container.mY;

        // Flip to the right
        if (right) {
            container.mX = -y;
            container.mY = x;
        } else {

            // Flip to left perpendicular
            container.mX = y;
            container.mY = -x;
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
        return (float) Math.sqrt((mX * mX) + (mY * mY));
    }

    /**
     * <p>Gets the number of values.</p>
     *
     * @return value count.
     */
    public int size()
    {
        return SIZE;
    }

    /**
     * <p>Checks if the {@link Vector2F} is the zero vector (magnitude is 0). This method uses
     * {@link Point2F#PRECISION} for fp comparison.</p>
     *
     * @return true if zero vector.
     */
    public boolean isZero()
    {
        return Point2F.isEqual(mX, 0f) && Point2F.isEqual(mY, 0f);
    }

    /**
     * <p>Gets a {@link Vector2F} that forms a specific angle counterclockwise from the direction (1,0).</p>
     *
     * @param radians angle.
     * @param magnitude magnitude.
     * @return vector.
     */
    public static Vector2F fromAngle(double radians, double magnitude)
    {
        final double x = (Math.sin(radians) * magnitude);
        final double y = (Math.cos(radians) * magnitude);
        return new Vector2F((float) x, (float) y);
    }

    /**
     * <p>Gets the angle between 0 (direction vector (1,0)) and the vector's (x,y) in radians.</p>
     *
     * @return angle in radians.
     */
    public final double getAngle()
    {
        return Math.atan2(mY, mX);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("(");
        builder.append(mX);
        builder.append(",");
        builder.append(mY);
        return builder.substring(0, builder.length() - 1) + ") [mag=" + magnitude() + "]";
    }
}
