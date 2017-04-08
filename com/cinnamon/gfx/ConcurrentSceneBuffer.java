package com.cinnamon.gfx;

import com.cinnamon.system.Game;

/**
 * <p>
 *     ConcurrentSceneBuffer allows {@link Drawable} data to be safely passed from the game's update {@link Thread} to
 *     the drawing Thread. Below is a simple use example between {@link Game} and {@link Canvas}. Only one Thread
 *     must write to a {@link Scene} (update thread) and one Thread must read (drawing thread).
 * </p>
 *
 * <br>
 *
 * <b>Game</b>
 *
 * <pre>
 *     {@code
 *
 *     private void onFrameEnd()
 *     {
 *         // Get access to Canvas' buffer
 *         final Canvas.SceneBuffer buffer = getCanvas().getSceneBuffer();
 *
 *         // Write drawing draw data for Canvas to poll
 *         final Scene scene = buffer.getWriteScene();
 *         scene.add(getImagegFactory(), getView());
 *
 *         // Update Scene available for drawing thread
 *         buffer.flush();
 *     }
 *
 *     }
 * </pre>
 *
 * <br>
 *
 * <b>Canvas</b>
 *
 * <pre>
 *     {@code
 *
 *     public void draw(SceneBuffer buffer)
 *     {
 *         // Pull available Scene
 *         final Scene<Batch<Drawable>> scene = buffer.getReadScene();
 *
 *         // Draw each Batch in Scene
 *         while (!scene.isEmpty()) {
 *
 *              final Batch<Drawable> batch = scene.poll();
 *
 *              // Draw operations
 *
 *         }
 *     }
 *
 *     }
 * </pre>
 */
public final class ConcurrentSceneBuffer extends Canvas.SceneBuffer<Batch<Drawable>>
{
    // Number of Drawables that can be drawn in any given Scene
    private static final int LOAD = 300;

    // Lock for draw data reads
    private final Object mUpdateLock = new Object();

    // Flag for checking if producer Thread has a Scene ready
    private volatile boolean mNewSceneAvailable = false;

    // Scene to add drawable data to
    private Scene<Batch<Drawable>> mWriteScene = new BatchedScene(LOAD);

    // Buffer scene prevents blocking while writing and reading
    private Scene<Batch<Drawable>> mBufferScene = new BatchedScene(LOAD);

    // Scene to draw
    private Scene<Batch<Drawable>> mReadScene = new BatchedScene(LOAD);

    @Override
    public Scene<Batch<Drawable>> getWriteScene()
    {
        // Make all batches available for use
        mWriteScene.clear();

        return mWriteScene;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned {@link Scene} may be the previous reset to be drawn again. This occurs if no new Scene has
     * yet been made available with {@link #flush()}.</p>
     *
     * @return next Scene to draw.
     */
    @Override
    public Scene<Batch<Drawable>> getReadScene()
    {
        // When no new Scene available, return previous reset
        if (!mNewSceneAvailable) {
            mReadScene.reset();
            return mReadScene;
        }

        synchronized (mUpdateLock) {

            // Rotate Scenes for reuse
            final Scene<Batch<Drawable>> readScene = mReadScene;
            mReadScene = mBufferScene;
            mBufferScene = readScene;

            // Flag scene as consumed
            mNewSceneAvailable = false;

            return mReadScene;
        }
    }

    @Override
    public void flush()
    {
        synchronized (mUpdateLock) {
            final Scene<Batch<Drawable>> oldScene = mWriteScene;
            mWriteScene = mBufferScene;
            mBufferScene = oldScene;

            // Flag new scene for reading
            mNewSceneAvailable = true;
        }
    }
}
