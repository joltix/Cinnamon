package com.cinnamon.system;

import com.cinnamon.gfx.Canvas;

/**
 * <p>
 *     Listener to be used for changes in sizes such as with {@link Canvas#setOnResizeListener(OnResizeListener)}
 *     when the {@link Window} is resized.
 * .</p>
 */
public interface OnResizeListener
{
    /**
     * <p>Called when there's a change in size..</p>
     *
     * @param oldWidth width before resize.
     * @param oldHeight height before resize.
     * @param width new width.
     * @param height new height.
     */
    void onResize(float oldWidth, float oldHeight, float width, float height);
}