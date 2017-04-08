package com.cinnamon.object;

import com.cinnamon.utils.AxisAlignedRect;
import com.cinnamon.utils.Rect2D;

import java.util.List;
import java.util.Stack;

/**
 * <p>
 *     Organizes {@link BodyComponent}s in a bounding area hierarchy for detecting bounding box collisions between
 *     bodies. This class is used for eliminating potential collisions from reaching the narrow phase of collision
 *     detection by comparing axis aligned {@link Rect2D}s surrounding each BodyComponent before more complex
 *     detection algorithms are used.
 * </p>
 */
public final class BoundingTree
{
    // Root
    private Node mRoot;

    // Number of BodyComponents
    private int mSize;

    // Stack for traversal without recursion
    private Stack<Node> mFrames = new Stack<Node>();

    // Grand child node used when climbing up the tree to balance
    private Node mGrandchild;

    // Child node used when climbing up the tree to balance
    private Node mChild;

    /**
     * <p>Fills a {@link List} with {@link BodyComponent}s that collide with a given BodyComponent.</p>
     *
     * @param collisions List to fill.
     * @param body BodyComponent to test against.
     * @throws IllegalArgumentException if the given List is not empty.
     */
    public void getCollisions(List<BodyComponent> collisions, BodyComponent body)
    {
        // Ensure incoming list is ready to be filled
        if (!collisions.isEmpty()) {
            throw new IllegalArgumentException("List must be empty");
        }

        // No potential collisions if no objs
        if (isEmpty()) {
            return;
        }

        // Begin traversal from root
        Node cursor = mRoot;
        mFrames.clear();
        mFrames.push(cursor);

        // Traverse tree without recursion
        while (!mFrames.isEmpty()) {
            // Get Node to examine
            cursor = mFrames.pop();

            // Add Node's BodyComponent to list if leaf and intersects
            if (isLeaf(cursor)) {
                final Rect2D bodyRect = body.getBounds();
                final Rect2D cursorRect = cursor.mBody.getBounds();
                if (body != cursor.mBody && bodyRect.intersects(cursorRect)) {
                    collisions.add(cursor.mBody);
                }

            } else {

                // If Body doesn't intersect this Node, then ignore this subtree
                if (!cursor.mBox.intersects(body.getBounds())) {
                    continue;
                }

                // Add both directions to be visited in the next two loops
                mFrames.push(cursor.mRight);
                mFrames.push(cursor.mLeft);
            }
        }
    }

    /**
     * <p>Adds a {@link BodyComponent} to be placed for collision checks.</p>
     *
     * @param body BodyComponent.
     * @return true if the body was successfully added, false if already in the tree.
     */
    public boolean add(BodyComponent body)
    {
        // Can't add if already in tree
        if (contains(body)) {
            return false;
        }

        // Add body as root when empty
        if (isEmpty()) {
            mRoot = new Node();
            mRoot.mBody = body;
            body.setContainer(mRoot);
            mRoot.mHeight = 0;
        } else {

            // Create Node for new body
            final Node node = new Node();
            node.mBody = body;
            body.setContainer(node);

            // Get desired sibling Node for new child Node
            final Node sibling = findSibling(body);

            // Rearrange sibling and new child as children of a new container Node
            containChildren(sibling, node);

            // Traverse up tree to ensure parents' sizes still contain child Nodes
            climbAndBalance(sibling.mParent);
        }

        // Update count
        mSize++;

        return true;
    }

    /**
     * <p>Removes a {@link BodyComponent}.</p>
     *
     * @param body BodyComponent.
     */
    public boolean remove(BodyComponent body)
    {
        // Remove container from body
        final BoundingTree.Node container = body.getContainer();

        // Wasn't added if no Node ref or refs wrong tree
        if (container == null || !contains(body)) {
            return false;
        }

        // Remove association with tree
        body.setContainer(null);
        container.mBody = null;

        // Get refs to parent to be removed, grandparent, and sibling to replace parent
        final BoundingTree.Node parent = container.mParent;

        // Null parent implies container is last Node
        if (parent == null) {
            assert (mRoot == container && size() == 1);

            mRoot = null;
        } else {

            // Remove container and parent reference (and link sibling to grandparent)
            final BoundingTree.Node grandParent = parent.mParent;
            final BoundingTree.Node sibling = (parent.mLeft == container) ? parent.mRight : parent.mLeft;

            // Null grandparent means depth 1 -> sibling should become root
            if (grandParent == null) {
                mRoot = sibling;

                // Link grandparent and sibling together
            } else if (grandParent.mLeft == parent) {
                grandParent.mLeft = sibling;
            } else {
                grandParent.mRight = sibling;
            }
            sibling.mParent = grandParent;
        }

        // Update obj count
        mSize--;

        return true;
    }

