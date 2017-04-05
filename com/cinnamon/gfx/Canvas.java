package com.cinnamon.gfx;

import com.cinnamon.system.Game;
import com.cinnamon.system.OnResizeListener;
import com.cinnamon.system.RateLogger;
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
 */
public abstract class Canvas<E extends Canvas.SceneBuffer, T extends ShaderFactory>
{
    // Identity matrix
    private static final float[] IDENTITY = new float[] {1, 0, 0, 0,
                                                         0, 1, 0, 0,
                                                         0, 0, 1, 0,
                                                         0, 0, 0, 1};

    /**
     * Indices for translation matrix modification; OpenGL uses column-major ordering so XYZ is at mat bottom
     */
    private static final int TRANSLATION_X = 12;
    private static final int TRANSLATION_Y = 13;
    private static final int TRANSLATION_Z = 14;

    // Interaction w/ Window and Game input
    private final Window mWindow;
    private final E mDrawInput;

    // Separate thread for looping independent of main
    private Thread mThread;

    // Loop rate measurement
    private RateLogger mRateLogger;

    // ShaderProgram storage
    private final T mShaderFactory;

    // Projection matrix
    private float[] mProjectionMat;

    // OpenGL version
    private String mGLVersion;

    // GPU vendor
    private String mGPUVendor;

    // GPU model
    private String mGPUModel;

    // Framebuffer size saved since last notified resize
    private int mWidth;
    private int mHeight;

    // Whether or not the framebuffer has resized (changed the drawing area)
    private volatile boolean mHasResized = true;

    // Whether or not the Canvas has stopped drawing ops
    private volatile boolean mStopped = false;

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

