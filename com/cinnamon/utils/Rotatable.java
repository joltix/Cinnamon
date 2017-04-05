package com.cinnamon.utils;

/**
 * <p>
 *     Describes an object that can be rotated.
 * </p>
 *
 * <p>
 *     Angles are operated with in radians. An angle of 0's direction is understood as the unit vector (1,0).
 * </p>
 */
public interface Rotatable
{
    /**
     * <p>Gets the angle of rotation.</p>
     *
     * @return angle in radians.
     */
    double getRotation();

    /**
     * <p>Rotates the object to a specific angle from angle 0.</p>
     *
     * @param angle in radians.
     */
    void rotateTo(double angle);

    /**
     * <p>Rotates the object by an angle.</p>
     *
     * @param angle in radians.
     */
    void rotateBy(double angle);
}
