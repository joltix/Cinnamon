package cinnamon.engine.object;

import cinnamon.engine.object.ComponentFactoryTest.DummyComponent;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HealthComponentTest
{
    private HealthComponent mComponent;

    @Before
    public void setUp()
    {
        mComponent = new HealthComponent();
    }

    @After
    public void tearDown()
    {
        mComponent = null;
    }

    @Test
    public void testConstructorHealthIs100ByDefault()
    {
        Assert.assertEquals(100f, mComponent.getHealth(), 0f);
    }

    @Test
    public void testConstructorMaximumHealthIs100ByDefault()
    {
        Assert.assertEquals(100f, mComponent.getMaximumHealth(), 0f);
    }

    @Test
    public void testSetHealthReplacesCurrentHealth()
    {
        mComponent.setHealth(50f);

        Assert.assertEquals(50f, mComponent.getHealth(), 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetHealthIAENegativeHealth()
    {
        mComponent.setHealth(-1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetHealthIAEHealthHigherThanMaximum()
    {
        mComponent.setHealth(mComponent.getMaximumHealth() + 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetHealthIAEHealthNotANumber()
    {
        mComponent.setHealth(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetHealthIAEPositivelyInfiniteHealth()
    {
        mComponent.setHealth(Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetHealthIAENegativelyInfiniteHealth()
    {
        mComponent.setHealth(Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testAddHealthAddsWhenPositiveAmount()
    {
        mComponent.setHealth(mComponent.getMaximumHealth() / 2f);

        final float expected = mComponent.getHealth() + 1f;
        mComponent.addHealth(1f);

        Assert.assertEquals(expected, mComponent.getHealth(), 0f);
    }

    @Test
    public void testAddHealthRemovesWhenNegativeAmount()
    {
        mComponent.setHealth(mComponent.getMaximumHealth() / 2f);

        final float expected = mComponent.getHealth() - 1f;
        mComponent.addHealth(-1f);

        Assert.assertEquals(expected, mComponent.getHealth(), 0f);
    }

    @Test
    public void testAddHealthDoesNotRaiseHealthAboveMaximum()
    {
        mComponent.addHealth(mComponent.getMaximumHealth() + 1f);

        Assert.assertEquals(mComponent.getMaximumHealth(), mComponent.getHealth(), 0f);
    }

    @Test
    public void testAddHealthDoesNotLowerHealthBelowZero()
    {
        mComponent.addHealth(-(mComponent.getMaximumHealth() + 1f));

        Assert.assertEquals(0f, mComponent.getHealth(), 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddHealthIAEHealthNotANumber()
    {
        mComponent.addHealth(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddHealthIAEHealthPositivelyInfinite()
    {
        mComponent.addHealth(Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddHealthIAEHealthNegativelyInfinite()
    {
        mComponent.addHealth(Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testSetMaximumHealthReplacesCurrentMaximum()
    {
        final float newMax = mComponent.getMaximumHealth() + 1f;

        mComponent.setMaximumHealth(newMax);

        Assert.assertEquals(newMax, mComponent.getMaximumHealth(), 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAENegativeMaximum()
    {
        mComponent.setMaximumHealth(-1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAEZeroMaximum()
    {
        mComponent.setMaximumHealth(0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAEMaximumHigherThanConstant()
    {
        mComponent.setMaximumHealth(HealthComponent.MAX_HEALTH + 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAEMaximumNotANumber()
    {
        mComponent.setMaximumHealth(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAEMaximumPositivelyInfinite()
    {
        mComponent.setMaximumHealth(Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMaximumHealthIAEMaximumNegativelyInfinite()
    {
        mComponent.setMaximumHealth(Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testIsFullReturnsTrueWhenHealthEqualsMaximum()
    {
        mComponent.setHealth(mComponent.getMaximumHealth());

        Assert.assertTrue(mComponent.isFull());
    }

    @Test
    public void testIsFullReturnsFalseWhenHealthDoesNotEqualMaximum()
    {
        mComponent.setHealth(0f);

        Assert.assertFalse(mComponent.isFull());
    }

    @Test
    public void testIsEmptyReturnsTrueWhenHealthEqualsZero()
    {
        mComponent.setHealth(0f);

        Assert.assertTrue(mComponent.isEmpty());
    }

    @Test
    public void testIsEmptyReturnsFalseWhenHealthDoesNotEqualZero()
    {
        mComponent.setHealth(1f);

        Assert.assertFalse(mComponent.isEmpty());
    }

    @Test
    public void testCopyCopiesHealthAndMaximumHealth()
    {
        mComponent.setHealth(50f);
        mComponent.setMaximumHealth(150f);

        final HealthComponent copy = new HealthComponent();
        copy.copy(mComponent);

        Assert.assertEquals(50f, copy.getHealth(),0f);
        Assert.assertEquals(150f, copy.getMaximumHealth(),0f);
    }

    @Test (expected = ClassCastException.class)
    public void testCopyCCESource()
    {
        mComponent.copy(new DummyComponent());
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNPESource()
    {
        mComponent.copy(null);
    }
}