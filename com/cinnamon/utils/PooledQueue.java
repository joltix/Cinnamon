package com.cinnamon.utils;

import java.util.LinkedList;

/**
 * <p>A node-based queue with an internal object pool to reuse nodes whose
 * data have been removed. This class prevents a case where implementations
 * such as {@link LinkedList}, which leave a dangling internal node when its
 * data is removed, can cause a rise in memory use if add and remove
 * operations are executed continuously at a fast enough pace.</p>
 *
 * @param <E> object to store.
 */
public class PooledQueue<E>
{
    // Queue's head and tail Nodes
    private Node mHead = null;
    private Node mTail = null;

    // Top of stack for reusing Nodes
    private Node mPoolHead = null;

    // Queue size
    private int mSize = 0;

    /**
     * <p>Removes data from the front of the PooledQueue.</p>
     *
     * @return queue head, or null if the PooledQueue is empty.
     */
    public E poll()
    {
        // Empty queue can't return anything
        if (isEmpty()) {
            return null;
        }

        // Erase head and tail if just emptied queue
        final Node oldNode = mHead;
        if (mHead == mTail) {
            mHead = null;
            mTail = null;

        } else {
            // Select next head Node
            mHead = mHead.mNext;
        }

        // Move removed Node to pool for reuse and update queue size
        oldNode.mNext = mPoolHead;
        mPoolHead = oldNode;
        mSize--;

        // Remove Node's data and return
        final E data = oldNode.mReference;
        oldNode.mReference = null;
        return data;
    }

    /**
     * <p>Adds some data to the PooledQueue. This method does nothing if
     * <i>null</i> is passed.</p>
     *
     * @param obj data.
     * @return true if the data was successfully added.
     */
    public boolean add(E obj)
    {
        // Fail to add if given null
        if (obj == null) {
            return false;
        }

        // Wrap value in Node
        final Node node = nodify(obj);

        // Node becomes both head and tail if only Node
        if (isEmpty()) {
            mHead = node;
            mTail = node;

        } else {
            // Append new node to end
            mTail.mNext = node;
            mTail = node;
        }

        // Update queue count
        mSize++;

        return true;
    }

    /**
     * <p>Wraps a given value in a Node and returns the Node. The Node used
     * will be taken from a stack of old used Nodes. If the stack is empty,
     * a new Node will be constructed.</p>
     *
     * @param value data to wrap.
     * @return Node.
     */
    private Node nodify(E value)
    {
        final Node node;

        // Node pool empty so create new Node
        if (mPoolHead == null) {
            node = new Node(value);

        } else {
            // Pop Node from reuse stack and update its value
            node = mPoolHead;
            mPoolHead = mPoolHead.mNext;
            node.mReference = value;
        }

        return node;
    }

    /**
     * <p>Gets the data at the head of the queue without removing it.</p>
     *
     * @return data, or null if the queue is empty.
     */
    public E peek()
    {
        return (mHead == null) ? null : mHead.mReference;
    }

    /**
     * <p>Empties the PooledQueue of any stored data.</p>
     *
     * <p>This method performs in O(n) to ensure no references to previously
     * added Objects remain.</p>
     */
    public void clear()
    {
        if (isEmpty()) {
            return;
        }

        // Prevent Nodes from holding references to old values
        Node cursor = mHead;
        while (cursor != null) {
            cursor.mReference = null;
            cursor = cursor.mNext;
        }

        // Move all active Nodes to pool for reuse
        mTail.mNext = mPoolHead;
        mPoolHead = mHead;

        // Set state to "empty"
        mHead = null;
        mTail = null;
        mSize = 0;
    }

    /**
     * <p>Checks whether or not there are any Objects stored.</p>
     *
     * @return true if Object count == 0.
     */
    public boolean isEmpty()
    {
        return mSize == 0;
    }

    /**
     * <p>Gets the number of stored Objects.</p>
     *
     * @return Object count.
     */
    public int size()
    {
        return mSize;
    }

    /**
     * <p>Intermediate shell wrapping data to be moved around the PooledQueue
     * .</p>
     */
    private class Node
    {
        // Next Node in queue and stored data
        private Node mNext = null;
        private E mReference = null;

        /**
         * <p>Constructs a demo Node wrapping data.</p>
         *
         * @param obj data.
         */
        public Node(E obj)
        {
            mReference = obj;
        }
    }
}
