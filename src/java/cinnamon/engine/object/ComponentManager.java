package cinnamon.engine.object;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * {@code Component} producer with the ability to create from prototypes.
 */
public final class ComponentManager
{
    // Component producers
    private final Map<Class<? extends Component>, ComponentFactory> mSources = new HashMap<>();

    // Named templates to copy from
    private final Map<String, Component> mPrototypes = new HashMap<>();

    /**
     * Constructs a {@code ComponentManager}.
     */
    public ComponentManager()
    {
        super();
    }

    /**
     * Gets a map of prototypes. Changes to the map have no effect on set prototypes.
     *
     * @return map of prototypes.
     */
    public Map<String, Component> getPrototypes()
    {
        return new HashMap<>(mPrototypes);
    }

    /**
     * Sets a map of prototypes.
     *
     * <p>Changes to the map have no effect on set prototypes. Previously set prototypes are removed.</p>
     *
     * @param prototypes named prototypes.
     * @throws NullPointerException if prototypes is null.
     * @throws IllegalArgumentException if a prototype name specifies a null component.
     */
    public void setPrototypes(Map<String, Component> prototypes)
    {
        checkNotNull(prototypes);
        checkNoNullValues(prototypes);

        mPrototypes.clear();
        mPrototypes.putAll(prototypes);
    }

    /**
     * Sets a component's source of new instances.
     *
     * @param cls component class.
     * @param factory component source.
     * @param <T> component type.
     * @throws NullPointerException if cls or factory is null.
     */
    public <T extends Component> void setSource(Class<T> cls, ComponentFactory<T> factory)
    {
        checkNotNull(cls);
        checkNotNull(factory);

        mSources.put(cls, factory);
    }

    /**
     * Creates a component of the specified class.
     *
     * @param cls component class.
     * @param <T> component type.
     * @return new component.
     * @throws NullPointerException if cls is null.
     * @throws IllegalStateException if no source has been set for the component class.
     */
    public <T extends Component> T createComponent(Class<T> cls)
    {
        checkNotNull(cls);

        @SuppressWarnings("unchecked")
        final ComponentFactory<T> factory = mSources.get(cls);
        checkSourceExists(factory);

        return factory.createComponent();
    }

    /**
     * Creates a component after the specified prototype.
     *
     * @param prototype prototype name.
     * @return new component.
     * @throws NullPointerException if prototype is null.
     * @throws NoSuchElementException if prototype is unrecognized.
     * @throws IllegalStateException if no component source has been set
     */
    public Component createComponent(String prototype)
    {
        checkNotNull(prototype);

        final Component protoComp = mPrototypes.get(prototype);
        checkPrototypeExists(prototype, protoComp);

        @SuppressWarnings("unchecked")
        final ComponentFactory factory = mSources.get(protoComp.getClass());
        checkSourceExists(factory);

        // Configure new instance after prototype
        final Component newInstance = factory.createComponent();
        newInstance.copy(protoComp);

        return newInstance;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    Map<String, Component> getReadOnlyPrototypes()
    {
        return Collections.unmodifiableMap(mPrototypes);
    }

    private void checkNoNullValues(Map<String, Component> prototypes)
    {
        prototypes.forEach((name, prototype) ->
        {
            if (prototype == null) {
                final String format = "Prototype \'%s\' specifies a null component";
                throw new IllegalArgumentException(String.format(format, name));
            }
        });
    }

    private static void checkSourceExists(ComponentFactory factory)
    {
        if (factory == null) {
            throw new IllegalStateException("Component source has not been set");
        }
    }

    private static void checkPrototypeExists(String name, Component prototype)
    {
        if (prototype == null) {
            final String format = "Unrecognized prototype \'%s\'";
            throw new NoSuchElementException(String.format(format, name));
        }
    }

    private static void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Instantiates new {@code Component} instances.
     *
     * @param <T> type of component.
     */
    public interface ComponentFactory<T extends Component>
    {
        /**
         * Creates a new component instance.
         *
         * @return new instance.
         */
        T createComponent();
    }
}
