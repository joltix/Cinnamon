package com.cinnamon.gfx;

import com.cinnamon.object.GObject;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.Game;
import com.cinnamon.system.OnOrphanChangedListener;
import com.cinnamon.utils.Comparison;
import com.cinnamon.utils.Sort;

/**
 * <p>
 *     ImageFactory is responsible for not only producing {@link ImageComponent}s to provide a visual representation for
 *     {@link GObject}s, but also supplying the {@link Canvas} with drawing information.
 * </p>
 */
public abstract class ImageFactory extends ComponentFactory<ImageComponent, ShaderFactory>
{
    // Listener for ImageComponent visibility changes
    private final OnDrawVisibilityChangedListener mVisibilityListener = new VisibilitySentry();

    // Listener for drawing order needing sort
    private final OnDrawOrderChangeListener mDrawOrderListener = new DrawOrderSentry();

    // Listener for orphan status - implies visibility change
    private final OnOrphanChangedListener mOrphanVisibilityListener = new OrphanChangedVisibilitySentry();

    // Listener for pending drawing
    private final OnFrameEndListener mFrameEndListener;

    // Sorting alg for sorting draw order before drawing
    private final Sort<ImageComponent> mDrawSorter;

    // Order of drawing ImageComponents
    private ImageComponent[] mDrawOrder;

    // Number of visible ImageComponents
    private int mVisibleCount;

    // Growth rate of draw order array
    private float mDrawGrowth;

    // Whether or not the draw order array needs to be sorted
    private boolean mDrawChanged = false;

    /**
     * <p>Constructor for an ImageFactory.</p>
     *
     * @param factory {@link ShaderFactory}.
     * @param sort {@link Sort} used for arranging the draw order.
     * @param load initial capacity.
     * @param growth normalized capacity growth.
     */
    protected ImageFactory(ShaderFactory factory, Sort<ImageComponent> sort,
                           int load, float growth)
    {
        super(factory, load, growth);
        mDrawSorter = sort;
        mDrawOrder = new ImageComponent[load];
        mDrawGrowth = growth;

        mFrameEndListener = new OnFrameEndListener();

        // Wrap user provided compare with outer layer pushing null and orphaned components to the arr's right
        final Comparison<ImageComponent> cmp = new OrphanFilter(mDrawSorter.getComparison());
        mDrawSorter.setComparison(cmp);
    }

