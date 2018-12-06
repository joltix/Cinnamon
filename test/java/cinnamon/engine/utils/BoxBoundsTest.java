package cinnamon.engine.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class BoxBoundsTest
{
    private static final Size SIZE = size(1f, 1f, 1f);

    private static final Size SMALLER_SIZE = size(0.25f, 0.25f, 0.25f);

    private BoxBounds mBounds;

    @Before
    public void setUp()
    {
        mBounds = new BoxBounds(new Point(), SIZE);
    }

    @After
    public void tearDown()
    {
        mBounds = null;
    }

    @Test
    public void testConstructorContainsAnotherBounds()
    {
        final Bounds other = new DummyBounds();
        final BoxBounds bounds = new BoxBounds(other);

        Assert.assertTrue(bounds.contains(other));
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorContainsNPEBounds()
    {
        final Bounds bounds = null;

        new BoxBounds(bounds);
    }

    /**
     * Failure cases for arguments passed to this constructor are not tested due to the large number of cases checking
     * each float's value while this constructor delegates exception throwing to the more flexible
     * {@link BoxBounds#BoxBounds(Point, Size)}, which is fully tested.
     */
    @Test
    public void testConstructorOriginCenteredHasCenterAtOrigin()
    {
        final BoxBounds bounds = new BoxBounds(SIZE);
        final Point center = bounds.getCenter();

        Assert.assertEquals(0f, center.getX(), 0f);
        Assert.assertEquals(0f, center.getY(), 0f);
        Assert.assertEquals(0f, center.getZ(), 0f);
    }

    @Test
    public void testConstructor()
    {
        new BoxBounds(new Point(), SIZE);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEMinimum()
    {
        new BoxBounds(null, SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumXNotANumber()
    {
        new BoxBounds(new Point(Float.NaN, 0f, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumYNotANumber()
    {
        new BoxBounds(new Point(0f, Float.NaN, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumZNotANumber()
    {
        new BoxBounds(new Point(0f, 0f, Float.NaN), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumXPositiveInfinity()
    {
        new BoxBounds(new Point(Float.POSITIVE_INFINITY, 0f, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumYPositiveInfinity()
    {
        new BoxBounds(new Point(0f, Float.POSITIVE_INFINITY, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumZPositiveInfinity()
    {
        new BoxBounds(new Point(0f, 0f, Float.POSITIVE_INFINITY), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumXNegativeInfinity()
    {
        new BoxBounds(new Point(Float.NEGATIVE_INFINITY, 0f, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumYNegativeInfinity()
    {
        new BoxBounds(new Point(0f, Float.NEGATIVE_INFINITY, 0f), SIZE);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAEMinimumZNegativeInfinity()
    {
        new BoxBounds(new Point(0f, 0f, Float.NEGATIVE_INFINITY), SIZE);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPESize()
    {
        new BoxBounds(new Point(), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeXNegativeDimension()
    {
        new BoxBounds(new Point(), size(-1f, 0f, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeYNegativeDimension()
    {
        new BoxBounds(new Point(), size(0f, -1f, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeZNegativeDimension()
    {
        new BoxBounds(new Point(), size(0f, 0f, -1f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeXNotANumber()
    {
        new BoxBounds(new Point(), size(Float.NaN, 0f, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeYNotANumber()
    {
        new BoxBounds(new Point(), size(0f, Float.NaN, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeZNotANumber()
    {
        new BoxBounds(new Point(), size(0f, 0f, Float.NaN));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeXPositiveInfinity()
    {
        new BoxBounds(new Point(), size(Float.POSITIVE_INFINITY, 0f, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeYPositiveInfinity()
    {
        new BoxBounds(new Point(), size(0f, Float.POSITIVE_INFINITY, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeZPositiveInfinity()
    {
        new BoxBounds(new Point(), size(0f, 0f, Float.POSITIVE_INFINITY));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeXNegativeInfinity()
    {
        new BoxBounds(new Point(), size(Float.NEGATIVE_INFINITY, 0f, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeYNegativeInfinity()
    {
        new BoxBounds(new Point(), size(0f, Float.NEGATIVE_INFINITY, 0f));
    }

    @Test (expected = IllegalArgumentException.class)
    public void testConstructorIAESizeZNegativeInfinity()
    {
        new BoxBounds(new Point(), size(0f, 0f, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testContainsBoxBoundsReturnsTrue()
    {
        final BoxBounds bounds = new BoxBounds(new Point(0.5f, 0.5f, 0.5f), SMALLER_SIZE);

        Assert.assertTrue(mBounds.contains(bounds));
    }

    @Test
    public void testContainsBoxBoundsReturnsFalse()
    {
        final BoxBounds bounds = new BoxBounds(new Point(-0.5f, -0.5f, -0.5f), SIZE);

        Assert.assertFalse(mBounds.contains(bounds));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsBoxBoundsNPEBoxBounds()
    {
        final BoxBounds bounds = null;

        mBounds.contains(bounds);
    }

    @Test
    public void testContainsPointReturnsTrue()
    {
        Assert.assertTrue(mBounds.contains(new Point(0.5f, 0.5f, 0.5f)));
    }

    @Test
    public void testContainsPointReturnsFalse()
    {
        Assert.assertFalse(mBounds.contains(new Point(-0.5f, -0.5f, -0.5f)));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsPointNPEPoint()
    {
        final Point point = null;

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointXNaN()
    {
        final Point point = new Point(Float.NaN, 0.5f, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointYNaN()
    {
        final Point point = new Point(0.5f, Float.NaN, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointZNaN()
    {
        final Point point = new Point(0.5f, 0.5f, Float.NaN);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointXPositiveInfinity()
    {
        final Point point = new Point(Float.POSITIVE_INFINITY, 0.5f, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointXNegativeInfinity()
    {
        final Point point = new Point(Float.NEGATIVE_INFINITY, 0.5f, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointYPositiveInfinity()
    {
        final Point point = new Point(0.5f, Float.POSITIVE_INFINITY, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointYNegativeInfinity()
    {
        final Point point = new Point(0.5f, Float.NEGATIVE_INFINITY, 0.5f);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointZPositiveInfinity()
    {
        final Point point = new Point(0.5f, 0.5f, Float.POSITIVE_INFINITY);

        mBounds.contains(point);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testContainsPointIAEPointZNegativeInfinity()
    {
        final Point point = new Point(0.5f, 0.5f, Float.NEGATIVE_INFINITY);

        mBounds.contains(point);
    }

    @Test
    public void testIntersectsBoxBoundsReturnsTrue()
    {
        final BoxBounds bounds = new BoxBounds(new Point(-0.5f, -0.5f, -0.5f), SIZE);

        Assert.assertTrue(mBounds.intersects(bounds));
    }

    @Test
    public void testIntersectsBoxBoundsReturnsFalse()
    {
        final BoxBounds bounds = new BoxBounds(new Point(-2f, -2f, -2f), SIZE);

        Assert.assertFalse(mBounds.intersects(bounds));
    }

    @Test (expected = NullPointerException.class)
    public void testIntersectsBoxBoundsNPEBoxBounds()
    {
        final BoxBounds bounds = null;

        mBounds.intersects(bounds);
    }

    @Test
    public void testSetMinimumChangesMinimumPoint()
    {
        final float x = 3f;
        final float y = 4f;
        final float z = 3f;

        mBounds.setMinimum(x, y, z);

        Assert.assertEquals(x, mBounds.getMinimumX(), 0f);
        Assert.assertEquals(y, mBounds.getMinimumY(), 0f);
        Assert.assertEquals(z, mBounds.getMinimumZ(), 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEXNotANumber()
    {
        mBounds.setMinimum(Float.NaN, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEYNotANumber()
    {
        mBounds.setMinimum(0f, Float.NaN, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEZNotANumber()
    {
        mBounds.setMinimum(0f, 0f, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEXPositiveInfinity()
    {
        mBounds.setMinimum(Float.POSITIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEYPositiveInfinity()
    {
        mBounds.setMinimum(0f, Float.POSITIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEZPositiveInfinity()
    {
        mBounds.setMinimum(0f, 0f, Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEXNegativeInfinity()
    {
        mBounds.setMinimum(Float.NEGATIVE_INFINITY, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEYNegativeInfinity()
    {
        mBounds.setMinimum(0f, Float.NEGATIVE_INFINITY, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetMinimumIAEZNegativeInfinity()
    {
        mBounds.setMinimum(0f, 0f, Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testSetSizeChangesSize()
    {
        final float x = 3f;
        final float y = 4f;
        final float z = 3f;

        mBounds.setSize(x, y, z);

        Assert.assertEquals(x, mBounds.getWidth(), 0f);
        Assert.assertEquals(y, mBounds.getHeight(), 0f);
        Assert.assertEquals(z, mBounds.getDepth(), 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEXNegative()
    {
        mBounds.setSize(-1f, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEYNegative()
    {
        mBounds.setSize(1f, -1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEZNegative()
    {
        mBounds.setSize(1f, 1f, -1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEXNotANumber()
    {
        mBounds.setSize(Float.NaN, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEYNotANumber()
    {
        mBounds.setSize(1f, Float.NaN, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEZNotANumber()
    {
        mBounds.setSize(1f, 1f, Float.NaN);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEXPositiveInfinity()
    {
        mBounds.setSize(Float.POSITIVE_INFINITY, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEYPositiveInfinity()
    {
        mBounds.setSize(1f, Float.POSITIVE_INFINITY, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEZPositiveInfinity()
    {
        mBounds.setSize(1f, 1f, Float.POSITIVE_INFINITY);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEXNegativeInfinity()
    {
        mBounds.setSize(Float.NEGATIVE_INFINITY, 1f, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEYNegativeInfinity()
    {
        mBounds.setSize(1f, Float.NEGATIVE_INFINITY, 1f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetSizeIAEZNegativeInfinity()
    {
        mBounds.setSize(1f, 1f, Float.NEGATIVE_INFINITY);
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertEquals(mBounds, mBounds);
    }

    @Test
    public void testEqualsSymmetric()
    {
        final BoxBounds bounds = new BoxBounds(new Point(), SIZE);

        final boolean aToB = mBounds.equals(bounds);
        final boolean bToA = bounds.equals(mBounds);

        Assert.assertTrue(aToB && bToA);
    }

    @Test
    public void testEqualsTransitive()
    {
        final BoxBounds boundsB = new BoxBounds(new Point(), SIZE);
        final BoxBounds boundsC = new BoxBounds(new Point(), SIZE);

        final boolean aToB = mBounds.equals(boundsB);
        final boolean bToC = boundsB.equals(boundsC);
        final boolean cToA = boundsC.equals(mBounds);

        Assert.assertTrue(aToB && bToC && cToA);
    }

    @Test
    public void testEqualsReturnsFalseWhenNull()
    {
        Assert.assertNotEquals(mBounds, null);
    }

    @Test
    public void testEqualsReturnsFalseWhenDifferentClass()
    {
        Assert.assertNotEquals(mBounds, new Object());
    }

    @Test
    public void testHashCodeIsTheSameWhenEqual()
    {
        final BoxBounds other = new BoxBounds(new Point(), SIZE);

        Assert.assertEquals(mBounds, other);
        Assert.assertEquals(mBounds.hashCode(), other.hashCode());
    }

    private static Size size(float width, float height, float depth)
    {
        return new Size()
        {
            @Override
            public float getWidth()
            {
                return width;
            }

            @Override
            public float getHeight()
            {
                return height;
            }

            @Override
            public float getDepth()
            {
                return depth;
            }
        };
    }

    private static class DummyBounds extends Bounds
    {
        @Override
        public boolean contains(BoxBounds bounds)
        {
            return false;
        }

        @Override
        public boolean contains(Point point)
        {
            return false;
        }

        @Override
        public boolean intersects(BoxBounds bounds)
        {
            return false;
        }

        @Override
        public Point getCenter()
        {
            return null;
        }

        @Override
        public float getMinimumX()
        {
            return 0;
        }

        @Override
        public float getMaximumX()
        {
            return 0;
        }

        @Override
        public float getMinimumY()
        {
            return 0;
        }

        @Override
        public float getMaximumY()
        {
            return 0;
        }

        @Override
        public float getMinimumZ()
        {
            return 0;
        }

        @Override
        public float getMaximumZ()
        {
            return 0;
        }
    }
}
