package cinnamon.engine.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Set of instructions to be executed in reaction to an axis. Each {@code AxisRule} consists of an
 * {@link EventListener} to execute, {@link MotionPreferences} to specify its requirements and details, and the
 * constants for which the reaction belongs. A {@code AxisRule}'s components cannot be changed.</p>
 *
 * @param <K> constant type owning the reaction.
 * @param <T> event type to react to.
 */
public final class AxisRule<K extends Enum<K>, T extends InputEvent> implements InputRule<K, T, MotionPreferences>
{
    // Actual reaction
    private final EventListener<T> mListener;

    // Constant
    private final K mAxis;

    // Requirements
    private final MotionPreferences mPrefs;

    private final int mPriority;

    private final int mHash;

    /**
     * <p>Constructs an {@code AxisRule}.</p>
     *
     * @param axis axis.
     * @param listener listener.
     * @param preferences preferences.
     * @param priority priority.
     * @throws NullPointerException if axis, listener, or preferences is null.
     */
    public AxisRule(K axis, EventListener<T> listener, MotionPreferences preferences, int priority)
    {
        checkNull(axis);
        checkNull(listener);
        checkNull(preferences);

        mListener = listener;
        mAxis = axis;
        mPrefs = preferences;
        mPriority = priority;

        mHash = computeHash();
    }

    @Override
    public List<K> getConstants()
    {
        final List<K> list = new ArrayList<>(1);
        list.add(mAxis);
        return Collections.unmodifiableList(list);
    }

    @Override
    public EventListener<T> getListener()
    {
        return mListener;
    }

    @Override
    public MotionPreferences getPreferences()
    {
        return mPrefs;
    }

    @Override
    public int getPriority()
    {
        return mPriority;
    }

    @Override
    public int hashCode()
    {
        return mHash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final AxisRule other = (AxisRule) obj;

        return getListener() == other.getListener()
                && getPreferences() == other.getPreferences()
                && getConstants().equals(other.getConstants())
                && getPriority() == other.getPriority();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private int computeHash()
    {
        int hash = 17 * 31 + mListener.hashCode();
        hash = 31 * hash + mPrefs.hashCode();
        hash = 31 * hash + mAxis.hashCode();
        return 31 * hash + Integer.hashCode(mPriority);
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
