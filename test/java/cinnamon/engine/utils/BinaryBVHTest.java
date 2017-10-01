package cinnamon.engine.utils;

import cinnamon.engine.utils.BinaryBVH.Node;
import cinnamon.engine.utils.BinaryBVHTestSuite.BoxPartitionable;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * <p>Constructors and the following methods are not tested explicitly.</p>
 * <ul>
 *     <li>{@code size()}</li>
 *     <li>{@code isEmpty()}</li>
 * </ul>
 */
public class BinaryBVHTest
{
    private static final int ELEMENT_COUNT = 10_000;

    private BinaryBVH<BoxPartitionable> mTree;

    // Track tree's contents separately
    private BoxPartitionable[] mElements = new BoxPartitionable[ELEMENT_COUNT];
    private boolean[] mMarked = new boolean[ELEMENT_COUNT];

    @Before
    public void setUp()
    {
        mTree = new BinaryBVH<>();
        mElements = BinaryBVHTestSuite.generateDistinctBoxPartitionables(ELEMENT_COUNT);

        for (final BoxPartitionable o : mElements) {
            mTree.add(o.getBounds(), o);
        }
    }

    @After
    public void tearDown()
    {
        mTree = null;
        mElements = null;
        mMarked = null;
    }

    @Test
    public void testGetContainedListForBox()
    {
        final Node root = getRootForHalfTree();
        final Bounds container = root.getBounds();

        List<BoxPartitionable> results = new ArrayList<>();
        results = mTree.getContained(results, container, mTree.size());

        for (final BoxPartitionable obj : results) {
            Assert.assertTrue(container.contains(obj.getBounds()));

            // Ensure haven't seen obj before
            Assert.assertFalse(isMarked(obj));
            mark(obj);
        }

        // Objs in tree but not in results should not be contained
        for (final BoxPartitionable obj : mElements) {
            if (isMarked(obj)) {
                continue;
            }

            Assert.assertFalse(container.contains(obj.getBounds()));
        }
    }

    @Test
    public void testGetContainedArrayForBox()
    {
        final Node node = getRootForHalfTree();
        final Bounds container = node.getBounds();

        BoxPartitionable[] results = new BoxPartitionable[ELEMENT_COUNT];
        results = mTree.getContained(results, container, mTree.size());

        for (final BoxPartitionable obj : results) {
            if (obj == null) {
                continue;
            }

            Assert.assertTrue(container.contains(obj.getBounds()));

            // Ensure haven't seen obj before
            Assert.assertFalse(isMarked(obj));
            mark(obj);
        }

        // Test that the objs in tree but not in results are not contained by the container
        for (final BoxPartitionable obj : mElements) {
            if (obj == null || isMarked(obj)) {
                continue;
            }

            Assert.assertFalse(container.contains(obj.getBounds()));
        }
    }

    @Test (expected = NullPointerException.class)
    public void testGetContainedListForBoxNPEList()
    {
        final List<BoxPartitionable> list = null;
        mTree.getContained(list, new MutableBounds(), 1);
    }

