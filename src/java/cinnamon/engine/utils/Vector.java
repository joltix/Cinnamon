package cinnamon.engine.utils;


/**
 * <p>Three dimensional vector.</p>
 *
 * <p>Some arithmetic operations can be chained, as shown below.</p>
 *
 * <pre>
 *     <code>
 *     final Vector vector = new Vector(2f, 2f, 0f);
 *
 *     // Final vector = (1f, 1f, -0f)
 *     vector.subtract(new Vector(5f, 5f, 0f)).add(new Vector(2f, 2f, 0f)).multiply(-1f);
 *     </code>
 * </pre>
 */
public final class Vector implements Repositionable, Copier<Vector>
{
    private float mX;
    private float mY;
    private float mZ;

    /**
     * <p>Constructs a <tt>Vector</tt> whose components are all zero.</p>
     */
    public Vector()
    {
        this(0f, 0f, 0f);
    }

    /**
     * <p>Constructs a <tt>Vector</tt> with the given components.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public Vector(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    /**
     * <p>Constructs a <tt>Vector</tt> with the same components as another.</p>
     *
     * @param vector to copy.
     * @throws NullPointerException if the given vector is null.
     */
    public Vector(Vector vector)
    {
        copy(vector);
    }

    /**
     * <p>Copies all components from the given vector.</p>
     *
     * @throws NullPointerException if the given vector is null.
     */
    @Override
    public void copy(Vector vector)
    {
        checkNull( vector, "copy");

        mX =  vector.mX;
        mY =  vector.mY;
        mZ =  vector.mZ;
    }

    /**
     * <p>Computes the dot product with another vector.</p>
     *
     * @param vector other.
     * @return dot product.
     * @throws NullPointerException if the given vector is null.
     */
    public float dot(Vector vector)
    {
        checkNull(vector, "dot");

        return (mX * vector.mX) + (mY * vector.mY) + (mZ * vector.mZ);
    }

    /**
     * <p>Computes the cross product with another vector.</p>
     *
     * @param vector other.
     * @return calling vector.
     * @throws NullPointerException if the given vector is null.
     */
    public Vector cross(Vector vector)
    {
        checkNull(vector, "cross");

        final float x = (mY * vector.mZ) - (mZ * vector.mY);
        final float y = (mZ * vector.mX) - (mX * vector.mZ);
        final float z = (mX * vector.mY) - (mY * vector.mX);

        mX = x;
        mY = y;
        mZ = z;

        return this;
    }

    /**
     * <p>Adds another vector's components.</p>
     *
     * @param vector other.
     * @return calling vector.
     * @throws NullPointerException if the given vector is null.
     */
    public Vector add(Vector vector)
    {
        checkNull(vector, "add");

        mX += vector.mX;
        mY += vector.mY;
        mZ += vector.mZ;
        return this;
    }

    /**
     * <p>Subtracts another vector's components.</p>
     *
     * @param vector other.
     * @return calling vector.
     * @throws NullPointerException if the given vector is null.
     */
    public Vector subtract(Vector vector)
    {
        checkNull(vector, "subtract");

        mX -= vector.mX;
        mY -= vector.mY;
        mZ -= vector.mZ;
        return this;
    }

    /**
     * <p>Multiplies all components by a scalar.</p>
     *
     * @param scalar scalar.
     * @return calling vector.
     */
    public Vector multiply(float scalar)
    {
        mX *= scalar;
        mY *= scalar;
        mZ *= scalar;
        return this;
    }

    /**
     * <p>Divides all components by a scalar.</p>
     *
     * @param scalar scalar.
     * @return calling vector.
     */
    public Vector divide(float scalar)
    {
        mX /= scalar;
        mY /= scalar;
        mZ /= scalar;
        return this;
    }

    /**
     * <p>Transforms the calling vector into a unit vector. If the vector's magnitude is exactly zero, this method does
     * nothing.</p>
     *
     * @return calling vector.
     */
    public Vector normalize()
    {
        final float magnitude = magnitude();

        if (magnitude != 0f) {
            divide(magnitude);
        }

        return this;
    }

    /**
     * <p>Negates the direction.</p>
     *
     * @return calling vector.
     */
    public Vector negate()
    {
        return multiply(-1f);
    }

    /**
     * <p>Computes the magnitude.</p>
     *
     * @return magnitude.
     */
    public float magnitude()
    {
        return (float) Math.sqrt((mX * mX) + (mY * mY) + (mZ * mZ));
    }

    @Override
    public void setPosition(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    @Override
    public float getX() {
        return mX;
    }

    @Override
    public void setX(float x)
    {
        mX = x;
    }

    @Override
    public float getY() {
        return mY;
    }

    @Override
    public void setY(float y)
    {
        mY = y;
    }

    @Override
    public float getZ()
    {
        return mZ;
    }

    @Override
    public void setZ(float z)
    {
        mZ = z;
    }

    @Override
    public void addX(float x)
    {
        mX += x;
    }

    @Override
    public void addY(float y)
    {
        mY += y;
    }

    @Override
    public void addZ(float z)
    {
        mZ += z;
    }

    /**
     * <p>Checks if all components are exactly zero.</p>
     *
     * @return true if the zero vector.
     */
    public boolean isZero()
    {
        return mX == 0f && mY == 0f && mZ == 0f;
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + ((Float) mX).hashCode();
        hash = 31 * hash + ((Float) mY).hashCode();
        return 31 * hash + ((Float) mZ).hashCode();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Comparison between two coordinates of {@link Float#NaN} are treated as true and no distinction is made
     * between positive and negative zeros. Floating point comparison is exact (delta of 0).</p>
     *
     * @param obj the vector with which to compare.
     * @return true if the given object is a vector and both vectors refer to the exact same coordinates.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        } else if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        final Vector vector = (Vector) obj;

        // Compare components (account for corresponding being NaN)
        boolean sameComps= (mX == vector.mX || (Float.isNaN(mX) && Float.isNaN(vector.mX)));
        sameComps = sameComps && (mY == vector.mY || (Float.isNaN(mY) && Float.isNaN(vector.mY)));
        sameComps = sameComps && (mZ == vector.mZ || (Float.isNaN(mZ) && Float.isNaN(vector.mZ)));

        return sameComps;
    }

    @Override
    public String toString() {
        return "(" + mX + "," + mY + "," + mZ + ")[mag=" + magnitude() + "]";
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Use the copy constructor instead");
    }

    /**
     * <p>Throws a <tt>NullPointerException</tt> if the given vector is null. The Exception's message follows the format
     * 'Cannot &lt;action&gt; null vector' where <i>&lt;action&gt;</i> is replaced by the corresponding argument.</p>
     *
     * @param vector to check.
     * @param action attempted operation.
     * @throws NullPointerException if the given vector is null.
     */
    private void checkNull(Vector vector, String action)
    {
        if (vector == null) {
            throw new NullPointerException("Cannot " + action + " null vector");
        }
    }
}
