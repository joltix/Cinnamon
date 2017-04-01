package com.cinnamon.system;

import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;

/**
 * <p>
 *     ComponentFactory is a base class for producing {@link Component}s. This class offers id and version tracking
 *     as well as object pooling for produced Components.
 * </p>
 */
public abstract class ComponentFactory<E extends ComponentFactory.Component, R> extends IndexedFactory<E, R>
{
    /**
     * <p>Constructor for ComponentFactory.</p>
     *
     * @param resource resource to help assemble {@link Component}s.
     * @param load initial Component capacity.
     * @param growth normalized capacity expansion.
     */
    protected ComponentFactory(R resource, int load, float growth)
    {
        super(resource, load, growth);
    }

    /**
     * <p>
     *     Classes extending Component are meant to be associated with a {@link GObject} such that the Component
     *     not only knows the id and version of its owning GObject but that the GObject may also retrieve the
     *     Component. This latter requirement means that GObject and {@link GObjectFactory} need to be subclassed in
     *     order to integrate new Components.
     * </p>
     */
    public static abstract class Component extends IndexedFactory.Identifiable
    {
        /**
         * <p>{@link GObject} id constant for {@link ComponentFactory.Component}s that have been abandoned (i.e. no
         * GObject uses them). This occurs when {@link #setGObject(GObject)} is given the value <i>null</i>.</p>
         */
        public static final int NULL = -1;

        // Listener for changes to orphan status
        private OnOrphanChangedListener mOnOrphanChangedListener;

        // Owning GObject's id and version
        private int mOwnerId;
        private int mOwnerVersion;

        /**
         * <p>Gets the owning {@link GObject}'s id.</p>
         *
         * @return owner's id.
         */
        public final int getGObjectId()
        {
            return mOwnerId;
        }

        /**
         * <p>Gets the owning {@link GObject}'s version.</p>
         *
         * @return owner's version.
         */
        public final int getGObjectVersion()
        {
            return mOwnerVersion;
        }

        /**
         * <p>Sets the owning {@link GObject}.</p>
         *
         * @param object owner.
         */
        public final void setGObject(GObject object)
        {
            // Assign id/version or NULL if abandoned
            if (object == null) {
                mOwnerId = NULL;
                mOwnerVersion = NULL;

                // Notify listener of orphan status
                if (mOnOrphanChangedListener != null) {
                    mOnOrphanChangedListener.onOrphanChanged(getId(), true);
                }

            } else {

                // Check if was already orphan before gaining new owner
                final boolean gainedOwner = mOwnerId != NULL && mOwnerVersion != NULL;

                // Update id and version to new
                mOwnerId = object.getId();
                mOwnerVersion = object.getVersion();

                // Notify no longer an orphan after gaining an owner
                if (gainedOwner && mOnOrphanChangedListener != null) {
                    mOnOrphanChangedListener.onOrphanChanged(getId(), false);
                }
            }
        }

        /**
         * <p>Checks whether or not the Component has orphan status (i.e. has no owning {@link GObject}).</p>
         *
         * @return true if Component has been orphaned.
         */
        public final boolean isOrphan()
        {
            return mOwnerId == NULL && mOwnerVersion == NULL;
        }

        /**
         * <p>Sets an {@link OnOrphanChangedListener} to be notified of orphan status changes.</p>
         *
         * @param listener OnOrphanChangedListener.
         */
        public final void setOnOrphanChangedListener(OnOrphanChangedListener listener)
        {
            mOnOrphanChangedListener = listener;
        }
    }
}
