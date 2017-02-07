package com.cinnamon.utils;


/**
 * <p>
 *     Listener to be triggered on a mouse click.
 * </p>
 *
 *
 */
public interface OnClickListener
{
    /**
     * <p>Called when an object is clicked on.</p>
     *
     * @param event {@link MouseEvent}.
     * @return true if the {@link Event} was used.
     */
    boolean onClick(MouseEvent event);
}
