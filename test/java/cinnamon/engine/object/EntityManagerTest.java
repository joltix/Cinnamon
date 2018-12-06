package cinnamon.engine.object;

import cinnamon.engine.object.ComponentFactoryTest.DummyComponent;
import cinnamon.engine.object.EntityManager.Tuner;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class EntityManagerTest
{
    private static final String CONFIGURATION = "dummy_config";

    private static final String[] COMPONENT_PROTOTYPES = {"comp1", "comp2", "comp3"};

    private static final String UNRECOGNIZED_COMPONENT_PROTOTYPE = "dummy_comp";

    private static final String UNRECOGNIZED_CONFIGURATION = "unrecognized_config";

    private EntityManager mManager;

    private Tuner mTuner;

    @Before
    public void setUp()
    {
        mTuner = new EntityManager.Tuner(0);
        mManager = mTuner.getManager();

        createComponentPrototypes();
    }

    @After
    public void tearDown()
    {
        mManager = null;
    }

    @Test
    public void testGetConfigurationsReturnsEmptyMap()
    {
        Assert.assertNotNull(mManager.getConfigurations());
    }

    @Test
    public void testGetConfigurationsReturnsEquivalentSetMap()
    {
        final Map<String, String[]> configs = createConfigs();

        mManager.setConfigurations(configs);

        Assert.assertEquals(configs, mManager.getConfigurations());
    }

    @Test
    public void testSetConfigurations()
    {
        mManager.setConfigurations(new HashMap<>());
    }

    @Test (expected = NullPointerException.class)
    public void testSetConfigurationsNPEMap()
    {
        mManager.setConfigurations(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testSetConfigurationsNSEUnrecognizedComponentPrototype()
    {
        final Map<String, String[]> configs = createConfigs(new String[] {UNRECOGNIZED_COMPONENT_PROTOTYPE});

        mManager.setConfigurations(configs);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetConfigurationsIAEComponentPrototypesArrayIsNull()
    {
        final Map<String, String[]> configs = createConfigs(null);

        mManager.setConfigurations(configs);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetConfigurationsIAEComponentPrototypesArrayHasAllNullElements()
    {
        final Map<String, String[]> configs = createConfigs(new String[] {null});

        mManager.setConfigurations(configs);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetConfigurationsIAEComponentPrototypesArrayHasLengthZero()
    {
        final Map<String, String[]> configs = createConfigs(new String[0]);

        mManager.setConfigurations(configs);
    }

    @Test
    public void testCreateEntityDoesNotReturnNull()
    {
        Assert.assertNotNull(mManager.createEntity());
    }

    @Test
    public void testCreateEntityReturnsEntityWithoutComponents()
    {
        final Entity entity = mManager.createEntity();

        Assert.assertEquals(0, entity.getComponentCount());
    }

    @Test
    public void testCreateEntityCanCreateMaximumNumberOfEntitiesWithoutDestroying()
    {
        final int sz = EntityManager.MAXIMUM_ENTITY_COUNT;
        for (int i = 0; i < sz; i++) {
            mManager.createEntity();
        }
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateEntityISELivingEntityLimitReached()
    {
        final int sz = EntityManager.MAXIMUM_ENTITY_COUNT + 1;
        for (int i = 0; i < sz; i++) {
            mManager.createEntity();
        }
    }

    @Ignore ("Takes too long for general testing")
    @Test (expected = IllegalStateException.class)
    public void testCreateEntityISENoAvailableIds()
    {
        while (mTuner.getAvailableIdCount() >= 0L) {
            mManager.destroyEntity(mManager.createEntity().getId());
            mTuner.removeDestroyedEntities();
        }
    }

    @Test
    public void testCreateEntityWithConfigurationDoesNotReturnNull()
    {
        mManager.setConfigurations(createConfigs());

        Assert.assertNotNull(mManager.createEntity(CONFIGURATION));
    }

    @Test
    public void testCreateEntityWithConfigurationCanCreateMaximumNumberOfEntitiesWithoutDestroying()
    {
        mManager.setConfigurations(createConfigs());

        final int sz = EntityManager.MAXIMUM_ENTITY_COUNT;
        for (int i = 0; i < sz; i++) {
            mManager.createEntity(CONFIGURATION);
        }
    }

    @Test (expected = IllegalStateException.class)
    public void testCreateEntityWithConfigurationISELivingEntityLimitReached()
    {
        mManager.setConfigurations(createConfigs());

        final int sz = EntityManager.MAXIMUM_ENTITY_COUNT + 1;
        for (int i = 0; i < sz; i++) {
            mManager.createEntity(CONFIGURATION);
        }
    }

    @Test (expected = NullPointerException.class)
    public void testCreateEntityWithConfigurationNPEConfiguration()
    {
        mManager.createEntity(null);
    }

    @Test (expected = NoSuchElementException.class)
    public void testCreateEntityWithConfigurationNSEUnrecognizedConfiguration()
    {
        mManager.createEntity(UNRECOGNIZED_CONFIGURATION);
    }

    @Test (expected = NoSuchElementException.class)
    public void testCreateEntityWithConfigurationNSEUnrecognizedComponentPrototype()
    {
        mManager.createEntity(CONFIGURATION);
    }

    @Ignore ("Takes too long for general testing")
    @Test (expected = IllegalStateException.class)
    public void testCreateEntityWithConfigurationISENoAvailableIds()
    {
        mManager.setConfigurations(createConfigs());

        while (mTuner.getAvailableIdCount() >= 0L) {
            mManager.destroyEntity(mManager.createEntity(CONFIGURATION).getId());
            mTuner.removeDestroyedEntities();
        }
    }

    @Test
    public void testDestroyEntity()
    {
        mManager.destroyEntity(0);
    }

    @Test
    public void testDestroyEntitySetsDestroyFlag()
    {
        final Entity entity = mManager.createEntity();

        mManager.destroyEntity(entity.getId());

        Assert.assertFalse(entity.isAlive());
    }

    @Test
    public void testGetEntityReturnsSameEntity()
    {
        final Entity entity = mManager.createEntity();

        Assert.assertSame(entity, mManager.getEntity(entity.getId()));
    }

    @Test
    public void testGetEntityReturnsNullWhenIdIsUnrecognized()
    {
        Assert.assertNull(mManager.getEntity(0));
    }

    private Map<String, String[]> createConfigs()
    {
        final Map<String, String[]> configs = new HashMap<>();
        configs.put(CONFIGURATION, COMPONENT_PROTOTYPES);
        return configs;
    }

    private Map<String, String[]> createConfigs(String[] prototype)
    {
        final Map<String, String[]> configs = new HashMap<>();
        configs.put(CONFIGURATION, prototype);
        return configs;
    }

    private void createComponentPrototypes()
    {
        final ComponentFactory factory = mManager.getComponentFactory();

        for (final String name : COMPONENT_PROTOTYPES) {
            factory.setPrototype(name, new DummyComponent());
        }

        factory.setSource(DummyComponent.class, ComponentFactoryTest.DummyComponent::new);
    }
}
