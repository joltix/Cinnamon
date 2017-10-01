package cinnamon.engine.utils;

import java.util.*;

/**
 * <p>Self-balancing binary tree implementation of a {@code LockingSpacePartitioningTree} using bounding boxes for
 * partitioning. This implementation follows the scheme of a bounding volume hierarchy where volumes that are not at
 * the leaves are merely container volumes for those below. As the significant portion of the {@code BinaryBVH} does
 * not hold workable elements, this has the effect of increased memory.</p>
 *
 * <b>Padding</b>
 * <p>Each containing volume can be expanded more than is necessary to encompass its children volumes through the
 * {@code padding} passed during construction. A larger than necessary containing volume could allow volumes below
 * it in the hierarchy to move without requiring a call to {@code update(Bounds)} on the moved element, avoiding the
 * need to balance the tree.</p>
 *
 * <b>Performance</b>
 * <p>{@code add(Bounds, E)}, {@code remove(Bounds)}, and {@code update(Bounds)} take O(log n) in the worst-case
 * with the generalized mass {@code update()} method taking O(n log n). The ancillary methods {@code size()},
 * {@code isEmpty()}, and {@code contains(Bounds)} are constant.</p>
 *
 * <p>It is slightly more efficient to synchronize all elements at once through {@code update()} instead of looping
 * through all elements and calling {@code update(Bounds)} unless only a handful of {@code Bounds}-element pairs
 * have been moved or resized, in which case the latter is sufficient.</p>
 */
public final class BinaryBVH<E> implements LockingSpacePartitioningTree<E>
{
    private static final float VOLUME_PADDING = 0.15f;
    private static final int INITIAL_NODE_CAPACITY = 50;
    private static final float NODE_GROWTH = 0.85f;

    // To minimize node instantiation in high frequency updates
    private final Queue<Node> mNodePool = new ArrayDeque<>();

    // Index for nodes associating bounds with an element
    private final IndexList<Node> mLeafNodes;

    // Percentage of a minimal containing box's size to expand by
    private final float mPadding;

    private Node mRoot;
    private int mSize;

    // Stack for non-recursive traversal
    private final Stack<Node> mFrames = new Stack<>();

    // Represents a point during point intersection testing
    private final MutableBounds mPointConvert = new MutableBounds();

    /**
     * <p>Constructs a {@code BinaryBVH}.</p>
     */
    public BinaryBVH()
    {
        this(VOLUME_PADDING, INITIAL_NODE_CAPACITY, NODE_GROWTH);
    }

    /**
     * <p>Constructs a {@code BinaryBVH} with a specific volume padding, initial capacity, and growth.</p>
     *
     * @param padding for containing boxes.
     * @param initialCapacity initial capacity for key-value pairs.
     * @param growth normalized factor for capacity expansion.
     * @throws IllegalArgumentException if padding {@literal <}= 0 or padding is {@literal >} 1, initialCapacity is
     * {@literal <}= 0, or growth is {@literal <}= 0 or growth {@literal >} 1.
     */
    public BinaryBVH(float padding, int initialCapacity, float growth)
    {
        if (padding <= 0f || padding > 1f) {
            throw new IllegalArgumentException("Padding must be > 0 and <= 1, padding: " + padding);
        }
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Initial capacity must be > 0, initialCapacity: " + initialCapacity);
        }
        if (growth <= 0f || growth > 1f) {
            throw new IllegalArgumentException("Growth factor must be > 0 and <= 1, growth: " + growth);
        }