    /**
     * <p>Initializes OpenGL, loads resources, and begins drawing on a separate thread.</p>
     *
     * @param rateSamples number of samples to measure before averaging framerate.
     */
    public final void start(int rateSamples)
    {
        mRateLogger = new RateLogger(rateSamples);

        // Create a new projection matrix each time the Window size changes
        mWindow.addOnFramebufferResizeListener(new OnResizeListener()
        {
            @Override
            public void onResize(float oldWidth, float oldHeight, float width, float height)
            {
                // Defer viewport resize and related ops to Canvas' looping to allow GL access on other thread
                mHasResized = true;
            }
        });

        // Launch drawing on another Thread
        mThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                loop();
            }
        });

        mThread.start();
    }

    /**
     * <p>Begins the drawing loop. This method blocks.</p>
     */
    private void loop()
    {
        // Create projection and translation matrices
        mProjectionMat = createProjectionMatrix();

        // Enable OpenGL for current thread
        mWindow.selectThreadGL();

        GL.createCapabilities();

        mGLVersion = GL11.glGetString(GL11.GL_VERSION);
        mGPUVendor = GL11.glGetString(GL11.GL_VENDOR);
        mGPUModel = GL11.glGetString(GL11.GL_RENDERER);

        // Notify subclasses that GL resources can load
        onLoad();

        mRateLogger.start();

        while (!mWindow.isClosing()) {

            if (mHasResized) {
                // Update framebuffer size
                mWidth = mWindow.getFramebufferWidth();
                mHeight = mWindow.getFramebufferHeight();

                // Create new projection matrix from new size and match GL area
                mProjectionMat = createProjectionMatrix();
                GL11.glViewport(0, 0, getWidth(), getHeight());

                // Notify subclasses
                onResize();
                mHasResized = false;
            }

            // Defer drawing to subclasses
            draw(mDrawInput, mShaderFactory);

            // Display frame
            mWindow.swapBuffers();
            mRateLogger.log();
        }

        // Cleanup shaders and textures
        mShaderFactory.clear();

        mStopped = true;
    }

    /**
     * <p>Allows one-time rendering setup operations before drawing. This method will be called after OpenGL has been
     * initialized and related functions may be used.</p>
     */
    protected abstract void onLoad();

    /**
     * <p>Each call to this method will draw an individual frame. Typically, transformations for all objects in a
     * frame should be applied here.</p>
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
     * <p>Fills a given array with a translation matrix modified for the given x, y, and z values.</p>
     *
     * @param x x.
     * @param y y.
     * @param z z.
     * @throws IllegalArgumentException if the given array is not of length 16 (the length of a flattened 4x4 matrix).
     */
    protected final void getTranslationMatrix(float[] container, float x, float y, float z)
    {
        // Check length is a 4x4 matrix
        if (container.length != IDENTITY.length) {
            throw new IllegalArgumentException("Container must have a length of 16 (4x4 matrix)");
        }

        // Ensure container has identity matrix as base
        System.arraycopy(IDENTITY, 0, container, 0, IDENTITY.length);

        // Modify proper indices for given coordinates
        container[TRANSLATION_X] = x;
        container[TRANSLATION_Y] = y;
        container[TRANSLATION_Z] = z;
    }

    /**
     * <p>Sets the background color using color values 0-255 inclusive.</p>
     *
     * @param r red.
     * @param g green.
     * @param b blue.
     * @param a alpha.
     * @throws IllegalArgumentException if any of the color values are < 0 or > 255.
     */
    public void setBackgroundColor(int r, int g, int b, int a)
    {
        checkColorValues(r, g, b, a, 0, 255, "Colors must be 0.0-1.0 inclusive");

        GL11.glClearColor((float) r / 255, (float) g / 255, (float) b / 255, (float) a / 255);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * <p>Sets the background color using normalized values 0.0f - 1.0f inclusive.</p>
     *
     * @param r red.
     * @param g green.
     * @param b blue.
     * @param a alpha.
     * @throws IllegalArgumentException if any of the color values are < 0.0f or > 1.0f.
     */
    public void setBackgroundColorPercent(float r, float g, float b,
                                          float a)
    {
        checkColorValues(r, g, b, a, 0, 1, "Colors must be 0-255 inclusive");
        GL11.glClearColor(r, g, b, a);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    /**
     * <p>Throws an Exception if any of the given color values are out of a desired range.</p>
     *
     * @param r   red.
     * @param g   green.
     * @param b   blue.
     * @param a   alpha.
     * @param lo  lower bound.
     * @param hi  upper bound.
     * @param msg error.
     * @throws IllegalArgumentException if any of the color values surpass the range.
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
        return mWidth;
    }

    /**
     * <p>Gets the height of the drawing area.</p>
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mHeight;
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
     * <p>Gets the projection matrix applied to each image drawn on the Canvas. This method returns a copy of the
     * desired matrix instead of a reference to the original.</p>
     *
     * @return projection matrix.
     */
    protected final float[] getProjectionMatrix()
    {
        // Create on first call and store
        if (mProjectionMat == null) {
            mProjectionMat = createProjectionMatrix();
        }

        // In case subclass returns null from createProjectionMatrix()
        if (mProjectionMat == null) {
            return IDENTITY.clone();
        }

        return mProjectionMat.clone();
    }

    /**
     * <p>This method is called when the Canvas' drawing area has been resized. Calling
     * {@link #getProjectionMatrix()} will return a new projection matrix updated for the new dimensions.</p>
     */
    protected abstract void onResize();

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
     * <p>Gets the {@link ShaderFactory} used for shader selection when drawing.</p>
     *
     * @return ShaderFactory.
     */
    public final T getShaderFactory()
    {
        return mShaderFactory;
    }

    /**
     * <p>Creates the projection matrix to map objects drawn by the Canvas onto the screen.</p>
     *
     * @return a 4x4 matrix.
     */
    protected abstract float[] createProjectionMatrix();

    /**
     * <p>Queries OpenGL for any errors and prints them to the console if any are found.</p>
     */
    public static final void checkForOpenGLErrors()
    {
        final int err = GL11.glGetError();
        if (err != GL11.GL_NO_ERROR) {
            throw new RuntimeException("OpenGL error code: " + err);
        }
    }

    /**
     * <p>Gets a {@link String} describing the OpenGL version in use.</p>
     *
     * @return OpenGL version.
     */
    public final String getGLVersion()
    {
        return mGLVersion;
    }

    /**
     * <p>Gets a {@link String} describing the vendor ofthe gpu used with OpenGL</p>
     *
     * @return gpu vendor.
     */
    public final String getGPUVendor()
    {
        return mGPUVendor;
    }

    /**
     * <p>Gets a {@link String} describing the model of the gpu used with OpenGL.</p>
     *
     * @return gpu model.
     */
    public final String getGPUModel()
    {
        return mGPUModel;
    }

    /**
     * <p>Gets the most recently measured framerate.</p>
     *
     * @return framerate.
     */
    public final int getCurrentFramerate()
    {
        return mRateLogger.getRate();
    }

    /**
     * <p>Checks whether or not the Canvas has stopped drawing operations.</p>
     *
     * <p>If this method returns true, no more GL operations will be performed from the Canvas' thread.</p>
     *
     * @return true if no more GL methods will be called.
     */
    public final boolean hasStopped()
    {
        return mStopped;
    }

    /**
     * <p>
     *     SceneBuffer acts as an intermediary between the {@link Game}'s updating and the {@link Canvas}'s drawing.
     *     {@link Scene}s to be drawn are retrieved with {@link #getWriteScene()}, populated, then sent to be drawn
     *     with {@link #flush()}. In turn, the Canvas retrieves an available Scene with {@link #getReadScene()}.
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
