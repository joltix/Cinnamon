package com.cinnamon.object;


import com.cinnamon.system.Config;
import com.cinnamon.system.Game;
import com.cinnamon.utils.IndexList;
import com.cinnamon.utils.PooledQueue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 *     GObjectFactory is responsible for producing {@link GObject}s to act as
 *     whatever game object is needed.
 * </p>
 *
 * <p>
 *     GObjects are neither destroyed nor forgotten when removed from the
 *     GObjectFactory. Instead, GObjects are placed in an object pool to be
 *     reused at a later GObject request and reconfigured for a different
 *     task. The GObject's id, version, or both will be different than
 *     previously, marking a new "life cycle". Therefore, references to
 *     GObjects should not be made as the reference may become stale (e.g.
 *     pointing to the GObject in the object pool or as a different entity
 *     after being recycled).
 * </p>
 * <p>
 *     Instead, the id and version of a GObject should
 *     be passed around as a compound key. GObjectFactory Look ups using the id
 *     and version a of GObject from a different life cycle will yield
 *     <i>null</i>.
 * </p>
 *
 *
 */
public abstract class GObjectFactory
{
    /**
     * <p>Configuration name for {@link Room}s.</p>
     */
    public static final String CONFIG_ROOM = "room";

    // Config mapping
    private final HashMap<String, GObjectConfig> mConfigs = new
            HashMap<String, GObjectConfig>();

    // Object pool for reusing GObjects
    private final PooledQueue<GObject> mObjPool = new PooledQueue<GObject>();

    // GObject Lookup and id assignment
    private final IndexList<GObject> mGObIndex;

    // Component factories for configuring GObjects
    private final Game.Resources mResources;

    // Whether or not the GObjectFactory is done initializing
    private boolean mLoaded = false;

    /**
     * <p>Constructor for GObjectFactory.</p>
     *
     * @param resources resources for assembling {@link GObject}s.
     * @param load initial capacity.
     * @param growth normalized capacity growth.
     */
    protected GObjectFactory(Game.Resources resources, int load,
                             float growth)
    {
        mResources = resources;
        mGObIndex = new IndexList<GObject>(load, growth);
    }

    /**
     * <p>Begins initializing operations such as adding initial
     * {@link Config}s.</p>
     */
    public final void load()
    {
        addConfiguration(CONFIG_ROOM, new RoomConfig());

        onLoad(mResources);
        mLoaded = true;
    }

    /**
     * <p>Callback for initialization operations.</p>
     *
     * @param resources component factories.
     */
    protected abstract void onLoad(Game.Resources resources);

    /**
     * <p>Checks whether or not the {@link GObjectFactory} has been loaded (i
     * .e. added initial {@link Config}s and other initialization operations).
     * </p>
     *
     * @return true if the factory has completed loading.
     */
    public final boolean isLoaded()
    {
        return mLoaded;
    }

    /**
     * <p>Gets an array of {@link GObject}s configured with the
     * {@link Config} of the given name.</p>
     *
     * @param configName configuration.
     * @param count number of GObjects to produce.
     * @return GObject[].
     */
    public final GObject[] getGObject(String configName, int count)
    {
        // Get configuration
        final GObjectConfig config = getConfig(configName);

        // Get a new GObject and configure each into the output array
        final GObject[] objs = new GObject[count];
        for (int i = 0; i < count; i++) {
            objs[i] = getGObject(config);
        }

        return objs;
    }

    /**
     * <p>Gets a {@link GObject} configured with the {@link Config} of the
     * given name.</p>
     *
     * @param configName configuration.
     * @return GObject.
     */
    public final GObject getGObject(String configName)
    {
        final GObjectConfig config = getConfig(configName);
        return getGObject(config);
    }

