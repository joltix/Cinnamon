package com.cinnamon.system;

import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;

/**
 * <p>
 *     Wrapper class for the GLFW windowing library.
 * </p>
 */
public class Window
{
    // Callback for Window closing
    private OnEndListener mOnEndListener;

    // Generates Events from user input
    private Input mInput;

    // Window handle
    private long mWinId;

    // Window title bar
    private String mWinTitle;

    // Whether or not the Window is open
    private boolean mOpen = false;

    // Desired dimensions
    private int mWidth;
    private int mHeight;

    // Primary monitor dimensions
    private int mPrimaryWidth;
    private int mPrimaryHeight;

    // Whether or not vsync is desired
    private boolean mVsync = false;

    /**
     * <p>Constructor for a Window.</p>
     *
     * @param width width.
     * @param height height.
     * @param title title bar text.
     * @param input Input to generate {@link InputEvent}s.
     * @throws IllegalStateException if the windowing toolkit failed to
     * load.
     */
    public Window(int width, int height, String title, Input input)
    {
        mWidth = width;
        mHeight = height;
        mWinTitle = title;

        // Prep windowing toolkit and OpenGL
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }

        // Fix dimensions and hide window
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        // Read primary monitor size and set title
        getPrimaryResolution();
        if (title != null) {
            mWinTitle = title;
        }

        // Build (hidden) window
        setResolution(width, height);
        createWindow();

        // Instantiate default Input if none given
        mInput = (input == null) ? mInput = new DefaultInput(this) : input;
        mInput.bind();
    }

    /**
     * <p>Queries GLFW for the user's primary monitor dimensions.</p>
     */
    private void getPrimaryResolution()
    {
        GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        mPrimaryWidth = mode.width();
        mPrimaryHeight = mode.height();
    }

    /**
     * <p>Sets the Window's resolution. If either specified dimension is invalid (i.e. less than 1 or greater than
     * the primary monitor's full resolution) then the Window's will go fullscreen.</p>
     *
     * @param width width.
     * @param height height.
     */
    private void setResolution(int width, int height)
    {
        // Default to fullscreen on invalid resolution
        if (width < 1 || width > mPrimaryWidth
        || height < 1 || height > mPrimaryHeight) {
            mWidth = mPrimaryWidth;
            mHeight = mPrimaryHeight;

        } else {

            // Assign desired resolution
            mWidth = width;
            mHeight = height;
        }
    }

    /**
     * <p>Generates GLFW's Window.</p>
     */
    private void createWindow()
    {
        // Set monitor to apply fullscreen too
        long fillMonitorId = MemoryUtil.NULL;
        if (isFullscreen()) fillMonitorId = GLFW.glfwGetPrimaryMonitor();

        // Initialize window
        mWinId = GLFW.glfwCreateWindow(mWidth, mHeight, mWinTitle, fillMonitorId,
                MemoryUtil.NULL);

        if (mWinId == MemoryUtil.NULL) {
            throw new IllegalStateException("GameWindow failed to load");
        }

        // Center if windowed
        attemptToCenter();
    }

    /**
     * <pTries to centers the Window onscreen if it's not fullscreen.</p>
     */
    private void attemptToCenter()
    {
        if (!isFullscreen()) {
            int centerX = (mPrimaryWidth / 2) - (mWidth / 2);
            int centerY = (mPrimaryHeight / 2) - (mHeight / 2);
            GLFW.glfwSetWindowPos(mWinId, centerX, centerY);
        }
    }

    /**
     * <p>Shows the Window and begins processing input {@link Event}s.</p>
     */
    public final void show()
    {
        // Create default Input if none was set
        if (mInput == null) {
            mInput = new DefaultInput(this);
        }

        GLFW.glfwShowWindow(mWinId);
        mOpen = true;
    }

    /**
     * <p>Attempts to close the Window.</p>
     */
    public void close()
    {
        mOpen = false;
        GLFW.glfwSetWindowShouldClose(mWinId, true);
    }

    public void cleanup()
    {
        try {
            // Remove GLFW callbacks
            mInput.unbind();

            GLFW.glfwDestroyWindow(mWinId);
        } finally {
            GLFW.glfwTerminate();
        }
    }

    public void pollEvents()
    {
        GLFW.glfwPollEvents();
    }

    /**
     * <p>Gets the Window's width.</p>
     *
     * @return width.
     */
    public final int getWidth()
    {
        return mWidth;
    }

    /**
     * <p>Gets the Window's height.</p>
     *
     * @return height.
     */
    public final int getHeight()
    {
        return mHeight;
    }

    /**
     * <p>Checks whether or not the Window is fullscreen.</p>
     *
     * @return true if the dimensions match the primary monitor's.
     */
    public final boolean isFullscreen()
    {
        return mWidth == mPrimaryWidth && mHeight == mPrimaryHeight;
    }

    /**
     * <p>Sets the calling thread to enable OpenGL operations.</p>
     */
    public final void selectThreadGL()
    {
        GLFW.glfwMakeContextCurrent(mWinId);
    }

    /**
     * <p>Checks whether or not the Window is closing.</p>
     *
     * @return true if the Window is closing.
     */
    public final boolean isClosing()
    {
        return GLFW.glfwWindowShouldClose(mWinId);
    }

    /**
     * <p>Sets a listener to be called when the Window is closed.</p>
     *
     * @param listener callback.
     */
    public final void setOnEndListener(OnEndListener listener)
    {
        mOnEndListener = listener;
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
        GLFW.glfwSwapInterval((mVsync) ? 1 : 0);
    }

    public final void swapBuffers()
    {
        GLFW.glfwSwapBuffers(mWinId);
    }

    /**
     * <p>Gets the Window's handle.</p>
     *
     * @return Window handle.
     */
    public final long getId()
    {
        return mWinId;
    }

    /**
     * <p>Gets the {@link Input} associated with the Window for {@link Event}
     * polling.</p>
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
     * @param input
     * @throws IllegalStateException if this method is called after {@link #show()}.
     */
    public final void setInput(Input input)
    {
        // Don't allow swapping Input
        if (mOpen) {
            throw new IllegalStateException("Input may no longer be set after Window has been opened");
        }

        mInput = input;
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
        private long mWinId;

        /**
         * <p>Constructs an Input to gather input data from a given {@link Window}.</p>
         *
         * @param window Window to bind to.
         */
        protected Input(Window window)
        {
            mWinId = window.getId();
        }

        /**
         * <p>Retrieves the oldest stored {@link InputEvent}s and places them in a {@link ControlMap} for user
         * control bindings and an {@link DefaultEventHub} for propagation.</p>
         *
         * @param controls user control bindings.
         * @param hub {@link Event} propagator.
         */
        abstract void poll(ControlMap controls, EventHub hub);

        /**
         * <p>Attaches all input handling to the Window and begins processing user input into {@link InputEvent}s.</p>
         */
        abstract void bind();

        /**
         * <p>Unbinds all input handling from the Window. After this method is called, Input will no longer
         * receive user input to process and calling {@link #poll(ControlMap, EventHub)} will no longer produce
         * {@link InputEvent}s.</p>
         */
        abstract void unbind();

        /**
         * <p>Gets the host Window's id.</p>
         *
         * @return window id.
         */
        protected final long getWindowId()
        {
            return mWinId;
        }
    }
}
