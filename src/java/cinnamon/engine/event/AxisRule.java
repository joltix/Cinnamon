package cinnamon.engine.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Set of instructions to be executed in reaction to an axis. Each {@code AxisRule} consists of an
 * {@link EventListener} to execute, {@link AxisPreferences} to specify its requirements and details, and the
 * constants for which the reaction belongs. A {@code AxisRule}'s components cannot be changed.</p>
 *
 * @param <K> constant type owning the reaction.
 * @param <T> event type to react to.
 */
public final class AxisRule<K extends Enum<K>, T extends InputEvent> implements InputRule<K, T, AxisPreferences>
{
    // Actual reaction
    private final EventListener<T> mListener;

    // Requirements
    private final AxisPreferences mPrefs;

    // Constant
    private final K mAxis;

    private final int mHash;

    /**
     * <p>Constructs an {@code AxisRule}.</p>
     *
     * @param axis axis.
     * @param listener listener.
     * @param preferences preferences.
     * @throws NullPointerException if axis, listener, or preferences is null.
     */
    public AxisRule(K axis, EventListener<T> listener, AxisPreferences preferences)
    {
        checkNull(axis);
        checkNull(listener);
        checkNull(preferences);

        mListener = listener;
        mAxis = axis;
        mPrefs = preferences;

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
    public AxisPreferences getPreferences()
    {
        return mPrefs;
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
                && getConstants().equals(other.getConstants());
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
        return 31 * hash + mAxis.hashCode();
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}