package cinnamon.engine.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * <p>The following methods from {@code MutableBounds} are not explicitly tested and are presumed correct.</p>
 *
 * <ul>
 *     <li>{@code getWidth()}</li>
 *     <li>{@code getHeight()}</li>
 *     <li>{@code getDepth()}</li>
 *     <li>{@code getX()}</li>
 *     <li>{@code getY()}</li>
 *     <li>{@code getZ()}</li>
 *     <li>{@code getMaximumX()}</li>
 *     <li>{@code getMaximumY()}</li>
 *     <li>{@code getMaximumZ()}</li>
 * </ul>
 */
@RunWith (PowerMockRunner.class)
@PrepareForTest ({Point.class, MutableBounds.class, MutableSphereBounds.class})
public class MutableBoundsTest
{
    // FP comparison epsilon
    private static final float DELTA = 0.0001f;

    private MutableBounds mBounds;

    @Before
    public void setUp()
    {
        mBounds = new MutableBounds();
    }

    @After
    public void tearDown()
    {
        mBounds = null;
    }

    @Test
    public void testContainsBoundsTrue()
    {
        final float w = mBounds.getWidth() / 2f;
        final float h = mBounds.getHeight() / 2f;
        final float d = mBounds.getDepth() / 2f;

        // Position smaller bounds centered inside
        final float x = mBounds.getX() + mBounds.getWidth() / 4f;
        final float y = mBounds.getY() + mBounds.getHeight() / 4f;
        final float z = mBounds.getZ() + mBounds.getDepth() / 4f;
        final MutableBounds inner = mockBox(w, h, d, x, y, z);

        Assert.assertTrue(mBounds.contains(inner));
    }

