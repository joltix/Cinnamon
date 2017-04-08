package com.cinnamon.gfx;

/**
 * <p>
 *     Listener to be called whenever the {@link ImageFactory}'s drawable data order may need to be updated.
 * </p>
 */
public interface OnDrawOrderChangeListener
{
    /**
     * <p>Called when there's been a change that affects the {@link ImageFactory}'s drawing order.</p>
     */
    void onChange();
}
