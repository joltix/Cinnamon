package com.cinnamon.gfx;

import com.cinnamon.object.GObject;
import com.cinnamon.object.Room;
import com.cinnamon.system.ComponentFactory;
import com.cinnamon.system.Config;
import com.cinnamon.system.Game;
import com.cinnamon.utils.Sort;

/**
 * <p>
 *     ImageFactory is responsible for not only producing
 *     {@link ImageComponent}s to provide a visual representation for
 *     {@link GObject}s, but also supplying the {@link Canvas} with drawing
 *     information.
 * </p>
 */
public abstract class ImageFactory extends ComponentFactory<ImageComponent,
        ShaderFactory>
{
    /**
     * <p>Configuration name for {@link Room}s.</p>
     */
    public static final String CONFIG_ROOM = "room";

    // Listener for ImageComponent visibility changes
    private final OnDrawVisibilityChangeListener mVisibilityListener;

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

        // Instantiate listener for draw order changes
        mVisibilityListener = new VisibilitySentry();
        mFrameEndListener = new OnFrameEndListener();
    }

    @Override
    protected void onRequisition(ImageComponent component)
    {
        // Expand drawing arr if constrained
        if (mVisibleCount >= mDrawOrder.length) {
            final int newCap = (int) (mDrawOrder.length * (1f + mDrawGrowth));
            increaseDrawCapacity(newCap);
        }

        // Attach listener for changes to drawing order
        component.setOnVisibilityChangeListener(mVisibilityListener);

        // Add new component to be sorted for drawing
        mDrawOrder[mVisibleCount++] = component;
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
     * <p>Gets an {@link ImageComponent} of a certain distance away from the
     * front-most ImageComponent on the screen. The given index is the n-th
     * ImageComponent from the front.</p>
     *
     * @param index n-th from the front.
     * @return ImageComponent.
     */
    public final ImageComponent getAtDistance(int index)
    {
        return mDrawOrder[index];
    }

    /**
     * <p>Notifies the ImageFactory that an {@link ImageComponent} has
     * changed such that the drawing order is affected.</p>
     */
    public final void notifyDrawOrderChanged()
    {
        mDrawChanged = true;
    }

    /**
     * <p>Checks whether or not the ImageFactory's drawing order needs to be
     * resorted.</p>
     *
     * @return true if drawing data has changed.
     */
    private boolean hasDrawOrderChanged()
    {
        return mDrawChanged;
    }

    /**
     * <p>
     *     {@link Config} for assembling an {@link ImageComponent}.
     * </p>
     */
    public interface ImageConfig extends Config<ImageComponent, ShaderFactory>
    {

    }

    /**
     * <p>Gets an {@link OnFrameEndListener} to be be called whenever drawing
     * data is about to be sent to the {@link Canvas}.</p>
     *
     * @return OnFrameEndListener.
     */
    public final OnFrameEndListener newOnFrameEndListener()
    {
        return mFrameEndListener;
    }

    /**
     * <p>
     *     Listener to be called by {@link Game} at the end of a frame but
     *     before the drawing data is requested to be sent for rendering.
     *     This is where the drawing order may be resorted, should
     *     {@link #notifyDrawOrderChanged()} have been called at any time
     *     during the frame.
     * </p>
     */
    public class OnFrameEndListener
    {
        public void onFrameEnd()
        {
            final ImageFactory factory = ImageFactory.this;
            if (factory.hasDrawOrderChanged()) {
                factory.mDrawSorter.sort(factory.mDrawOrder);
                factory.mDrawChanged = false;
            }
        }
    }

    /**
     * <p>
     *     Private listener instance for
     *     {@link OnDrawVisibilityChangeListener}. This class is to be set on
     *     each {@link ImageComponent} produced by the {@link ImageFactory} in
     *     order to notify the factory whenever the draw order needs sorting.
     * </p>
     */
    private class VisibilitySentry implements OnDrawVisibilityChangeListener
    {
        @Override
        public void onChange()
        {
            // Notify ImageFactory drawing order needs resort
            ImageFactory.this.notifyDrawOrderChanged();
        }
    }
}