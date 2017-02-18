package com.cinnamon.system;

/**
 * <p>
 *     Callback for changes to an {@link Object}'s owner. Specifically, this listener is notified whenever an Object
 *     either loses its owner and becomes an orphaned or gains an owner after being an orphan.
 * </p>
 */
public interface OnOrphanChangedListener
{
    /**
     * <p>Called whenever an {@link Object} either gains or loses orphan status. That is, either the Object no
     * longer has an owner or it gains one.</p>
     *
     * @param id Object id.
     * @param isOrphan true if gained orphan status.
     */
    void onOrphanChanged(int id, boolean isOrphan);
}