    /**
     * <p>Gets a {@link GObject} to be configured by a given {@link Config}.</p>
     *
     * @param config Config.
     * @return GObject.
     */
    private GObject getGObject(GObjectConfig config)
    {
        // Instantiate new GObject if none can be reused
        final GObject obj;
        if (mObjPool.isEmpty()) {
            obj = new GObject();
        } else {
            obj = mObjPool.poll();
        }

        // Store and assign id
        final int id = mGObIndex.add(obj);
        obj.setId(id);

        // Apply config if availabe
        if (config != null) {
            config.configure(obj, mResources);
        }

        // Notify callback of GObject request
        onRequisition(obj);

        return obj;
    }

    /**
     * <p>Callback for {@link GObject} requests. This method is called after
     * the GObject has been given a new identity and configured.</p>
     *
     * @param object new GObject.
     */
    protected abstract void onRequisition(GObject object);

    /**
     * <p>Gets a {@link GObject} given its id and version.</p>
     *
     * @param id id.
     * @param version version.
     * @return GObject.
     */
    public final GObject get(int id, int version)
    {
        final GObject obj = mGObIndex.get(id);
        return (obj != null && obj.getVersion() == version) ? obj : null;
    }

    /**
     * <p>Removes a {@link GObject} from use. The object itself may be
     * reused at a later time, though one or both its id and version will be
     * different.</p>
     *
     * @param id id.
     * @param version version.
     * @return true if the GObject was successfully removed, false if it was
     * not found.
     */
    public final boolean remove(int id, int version)
    {
        final GObject obj = get(id, version);
        if (obj != null) {

            obj.setImageComponent(null);

            // Notify subclasses
            onRemove(obj);

            // Update GObject version # for next use
            obj.setVersion(obj.getVersion() + 1);

            // Reset GObject to default state and move to obj pool
            makeDefault(obj);
            mGObIndex.remove(id);
            mObjPool.add(obj);

            return true;
        }

        return false;
    }

    /**
     * <p>Callback for a {@link GObject}'s removal. The given GObject state
     * has not yet been altered and is, at the time of this method call,
     * still "alive" as it pertains to the factory.</p>
     *
     * @param object GObject about to be removed.
     */
    protected abstract void onRemove(GObject object);

    /**
     * <p>Sets a {@link GObject} to a default state to be implemented by
     * subclasses. This method will be called to reset GObjects for reuse.</p>
     *
     * @param object GObject o reset.
     */
    protected abstract void makeDefault(GObject object);

    /**
     * <p>Removes all {@link GObject}s.</p>
     */
    public final void clear()
    {
        mGObIndex.clear();
        mObjPool.clear();
    }

    /**
     * <p>Gets the number of {@link GObject}s.</p>
     *
     * @return GObject count.
     */
    public final int size()
    {
        return mGObIndex.size();
    }

    /**
     * <p>Gets the {@link Config} associated with a given name.</p>
     *
     * @param name configuration name.
     * @return configuration.
     */
    public final GObjectConfig getConfig(String name)
    {
        return mConfigs.get(name);
    }

    /**
     * <p>Adds and associates a {@link Config} with a name for producing
     * {@link GObject}s with the same exact setup.</p>
     *
     * @param name configuration name.
     * @param config configuration.
     * @return Config previously associated with the given name.
     */
    public final void addConfiguration(String name, GObjectConfig config)
    {
        mConfigs.put(name, config);
    }

    /**
     * <p>Gets a {@link Set} of all {@link GObjectConfig} names.</p>
     *
     * @return all GObjectConfig names.
     */
    public final Set<String> getConfigNames()
    {
        return mConfigs.keySet();
    }

    /**
     * <p>
     *     {@link Config} for assembling a {@link GObject}.
     * </p>
     */
    public interface GObjectConfig extends Config<GObject, Game.Resources>
    {

    }

    /**
     * <p>
     *     {@link Config} for setting up a {@link GObject} meant to host a
     *     {@link Room}.
     * </p>
     */
    private static class RoomConfig implements GObjectConfig
    {
        @Override
        public void configure(GObject object, Game.Resources resource)
        {
            // Move GObject to far back (behind other GObjects) for drawing
            object.moveTo(object.getX(), object.getY(), -Float.MAX_VALUE);
        }
    }
}
