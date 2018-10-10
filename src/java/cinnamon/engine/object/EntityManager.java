package cinnamon.engine.object;

import java.util.*;

/**
 * Creates and tracks {@link Entity} objects.
 *
 * <p>All {@code Entity} objects are assigned an immutable int-valued id distinct amongst other entities created by the
 * same {@code EntityManager}. These integer names can be used for lookup with {@code getEntity(int)} or destruction
 * with {@link #destroyEntity(int)}.</p>
 *
 * <p>An {@code Entity} can be produced with preset {@code Component} elements by setting a {code Map} of
 * configuration names to the desired component prototype names. These prototypes must already be available for use
 * with the {@link ComponentManager} given during the {@code EntityManager}'s setup.</p>
 *
 * <h3>Tuning</h3>
 * <p>Finer-grain control is provided through the {@link Tuner} required to construct the {@code EntityManager}. This
 * includes performance-affecting operations like entity capacity changes or clean up of destroyed entities. This
 * access is meant for infrastructure - not gameplay code.</p>
 */
public final class EntityManager
{
    // Bits in an integer reserved for array index
    private static final int INDEX_BITS = 20;

    // Bits in an integer reserved for an index's reuse counter
    private static final int VERSION_BITS = 32 - INDEX_BITS;

    /**
     * Maximum number of living entities this manager can track at a given time.
     */
    public static final int MAXIMUM_ENTITY_COUNT = (int) Math.pow(2, INDEX_BITS);

    // Number of assignable ids per manager
    private static final long MAXIMUM_ID_COUNT = ((long) MAXIMUM_ENTITY_COUNT) * ((long) Math.pow(2, VERSION_BITS));

    // Isolates index bits
    private static final int INDEX_MASK = ~(-1 << INDEX_BITS);

    // Isolates version bits
    private static final int VERSION_MASK = ~INDEX_MASK;

    // Number of times an index can be reused
    private static final int MAXIMUM_VERSION_COUNT = (int) Math.pow(2, 32 - INDEX_BITS);

    // Automatic capacity increase factor
    private static final float GROWTH_FACTOR = 1.2f;

    // Starting capacity before first growth
    private static final int INITIAL_ENTITY_CAPACITY = 20;

    // Special reference inserted in indices when the index has used up all versions
    private static final Entity UNUSABLE = new Entity(0);

    // Named groups of component prototypes
    private final Map<String, String[]> mConfigs = new HashMap<>();

    // Source of components
    private final ComponentManager mCompMan;

    // Entities to be destroyed on the next cleanup
    private final Queue<Entity> mDestroyedEntities = new ArrayDeque<>();

    // Entity lookup
    private Entity[] mEntities;

    // Last index before all higher indices are unused; array of length 0 has highest index of 0
    private int mHighestEntityIndex = 0;

    // Number of entities still tracked
    private int mAliveCount = 0;

    // Total number of ids assigned to entities
    private long mAssignedIdCount = 0;

    // Ids from destroyed entities whose index might be reusable
    private final Deque<Integer> mRecycledIds = new ArrayDeque<>();

    // New index to use when none are recyclable
    private int mNextUnusedIndex = 0;

    private EntityManager(ComponentManager manager)
    {
        mCompMan = manager;
        mEntities = new Entity[INITIAL_ENTITY_CAPACITY];
    }

    /**
     * Gets a map of entity configurations and utilized component prototypes. Changes to the map have no effect
     * on the actual configurations.
     *
     * @return map of configurations.
     */
    public Map<String, String[]> getConfigurations()
    {
        return new HashMap<>(mConfigs);
    }

    /**
     * Sets a map of entity configurations and utilized component prototypes.
     *
     * @param configurations map of configurations.
     * @throws NullPointerException if configurations is null.
     * @throws IllegalArgumentException if component prototypes are not specified.
     * @throws NoSuchElementException if a component prototype is unrecognized.
     */
    public void setConfigurations(Map<String, String[]> configurations)
    {
        checkNotNull(configurations);

        configurations.forEach((name, prototypes) ->
        {
            checkPrototypesSpecified(name, prototypes);
            checkPrototypesAreRecognized(prototypes);
        });

        mConfigs.putAll(configurations);
    }

    /**
     * Gets the entity with the specified id.
     *
     * @param id entity id.
     * @return entity or null if id is unused.
     */
    public Entity getEntity(int id)
    {
        final int index = pullIndexFromId(id);

        if (index < 0) {
            return null;
        }

        final Entity entity = mEntities[index];
        return (entity == UNUSABLE) ? null : entity;
    }

    /**
     * Creates a new entity.
     *
     * @return entity.
     * @throws IllegalStateException if the entity limit has been reached or there are no available ids.
     */
    public Entity createEntity()
    {
        return instantiateNewEntity();
    }

