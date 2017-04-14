package com.cinnamon.utils;

/**
 * <p>
 *     TimeInterpolator where the interpolated value shrinks exponentially over time.
 * </p>
 */
public final class DecelInterpolator extends TimeInterpolator
{
    @Override
    protected float interpolate(float from, float to, float progress)
    {
        progress = 1f - progress;
        return to + ((from - to) * progress * progress);
    }
}
