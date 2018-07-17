package cinnamon.engine.core;

import cinnamon.engine.utils.Prioritizable;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for user-written game systems, deferring its lifecycle's management to a {@link Domain}.
 *
 * <h3>Lifecycle</h3>
 * <p>While the lifecycle generally transitions to an increasing step number, step 3 can cycle back to step 2 when the
 * framework (or another game system) requests a temporary cease to operations.</p>
 *
 * <ol>
 *     <li>start</li>
 *     <li>pause</li>
 *     <li>resume</li>
 *     <li>stop</li>
 * </ol>
 *
 * <p>This lifecycle is heavily influenced by the Android <i>Activity</i> lifecycle but with some operational changes -
 * one of the major differences being that <i>the pause state is not expected to occur prior to stopping</i>.
 * Pausing a {@code GameSystem} delivers an intention to explicitly resume where the system left off.</p>
 *
 * <p>While the start, pause, resume, and stop states outline a general progression, subclasses are expected to
 * insert additional steps among those provided, as appropriate to the system's environment.</p>
 */
public abstract class GameSystem implements Prioritizable
{
    /**
     * Possible states depending on the most recent call to either {@link #start()}, {@link #stop()},
     * {@link #pause(int)}, or {@link #resume(int)}.
     */
    private enum ProcessState
    {
        PAUSED,
        RUNNING,
        STOPPED
    }

    private final List<OnPauseListener> mOnPauseListeners = new ArrayList<>();

    private final List<OnResumeListener> mOnResumeListeners = new ArrayList<>();

    // Relative to systems of the same type
    private final int mPriority;

    // Coordinator
    private SystemCoordinator mCoordinator;

    // Whether running, paused, or stopped
    private ProcessState mState;

    /**
     * Constructs a {@code GameSystem}.
     *
     * @param priority priority.
     */
    protected GameSystem(int priority)
    {
        mPriority = priority;
    }

    @Override
    public final int getPriority()
    {
        return mPriority;
    }

    /**
     * Checks if the system is not receiving lifecycle notifications.
     *
     * @return true if paused.
     */
    public final boolean isPaused()
    {
        return mState == ProcessState.PAUSED;
    }

    /**
     * Adds a listener to be notified when the system is paused.
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public final void addOnPauseListener(OnPauseListener listener)
    {
        checkNull(listener);

        mOnPauseListeners.add(listener);
    }

    /**
     * Removes a pause listener.
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public final void removeOnPauseListener(OnPauseListener listener)
    {
        checkNull(listener);

        mOnPauseListeners.remove(listener);
    }

    /**
     * Adds a listener to be notified when the system is resumed.
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public final void addOnResumeListener(OnResumeListener listener)
    {
        checkNull(listener);

        mOnResumeListeners.add(listener);
    }

    /**
     * Removes a resume listener.
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public final void removeOnResumeListener(OnResumeListener listener)
    {
        checkNull(listener);

        mOnResumeListeners.remove(listener);
    }

    /**
     * Called when the domain starts the system.
     */
    protected abstract void onStart();

    /**
     * Called when the domain pauses the system.
     *
     * @param reason reason.
     */
    protected abstract void onPause(int reason);

    /**
     * Called when the domain resumes the system.
     *
     * @param reason reason.
     */
    protected abstract void onResume(int reason);

    /**
     * Called when the domain stops the system.
     */
    protected abstract void onStop();

    /**
     * Gets a coordinator allowing pausing and resuming other systems.
     *
     * @return coordinator.
     */
    protected final SystemCoordinator getCoordinator()
    {
        return mCoordinator;
    }

    @Override
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Lets the system know that its service is now needed and to prepare. {@link #onStart()} will be called.
     */
    final void start()
    {
        mState = ProcessState.RUNNING;
        onStart();
    }

    /**
     * Lets the system know that it will not be receiving notifications. This freeze will be lifted on the next
     * call to {@link #resume(int)}. {@link #onPause(int)} will be called followed by added pause listeners.
     *
     * @param reason reason.
     */
    final void pause(int reason)
    {
        mState = ProcessState.PAUSED;
        onPause(reason);

        notifyOnPauseListeners(reason);
    }

    /**
     * Lets the system know that it will begin receiving notifications again. {@link #onResume(int)} will be
     * called.
     *
     * @param reason reason.
     */
    final void resume(int reason)
    {
        mState = ProcessState.RUNNING;
        onResume(reason);

        notifyOnResumeListeners(reason);
    }

    /**
     * Lets the system know that its service is no longer needed and that it should perform clean up operations.
     * {@link #onStop()} will be called.
     */
    final void stop()
    {
        mState = ProcessState.STOPPED;
        onStop();
    }

    /**
     * Sets the coordinator.
     *
     * @param coordinator domain.
     */
    final void setCoordinator(SystemCoordinator coordinator)
    {
        mCoordinator = coordinator;
    }

    private void notifyOnPauseListeners(int reason)
    {
        for (final OnPauseListener listener : mOnPauseListeners) {
            listener.onPause(this, reason);
        }
    }

    private void notifyOnResumeListeners(int reason)
    {
        for (final OnResumeListener listener : mOnResumeListeners) {
            listener.onResume(this, reason);
        }
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    public interface OnPauseListener
    {
        /**
         * Called when the system is paused.
         *
         * @param system system.
         * @param reason reason.
         */
        void onPause(GameSystem system, int reason);
    }

    public interface OnResumeListener
    {
        /**
         * Called when the system is resumed.
         *
         * @param system system.
         * @param reason reason.
         */
        void onResume(GameSystem system, int reason);
    }
}
