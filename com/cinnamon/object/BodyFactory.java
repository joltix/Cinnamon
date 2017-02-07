package com.cinnamon.object;

import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.Config;

/**
 * <p>
 *     At the present time, this class is but a wrapper for a factory to
 *     produce {@link BodyComponent}s. It is not dissimilar to
 *     {@link ImageFactory} and will develop more functionality in time.
 * </p>
 *
 *
 */
public abstract class BodyFactory extends
        ComponentFactory<BodyComponent, Object>
{
    /**
     * <p>Constructor for a BodyFactory.</p>
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
     * <p>
     *     Config for assembling a {@link BodyComponent}.
     * </p>
     */
    public interface BodyConfig extends Config<BodyComponent, Object>
    {

    }
}