    @Test
    public void testContainsBoundsFalse()
    {
        final float w = mBounds.getWidth() * 2f;
        final float h = mBounds.getHeight() * 2f;
        final float d = mBounds.getDepth() * 2f;

        // Position larger bounds centered
        final float x = mBounds.getX() - w / 4f;
        final float y = mBounds.getY() - h / 4f;
        final float z = mBounds.getZ() - d / 4f;
        final MutableBounds outer = mockBox(w, h, d, x, y, z);

        Assert.assertFalse(mBounds.contains(outer));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsBoundsNPE()
    {
        final MutableBounds box = null;
        mBounds.contains(box);
    }

    @Test
    public void testContainsSphereTrue()
    {
        // Inner sphere using box's smallest dimen guarantees smallest
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float radius = Math.min(w, Math.min(h, d)) / 4f;

        // Create sphere centered
        final float centerX = mBounds.getX() + w / 2f;
        final float centerY = mBounds.getY() + h / 2f;
        final float centerZ = mBounds.getZ() + d / 2f;
        final MutableSphereBounds inner = mockSphere(radius, centerX, centerY, centerZ);

        Assert.assertTrue(mBounds.contains(inner));
    }

    @Test
    public void testContainsSphereFalse()
    {
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float radius = Math.min(w, Math.min(h, d));

        // Create sphere to be too big to fit inside
        final MutableSphereBounds sphere = mockSphere(radius * 2f, 0f, 0f, 0f);

        Assert.assertFalse(mBounds.contains(sphere));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsSphereNPE()
    {
        final MutableSphereBounds sphere = null;
        mBounds.contains(sphere);
    }

    @Test
    public void testContainsPointTrue()
    {
        // Create point at bounds's center
        final float x = mBounds.getX() + mBounds.getWidth();
        final float y = mBounds.getY() + mBounds.getHeight();
        final float z = mBounds.getZ() + mBounds.getDepth();
        final Point pt = mockPoint(x, y, z);

        Assert.assertTrue(mBounds.contains(pt));
    }

    @Test
    public void testContainsPointFalse()
    {
        // Create point outside sphere
        final float x = mBounds.getX() - mBounds.getWidth();
        final float y = mBounds.getY() - mBounds.getHeight();
        final float z = mBounds.getZ() - mBounds.getDepth();
        final Point pt = mockPoint(x, y, z);

        Assert.assertFalse(mBounds.contains(pt));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsPointNPE()
    {
        final Point pt = null;
        mBounds.contains(pt);
    }

    @Test
    public void testIntersectsBoundsTrue()
    {
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float x = mBounds.getX() + w / 2f;
        final float y = mBounds.getY() + h / 2f;
        final float z = mBounds.getZ() + d / 2f;

        // Duplicate but shift halfway out
        final MutableBounds box = mockBox(w, h, d, x, y, z);

        Assert.assertTrue(mBounds.intersects(box));
    }

    @Test
    public void testIntersectsBoundsFalse()
    {
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float x = mBounds.getX() + w * 2f;
        final float y = mBounds.getY() + h * 2f;
        final float z = mBounds.getZ() + d * 2f;

        // Duplicate and move far away
        final MutableBounds box = mockBox(w, h, d, x, y, z);

        Assert.assertFalse(mBounds.intersects(box));
    }

    @Test (expected = NullPointerException.class)
    public void testIntersectsBoundsNPE()
    {
        final MutableBounds box = null;
        mBounds.intersects(box);
    }

    @Test
    public void testIntersectsSphereTrue()
    {
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float r = Math.min(w, Math.min(h, d));

        // Partially embed sphere's corner into bounds' corner
        final float x = mBounds.getX() + w - 0.25f;
        final float y = mBounds.getY() + h - 0.25f;
        final float z = mBounds.getZ() + d / 2f;
        final MutableSphereBounds sphere = mockSphere(r, x, y, z);

        Assert.assertTrue(mBounds.intersects(sphere));
    }

    @Test
    public void testIntersectsSphereFalse()
    {
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float r = Math.min(w, Math.min(h, d));

        // Move sphere far away
        final float x = mBounds.getX() + w * 2f;
        final float y = mBounds.getY() + h * 2f;
        final float z = mBounds.getZ() + d * 2f;
        final MutableSphereBounds sphere = mockSphere(r, x, y, z);

        Assert.assertFalse(mBounds.intersects(sphere));
    }

    @Test (expected = NullPointerException.class)
    public void testIntersectsSphereNPE()
    {
        final MutableSphereBounds sphere = null;
        mBounds.intersects(sphere);
    }

    @Test
    public void testEncompassPoint()
    {
        // Create point slightly outside bounds
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final Point pt = mockPoint(x - 1f, y - 1f, z - 1f);

        mBounds.encompass(pt);
        Assert.assertEquals(pt.getX(), mBounds.getX(), 0f);
        Assert.assertEquals(pt.getY(), mBounds.getY(), 0f);
        Assert.assertEquals(pt.getZ(), mBounds.getZ(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassPointNPE()
    {
        mBounds.encompass(null);
    }

    @Test
    public void testEncompassCoordinates()
    {
        // Create point slightly outside bounds
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final Point pt = mockPoint(x - 1f, y - 1f, z - 1f);

        mBounds.encompass(pt.getX(), pt.getY(), pt.getZ());
        Assert.assertEquals(pt.getX(), mBounds.getX(), 0f);
        Assert.assertEquals(pt.getY(), mBounds.getY(), 0f);
        Assert.assertEquals(pt.getZ(), mBounds.getZ(), 0f);
    }

    @Test
    public void testEncompassRectPoints()
    {
        // Define much larger rect to encompass
        final Point min = mockPoint(-10f, -10f, -10f);
        final Point max = mockPoint(10f, 10f, 10f);
        mBounds.encompass(min, max);

        Assert.assertEquals(min.getX(), mBounds.getX(), DELTA);
        Assert.assertEquals(min.getY(), mBounds.getY(), DELTA);
        Assert.assertEquals(min.getZ(), mBounds.getZ(), DELTA);
        Assert.assertEquals(max.getX(), mBounds.getX() + mBounds.getWidth(), DELTA);
        Assert.assertEquals(max.getY(), mBounds.getY() + mBounds.getHeight(), DELTA);
        Assert.assertEquals(max.getZ(), mBounds.getZ() + mBounds.getDepth(), DELTA);
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassRectNPEMinPoint()
    {
        mBounds.encompass(null, mockPoint(0f, 0f, 0f));
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassRectNPEMaxPoint()
    {
        mBounds.encompass(mockPoint(0f, 0f, 0f), null);
    }

    @Test
    public void testEncompassRectCoordinates()
    {
        // Define much larger rect to encompass
        final Point min = mockPoint(-10f, -10f, -10f);
        final Point max = mockPoint(10f, 10f, 10f);
        mBounds.encompass(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());

        Assert.assertEquals(min.getX(), mBounds.getX(), DELTA);
        Assert.assertEquals(min.getY(), mBounds.getY(), DELTA);
        Assert.assertEquals(min.getZ(), mBounds.getZ(), DELTA);
        Assert.assertEquals(max.getX(), mBounds.getX() + mBounds.getWidth(), DELTA);
        Assert.assertEquals(max.getY(), mBounds.getY() + mBounds.getHeight(), DELTA);
        Assert.assertEquals(max.getZ(), mBounds.getZ() + mBounds.getDepth(), DELTA);
    }

    @Test
    public void testCenterOnPoint()
    {
        final Point pt = mockPoint(3f, 4f, 3f);
        mBounds.centerOn(pt);

        Assert.assertEquals(pt.getX(), mBounds.getX() + mBounds.getWidth() / 2f, DELTA);
        Assert.assertEquals(pt.getY(), mBounds.getY() + mBounds.getHeight() / 2f, DELTA);
        Assert.assertEquals(pt.getZ(), mBounds.getZ() + mBounds.getDepth() / 2f, DELTA);
    }

    @Test (expected = NullPointerException.class)
    public void testCenterOnPointNPE()
    {
        mBounds.centerOn(null);
    }

    @Test
    public void testCenterOnCoordinates()
    {
        mBounds.centerOn(3f, 4f, 3f);

        Assert.assertEquals(3f, mBounds.getX() + mBounds.getWidth() / 2f, DELTA);
        Assert.assertEquals(4f, mBounds.getY() + mBounds.getHeight() / 2f, DELTA);
        Assert.assertEquals(3f, mBounds.getZ() + mBounds.getDepth() / 2f, DELTA);
    }

    @Test
    public void testCopy()
    {
        final MutableBounds mock = mockBox(321f, 213f, 132f, 3f, 4f, 3f);
        mBounds.copy(mock);

        Assert.assertEquals(mock.getWidth(), mBounds.getWidth(), 0f);
        Assert.assertEquals(mock.getHeight(), mBounds.getHeight(), 0f);
        Assert.assertEquals(mock.getDepth(), mBounds.getDepth(), 0f);

        Assert.assertEquals(mock.getX(), mBounds.getX(), 0f);
        Assert.assertEquals(mock.getY(), mBounds.getY(), 0f);
        Assert.assertEquals(mock.getZ(), mBounds.getZ(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNPE()
    {
        mBounds.copy(null);
    }

    @Test
    public void testEqualsTrue()
    {
        // Duplicate
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final MutableBounds duplicate = new MutableBounds(w, h, d, x, y, z);

        Assert.assertTrue(mBounds.equals(duplicate));
    }

    @Test
    public void testEqualsFalse()
    {
        // Create slightly different bounds
        final float w = mBounds.getWidth() * 2f;
        final float h = mBounds.getHeight() * 2f;
        final float d = mBounds.getDepth() * 2f;
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final MutableBounds duplicate = new MutableBounds(w, h, d, x, y, z);

        Assert.assertFalse(mBounds.equals(duplicate));
    }

    @Test
    public void testEqualsWrongClass()
    {
        Assert.assertFalse(mBounds.equals("Wrong class"));
    }

    @Test
    public void testEqualsFalseNull()
    {
        Assert.assertFalse(mBounds.equals(null));
    }

    @Test
    public void testHashCodeEqual()
    {
        // Duplicate
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final MutableBounds duplicate = new MutableBounds(w, h, d, x, y, z);

        Assert.assertEquals(mBounds.hashCode(), duplicate.hashCode());
    }

    @Test
    public void testHashCodeUnequal()
    {
        // Different
        final float w = mBounds.getWidth();
        final float h = mBounds.getHeight();
        final float d = mBounds.getDepth();
        final float x = mBounds.getX();
        final float y = mBounds.getY();
        final float z = mBounds.getZ();
        final MutableBounds duplicate = new MutableBounds(w, h + 1f, d, x, y, z);

        Assert.assertNotEquals(mBounds.hashCode(), duplicate.hashCode());
    }

    @Test (expected = CloneNotSupportedException.class)
    public void testCloneCloneNotSupported() throws CloneNotSupportedException
    {
        mBounds.clone();
    }

    /**
     * <p>Mocks a {@code Point} that returns the specified x, y, and z values when the corresponding getters are
     * called.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     * @return point.
     */
    private static Point mockPoint(float x, float y, float z)
    {
        final Point pt = Mockito.mock(Point.class);
        Mockito.when(pt.getX()).thenReturn(x);
        Mockito.when(pt.getY()).thenReturn(y);
        Mockito.when(pt.getZ()).thenReturn(z);
        return pt;
    }

    /**
     * <p>Mocks a {@code MutableBounds} at the specified (x,y,z) position and with the given size.</p>
     *
     * <p>The following methods are supported.</p>
     * <ul>
     *     <li>{@code getWidth()}</li>
     *     <li>{@code getHeight()}</li>
     *     <li>{@code getDepth()}</li>
     *     <li>{@code getCenter()}</li>
     *     <li>{@code getX()}</li>
     *     <li>{@code getY()}</li>
     *     <li>{@code getZ()}</li>
     *     <li>{@code getMaximumX()}</li>
     *     <li>{@code getMaximumY()}</li>
     *     <li>{@code getMaximumZ()}</li>
     * </ul>
     *
     * @param w width.
     * @param h height.
     * @param d depth.
     * @param x min x.
     * @param y min y.
     * @param z min z.
     * @return box.
     */
    private static MutableBounds mockBox(float w, float h, float d, float x, float y, float z)
    {
        final MutableBounds box = Mockito.mock(MutableBounds.class);

        Mockito.when(box.getWidth()).thenReturn(w);
        Mockito.when(box.getHeight()).thenReturn(h);
        Mockito.when(box.getDepth()).thenReturn(d);

        Mockito.when(box.getCenter()).thenAnswer((InvocationOnMock invocation) -> {
            return mockPoint(x + w / 2f, y + h / 2f, z + d / 2f);
        });

        Mockito.when(box.getX()).thenReturn(x);
        Mockito.when(box.getY()).thenReturn(y);
        Mockito.when(box.getZ()).thenReturn(z);

        Mockito.when(box.getMaximumX()).thenReturn(x + w);
        Mockito.when(box.getMaximumY()).thenReturn(y + h);
        Mockito.when(box.getMaximumZ()).thenReturn(z + d);

        return box;
    }

    /**
     * <p>Mocks a {@code MutableSphereBounds} that returns the specified center (x,y,z) position as well as size.</p>
     *
     * <p>The following methods are supported.</p>
     * <ul>
     *     <li>{@code getRadius()}</li>
     *     <li>{@code getWidth()}</li>
     *     <li>{@code getHeight()}</li>
     *     <li>{@code getDepth()}</li>
     *     <li>{@code getCenter()}</li>
     *     <li>{@code getX()}</li>
     *     <li>{@code getY()}</li>
     *     <li>{@code getZ()}</li>
     *     <li>{@code getMaximumX()}</li>
     *     <li>{@code getMaximumY()}</li>
     *     <li>{@code getMaximumZ()}</li>
     * </ul>
     *
     * @param r radius.
     * @param x center x.
     * @param y center y.
     * @param z center z.
     * @return sphere.
     */
    private static MutableSphereBounds mockSphere(float r, float x, float y, float z)
    {
        final MutableSphereBounds sphere = Mockito.mock(MutableSphereBounds.class);

        final float diameter = r * 2f;
        Mockito.when(sphere.getRadius()).thenReturn(r);
        Mockito.when(sphere.getWidth()).thenReturn(diameter);
        Mockito.when(sphere.getHeight()).thenReturn(diameter);
        Mockito.when(sphere.getDepth()).thenReturn(diameter);

        Mockito.when(sphere.getCenter()).thenAnswer((InvocationOnMock invocation) -> {
            return mockPoint(x, y, z);
        });

        Mockito.when(sphere.getX()).thenReturn(x - r);
        Mockito.when(sphere.getY()).thenReturn(y - r);
        Mockito.when(sphere.getZ()).thenReturn(z - r);

        Mockito.when(sphere.getMaximumX()).thenReturn(x + r);
        Mockito.when(sphere.getMaximumY()).thenReturn(y + r);
        Mockito.when(sphere.getMaximumZ()).thenReturn(z + r);

        return sphere;
    }
}