    /**
     * <p>Checks and updates, if needed, a {@link BodyComponent}'s placement within the BoundingTree. This method
     * should be called for a BodyComponent after it has moved or its size has changed in order to ensure proper
     * placement within the tree for correct results from {@link #getCollisions(List, BodyComponent)}.</p>
     *
     * @param body {@link BodyComponent} to update.
     * @return true if the BodyComponent's in-tree position had to be updated.
     */
    public boolean update(BodyComponent body)
    {
        // Bail out if no owning tree or owned by another
        final Node container = body.getContainer();
        if (container == null || !container.isOwnedBy(this)) {
            return false;
        }

        // Nothing to update if only one in tree (null parent implies container is root)
        final Node parent = container.mParent;
        if (parent == null) {
            return false;
        }

        // Update position in tree if container's parent can no longer accommodate it
        final Rect2D parentRect = parent.mBox;
        if (!parentRect.contains(container.mBody.getBounds())) {
            remove(body);
            add(body);
            return true;
        }

        // No update was performed
        return false;
    }

    /**
     * <p>Climbs up the tree performing rotations where necessary for a rough balance. Each bounding box visited on
     * the way to the root is also sized to ensure containment of its children.</p>
     *
     * @param start Node to begin climbing from (inclusive).
     */
    private void climbAndBalance(Node start)
    {
        // Prepare refs for a Node and two Nodes into its past
        Node cursor = start;
        mGrandchild = null;
        mChild = null;

        // Traverse up the tree till the root
        while (cursor != null) {

            // Update height stored in cursor Node
            updateHeight(cursor);

            // Perform rotations to balance
            cursor = balance(cursor, mChild, mGrandchild);

            // Ensure AABB fully contains children's dimensions
            resizeContainer(cursor.mBox, cursor.mLeft.getRect(), cursor.mRight.getRect());

            // Move up tree while tracking child and grandchild Nodes
            mGrandchild = mChild;
            mChild = cursor;
            cursor = cursor.mParent;
        }
    }

    /**
     * <p>Attempts to balance the tree with rotations. Whether or not rotation(s) occur depends on if an imbalance
     * is detected at a given Node.</p>
     *
     * <p>Balancing is decided with AVL's balance factor. There are four cases of rotations, requiring the examined
     * {@link Node}'s previously visited child and grandchild in order to resolve cases requiring two rotations.</p>
     *
     * @param node examined Node.
     * @param child examined's child.
     * @param grandChild examined's grandchild.
     * @return Node at same position in tree as before balance.
     */
    private Node balance(Node node, Node child, Node grandChild)
    {
        if (grandChild == null) {
            return node;
        }

        final Node left = node.mLeft;
        final Node right = node.mRight;

        final int balFactor = (left.mHeight) - (right.mHeight);

        if (balFactor > 1 || balFactor < -1) {
            if (child == node.mLeft) {
                if (grandChild == child.mLeft) {
                    // Double left case
                    rotate(child, node, true);

                    mChild = child.mLeft;
                    mGrandchild = mChild.mLeft;

                    return child;
                } else {
                    // Left right case
                    rotate(grandChild, child, false);
                    rotate(grandChild, node, true);

                    mChild = grandChild.mLeft;
                    mGrandchild = mChild.mRight;

                    return grandChild;
                }
            } else {
                if (grandChild == child.mRight) {
                    // Double right case
                    rotate(child, node, false);

                    mChild = child.mRight;
                    mGrandchild = mChild.mRight;

                    return child;
                } else {
                    // Right left case
                    rotate(grandChild, child, true);
                    rotate(grandChild, node, false);

                    mChild = grandChild.mRight;
                    mGrandchild = mChild.mLeft;

                    return grandChild;
                }
            }
        }

        return node;
    }

