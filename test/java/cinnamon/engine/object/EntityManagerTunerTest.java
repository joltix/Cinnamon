package cinnamon.engine.object;

import cinnamon.engine.object.EntityManager.Tuner;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.stream.Stream;

public class EntityManagerTunerTest
{
    private Tuner mTuner;

    @Before
    public void setUp()
    {
        mTuner = new Tuner(new ComponentManager());
    }

    @After
    public void tearDown()
    {
        mTuner = null;
    }

    @Test
    public void testIncreaseCapacityBySuccessfullyExpandsCapacity()
    {
        final int more = 1;
        final int capacity = mTuner.getEntityCapacity();

        mTuner.increaseEntityCapacityBy(more);

        Assert.assertEquals(capacity + more, mTuner.getEntityCapacity());
    }

    @Test
    public void testIncreaseCapacityByDoesNotIncreaseBeyondMaximum()
    {
        mTuner.increaseEntityCapacityBy(EntityManager.MAXIMUM_ENTITY_COUNT + 1);

        Assert.assertEquals(EntityManager.MAXIMUM_ENTITY_COUNT, mTuner.getEntityCapacity());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIncreaseCapacityByIAENegativeAddition()
    {
        mTuner.increaseEntityCapacityBy(-1);
    }

    @Test
    public void testDecreaseCapacityBySuccessfullyShrinksCapacity()
    {
        final int less = 1;
        final int capacity = mTuner.getEntityCapacity();

        mTuner.decreaseEntityCapacityBy(less);

        Assert.assertEquals(capacity - less, mTuner.getEntityCapacity());
    }

    @Test
    public void testDecreaseCapacityByDoesNotDecreaseBelowOne()
    {
        // Increase capacity then force an update to manager's lookup
        destroyEntities(createEntities(50_000));
        mTuner.removeDestroyedEntities();

        mTuner.decreaseEntityCapacityBy(EntityManager.MAXIMUM_ENTITY_COUNT);

        Assert.assertEquals(1, mTuner.getEntityCapacity());
    }

    @Test (expected = IllegalArgumentException.class)
    public void testDecreaseCapacityByIAENegativeSubtraction()
    {
        mTuner.decreaseEntityCapacityBy(-1);
    }

    @Test
    public void testRemoveDestroyedEntitiesMakesDestroyedEntitiesUnusable()
    {
        final Entity[] entities = createEntities(50_000);
        destroyEntities(entities);

        mTuner.removeDestroyedEntities();

        Stream.of(entities).forEach((entity) ->
        {
            Assert.assertFalse(entity.isAlive());
            Assert.assertEquals(0, entity.getComponentCount());
            Assert.assertNull(mTuner.getManager().getEntity(entity.getId()));
        });
    }

    private Entity[] createEntities(int count)
    {
        final Entity[] entities = new Entity[count];

        for (int i = 0; i < 50_000; i++) {
            entities[i] = mTuner.getManager().createEntity();
        }

        return entities;
    }

    private void destroyEntities(Entity[] entities)
    {
        final EntityManager manager = mTuner.getManager();

        Stream.of(entities).forEach((entity) ->
        {
            manager.destroyEntity(entity.getId());
        });
    }
}
