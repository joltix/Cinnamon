package com.cinnamon.gfx;

import com.cinnamon.system.View;
import com.cinnamon.utils.PooledQueue;

/**
 * <p>
 *     {@link Scene} implementation with an object pool to curb rising memory
 *     usage from the drawing data's node backing.
 * </p>
 */
public class PooledScene implements Scene<Batch<Drawable>>
{
    // Pool of reusable Drawables
    private PooledQueue<ImageLite> mDrawablePool = new PooledQueue<ImageLite>();

    // Pool of reusable Batches
    private PooledQueue<PooledBatch> mBatchPool = new PooledQueue<PooledBatch>();

    // Current Batches active as PooledScene's contents
    private PooledQueue<PooledBatch> mBatches = new PooledQueue<PooledBatch>();

    // Total num of Drawables across all Groups
    private int mDrawableCount = 0;

    @Override
    public void add(ImageFactory factory, View view)
    {
        // Process first game object to get an initial texture val
        final ImageComponent first = factory.getAtDistance(0);
        int lastTex = first.getTexture();
        PooledBatch batch = getBatch();
        batch.add(copyImage(first, -view.getX(), -view.getY()));
        mDrawableCount++;

        // Add the rest of the objects
        for (int i = 1, sz = factory.size(); i < sz; i++) {

            // Treat null entry as the arr's end
            final ImageComponent comp = factory.getAtDistance(i);
            if (comp == null) {
                break;
            }

            // Skip components that wouldn't be seen
            if (!isViewable(view, comp)) {
                continue;
            }

            // Get a new PooledBatch for a new texture sequence
            if (lastTex != comp.getTexture()) {
                lastTex = comp.getTexture();
                batch = getBatch();
            }

            // Add Drawable to PooledBatch
            final ImageLite img = copyImage(comp, -view.getX(), -view.getY());
            batch.add(img);
            mDrawableCount++;
        }
    }

    /**
     * <p>Creates a copy of a given {@link ImageComponent} to be sent for
     * drawing by the {@link Canvas}.</p>
     *
     * @param comp ImageComponent to copy.
     * @param shiftX shift in x direction.
     * @param shiftY shift in y direction.
     * @return copy of drawing data.
     */
    private ImageLite copyImage(ImageComponent comp, float shiftX, float shiftY)
    {
        // Make Drawable copy
        final ImageLite img;
        if (mDrawablePool.isEmpty()) {

            img = new ImageLite();
            img.copy(comp, shiftX, shiftY);
        } else {

            // Reuse old Drawable
            img = mDrawablePool.poll();
            img.copy(comp, shiftX, shiftY);
        }

        return img;
    }

    /**
     * <p>Checks whether or not an {@link ImageComponent} is viewable within
     * the area of the given {@link View}.</p>
     *
     * @param view View.
     * @param component drawing data.
     * @return true if the drawing data would be visible on screen.
     */
    private boolean isViewable(View view, ImageComponent component)
    {
        // Component must be View contained, visible toggle true, and alpha > 0
        return (view.intersects(component) && component.isVisible()
                && component.getTransparency() > 0f);
    }

    /**
     * <p>Gets the next {@link Batch} of {@link Drawable}s.</p>
     *
     * <p>The returned Batch and its contents are reused in future
     * {@link Scene}s so neither Batch nor its contents should be referenced
     * .</p>
     *
     * @return Batch.
     */
    @Override
    public Batch<Drawable> poll()
    {
        // Move PooledBatch to obj pool for reuse
        final PooledBatch batch = mBatches.poll();
        mBatchPool.add(batch);

        return batch;
    }

    /**
     * <p>Gets a PooledBatch from an object pool or instantiates a new one
     * if none can be reused.</p>
     *
     * @return PooledBatch.
     */
    private PooledBatch getBatch()
    {
        // Create a new group if none can be reused
        final PooledBatch batch;
        batch = (mBatchPool.isEmpty()) ? new PooledBatch() : mBatchPool.poll();

        // Add group to PooledScene
        mBatches.add(batch);
        return batch;
    }