    /**
     * <p>Creates a {@link Node} with a {@link Rect2D} sized to encompass a {@link Node} meant to be the sibling of a
     * Node to be added to the tree. The containing Node takes the sibling's place in the tree while the sibling and
     * added Node are made the container's children.</p>
     *
     * <p>This method is meant only for leaf Nodes.</p>
     *
     * <p>The tree size is not updated.</p>
     *
     * @param sibling sibling Node in tree.
     * @param child Node to be added.
     */
    private void containChildren(Node sibling, Node child)
    {
        assert (isLeaf(sibling) && isLeaf(child));

        // Create bounding box, size, and relocate to surround children Nodes
        final Rect2D box = new AxisAlignedRect(0, 0);
        resizeContainer(box, sibling.mBody.getBounds(), child.mBody.getBounds());

        // Create encompassing Node
        final Node container = new Node();
        container.mBox = box;

        // Link container and parent
        final Node parent = sibling.mParent;
        container.mParent = parent;
        if (parent == null) {
            mRoot = container;
            container.mParent = null;
        } else if (parent.mLeft == sibling) {
            parent.mLeft = container;
        } else {
            parent.mRight = container;
        }

        // Make new child and sibling children of container
        container.mLeft = child;
        container.mRight = sibling;
        child.mParent = container;
        sibling.mParent = container;
    }

    /**
     * <p>Change a given {@link Rect2D}'s size to contain two other Rect2Ds.</p>
     *
     * @param container Rect2D to resize.
     * @param rect0 contained Rect2D.
     * @param rect1 other contained Rect2D.
     */
    private void resizeContainer(Rect2D container, Rect2D rect0, Rect2D rect1)
    {
        // Compute minimum and maximum x coords to get containing width
        final float minX = Math.min(rect0.getX(), rect1.getX());
        final float maxX = Math.max(rect0.getX() + rect0.getWidth(), rect1.getX() + rect1.getWidth());

        // Compute minimum and maximum y coords to get containing height
        final float minY = Math.min(rect0.getY(), rect1.getY());
        final float maxY = Math.max(rect0.getY() + rect0.getHeight(), rect1.getY() + rect1.getHeight());

        // Resize and relocate container to wrap left and right Nodes
        container.setWidth(maxX - minX);
        container.setHeight(maxY - minY);
        container.moveTo(minX, minY);
    }

    /**
     * <p>Computes the area of a rectangle containing two given {@link Rect2D}s used as the rectangle's corners.</p>
     *
     * @param rect0 one Rect2D.
     * @param rect1 other Rect2D.
     * @return area.
     */
    private float computeArea(Rect2D rect0, Rect2D rect1)
    {
        // Compute minimum and maximum x coords to get containing width
        final float minX = Math.min(rect0.getX(), rect1.getX());
        final float maxX = Math.max(rect0.getX() + rect0.getWidth(), rect1.getX() + rect1.getWidth());

        // Compute minimum and maximum y coords to get containing height
        final float minY = Math.min(rect0.getY(), rect1.getY());
        final float maxY = Math.max(rect0.getY() + rect0.getHeight(), rect1.getY() + rect1.getHeight());

        // Compute area
        return (maxX - minX) * (maxY - minY);
    }

    /**
     * <p>Finds the {@link Node} holding the {@link Rect2D} that should be the sibling of a given Rect2D to be added
     * .</p>
     *
     * @param body to be added.
     * @return sibling Node.
     */
    private Node findSibling(BodyComponent body)
    {
        // Public methods should only call this if tree's non-empty
        assert (!isEmpty());

        // Find encompassing AABB
        final Rect2D bodyRect = body.getBounds();
        Node cursor = mRoot;
        while (cursor != null) {

            // Finding leaf means reached a potential sibling
            if (isLeaf(cursor)) {
                break;
            }

            // Get left child of box Node
            final Node left = cursor.mLeft;
            final Node right = cursor.mRight;

            // Measure what AABB's area would be for each child after adding
            final float leftArea = computeArea(left.getRect(), bodyRect);
            final float rightArea = computeArea(right.getRect(), bodyRect);

            // Go towards whichever child would give the smallest area
            cursor = (leftArea <= rightArea) ? left : right;
        }

        return cursor;
    }

