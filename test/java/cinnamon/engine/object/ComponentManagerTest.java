package cinnamon.engine.object;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class ComponentManagerTest
{
    private static final String PROTOTYPE_NAME = "dummy";

    private ComponentManager mManager;

    @Before
    public void setUp()
    {
        mManager = new ComponentManager();
    }

    @After
    public void tearDown()
    {
        mManager = null;
    }

    @Test
    public void testGetPrototypesReturnsEmptyMapWhenNoSetPrototypes()
    {
        Assert.assertTrue(mManager.getPrototypes().isEmpty());
    }

    @Test
    public void testGetPrototypesReturnsSetPrototypes()
    {
        final Map<String, Component> prototypes = createPrototypeMap(new DummyComponent());
        mManager.setPrototypes(prototypes);

        Assert.assertEquals(prototypes, mManager.getPrototypes());
    }

    @Test
    public void testSetPrototypesOverwritesPreviouslySet()
    {
        setPrototype();

        mManager.setPrototypes(new HashMap<>());
        Assert.assertTrue(mManager.getPrototypes().isEmpty());
    }

    @Test (expected = NullPointerException.class)
    public void testSetPrototypesNPEMap()
    {
        mManager.setPrototypes(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetPrototypesIAENameSpecifiesNullComponent()
    {
        mManager.setPrototypes(createPrototypeMap(null));
    }

    @Test
    public void testSetComponentSource()
    {
        mManager.setSource(DummyComponent.class, DummyComponent::new);
    }

    @Test (expected = NullPointerException.class)
    public void testSetComponentSourceNPEClass()
    {
        mManager.setSource(null, DummyComponent::new);
    }

    @Test (expected = NullPointerException.class)
    public void testSetComponentSourceNPEFactory()
    {
        mManager.setSource(DummyComponent.class, null);
    }

    @Test
    public void testCreateComponentReturnsComponentOfSameClass()
    {
        setSource();

        final Component component = mManager.createComponent(DummyComponent.class);

        Assert.assertEquals(DummyComponent.class, component.getClass());
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateComponentISENoSourceSet()
    {
        mManager.createComponent(DummyComponent.class);
    }

    @Test
    public void testCreateComponentFromPrototype()
    {
        setSource();
        setPrototype();

        mManager.createComponent(PROTOTYPE_NAME);
    }

    @Test
    public void testCreateComponentFromPrototypeReturnsComponentOfSameClass()
    {
        setSource();
        setPrototype();

        final Component component = mManager.createComponent(PROTOTYPE_NAME);

        Assert.assertEquals(DummyComponent.class, component.getClass());
    }

    @Test (expected = NullPointerException.class)
    public void testCreateComponentFromPrototypeNPEPrototype()
    {
        setSource();
        setPrototype();

        final String prototype = null;
        mManager.createComponent(prototype);
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateComponentFromPrototypeISENoSourceSet()
    {
        setPrototype();

        mManager.createComponent(PROTOTYPE_NAME);
    }

    @Test (expected = NoSuchElementException.class)
    public void testCreateComponentFromPrototypeNSEUnrecognizedPrototypeName()
    {
        setSource();

        mManager.createComponent(PROTOTYPE_NAME);
    }

    private void setSource()
    {
        mManager.setSource(DummyComponent.class, DummyComponent::new);
    }

    private void setPrototype()
    {
        mManager.setPrototypes(createPrototypeMap(new DummyComponent()));
    }

    private Map<String, Component> createPrototypeMap(Component component)
    {
        final Map<String, Component> prototypes = new HashMap<>();
        prototypes.put(PROTOTYPE_NAME, component);
        return prototypes;
    }

    public static class DummyComponent extends Component
    {
        @Override
        public void copy(Component object) { }

        @Override
        protected void onAttach() { }

        @Override
        protected void onDetach() { }
    }
}
