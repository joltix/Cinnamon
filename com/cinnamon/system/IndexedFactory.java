package com.cinnamon.system;

import com.cinnamon.utils.IndexList;
import com.cinnamon.utils.OnRemoveListener;
import com.cinnamon.utils.PooledQueue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * <p>
 *     Assembles {@link Identifiable}s and provides lookup through id and version numbers assigned on object creation
 *     . References to an Identifiable instance should consist of the instance's id and version instead of a direct
 *     reference to the object as the object may be recycled as another. In such a case, the direct reference no
 *     longer points to the desired object.
 * </p>
 *
 * <p>
 *     Each Identifiable is bound to an id and version number when first created and retrieved through
 *     {@link #get()} or {@link #get(String)}. These two numbers uniquely identify an Identifiable amongst all created
 *     by a specific IndexedFactory instance. The id, retrievable with {@link Identifiable#getId()}, refers to an
 *     Identifiable instance at the time of the lookup. The version number, however, refers to a specific period of
 *     time the Identifiable was used before {@link #remove(int)} or {@link #remove(int, int)} was called and the
 *     instance was recycled. Together, both values point not only to a specific Identifiable instance but also a
 *     specific "life". While an id and version combination will not be reused once the instance has been recycled,
 *     both values may be reused independently of each other.
 * </p>
 *
 * <p>
 *     Setup operations can be performed on Identifiables when first created by specifying a {@link Config} that has
 *     been added to the IndexedFactory. Configs can use the resource from {@link #getResource()} to configure an
 *     Identifiable instance according to a template before providing the instance for use.
 * </p>
 */
public abstract class IndexedFactory<E extends IndexedFactory.Identifiable, R>
{
    // Config to reset objects to a default state
    private final Config<E, R> mDefaultConfig;

    // Configurations lookup
    private final HashMap<String, Config<E, R>> mConfigs = new HashMap<String, Config<E, R>>();

    // Object pool for recycling
    private final PooledQueue<E> mCompPool = new PooledQueue<E>();

    // Object id lookup
    private final IndexList<E> mLookup;

    // Object to help when configuring
    private final R mResource;

    // Whether or not init ops are complete
    private boolean mLoaded = false;

    /**
     * <p>Constructor for an IndexedFactory.</p>
     *
     * @param resource resource to help create {@link E}.
     * @param load initial capacity.
     * @param growth normalized capacity expansion.
     */
    protected IndexedFactory(R resource, int load, float growth)
    {
        mResource = resource;
        mLookup = new IndexList<E>(load, growth);

        // Add Config for default configuration
        mDefaultConfig = createDefaultConfig();
        if (mDefaultConfig == null) {
            throw new IllegalStateException("createDefaultConfig() may not return null");
        }
    }

    /**
     * <p>Creates a {@link Config} that will configure a {@link E} to a default state. The object's default state is
     * defined by the IndexedFactory's subclasses.</p>
     *
     * @return default state Config.
     */
    protected abstract Config<E, R> createDefaultConfig();

    /**
     * <p>Begins initializing operations such as adding {@link Config}s or loading files. Specific work is deferred
     * to overriders of {@link #onLoad(Object)}.</p>
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
    protected abstract void onLoad(R resource);

    /**
     * <p>Gets the resource used to configure objects.</p>
     *
     * @return resource.
     */
    protected final R getResource()
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
     * <p>Gets an object in the default state.</p>
     *
     * @return object.
     */
    public E get()
    {
        return get(null);
    }

    /**
     * <p>Gets an object configured by a {@link Config} matching the given configuration name. If no
     * matching Config was found, this method acts as if the overload {@link #getIdentifiableInstance()} was called
     * and the returned object will be set to a default state.</p>
     *
     * @param configName configuration name.
     * @return object.
     */
    public E get(String configName)
    {
        Config<E, R> config = getConfig(configName);

        // Prepare Config for default state if no name matched
        if (config == null) {
            config = mDefaultConfig;
        }

        final E inst = getIdentifiableInstance();

        // Store in lookup and assign an id
        final int id = mLookup.add(inst);
        inst.setId(id);

        // Apply configuration if available
        config.configure(inst, mResource);

        // Allow subclass' work
        onRequisition(inst);

        return inst;
    }

    /**
     * <p>Gets an object. The returned instance may be configured according to its last use.</p>
     *
     * @return {@link E}.
     */
    private E getIdentifiableInstance()
    {
        // Instantiate a new object if none can be reused
        final E comp;
        if (mCompPool.isEmpty()) {
            comp = createIdentifiable();
        } else {

            // Reuse an object
            comp = mCompPool.poll();
        }

        return comp;
    }

    /**
     * <p>Creates a {@link E}.</p>
     *
     * @return a new {@link E}.
     */
    protected abstract E createIdentifiable();

    /**
     * <p>Callback for an object request. This method is called when an object is about to be given via
     * {@link #get(String)} or its overloads.</p>
     *
     * @param object object.
     */
    protected abstract void onRequisition(E object);

    /**
     * <p>Gets the {@link E} with the given id and version.</p>
     *
     * @param id id.
     * @return object.
     */
    public E get(int id, int version)
    {
        final E obj = mLookup.get(id);
        return (obj != null && obj.getVersion() == version) ? obj : null;
    }

    /**
     * <p>Gets the {@link E} with the given id.</p>
     *
     * @param id id.
     * @return object.
     */
    public E get(int id)
    {
        return mLookup.get(id);
    }

    /**
     * <p>Removes an object matching a specific id version combination. The id associated with the object may also
     * be reused at the factory's discretion though the id version pair will not be reused.</p>
     *
     * @param id id.
     * @param version version.
     * @return removed object or null if combination was not found.
     */
    public E remove(int id, int version)
    {
        final E obj = get(id, version);

        // Id version combo doesn't exist so nothing to remove
        if (obj == null) {
            return null;
        }

        // Remove from lookup
        return remove(id);
    }

    /**
     * <p>Removes an object matching a specific id. The id associated with the object may also be reused at the
     * factory's discretion though the id version pair will not be reused.</p>
     *
     * @param id id.
     * @return removed object.
     */
    public E remove(int id)
    {
        // Remove from lookup
        final E obj = mLookup.remove(id);

        // Nothing to remove so don't call onRemove(E)
        if (obj == null) {
            return null;
        }

        // Notify subclasses of removal
        onRemove(obj);

        // Move to pool to be used again later
        mCompPool.add(obj);

        return obj;
    }

    /**
     * <p>Notifies the factory of an object's removal. The object's state is as it was when removal was requested.</p>
     *
     * @param object object.
     */
    protected abstract void onRemove(E object);

    /**
     * <p>Removes all objects. Associated ids may be reused though the id version pairs will not be.</p>
     */
    public final void removeAll()
    {
        for (int i = 0, sz = mLookup.size(); i < sz; i++) {
            remove(i);
        }
    }

    /**
     * <p>Removes all objects.</p>
     */
    public final void clear()
    {
        mLookup.clear();
    }

    /**
     * <p>Gets the number of objects.</p>
     *
     * @return object count.
     */
    public final int size()
    {
        return mLookup.size();
    }

    /**
     * <p>Checks if no objects are available for lookup.</p>
     *
     * @return true if size == 0.
     */
    public final boolean isEmpty()
    {
        return mLookup.isEmpty();
    }

    /**
     * <p>Gets the {@link Config} associated with a given name.</p>
     *
     * @param name configuration name.
     * @return configuration.
     */
    public Config<E, R> getConfig(String name)
    {
        return mConfigs.get(name);
    }

    /**
     * <p>Adds and associates a {@link Config} with a name for producing objects with the same exact configuration.</p>
     *
     * @param name configuration name.
     * @param config configuration.
     * @return Config previously associated with the given name.
     */
    public Config<E, R> addConfig(String name, Config<E, R> config)
    {
        return mConfigs.put(name, config);
    }

    /**
     * <p>Removes the {@link Config} associated with a given name.</p>
     *
     * @param name configuration name.
     * @return Config.
     */
    public Config<E, R> removeConfig(String name)
    {
        return mConfigs.remove(name);
    }

    /**
     * <p>Gets a {@link Set} of all stored {@link Config}s' names.</p>
     *
     * @return all Config names.
     */
    public final Set<String> getConfigNames()
    {
        return mConfigs.keySet();
    }

    /**
     * <p>Removes all {@link Config}s.</p>
     */
    public final void clearConfigs()
    {
        mConfigs.clear();
    }

    /**
     * <p>Class keeping track of an id and version assigned from the {@link IndexedFactory}.</p>
     */
    public static abstract class Identifiable
    {
        // Index for lookup
        private int mId;

        // Version to differentiate between recycles
        private int mVersion;

        /**
         * <p>Gets the id.</p>
         *
         * @return id.
         */
        public final int getId()
        {
            return mId;
        }

        /**
         * <p>Sets the id.</p>
         *
         * @param id id.
         */
        final void setId(int id)
        {
            mId = id;
        }

        /**
         * <p>Gets the version.</p>
         *
         * @return version.
         */
        public final int getVersion()
        {
            return mVersion;
        }

        /**
         * <p>Sets the version.</p>
         *
         * @param version version.
         */
        final void setVersion(int version)
        {
            mVersion = version;
        }
    }
}
