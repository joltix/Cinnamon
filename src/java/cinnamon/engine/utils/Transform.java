package cinnamon.engine.utils;

/**
 * Position and scale container. {@code Transform} objects hold an (x,y,z) position and multipliers for width,
 * height, and depth.
 */
public final class Transform implements Position
{
    private float mScaleX = 1f;

    private float mScaleY = 1f;

    private float mScaleZ = 1f;

    private float mX = 0f;
    
    private float mY = 0f;

    private float mZ = 0f;

    /**
     * Constructs a {@code Transform}.
     */
    public Transform() { }

    /**
     * Sets the scale.
     * 
     * @param x x scale.
     * @param y y scale.
     * @param z z scale.
     * @throws IllegalArgumentException if {@code x}, {@code y}, or {@code z} is {@code 0}, {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setScale(float x, float y, float z)
    {
        setScaleX(x);
        setScaleY(y);
        setScaleZ(z);
    }

    /**
     * Gets the position.
     *
     * @return point.
     */
    public Point getPosition()
    {
        return new Point(mX, mY, mZ);
    }

    /**
     * Sets the position.
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    public void setPosition(float x, float y, float z)
    {
        mX = x;
        mY = y;
        mZ = z;
    }

    /**
     * Gets the scale along the x axis.
     *
     * @return scale.
     */
    public float getScaleX()
    {
        return mScaleX;
    }

    /**
     * Sets the scale along the x axis.
     *
     * @param scale scale.
     * @throws IllegalArgumentException if {@code scale} is {@code 0}, {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setScaleX(float scale)
    {
        checkScaleNotNaN("X scale", scale);
        checkScaleNotZero("X scale", scale);

        mScaleX = scale;
    }

    /**
     * Gets the scale along the y axis.
     *
     * @return scale.
     */
    public float getScaleY()
    {
        return mScaleY;
    }

    /**
     * Sets the scale along the y axis.
     *
     * @param scale scale.
     * @throws IllegalArgumentException if {@code scale} is {@code 0}, {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setScaleY(float scale)
    {
        checkScaleNotNaN("Y scale", scale);
        checkScaleNotZero("Y scale", scale);

        mScaleY = scale;
    }

    /**
     * Gets the scale along the z axis.
     *
     * @return scale.
     */
    public float getScaleZ()
    {
        return mScaleZ;
    }

    /**
     * Sets the scale along the z axis.
     *
     * @param scale scale.
     * @throws IllegalArgumentException if {@code scale} is {@code 0}, {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     */
    public void setScaleZ(float scale)
    {
        checkScaleNotNaN("Z scale", scale);
        checkScaleNotZero("Z scale", scale);

        mScaleZ = scale;
    }

    @Override
    public float getX()
    {
        return mX;
    }

    @Override
    public float getY()
    {
        return mY;
    }

    @Override
    public float getZ()
    {
        return mZ;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null || object.getClass() != Transform.class) {
            return false;
        } else if (object == this) {
            return true;
        }

        final Transform other = (Transform) object;

        return mX == other.getX() && mY == other.getY() && mZ == other.getZ() && 
            mScaleX == other.getScaleX() && mScaleY == other.getScaleY() && mScaleZ == other.getScaleZ();
    }

    @Override
    public int hashCode()
    {
        int hash = 17 * 31;

        hash = 31 * hash + Float.hashCode(mX);
        hash = 31 * hash + Float.hashCode(mY);
        hash = 31 * hash + Float.hashCode(mZ);
        
        hash = 31 * hash + Float.hashCode(mScaleX);
        hash = 31 * hash + Float.hashCode(mScaleY);
        hash = 31 * hash + Float.hashCode(mScaleZ);

        return hash;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void checkScaleNotZero(String name, float scale)
    {
        if (scale == 0f) {
            final String format = "%s cannot be 0, given: %f";
            throw new IllegalArgumentException(String.format(format, name, scale));
        }
    }

    private void checkScaleNotNaN(String name, float scale)
    {
        if (Float.isNaN(scale)) {
            final String format = "%s cannot be NaN";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }
}