package com.cinnamon.system;

import com.cinnamon.utils.Point2F;
import com.cinnamon.object.GObject;

/**
 * <p>
 *     CreateEvents represent the creation of {@link GObject}s and subsequent placement in the game world. Each
 *     CreateEvent holds the name of the {@link Config} to use when creating the GObject along with the deisred (x,y)
 *     position for placement.
 * </p>
 */
public class CreateEvent extends Event {

    // Location to place new game object
    private final Point2F mPosition = new Point2F(0, 0);

    // Game object config
    private String mConfig;

    /**
     * <p>Constructs a CreateEvent with the desired {@link GObject}'s {@link Config} name and position to place in the
     * game.</p>
     *
     * @param config Config name.
     * @param x x.
     * @param y y.
     */
    public CreateEvent(String config, float x, float y)
    {
        mConfig = config;
        mPosition.set(x, y);
    }

    /**
     * <p>Gets the desired x position.</p>
     *
     * @return x;
     */
    public final float getX()
    {
        return mPosition.getX();
    }

    /**
     * <p>Gets the desired y position.</p>
     *
     * @return y.
     */
    public final float getY()
    {
        return mPosition.getY();
    }

    /**
     * <p>Gets the name of the {@link Config} to use when creating the {@link GObject}.</p>
     *
     * @return Config name.
     */
    public final String getConfig()
    {
        return mConfig;
    }

    @Override
    protected void handle(EventDispatcher dispatcher) {
        dispatcher.process(this);
    }
}
