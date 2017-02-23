package com.cinnamon.system;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallbackI;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 *     Represents an application window wrapping the GLFW windowing library.
 * </p>
 *
 * <p>
 *     While a Window's maximum resolution is bounded by the primary display's maximum resolution, the Window's
 *     minimum is set at {@link #MINIMUM_WIDTH} x {@link #MINIMUM_HEIGHT}. If the desired width or height is
 *     smaller than either bounds, the Window will execute in fullscreen. There are no aspect ratio
 *     controls and all width-height pairings in between the minimum and maximum dimensions are permitted.
 * </p>
 */
public class Window
{
    /**
     * <p>Minimum supported Window width.</p>
     */
    public static final int MINIMUM_WIDTH = 640;

    /**
     * <p>Minimum supported Window height.</p>
     */
    public static final int MINIMUM_HEIGHT = 480;

    // Listener for screen coordinate-based size changes
    private OnResizeListener mOnResizeListener;

    // Listener for framebuffer changes
    private OnResizeListener mOnFramebufferResizeListener;

    // Generates Events from user input
    private Input mInput;

    // Window handle
    private final long mId;

    // Window title bar
    private final String mWinTitle;

    // Whether or not the Window is open
    private boolean mOpen = false;

    // Desired dimensions
    private int mWidth;
    private int mHeight;

    // Framebuffer size in pixels
    private final AtomicInteger mFBWidth = new AtomicInteger();
    private final AtomicInteger mFBHeight = new AtomicInteger();

    // Primary monitor dimensions
    private int mPrimaryWidth;
    private int mPrimaryHeight;

    // Whether or not vsync is desired
    private boolean mVsync = true;

    /**
     * <p>Constructor for a Window.</p>
     *
     * <p>Although the Window can be set as resizable, this is only a request. In some cases, such as the requested
     * resolution being higher than the display can support or lower than {@link #MINIMUM_WIDTH} x
     * {@link #MINIMUM_HEIGHT}, the Window will not be resizable.</p>
     *
     * <p>This constructor should only be called on the main thread.</p>
     *
     * @param width width.
     * @param height height.
     * @param title title bar text.
     * @param resizable whether or not to allow resizing.
     * @throws IllegalStateException if the windowing toolkit failed to
     * load.
     */
    public Window(int width, int height, String title, boolean resizable)
    {
        // Prep windowing toolkit and OpenGL
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }

        // Apply resizability and hide window
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, (resizable) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        // Read main display size
        getPrimaryResolution();

        // Apply title
        mWinTitle = (title == null) ? "" : title;

        // Build (hidden) window
        setResolution(width, height);
        mId = createWindow();

        // Center if windowed
        attemptToCenter();

        // Set callback to update stored size when resizing
        GLFW.glfwSetWindowSizeCallback(mId, new SizeUpdateCallback());

        // Read framebuffer size
        final IntBuffer frameWidth = BufferUtils.createIntBuffer(1);
        final IntBuffer frameHeight = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(mId, frameWidth, frameHeight);
        frameWidth.clear();
        frameHeight.clear();

        // Store dimensions for later
        mFBWidth.set(frameWidth.get(0));
        mFBHeight.set(frameHeight.get(0));

        // Set min-max supported sizes when resizing
        GLFW.glfwSetWindowSizeLimits(mId, MINIMUM_WIDTH, MINIMUM_HEIGHT, mPrimaryWidth, mPrimaryHeight);
    }

    /**
     * <p>Queries GLFW for the user's primary monitor dimensions.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    private void getPrimaryResolution()
    {
        GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        mPrimaryWidth = mode.width();
        mPrimaryHeight = mode.height();
    }

    /**
     * <p>Sets the Window's resolution. If the desired resolution is more than the display is capable of or smaller
     * than the minimum supported resolution, the Window will be set to fullscreen. If either of these cases occur,
     * the Window's ability to resize will be disabled regardless of the argument passed in the constructor.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width.
     * @param height height.
     */
    private void setResolution(int width, int height)
    {
        // Check if desired dimensions are allowed
        final boolean tooBig = width > mPrimaryWidth && height > mPrimaryHeight;
        final boolean tooSmall = width < MINIMUM_WIDTH && height < MINIMUM_HEIGHT;

        // Default to fullscreen if larger than monitor's capability or smaller than min support
        if (tooBig || tooSmall) {
            mWidth = mPrimaryWidth;
            mHeight = mPrimaryHeight;

            // Turn off resizable regardless of constructor's args since fullscreen
            GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        } else {
            // Assign desired resolution
            mWidth = width;
            mHeight = height;
        }
    }

    /**
     * <p>Generates GLFW's window and assigns it an id.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if GLFW failed to create a window.
     */
    private long createWindow()
    {
        // Set monitor to apply fullscreen too
        final long fillMonitorId = (isFullscreen()) ? GLFW.glfwGetPrimaryMonitor() : MemoryUtil.NULL;

        // Initialize window
        final long id = GLFW.glfwCreateWindow(mWidth, mHeight, mWinTitle, fillMonitorId, MemoryUtil.NULL);

        if (id == MemoryUtil.NULL) {
            throw new IllegalStateException("Window failed to load");
        }

        return id;
    }

    /**
     * <p>Tries to centers the Window onscreen if it's not fullscreen.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    private void attemptToCenter()
    {
        // Nothing to center when fullscreen
        if (isFullscreen()) {
            return;
        }

        // Compute main display's center and apply
        int centerX = (mPrimaryWidth / 2) - (mWidth / 2);
        int centerY = (mPrimaryHeight / 2) - (mHeight / 2);
        GLFW.glfwSetWindowPos(mId, centerX, centerY);
    }

    /**
     * <p>Shows the Window and begins processing input {@link Event}s.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if no {@link Input} has yet been set.
     */
    public final void show()
    {
        if (mInput == null) {
            throw new IllegalStateException("A Window.Input must be set before the Window is shown");
        }

        GLFW.glfwShowWindow(mId);
        mOpen = true;

        setVsyncEnabled(mVsync);
    }

    /**
     * <p>Attempts to close the Window.</p>
     *
     * <p>This method should be called before {@link #destroy()} and only from the main thread.</p>
     */
    public final void close()
    {
        mOpen = false;
        GLFW.glfwSetWindowShouldClose(mId, true);

        // Remove GLFW callbacks
        mInput.unbind();
    }

    /**
     * <p>Makes the calling thread's context current and releases the window's resources.</p>
     *
     * <p>This method should only be called after {@link #close()} and only from the main thread.</p>
     */
    public final void destroy()
    {
        selectThreadGL();
        GLFW.glfwDestroyWindow(mId);
        GLFW.glfwTerminate();
    }

    /**
     * <p>Processes windowing events and should be called continously to trigger the window's callbacks and update
     * its state. This method wraps {@link GLFW#glfwPollEvents()}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public final void pollEvents()
    {
        GLFW.glfwPollEvents();
    }

    /**
     * <p>Gets the Window's width in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return width.
     */
    public final int getWidth()
    {
        return mWidth;
    }

    /**
     * <p>Gets the Window's height in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mHeight;
    }

    /**
     * <p>Gets the width of the Window's drawable area in pixels.</p>
     *
     * <p>On some systems, this method will return a value different than {@link #getWidth()}. This method should be
     * used when dealing specifically with pixels. For screen coordinates, see getWidth().</p>
     *
     * <p>This method can safely be called from any thread.</p>
     *
     * @return width in pixels.
     */
    public final int getFramebufferWidth()
    {
        return mFBWidth.get();
    }

    /**
     * <p>Gets the height of the Window's drawable area in pixels.</p>
     *
     * <p>On some systems, this method will return a value different than {@link #getHeight()}. This method should
     * be used when dealing specifically with pixels. For screen coordinates, see getHeight().</p>
     *
     * <p>This method can safely be called from any thread.</p>
     *
     * @return height in pixels.
     */
    public final int getFramebufferHeight()
    {
        return mFBHeight.get();
    }

    /**
     * <p>Checks whether or not the Window is fullscreen.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if the dimensions match the primary monitor's.
     */
    public final boolean isFullscreen()
    {
        return mWidth == mPrimaryWidth && mHeight == mPrimaryHeight;
    }

    /**
     * <p>Sets the calling thread as responsible for GL operations for the Window and enables use of GL methods.</p>
     */
    public final void selectThreadGL()
    {
        GLFW.glfwMakeContextCurrent(mId);

        // Reapply vsync desire
        // (must be called after selectThreadGL for effect)
        setVsyncEnabled(mVsync);
    }

    /**
     * <p>Checks whether or not the Window is closing.</p>
     *
     * <p>This method can safely be called from any thread.</p>
     *
     * @return true if the Window is closing.
     */
    public final boolean isClosing()
    {
        return GLFW.glfwWindowShouldClose(mId);
    }

    /**
     * <p>Checks whether or not the frame rate is being limited to match the monitor's refresh rate.</p>
     *
     * @return true if Vsync is enabled.
     */
    public final boolean isVsyncEnabled()
    {
        return mVsync;
    }

    /**
     * <p>Limits the frame rate to match the monitor's.</p>
     *
     * @param enable true to control fps.
     */
    public final void setVsyncEnabled(boolean enable)
    {
        mVsync = enable;

        // Apply vsync setting
        GLFW.glfwSwapInterval((enable) ? 1 : 0);
    }

    /**
     * <p>Swaps the front and back frames to display the next image.</p>
     *
     * <p>This method should only be called from a thread who holds the Window's context through
     * {@link #selectThreadGL()}.</p>
     */
    public final void swapBuffers()
    {
        GLFW.glfwSwapBuffers(mId);
    }

    /**
     * <p>Gets the Window's id.</p>
     *
     * <p>This method can safely be called from any thread.</p>
     *
     * @return Window id.
     */
    private long getId()
    {
        return mId;
    }

    /**
     * <p>Gets the {@link Input} associated with the Window for {@link Event} polling.</p>
     *
     * @return Input.
     */
    public final Input getInput()
    {
        return mInput;
    }

    /**
     * <p>Sets the {@link Input} to use for generating {@link InputEvent}s.</p>
     *
     * @param input Input.
     * @throws IllegalStateException if this method is called after {@link #show()}.
     */
    public final void setInput(Input input)
    {
        // Don't allow swapping Input
        if (mOpen) {
            throw new IllegalStateException("Input may no longer be set after Window has been shown");
        }

        mInput = input;
        mInput.bind();
    }

    /**
     * <p>Sets an {@link OnResizeListener} to be notified of changes to the Window's screen size. These
     * dimensions are measured in screen coordinates. For dealing with pixel-based methods such as GL, see {@link
     * #setOnFramebufferResizeListener(OnResizeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener OnResizeListener.
     */
    public void setOnResizeListener(OnResizeListener listener)
    {
        mOnResizeListener = listener;
    }

    /**
     * <p>Sets an {@link OnResizeListener} to be notified of changes to the Window's framebuffer size. These
     * dimensions typically change when the Window's dimensions are changed.</p>
     *
     * <p>For dealing in screen coordinates such as {@link MouseEvent}s, see
     * {@link #setOnResizeListener(OnResizeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener OnResizeListener.
     */
    public void setOnFramebufferResizeListener(OnResizeListener listener)
    {
        mOnFramebufferResizeListener = listener;

        // Notify listener of resize
        if (mOnFramebufferResizeListener == null) {
            // Remove any previously set resize callback
            GLFW.glfwSetFramebufferSizeCallback(mId, null);

        } else {
            GLFW.glfwSetFramebufferSizeCallback(mId, new GLFWFramebufferSizeCallbackI()
            {
                @Override
                public void invoke(long window, int width, int height)
                {
                    final float oldW = mFBWidth.get();
                    final float oldH = mFBHeight.get();

                    // Update dimens in px
                    mFBWidth.set(width);
                    mFBHeight.set(height);

                    // Notify listener of size change
                    listener.onResize(oldW, oldH, width, height);
                }
            });
        }
    }

    /**
     * <p>
     *     Callback for updating {@link Window}'s stored dimensions according to changes in GLFW's window size.
     * </p>
     */
    private class SizeUpdateCallback implements GLFWWindowSizeCallbackI
    {
        @Override
        public void invoke(long window, int width, int height)
        {
            final float oldW = Window.this.mWidth;
            final float oldH = Window.this.mHeight;

            // Update size
            Window.this.mWidth = width;
            Window.this.mHeight = height;

            // Notify any set listener
            if (mOnResizeListener != null) {
                mOnResizeListener.onResize(oldW, oldH, width, height);
            }
        }
    }


    /**
     * <p>
     *     Facilitates a connection to the {@link Window}'s keyboard and mouse input.
     * </p>
     * <p>
     *     Classes wanting to receive {@link InputEvent}s from the @link Window} must call
     *     {@link #poll(ControlMap, EventHub)} continuously to remain up-to-date on newly available InputEvents.
     * </p>
     * <p>
     *     Implementors are recommended to use GLFW callbacks for gathering input data.
     * </p>
     */
    public static abstract class Input
    {
        // Host Window's id
        private long mId;

        /**
         * <p>Constructs an Input to gather input data from a given {@link Window}.</p>
         *
         * @param window Window to bind to.
         */
        protected Input(Window window)
        {
            mId = window.getId();
        }

        /**
         * <p>Retrieves the oldest stored {@link InputEvent}s and places them in a {@link ControlMap} for user
         * control bindings and an {@link EventHub} for propagation.</p>
         *
         * @param controls user control bindings.
         * @param hub {@link Event} propagator.
         */
        abstract void poll(ControlMap controls, EventHub hub);

        /**
         * <p>Attaches all input handling to the Window and begins translating user input into {@link InputEvent}s.</p>
         */
        abstract void bind();

        /**
         * <p>Unbinds all input handling from the Window. After this method is called, the set {@link Input} will no
         * longer receive user input to process and calling {@link #poll(ControlMap, EventHub)} will no longer produce
         * {@link InputEvent}s.</p>
         *
         * <p>This method should be used when ending operations.</p>
         */
        abstract void unbind();

        /**
         * <p>Gets the host Window's id.</p>
         *
         * @return window id.
         */
        protected final long getId()
        {
            return mId;
        }
    }
}