        mPadding = padding / 2f;
        mLeafNodes = new IndexList<>(initialCapacity, growth);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public List<E> getContained(List<E> results, Bounds capture, int limit)
    {
        checkNull(results);
        checkNull(capture);
        checkLimit(limit);

        if (isEmpty()) {
            return results;
        }

        return query(results, capture, limit, Bounds::contains);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public E[] getContained(E[] results, Bounds capture, int limit)
    {
        checkNull(results);
        checkNull(capture);
        checkLimit(results, limit);

        if (isEmpty()) {
            return results;
        }

        return query(results, capture, limit, Bounds::contains);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public List<E> getIntersections(List<E> results, Bounds capture, int limit)
    {
        checkNull(results);
        checkNull(capture);
        checkLimit(limit);

        if (isEmpty()) {
            return results;
        }

        return query(results, capture, limit, (Bounds query, Bounds bounds) -> {return true;});
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public E[] getIntersections(E[] results, Bounds capture, int limit)
    {
        checkNull(results);
        checkNull(capture);
        checkLimit(results, limit);

        if (isEmpty()) {
            return results;
        }

        return query(results, capture, limit, (Bounds query, Bounds bounds) -> {return true;});
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public List<E> getIntersections(List<E> results, Point point, int limit)
    {
        checkNull(results);
        checkNull(point);
        checkLimit(limit);

        if (isEmpty()) {
            return results;
        }

        final Bounds capture = convertPointToBounds(point);
        return query(results, capture, limit, Bounds::contains);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public E[] getIntersections(E[] results, Point point, int limit)
    {
        checkNull(results);
        checkNull(point);
        checkLimit(results, limit);

        if (isEmpty()) {
            return results;
        }

        final Bounds capture = convertPointToBounds(point);
        return query(results, capture, limit, Bounds::contains);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    @Override
    public boolean add(Bounds bounds, E element)
    {
        checkNull(bounds);
        checkNull(element);

        if (bounds.isExclusivelyPartitioned()) {
            final Node node = mLeafNodes.get(bounds.getIndex());

            if (node == null || node.mBounds != bounds) {
                throw new IllegalArgumentException("Bounds already locked by another locking tree");
            } else {
                // Bounds' already in this tree
                return false;
            }
        }

        // Assign node for fast lookup
        final Node leaf = getNode();
        leaf.mElement = element;
        leaf.mBounds = bounds;
        bounds.setIndex(mLeafNodes.add(leaf));

        if (isEmpty()) {
            mRoot = leaf;
            mRoot.mHeight = 0;

        } else {
            // Insert at bottom in a new subtree
            addChildToSubtree(leaf, findFutureSibling(bounds));
            climbAndBalance(leaf);
        }

        mSize++;
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public E remove(Bounds bounds)
    {
        if (!contains(bounds)) {
            return null;
        }

        final Node leaf = mLeafNodes.remove(bounds.getIndex());
        assert (leaf != null && leaf.mElement != null);

        climbAndBalance(removeLeaf(leaf).mParent);

        // Remove association with tree
        bounds.setIndex(Bounds.UNPARTITIONED);
        final E element = leaf.mElement;
        recycleNode(leaf);

        mSize--;
        return element;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean update(Bounds bounds)
    {
        return contains(bounds) && attemptToUpdate(mLeafNodes.get(bounds.getIndex()));
    }

    @Override
    public boolean update()
    {
        boolean hadToUpdate = false;
        for (final Node node : mLeafNodes) {
            if (attemptToUpdate(node)) {
                hadToUpdate = true;
            }
        }

        return hadToUpdate;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public E getElement(Bounds bounds)
    {
        if (bounds == null) {
            throw new NullPointerException();
        } else if (!bounds.isExclusivelyPartitioned()) {
            // Not in a locking tree
            return null;
        }

        final Node node = mLeafNodes.get(bounds.getIndex());
        return (node != null) ? node.mElement : null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean contains(Bounds bounds)
    {
        checkNull(bounds);

        // Since this is a locking tree, bounds should be locked if inside
        if (!bounds.isExclusivelyPartitioned()) {
            return false;
        }

        // Differing bounds references implies partitioned in different tree
        final Node node = mLeafNodes.get(bounds.getIndex());
        return (node.mBounds != null && node.mBounds.equals(bounds));
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public boolean containsElement(E element)
    {
        checkNull(element);

        for (final Node node : mLeafNodes) {
            if (node.mElement.equals(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear()
    {
        final Iterator<Node> iter = mLeafNodes.iterator();
        while (iter.hasNext()) {
            final Node node = iter.next();

            // Unlock for use in other locking trees
            node.mBounds.setIndex(Bounds.UNPARTITIONED);

            recycleNode(node);
            iter.remove();
        }

        mSize = 0;
    }

    @Override
    public int size()
    {
        return mSize;
    }

    @Override
    public boolean isEmpty()
    {
        return mSize == 0;
    }

    @Override
    public int hashCode()
    {
        int hash = 17;
        for (final Node node : mLeafNodes) {
            int pair = 527 + node.mElement.hashCode();
            pair = 31 * pair + node.mBounds.hashCode();
            hash = 31 * hash + pair;
        }

        return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof SpacePartitioningTree)) {
            return false;
        } else if (obj == this) {
            return true;
        }

        // Each bounds in given tree should have same element
        final BinaryBVH bvh = (BinaryBVH) obj;
        for (final Node node : mLeafNodes) {
            if (!node.mElement.equals(bvh.getElement(node.mBounds))) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>Gets the root of the entire tree for external tree traversal.</p>
     *
     * @return root node.
     */
    Node getRoot()
    {
        return mRoot;
    }

    /**
     * <p>Adds all elements to the given {@code List} accepted according to the specified {@code AcceptancePolicy} and
     * capturing {@code Bounds}.</p>
     *
     * @param results output list.
     * @param capture capturing bounds.
     * @param limit max number to add.
     * @param policy to decide whether an element is added.
     * @return output list.
     */
    private List<E> query(List<E> results, Bounds capture, int limit, AcceptancePolicy policy)
    {
        pushRootToStack();
        Node cursor;
        int count = 0;

        // Search tree from root
        while (!mFrames.isEmpty()) {
            cursor = mFrames.pop();

            // No intersection means wrong subtree
            if (!capture.intersects(cursor.getBounds())) {
                continue;
            }

            if (isLeaf(cursor)) {
                // Ignore capture
                if (capture == cursor.mContainer) {
                    continue;
                }

                if (policy.accepts(capture, cursor.getBounds())) {
                    results.add(cursor.mElement);
                }
                if (count >= limit) {
                    break;
                }

            } else {
                pushChildrenToStack(cursor);
            }
        }
        return results;
    }

    /**
     * <p>Adds all elements to the given array accepted according to the specified {@code AcceptancePolicy} and
     * capturing {@code Bounds}.</p>
     *
     * @param results output array.
     * @param capture capturing bounds.
     * @param limit max number to add.
     * @param policy to decide whether an element is added.
     * @return output array.
     */
    private E[] query(E[] results, Bounds capture, int limit, AcceptancePolicy policy)
    {
        pushRootToStack();
        Node cursor;
        int count = 0;

        // Search tree from root
        while (!mFrames.isEmpty()) {
            cursor = mFrames.pop();

            // No intersection means wrong subtree
            if (!capture.intersects(cursor.getBounds())) {
                continue;
            }

            if (isLeaf(cursor)) {
                // Ignore capture
                if (capture == cursor.mContainer) {
                    continue;
                }

                if (policy.accepts(capture, cursor.getBounds())) {
                    results[count++] = cursor.mElement;
                }
                if (count >= limit) {
                    break;
                }

            } else {
                pushChildrenToStack(cursor);
            }
        }
        return results;
    }

    /**
     * <p>Checks the given node if its position in space has moved enough such that its in-tree position is no
     * longer valid (i.e. no longer contained by its parent's box). In such a situation, the {@code Bounds} and
     * element of the given node is removed from the tree and added again.</p>
     *
     * @param node to update.
     * @return true if the bounds-element pair had to be reinserted.
     */
    private boolean attemptToUpdate(Node node)
    {
        // Nothing to update if only one in tree (null parent implies container is root)
        final Node parent = node.mParent;
        if (parent == null) {
            return false;
        }

        // Reinsert entity if parent box no longer contains it
        if (!parent.mContainer.contains(node.mBounds)) {
            add(node.mBounds, remove(node.mBounds));
            return true;
        }

        return false;
    }

    /**
     * <p>Empties the simulated call stack used in non-recursive tree traversal then pushes the tree's root. This
     * method is called at the beginning of non-recursive tree traversal to make the root's subtree (the entire tree)
     * available to visit through popping.</p>
     */
    private void pushRootToStack()
    {
        mFrames.clear();
        mFrames.push(mRoot);
    }

    /**
     * <p>Pushes the specified node's left and right children onto the simulated call stack. This method is used in
     * non-recursive tree traversal.</p>
     *
     * @param node subtree root.
     */
    private void pushChildrenToStack(Node node)
    {
        mFrames.push(node.mRight);
        mFrames.push(node.mLeft);
    }

    /**
     * <p>Removes a given leaf and its parent from the tree's structure and returns the first ancestor that's still
     * connected to the tree. If the leaf to remove has a sibling, the sibling takes the parent's place and is
     * returned as the ancestor. The removed parent is set aside in the node pool.</p>
     *
     * @param node leaf.
     * @return remaining ancestor.
     */
    private Node removeLeaf(Node node)
    {
        assert(isLeaf(node));

        // Case of only node in tree
        final Node parent = node.mParent;
        if (parent == null) {
            mRoot = null;
            return node;
        } else {

            final Node grandparent = parent.mParent;
            final Node sibling = (node == parent.mLeft) ? parent.mRight : parent.mLeft;
            recycleNode(parent);

            // Parent is root
            if (grandparent == null) {
                mRoot = sibling;

            } else if (grandparent.mLeft == parent) {
                // Sibling replaces parent
                grandparent.mLeft = sibling;
            } else {
                grandparent.mRight = sibling;
            }

            sibling.mParent = grandparent;
            return sibling;
        }
    }

    /**
     * <p>Gets an unused node.</p>
     *
     * @return unused node.
     */
    private Node getNode()
    {
        return (mNodePool.isEmpty()) ? new Node() : mNodePool.poll();
    }

    /**
     * <p>Empties a node of all references to both the tree and its held data and adds the node to the node pool.</p>
     *
     * @param node to set aside.
     */
    private void recycleNode(Node node)
    {
        node.mElement = null;
        node.mBounds = null;
        node.mContainer = null;
        node.mLeft = null;
        node.mRight = null;
        node.mParent = null;

        mNodePool.add(node);
    }

    /**
     * <p>Creates a parent sized to encompass a node and its sibling. The parent takes the sibling's place in the
     * tree while both given nodes are made the parent's children. The tree's size is not updated but the parent's
     * height is measured.</p>
     *
     * <p>This method is meant only for leaf nodes.</p>
     *
     * @param newChild Node to be added.
     * @param sibling sibling Node in tree.
     */
    private void addChildToSubtree(Node newChild, Node sibling)
    {
        assert (isLeaf(sibling) && isLeaf(newChild));

        // Create parent to encompass children
        final Node parent = getNode();
        parent.mContainer = new MutableBounds();

        // Connect subtree to overall tree
        final Node grandparent = sibling.mParent;
        parent.mParent = grandparent;
        if (grandparent == null) {
            mRoot = parent;
        } else if (grandparent.mLeft == sibling) {
            grandparent.mLeft = parent;
        } else {
            grandparent.mRight = parent;
        }

        // Connect parent and children
        parent.mLeft = sibling;
        parent.mRight = newChild;
        sibling.mParent = parent;
        newChild.mParent = parent;

        resizeParentToContainChildren(parent);
        updateHeight(parent);
    }

    /**
     * <p>Climbs up to the tree's root from the given node while performing rotations, updating heights, and
     * resizing boxes to contain subtrees. Balancing is in the style of AVL.</p>
     *
     * @param from starting node (inclusive).
     */
    private void climbAndBalance(Node from)
    {
        Node root = from;
        while (root != null) {
            final int balance = (root.mLeft == null || root.mRight == null) ? 0 : computeBalance(root);

            // Left subtree is higher
            if (balance > 1) {
                final Node left = root.mLeft;
                final int subtreeBalance = computeBalance(left);

                // Left subtree's right subtree is higher
                if (subtreeBalance <= -1) {
                    final Node leftRight = left.mRight;
                    rotate(leftRight, left, false);
                    rotate(leftRight, root, true);
                    resizeParentToContainChildren(left);
                    resizeParentToContainChildren(root);
                    resizeParentToContainChildren(leftRight);
                    root = leftRight;

                } else {
                    // Rotate when right subtree is short to minimize branch transfer
                    rotate(left, root, true);
                    resizeParentToContainChildren(root);
                    resizeParentToContainChildren(left);
                    root = left;
                }

                // Right subtree is higher
            } else if (balance < -1) {
                final Node right = root.mRight;
                final int subtreeBalance = computeBalance(right);

                // Right subtree's left subtree is higher
                if (subtreeBalance >= 1) {
                    final Node rightLeft = right.mLeft;
                    rotate(rightLeft, right, true);
                    rotate(rightLeft, root, false);
                    resizeParentToContainChildren(right);
                    resizeParentToContainChildren(root);
                    resizeParentToContainChildren(rightLeft);
                    root = rightLeft;

                } else {
                    // Rotate when left subtree is short to minimize branch transfer
                    rotate(right, root, false);
                    resizeParentToContainChildren(root);
                    resizeParentToContainChildren(right);
                    root = right;
                }
            } else {
                // No imbalance
                updateHeight(root);
                resizeParentToContainChildren(root);
            }

            root = root.mParent;
        }
    }

    /**
     * <p>Change a given node's {@code MutableBounds}'s size and position to encompass the node's two subtrees. If
     * either left or right child is null, the parent's {@code MutableBounds} is resized to match the remaining. If
     * both children are null, this method does nothing.</p>
     *
     * @param parent node to resize.
     */
    private void resizeParentToContainChildren(Node parent)
    {
        // If either is null, parent's box matches the one that exists
        final Node left = (parent.mLeft != null) ? parent.mLeft : parent.mRight;
        final Node right = (parent.mRight != null) ? parent.mRight : parent.mLeft;

        if (left == null) {
            return;
        }
        final Bounds boundsA = left.getBounds();
        final Bounds boundsB = right.getBounds();

        final float minX = Math.min(boundsA.getX(), boundsB.getX());
        final float maxX = Math.max(boundsA.getMaximumX(), boundsB.getMaximumX());

        final float minY = Math.min(boundsA.getY(), boundsB.getY());
        final float maxY = Math.max(boundsA.getMaximumY(), boundsB.getMaximumY());

        final float minZ = Math.min(boundsA.getZ(), boundsB.getZ());
        final float maxZ = Math.max(boundsA.getMaximumZ(), boundsB.getMaximumZ());

        // Compute padding to enlarge parent by
        final float padX = (maxX - minX) * mPadding;
        final float padY = (maxY - minY) * mPadding;
        final float padZ = (maxZ - minZ) * mPadding;

        parent.mContainer.encompass(minX - padX, minY - padX, minZ - padZ, maxX + padX, maxY + padY, maxZ +
                padZ);
    }

    /**
     * <p>Computes the volume of a box containing two given {@code Bounds} used as the box's corners.</p>
     *
     * @param boundsA one box.
     * @param boundsB other box.
     * @return volume.
     */
    private float computeVolumeBetween(Bounds boundsA, Bounds boundsB)
    {
        final float minX = Math.min(boundsA.getX(), boundsB.getX());
        final float maxX = Math.max(boundsA.getX() + boundsA.getWidth(), boundsB.getX() + boundsB.getWidth());

        final float minY = Math.min(boundsA.getY(), boundsB.getY());
        final float maxY = Math.max(boundsA.getY() + boundsA.getHeight(), boundsB.getY() + boundsB.getHeight());

        final float minZ = Math.min(boundsA.getZ(), boundsB.getZ());
        final float maxZ = Math.max(boundsA.getZ() + boundsA.getDepth(), boundsB.getZ() + boundsB.getDepth());

        // Compute volume
        return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
    }

    /**
     * <p>Finds the node holding the {@code Bounds} that should be the sibling of a given {@code Bounds} when the
     * given {@code Bounds} is inserted. To determine the sibling, the tree is searched for a leaf whose element's
     * {@code Bounds} would form a minimal volume with the given {@code Bounds} where the formed volume is defined by
     * the minimum and maximum points of both leaves' {@code Bounds}.</p>
     *
     * @param bounds to be inserted.
     * @return future sibling.
     */
    private Node findFutureSibling(Bounds bounds)
    {
        assert (!isEmpty());

        // Find encompassing box
        Node cursor = mRoot;
        while (cursor != null) {

            // Found sibling
            if (isLeaf(cursor)) {
                break;
            }

            final Node left = cursor.getLeft();
            final Node right = cursor.getRight();

            if (left == null) {
                cursor = right;
            } else if (right == null) {
                cursor = left;
            } else {
                // Measure what each bounds' volume would be for each child, if added
                final float leftVolume = computeVolumeBetween(left.getBounds(), bounds);
                final float rightVolume = computeVolumeBetween(right.getBounds(), bounds);

                // Go towards whichever would give the smallest volume
                cursor = (leftVolume <= rightVolume) ? left : right;
            }
        }

        return cursor;
    }

    /**
     * <p>Checks if a given node has no children. A node without children also has an element and no container
     * {@code MutableBounds}. If at least one child exists, the reverse is true, i.e. there is no element but there
     * is a {@code MutableBounds}.</p>
     *
     * @param node to examine.
     * @return true if node has no children and has an element.
     */
    private boolean isLeaf(Node node)
    {
        return node.mLeft == null && node.mRight == null && node.mElement != null;
    }

    /**
     * <p>Rotates a node and its parent to the right or left.</p>
     *
     * @param node Node to rotate.
     * @param parent Parent of node.
     * @param right true to rotate right, false for left rotation.
     */
    private void rotate(Node node, Node parent, boolean right)
    {
        // Child must be left child if rotating right, right child if rotating left
        assert ((right && node == parent.mLeft) || (!right && node == parent.mRight));

        // Rotate nodes right such that the parent becomes node's right child
        if (right) {
            parent.mLeft = node.mRight;
            if (node.mRight != null) {
                node.mRight.mParent = parent;
            }
            node.mRight = parent;

        } else {
            // Rotate nodes left such that the parent becomes node's left child
            parent.mRight = node.mLeft;
            if (node.mLeft != null) {
                node.mLeft.mParent = parent;
            }
            node.mLeft = parent;
        }

        // No grandparent implies parent grandparent is root
        if (parent.mParent == null) {
            mRoot = node;
            node.mParent = null;

        } else if (parent.mParent.mLeft == parent) {
            parent.mParent.mLeft = node;
        } else {
            parent.mParent.mRight = node;
        }

        // Make node the new "parent" and node as old parent's parent
        node.mParent = parent.mParent;
        parent.mParent = node;

        updateHeight(parent);
        updateHeight(node);
    }

    /**
     * <p>Computes the difference in balance between the given node's children. The returned balance's sign
     * identifies which child has a larger height; a positive difference indicates the left subtree has a taller
     * height while a negative difference indicates the right subtree is shorter.</p>
     *
     * <p>If either of the given node's children is null, its respective height is considered 0. The given node must
     * not be null.</p>
     *
     * @param node to measure for.
     * @return difference in children's heights.
     */
    private int computeBalance(Node node)
    {
        final int left = (node.mLeft == null) ? 0 : node.mLeft.mHeight;
        final int right = (node.mRight == null) ? 0 : node.mRight.getHeight();

        return left - right;
    }

    /**
     * <p>Makes sure the height of a given node is the largest height of either of its children, + 1.</p>
     *
     * @param node to update.
     */
    private void updateHeight(Node node)
    {
        final int left = (node.mLeft != null) ? node.mLeft.mHeight : -1;
        final int right = (node.mRight != null) ? node.mRight.mHeight : -1;

        node.mHeight = 1 + Math.max(left, right);
    }

    /**
     * <p>Converts the given {@code Point} into a {@code Bounds}.</p>
     *
     * @param point to convert.
     * @return bounds form.
     */
    private Bounds convertPointToBounds(Point point)
    {
        mPointConvert.encompass(point);
        return mPointConvert;
    }

    /**
     * <p>Throws a {@code NullPointerException} if the given object is null.</p>
     *
     * @param object to check.
     * @throws NullPointerException if object is null.
     */
    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Throws an {@code IllegalArgumentException} if the limit is {@literal <}= 0.</p>
     *
     * @param limit limit.
     * @throws IllegalArgumentException if limit {@literal <}= 0.
     */
    private void checkLimit(int limit)
    {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be > 0, limit: " + limit);
        }
    }

    /**
     * <p>Throws an {@code IllegalArgumentException} if the given array's length is less than the specified limit.</p>
     *
     * @param array array.
     * @param limit minimum array length.
     * @throws IllegalArgumentException if array length {@literal <} limit.
     */
    private void checkLimit(E[] array, int limit)
    {
        if (array.length < limit) {
            throw new IllegalArgumentException("Array length must be >= limit, array: " + array.length + ", limit: "
                    + limit);
        }
    }

    /**
     * <p>Holds either a box or {@code Bounds}-element pair. A node stores a reference to its parent, left and
     * right children, and its subtree's height + 1.</p>
     */
    final class Node
    {
        // Box for non-leaf
        private MutableBounds mContainer;

        // Leaf data
        private Bounds mBounds;
        private E mElement;

        private int mHeight;

        private Node mParent;
        private Node mLeft;
        private Node mRight;

        private Node() {}

        /**
         * <p>Gets the parent.</p>
         *
         * @return parent node.
         */
        public Node getParent()
        {
            return mParent;
        }

        /**
         * <p>Gets the left child.</p>
         *
         * @return left child node.
         */
        public Node getLeft()
        {
            return mLeft;
        }

        /**
         * <p>Gets the right child.</p>
         *
         * @return right child node.
         */
        public Node getRight()
        {
            return mRight;
        }

        /**
         * <p>Gets the {@code Bounds} representing the node's volume in the tree.</p>
         *
         * <p>For leaf nodes, the returned {@code Bounds} is the stored element's paired {@code Bounds}. For non-leaf
         * nodes, the returned {@code Bounds} is the volume encompassing all elements in the subtree of the node in
         * question.</p>
         *
         * @return bounds.
         */
        public Bounds getBounds()
        {
            return (mElement == null) ? mContainer : mBounds;
        }

        /**
         * <p>Gets the element.</p>
         *
         * @return element.
         */
        public E getElement()
        {
            return mElement;
        }

        /**
         * <p>Gets the height of the subtree with this node as root.</p>
         *
         * @return height.
         */
        public int getHeight()
        {
            return mHeight;
        }
    }

    /**
     * <p>{@code AcceptancePolicies} wrap the decision as to whether or not an element is added to an output
     * container. For containment queries, the policy wraps {@link Bounds#contains(Bounds)} and its overloads.
     * Intersection queries are handled in the same way except with {@link Bounds#intersects(Bounds)}.</p>
     */
    private interface AcceptancePolicy
    {
        /**
         * <p>Decides if a given {@code Bounds} is accepted by a capturing {@code Bounds}.</p>
         *
         * @param capture acceptance gate.
         * @param bounds to test.
         * @return true if bounds is accepted by capture.
         */
        boolean accepts(Bounds capture, Bounds bounds);
    }
}
