package cinnamon.engine.core;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the lifecycles for a list of {@code BaseSystem}s from start to stop and any number of pauses and resumes
 * in-between.
 *
 * @param <E> type of system.
 */
public final class Domain<E extends BaseSystem> implements WritableSystemDirectory<E>, SystemCoordinator<E>,
        SystemGroupCoordinator<E>
{
    private final Map<String, E> mLookup = new HashMap<>();

    private final List<E> mSystems = new ArrayList<>();

    // Defines sort relationship
    private final Comparator<BaseSystem> mComparator;

    private boolean mSortNeeded = false;

    // Guards calls with external code to block adding/removing systems
    private boolean mPreventEdits = false;

    // Whether systems have been started
    private boolean mStarted;

    /**
     * Constructs a {@code BaseSystem}.
     *
     * @param comparator system sort order.
     * @throws NullPointerException if comparator is null.
     */
    public Domain(Comparator<BaseSystem> comparator)
    {
        checkNotNull(comparator);

        mComparator = comparator;
    }

    @Override
    public void startSystems()
    {
        if (mStarted) {
            throw new IllegalStateException("Systems have already started");
        }

        ensureSystemsAreSorted();
        mStarted = true;

        mPreventEdits = true;
        for (final BaseSystem system : mSystems) {
            system.start();
        }
        mPreventEdits = false;
    }

    @Override
    public void stopSystems()
    {
        checkSystemsHaveStarted();

        ensureSystemsAreSorted();
        mStarted = false;

        mPreventEdits = true;
        for (final BaseSystem system : mSystems) {
            system.stop();
        }
        mPreventEdits = false;
    }

    @Override
    public void pauseSystems(int reason)
    {
        checkSystemsHaveStarted();

        ensureSystemsAreSorted();

        mPreventEdits = true;
        for (final BaseSystem system : mSystems) {
            if (system.isPausable() && !system.isPaused()) {
                system.pause(reason);
            }
        }
        mPreventEdits = false;
    }

    @Override
    public void resumeSystems(int reason)
    {
        checkSystemsHaveStarted();

        ensureSystemsAreSorted();

        mPreventEdits = true;
        for (final BaseSystem system : mSystems) {
            if (system.isPaused()) {
                system.resume(reason);
            }
        }
        mPreventEdits = false;
    }

    @Override
    public void callWithSystems(Consumer<E> action)
    {
        checkNotNull(action);

        ensureSystemsAreSorted();

        mPreventEdits = true;
        for (final E system : mSystems) {
            action.accept(system);
        }
        mPreventEdits = false;
    }

    @Override
    public void callWithUnpausedSystems(Consumer<E> action)
    {
        checkNotNull(action);

        ensureSystemsAreSorted();

        mPreventEdits = true;
        for (final E system : mSystems) {
            if (system.isPausable() && !system.isPaused()) {
                action.accept(system);
            }
        }
        mPreventEdits = false;
    }

    @Override
    public void pauseSystem(String name, int reason)
    {
        checkNotNull(name);
        checkSystemsHaveStarted();

        final BaseSystem system = mLookup.get(name);
        checkElementExists(system);

        if (system.isPausable() && !system.isPaused()) {
            mPreventEdits = true;
            system.pause(reason);
            mPreventEdits = false;
        }
    }

    @Override
    public void resumeSystem(String name, int reason)
    {
        checkNotNull(name);
        checkSystemsHaveStarted();

        final BaseSystem system = mLookup.get(name);
        checkElementExists(system);

        if (system.isPaused()) {
            mPreventEdits = true;
            system.resume(reason);
            mPreventEdits = false;
        }
    }

    @Override
    public E getSystem(String name)
    {
        checkNotNull(name);

        final E system = mLookup.get(name);
        checkElementExists(system);

        return system;
    }

    /**
     * {@inheritDoc}
     *
     * @param name name.
     * @param system system.
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException if this method is called from {@link BaseSystem#onStart()},
     * {@link BaseSystem#onStop()}, {@link BaseSystem#onPause(int)}, or {@link BaseSystem#onResume(int)}.
     */
    @Override
    public void addSystem(String name, E system)
    {
        checkNotNull(name);
        checkNotNull(system);

        if (mLookup.containsKey(name)) {
            final String format = "System name \"%s\" already in use";
            throw new IllegalArgumentException(String.format(format, name));

        } else if (system.getCoordinator() != null) {
            throw new IllegalArgumentException("System has already been added to a Domain");

        }

        checkSystemsCanBeEdited();

        mLookup.put(name, system);
        mSystems.add(system);
        system.setCoordinator(this);

        mSortNeeded = true;
    }

    /**
     * {@inheritDoc}
     *
     * @param name name.
     * @throws NullPointerException {@inheritDoc}
     * @throws NoSuchElementException {@inheritDoc}
     * @throws IllegalStateException if this method is called from {@link BaseSystem#onStart()},
     * {@link BaseSystem#onStop()}, {@link BaseSystem#onPause(int)}, or {@link BaseSystem#onResume(int)}.
     */
    @Override
    public void removeSystem(String name)
    {
        checkNotNull(name);

        final BaseSystem system = mLookup.remove(name);
        checkElementExists(system);
        checkSystemsCanBeEdited();

        system.setCoordinator(null);

        mSortNeeded = true;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void ensureSystemsAreSorted()
    {
        if (mSortNeeded) {
            mSystems.sort(mComparator);
            mSortNeeded = false;
        }
    }

    private void checkSystemsCanBeEdited()
    {
        if (mPreventEdits) {
            throw new IllegalStateException("System cannot be added at this time");
        }
    }

    private void checkSystemsHaveStarted()
    {
        if (!mStarted) {
            throw new IllegalStateException("Systems have not yet started");
        }
    }

    private void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    private void checkElementExists(Object element)
    {
        if (element == null) {
            throw new NoSuchElementException();
        }
    }
}
