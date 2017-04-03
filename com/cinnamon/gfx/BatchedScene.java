package com.cinnamon.gfx;

import com.cinnamon.system.View;
import com.cinnamon.utils.Point3F;
import com.cinnamon.utils.PooledQueue;

/**
 * <p>
 *     {@link Scene} implementation providing drawable data in {@link Batch}es for batch drawing.
 * </p>
 */
public final class BatchedScene implements Scene<Batch<Drawable>>
{
    // Drawables for the Canvas to draw
    private final ImageLite[] mDrawables;

    // Pool of reusable Batches
    private final PooledQueue<ImageBatch> mBatchPool = new PooledQueue<ImageBatch>();

    // Current Batches active as BatchedScene's contents
    private final PooledQueue<ImageBatch> mBatchesInUse = new PooledQueue<ImageBatch>();

    // Total num of Drawables across all Groups
    private int mDrawableCount = 0;

    /**
     * <p>Constructs a BatchedScene with a specific {@link Drawable} capacity.</p>
     *
     * @param capacity number of Drawables that can be stored.
     */
    public BatchedScene(int capacity)
    {
        mDrawables = new ImageLite[capacity];
    }

    /**
     * {@inheritDoc}
     *
     * <p>All {@link ImageComponent}s eligible for drawing will be copied from the {@link ImageFactory}. The given
     * {@link View} will be used to filter {@link Drawable}s that cannot be seen and apply transformations to those
     * being drawn as per the View's configuration.</p>
     *
     * @param factory factory storing drawing data.
     * @param view View for filtering and transformations.
     */
    @Override
    public void add(ImageFactory factory, View view)
    {
        // Bail out if nothing to draw
        if (factory.isEmpty()) {
            return;
        }

        // Get initial texture
        final int firstIndex = factory.getVisibleCount() - 1;
        final ImageComponent first = factory.getAtDistance(firstIndex);
        int lastTexture = first.getTexture();

        // Get initial batch
        ImageBatch batch = getBatch();
        batch.mBegin = 0;
        batch.mTexture = lastTexture;

        // Copy image to scene's arr and allow View to apply its transforms
        final Drawable firstImg = copyImageTo(0, first);
        view.transform(firstImg);
        mDrawableCount++;

        // Add the rest of the objects
        for (int from = firstIndex - 1, to = 1; from >= 0; from--) {

            // Treat null entry as the arr's end
            final ImageComponent comp = factory.getAtDistance(from);
            if (comp == null) {
                break;
            }

            // Skip components that wouldn't be seen
            if (!isViewable(view, comp)) {
                continue;
            }

            // Get new ImageBatch for a new chain of a texture
            final int currentTexture = comp.getTexture();
            if (lastTexture != currentTexture) {

                // Save ending index for old batch and reset cursor for polling
                batch.mEnd = to;
                batch.mCursor = batch.mBegin;

                // Get a new batch for the next Texture sequence
                lastTexture = currentTexture;
                batch = getBatch();
                batch.mBegin = to;
                batch.mTexture = currentTexture;
            }

            // Add Drawable to Scene's arr
            final Drawable drawable = copyImageTo(to++, comp);

            // Apply View's transforms and update Scene size
            view.transform(drawable);
            mDrawableCount++;
        }

        // Ending index of last batch will be scene's total size;
        batch.mEnd = mDrawableCount;
        batch.mCursor = batch.mBegin;
    }

    /**
     * <p>Copies a given {@link ImageComponent} to a specific index in the Scene.</p>
     *
     * @param index index in drawing order to copy to.
     * @param component ImageComponent to copy.
     * @return copy of ImageComponent.
     */
    private Drawable copyImageTo(int index, ImageComponent component)
    {
        ImageLite img = mDrawables[index];

        // Instantiate new ImageLite if none before
        if (img == null) {
            img = new ImageLite();
            mDrawables[index] = img;
        }

        // Copy Component data in
        img.copy(component);

        return img;
    }

    /**
     * <p>Checks whether or not an {@link ImageComponent} is viewable within the area of the given {@link View}.</p>
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
     * <p>The returned Batch and its contents are reused in future {@link Scene}s so neither Batch nor its contents
     * should be referenced outside the Scene.</p>
     *
     * @return Batch.
     */
    @Override
    public Batch<Drawable> poll()
    {
        // Move ImageBatch to obj pool for reuse
        final ImageBatch batch = mBatchesInUse.poll();
        mBatchPool.add(batch);

        return batch;
    }

    /**
     * <p>Gets an {@link ImageBatch} from an object pool or instantiates a new one if none can be reused.</p>
     *
     * @return ImageBatch.
     */
    private ImageBatch getBatch()
    {
        // Create a new group if none can be reused
        final ImageBatch batch = (mBatchPool.isEmpty()) ? new ImageBatch() : mBatchPool.poll();

        // Add group to BatchedScene
        mBatchesInUse.add(batch);
        return batch;
    }

    @Override
    public void clear()
    {
        // Move all in use Batches to the object pool
        for (int i = 0, sz = mBatchesInUse.size(); i < sz; i++) {
            // Move batch to object pool for reuse
            final ImageBatch batch = mBatchesInUse.poll();
            mBatchPool.add(batch);

            // Erase indices
            batch.clear();
        }

        mDrawableCount = 0;
    }

