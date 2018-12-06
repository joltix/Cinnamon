package cinnamon.engine.object;

import cinnamon.engine.core.BaseSystem;
import cinnamon.engine.core.Domain;
import cinnamon.engine.core.SystemCoordinator;
import cinnamon.engine.core.WritableSystemDirectory;
import cinnamon.engine.object.Level.WorldSystem;
import cinnamon.engine.utils.Bounds;
import cinnamon.engine.utils.BoxBounds;
import cinnamon.engine.utils.Size;

import java.util.Comparator;

import static java.util.Objects.requireNonNull;

/**
 * Game area for containing game objects and level-unique systems.
 *
 * <p>Adding and removing systems can only be done when the level is not loaded. Once a level is loaded through a
 * {@code LevelManager}, it can only be paused or resumed until the level is no longer loaded.</p>
 */
public abstract class Level implements LevelDirector, WritableSystemDirectory<WorldSystem>, SystemCoordinator<WorldSystem>
{
    /**
     * Largest possible distance from end-to-end.
     */
    public static final float MAX_SIZE = 40_000.0f;

    private final Domain<WorldSystem> mSystems = new Domain<>(Comparator.comparingInt(BaseSystem::getPriority));

    private EntityManager mEntMan;

    // Level boundaries
    private final BoxBounds mBounds;

    private final String mName;

    // True if level is active
    private boolean mLoaded = false;

    /**
     * Constructs a {@code Level}.
     *
     * @param name level name.
     * @param size area dimensions.
     * @throws NullPointerException if {@code name} or {@code size} is {@code null}.
     * @throws IllegalArgumentException if {@code name} is made of whitespace or a dimension is {@code <= 0},
     * {@code > Level.MAX_SIZE}, or is {@code Float.NaN}, {@code Float.POSITIVE_INFINITY}, or
     * {@code Float.NEGATIVE_INFINITY}.
     */
    protected Level(String name, Size size)
    {
        requireNonNull(name);
        requireNonNull(size);

        checkNameNotWhitespace(name);

        checkValueNotNaN("Width", size.getWidth());
        checkValueNotNaN("Height", size.getHeight());
        checkValueNotNaN("Depth", size.getDepth());

        checkValueNotInfinite("Width", size.getWidth());
        checkValueNotInfinite("Height", size.getHeight());
        checkValueNotInfinite("Depth", size.getDepth());

        checkDimensionWithinRange("Width", size.getWidth());
        checkDimensionWithinRange("Height", size.getHeight());
        checkDimensionWithinRange("Depth", size.getDepth());

        mBounds = new BoxBounds(size);
        mName = name;

        centerOnOrigin(mBounds);
    }

    @Override
    public void pauseSystem(String name, int reason)
    {
        if (isLoaded()) {
            mSystems.pauseSystem(name, reason);
        }
    }

    @Override
    public void resumeSystem(String name, int reason)
    {
        if (isLoaded()) {
            mSystems.resumeSystem(name, reason);
        }
    }

    @Override
    public WorldSystem getSystem(String name)
    {
        return mSystems.getSystem(name);
    }

    /**
     * @throws IllegalStateException if this method is called while the level is loaded.
     */
    @Override
    public void addSystem(String name, WorldSystem system)
    {
        if (isLoaded()) {
            throw new IllegalStateException("Systems cannot be added while the level is loaded");
        }

        mSystems.addSystem(name, system);
        system.mLevelBoundary = mBounds;
    }

    /**
     * @throws IllegalStateException if this method is called while the level is loaded.
     */
    @Override
    public void removeSystem(String name)
    {
        if (isLoaded()) {
            throw new IllegalStateException("Systems cannot be removed while the level is loaded");
        }

        final WorldSystem system = mSystems.getSystem(name);
        mSystems.removeSystem(name);
        system.mLevelBoundary = null;
    }

    /**
     * Returns {@code true} if this level is currently loaded.
     *
     * @return {@code true} if loaded.
     */
    public final boolean isLoaded()
    {
        return mLoaded;
    }

    /**
     * Gets this level's name.
     *
     * @return level name.
     */
    public final String getName()
    {
        return mName;
    }

    /**
     * @return entity management, or {@code null} if this level is not loaded.
     */
    @Override
    public final EntityManager getEntityManager()
    {
        return mEntMan;
    }

    /**
     * @return level boundary.
     */
    @Override
    public Bounds getLevelBoundary()
    {
        return new BoxBounds(mBounds);
    }

    /**
     * Called when this {@code Level} is loaded.
     *
     * <p>Systems have not yet been started.</p>
     */
    protected abstract void onLoad();

    /**
     * Called when this {@code Level} is unloaded.
     *
     * <p>Systems have not yet been stopped.</p>
     */
    protected abstract void onUnload();

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    void load()
    {
        onLoad();
        mSystems.startSystems();
        mLoaded = true;
    }

    void unload()
    {
        onUnload();
        mSystems.stopSystems();
        mLoaded = false;
    }

    void updateSystems()
    {
        mSystems.callWithUnpausedSystems(WorldSystem::onUpdate);
    }

    void setEntityManager(EntityManager manager)
    {
        mEntMan = manager;
        mSystems.callWithSystems((system) ->
        {
            system.mEntityManagement = manager;
        });
    }

    private void centerOnOrigin(BoxBounds bounds)
    {
        final float x = -bounds.getWidth() / 2f;
        final float y = -bounds.getHeight() / 2f;
        final float z = -bounds.getDepth() / 2f;

        bounds.setMinimum(x, y, z);
    }

    private void checkNameNotWhitespace(String name)
    {
        if (name.trim().isEmpty()) {
            final String format = "Level name cannot be whitespace, given: %s";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    private void checkDimensionWithinRange(String name, float size)
    {
        if (size <= 0f || size > MAX_SIZE) {
            final String format = "%s must be > 0 and <= %f, given: %f";
            throw new IllegalArgumentException(String.format(format, name, MAX_SIZE, size));
        }
    }

    private void checkValueNotNaN(String name, float value)
    {
        if (Float.isNaN(value)) {
            final String format = "%s cannot be NaN";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    private void checkValueNotInfinite(String name, float value)
    {
        if (Float.isInfinite(value)) {
            final String format = "%s cannot be infinite, given: %s";
            final String inf;

            if (value == Float.POSITIVE_INFINITY) {
                inf = "Float.POSITIVE_INFINITY";
            } else {
                inf = "Float.NEGATIVE_INFINITY";
            }

            throw new IllegalArgumentException(String.format(format, name, inf));
        }
    }

    /**
     * Organizational unit for implementing level-wide gameplay mechanics.
     */
    public static abstract class WorldSystem extends BaseSystem implements LevelDirector
    {
        private EntityManager mEntityManagement;

        private BoxBounds mLevelBoundary;

        /**
         * Constructs a {@code WorldSystem}.
         *
         * @param priority priority.
         */
        protected WorldSystem(int priority)
        {
            super(priority);
        }

        /**
         * @return entity management, or {@code null} if the level is not loaded.
         */
        @Override
        public final EntityManager getEntityManager()
        {
            return mEntityManagement;
        }

        /**
         * @return level boundary, or {@code null} if this system is not attached to a level.
         */
        @Override
        public Bounds getLevelBoundary()
        {
            return mLevelBoundary;
        }

        /**
         * Called once per game tick.
         */
        protected abstract void onUpdate();
    }
}