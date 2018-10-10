package cinnamon.engine.object;

import cinnamon.engine.object.ComponentManagerTest.DummyComponent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class EntityTest
{
    private Entity mEntity;

    @Before
    public void setUp()
    {
        mEntity = new Entity(0);
    }

    @After
    public void tearDown()
    {
        mEntity = null;
    }

    @Test
    public void testAddComponentReturnsNull()
    {
        Assert.assertNull(mEntity.addComponent(new DummyComponent()));
    }

    @Test
    public void testAddComponentReturnsPreviousComponent()
    {
        final DummyComponent oldComponent = new DummyComponent();
        mEntity.addComponent(oldComponent);

        final DummyComponent newComponent = new DummyComponent();
        final Component removed = mEntity.addComponent(newComponent);

        Assert.assertSame(oldComponent, removed);
    }

    @Test (expected = NullPointerException.class)
    public void testAddComponentNPEComponent()
    {
        mEntity.addComponent(null);
    }

    @Test
    public void testRemoveComponentReturnsNullWhenNothingIsRemoved()
    {
        Assert.assertNull(mEntity.removeComponent(DummyComponent.class));
    }

    @Test
    public void testRemoveComponentReturnsRemovedComponent()
    {
        final DummyComponent component = new DummyComponent();

        mEntity.addComponent(component);
        final DummyComponent removed = mEntity.removeComponent(DummyComponent.class);

        Assert.assertNotNull(removed);
        Assert.assertSame(component, removed);
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveComponentNPEClass()
    {
        mEntity.removeComponent(null);
    }

    @Test
    public void testGetComponentReturnsAddedComponent()
    {
        mEntity.addComponent(new DummyComponent());

        final DummyComponent component = mEntity.getComponent(DummyComponent.class);

        Assert.assertNotNull(component);
        Assert.assertSame(DummyComponent.class, component.getClass());
    }

    @Test
    public void testGetComponentReturnsNullWhenNoMatchingClass()
    {
        Assert.assertNull(mEntity.getComponent(DummyComponent.class));
    }

    @Test (expected = NullPointerException.class)
    public void testGetComponentNPEClass()
    {
        mEntity.getComponent(null);
    }

    @Test
    public void testContainsComponentReturnsTrue()
    {
        mEntity.addComponent(new DummyComponent());

        Assert.assertTrue(mEntity.containsComponent(DummyComponent.class));
    }

    @Test
    public void testContainsComponentReturnsFalse()
    {
        Assert.assertFalse(mEntity.containsComponent(DummyComponent.class));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsComponentNPEClass()
    {
        mEntity.containsComponent(null);
    }

    @Test
    public void testGetComponentCountIncrementsAfterComponentAdded()
    {
        final int previous = mEntity.getComponentCount();

        mEntity.addComponent(new DummyComponent());

        Assert.assertEquals(previous + 1, mEntity.getComponentCount());
    }

    @Test
    public void testGetComponentCountDecrementsAfterComponentRemoved()
    {
        mEntity.addComponent(new DummyComponent());
        final int previous = mEntity.getComponentCount();

        mEntity.removeComponent(DummyComponent.class);

        Assert.assertEquals(previous - 1, mEntity.getComponentCount());
    }

    @Test
    public void testGetComponentCountReturnsZeroAfterAllComponentsRemoved()
    {
        mEntity.addComponent(new DummyComponent());

        mEntity.removeAllComponents();

        Assert.assertEquals(0, mEntity.getComponentCount());
    }
}
