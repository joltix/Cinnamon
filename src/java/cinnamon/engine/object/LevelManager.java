package cinnamon.engine.object;

import cinnamon.engine.core.Game.CoreSystem;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * Acts as a registry for {@link Level} objects.
 *
 * <p>Levels are executed by requesting load through {@link #loadLevel(String)} and unload with
 * {@link #unloadLevel(String)}. These load changes are delayed and occur when the
 * {@code LevelManager} updates through {@code onTick()}. Only one level can be loaded at a time.</p>
 */
public final class LevelManager extends CoreSystem
{
    // Usually called once per tick so long as no level load requested
    private static final Consumer<LevelManager> NO_OP_TASK = (manager) -> { };

    // Unloads previous loaded level
    private static final Consumer<LevelManager> UNLOAD_TASK = (manager) ->
    {
        manager.mLoadedLevel.unload();
        manager.mLoadedLevel.setEntityManager(null);
        manager.mLoadedLevel = null;

        manager.mTask = NO_OP_TASK;
    };

    // Unloads previously loaded level and loads a new one
    private static final Consumer<LevelManager> LOAD_TASK = (manager) ->
    {
        // Unload previous level
        if (manager.mLoadedLevel != null) {
            UNLOAD_TASK.accept(manager);
        }

        manager.mLoadedLevel = manager.mPendingLevel;
        manager.mPendingLevel = null;

        // Load new level
        manager.mLoadedLevel.setEntityManager(manager.mEntMan);
        manager.mLoadedLevel.load();

        // Update level's systems for the first time
        manager.mTask = LevelManager.UPDATE_TASK;
        manager.mTask.accept(manager);
    };

    // Usual task to execute once per tick
    private static final Consumer<LevelManager> UPDATE_TASK = (manager) ->
    {
        manager.mLoadedLevel.updateSystems();
    };

    // Level lookup
    private final Map<String, Level> mLevels = new HashMap<>();

    private final EntityManager mEntMan;

    // Currently loaded
    private Level mLoadedLevel;

    // Requested to be loaded
    private Level mPendingLevel;

    // Executed once per tick
    private Consumer<LevelManager> mTask;

    /**
     * Constructs a {@code LevelManager}.
     *
     * @param priority priority.
     * @param manager manager.
     * @throws NullPointerException if {@code manager} is {@code null}.
     */
    public LevelManager(int priority, EntityManager manager)
    {
        super(priority);

        requireNonNull(manager);

        mEntMan = manager;
        mTask = NO_OP_TASK;
    }

    /**
     * Loads a level.
     *
     * <p>The level will be loaded on the next game update. Any previous pending load request is overwritten.</p>
     *
     * @param name level name.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws NoSuchElementException if no level has the specified name.
     * @throws IllegalStateException if the level is already loaded.
     * @see #unloadLevel(String)
     */
    public void loadLevel(String name)
    {
        requireNonNull(name);

        final Level lvl = mLevels.get(name);

        if (lvl == null) {
            final String format = "Level \'%s\' is unrecognized";
            throw new NoSuchElementException(String.format(format, name));
        }
        if (lvl.isLoaded()) {
            final String format = "Level \'%s\' is already loaded";
            throw new IllegalStateException(String.format(format, name));
        }

        mPendingLevel = lvl;
        mTask = LOAD_TASK;
    }

    /**
     * Unloads a level.
     *
     * <p>The level will be unloaded on the next game update. If the named level was requested by
     * {@link #loadLevel(String)} but is still pending, its load is cancelled.</p>
     *
     * @param name level name.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws NoSuchElementException if no loaded level has the specified name.
     */
    public void unloadLevel(String name)
    {
        requireNonNull(name);

        // Cancel load request if name matches
        if (mPendingLevel != null && mPendingLevel.getName().equals(name)) {

            mPendingLevel = null;
            mTask = NO_OP_TASK;

        } else if (mLoadedLevel != null) {

            if (!mLoadedLevel.getName().equals(name)) {
                final String format = "\'%s\' is not a loaded level";
                throw new NoSuchElementException(String.format(format, name));
            }

            // Don't need to explicitly unload since loading unloads previous
            if (mTask != LOAD_TASK) {
                mTask = UNLOAD_TASK;
            }
        }
    }

    /**
     * Gets the level with the given name.
     *
     * @param name level name.
     * @return level.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws NoSuchElementException if no level has the specified name.
     */
    public Level getLevel(String name)
    {
        requireNonNull(name);

        final Level lvl = mLevels.get(name);

        if (lvl == null) {
            final String format = "Level \'%s\' is unrecognized";
            throw new NoSuchElementException(String.format(format, name));
        }

        return lvl;
    }

    /**
     * Adds a level.
     * 
     * @param level level.
     * @throws NullPointerException if {@code level} is {@code null}.
     * @throws IllegalArgumentException if the level's name is already in use.
     */
    public void addLevel(Level level)
    {
        requireNonNull(level);

        if (mLevels.containsKey(level.getName())) {
            final String format = "Level name \'%s\' already in use";
            throw new IllegalArgumentException(String.format(format, level.getName()));
        }

        mLevels.put(level.getName(), level);
    }

    /**
     * Removes a level.
     *
     * <p>This method has no effect if the given name does not refer to a level.</p>
     * 
     * @param name level name.
     * @return removed level.
     * @throws NullPointerException if {@code name} is {@code null}.
     * @throws IllegalStateException if the level is currently loaded.
     */
    public Level removeLevel(String name)
    {
        requireNonNull(name);

        final Level lvl = mLevels.remove(name);

        if (lvl != null && lvl.isLoaded()) {
            final String format = "Level \'%s\' is in use. Only unloaded levels can be removed";
            throw new IllegalStateException(String.format(format, name));
        }

        return lvl;
    }

    /**
     * Returns {@code true} if the given name is already associated with a level.
     * 
     * @param name level name.
     * @return true if in use.
     * @throws NullPointerException if {@code name} is {@code null}.
     */
    public boolean isNameInUse(String name)
    {
        requireNonNull(name);

        return mLevels.containsKey(name);
    }

    /**
     * Gets the currently loaded level.
     *
     * @return current level, or {@code null} if none is yet loaded.
     */
    public Level getCurrentLevel()
    {
        return mLoadedLevel;
    }

    /**
     * Gets the number of levels.
     *
     * @return level count.
     */
    public int getLevelCount()
    {
        return mLevels.size();
    }

    /**
     * Returns {@code false}. This system cannot be paused.
     * 
     * @return {@code false}.
     */
    @Override
    public boolean isPausable()
    {
        return false;
    }

    @Override
    protected void onTick()
    {
        mTask.accept(this);
    }

    @Override
    protected void onStart() { }

    @Override
    protected void onPause(int reason) { }

    @Override
    protected void onResume(int reason) { }

    @Override
    protected void onStop() { }
}