package com.cinnamon.utils;

/**
 * <p>
 *     TimeInterpolator where the interpolated value grows exponentially over time.
 * </p>
 */
public final class AccelInterpolator extends TimeInterpolator
{
    @Override
    protected float interpolate(float from, float to, float progress)
    {
        return from + ((to - from) * progress * progress);
    }
}
