package cinnamon.engine.gfx;

import cinnamon.engine.utils.LoopMeasure;

/**
 * Facilitates rendering in a window's client area.
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *     <li>{@link #onStartUp()}</li>
 *     <li>{@link #onDraw()}</li>
 *     <li>{@link #onShutDown()}</li>
 * </ul>
 *
 * <h3>GL Use</h3>
 * <p>{@code onStartUp()} is the first method safely allowing GL calls. Since GL resources are not initialized until
 * this hook, such methods should <i>not</i> be used in the constructor. Methods where it is safe to call GL methods
 * are noted as such in their documentation.</p>
 */
public abstract class Canvas
{
    private final LoopMeasure mMeasure = new LoopMeasure(2);

    // Framebuffer width
    private int mWidth = 0;

    // Framebuffer height
    private int mHeight = 0;

    /**
     * Constructs a {@code Canvas}.
     */
    protected Canvas() { }

    /**
     * Gets the most recent frame rate.
     *
     * <p>This is a measurement of the actual frame rate and is not necessarily the same as the target rate specified
     * at construction.</p>
     *
     * @return measured frames per second.
     */
    public final int getFrameRate()
    {
        return mMeasure.getRate();
    }

    /**
     * Gets the average frame duration.
     *
     * @return duration in milliseconds.
     */
    public final double getFrameDuration()
    {
        return mMeasure.getAverageDuration();
    }

    /**
     * Gets the shortest recent frame duration.
     *
     * @return shortest duration in milliseconds.
     */
    public final double getMinimumFrameDuration()
    {
        return mMeasure.getMinimumDuration();
    }

    /**
     * Gets the longest recent frame duration.
     *
     * @return longest duration in milliseconds.
     */
    public final double getMaximumFrameDuration()
    {
        return mMeasure.getMaximumDuration();
    }

    /**
     * Gets the width of the drawing area.
     *
     * @return width.
     */
    public final int getWidth()
    {
        return mWidth;
    }

    /**
     * Gets the height of the drawing area.
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mHeight;
    }

    /**
     * Allows one-time setup operations prior to rendering the first frame. This is the first hook called after
     * OpenGL has been initialized and related functions may be used.
     *
     * <p>OpenGL methods may be used in this method.</p>
     */
    protected abstract void onStartUp();

    /**
     * Renders a frame.
     *
     * <p>Swapping the front and back buffers is scheduled outside the {@code Canvas} and should not be done here.</p>
     *
     * <p>OpenGL methods may be used in this method.</p>
     */
    protected abstract void onDraw();

    /**
     * This method marks when rendering has stopped and {@link #onDraw()} is no longer called. Implementations should
     * perform cleanup here.
     *
     * <p>OpenGL methods may be used in this method.</p>
     */
    protected abstract void onShutDown();

    /**
     * Called when the drawable area changes size.
     *
     * <p>OpenGL methods may be used in this method.</p>
     */
    protected abstract void onResize();

    @Override
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    final void startUp()
    {
        onStartUp();
        mMeasure.markLoopBegins(System.nanoTime());
    }

    final void shutDown()
    {
        onShutDown();
    }

    final void draw()
    {
        mMeasure.markIterationBegins(System.nanoTime());

        onDraw();

        mMeasure.markIterationEnds(System.nanoTime());
    }

    final void resize(int width, int height)
    {
        assert (width >= 0);
        assert (height >= 0);

        mWidth = width;
        mHeight = height;

        onResize();
    }
}