    @Override
    protected final ImageComponent createIdentifiable()
    {
        return new ImageComponent();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overriding classes must call super for {@link ImageComponent}s to be properly setup.</p>
     *
     * @param object ImageComponent.
     */
    @Override
    protected void onRequisition(ImageComponent object)
    {
        // Expand drawing arr if constrained
        if (mVisibleCount >= mDrawOrder.length) {
            final int newCap = (int) (mDrawOrder.length * (1f + mDrawGrowth));
            increaseDrawCapacity(newCap);
        }

        // Attach listener for orphan status changes
        object.setOnOrphanChangedListener(mOrphanVisibilityListener);

        // Attach listener for changes to visibility such as transparency or toggles
        object.setOnVisibilityChangeListener(mVisibilityListener);

        // Attach listener for changes to the z position
        object.setOnDrawOrderChangeListener(mDrawOrderListener);

        // Add new component to be sorted for drawing
        mDrawOrder[mVisibleCount] = object;
        notifyDrawOrderChanged();
    }

    /**
     * <p>Expands the draw order array to a new capacity.</p>
     *
     * @param capacity new capacity.
     */
    private void increaseDrawCapacity(int capacity)
    {
        final ImageComponent[] larger = new ImageComponent[capacity];
        System.arraycopy(mDrawOrder, 0, larger, 0, mVisibleCount);
        mDrawOrder = larger;
    }

    /**
     * <p>Gets an {@link ImageComponent} of a certain distance away from the front-most ImageComponent on the screen.
     * The given index is the n-th ImageComponent from the front.</p>
     *
     * @param index n-th from the front.
     * @return ImageComponent.
     */
    public final ImageComponent getAtDistance(int index)
    {
        return mDrawOrder[index];
    }

    /**
     * <p>Notifies the ImageFactory that an {@link ImageComponent} has changed such that the drawing order is
     * affected.</p>
     */
    public final void notifyDrawOrderChanged()
    {
        mDrawChanged = true;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Overriding classes must call super for {@link ImageComponent}s to be properly drawn.</p>
     *
     * @param object ImageComponent.
     */
    @Override
    protected void onRemove(ImageComponent object)
    {
        notifyDrawOrderChanged();
    }

    /**
     * <p>Gets the number of visible {@link ImageComponent}s. This is the number of ImageComponents to be drawn.</p>
     *
     * @return number of visible ImageComponents.
     */
    public final int getVisibleCount()
    {
        return mVisibleCount;
    }

    /**
     * <p>Gets an {@link OnFrameEndListener} to be be called whenever drawing data is about to be sent to the
     * {@link Canvas}.</p>
     *
     * @return OnFrameEndListener.
     */
    public final OnFrameEndListener newOnFrameEndListener()
    {
        return mFrameEndListener;
    }

    /**
     * <p>
     *     Listener to be called by {@link Game} at the end of a frame but before the drawing data is requested to be
     *     sent for rendering. This is where the drawing order may be resorted, should
     *     {@link #notifyDrawOrderChanged()} have been called at any time during the frame.
     * </p>
     */
    public final class OnFrameEndListener
    {
        public void onFrameEnd()
        {
            final ImageFactory factory = ImageFactory.this;
            if (factory.mDrawChanged) {
                factory.mDrawSorter.sort(factory.mDrawOrder);
                factory.mDrawChanged = false;
            }
        }
    }

    /**
     * <p>
     *     This class is to be set on each {@link ImageComponent} produced by the {@link ImageFactory} in order to
     *     notify the factory of transparency or toggle changes and the need to sort the drawing order.
     * </p>
     */
    private class VisibilitySentry implements OnDrawVisibilityChangedListener
    {
        @Override
        public void onChange(boolean visible)
        {
            // Update visible count based off of current visibility
            mVisibleCount = (visible) ? (mVisibleCount + 1) : (mVisibleCount - 1);

            // Notify ImageFactory drawing order needs resort
            ImageFactory.this.notifyDrawOrderChanged();
        }
    }

    /**
     * <p>
     *     This class is to be set on each {@link ImageComponent} produced by the {@link ImageFactory} in order to
     *     notify the factory of changes to the z position and the need to sort the drawing order.
     * </p>
     */
    private class DrawOrderSentry implements OnDrawOrderChangeListener
    {
        @Override
        public void onChange()
        {
            ImageFactory.this.notifyDrawOrderChanged();
        }
    }

    /**
     * <p>
     *     Orphan status listener for {@link ImageComponent}s. Disables ImageComponent's drawing and notifies the
     *     factory of a change in drawing order.
     * </p>
     */
    private class OrphanChangedVisibilitySentry implements OnOrphanChangedListener
    {
        @Override
        public void onOrphanChanged(int id, boolean isOrphan)
        {
            // Update visible count based off of current visibility
            mVisibleCount = (isOrphan) ? (mVisibleCount - 1) : (mVisibleCount + 1);

            // Notify ImageFactory drawing order needs resort
            notifyDrawOrderChanged();
        }
    }

    /**
     * <p>
     *     Wraps the {@link Comparison} used to order drawing data and adds the {@link Component}'s orphan status as
     *     a factor in the sorting.
     * </p>
     */
    private class OrphanFilter implements Comparison<ImageComponent>
    {
        // Original drawing order decider
        private final Comparison<ImageComponent> mUserCmp;

        /**
         * <p>Constructs an OrphanFilter.</p>
         *
         * @param comparison {@link Comparison} submitted to order drawing data.
         */
        OrphanFilter(Comparison<ImageComponent> comparison)
        {
            mUserCmp = comparison;
        }

        @Override
        public int compare(ImageComponent obj0, ImageComponent obj1)
        {
            // Null ImageComponents should be on the arr's right side
            final boolean null0 = obj0 == null;
            final boolean null1 = obj1 == null;
            if (null0 && !null1) {
                return 1;
            } else if (!null0 && null1) {
                return -1;
            } else if (null0) {
                return 0;
            }

            // Orphan ImageComponents should be on the arr's right side
            final boolean orphan0 = obj0.getGObjectId() == Component.NULL;
            final boolean orphan1 = obj1.getGObjectId() == Component.NULL;
            if (orphan0 && !orphan1) {
                return 1;
            } else if (!orphan0 && orphan1) {
                return -1;
            } else if (orphan0) {
                return 0;
            }

            // Defer compare to user's comparison when objs aren't null or orphans
            return mUserCmp.compare(obj0, obj1);
        }
    }
}