    /**
     * Creates a new entity after the specified configuration.
     *
     * @param configuration configuration name.
     * @return entity.
     * @throws NullPointerException if configuration is null.
     * @throws NoSuchElementException if configuration does not exist or any of the required component prototypes are
     * unrecognized.
     * @throws IllegalStateException if the entity limit has been reached or there are no available ids.
     */
    public Entity createEntity(String configuration)
    {
        checkNotNull(configuration);

        final String[] prototypes = mConfigs.get(configuration);

        checkConfigurationExists(configuration, prototypes);
        checkPrototypesAreRecognized(prototypes);

        final Entity entity = instantiateNewEntity();
        createAndAddSpecifiedComponents(entity, prototypes);

        return entity;
    }

    /**
     * Destroys the entity assigned to the specified id.
     *
     * <p>This action is delayed. Although {@code entity.isAlive()} will return {@code false}, the entity can
     * still be retrieved through {@link #getEntity(int)} and is included in the entity count until the next cleanup
     * pass.</p>
     *
     * @param id entity id.
     */
    public void destroyEntity(int id)
    {
        final Entity entity = getEntity(id);

        if (entity != null && entity != UNUSABLE && entity.isAlive()) {

            mDestroyedEntities.add(entity);
            entity.destroy();
        }
    }

