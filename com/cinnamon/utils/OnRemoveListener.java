package com.cinnamon.utils;

/**
 * <p>
 *     Listener callback to be notified when an Object has been removed.
 * </p>
 */
public interface OnRemoveListener<E>
{
    /**
     * <p>Called when an Object is removed from a container.</p>
     *
     * @param object Object.
     */
    void onRemove(E object);
}
