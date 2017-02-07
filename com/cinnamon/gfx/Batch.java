package com.cinnamon.gfx;

/**
 * <p>
 *     Batches are queues of {@link Drawable}s for delivering drawing data
 *     to a {@link Canvas}. All Drawables in a Batch must share the same
 *     {@link Texture} id in order to support batched drawing when received
 *     by the Canvas.to be
 * </p>
 *
 * <p>
 *     Batches are not required for drawing and are only an option, as
 *     implemented by the {@link ConcurrentSceneBuffer}.
 * </p>
 *
 *
 */
public interface Batch<E extends Drawable>
{
    /**
     * <p>Gets the next {@link Drawable} available.</p>
     *
     * @return E.
     */
    E poll();

    /**
     * <p>Gets the {@link Texture} id of all {@link Drawable}s in the Batch.</p>
     *
     * @return Texture id.
     */
    int getTexture();

    /**
     * <p>Gets the number of {@link Drawable}s in the Batch.</p>
     *
     * @return {@link Texture} id.
     */
    int size();

    /**
     * <p>Checks whether or not there are any {@link Drawable}s in the Batch
     * .</p>
     *
     * @return true if there are no Drawables.
     */
    boolean isEmpty();
}
