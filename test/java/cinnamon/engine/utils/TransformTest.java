package cinnamon.engine.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TransformTest
{
    private Transform mTransform;

    @Before
    public void setUp()
    {
        mTransform = new Transform();
    }

    @After
    public void tearDown()
    {
        mTransform = null;
    }

    @Test
    public void testSetPosition()
    {
        mTransform.setPosition(0f, 0f, 0f);
    }

    @Test
    public void testSetScale()
    {
        mTransform.setScale(1f, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEXNotANumber()
    {
        mTransform.setScale(Float.NaN, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEYNotANumber()
    {
        mTransform.setScale(1f, Float.NaN, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEZNotANumber()
    {
        mTransform.setScale(1f, 1f, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEXNotPositive()
    {
        mTransform.setScale(0f, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEYNotPositive()
    {
        mTransform.setScale(1f, 0f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleIAEZNotPositive()
    {
        mTransform.setScale(1f, 1f, 0f);
    }

    @Test
    public void testSetScaleX()
    {
        mTransform.setScaleX(1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleXIAENotANumber()
    {
        mTransform.setScaleX(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleXIAENotPositive()
    {
        mTransform.setScaleX(0f);
    }

    @Test
    public void testSetScaleY()
    {
        mTransform.setScaleY(1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleYIAENotANumber()
    {
        mTransform.setScaleY(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleYIAENotPositive()
    {
        mTransform.setScaleY(0f);
    }

    @Test
    public void testSetScaleZ()
    {
        mTransform.setScaleZ(1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleZIAENotANumber()
    {
        mTransform.setScaleZ(Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetScaleZIAENotPositive()
    {
        mTransform.setScaleZ(0f);
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertEquals(mTransform, mTransform);
    }

    @Test
    public void testEqualsSymmetric()
    {
        final Transform other = new Transform();

        final boolean aToB = mTransform.equals(other);
        final boolean bToA = other.equals(mTransform);

        Assert.assertTrue(aToB && bToA);
    }

    @Test
    public void testEqualsTransitive()
    {
        final Transform transformB = new Transform();
        final Transform transformC = new Transform();

        final boolean aToB = mTransform.equals(transformB);
        final boolean bToC = transformB.equals(transformC);
        final boolean cToA = transformC.equals(mTransform);

        Assert.assertTrue(aToB && bToC && cToA);
    }


    @Test
    public void testEqualsReturnsFalseWhenNull()
    {
        Assert.assertNotEquals(mTransform, null);
    }

    @Test
    public void testEqualsReturnsFalseWhenDifferentClass()
    {
        Assert.assertNotEquals(mTransform, new Object());
    }

    @Test
    public void testHashCodeIsTheSameWhenEqual()
    {
        final Transform other = new Transform();

        Assert.assertEquals(mTransform, other);
        Assert.assertEquals(mTransform.hashCode(), other.hashCode());
    }
}