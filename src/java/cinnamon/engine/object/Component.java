package cinnamon.engine.object;

import cinnamon.engine.utils.Copier;

/**
 * Base class for introducing new functionality to an {@link Entity}.
 *
 * <p>While an {@code Entity} can only use one {@code Component} of the same class at a time, each {@code Component}
 * can be attached to more than one {@code Entity}. This allows for certain situations such as characters with shared
 * health.</p>
 */
public abstract class Component implements Copier<Component>
{
    // Number of objects using this component
    private int mUsers;

    /**
     * Constructs a {@code Component}.
     */
    public Component()
    {
        super();
    }

    /**
     * Returns {@code true} if the component is attached to a game object.
     *
     * @return true if in use.
     */
    public final boolean isAttached()
    {
        return mUsers > 0;
    }

    /**
     * Called when this component is added to a game object.
     */
    protected abstract void onAttach();

    /**
     * Called when this component is removed from a game object.
     */
    protected abstract void onDetach();

    final void attach()
    {
        mUsers++;
        onAttach();
    }

    final void detach()
    {
        assert (mUsers > 0);

        mUsers--;
        onDetach();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }
}
