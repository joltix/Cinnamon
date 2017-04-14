package com.cinnamon.utils;

/**
 * <p>
 *     TimeInterpolator where the interpolated value quickly grows from the initial value but eventually
 *     shrinks rapidly as it approaches the target value.
 * </p>
 */
public final class EaseInterpolator extends TimeInterpolator
{
    @Override
    protected float interpolate(float from, float to, float progress)
    {
        // Hermite function: -2t^3 + 3t^2
        final float progSqr = progress * progress;
        progress = (-2f * progress * progSqr) + (3f * progSqr);

        return super.interpolate(from, to, progress);
    }
}
