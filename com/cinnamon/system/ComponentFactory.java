package com.cinnamon.system;

import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.utils.IndexList;
import com.cinnamon.utils.PooledQueue;

import java.util.HashMap;

/**
 * <p>ComponentFactory is a base class for producing {@link Component}s.
 * This class offers id tracking and object pool for produced Components.</p>
 *
 *
 */
public abstract class ComponentFactory<E extends ComponentFactory.Component, U>
{
    // Configurations lookup
    private HashMap<String, Config<E, U>> mConfigs = new HashMap<String,
            Config<E, U>>();

    // Component pool for recycling
    private final PooledQueue<E> mCompPool = new PooledQueue<E>();

    // Component storage
    private final IndexList<E> mLookup;

    // Assembly resource
    private final U mResource;

    // Whether or not init ops are complete
    private boolean mLoaded = false;

    /**
     * <p>Constructor for ComponentFactory.</p>
     *
     * @param resource resource to help assemble {@link Component}s.
     * @param load initial Component capacity.
     * @param growth normalized capacity expansion.
     */
    protected ComponentFactory(U resource, int load, float growth)
    {
        mResource = resource;
        mLookup = new IndexList<E>(load, growth);
    }

    /**
     * <p>Begins initializing operations such as adding {@link Config}s or
     * loading files. Specific work is deferred to overriders of
     * {@link #onLoad(Object)}.</p>
     */
    public final void load()
    {
        onLoad(getResource());
        mLoaded = true;
    }

    /**
     * <p>Callback for initialization operations.</p>
     *
     * @param resource resource.
     */
    protected abstract void onLoad(U resource);

    /**
     * <p>Gets the resource used to assemble {@link Component}s.</p>
     *
     * @return resource.
     */
    protected final U getResource()
    {
        return mResource;
    }

    /**
     * <p>Checks whether or not the factory has completed initializing.</p>
     *
     * @return true if initialization is done.
     */
    public final boolean isLoaded()
    {
        return mLoaded;
    }

    /**
     * <p>Gets a {@link Component} configured by a {@link Config} matching
     * the given configuration name. If no matching Config was found, this
     * method acts as if the overload {@link #getComponent()} was called and
     * the returned Component will be set to default.</p>
     *
     * @param configName configuration name.
     * @return Component.
     */
    public final E getComponent(String configName)
    {
        final Config<E, U> config = getConfig(configName);

        final E comp = getComponent();

        // Store in lookup and assign an id
        final int id = mLookup.add(comp);
        comp.setId(id);

        // Apply configuration if available
        if (config != null) {
            config.configure(comp, mResource);
        }

        // Allow subclass' work
        onRequisition(comp);

        return comp;
    }

    /**
     * <p>Gets a blank {@link Component}.</p>
     *
     * @return {@link E}.
     */
    protected final E getComponent()
    {
        // Instantiate a new Component if none can be reused
        final E comp;
        if (mCompPool.isEmpty()) {
            comp = createComponent();
        } else {

            // Reuse a component and reset to defaults
            comp = mCompPool.poll();
            makeDefault(comp);
        }

        return comp;
    }

    /**
     * <p>Creates a {@link E}.</p>
     *
     * @return a new {@link E}.
     */
    protected abstract E createComponent();

    /**
     * <p>Sets a {@link E} to a default state to be implemented by
     * subclasses. This method will be called to reset reused Components.</p>
     *
     * @param component Component to reset.
     */
    protected abstract void makeDefault(E component);

    /**
     * <p>Callback for a {@link Component} requests. This method is called
     * when a Component is about to be given via
     * {@link #getComponent(String)} or its overloads.</p>
     *
     * @param component Component.
     */
    protected abstract void onRequisition(E component);

    /**
     * <p>Gets the {@link Component} with the given id.</p>
     *
     * @param id id.
     * @return Component.
     */
    protected final E getComponent(int id)
    {
        return mLookup.get(id);
    }

    /**
     * <p>Removes a {@link Component} and puts it in an object pool for
     * reuse. The id associated with the Component may also be reused at the
     * factory's discretion.</p>
     *
     * @param id Component id.
     * @return removed Component.
     */
    public final E remove(int id)
    {
        // Remove from lookup
        final E component = mLookup.remove(id);

        // Notify subclasses of removal
        onRemove(component);

        // Move to pool to be used again later
        mCompPool.add(component);

        return component;
    }

    /**
     * <p>Notifies subclasses of a {@link Component}'s removal. The Component's state is as it was when removal was
     * requested.</p>
     *
     * @param component Component.
     */
    protected abstract void onRemove(E component);

    /**
     * <p>Removes all {@link Component}s.</p>
     */
    public final void clear()
    {
        mLookup.clear();
    }

    /**
     * <p>Gets the number of {@link Component}s.</p>
     *
     * @return Component count.
     */
    public int size()
    {
        return mLookup.size();
    }

    /**
     * <p>Gets the {@link Config} associated with a given name.</p>
     *
     * @param name configuration name.
     * @return configuration.
     */
    public final Config<E, U> getConfig(String name)
    {
        return mConfigs.get(name);
    }

    /**
     * <p>Adds and associates a {@link Config} with a name for producing
     * {@link Component}s with the same exact setup.</p>
     *
     * @param name configuration name.
     * @param config configuration.
     * @return Config previously associated with the given name.
     */
    public final Config<E, U> addConfig(String name, Config<E, U> config)
    {
        return mConfigs.put(name, config);
    }

    /**
     * <p>
     *     Classes extending Component are are meant to be associated with a
     *     {@link GObject} such that the Component not only knows the
     *     id and version of its owning GObject but that the GObject may also
     *     retrieve the Component with a getter. This latter requirement
     *     means not only will GObject need to be subclassed in order to
     *     integrate new Components, but also the {@link GObjectFactory} that
     *     produces it.
     * </p>
     */
    public static abstract class Component
    {
        /**
         * <p>{@link GObject} id constant for {@link Component}s who have been
         * abandoned (i.e. no GObject uses them). This occurs when
         * {@link #setGObject(GObject)} is given the value <i>null</i>.</p>
         */
        public static final int NULL = -1;

        private OnOrphanChangedListener mOnOrphanChangedListener;

        // Owning GObject's id and version
        private int mOwnerId;
        private int mOwnerVersion;

        // Component id
        private int mId;

        /**
         * <p>Gets the Component's id.</p>
         *
         * @return Component id.
         */
        public final int getId()
        {
            return mId;
        }

        /**
         * <p>Sets the Component's id.</p>
         *
         * @param id Component id.
         */
        final void setId(int id)
        {
            mId = id;
        }

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
                    mOnOrphanChangedListener.onOrphanChanged(mId, true);
                }

            } else {

                // Check if was already orphan before gaining new owner
                final boolean gainedOwner = mOwnerId != Component.NULL && mOwnerVersion != Component.NULL;

                // Update id and version to new
                mOwnerId = object.getId();
                mOwnerVersion = object.getVersion();

                // Notify no longer an orphan after gaining an owner
                if (gainedOwner && mOnOrphanChangedListener != null) {
                    mOnOrphanChangedListener.onOrphanChanged(mId, false);
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
