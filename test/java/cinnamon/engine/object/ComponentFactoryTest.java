package cinnamon.engine.object;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.NoSuchElementException;

public class ComponentFactoryTest
{
    private static final String PROTOTYPE_NAME = "dummy";

    private ComponentFactory mFactory;

    @Before
    public void setUp()
    {
        mFactory = new ComponentFactory();
    }

    @After
    public void tearDown()
    {
        mFactory = null;
    }

    @Test
    public void testSetPrototypeCompletes()
    {
        mFactory.setPrototype(PROTOTYPE_NAME, new DummyComponent());
    }

    @Test
    public void testSetPrototypeNullPrototypeCompletes()
    {
        mFactory.setPrototype(PROTOTYPE_NAME, null);
    }

    @Test (expected = NullPointerException.class)
    public void testSetPrototypeNPEName()
    {
        mFactory.setPrototype(null, new DummyComponent());
    }

    @Test
    public void testSetSourceCompletes()
    {
        mFactory.setSource(DummyComponent.class, DummyComponent::new);
    }

    @Test (expected = NullPointerException.class)
    public void testSetSourceNPEClass()
    {
        mFactory.setSource(null, DummyComponent::new);
    }

    @Test (expected = NullPointerException.class)
    public void testSetSourceNPESource()
    {
        mFactory.setSource(DummyComponent.class, null);
    }

    @Test
    public void testCreateComponentReturnsComponentOfSameClass()
    {
        setSource();

        final Component component = mFactory.createComponent(DummyComponent.class);

        Assert.assertEquals(DummyComponent.class, component.getClass());
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateComponentISENoSourceSet()
    {
        mFactory.createComponent(DummyComponent.class);
    }

    @Test
    public void testCreateComponentFromPrototypeReturnsComponentOfSameClass()
    {
        setSource();
        setPrototype();

        final Component component = mFactory.createComponent(PROTOTYPE_NAME);

        Assert.assertEquals(DummyComponent.class, component.getClass());
    }

    @Test (expected = NullPointerException.class)
    public void testCreateComponentFromPrototypeNPEPrototype()
    {
        setSource();
        setPrototype();

        final String prototype = null;
        mFactory.createComponent(prototype);
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateComponentFromPrototypeISENoSourceSet()
    {
        setPrototype();

        mFactory.createComponent(PROTOTYPE_NAME);
    }

    @Test (expected = NoSuchElementException.class)
    public void testCreateComponentFromPrototypeNSEUnrecognizedPrototypeName()
    {
        setSource();

        mFactory.createComponent(PROTOTYPE_NAME);
    }

    private void setSource()
    {
        mFactory.setSource(DummyComponent.class, DummyComponent::new);
    }

    private void setPrototype()
    {
        mFactory.setPrototype(PROTOTYPE_NAME, new DummyComponent());
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
