package com.cinnamon.system;


import com.cinnamon.utils.Comparison;
import com.cinnamon.utils.Merge;
import com.cinnamon.utils.Sort;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     Default {@link EventDispatcher} implementation used when no user implementation is provided.
 * </p>
 */
public final class DefaultEventDispatcher implements EventDispatcher
{
    // Mergesort for keeping handler call order sorted (min to max)
    private Sort<Dispatch.Node> mSort = new Merge<Dispatch.Node>(new Comparison<Dispatch.Node>()
    {
        @Override
        public int compare(Dispatch.Node obj0, Dispatch.Node obj1)
        {
            // Order by which is null
            final boolean obj0Null = obj0 == null;
            final boolean obj1Null = obj1 == null;
            if (obj0Null && !obj1Null) {
                return 1;
            } else if (obj1Null && !obj0Null) {
                return -1;
            } else if (obj0Null && obj1Null) {
                return 0;
            }

            // Both exist so order by priority
            if (obj0.mPriority < obj1.mPriority) {
                return -1;
            } else if (obj0.mPriority > obj1.mPriority) {
                return 1;
            } else {
                return 0;
            }
        }
    });

    // KeyEvent handlers
    private Dispatch<KeyEvent> mKeyHandles = new Dispatch<KeyEvent>();
    private boolean mKeyChanged = false;

    // MouseEvent handlers
    private Dispatch<MouseEvent> mMouseHandles = new Dispatch<MouseEvent>();
    private boolean mMouseChanged = false;

    // CreateEvent handlers
    private Dispatch<CreateEvent> mCreateHandles = new Dispatch<CreateEvent>();
    private boolean mCreateChanged = false;

    // DestroyEvent handlers
    private Dispatch<DestroyEvent> mDestroyHandles = new Dispatch<DestroyEvent>();
    private boolean mDestroyChanged = false;

    @Override
    public final void addHandler(KeyEventHandler handler, EventFilter<KeyEvent> filter, int priority)
    {
        mKeyHandles.add(handler, filter, priority);
        mKeyChanged = true;
    }

    @Override
    public final void addHandler(MouseEventHandler handler, EventFilter<MouseEvent> filter, int priority)
    {
        mMouseHandles.add(handler, filter, priority);
        mMouseChanged = true;
    }

    @Override
    public final void addHandler(CreateEventHandler handler, EventFilter<CreateEvent> filter, int priority)
    {
        mCreateHandles.add(handler, filter, priority);
        mCreateChanged = true;
    }

    @Override
    public final void addHandler(DestroyEventHandler handler, EventFilter<DestroyEvent> filter, int priority)
    {
        mDestroyHandles.add(handler, filter, priority);
        mDestroyChanged = true;
    }

    @Override
    public final void removeHandler(KeyEventHandler handler)
    {
        mKeyHandles.remove(handler);
        mKeyChanged = true;
    }

    @Override
    public final void removeHandler(MouseEventHandler handler)
    {
        mMouseHandles.remove(handler);
        mMouseChanged = true;
    }

    @Override
    public final void removeHandler(CreateEventHandler handler)
    {
        mCreateHandles.remove(handler);
        mCreateChanged = true;
    }

    @Override
    public final void removeHandler(DestroyEventHandler handler)
    {
        mDestroyHandles.remove(handler);
        mDestroyChanged = true;
    }

    @Override
    public void process(KeyEvent event)
    {
        // Update handler call order if added or removed
        if (mKeyChanged) {
            mSort.sort(mKeyHandles.mHandles);
            mKeyChanged = false;
        }
        DefaultEventDispatcher.triggerHandlers(mKeyHandles, event);
    }

    @Override
    public void process(MouseEvent event)
    {
        // Update handler call order if added or removed
        if (mMouseChanged) {
            mSort.sort(mMouseHandles.mHandles);
            mMouseChanged = false;
        }
        DefaultEventDispatcher.triggerHandlers(mMouseHandles, event);
    }

    @Override
    public void process(CreateEvent event)
    {
        // Update handler call order if added or removed
        if (mCreateChanged) {
            mSort.sort(mCreateHandles.mHandles);
            mCreateChanged = false;
        }
        DefaultEventDispatcher.triggerHandlers(mCreateHandles, event);
    }

    @Override
    public void process(DestroyEvent event)
    {
        // Update handler call order if added or removed
        if (mDestroyChanged) {
            mSort.sort(mDestroyHandles.mHandles);
            mDestroyChanged = false;
        }
        DefaultEventDispatcher.triggerHandlers(mDestroyHandles, event);
    }

