package com.cinnamon.object;

import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.Config;
import com.cinnamon.utils.Shape;

/**
 * <p>
 *     Provides {@link BodyComponent} lookup when computing physics updates.
 * </p>
 */
public abstract class BodyFactory extends ComponentFactory<BodyComponent, Object>
{
    // Width to use when body's instantiated
    private static final float DEFAULT_WIDTH = 1f;

    // Height to use when body's instantiated
    private static final float DEFAULT_HEIGHT = 1f;

    /**
     * <p>Constructs a BodyFactory.</p>
     *
     * @param object resource for constructing {@link BodyComponent}s.
     * @param load initial BodyComponent capacity.
     * @param growth normalized capacity expansion.
     */
    protected BodyFactory(Object object, int load, float growth)
    {
        super(object, load, growth);
    }

    /**
     * <p>Gets a {@link BodyComponent} of a specific mass.</p>
     *
     * @param mass in kilograms.
     * @return body.
     */
    public final BodyComponent get(float mass)
    {
        final BodyComponent body = super.get();

        // Set mass before being used
        body.setMass(mass);

        return body;
    }

    @Override
    public final BodyComponent get()
    {
        return super.get();
    }

    @Override
    public final BodyComponent get(String configName)
    {
        return super.get(configName);
    }

    @Override
    public final BodyComponent get(int id, int version)
    {
        return super.get(id, version);
    }

    @Override
    public final BodyComponent get(int id)
    {
        return super.get(id);
    }

    @Override
    protected final BodyComponent createIdentifiable()
    {
        return new BodyComponent(new Shape(DEFAULT_WIDTH, DEFAULT_HEIGHT));
    }

    @Override
    public final BodyComponent remove(int id, int version)
    {
        return super.remove(id, version);
    }

    @Override
    public final BodyComponent remove(int id)
    {
        return super.remove(id);
    }

    @Override
    public final Config<BodyComponent, Object> getConfig(String name)
    {
        return super.getConfig(name);
    }

    @Override
    public final Config<BodyComponent, Object> addConfig(String name, Config<BodyComponent, Object> config)
    {
        return super.addConfig(name, config);
    }

    @Override
    public final Config<BodyComponent, Object> removeConfig(String name)
    {
        return super.removeConfig(name);
    }
}