    @Test (expected = NullPointerException.class)
    public void testGetContainedListForBoxNPEBounds()
    {
        final Bounds box = null;
        mTree.getContained(new ArrayList<>(), box, 1);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetContainedListForBoxIllegalArgumentException()
    {
        mTree.getContained(new ArrayList<>(), new MutableBounds(), -1);
    }

    @Test
    public void testGetContainedPoint()
    {
        final Node node = getRootForHalfTree();
        final Bounds rect = node.getBounds();

        final float centerX = rect.getX() + (rect.getWidth() /2f);
        final float centerY = rect.getY() + (rect.getHeight() / 2f);
        final Point pt = new Point(centerX, centerY, 0f);

        final int max = mTree.size();

        final List<BoxPartitionable> contained = new ArrayList<>();
        final List<BoxPartitionable> results = mTree.getIntersections(contained, new Point(centerX, centerY, 0f), max);

        Assert.assertNotNull(results);
        Assert.assertEquals(contained, results);
        Assert.assertTrue(results.size() <= max);

        final MutableBounds pointAsBounds = new MutableBounds();
        pointAsBounds.encompass(pt);
        pointAsBounds.encompass(pt.getX(), pt.getY(), pt.getZ(), pt.getX(), pt.getY(), pt.getZ());

        // All results should contain obj's rect
        for (final BoxPartitionable obj : results) {
            Assert.assertTrue(obj.getBounds().contains(pointAsBounds));
        }
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetContainedPointIllegalArgumentException()
    {
        mTree.getIntersections(new ArrayList<>(), new Point(), -1);
    }

    @Test (expected = NullPointerException.class)
    public void testGetContainedPointNPE()
    {
        final Point pt = null;
        mTree.getIntersections(new ArrayList<>(), pt, 1);
    }

    @Test (expected = NullPointerException.class)
    public void testGetContainedPointNPEList()
    {
        final List<BoxPartitionable> list = null;
        mTree.getIntersections(list, new Point(), 1);
    }

    @Test
    public void testGetIntersections()
    {
        final Node node = getRootForHalfTree();
        final Bounds bounds = node.getBounds();
        final int max = mTree.size();

        final List<BoxPartitionable> intersections = new ArrayList<>();
        final List<BoxPartitionable> results = mTree.getIntersections(intersections, bounds, max);

        Assert.assertNotNull(results);
        Assert.assertEquals(intersections, results);
        Assert.assertTrue(results.size() <= max);

        // All results should intersect obj's rect
        for (final BoxPartitionable obj : results) {
            Assert.assertTrue(obj.getBounds().intersects(bounds));
        }
    }

    /**
     * <p>This tests the use of {@link BinaryBVH#add(Bounds, Object)} during {@code setUp()} and does not actually
     * add anything to the tree.</p>
     */
    @Test
    public void testAdd()
    {
        for (final BoxPartitionable obj : mElements) {
            Assert.assertTrue(isInTree(obj));
        }
    }

    @Test
    public void testAddReturnsFalse()
    {
        Assert.assertFalse(mTree.add(mElements[0].getBounds(), mElements[0]));
    }

    @Test (expected = NullPointerException.class)
    public void testAddNPEBounds()
    {
        mTree.add(null, new BoxPartitionable(1f, 1f, 1f, 0f, 0f, 0f));
    }

    @Test (expected = NullPointerException.class)
    public void testAddNPEElement()
    {
        mTree.add(new MutableBounds(1f, 1f, 1f, 0f, 0f, 0f), null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testAddIllegalArgumentException()
    {
        final BoxPartitionable element = mElements[0];
        final BoxPartitionable[] newElement = BinaryBVHTestSuite.generateDistinctBoxPartitionables(1);

        final LockingSpacePartitioningTree<BoxPartitionable> otherTree = new BinaryBVH<>();
        otherTree.add(element.getBounds(), newElement[0]);
    }

    @Test
    public void testUpdate()
    {
        // Farthest left leaf as test subject
        Node node = mTree.getRoot();
        while (node.getLeft() != null) {
            node = node.getLeft();
        }

        final BoxPartitionable obj = (BoxPartitionable) node.getElement();
        final Bounds parBox = node.getParent().getBounds();

        // Move box outside parent's
        final Bounds box = obj.getBounds();
        final float minY = parBox.getY() - 10f;
        final float minZ = parBox.getZ() - 10f;
        final float maxX = box.getX() + box.getWidth();
        final float maxY = parBox.getY() - 5f;
        final float maxZ = parBox.getZ() - 5f;
        obj.getMutableBounds().encompass(box.getX(), minY, minZ, maxX, maxY, maxZ);

        Assert.assertTrue(mTree.update(box));
    }

    @Test
    public void testUpdateReturnsFalse()
    {
        Node node = mTree.getRoot();

        // Farthest left leaf as test subject
        while (node.getLeft() != null) {
            node = node.getLeft();
        }

        final Bounds box = ((BoxPartitionable) node.getElement()).getBounds();
        Assert.assertFalse(mTree.update(box));
    }

    @Test (expected = NullPointerException.class)
    public void testUpdateNPE()
    {
        mTree.update(null);
    }

    @Test
    public void testUpdateMass()
    {
        Node node = mTree.getRoot();

        // Farthest left leaf as test subject
        while (node.getLeft() != null) {
            node = node.getLeft();
        }

        final BoxPartitionable obj = (BoxPartitionable) node.getElement();
        final Bounds parBox = node.getParent().getBounds();

        // Move box outside parent's
        final Bounds box = obj.getBounds();
        final float minY = parBox.getY() - 10f;
        final float minZ = parBox.getZ() - 10f;
        final float maxX = box.getX() + box.getWidth();
        final float maxY = parBox.getY() - 5f;
        final float maxZ = parBox.getZ() - 5f;
        obj.getMutableBounds().encompass(box.getX(), minY, minZ, maxX, maxY, maxZ);

        Assert.assertTrue(mTree.update());
    }

    @Test
    public void testUpdateMassReturnsFalse()
    {
        Assert.assertFalse(mTree.update());
    }

    @Test
    public void testRemove()
    {
        // Each added obj is
        // (1) reports successfully removed
        // (2) no longer in tree after remove
        for (final BoxPartitionable obj : mElements) {
            Assert.assertNotNull(mTree.remove(obj.getBounds()));
            Assert.assertFalse(isInTree(obj));
        }
    }

    @Test
    public void testRemoveReturnsNull()
    {
        // When bounds is unused
        final Bounds bounds = new MutableBounds();
        Assert.assertNull(mTree.remove(bounds));

        // When bounds is locked by another tree
        final Bounds boundsOfOtherTree = new MutableBounds();
        final LockingSpacePartitioningTree<Integer> otherTree = new BinaryBVH<>();

        otherTree.add(boundsOfOtherTree, new Integer(42));
        Assert.assertNull(mTree.remove(boundsOfOtherTree));
    }

    @Test (expected = NullPointerException.class)
    public void testRemoveNPE()
    {
        mTree.remove(null);
    }

    @Test
    public void testContains()
    {
        for (BoxPartitionable e : mElements) {
            Assert.assertTrue(mTree.contains(e.getBounds()));
        }
    }

    @Test
    public void testContainsReturnsFalse()
    {
        final float max = Float.MAX_VALUE;
        final Bounds bounds = new MutableBounds(max, max, max, max, max, max);
        Assert.assertFalse(mTree.contains(bounds));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsNPE()
    {
        mTree.contains(null);
    }

    @Test
    public void testContainsElement()
    {
        for (BoxPartitionable e : mElements) {
            Assert.assertTrue(mTree.containsElement(e));
        }
    }

    @Test
    public void testContainsElementReturnsFalse()
    {
        final float max = Float.MAX_VALUE;
        final BoxPartitionable element = new BoxPartitionable(max, max, max, max, max, max);
        Assert.assertFalse(mTree.containsElement(element));
    }

    @Test (expected = NullPointerException.class)
    public void testContainsElementNPE()
    {
        mTree.containsElement(null);
    }

    @Test
    public void testClear()
    {
        mTree.clear();
        Assert.assertTrue(mTree.isEmpty());
    }

    @Test
    public void testEquals()
    {
        final LockingSpacePartitioningTree<BoxPartitionable> duplicateTree = new BinaryBVH<>();

        // Duplicate each pair's bounds to avoid exception from locking
        for (final BoxPartitionable e : mElements) {
            duplicateTree.add(new MutableBounds(e.getBounds()), e);
        }

        Assert.assertTrue(mTree.equals(duplicateTree));
    }

    @Test
    public void testEqualsReturnsFalse()
    {
        // Different content should fail
        mElements[0] = null;
        Assert.assertFalse(mTree.equals(createTree(mElements)));
    }

    @Test
    public void testEqualsTransitive()
    {
        final BinaryBVH<BoxPartitionable> duplicateA = createTree(mElements);
        final BinaryBVH<BoxPartitionable> duplicateB = createTree(mElements);

        // Transitive
        Assert.assertTrue(mTree.equals(duplicateA));
        Assert.assertTrue(duplicateA.equals(duplicateB));
        Assert.assertTrue(mTree.equals(duplicateB));
    }

    @Test
    public void testEqualsSymmetric()
    {
        final BinaryBVH<BoxPartitionable> duplicateA = createTree(mElements);

        Assert.assertTrue(mTree.equals(duplicateA));
        Assert.assertTrue(duplicateA.equals(mTree));
    }

    @Test
    public void testEqualsReflexive()
    {
        Assert.assertTrue(mTree.equals(mTree));
    }

    @Test
    public void testEqualsNull()
    {
        Assert.assertFalse(mTree.equals(null));
    }

    @Test
    public void testHashCode()
    {
        final BinaryBVH<BoxPartitionable> duplicateA = createTree(mElements);
        Assert.assertEquals(mTree.hashCode(), duplicateA.hashCode());
    }

    /**
     * <p>Creates a {@code BinaryBVH} with the {@code Partitionables} from the given array. Null indices in the
     * array are ignored.</p>
     *
     * @param elements to add to tree.
     * @return new tree.
     */
    private BinaryBVH<BoxPartitionable> createTree(BoxPartitionable[] elements)
    {
        final BinaryBVH<BoxPartitionable> tree = new BinaryBVH<>();
        for (final BoxPartitionable e : elements) {
            if (e == null) {
                continue;
            }

            final BoxPartitionable newElement = new BoxPartitionable(e);
            tree.add(newElement.getBounds(), newElement);
        }

        return tree;
    }

    /**
     * <p>Traverses the tree and returns true if the given {@code Partitionable} was found.</p>
     *
     * @param obj partitionable.
     * @return true if in tree.
     */
    private boolean isInTree(Partitionable obj)
    {
        return getTreeNodeFor(obj) != null;
    }

    /**
     * <p>Traverses the tree looking for a reference to the given {@code Partitionable} and returns the node holding
     * it in the tree.</p>
     *
     * @param obj partitionable.
     * @return node holding object in tree, or null if not in tree.
     */
    private Node getTreeNodeFor(Partitionable obj)
    {
        // No root implies empty tree
        if (mTree.getRoot() == null) {
            return null;
        }

        final Stack<Node> calls = new Stack<>();
        calls.push(mTree.getRoot());

        // Traverse tree
        while (!calls.isEmpty()) {

            final Node root = calls.pop();

            if (isLeaf(root) && root.getElement() == obj) {
                return root;
            } else {

                // Push next subtrees onto simulated call stack
                if (root.getLeft() != null) {
                    calls.push(root.getLeft());
                }
                if (root.getRight() != null) {
                    calls.push(root.getRight());
                }
            }
        }

        return null;
    }

    /**
     * <p>Checks if the given node has an element and no children.</p>
     *
     * @param node to check.
     * @return true if leaf.
     */
    private boolean isLeaf(Node node)
    {
        return node.getElement() != null && node.getLeft() == null && node.getRight() == null;
    }

    /**
     * <p>Gets the root node for half of the tree. The overall tree's left subtree is prioritized.</p>
     *
     * @return subtree root, or null if tree size is {@literal <} 2.
     */
    private Node getRootForHalfTree()
    {
        if (mTree.size() < 2) {
            return null;
        }

        Node root = mTree.getRoot();
        return (root.getLeft() != null) ? root.getLeft() : root.getRight();
    }

    /**
     * <p>Checks if the given {@code BoxPartitionable} has been visited through {@code mark(BoxPartitionable)}.</p>
     *
     * @param partitionable to check.
     * @return true if marked.
     * @throws IllegalArgumentException if partitionable is not in the list of objects populated during {@code setUp()}.
     */
    private boolean isMarked(BoxPartitionable partitionable)
    {
        for (int i = 0; i < mElements.length; i++) {
            if (mElements[i] == partitionable) {
                return mMarked[i];
            }
        }

        throw new IllegalArgumentException("Given Partitionable wasn't in the list of added objects");
    }

    /**
     * <p>Marks the {@code BoxPartitionable} so that calling {@code isMarked(BoxPartitionable)} with the same
     * instance returns true. This is used to signify that a {@code BoxPartitionable} has been visited.</p>
     *
     * @param partitionable to mark.
     * @throws IllegalArgumentException if partitionable is not in the list of objects populated during {@code setUp()}.
     */
    private void mark(BoxPartitionable partitionable)
    {
        for (int i = 0; i < mElements.length; i++) {
            if (mElements[i] == partitionable) {
                mMarked[i] = true;
                return;
            }
        }

        throw new IllegalArgumentException("Given Partitionable wasn't in the list of added objects");
    }
}