    /**
     * <p>Notifies every {@link EventHandler} in a given {@link Dispatch} of the specified {@link Event}.</p>
     *
     * @param dispatch Dispatch of handlers.
     * @param event target Event.
     * @param <E> target Event type.
     */
    protected static <E extends Event> void triggerHandlers(Dispatch<E> dispatch, E event)
    {
        final Dispatch.Node[] handles = dispatch.mHandles;
        final int size = dispatch.mHandleCount;

        // Process each EventHandler if EventFilter allows (from max to min priority)
        for (int i = size - 1; i >= 0; i--) {
            final Dispatch.Node node = handles[i];
            if (node.mFilter.filter(event)) {
                node.mHandler.handle(event);
            }
        }
    }

    @Override
    public void clearHandlers()
    {
        mKeyHandles.clear();
        mMouseHandles.clear();
        mCreateHandles.clear();
        mDestroyHandles.clear();
    }

    /**
     * <p>
     *     Container for all {@link EventHandler}s and {@link EventFilter}s targeting a specific {@link Event} type.
     * </p>
     *
     * @param <E> target Event.
     */
    private static class Dispatch<E extends Event>
    {
        private static final int DISPATCH_LOAD = 10;
        private static final float GROWTH = 1.25f;

        // Null filter is assigned to handlers added w/o a filter (always allow Event)
        private static final EventFilter<Event> NULL_FILTER = new EventFilter<Event>() {
            @Override
            public boolean filter(Event event) {
                return true;
            }
        };

        // EventHandler storage
        private Node[] mHandles = new Node[DISPATCH_LOAD];

        // EventHandler inst count
        private int mHandleCount = 0;

        /**
         * <p>Adds an {@link EventHandler} and {@link EventFilter} to be called whenever the Dispatch's {@link Event}
         * type is processed.</p>
         *
         * @param handler EventHandler to process {@link Event}.
         * @param filter EventFilter for pruning Events.
         * @param priority Execution priority.
         */
        public void add(EventHandler<E> handler, EventFilter<E> filter, int priority)
        {
            // Grow array if out of space
            if ((mHandleCount + 1) >= mHandles.length) {
                expandCapacity((int) (GROWTH * mHandles.length));
            }

            // Wrap EventHandler, EventFilter, and their priority in a node for sorting
            final Node node = new Node();
            node.mHandler = handler;
            node.mFilter = (filter == null) ? NULL_FILTER : filter;
            node.mPriority = priority;

            // Store
            mHandles[mHandleCount++] = node;
        }

        /**
         * <p>Expands the Node[] if out of space.</p>
         *
         * @param newCapacity new desired size.
         */
        private void expandCapacity(int newCapacity)
        {
            assert (newCapacity > mHandles.length);

            final Node[] moreNodes = new Node[newCapacity];

            // Copy Handles over and replace reference
            System.arraycopy(mHandles, 0, moreNodes, 0, mHandleCount);
            mHandles = moreNodes;
        }

        /**
         * <p>Removes an {@link EventHandler} from being triggered. This method performs a linear search through all
         * stored EventHandlers.</p>
         *
         * @param handler EventHandler.
         */
        public void remove(EventHandler<E> handler)
        {
            // Linearly search through arr for handler
            for (int i = 0; i < mHandleCount; i++) {
                // Skip other instances
                if (mHandles[i].mHandler != handler) {
                    continue;
                }

                // Remove ref from arr
                final Node handle = mHandles[i];
                mHandles[i] = null;

                // Remove EventHandler and EventFilter refs from node
                handle.mHandler = null;
                handle.mFilter = null;

                // Update inst count
                mHandleCount--;

                // Shift all objects left by 1 to keep arr packed
                System.arraycopy(mHandles, i + 1, mHandles, i, mHandleCount - i + 1);
                break;
            }
        }

        /**
         * <p>Removes all {@link EventHandler}s and their associated {@link EventFilter}s.</p>
         */
        public void clear()
        {
            for (int i = 0; i < mHandleCount; i++) {
                // Remove ref from arr
                final Node node = mHandles[i];
                mHandles[i] = null;

                // Remove EventHandler and EventFilter ref from Node
                node.mHandler = null;
                node.mFilter = null;
            }

            mHandleCount = 0;
        }

        /**
         * <p>
         *     Bundles an {@link EventHandler} with its {@link EventFilter} and priority.
         * </p>
         *
         * @param <E> {@link Event} type to handle.
         */
        private class Node<E extends Event>
        {
            // Operation for event
            private EventHandler<E> mHandler;

            // Decides whether or not to process event
            private EventFilter<E> mFilter;

            // Notification urgency
            private int mPriority;
        }
    }
}
