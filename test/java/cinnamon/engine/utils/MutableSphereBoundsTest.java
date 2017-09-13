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
 * <p>The following methods from {@code MutableSphereBounds} are not explicitly tested and are presumed correct.</p>
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
public class MutableSphereBoundsTest
{
    private static final float DELTA = 0.0001f;

    private MutableSphereBounds mSphere;

    @Before
    public void setUp()
    {
        mSphere = new MutableSphereBounds();
    }

    @After
    public void tearDown()
    {
        mSphere = null;
    }

    @Test
    public void testContainsBoundsTrue()
    {
        final float radius = mSphere.getRadius();
        final float centerX = mSphere.getX() + radius;
        final float centerY = mSphere.getY() + radius;
        final float centerZ = mSphere.getZ() + radius;

        // Create box to fit inside sphere
        final float size = radius / 3f;
        final float halfSize = size / 2f;
        final float x = centerX - halfSize;
        final float y = centerY - halfSize;
        final float z = centerZ - halfSize;
        final MutableBounds box = mockBox(size, size, size, x, y, z);

        Assert.assertTrue(mSphere.contains(box));
    }

    @Test
    public void testContainsBoundsFalse()
    {
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX();
        final float y = mSphere.getY();
        final float z = mSphere.getZ();

        // Create box larger than sphere
        final float size = radius * 2f;
        final MutableBounds box = mockBox(size, size, size, x, y, z);

        Assert.assertFalse(mSphere.contains(box));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsBoundsNPE()
    {
        final MutableBounds box = null;
        mSphere.contains(box);
    }

    @Test
    public void testContainsSphereTrue()
    {
        final float radius = mSphere.getRadius();
        final float centerX = mSphere.getX() + radius;
        final float centerY = mSphere.getY() + radius;
        final float centerZ = mSphere.getZ() + radius;

        // Create sphere to fit inside
        final float innerRadius = radius / 3f;
        final float x = centerX - innerRadius;
        final float y = centerY - innerRadius;
        final float z = centerZ - innerRadius;
        final MutableSphereBounds sphere = mockSphere(innerRadius, x, y, z);

        Assert.assertTrue(mSphere.contains(sphere));
    }

    @Test
    public void testContainsSphereFalse()
    {
        final float radius = mSphere.getRadius();

        // Create sphere to be too big to fit inside
        final float outerRadius = radius * 2f;
        final MutableSphereBounds sphere = mockSphere(outerRadius, 0f, 0f, 0f);

        Assert.assertFalse(mSphere.contains(sphere));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsSphereNPE()
    {
        final MutableSphereBounds sphere = null;
        mSphere.contains(sphere);
    }

    @Test
    public void testContainsPointTrue()
    {
        // Create point at sphere's center
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius;
        final float z = mSphere.getZ() + radius;
        final Point pt = mockPoint(x, y, z);

        Assert.assertTrue(mSphere.contains(pt));
    }

    @Test
    public void testContainsPointFalse()
    {
        // Create point outside sphere
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() - radius;
        final float y = mSphere.getY() - radius;
        final float z = mSphere.getZ() - radius;
        final Point pt = mockPoint(x, y, z);

        Assert.assertFalse(mSphere.contains(pt));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsPointNPE()
    {
        final Point pt = null;
        mSphere.contains(pt);
    }

    @Test
    public void testIntersectsBoundsTrue()
    {
        // Create larger box whose corner is embedded in 1/4 of the sphere
        final float radius = mSphere.getRadius();
        final float size = radius * 4f;
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius;
        final float z = mSphere.getZ() + radius;
        final MutableBounds box = mockBox(size, size, size, x, y, z);

        Assert.assertTrue(mSphere.intersects(box));
    }

    @Test
    public void testIntersectsBoundsFalse()
    {
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX();
        final float y = mSphere.getY();
        final float z = mSphere.getZ();

        // Create box larger than sphere
        final float size = radius * 2f;
        final MutableBounds box = mockBox(size, size, size, x, y, z);

        Assert.assertFalse(mSphere.intersects(box));
    }

    @Test (expected = NullPointerException.class)
    public void testIntersectsBoundsNPE()
    {
        final MutableBounds box = null;
        mSphere.intersects(box);
    }

    @Test
    public void testIntersectsSphereTrue()
    {
        // Create smaller sphere partially embedded on right side
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius / 2f;
        final float z = mSphere.getZ() + radius / 2f;
        final MutableSphereBounds sphere = mockSphere(radius / 4f, x, y, z);

        Assert.assertTrue(mSphere.intersects(sphere));
    }

    @Test
    public void testIntersectsSphereFalse()
    {
        // Create sphere far away
        final float diameter = mSphere.getWidth();
        final float x = mSphere.getX() - diameter * 4f;
        final float y = mSphere.getY() - diameter * 4f;
        final float z = mSphere.getZ() - diameter * 4f;
        final MutableSphereBounds sphere = mockSphere(mSphere.getRadius(), x, y, z);

        Assert.assertFalse(mSphere.intersects(sphere));
    }

    @Test (expected = NullPointerException.class)
    public void testIntersectsSphereNPE()
    {
        final MutableSphereBounds sphere = null;
        mSphere.intersects(sphere);
    }

    @Test
    public void testEncompassPoint()
    {
        // Ensure point to encompass is farther than current radius
        final float initialRadius = mSphere.getRadius();
        final Point pt = mockPoint(initialRadius * 40f, initialRadius * 70f, initialRadius * 40f);

        mSphere.encompass(pt);

        final Point center = mSphere.getCenter();
        final float dist = dist(center.getX(), center.getY(), center.getZ(), pt.getX(), pt.getY(), pt.getZ());
        Assert.assertTrue(dist <= mSphere.getRadius());
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassPointNPE()
    {
        mSphere.encompass(null);
    }

    @Test
    public void testEncompassCoordinates()
    {
        final float initialRadius = mSphere.getRadius();
        final float x = initialRadius * 40f;
        final float y = initialRadius * 70f;
        final float z = initialRadius * 90f;

        mSphere.encompass(x, y, z);

        final Point center = mSphere.getCenter();
        final float dist = dist(center.getX(), center.getY(), center.getZ(), x, y, z);
        Assert.assertTrue(dist <= mSphere.getRadius());
    }

    @Test
    public void testEncompassRectPoints()
    {
        final Point min = mockPoint(0f, 0f, 0f);
        final Point max = mockPoint(10f, 10f, 10f);
        mSphere.encompass(min, max);

        // Compute rect's center
        final float centerX = (max.getX() - min.getX()) / 2f + min.getX();
        final float centerY = (max.getY() - min.getY()) / 2f + min.getY();
        final float centerZ = (max.getZ() - min.getZ()) / 2f + min.getZ();

        // Radius should be half of rect's diagonal
        final float halfDiag = dist(min.getX(), min.getY(), min.getZ(), centerX, centerY, centerZ);
        Assert.assertEquals(halfDiag, mSphere.getRadius(), DELTA);
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassRectNPEMinPoint()
    {
        mSphere.encompass(null, mockPoint(0f, 0f, 0f));
    }

    @Test (expected = NullPointerException.class)
    public void testEncompassRectNPEMaxPoint()
    {
        mSphere.encompass(mockPoint(0f, 0f, 0f), null);
    }

    @Test
    public void testEncompassRectCoordinates()
    {
        final Point min = new Point(0f, 0f, 0f);
        final Point max = new Point(10f, 10f, 10f);
        mSphere.encompass(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());

        // Compute rect's center
        final float centerX = (max.getX() - min.getX()) / 2f + min.getX();
        final float centerY = (max.getY() - min.getY()) / 2f + min.getY();
        final float centerZ = (max.getZ() - min.getZ()) / 2f + min.getZ();

        // Radius should be half of rect's diagonal
        final float halfDiag = dist(min.getX(), min.getY(), min.getZ(), centerX, centerY, centerZ);
        Assert.assertEquals(halfDiag, mSphere.getRadius(), DELTA);
    }

    @Test
    public void testCenterOnPoint()
    {
        mSphere.centerOn(mockPoint(2f, 4f, 7f));

        final float radius = mSphere.getRadius();
        Assert.assertEquals(2f - radius, mSphere.getX(), DELTA);
        Assert.assertEquals(4f - radius, mSphere.getY(), DELTA);
        Assert.assertEquals(7f - radius, mSphere.getZ(), DELTA);
    }

    @Test (expected = NullPointerException.class)
    public void testCenterOnPointNPE()
    {
        mSphere.centerOn(null);
    }

    @Test
    public void testCenterOnCoordinates()
    {
        mSphere.centerOn(7f, 4f, 7f);

        Assert.assertEquals(7f - 1f, mSphere.getX(), DELTA);
        Assert.assertEquals(4f - 1f, mSphere.getY(), DELTA);
        Assert.assertEquals(7f - 1f, mSphere.getZ(), DELTA);
    }

    @Test
    public void testCopy()
    {
        final MutableSphereBounds mock = mockSphere(42f, 3f, 4f, 3f);

        mSphere.copy(mock);

        Assert.assertEquals(mock.getRadius(), mSphere.getRadius(), 0f);
        Assert.assertEquals(mock.getX(), mSphere.getX(), 0f);
        Assert.assertEquals(mock.getY(), mSphere.getY(), 0f);
        Assert.assertEquals(mock.getZ(), mSphere.getZ(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testCopyNPE()
    {
        mSphere.copy(null);
    }

    @Test
    public void testEqualsTrue()
    {
        // Duplicate
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius;
        final float z = mSphere.getZ() + radius;
        final MutableSphereBounds duplicate = new MutableSphereBounds(radius, x, y, z);

        Assert.assertTrue(mSphere.equals(duplicate));
    }

    @Test
    public void testEqualsFalse()
    {
        // Create slightly different sphere
        final float radius = mSphere.getRadius();
        final MutableSphereBounds sphere = mockSphere(radius * 2f, mSphere.getX(), mSphere.getY(), mSphere.getZ());

        Assert.assertFalse(mSphere.equals(sphere));
    }

    @Test
    public void testEqualsWrongClass()
    {
        Assert.assertFalse(mSphere.equals("Wrong class"));
    }

    @Test
    public void testEqualsFalseNull()
    {
        Assert.assertFalse(mSphere.equals(null));
    }

    @Test
    public void testHashCodeEqual()
    {
        // Duplicate
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius;
        final float z = mSphere.getZ() + radius;
        final MutableSphereBounds duplicate = new MutableSphereBounds(radius, x, y, z);

        Assert.assertEquals(mSphere.hashCode(), duplicate.hashCode());
    }

    @Test
    public void testHashCodeUnequal()
    {
        // Different
        final float radius = mSphere.getRadius();
        final float x = mSphere.getX() + radius;
        final float y = mSphere.getY() + radius;
        final float z = mSphere.getZ() + radius;
        final MutableSphereBounds duplicate = new MutableSphereBounds(radius, x + 1f, y, z);

        Assert.assertNotEquals(mSphere.hashCode(), duplicate.hashCode());
    }

    @Test (expected = CloneNotSupportedException.class)
    public void testCloneCloneNotSupported() throws CloneNotSupportedException
    {
        mSphere.clone();
    }

    /**
     * <p>Computes the distance between two points.</p>
     *
     * @param x0 first x.
     * @param y0 first y.
     * @param z0 first z.
     * @param x1 second x.
     * @param y1 second y.
     * @param z1 second z.
     * @return distance between both points.
     */
    private static float dist(float x0, float y0, float z0, float x1, float y1, float z1)
    {
        final float xDiff = x1 - x0;
        final float yDiff = y1 - y0;
        final float zDiff = z1 - z0;

        return (float) Math.sqrt(xDiff * xDiff + yDiff * yDiff + zDiff * zDiff);
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
