package cinnamon.engine.core;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages the lifecycles for a list of {@code GameSystem}s from start to stop and any number of pauses and resumes
 * in-between.
 *
 * @param <E> type of system.
 */
public final class Domain<E extends GameSystem> implements WritableSystemDirectory<E>, SystemCoordinator<E>,
        SystemGroupCoordinator<E>
{
    private final Map<String, E> mLookup = new HashMap<>();

    private final List<E> mSystems = new ArrayList<>();

    // Defines sort relationship
    private final Comparator<GameSystem> mComparator;

    private boolean mSortNeeded = false;

    // Whether systems have been started
    private boolean mStarted;

    /**
     * Constructs a {@code GameSystem}.
     *
     * @param comparator system sort order.
     * @throws NullPointerException if comparator is null.
     */
    public Domain(Comparator<GameSystem> comparator)
    {
        checkNull(comparator);

        mComparator = comparator;
    }

    @Override
    public void startSystems()
    {
        if (mSystems.isEmpty()) {
            return;
        }
        if (mStarted) {
            throw new IllegalStateException("Systems have already started");
        }

        if (mSortNeeded) {
            ensureSystemsAreSorted();
        }

        mStarted = true;

        for (final GameSystem system : mSystems) {
            system.onStart();
        }
    }

    @Override
    public void stopSystems()
    {
        checkSystemsHaveStarted();

        if (mSortNeeded) {
            ensureSystemsAreSorted();
        }

        mStarted = false;

        for (final GameSystem system : mSystems) {
            system.onStop();
        }
    }

    @Override
    public void pauseSystems(int reason)
    {
        checkSystemsHaveStarted();

        if (mSortNeeded) {
            ensureSystemsAreSorted();
        }

        for (final GameSystem system : mSystems) {
            if (!system.isPaused()) {
                system.pause(reason);
            }
        }
    }

    @Override
    public void resumeSystems(int reason)
    {
        checkSystemsHaveStarted();

        if (mSortNeeded) {
            ensureSystemsAreSorted();
        }

        for (final GameSystem system : mSystems) {
            if (system.isPaused()) {
                system.resume(reason);
            }
        }
    }

    @Override
    public void callWithSystems(Consumer<E> action)
    {
        checkNull(action);

        if (mSortNeeded) {
            ensureSystemsAreSorted();
        }

        for (final E system : mSystems) {
            action.accept(system);
        }
    }

    @Override
    public void pauseSystem(String name, int reason)
    {
        checkNull(name);
        checkSystemsHaveStarted();

        final GameSystem system = mLookup.get(name);
        checkNoSuchElement(system);

        if (!system.isPaused()) {
            system.pause(reason);
        }
    }

    @Override
    public void resumeSystem(String name, int reason)
    {
        checkNull(name);
        checkSystemsHaveStarted();

        final GameSystem system = mLookup.get(name);
        checkNoSuchElement(system);

        if (system.isPaused()) {
            system.resume(reason);
        }
    }

    @Override
    public E getSystem(String name)
    {
        checkNull(name);

        final E system = mLookup.get(name);
        checkNoSuchElement(system);

        return system;
    }

    @Override
    public void addSystem(String name, E system)
    {
        checkNull(name);
        checkNull(system);

        if (mLookup.containsKey(name)) {
            final String format = "System name \"%s\" already in use";
            throw new IllegalArgumentException(String.format(format, name));

        } else if (system.getCoordinator() != null) {
            throw new IllegalArgumentException("System has already been added to a Domain");
        }

        mLookup.put(name, system);
        mSystems.add(system);
        system.setCoordinator(this);

        mSortNeeded = true;
    }

    @Override
    public void removeSystem(String name)
    {
        checkNull(name);

        final GameSystem system = mLookup.remove(name);
        checkNoSuchElement(system);

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
        mSystems.sort(mComparator);
        mSortNeeded = false;
    }

    private void checkSystemsHaveStarted()
    {
        if (!mStarted) {
            throw new IllegalStateException("Systems have not yet started");
        }
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    private void checkNoSuchElement(Object object)
    {
        if (object == null) {
            throw new NoSuchElementException();
        }
    }
}
