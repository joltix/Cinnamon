package com.cinnamon.object;

import com.cinnamon.utils.Vector2F;

import java.util.List;

/**
 * <p>
 *     Base class for a fixed step physics solver to move {@link BodyComponent}s through physics computations.
 * </p>
 */
public abstract class Solver
{
    // Maximum world speed for all bodies
    public static final float MAX_SPEED = 100f;

    // Fixed timestep only
    private final float mTimestep;

    /**
     * <p>Constructs a Solver.</p>
     *
     * @param timestep timestep.
     * @throws IllegalArgumentException if timestep <= 0 or timestep >= 1.</>
     */
    public Solver(float timestep)
    {
        // Confirm valid step range
        if (timestep <= 0f || timestep >= 1f) {
            throw new IllegalArgumentException("Timestep should be > 0 and < 1");
        }

        mTimestep = timestep;
    }

    /**
     * <p>Steps all {@link BodyComponent}s through physics computations for one moment in game time.</p>
     *
     * @param objectFactory {@link GObject} lookup.
     * @param bodyFactory body lookup.
     */
    public abstract void update(GObjectFactory objectFactory, BodyFactory bodyFactory);

    /**
     * <p>Gets a list of all collisions with a specific {@link BodyComponent}.</p>
     *
     * @param factory body lookup.
     * @param body body.
     * @return list of collisions.
     */
    public abstract List<BodyComponent> getCollisions(BodyFactory factory, BodyComponent body);

    /**
     * <p>Gets the timestep. This value is always > 0 and < 1.</p>
     *
     * @return timestep.
     */
    public final float getTimestep()
    {
        return mTimestep;
    }

    /**
     * <p>Gets the global acceleration vector applied to all {@link BodyComponent}s in every update.</p>
     *
     * @return gravity.
     */
    public abstract Vector2F getGlobalAcceleration();

    /**
     * <p>Copies the global acceleration vector to a given vector container.</p>
     *
     * @param container container.
     */
    public abstract void getGlobalAcceleration(Vector2F container);

    /**
     * <p>Sets the global acceleration vector to apply to all {@link BodyComponent}s in every update.</p>
     *
     * @param vector global acceleration.
     */
    public abstract void setGlobalAcceleration(Vector2F vector);
}
