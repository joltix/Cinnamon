package com.cinnamon.gfx;

import com.cinnamon.system.Game;

/**
 * <p>
 *     ConcurrentSceneBuffer allows {@link Drawable} data to be safely
 *     passed from the game update {@link Thread} to the drawing Thread. Below
 *     is a simple use example from within {@link Game} and {@link Canvas}.
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
 *         final Scene scene = buffer.getReadFrame();
 *
 *         // Draw each Batch in Scene
 *         while (!scene.isEmpty()) {
 *
 *              final Batch batch = scene.poll();
 *
 *              // Draw operations
 *
 *         }
 *     }
 *
 *     }
 * </pre>
 *
 *
 *
 */
public class ConcurrentSceneBuffer extends Canvas.SceneBuffer<Batch<Drawable>>
{
    // Lock for draw data reads
    private Object mUpdateLock = new Object();

    // Scene to add drawable data to
    private Scene mWriteScene = new PooledScene();

    // Buffer scene prevents blocking while writing and reading
    private Scene mBufferScene = new PooledScene();

    // Scene to draw
    private Scene mReadScene = new PooledScene();

    @Override
    public Scene<Batch<Drawable>> getWriteScene()
    {
        // Overwrite any existing Drawables
        mWriteScene.clear();

        return mWriteScene;
    }

    @Override
    public Scene<Batch<Drawable>> getReadScene()
    {
        synchronized (mUpdateLock) {
            final Scene readScene = mReadScene;
            mReadScene = mBufferScene;
            mBufferScene = readScene;

            return mReadScene;
        }
    }

    @Override
    public void flush()
    {
        synchronized (mUpdateLock) {
            final Scene oldScene = mWriteScene;
            mWriteScene = mBufferScene;
            mBufferScene = oldScene;
        }
    }
}