    /**
     * <p>Checks if a given {@link Node} has no children. Consequently, a Node without children also has a
     * {@link BodyComponent} and no {@link Rect2D}. If at least one child exists, the reverse is true, i.e. there is
     * no BodyComponent but there is a Rect2D.</p>
     *
     * @param node Node to examine.
     * @return true if Node has no children.
     */
    private boolean isLeaf(Node node)
    {
        return node.mLeft == null && node.mRight == null;
    }

    /**
     * <p>Rotates a {@link Node} and its parent to the right or left.</p>
     *
     * @param node Node to rotate.
     * @param parent Parent of node.
     * @param right true to rotate right, false for left rotation.
     */
    private void rotate(Node node, Node parent, boolean right)
    {
        // Child must be left child if rotating right - right child if rotating left
        assert ((right && node == parent.mLeft) || (!right && node == parent.mRight));

        // Rotate Nodes right such that the parent becomes node's right child
        if (right) {
            parent.mLeft = node.mRight;
            node.mRight.mParent = parent;
            node.mRight = parent;

        } else {
            // Rotate Nodes left such that the parent becomes node's left child
            parent.mRight = node.mLeft;
            node.mLeft.mParent = parent;
            node.mLeft = parent;
        }

        // No grandparent implies parent grandparent is root
        if (parent.mParent == null) {
            mRoot = node;
            node.mParent = null;

        } else if (parent.mParent.mLeft == parent) {
            // Set grandparent's left child ref to node
            parent.mParent.mLeft = node;
        } else {
            // Set grandparent's right child ref to node
            parent.mParent.mRight = node;
        }

        // Make node the new "parent" and node as old parent's parent
        node.mParent = parent.mParent;
        parent.mParent = node;

        // Update heights based off of left and right children's heights
        updateHeight(parent);
        updateHeight(node);
    }

    /**
     * <p>Updates the height of a given {@link Node}.</p>
     *
     * @param node Node to update.
     */
    private void updateHeight(Node node)
    {
        node.mHeight = Math.max(node.mLeft.mHeight, node.mRight.mHeight) + 1;
    }

    /**
     * <p>Checks whether or not a {@link BodyComponent} has been added to the tree.</p>
     *
     * @param body BodyComponent.
     * @return true if in tree.
     */
    public boolean contains(BodyComponent body)
    {
        final Node container = body.getContainer();
        return container != null && container.isOwnedBy(this);
    }

    /**
     * <p>Checks whether or not the tree contains any {@link BodyComponent}s.</p>
     *
     * @return true if the tree is empty.
     */
    public boolean isEmpty()
    {
        return mSize == 0;
    }

    /**
     * <p>Gets the number of {@link BodyComponent}s.</p>
     *
     * @return BodyComponent count.
     */
    public int size()
    {
        return mSize;
    }

    /**
     * <p>Gets the root for external tree traversal.</p>
     *
     * @return root node.
     */
    public Node getRoot()
    {
        return mRoot;
    }

    /**
     * <p>
     *     Holds either a {@link Rect2D} bounding box or a {@link BodyComponent} added through
     *     {@link #add(BodyComponent)}.
     * </p>
     */
    public final class Node
    {
        // Box to check collisions against
        private Rect2D mBox;
        private BodyComponent mBody;

        // Subtree height
        private int mHeight;

        // Node links for parent and children
        private Node mParent;
        private Node mLeft;
        private Node mRight;

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
         * <p>Gets the {@link Rect2D} bounding box.</p>
         *
         * @return bounding box.
         */
        public Rect2D getBounds()
        {
            return mBox;
        }

        /**
         * <p>Gets the {@link BodyComponent}.</p>
         *
         * @return body.
         */
        public BodyComponent getBody()
        {
            return mBody;
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

        /**
         * <p>Gets the {@link Rect2D} held by the Node. This is either the bounding box of a {@link BodyComponent}
         * or a bounding box created to contain the Node's children.</p>
         *
         * @return bounding box.
         */
        private Rect2D getRect()
        {
            return (mBox == null) ? mBody.getBounds() : mBox;
        }

        /**
         * <P>Checks whether or not a {@link BoundingTree} is the same tree as the Node's parent BoundingTree.</P>
         *
         * @param tree BoundingTree in question.
         * @return true if tree is the parent tree.
         */
        private boolean isOwnedBy(BoundingTree tree)
        {
            return BoundingTree.this == tree;
        }
    }
}
