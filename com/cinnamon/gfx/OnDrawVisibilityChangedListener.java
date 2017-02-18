package com.cinnamon.gfx;

/**
 * <p>
 *     Listener to be called whenever the {@link ImageFactory}'s drawing data
 *     may need to be updated.
 * </p>
 *
 *
 */
public interface OnDrawVisibilityChangedListener
{
    /**
     * <p>Called when there's been a change that affects the
     * {@link ImageFactory}'s drawing data.</p>
     *
     * @param visible whether or not the {@link ImageComponent} is still visible after the change.
     */
    void onChange(boolean visible);
}