    @Override
    public void clear()
    {
        // Move all Groups' ShallowDrawables into obj pool
        while (!mBatches.isEmpty()) {
            final PooledBatch batch = mBatches.poll();
            mBatchPool.add(batch);

            // Empty the PooledBatch
            while (!batch.isEmpty()) {
                batch.pollImage();
            }
        }
    }

    @Override
    public int size()
    {
        return mDrawableCount;
    }

    @Override
    public boolean isEmpty()
    {
        return mBatches.isEmpty();
    }

    /**
     * <p>
     *     {@link Batch} with an object pool to limit node instantiation.
     * </p>
     */
    public class PooledBatch implements Batch<Drawable>
    {
        // Drawable pool
        private PooledQueue<ImageLite> drawables = new
                PooledQueue<ImageLite>();

        // Texture of all Drawables in Batch
        private int texture;

        /**
         * <p>Adds an {@link ImageComponent}'s drawing data.</p>
         *
         * @param image drawable data.
         */
        private void add(ImageLite image)
        {
            // Take on texture of first drawable
            if (drawables.isEmpty()) {
                texture = image.getTexture();
            }

            assert (texture == image.getTexture());

            // Assoc with PooledBatch
            drawables.add(image);
        }

        @Override
        public Drawable poll()
        {
            return pollImage();
        }

        /**
         * <p>Removes the next available {@link ImageLite} and moves it to the
         * {@link PooledScene}'s object pool.</p>
         *
         * @return drawing data.
         */
        private ImageLite pollImage()
        {
            final ImageLite shallow = drawables.poll();

            // Move Drawable back to pool for later reuse
            PooledScene.this.mDrawablePool.add(shallow);

            // Count Drawable usage towards total
            PooledScene.this.mDrawableCount--;

            return shallow;
        }

        @Override
        public int getTexture()
        {
            return texture;
        }

        @Override
        public int size()
        {
            return drawables.size();
        }

        @Override
        public boolean isEmpty()
        {
            return drawables.size() == 0;
        }
    }

    /**
     * <p>
     *     Represents the bare-bones data needed to draw a game object.
     *     ImageLites are meant to copy {@link ImageComponent}s and be passed
     *     around in {@link PooledScene} as part of a large object pool.
     * </p>
     *
     */
    private class ImageLite implements Drawable
    {
        // Texture and draw dimensions
        private int mTexId;
        private float mWidth;
        private float mHeight;

        // Color tint
        private float mRed;
        private float mGreen;
        private float mBlue;
        private float mAlpha;

        // Position
        private float mX;
        private float mY;

        /**
         * <p>Copies the drawing data from an {@link ImageComponent} with an
         * optional position shift.</p>
         *
         * @param component ImageComponent to copy.
         * @param shiftX shift in x direction.
         * @param shiftY shift in y direction.
         */
        private void copy(ImageComponent component, float shiftX, float shiftY)
        {
            // Copy texture id, width, and height
            mTexId = component.getTexture();
            mWidth = component.getWidth();
            mHeight = component.getHeight();

            // Copy color tinting
            mRed = component.getRed();
            mGreen = component.getGreen();
            mBlue = component.getBlue();
            mAlpha = component.getTransparency();

            // Copy position
            mX = component.getX() + shiftX;
            mY = component.getY() + shiftY;
        }

        @Override
        public float getX()
        {
            return mX;
        }

        @Override
        public float getY()
        {
            return mY;
        }

        @Override
        public float getWidth()
        {
            return mWidth;
        }

        @Override
        public float getHeight()
        {
            return mHeight;
        }

        @Override
        public int getTexture()
        {
            return mTexId;
        }

        @Override
        public float getTransparency()
        {
            return mAlpha;
        }

        @Override
        public float getRed()
        {
            return mRed;
        }

        @Override
        public float getGreen()
        {
            return mGreen;
        }

        @Override
        public float getBlue()
        {
            return mBlue;
        }

        /**
         * <p>This method always returns 0.</p>
         *
         * @return 0.
         */
        @Override
        public float getOffsetX()
        {
            return 0;
        }

        /**
         * <p>This method always returns 0.</p>
         *
         * @return 0.
         */
        @Override
        public float getOffsetY()
        {
            return 0;
        }
    }
}
