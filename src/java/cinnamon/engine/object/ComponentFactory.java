package cinnamon.engine.object;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

/**
 * {@code Component} producer with the ability to create from prototypes.
 */
public final class ComponentFactory
{
    // Component producers
    private final Map<Class<? extends Component>, Source> mSources = new HashMap<>();

    // Named templates to copy from
    private final Map<String, Component> mPrototypes = new HashMap<>();

    /**
     * Constructs a {@code ComponentFactory}.
     */
    public ComponentFactory()
    {
        super();
    }

    /**
     * Sets a prototype.
     *
     * <p>If {@code prototype} is {@code null}, any set prototype with the same name is removed.</p>
     *
     * @param name prototype name.
     * @param prototype component prototype.
     * @param <T> type of prototype.
     * @throws NullPointerException if {@code name} is {@code null}.
     */
    public <T extends Component> void setPrototype(String name, T prototype)
    {
        requireNonNull(name);

        mPrototypes.compute(name, (key, value) -> prototype);
    }

    /**
     * Sets a component's source of new instances.
     *
     * @param cls component class.
     * @param src component source.
     * @param <T> component type.
     * @throws NullPointerException if {@code cls} or {@code src} is {@code null}.
     */
    public <T extends Component> void setSource(Class<T> cls, Source<T> src)
    {
        requireNonNull(cls);
        requireNonNull(src);

        mSources.put(cls, src);
    }

    /**
     * Creates a component of the specified class.
     *
     * @param cls component class.
     * @param <T> component type.
     * @return new component.
     * @throws NullPointerException if {@code cls} is {@code null}.
     * @throws IllegalStateException if no source has been set for the component class.
     */
    public <T extends Component> T createComponent(Class<T> cls)
    {
        requireNonNull(cls);

        @SuppressWarnings("unchecked")
        final Source<T> src = mSources.get(cls);
        checkSourceExists(src);

        return src.createComponent();
    }

    /**
     * Creates a component after the specified prototype.
     *
     * @param prototype prototype name.
     * @return new component.
     * @throws NullPointerException if {@code prototype} is {@code null}.
     * @throws NoSuchElementException if {@code prototype} is unrecognized.
     * @throws IllegalStateException if no component source has been set.
     */
    public Component createComponent(String prototype)
    {
        requireNonNull(prototype);

        final Component protoComp = mPrototypes.get(prototype);
        checkPrototypeExists(prototype, protoComp);

        final Source src = mSources.get(protoComp.getClass());
        checkSourceExists(src);

        // Configure new instance after prototype
        final Component newInstance = src.createComponent();
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

    private static void checkSourceExists(Source src)
    {
        if (src == null) {
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

    /**
     * Instantiates new {@code Component} instances.
     *
     * @param <T> type of component.
     */
    public interface Source<T extends Component>
    {
        /**
         * Creates a new component instance.
         *
         * @return new instance.
         */
        T createComponent();
    }
}
