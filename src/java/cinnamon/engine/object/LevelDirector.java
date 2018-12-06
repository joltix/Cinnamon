package cinnamon.engine.object;

import cinnamon.engine.utils.Bounds;

import static java.util.Objects.requireNonNull;

import java.util.NoSuchElementException;

/**
 * This interface provides common actions when manipulating game objects within a particular level.
 *
 * <p>Some common actions require resources from the level. Implementing classes must return these from
 * their respective getters.</p>
 *
 * <ul>
 *     <li>entity management</li>
 *     <li>level boundary</li>
 * </ul>
 */
public interface LevelDirector
{
    /**
     * Moves an {@code Entity} to a location.
     * 
     * <p>The position may be adjusted to ensure it exists within the level's bounds.</p>
     *
     * @param entity entity.
     * @param x x.
     * @param y y.
     * @param z z.
     * @throws NullPointerException if {@code entity} is {@code null}.
     * @throws IllegalArgumentException if {@code x}, {@code y}, or {@code z} is {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     * @throws IllegalStateException if the level boundary is unavailable.
     */
    default void move(Entity entity, float x, float y, float z)
    {
        requireNonNull(entity);

        checkValueNotNaN("X", x);
        checkValueNotNaN("Y", y);
        checkValueNotNaN("Z", z);

        checkValueNotInfinite("X", x);
        checkValueNotInfinite("Y", y);
        checkValueNotInfinite("Z", z);

        final Bounds b = getLevelBoundary();
        checkBoundaryIsAvailable(b);

        // Ensure positioned within level
        x = Math.min(b.getMaximumX(), Math.max(b.getMinimumX(), x));
        y = Math.min(b.getMaximumY(), Math.max(b.getMinimumY(), y));
        z = Math.min(b.getMaximumZ(), Math.max(b.getMinimumZ(), z));

        entity.getTransform().setPosition(x, y, z);
    }

    /**
     * Spawns an {@code Entity} at a location.
     * 
     * @param entity configuration name.
     * @param x x.
     * @param y y.
     * @param z z.
     * @return entity.
     * @throws NullPointerException if {@code entity} is {@code null}.
     * @throws IllegalArgumentException if {@code x}, {@code y}, or {@code z} is {@code Float.NaN},
     * {@code Float.POSITIVE_INFINITY}, or {@code Float.NEGATIVE_INFINITY}.
     * @throws NoSuchElementException if {@code entity} is unrecognized.
     * @throws IllegalStateException if either entity management or the level boundary is unavailable.
     */
    default Entity spawn(String entity, float x, float y, float z)
    {
        requireNonNull(entity);

        final EntityManager manager = getEntityManager();
        checkEntityManagementIsAvailable(manager);

        final Entity obj = manager.createEntity(entity);
        move(obj, x, y, z);
        
        return obj;
    }

    /**
     * Despawns an {@code Entity}.
     * 
     * @param entity entity.
     * @throws NullPointerException if {@code entity} is {@code null}.
     * @throws IllegalStateException if entity management is unavailable.
     */
    default void despawn(Entity entity)
    {
        requireNonNull(entity);

        final EntityManager manager = getEntityManager();
        checkEntityManagementIsAvailable(manager);

        manager.destroyEntity(entity.getId());
    }

    /**
     * Gets the entity management system.
     * 
     * @return entity management, or {@code null} if management is unavailable.
     */
    EntityManager getEntityManager();

    /**
     * Gets a copy of the level's boundary.
     *
     * @return level boundary, or {@code null} if the level is unavailable.
     */
    Bounds getLevelBoundary();

    private void checkEntityManagementIsAvailable(EntityManager manager)
    {
        if (manager == null) {
            throw new IllegalStateException("Entity management unavailable");
        }
    }

    private void checkBoundaryIsAvailable(Bounds bounds)
    {
        if (bounds == null) {
            throw new IllegalStateException("Level boundary unavailable");
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
}