    /**
     * Gets the number of living entities.
     *
     * @return entity count.
     */
    public int getEntityCount()
    {
        return mAliveCount;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private Entity instantiateNewEntity()
    {
        final int id = retrieveNewId();

        final Entity entity = new Entity(id);
        final int index = pullIndexFromId(id);
        mEntities[index] = entity;
        mAliveCount++;

        // Track effective end of element array
        if (index > mHighestEntityIndex) {
            mHighestEntityIndex = index;
        }

        return entity;
    }

    private void createAndAddSpecifiedComponents(Entity entity, String[] prototypes)
    {
        for (String name : prototypes) {
            entity.addComponent(mCompMan.createComponent(name));
        }
    }

    private int retrieveNewId()
    {
        // Find next usable recycled id
        while (!mRecycledIds.isEmpty()) {
            final int recycled = mRecycledIds.poll();

            // Mark index as spent if it has been reused to the limit
            if (pullVersionFromId(recycled) >= MAXIMUM_VERSION_COUNT) {
                mEntities[pullIndexFromId(recycled)] = UNUSABLE;
                continue;
            }

            checkUnassignedIdsAreAvailable();
            mAssignedIdCount++;

            // Found a recycled id that has not yet been used up
            return incrementVersion(recycled);
        }

        checkEntityCapacityNotReached();
        checkUnassignedIdsAreAvailable();

        final int id = mNextUnusedIndex++;
        mAssignedIdCount++;

        // Using new index implies may need to grow array
        if (mAliveCount >= mEntities.length) {
            ensureCapacity((int) Math.ceil(mEntities.length * GROWTH_FACTOR));
        }

        return id;
    }

    private void checkUnassignedIdsAreAvailable()
    {
        if (mAssignedIdCount >= MAXIMUM_ID_COUNT) {
            throw new IllegalStateException("All available ids have been assigned");
        }
    }

    private void checkEntityCapacityNotReached()
    {
        if (mNextUnusedIndex >= MAXIMUM_ENTITY_COUNT) {
            throw new IllegalStateException("Reached maximum entity capacity");
        }
    }

    private void ensureCapacity(int capacity)
    {
        assert (capacity >= mEntities.length);

        final Entity[] larger = new Entity[capacity];
        System.arraycopy(mEntities, 0, larger, 0, mEntities.length);
        mEntities = larger;
    }

    /**
     * Reads the index component of the specified id.
     *
     * @param id id.
     * @return index.
     */
    private int pullIndexFromId(int id)
    {
        return id & INDEX_MASK;
    }

    /**
     * Reads the version component of the specified id.
     *
     * @param id id.
     * @return version.
     */
    private int pullVersionFromId(int id)
    {
        return (id & VERSION_MASK) >> INDEX_BITS;
    }

    /**
     * Returns a new id with the same index as the specified id but with a version bump.
     *
     * @param id id.
     * @return new id.
     */
    private int incrementVersion(int id)
    {
        return pullIndexFromId(id) | ((pullVersionFromId(id) + 1) << INDEX_BITS);
    }

    private void checkConfigurationExists(String configuration, String[] prototypes)
    {
        if (prototypes == null) {
            final String format = "Configuration \'%s\' was not found";
            throw new NoSuchElementException(String.format(format, configuration));
        }
    }

    private void checkPrototypesSpecified(String configuration, String[] prototypes)
    {
        final String format = "Configuration \'%s\' does not specify component prototypes";

        if (prototypes == null || prototypes.length == 0) {
            throw new IllegalArgumentException(String.format(format, configuration));
        }

        int nameCount = 0;

        // In case array is just full of nulls
        for (final String name : prototypes) {
            if (name != null) {
                nameCount++;
            }
        }

        if (nameCount == 0) {
            throw new IllegalArgumentException(String.format(format, configuration));
        }
    }

    private void checkPrototypesAreRecognized(String[] prototypes)
    {
        final Map<String, Component> referenceMap = mCompMan.getReadOnlyPrototypes();

        for (String name : prototypes) {

            if (!referenceMap.containsKey(name)) {
                final String format = "Component prototype \'%s\' was not found";
                throw new NoSuchElementException(String.format(format, name));
            }
        }
    }

    private static void checkValueNotNegative(String format, int value)
    {
        if (value < 0) {
            throw new IllegalArgumentException(String.format(format, value));
        }
    }

    private static void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Exposes details of the {@link EntityManager} for diagnostics and adjustment.
     *
     * <p>This access is meant for infrastructure - not gameplay code.</p>
     */
    public static final class Tuner
    {
        private final EntityManager mManager;

        /**
         * Constructs an {@code EntityManager.Tuner}.
         *
         * @param manager component producer.
         * @throws NullPointerException if manager is null.
         */
        public Tuner(ComponentManager manager)
        {
            checkNotNull(manager);

            mManager = new EntityManager(manager);
        }

        /**
         * Increases the manager's capacity for entities.
         *
         * <p>Capacity will not be increased past {@code EntityManager.MAXIMUM_ENTITY_COUNT}.</p>
         *
         * @param space capacity to add.
         * @throws IllegalArgumentException if space {@literal <} 0.
         */
        public void increaseEntityCapacityBy(int space)
        {
            checkValueNotNegative("Cannot increase capacity by a negative amount", space);

            if (space != 0) {
                final long desiredCap = (long) mManager.mEntities.length + space;
                final long newCapacity = Math.min(desiredCap, MAXIMUM_ENTITY_COUNT);

                mManager.ensureCapacity((int) newCapacity);
            }
        }

        /**
         * Decreases the manager's capacity for entities.
         *
         * <p>This method cannot guarantee decreasing capacity by the exact amount specified and may only partially
         * shrink capacity. Capacity will not be decreased {@literal <} 1.</p>
         *
         * @param space capacity to remove.
         * @throws IllegalArgumentException if space {@literal <} 0.
         */
        public void decreaseEntityCapacityBy(int space)
        {
            checkValueNotNegative("Cannot decrease capacity by a negative amount", space);

            if (space != 0) {
                final int desiredCap = mManager.mEntities.length - space;
                final int possibleCap = mManager.mHighestEntityIndex + 1;
                final Entity[] smaller = new Entity[Math.max(desiredCap, possibleCap)];

                System.arraycopy(mManager.mEntities, 0, smaller, 0, smaller.length);
                mManager.mEntities = smaller;
            }
        }

        /**
         * Finalizes the cleanup of destroyed entities by removing all attached components and making affected
         * entities unavailable through {@link EntityManager#getEntity(int)}.
         */
        public void removeDestroyedEntities()
        {
            final boolean removed = !mManager.mDestroyedEntities.isEmpty();
            mManager.mAliveCount -= mManager.mDestroyedEntities.size();

            mManager.mDestroyedEntities.forEach((entity) ->
            {
                entity.removeAllComponents();

                // Make unavailable through manager.getEntity(int)
                mManager.mEntities[mManager.pullIndexFromId(entity.getId())] = null;

                // Allow future versions to reuse index
                mManager.mRecycledIds.addFirst(entity.getId());
            });

            mManager.mDestroyedEntities.clear();

            if (removed) {
                updateEffectiveLastIndex();
            }
        }

        /**
         * Gets the current maximum number of entities.
         *
         * @return maximum entity count.
         */
        public int getEntityCapacity()
        {
            return mManager.mEntities.length;
        }

        /**
         * Gets the number of entities this manager can still create.
         *
         * @return remaining assignable ids.
         */
        public long getAvailableIdCount()
        {
            return MAXIMUM_ID_COUNT - mManager.mAssignedIdCount;
        }

        /**
         * Gets the manager being tuned.
         *
         * @return manager.
         */
        public EntityManager getManager()
        {
            return mManager;
        }

        /**
         * Finds the index in the manager's entity array such that no higher index refers to a usable entity. If
         * there are no usable entities, the index is 0.
         */
        private void updateEffectiveLastIndex()
        {
            if (mManager.mAliveCount == 0) {
                mManager.mHighestEntityIndex = 0;
                return;
            }

            final Entity[] elements = mManager.mEntities;

            // Seek first non-null from end as the last effective index
            for (int i = elements.length - 1; i >= 0; i--) {

                final Entity entity = elements[i];
                if (entity == null || entity == UNUSABLE) {
                    continue;
                }

                mManager.mHighestEntityIndex = i;
                break;
            }
        }
    }
}