    @Override
    public void reset()
    {
        // Move back to use all recently used Batches
        for (int i = 0, sz = mBatchPool.size(); i < sz; i++) {
            final ImageBatch batch = mBatchPool.poll();

            // Empty batch means was never filled when Scene was filled
            if (batch.isEmpty()) {
                continue;
            }

            // Move polling cursor back to start and make batch available
            batch.resetPolling();
            mBatchesInUse.add(batch);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>This count represents the Scene's size when it was filled and does not diminish with calls to
     * {@link #poll()}.</p>
     *
     * @return instance count.
     */
    @Override
    public int size()
    {
        return mDrawableCount;
    }

    /**
     * <p>Checks whether or not the BatchedScene has any {@link Batch}es left.</p>
     *
     * @return true if at least one Batch remains to be polled.
     */
    @Override
    public boolean isEmpty()
    {
        return mBatchesInUse.isEmpty();
    }

    /**
     * <p>
     *     {@link Batch} with an object pool to limit node instantiation.
     * </p>
     */
    private final class ImageBatch implements Batch<Drawable>
    {
        // Beginning index (inclusive)
        private int mBegin;

        // Ending index (exclusive)
        private int mEnd;

        // Polling index
        private int mCursor;

        // Texture of all Drawables in Batch
        private int mTexture;

        @Override
        public Drawable poll()
        {
            // Reached beyond range so act as if empty
            if (mCursor >= mEnd) {
                return null;
            }

            return mDrawables[mCursor++];
        }

        @Override
        public int getTexture()
        {
            return mTexture;
        }

        /**
         * <p>Removes the beginning and ending indices as well as the set {@link Texture} id. After this method is
         * called, the ImageBatch can no longer be reset.</p>
         */
        private void clear()
        {
            mBegin = 0;
            mEnd = 0;
            mCursor = 0;
            mTexture = -1;
        }

        /**
         * <p>Moves the cursor back to the beginning. This makes the ImageBatch appear "full" again, as if
         * {@link #poll()} was never called. This method will have no effect if {@link #clear()} has already been
         * called.</p>
         */
        private void resetPolling()
        {
            mCursor = mBegin;
        }

        @Override
        public int size()
        {
            return mEnd - mCursor;
        }

        @Override
        public boolean isEmpty()
        {
            return size() == 0;
        }
    }

    /**
     * <p>
     *     Represents the bare-bones data needed to draw a game object. ImageLites are meant to copy
     *     {@link ImageComponent}s and be passed around in {@link BatchedScene} as part of a large object pool.
     * </p>
     */
    private class ImageLite implements Drawable
    {
        // Texture and draw dimensions
        private int mTexId;
        private boolean mFlipV;
        private boolean mFlipH;

        // Dimensions and rotation angle
        private float mWidth;
        private float mHeight;
        private double mAngle;

        // Color tint
        private float mRed;
        private float mGreen;
        private float mBlue;
        private float mAlpha;

        // Position
        private float mX;
        private float mY;

        /**
         * <p>Copies the drawing data from an {@link ImageComponent} with an optional position shift.</p>
         *
         * @param component ImageComponent to copy.
         */
        private void copy(ImageComponent component)
        {
            // Copy texture id, coordinates, and flip directions
            mTexId = component.getTexture();
            mFlipH = component.isFlippedHorizontally();
            mFlipV = component.isFlippedVertically();

            // Copy width, and height
            mWidth = component.getWidth();
            mHeight = component.getHeight();
            mAngle = component.getRotation();

            // Copy color tinting
            mRed = component.getRed();
            mGreen = component.getGreen();
            mBlue = component.getBlue();
            mAlpha = component.getTransparency();

            // Copy position
            mX = component.getX();
            mY = component.getY();
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
        public void setWidth(float width)
        {
            mWidth = width;
        }

        @Override
        public Point3F getPosition()
        {
            return new Point3F(mX, mY, 0f);
        }

        @Override
        public void setHeight(float height)
        {
            mHeight = height;
        }

        @Override
        public float getCenterX()
        {
            return mX + (mWidth / 2f);
        }

        @Override
        public float getCenterY()
        {
            return mY + (mHeight / 2f);
        }

        @Override
        public float getZ()
        {
            return 0f;
        }

        @Override
        public void moveTo(float x, float y)
        {
            mX = x;
            mY = y;
        }

        @Override
        public void moveTo(float x, float y, float z)
        {
            moveTo(x, y);
        }

        @Override
        public void moveBy(float x, float y)
        {
            mX += x;
            mY += y;
        }

        @Override
        public void moveBy(float x, float y, float z)
        {
            moveBy(x, y);
        }

        @Override
        public void moveToCenter(float x, float y)
        {
            moveTo(x - (getWidth() / 2f), y - (getHeight() / 2f));
        }

        @Override
        public double getRotation()
        {
            return mAngle;
        }

        @Override
        public int getTexture()
        {
            return mTexId;
        }

        @Override
        public boolean isFlippedHorizontally()
        {
            return mFlipH;
        }

        @Override
        public boolean isFlippedVertically()
        {
            return mFlipV;
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
    }
}
