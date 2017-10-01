package cinnamon.engine.utils;

import cinnamon.engine.utils.BinaryBVH.Node;
import cinnamon.engine.utils.BinaryBVHTestSuite.BoxPartitionable;
import org.junit.*;

import java.util.Random;
import java.util.Stack;

/**
 * <p>Tests the following properties of the {@code BinaryBVH}'s internal structure.</p>
 * <ul>
 *     <li>Only leaves have elements.</li>
 *     <li>The absolute difference in height between the two halves of a node's subtree is either 0 or 1.</li>
 *     <li>Each node's {@code Bounds} contains its children nodes' {@code Bounds}.</li>
 * </ul>
 */
public class BinaryBVHPropertyTest
{
    private static final String MSG_BALANCE_FAIL = "Children height difference greater than 1, difference: ";

    private static final int ELEMENT_COUNT = 1_000_000;

    private Random mRNG;
    private BinaryBVH<BoxPartitionable> mTree;

    @Before
    public void setUp()
    {
        mRNG = new Random(System.nanoTime());
        mTree = new BinaryBVH<>();
        final BoxPartitionable[] elements = BinaryBVHTestSuite.generateDistinctBoxPartitionables(ELEMENT_COUNT);

        for (final BoxPartitionable e : elements) {
            mTree.add(e.getBounds(), e);
        }

        // To simulate actual use, perform some removes
        boolean removedAtLeastOnce = false;
        for (final BoxPartitionable e : elements) {
            if (mRNG.nextBoolean()) {
                removedAtLeastOnce = true;
                mTree.remove(e.getBounds());
            }
        }

        // In case random didn't remove any
        if (!removedAtLeastOnce) {
            mTree.remove(elements[0].getBounds());
        }
    }

    @After
    public void tearDown()
    {
        mRNG = null;
        mTree = null;
    }

    @Test
    public void testOnlyLeavesShouldHaveData()
    {
        final Stack<Node> frames = new Stack<>();
        frames.push(mTree.getRoot());

        while (!frames.isEmpty()) {
            final Node root = frames.pop();
            final Node left = root.getLeft();
            final Node right = root.getRight();

            if (left != null) {
                frames.push(left);
            }
            if (right != null) {
                frames.push(right);
            }

            // Only leaves should have elements
            if (left == null && right == null) {
                Assert.assertNotNull(root.getElement());
            } else {
                Assert.assertNull(root.getElement());
            }
        }
    }

    @Test
    public void testEachSubtreeIsBalanced()
    {
        measureHeight(mTree.getRoot());
    }

    @Test
    public void testParentContainsChildren()
    {
        final Stack<Node> frames = new Stack<>();
        frames.push(mTree.getRoot());

        while (!frames.isEmpty()) {
            final Node root = frames.pop();

            // Test left child's bounds
            final Node left = root.getLeft();
            if (left != null) {
                frames.push(left);
                Assert.assertTrue(root.getBounds().contains(left.getBounds()));
            }

            // Test right child's bounds
            final Node right = root.getRight();
            if (right != null) {
                frames.push(right);
                Assert.assertTrue(root.getBounds().contains(right.getBounds()));
            }
        }
    }

    /**
     * <p>Compares the height of the given node's left and right children for an absolute difference of either 0 or 1
     * and returns the taller height.</p>
     *
     * <p>Although there are no edges leading to the root of a tree, this method counts the root as if it did and so
     * returns the root's height as the taller height of its children + 1. The given node must not be null.</p>
     *
     * @param root of subtree.
     * @return subtree height.
     */
    private int measureHeight(BinaryBVH.Node root)
    {
        final BinaryBVH.Node left = root.getLeft();
        final BinaryBVH.Node right = root.getRight();

        // Reached leaf
        if (left == null && right == null) {
            return 1;
        }

        int leftHeight = 0;
        int rightHeight = 0;
        if (left != null) {
            leftHeight = measureHeight(left);
        }
        if (right != null) {
            rightHeight = measureHeight(right);
        }

        // Compare height
        final int heightDiff = Math.abs(leftHeight - rightHeight);
        Assert.assertTrue(MSG_BALANCE_FAIL + heightDiff, heightDiff <= 1);

        return Math.max(leftHeight, rightHeight) + 1;
    }
}
