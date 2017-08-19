package cinnamon.engine.utils;

/**
 * <p>Describes a movable <tt>Position</tt>.</p>
 */
public interface Repositionable extends Position
{
    /**
     * <p>Sets the x position.</p>
     *
     * @param x x.
     */
    void setX(float x);

    /**
     * <p>Sets the y position.</p>
     *
     * @param y y.
     */
    void setY(float y);

    /**
     * <p>Sets the z position.</p>
     *
     * @param z z.
     */
    void setZ(float z);

    /**
     * <p>Sets the x, y, and z positions.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     */
    void setPosition(float x, float y, float z);

    /**
     * <p>Adds an amount to the x position.</p>
     *
     * @param x amount to add.
     */
    void addX(float x);

    /**
     * <p>Adds an amount to the y position.</p>
     *
     * @param y amount to add.
     */
    void addY(float y);

    /**
     * <p>Adds an amount to the z position.</p>
     *
     * @param z amount to add.
     */
    void addZ(float z);
}
