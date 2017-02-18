package com.cinnamon.gfx;

import com.cinnamon.system.Game;
import com.cinnamon.system.Window;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

/**
 * <p>
 *     Represents an OpenGL backed drawing surface.
 * </p>
 *
 * <p>
 *     <b>No OpenGL calls should be made in the this class' constructor.</b>
 * </p>
 *
 *
 */
public abstract class Canvas<E extends Canvas.SceneBuffer, T extends
        ShaderFactory>
{
    // Identity matrix
    private static final float[] IDENTITY = new float[] {1, 0, 0, 0,
                                                         0, 1, 0, 0,
                                                         0, 0, 1, 0,
                                                         0, 0, 0, 1};

    // Translation matrix
    private final float[] TRANSLATION = new float[]{1, 0, 0, 0,
                                                    0, 1, 0, 0,
                                                    0, 0, 1, 0,
                                                    0, 0, 0, 1};

    // OpenGL uses column-major ordering so XYZ is at mat bottom
    private static final int TRANSLATION_X = 12;
    private static final int TRANSLATION_Y = 13;
    private static final int TRANSLATION_Z = 14;

    // Interaction w/ Window and Game input
    private final Window mWindow;
    private final E mDrawInput;

    // ShaderProgram storage
    private final T mShaderFactory;

    // Projection matrix
    private float[] mProjectionMat;

    /**
     * <p>Constructor for a Canvas.</p>
     *
     * @param window Window to draw in.
     * @param input source for draw requests.
     */
    public Canvas(Window window, E input, T shaders)
    {
        mWindow = window;
        mDrawInput = input;

        mShaderFactory = shaders;
    }

    public abstract void start();

    /**
     * <p>Begins the drawing loop. This method blocks.</p>
     */
    protected final void loop()
    {
        // Create projection and translation matrices
        mProjectionMat = createProjectionMatrix();

        // Enable OpenGL for current thread
        mWindow.selectThreadGL();

        // Reapply vsync desire
        // (must be called after selectThreadGL for effect)
        mWindow.setVsyncEnabled(mWindow.isVsyncEnabled());
        GL.createCapabilities();

        // Notify subclasses that GL resources can load
        onLoad();

        while (!mWindow.isClosing()) {

            // Defer drawing to subclasses
            draw(mDrawInput, mShaderFactory);

            // Display frame
            mWindow.swapBuffers();
        }
    }

    /**
     * <p>Allows one-time rendering setup operations before drawing. This method
     * will be called after OpenGL has been initialized and related functions
     * may be used. Subclasses must call this method before beginning the
     * looping process.</p>
     */
    protected abstract void onLoad();

    /**
     * <p>Each call to this method will draw an individual frame. Typically,
     * transformations for all objects in a frame should be applied here.</p>
     */
    protected abstract void draw(E input, ShaderFactory factory);

    /**
     * <p>Gets a copy of the identity matrix.</p>
     *
     * @return identity matrix.
     */
    public static final float[] getIdentityMatrix()
    {
        return IDENTITY.clone();
    }

    /**
     * <p>Constructs a translation matrix with the given shift values.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     * @return a 4x4 column major matrix.
     */
    protected float[] getTranslationMatrix(float x, float y, float z)
    {
        TRANSLATION[TRANSLATION_X] = x;
        TRANSLATION[TRANSLATION_Y] = y;
        TRANSLATION[TRANSLATION_Z] = z;
        return TRANSLATION;
    }

    /**
     * <p>Sets the background color using color values 0-255 inclusive.</p>
     *
     * @param r red.
     * @param g green.
     * @param b blue.
     * @param a alpha.
     * @throws IllegalArgumentException if any of the color values are < 0 or >
     *                                  255.
     */
    public void setBackgroundColor(int r, int g, int b, int a)
    {
        checkColorValues(r, g, b, a, 0, 255, "Colors must be 0.0-1.0 "
                + "inclusive");

        GL11.glClearColor((float) r / 255, (float) g / 255, (float) b /
                255, (float) a / 255);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * <p>Sets the background color using normalized values 0.0f - 1.0f
     * inclusive.</p>
     *
     * @param r red.
     * @param g green.
     * @param b blue.
     * @param a alpha.
     * @throws IllegalArgumentException if any of the color values are < 0.0f or
     *                                  > 1.0f.
     */
    public void setBackgroundColorPercent(float r, float g, float b,
                                          float a)
    {
        checkColorValues(r, g, b, a, 0, 1, "Colors must be 0-255 " +
                "inclusive");
        GL11.glClearColor(r, g, b, a);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * <p>Throws an Exception if any of the given color values are out of a
     * desired range.</p>
     *
     * @param r   red.
     * @param g   green.
     * @param b   blue.
     * @param a   alpha.
     * @param lo  lower bound.
     * @param hi  upper bound.
     * @param msg error.
     * @throws IllegalArgumentException if any of the color values surpass the
     *                                  range.
     */
    private void checkColorValues(float r, float g, float b, float a,
                                  float lo, float hi, String msg)
    {
        if ((r < lo || g < lo || b < lo || a < lo)
                || (r > hi || g > hi | b > hi | a > hi)) {
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * <p>Gets the width of the drawing area.</p>
     *
     * @return width.
     */
    public final int getWidth()
    {
        return mWindow.getWidth();
    }

    /**
     * <p>Gets the height of the drawing area.</p>
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mWindow.getHeight();
    }

    /**
     * <p>Gets the {@link Window} hosting the Canvas.</p>
     *
     * @return Window.
     */
    public final Window getWindow()
    {
        return mWindow;
    }

    /**
     * <p>Gets the projection matrix applied to each image drawn on the
     * Canvas. This method returns a copy of the desired matrix instead of a
     * reference to the original.</p>
     *
     * @return projection matrix.
     */
    protected final float[] getProjection()
    {
        if (mProjectionMat == null) {
            mProjectionMat = createProjectionMatrix();
        }

        // In case implementor returns null
        if (mProjectionMat == null) {
            return IDENTITY.clone();
        }
        return mProjectionMat.clone();
    }

    /**
     * <p>Gets the {@link SceneBuffer} used to make drawing data available.</p>
     *
     * @return SceneBuffer.
     */
    public final E getSceneBuffer()
    {
        return mDrawInput;
    }

    /**
     * <p>Gets the {@link ShaderFactory} used for shader selection when drawing
     * .</p>
     *
     * @return ShaderFactory.
     */
    public final T getShaderFactory()
    {
        return mShaderFactory;
    }

    /**
     * <p>Creates the projection matrix to map objects drawn by the Canvas
     * onto the screen.</p>
     *
     * @return a 4x4 matrix.
     */
    protected abstract float[] createProjectionMatrix();

    /**
     * <p>Queries OpenGL for any errors and prints them to the console if any
     * are found.</p>
     */
    public static final void checkOpenGLErrors()
    {
        final int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            throw new RuntimeException("OpenGL error code: " + err);
        }
    }

    /**
     * <p>
     *     SceneBuffer acts as an intermediary between the
     *     {@link Game}'s updating and the {@link Canvas}'s drawing.
     *     {@link Scene}s to be drawn are retrieved with
     *     {@link #getWriteScene()}, populated, then sent to be drawn
     *     with {@link #flush()}. In turn, the Canvas retrieves an available
     *     Scene with {@link #getReadScene()}.
     * </p>
     *
     * @param <E> drawing data to be moved from
     */
    public static abstract class SceneBuffer<E>
    {
        /**
         * <p>Gets an available {@link Scene} to populate with drawing data.</p>
         *
         * @return write Scene.
         */
        public abstract Scene<E> getWriteScene();

        /**
         * <p>Gets the next {@link Scene} to be drawn on screen.</p>
         *
         * @return read Scene.
         */
        public abstract Scene<E> getReadScene();

        /**
         * <p>Sends the read {@link Scene} to be drawn by the {@link Canvas} for
         * the next available frame.</p>
         *
         * <p>This method will block if called at the same time as
         * {@link #getReadScene()}.</p>
         */
        public abstract void flush();
    }
}
