package com.cinnamon.object;


import com.cinnamon.system.Config;
import com.cinnamon.system.Game;
import com.cinnamon.utils.IndexedFactory;

/**
 * <p>
 *     GObjectFactory is responsible for producing {@link GObject}s to act as whatever game object is needed.
 * </p>
 *
 * <p>
 *     GObjects are neither destroyed nor forgotten when removed from the GObjectFactory. Instead, GObjects are
 *     placed in an object pool to be reused at a later GObject request and reconfigured for a different task. The
 *     GObject's id, version, or both will be different than previously, marking a new "life cycle". Therefore,
 *     references to GObjects should not be made as the reference may become stale (e.g. pointing to the GObject in
 *     the object pool or as a different entity after being recycled).
 * </p>
 *
 * <p>
 *     Instead, the id and version of a GObject should be passed around as a compound key. GObjectFactory Look ups
 *     using the id and version a of GObject from a different life cycle will yield <i>null</i>.
 * </p>
 */
public abstract class GObjectFactory<E extends GObject> extends IndexedFactory<E, Game.Resources>
{
    /**
     * <p>Constructor for GObjectFactory.</p>
     *
     * @param resources resources for assembling {@link GObject}s.
     * @param load initial capacity.
     * @param growth normalized capacity growth.
     */
    protected GObjectFactory(Game.Resources resources, int load, float growth)
    {
        super(resources, load, growth);
    }

    @Override
    public final E get()
    {
        return super.get();
    }

    @Override
    public final E get(String configName)
    {
        return super.get(configName);
    }

    @Override
    public final E get(int id, int version)
    {
        return super.get(id, version);
    }

    @Override
    public final E get(int id)
    {
        return super.get(id);
    }

    @Override
    public final E remove(int id, int version)
    {
        return super.remove(id, version);
    }

    @Override
    public final E remove(int id)
    {
        final E obj = super.remove(id);
        if (obj == null) {
            return null;
        }

        // Remove body and image
        obj.setBodyComponent(null);
        obj.setImageComponent(null);

        return obj;
    }

    @Override
    public final Config<E, Game.Resources> getConfig(String name)
    {
        return super.getConfig(name);
    }

    @Override
    public final Config<E, Game.Resources> addConfig(String name, Config<E, Game.Resources> config)
    {
        return super.addConfig(name, config);
    }

    @Override
    public final Config<E, Game.Resources> removeConfig(String name)
    {
        return super.removeConfig(name);
    }
}
