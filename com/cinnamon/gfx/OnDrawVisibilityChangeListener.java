package com.cinnamon.gfx;

/**
 * <p>
 *     Listener to be called whenever the {@link ImageFactory}'s drawing data
 *     may need to be updated.
 * </p>
 *
 *
 */
public interface OnDrawVisibilityChangeListener
{
    /**
     * <p>Called when there's been a change that affects the
     * {@link ImageFactory}'s drawing data.</p>
     */
    void onChange();
}
