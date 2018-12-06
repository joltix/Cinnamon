package cinnamon.engine.object;

import cinnamon.engine.object.ComponentFactoryTest.DummyComponent;
import cinnamon.engine.utils.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class LevelDirectorTest
{
    private static final String ENTITY_CONFIGURATION = "dummy_entity";

    private LevelDirectorTestImpl mDirector;

    private Entity mEntity;

    @Before
    public void setUp()
    {
        mDirector = new LevelDirectorTestImpl();
        mEntity = new Entity(0);
    }

    @After
    public void tearDown()
    {
        mDirector = null;
        mEntity = null;
    }

    @Test
    public void testMoveRelocatesEntityToPosition()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 3f, 4f, 3f);

        final Point position = mEntity.getTransform().getPosition();
        Assert.assertEquals(3f, position.getX(), 0f);
        Assert.assertEquals(4f, position.getY(), 0f);
        Assert.assertEquals(3f, position.getZ(), 0f);
    }

    @Test
    public void testMoveDoesNotRelocateOutsideBoundary()
    {
        setBoundaryAvailable(true);

        final Bounds bounds = mDirector.getLevelBoundary();
        final float x = bounds.getMinimumX();
        final float y = bounds.getMinimumY();
        final float z = bounds.getMinimumZ();

        mDirector.move(mEntity, x - Level.MAX_SIZE, y - Level.MAX_SIZE, z - Level.MAX_SIZE);

        final Transform transform = mEntity.getTransform();
        Assert.assertEquals(x, transform.getX(), 0f);
        Assert.assertEquals(y, transform.getY(), 0f);
        Assert.assertEquals(z, transform.getZ(), 0f);

        Assert.assertTrue(bounds.contains(transform.getPosition()));
    }

    @Test (expected = NullPointerException.class)
    public void testMoveNPEEntity()
    {
        setBoundaryAvailable(true);

        mDirector.move(null, 0f, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEXNotANumber()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, Float.NaN, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEYNotANumber()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, Float.NaN, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEZNotANumber()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, 0f, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEXPositiveInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, Float.POSITIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEYPositiveInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, Float.POSITIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEZPositiveInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, 0f, Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEXNegativeInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, Float.NEGATIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEYNegativeInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, Float.NEGATIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testMoveIAEZNegativeInfinity()
    {
        setBoundaryAvailable(true);

        mDirector.move(mEntity, 0f, 0f, Float.NEGATIVE_INFINITY);
    }

    @Test (expected = IllegalStateException.class)
    public void testMoveISELevelBoundaryNotAvailable()
    {
        setBoundaryAvailable(false);

        mDirector.move(mEntity, 0f, 0f, 0f);
    }

    @Test
    public void testSpawnCreatesEntityAtPosition()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        final Entity entity = mDirector.spawn(ENTITY_CONFIGURATION, 3f, 4f, 3f);
        Assert.assertNotNull(entity);

        final Point position = entity.getTransform().getPosition();
        Assert.assertEquals(3f, position.getX(), 0f);
        Assert.assertEquals(4f, position.getY(), 0f);
        Assert.assertEquals(3f, position.getZ(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testSpawnNPEEntityConfiguration()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(null, 0f, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEXNotANumber()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, Float.NaN, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEYNotANumber()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, Float.NaN, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEZNotANumber()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, 0f, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEXPositiveInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, Float.POSITIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEYPositiveInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, Float.POSITIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEZPositiveInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, 0f, Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEXNegativeInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, Float.NEGATIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEYNegativeInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, Float.NEGATIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSpawnIAEZNegativeInfinity()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, 0f, Float.NEGATIVE_INFINITY);
    }

    @Test (expected = IllegalStateException.class)
    public void testSpawnISEEntityManagementNotAvailable()
    {
        setManagementAvailable(false);
        setBoundaryAvailable(true);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, 0f, 0f);
    }

    @Test (expected = IllegalStateException.class)
    public void testSpawnISELevelBoundaryNotAvailable()
    {
        setManagementAvailable(true);
        setBoundaryAvailable(false);

        mDirector.spawn(ENTITY_CONFIGURATION, 0f, 0f, 0f);
    }

    @Test
    public void testDespawnDestroysEntity()
    {
        setManagementAvailable(true);
        final Entity entity = mDirector.getEntityManager().createEntity(ENTITY_CONFIGURATION);

        mDirector.despawn(entity);

        Assert.assertFalse(entity.isAlive());
    }

    @Test (expected = NullPointerException.class)
    public void testDespawnNPEEntity()
    {
        setManagementAvailable(true);

        mDirector.despawn(null);
    }

    @Test (expected = IllegalStateException.class)
    public void testDespawnISEEntityManagementNotAvailable()
    {
        setManagementAvailable(false);

        mDirector.despawn(mEntity);
    }

    private void setManagementAvailable(boolean enable)
    {
        mDirector.mManagementAvailable = enable;
    }

    private void setBoundaryAvailable(boolean enable)
    {
        mDirector.mBoundaryAvailable = enable;
    }

    private class LevelDirectorTestImpl implements LevelDirector
    {
        private final EntityManager mManager = new EntityManager.Tuner(0).getManager();

        private final BoxBounds mBounds = new BoxBounds(new Point(), new Size()
        {
            @Override
            public float getWidth()
            {
                return Level.MAX_SIZE;
            }

            @Override
            public float getHeight()
            {
                return Level.MAX_SIZE;
            }

            @Override
            public float getDepth()
            {
                return Level.MAX_SIZE;
            }
        });

        private boolean mManagementAvailable;

        private boolean mBoundaryAvailable;

        private LevelDirectorTestImpl()
        {
            final ComponentFactory factory = mManager.getComponentFactory();
            factory.setPrototype("dummy_component", new DummyComponent());
            factory.setSource(DummyComponent.class, DummyComponent::new);

            final Map<String, String[]> configs = new HashMap<>();
            configs.put(ENTITY_CONFIGURATION, new String[] {"dummy_component"});

            mManager.setConfigurations(configs);
        }

        @Override
        public EntityManager getEntityManager()
        {
            return (mManagementAvailable) ? mManager : null;
        }

        @Override
        public Bounds getLevelBoundary()
        {
            return (mBoundaryAvailable) ? mBounds : null;
        }
    }
}