package cinnamon.engine.utils;

/**
 * <p>Variant of {@code SpacePartitioningTree} mandating that {@code Bounds} in use are locked from use by any other
 * tree instance across implementations of this interface. {@code LockingSpacePartitionTrees} are not intended to be
 * implemented outside of {@link cinnamon.engine.utils}.</p>
 *
 * <b>Exclusivity</b>
 * <p>Adding a {@code Bounds} that has not yet been added to a {@code LockingSpacePartitioningTree} will make the
 * {@code Bounds}' {@code isExclusivelyPartitioned()} return true. Attempting to add the same {@code Bounds} to
 * another locking tree instance will cause {@link #add(Bounds, Object)} to throw an exception unless the
 * {@code Bounds} is first removed from the tree that contains it.</p>
 *
 * <p>Whether or not a {@code Bounds} has been locked does not affect its use in non-locking
 * {@code SpacePartitioningTrees}.</p>
 *
 * @param <E> element to partition.
 */
public interface LockingSpacePartitioningTree<E> extends SpacePartitioningTree<E>
{
    /**
     * {@inheritDoc}
     *
     * <p>Successfully adding a {@code Bounds} to the tree will cause {@code bounds.isExclusivelyPartitioned()} to
     * return true and prevent the {@code Bounds} from being added to another {@code LockingSpacePartitioningTree}.</p>
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException if the bounds is already inside a {@code LockingSpacePartitioningTree}.
     */
    @Override
    boolean add(Bounds bounds, E element);

    /**
     * {@inheritDoc}
     *
     * <p>Successfully removing a {@code Bounds} from the tree will release this tree's lock and cause
     * {@code bounds.isExclusivelyPartitioned()} to return false. After this method executes, the {@code Bounds} can
     * be added to another {@code LockingSpacePartitioningTree}.</p>
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    E remove(Bounds bounds);

    /**
     * {@inheritDoc}
     *
     * <p>All {@code Bounds} are released and calling {@code bounds.isExclusivelyPartitioned()} on any of the removed
     * {@code Bounds} will return false. After this method executes, these {@code Bounds} are free to be added to other
     * {@code LockingSpacePartitioningTrees}.</p>
     */
    @Override
    void clear();
}
