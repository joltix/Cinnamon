package cinnamon.engine.object;

import cinnamon.engine.utils.Transform;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Set of {@link Component} elements describing a game object.
 *
 * <p>An {@code Entity} can only contain a single component per {@code Component} class.</p>
 *
 * <p>The identifier from {@code getId()} is immutable and is used to uniquely name an {@code Entity} amongst others
 * created by the same {@link EntityManager}.</p>
 *
 * <p>Entities should only be used so long as {@code isAlive()} returns {@code true} as systems may dispose of
 * destroyed entities.</p>
 */
public final class Entity
{
    private static final int INITIAL_COMPONENT_CAPACITY = 4;

    private final Transform mTransform = new Transform();

    private final List<Component> mComponents = new ArrayList<>(INITIAL_COMPONENT_CAPACITY);

    private final int mId;

    private boolean mAlive = true;

    Entity(int id)
    {
        mId = id;
    }

    /**
     * Gets a component with the specified class.
     *
     * @param cls component class.
     * @param <T> type of component.
     * @return component or null if no component has the same class.
     * @throws NullPointerException if cls is null.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Class<T> cls)
    {
        requireNonNull(cls);

        T target = null;

        for (final Component component : mComponents) {
            if (component.getClass() == cls) {
                target = (T) component;
                break;
            }
        }

        return target;
    }

    /**
     * Adds a component.
     *
     * @param component component.
     * @return previous component of the same class or null if there was no previous instance.
     * @throws NullPointerException if component is null.
     */
    public Component addComponent(Component component)
    {
        requireNonNull(component);

        final Component removed = removeComponent(component.getClass());
        mComponents.add(component);
        component.attach();

        return removed;
    }

    /**
     * Removes a component.
     *
     * @param cls component class.
     * @param <T> type of component.
     * @return removed component or null if no component has the same class.
     * @throws NullPointerException if cls is null.
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> T removeComponent(Class<T> cls)
    {
        requireNonNull(cls);

        final Iterator<Component> iter = mComponents.iterator();

        // Remove the first component with the same class (should only be one at most)
        while (iter.hasNext()) {

            final Component component = iter.next();

            if (component.getClass() == cls) {
                component.detach();
                iter.remove();
                return (T) component;
            }
        }

        return null;
    }

    /**
     * Removes all components.
     */
    public void removeAllComponents()
    {
        mComponents.forEach(Component::detach);
        mComponents.clear();
    }

    /**
     * Returns {@code true} if this entity has a component with the specified class.
     *
     * @param cls component class.
     * @return true if a matching component is attached.
     * @throws NullPointerException if cls is null.
     */
    public boolean containsComponent(Class<? extends Component> cls)
    {
        requireNonNull(cls);

        return getComponent(cls) != null;
    }

    /**
     * Gets the number of components.
     *
     * @return component count.
     */
    public int getComponentCount()
    {
        return mComponents.size();
    }

    /**
     * Gets this entity's orientation.
     *
     * @return transform.
     */
    public Transform getTransform()
    {
        return mTransform;
    }

    /**
     * Returns {@code true} if this entity has not been destroyed.
     *
     * @return true if still alive.
     */
    public boolean isAlive()
    {
        return mAlive;
    }

    /**
     * Gets the instance id.
     *
     * @return id.
     */
    public int getId()
    {
        return mId;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    void destroy()
    {
        mAlive = false;
    }
}
