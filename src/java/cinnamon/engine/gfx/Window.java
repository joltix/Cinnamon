package cinnamon.engine.gfx;

import cinnamon.engine.event.Gamepad;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.IntegratableInput;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import cinnamon.engine.event.IntegratableInput.MouseButtonCallback;
import cinnamon.engine.event.IntegratableInput.MouseScrollCallback;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * OO window management with rendering off a background thread. As a GLFW wrapper, Window.{@link #terminate()}
 * must be called when this class is no longer in use.
 *
 * <p>Keyboard, mouse, and gamepad input is exposed through a specialized {@code Input} implementation from
 * {@link #getInput()}.</p>
 *
 * <p>By default, a {@code Window} is not fullscreen, opens using the minimum size of {@link #MINIMUM_WIDTH} x
 * {@link #MINIMUM_HEIGHT}, is decorated, not resizable, and has vsync enabled.</p>
 *
 * <br><p><i>Warning: GLFW should not be used while this class is in use. This class makes use of {@code GLFW} to
 * maintain its state and so some of {@code GLFW}'s methods, such as {@code GLFW.glfwTerminate()} interferes with this
 * class' expectations.</i></p>
 *
 * <h3>State</h3>
 * <p>A {@code Window} can be open, closed, or destroyed. Upon instantiation, a {@code Window} begins in the closed
 * state and transitions to the open state with {@code open()}. Likewise, a {@code Window} closes with
 * {@code close()} but will also enter this state through {@code destroy()}.</p>
 *
 * <p>The difference between a closed and destroyed {@code Window} is that a closed {@code Window} can still be
 * reopened whereas a destroyed one has had its resources released and whose instance is no longer usable.</p>
 *
 * <pPortions of a {@code Window}'s state can still be modified while closed, such as its size. For state
 * transitioning methods like {@link #minimize()}, the desired state will not take effect until the next call to
 * {@code open()}.</p>
 *
 * <p>Continuously calling {@code Window.pollEvents()} is required to maintain the window's visibility and
 * ancillary operations. The following is an outline of managing a {@code Window} from instantiation to termination.</p>
 *
 * <pre>
 *     <code>
 *
 *         Window win = new Window(new CanvasImpl());
 *         Monitor primary = Monitor.getPrimaryMonitor();
 *
 *         win.setSize(2560, 1440);
 *         win.setPositionCenter(primary);
 *
 *         win.open();
 *
 *         while (!win.shouldClose()) {
 *
 *             // Process window and input events
 *             Window.pollEvents();
 *
 *             // Do work, eventually stopping with win.setShouldClose(true);
 *         }
 *
 *         // Or win.destroy() if another Window will be created and used soon after
 *         Window.terminate();
 *     </code>
 * </pre>
 *
 * <h3>Input</h3>
 * <p>Each {@code Window} produces an {@code Input} for generating events from the keyboard, mouse, and gamepads as
 * well as allowing read-access to the devices' event histories. The input update rate is tied to the rate of calling
 * {@code Window.pollEvents()}. It should be noted that calling this method at a low rate can result in missed
 * gamepad data since the hardware can be interacted with at moments in-between polls.</p>
 *
 * <h3>Multiple windows</h3>
 * <p>Managing more than one window differs only in favoring relinquishing the loop's conditional to the
 * application instead of using {@link #shouldClose()} and {@link #setShouldClose(boolean)}.</p>
 *
 * <p>Below is the earlier example modified for two.</p>
 *
 * <pre>
 *     <code>
 *
 *         Window first = new Window(new CanvasImpl(), "First");
 *         Window second = new Window(new CanvasImpl(), "Second");
 *
 *         first.setSize(640, 480);
 *         second.setSize(640, 480);
 *
 *         first.setPosition(0, 0);
 *         second.setPosition(640, 0);
 *
 *         first.open();
 *         second.open();
 *
 *         while (application should continue) {
 *
 *             // Process window and input events
 *             Window.pollEvents();
 *
 *             // Do work
 *         }
 *
 *         // Closes and destroys all windows
 *         Window.terminate();
 *     </code>
 * </pre>
 *
 * <h3>Concurrency</h3>
 * <p>Most methods should only be called on the main thread. This is noted in the documentation for those affected.</p>
 *
 * <p>All callbacks are notified on the main thread.</p>
 */
public final class Window
{
    /**
     * Minimum allowed width.
     */
    public static final int MINIMUM_WIDTH = 320;

    /**
     * Minimum allowed height.
     */
    public static final int MINIMUM_HEIGHT = 240;

    /**
     * Describes a closed window's desired state to be honored when opened. The state is set by the various state
     * transitioning methods while the window is closed.
     */
    private enum PendingVisibleState
    {
        WINDOWED,
        MINIMIZED,
        MAXIMIZED,
        FULLSCREEN
    }

    // Main thread tracks these windows
    private static final List<Window> mAvailableWindows = new ArrayList<>();

    // Used in methods called continuously; this prevents repeatedly creating arrays with values()
    private static final Connection[] CONNECTIONS = Connection.values();

    // Dedicated to running GL operations from Canvases
    private static RenderThread mRenderThread;

    private static Window mWindowInFocus;

    private final List<OnSizeChangeListener> mOnSizeChangeListeners = new ArrayList<>();

    private final List<OnSizeChangeListener> mOnFramebufferOnSizeChangeListeners = new ArrayList<>();

    private final List<OnFocusChangeListener> mOnFocusChangeListeners = new ArrayList<>();

    private final List<OnMinimizeListener> mOnMinimizeListeners = new ArrayList<>();

    private final List<OnMaximizeListener> mOnMaximizeListeners = new ArrayList<>();

    private CloseCallback mCloseCallback;

    private final GamepadUpdateCallback mGamepadUpdateCallback;

    // Client area rendering
    private final Canvas mCanvas;

    private final IntegratableInput mInput;

    // GLFW handle
    private final long mIdGLFW;

    // Width in pixels
    private int mFramebufferWidth = 0;

    // Height in pixels
    private int mFramebufferHeight = 0;

    private String mWinTitle = "";

    // State to go into when opened
    private PendingVisibleState mPending = PendingVisibleState.WINDOWED;

    // Most recent windowed mode size and position
    private RestoreState mRestore;

    // Monitor used when fullscreen
    private Monitor mFullscreenHost;

    private final double[] mMousePosX = new double[1];

    private final double[] mMousePosY = new double[1];

    // True if vsync has been enabled
    private boolean mVsync = true;

    // True if the canvas has already started up; window was opened before
    private boolean mCanvasStarted = false;

    // True if canvas is no longer used; GL methods will no longer be called
    private volatile boolean mRenderingStopped = false;

    // True if window can no longer be used
    private volatile boolean mDestroyed = false;

    /**
     * Constructs a {@code Window} with a specified canvas.
     *
     * <p>This constructor should only be called on the main thread.</p>
     *
     * @param canvas canvas.
     * @throws NullPointerException if canvas is null.
     * @throws IllegalStateException if GLFW failed to initialize.
     */
    public Window(Canvas canvas)
    {
        checkNotNull(canvas);

        ensureGLFWIsInitialized();

        mCanvas = canvas;
        mIdGLFW = createWindow();

        readFrameBufferSize();

        mInput = new IntegratableInput();
        createGamepads();

        mGamepadUpdateCallback = mInput.getGamepadUpdateCallback();
        setStateCallbacks();
        setInputCallbacks();

        Window.tryToSetJoystickConnectionCallback();
        mAvailableWindows.add(this);

        ensureRenderThreadIsAlive();

        // Make sure monitors list is populated
        Monitor.getConnectedMonitors();

        // Snapshot size and position
        mRestore = new RestoreState(this);
    }

    /**
     * Returns {@code true} if this window can no longer be opened.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if unusable.
     */
    public boolean isDestroyed()
    {
        return mDestroyed;
    }

    /**
     * Makes this window no longer usable. This window cannot be reopened and most methods cannot be used.
     *
     * <p>If this window is open, it is automatically closed. All callbacks are removed.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void destroy()
    {
        checkWindowIsAlive();

        destroyInstance();
        mAvailableWindows.remove(this);

        tryToRemoveJoystickCallback();
    }

    /**
     * Returns {@code true} if this window is open.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if open.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isOpen()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE;
    }

    /**
     * Opens this window. If this window is already open, this method does nothing.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void open()
    {
        checkWindowIsAlive();

        if (isOpen()) {
            return;
        }

        makeWindowVisible();

        if (!mCanvasStarted) {
            startUpCanvas();
            mCanvasStarted = true;
        } else {
            drawCanvas();
        }
    }

    /**
     * Closes this window and stops rendering operations.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void close()
    {
        checkWindowIsAlive();

        if (!isOpen()) {
            return;
        }

        GLFW.glfwHideWindow(mIdGLFW);

        mRenderThread.mGLFWMessages.add((thread) ->
        {
            thread.mRenderableWindows.remove(this);
        });
    }

    /**
     * Returns {@code true} if this window has been asked to close by either the close button on the title bar or
     * {@code setShouldClose(true)}. This result is unaffected by {@link #close()}.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return true if a close was requested.
     * @see #setShouldClose(boolean)
     */
    public boolean shouldClose()
    {
        return GLFW.glfwWindowShouldClose(mIdGLFW);
    }

    /**
     * Sets this window's close flag.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @param close true if the window should close.
     * @see #shouldClose()
     */
    public void setShouldClose(boolean close)
    {
        GLFW.glfwSetWindowShouldClose(mIdGLFW, close);
    }

    /**
     * Gets the rendering component.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return canvas.
     */
    public Canvas getCanvas()
    {
        return mCanvas;
    }

    /**
     * Gets the {@code InputEvent} generator.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return input event generator.
     */
    public IntegratableInput getInput()
    {
        return mInput;
    }

    /**
     * Gets the width of the client area in screen coordinates.
     *
     * <p>This width does not take into account this window's frame: {@code total width = this width + frame width}
     * where {@code frame width} is the sum of the frame's left and right extents.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return width in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     * @see #getFrameExtents()
     */
    public int getWidth()
    {
        checkWindowIsAlive();

        final int[] width = new int[1];
        GLFW.glfwGetWindowSize(mIdGLFW, width, null);

        return width[0];
    }

    /**
     * Gets the height of the client area in screen coordinates.
     *
     * <p>This height does not take into account this window's frame: {@code total height = this height + frame
     * height} where {@code frame height} is the sum of the frame's top and bottom extents.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return height in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     * @see #getFrameExtents()
     */
    public int getHeight()
    {
        checkWindowIsAlive();

        final int[] height = new int[1];
        GLFW.glfwGetWindowSize(mIdGLFW, null, height);

        return height[0];
    }

    /**
     * Sets the width of the client area in screen coordinates.
     *
     * <p>This width does not take into account this window's frame: {@code total width = this width + frame width}
     * where {@code frame width} is the sum of the frame's left and right extents.</p>
     *
     * <p>If {@code width} is {@literal <} {@link #MINIMUM_WIDTH}, it is clamped to {@code MINIMUM_WIDTH}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     * @see #getFrameExtents()
     */
    public void setWidth(int width)
    {
        checkWindowIsAlive();

        width = (width < MINIMUM_WIDTH) ? MINIMUM_WIDTH : width;

        GLFW.glfwSetWindowSize(mIdGLFW, width, getHeight());
    }

    /**
     * Sets the height of the client area in screen coordinates.
     *
     * <p>This height does not take into account this window's frame: {@code total height = this height + frame
     * height} where {@code frame height} is the sum of the frame's top and bottom extents.</p>
     *
     * <p>If {@code height} is {@literal <} {@link #MINIMUM_HEIGHT}, it is clamped to {@code MINIMUM_HEIGHT}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param height height in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     * @see #getFrameExtents()
     */
    public void setHeight(int height)
    {
        checkWindowIsAlive();

        height = (height < MINIMUM_HEIGHT) ? MINIMUM_HEIGHT : height;

        GLFW.glfwSetWindowSize(mIdGLFW, getWidth(), height);
    }

    /**
     * Sets the width and height of the client area in screen coordinates.
     *
     * <p>This size does not take into account this window's frame: {@code total width = this width + frame width}
     * and {@code total height = this height + frame height} where {@code frame width} is the sum of the frame's left
     * and right extents and {@code frame height} is the sum of the frame's top and bottom extents.</p>
     *
     * <p>If {@code width} is {@literal <} {@link #MINIMUM_WIDTH}, it is clamped to {@code MINIMUM_WIDTH} and if
     * {@code height} is {@literal <} {@link #MINIMUM_HEIGHT}, it is clamped to {@code MINIMUM_HEIGHT}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width in screen coordinates.
     * @param height height in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     * @see #getFrameExtents()
     */
    public void setSize(int width, int height)
    {
        checkWindowIsAlive();

        width = (width < MINIMUM_WIDTH) ? MINIMUM_WIDTH : width;
        height = (height < MINIMUM_HEIGHT) ? MINIMUM_HEIGHT : height;

        GLFW.glfwSetWindowSize(mIdGLFW, width, height);
    }

    /**
     * Gets the extents of this window's frame around the client area. The returned array will be of length 4 and
     * all values {@literal >=} 0.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return left, top, right, and bottom extents in screen coordinates, respectively.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public int[] getFrameExtents()
    {
        checkWindowIsAlive();

        final int[] extL = new int[1];
        final int[] extT = new int[1];
        final int[] extR = new int[1];
        final int[] extB = new int[1];

        GLFW.glfwGetWindowFrameSize(mIdGLFW, extL, extT, extR, extB);

        return new int[] {extL[0], extT[0], extR[0], extB[0]};
    }

    /**
     * Gets the width of the client area in pixels.
     *
     * <p>On some systems, this method will return a value different than {@link #getWidth()}. This method should be
     * used when dealing specifically with pixels (e.g. drawing operations). For screen coordinates, see
     * {@code getWidth()}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return width in pixels.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public int getFramebufferWidth()
    {
        checkWindowIsAlive();

        return mFramebufferWidth;
    }

    /**
     * Gets the height of the client area in pixels.
     *
     * <p>On some systems, this method will return a value different than {@link #getHeight()}. This method should be
     * used when dealing specifically with pixels (e.g. drawing operations). For screen coordinates, see
     * {@code getHeight()}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return height in pixels.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public int getFramebufferHeight()
    {
        checkWindowIsAlive();

        return mFramebufferHeight;
    }

    /**
     * Returns {@code true} if this window is fullscreen.
     *
     * <p>This method does not show intention to go fullscreen (i.e. this method will return {@code false} when
     * {@link #setFullscreen(Monitor)} is called when this window is not open.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if fullscreen.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isFullscreen()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowMonitor(mIdGLFW) != MemoryUtil.NULL;
    }

    /**
     * Sets the monitor to go fullscreen on.
     *
     * <p>If this window is not open, it will go fullscreen on the next call to {@link #open()}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param monitor fullscreen target.
     * @throws NullPointerException if monitor is null.
     * @throws IllegalArgumentException if monitor is not connected.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setFullscreen(Monitor monitor)
    {
        checkNotNull(monitor);
        checkMonitorIsConnected(monitor);
        checkWindowIsAlive();

        mFullscreenHost = monitor;

        if (isOpen()) {

            // Restore needed first because minimized windows don't go straight to fullscreen
            if (isMinimized()) {
                restore();
            }

            makeFullscreen(monitor.getHandle());
        } else {
            mPending = PendingVisibleState.FULLSCREEN;
        }
    }

    /**
     * Returns {@code true} if this window is resizable.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if resizable.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isResizable()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_RESIZABLE) == GLFW.GLFW_TRUE;
    }

    /**
     * Sets whether this window can be resized.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param resizable true to allow resizing.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setResizable(boolean resizable)
    {
        checkWindowIsAlive();

        final int enable = (resizable) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE;
        GLFW.glfwSetWindowAttrib(mIdGLFW, GLFW.GLFW_RESIZABLE, enable);
    }

    /**
     * Returns {@code true} if a frame is shown around this window.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if window frame is visible.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isDecorated()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
    }

    /**
     * Sets whether a frame around this window should be shown.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param decorated true to show a window frame.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setDecorated(boolean decorated)
    {
        checkWindowIsAlive();

        GLFW.glfwSetWindowAttrib(mIdGLFW, GLFW.GLFW_DECORATED, (decorated) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    /**
     * Gets the title bar's text.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return title.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public String getTitle()
    {
        checkWindowIsAlive();

        return mWinTitle;
    }

    /**
     * Sets the title bar's text.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param title title.
     * @throws NullPointerException if title is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setTitle(String title)
    {
        checkNotNull(title);
        checkWindowIsAlive();

        GLFW.glfwSetWindowTitle(mIdGLFW, title);
        mWinTitle = title;
    }

    /**
     * Returns {@code true} if the frame rate is being limited to match the host monitor's refresh rate.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if vsync is enabled.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isVsyncEnabled()
    {
        checkWindowIsAlive();

        return mVsync;
    }

    /**
     * Limits the frame rate to match the host monitor's refresh rate.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param enable true to limit fps.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setVsync(boolean enable)
    {
        checkWindowIsAlive();

        mVsync = enable;

        mRenderThread.mGLFWMessages.add((thread) ->
        {
            GLFW.glfwMakeContextCurrent(mIdGLFW);
            GLFW.glfwSwapInterval((mVsync) ? 1 : 0);
        });
    }

    /**
     * Returns {@code true} if this window has focus.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if focused.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isFocused()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE;
    }

    /**
     * Places this window above all others and gives it input focus. If this window is not open or this window is
     * minimized, this method does nothing.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void focus()
    {
        checkWindowIsAlive();

        // GLFW docs state should not be closed or minimized
        if (!isOpen() || isMinimized()) {
            return;
        }

        GLFW.glfwFocusWindow(mIdGLFW);
    }

    /**
     * Returns {@code true} if this window is minimized.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if minimized.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isMinimized()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE;
    }

    /**
     * Minimizes this window.
     *
     * <p>If this window is not open, it will be minimized when opened.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void minimize()
    {
        checkWindowIsAlive();

        if (isOpen()) {
            GLFW.glfwIconifyWindow(mIdGLFW);
        } else {
            mPending = PendingVisibleState.MINIMIZED;
        }
    }

    /**
     * Returns {@code true} if this window is maximized.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if maximized.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public boolean isMaximized()
    {
        checkWindowIsAlive();

        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE;
    }

    /**
     * Maximizes this window.
     *
     * <p>If this window is not open, it will be maximized when opened.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void maximize()
    {
        checkWindowIsAlive();

        if (isOpen()) {
            // Restore needed first because fullscreen windows don't go straight to maximized
            if (isFullscreen()) {
                restore();
            }
            GLFW.glfwMaximizeWindow(mIdGLFW);
        } else {
            mPending = PendingVisibleState.MAXIMIZED;
        }
    }

    /**
     * Restores this window to the most recent windowed size and screen position.
     *
     * <p>If this window is not open, it will appear in a windowed state when opened (i.e. un-minimized, un-maximized,
     * and not fullscreen). If the window is open, it will be restored to the most recent windowed state.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void restore()
    {
        checkWindowIsAlive();

        if (isOpen()) {

            if (isFullscreen()) {
                GLFW.glfwSetWindowMonitor(mIdGLFW, MemoryUtil.NULL, mRestore.mX, mRestore.mY, mRestore.mWidth,
                        mRestore.mHeight, GLFW.GLFW_DONT_CARE);

            } else {
                GLFW.glfwRestoreWindow(mIdGLFW);
            }

        } else {
            mPending = PendingVisibleState.WINDOWED;
        }
    }

    /**
     * Sets the icon. If more than one icon is given, the most appropriate size is chosen for the system.
     *
     * <p>All icons must be square with a side that is a positive multiple of 16, inclusive (sizes are expected along
     * the vein of 16x16, 32x32, and 48x48). Colors are expected to be 32-bit RGBA.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param icon icon.
     * @param more at varying scales.
     * @throws NullPointerException if icon is null.
     * @throws IllegalArgumentException if an icon's buffer has {@literal <} 1024 bytes remaining or if the remaining
     * amount of bytes are not representative of a square size of a positive multiple of 16.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setIcon(ByteBuffer icon, ByteBuffer...more)
    {
        checkNotNull(icon);
        checkWindowIsAlive();

        final ByteBuffer[] images = compactIconsAsSingleArray(icon, more);
        final GLFWImage.Buffer buffer = GLFWImage.malloc(images.length);

        for (int i = 0; i < images.length; i++) {
            final ByteBuffer img = images[i];
            checkNotNull(img);

            // Work back square resolution size
            final double totalSize = (double) img.remaining() / 4d;
            final double size = Math.sqrt(totalSize);

            if (img.remaining() < 1024) {
                throw new IllegalArgumentException("Icon has invalid bytes remaining: " + img.remaining());
            } else if (size % 16 != 0) {
                throw new IllegalArgumentException("Icon size must be a positive multiple of 16, actual: " + size);
            }

            buffer.position(i);
            buffer.pixels(images[i]);
            buffer.width((int) size);
            buffer.height((int) size);
        }

        GLFW.glfwSetWindowIcon(mIdGLFW, buffer);
    }

    /**
     * Gets the on-screen x position.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return x in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public int getX()
    {
        checkWindowIsAlive();

        final int[] x = new int[1];
        GLFW.glfwGetWindowPos(mIdGLFW, x, null);
        return x[0];
    }

    /**
     * Gets the on-screen y position.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return y in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public int getY()
    {
        checkWindowIsAlive();

        final int[] y = new int[1];
        GLFW.glfwGetWindowPos(mIdGLFW, null, y);
        return y[0];
    }

    /**
     * Sets this window's on-screen position.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param x x in screen coordinates.
     * @param y y in screen coordinates.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setPosition(int x, int y)
    {
        checkWindowIsAlive();

        GLFW.glfwSetWindowPos(mIdGLFW, x, y);
    }

    /**
     * Sets this window's on-screen position to center on a specified monitor. If this window is minimized,
     * maximized, or fullscreen, this method does nothing.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param monitor monitor to center on.
     * @throws NullPointerException if monitor is null.
     * @throws IllegalArgumentException if monitor is not connected.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setPositionCenter(Monitor monitor)
    {
        checkNotNull(monitor);
        checkMonitorIsConnected(monitor);
        checkWindowIsAlive();

        if (isMinimized() || isMaximized() || isFullscreen()) {
            return;
        }

        final int winWidth = getWidth();
        final int winHeight = getHeight();
        final float[] scale = computeMonitorScaleMismatchFactors(monitor);

        // Compute top left corner of a centered window
        final float x = ((float) monitor.getWidth() / 2f) - ((float) winWidth / 2f / scale[0]);
        final float y = ((float) monitor.getHeight() / 2f) - ((float) winHeight / 2f / scale[1]);

        // Shift for destination monitor's position
        GLFW.glfwSetWindowPos(mIdGLFW, (int) x + monitor.getX(), (int) y + monitor.getY());
    }

    /**
     * Adds an {@code OnSizeChangeListener} to be notified of changes to this window's size.
     *
     * <p>These dimensions are in screen coordinates. For dealing with pixel-based operations, see
     * {@link #addOnFramebufferSizeChangeListener(OnSizeChangeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void addOnSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnSizeChangeListeners.add(listener);
    }

    /**
     * Removes an {@code OnSizeChangeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void removeOnSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnSizeChangeListeners.remove(listener);
    }

    /**
     * Adds an {@code OnSizeChangeListener} to be notified of changes to this window's framebuffer size.
     *
     * <p>These dimensions are in pixels. For dealing with screen coordinate-based operations, see
     * {@link #addOnSizeChangeListener(OnSizeChangeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void addOnFramebufferSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnFramebufferOnSizeChangeListeners.add(listener);
    }

    /**
     * Removes an {@code OnSizeChangeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void removeOnFramebufferSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnFramebufferOnSizeChangeListeners.remove(listener);
    }

    /**
     * Adds an {@code OnFocusChangeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void addOnFocusChangeListener(OnFocusChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnFocusChangeListeners.add(listener);
    }

    /**
     * Removes an {@code OnFocusChangeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void removeOnFocusChangeListener(OnFocusChangeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnFocusChangeListeners.remove(listener);
    }

    /**
     * Adds an {@code OnMinimizeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void addOnMinimizeListener(OnMinimizeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnMinimizeListeners.add(listener);
    }

    /**
     * Removes an {@code OnMinimizeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void removeOnMinimizeListener(OnMinimizeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnMinimizeListeners.remove(listener);
    }

    /**
     * Adds an {@code OnMaximizeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void addOnMaximizeListener(OnMaximizeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnMaximizeListeners.add(listener);
    }

    /**
     * Removes an {@code OnMaximizeListener}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void removeOnMaximizeListener(OnMaximizeListener listener)
    {
        checkNotNull(listener);
        checkWindowIsAlive();

        mOnMaximizeListeners.remove(listener);
    }

    /**
     * Sets the {@code CloseCallback}.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param callback callback.
     * @throws NullPointerException if callback is null.
     * @throws IllegalStateException if this window has already been destroyed.
     */
    public void setCloseCallback(CloseCallback callback)
    {
        checkNotNull(callback);
        checkWindowIsAlive();

        mCloseCallback = callback;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * Reads the button and axis states of all connected gamepads to produce representative input events.
     */
    private void updateGamepads()
    {
        for (int i = 0; i < CONNECTIONS.length; i++) {

            final Connection connection = CONNECTIONS[i];
            final Gamepad pad = mInput.getGamepad(connection);

            if (pad == null || pad.isMuted()) {
                continue;
            }

            final ByteBuffer buttons = GLFW.glfwGetJoystickButtons(connection.toInt());
            final FloatBuffer axes = GLFW.glfwGetJoystickAxes(connection.toInt());

            // In case disconnected at system level right before reads
            if (buttons == null || axes == null) {
                continue;
            }

            mGamepadUpdateCallback.onButtonsUpdate(connection, buttons);
            mGamepadUpdateCallback.onAxesUpdate(connection, axes);
        }
    }

    /**
     * Permanently destroys the window. If the window is open, it is closed. All callbacks are removed. The
     * associated canvas is also shut down.
     *
     * <p>Note that this method will wait for the canvas to be removed from the render thread.</p>
     */
    private void destroyInstance()
    {
        if (isDestroyed()) {
            return;

        } else if (isOpen()) {
            // Stop rendering first
            close();
        }

        removeStateCallbacks();
        removeInputCallbacks();

        requestCanvasShutDown();
        waitForCanvasToStop();

        mDestroyed = true;
        GLFW.glfwDestroyWindow(mIdGLFW);
    }

    private void requestCanvasShutDown()
    {
        mRenderThread.mGLFWMessages.add((thread) ->
        {
            GLFW.glfwMakeContextCurrent(mIdGLFW);
            mCanvas.shutDown();

            GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);
            mRenderingStopped = true;
        });
    }

    private void waitForCanvasToStop()
    {
        while (!mRenderingStopped) {
            Thread.onSpinWait();
        }
    }

    /**
     * Sets the GLFW callbacks regarding the window's state such as whether it's maximized or has focus.
     */
    private void setStateCallbacks()
    {
        GLFW.glfwSetWindowPosCallback(mIdGLFW, (window, x, y) ->
        {
            // Track windowed position to restore back to
            if (!isMinimized() && !isMaximized() && !isFullscreen()) {
                mRestore.mX = x;
                mRestore.mY = y;
            }
        });

        GLFW.glfwSetWindowIconifyCallback(mIdGLFW, (handle, minimized) ->
        {
            if (minimized) {
                notifyMinimizeListeners();
            }
        });

        GLFW.glfwSetWindowMaximizeCallback(mIdGLFW, (handle, maximized) ->
        {
            if (maximized) {
                notifyMaximizeListeners();
            }
        });

        GLFW.glfwSetWindowFocusCallback(mIdGLFW, (handle, focused) ->
        {
            mWindowInFocus = (focused) ? this : null;

            notifyFocusListeners(focused);
        });

        GLFW.glfwSetWindowCloseCallback(mIdGLFW, (handle) ->
        {
            if (mCloseCallback != null) {

                // Check if callback wants to cancel the close request
                if (mCloseCallback.onClose()) {
                    GLFW.glfwSetWindowShouldClose(handle, false);
                } else {
                    close();
                }
            }
        });

        setSizeCallbacks();
    }

    /**
     * Removes GLFW callbacks regarding the window's state such as whether it's maximized and has focus.
     */
    private void removeStateCallbacks()
    {
        GLFW.glfwSetWindowPosCallback(mIdGLFW, null);
        GLFW.glfwSetWindowIconifyCallback(mIdGLFW, null);
        GLFW.glfwSetWindowMaximizeCallback(mIdGLFW, null);
        GLFW.glfwSetWindowFocusCallback(mIdGLFW, null);
        GLFW.glfwSetWindowCloseCallback(mIdGLFW, null);

        // Remove externally submitted listeners
        mOnMinimizeListeners.clear();
        mOnMaximizeListeners.clear();
        mOnFocusChangeListeners.clear();

        removeSizeCallbacks();
    }

    private void setInputCallbacks()
    {
        final MouseButtonCallback buttonCallback = mInput.getMouseButtonCallback();

        GLFW.glfwSetMouseButtonCallback(mIdGLFW, (window, button, action, mods) ->
        {
            GLFW.glfwGetCursorPos(mIdGLFW, mMousePosX, mMousePosY);
            buttonCallback.onButtonUpdate(button, action, mMousePosX[0], mMousePosY[0]);
        });

        final MouseScrollCallback scrollCallback = mInput.getMouseScrollCallback();

        GLFW.glfwSetScrollCallback(mIdGLFW, (window, xOffset, yOffset) ->
        {
            GLFW.glfwGetCursorPos(mIdGLFW, mMousePosX, mMousePosY);
            scrollCallback.onScrollUpdate(xOffset, yOffset, mMousePosX[0], mMousePosY[0]);
        });

        GLFW.glfwSetKeyCallback(mIdGLFW, mInput.getKeyboardKeyCallback());
        GLFW.glfwSetCursorPosCallback(mIdGLFW, mInput.getMousePositionCallback());
    }

    private void removeInputCallbacks()
    {
        GLFW.glfwSetMouseButtonCallback(mIdGLFW, null);
        GLFW.glfwSetCursorPosCallback(mIdGLFW, null);
        GLFW.glfwSetScrollCallback(mIdGLFW, null);
        GLFW.glfwSetKeyCallback(mIdGLFW, null);
    }

    private void removeSizeCallbacks()
    {
        GLFW.glfwSetWindowSizeCallback(mIdGLFW, null);
        GLFW.glfwSetFramebufferSizeCallback(mIdGLFW, null);

        mOnSizeChangeListeners.clear();
        mOnFramebufferOnSizeChangeListeners.clear();
    }

    private ByteBuffer[] compactIconsAsSingleArray(ByteBuffer icon, ByteBuffer[] more)
    {
        // Count extra images
        int scales = 0;
        for (int i = 0; i < more.length; i++) {
            scales += (more[i] != null) ? 1 : 0;
        }

        final ByteBuffer[] images = new ByteBuffer[1 + scales];
        images[0] = icon;

        // Compact all images into one array
        for (int i = 0, x = 1; i < more.length; i++) {
            if (more[i] != null) {
                images[x++] = more[i];
            }
        }

        return images;
    }

    /**
     * Checks each {@code Connection} for a present joystick and notifies the {@link IntegratableInput} of its presence.
     */
    private void createGamepads()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();

        for (final Connection connection : CONNECTIONS) {
            final int joystick = connection.toInt();

            if (GLFW.glfwJoystickPresent(joystick)) {
                final String name = GLFW.glfwGetJoystickName(joystick);

                // Alert gamepad connected
                callback.onConnection(joystick, name);
            }
        }
    }

    private void readFrameBufferSize()
    {
        // Read framebuffer size
        final IntBuffer frameWidth = BufferUtils.createIntBuffer(1);
        final IntBuffer frameHeight = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(mIdGLFW, frameWidth, frameHeight);

        frameWidth.clear();
        frameHeight.clear();

        mFramebufferWidth = frameWidth.get(0);
        mFramebufferHeight = frameHeight.get(0);
    }

    /**
     * These callbacks have been separated from {@link #setStateCallbacks()} to make {@link #setStateCallbacks()}
     * easier to read.
     */
    private void setSizeCallbacks()
    {
        GLFW.glfwSetWindowSizeCallback(mIdGLFW, (window, width, height) ->
        {
            // This callback should ignore size changes due to state transitions
            if (!isMinimized() && !isMaximized() && !isFullscreen()) {

                // Track size to restore back to
                if (width != 0 && height != 0) {
                    mRestore.mWidth = width;
                    mRestore.mHeight = height;
                }

                notifyWindowSizeListeners(width, height);
            }
        });

        GLFW.glfwSetFramebufferSizeCallback(mIdGLFW, (window, width, height) ->
        {
            // Guard against repeated size notifications
            if (mFramebufferWidth != width || mFramebufferHeight != height) {
                mRenderThread.mGLFWMessages.add((thread) ->
                {
                    GLFW.glfwMakeContextCurrent(mIdGLFW);
                    mCanvas.resize(width, height);
                });
            }

            mFramebufferWidth = width;
            mFramebufferHeight = height;
            notifyFramebufferSizeListeners(width, height);
        });
    }

    /**
     * Creates GLFW's window.
     *
     * @throws IllegalStateException if GLFW failed to create a window.
     * @return window handle.
     */
    private long createWindow()
    {
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);

        final long nullAddr = MemoryUtil.NULL;
        final long id = GLFW.glfwCreateWindow(MINIMUM_WIDTH, MINIMUM_HEIGHT, mWinTitle, nullAddr, nullAddr);

        if (id == MemoryUtil.NULL) {
            throw new IllegalStateException("Window creation failed");
        }

        return id;
    }

    private void makeWindowVisible()
    {
        switch (mPending) {
            case FULLSCREEN:
                makeFullscreen(mFullscreenHost.getHandle()); break;
            case MINIMIZED:
                GLFW.glfwIconifyWindow(mIdGLFW); break;
            case MAXIMIZED:
                GLFW.glfwMaximizeWindow(mIdGLFW); break;
            default:
                GLFW.glfwShowWindow(mIdGLFW);
        }

        assert (GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE);
    }

    private void makeFullscreen(long monitor)
    {
        final int[] width = new int[1];
        final int[] height = new int[1];
        GLFW.glfwGetWindowSize(mIdGLFW, width, height);

        final int[] x = new int[1];
        final int[] y = new int[1];
        GLFW.glfwGetWindowPos(mIdGLFW, x, y);

        GLFW.glfwSetWindowMonitor(mIdGLFW, monitor, x[0], y[0], width[0], height[0], GLFW.GLFW_DONT_CARE);
    }

    private void startUpCanvas()
    {
        mRenderThread.mGLFWMessages.add((thread) ->
        {
            GLFW.glfwMakeContextCurrent(mIdGLFW);
            GL.createCapabilities();

            mCanvas.startUp();
            thread.mRenderableWindows.add(this);
        });
    }

    private void drawCanvas()
    {
        mRenderThread.mGLFWMessages.add((thread) ->
        {
            thread.mRenderableWindows.add(this);
        });
    }

    private float[] computeMonitorScaleMismatchFactors(Monitor destination)
    {
        // Get destination's visual scale
        final float[] dstScale = destination.getContentScale();
        final float dstX = dstScale[0];
        final float dstY = dstScale[1];

        // Get source's visual scale
        final float[] srcScaleY = new float[1];
        GLFW.glfwGetWindowContentScale(mIdGLFW, dstScale, srcScaleY);

        // Reuse array to return factors
        dstScale[0] = dstScale[0] / dstX;
        dstScale[1] = srcScaleY[0] / dstY;

        return dstScale;
    }

    private void notifyFocusListeners(boolean focus)
    {
        for (final OnFocusChangeListener listener : mOnFocusChangeListeners) {
            listener.onFocusChange(focus);
        }
    }

    private void notifyMinimizeListeners()
    {
        for (final OnMinimizeListener listener : mOnMinimizeListeners) {
            listener.onMinimize();
        }
    }

    private void notifyMaximizeListeners()
    {
        for (final OnMaximizeListener listener : mOnMaximizeListeners) {
            listener.onMaximize();
        }
    }

    private void notifyWindowSizeListeners(int width, int height)
    {
        for (final OnSizeChangeListener listener : mOnSizeChangeListeners) {
            listener.onSizeChange(width, height);
        }
    }

    private void notifyFramebufferSizeListeners(int width, int height)
    {
        for (final OnSizeChangeListener callback : mOnFramebufferOnSizeChangeListeners) {
            callback.onSizeChange(width, height);
        }
    }

    private void checkMonitorIsConnected(Monitor monitor)
    {
        if (!monitor.isConnected()) {
            throw new IllegalArgumentException("Monitor is not connected");
        }
    }

    private void checkWindowIsAlive()
    {
        if (mDestroyed) {
            throw new IllegalStateException("Window has already been destroyed");
        }
    }

    /**
     * Processes window and input events. This method should be called continuously.
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void pollEvents()
    {
        GLFW.glfwPollEvents();

        if (mWindowInFocus != null) {
            mWindowInFocus.updateGamepads();
        }
    }

    /**
     * Destroys all {@code Window}s.
     *
     * <p>If no other {@code GLFW} wrapping classes are in use, {@code GLFW} is terminated.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void terminate()
    {
        if (Window.isInUse()) {
            Window.tearDown();
        }

        if (!Monitor.isInUse()) {
            GLFW.glfwTerminate();
        }
    }

    static boolean isInUse()
    {
        return mRenderThread != null;
    }

    static void ensureGLFWIsInitialized()
    {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW failed to initialize");
        }
    }

    private static void ensureRenderThreadIsAlive()
    {
        if (mRenderThread != null) {
            return;
        }

        mRenderThread = new RenderThread();
        mRenderThread.start();
    }

    /**
     * Stops each {@code Window}'s {@code Canvas} from rendering, shuts down the rendering thread, and destroys all
     * {@code Window}s. With the exception that {@code GLFW} is not terminated, this method effectively resets this
     * class as if no {@code Window} was instantiated.
     */
    private static void tearDown()
    {
        // Tear down all windows and stop their canvases from rendering
        mAvailableWindows.forEach(Window::destroyInstance);
        mAvailableWindows.clear();

        Window.tryToRemoveJoystickCallback();

        // Stop render thread
        mRenderThread.mContinue = false;
        try {
            mRenderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mRenderThread = null;
        mWindowInFocus = null;
    }


    private static void tryToSetJoystickConnectionCallback()
    {
        if (!mAvailableWindows.isEmpty()) {
            return;
        }

        GLFW.glfwSetJoystickCallback((joystick, event) ->
        {
            if (event == GLFW.GLFW_CONNECTED) {

                // Notify all widows about new gamepad
                mAvailableWindows.forEach((window) ->
                {
                    final GamepadConnectionCallback callback = window.mInput.getGamepadConnectionCallback();
                    callback.onConnection(joystick, GLFW.glfwGetJoystickName(joystick));
                });
            } else {

                // Notify all windows about disconnection
                mAvailableWindows.forEach((window) ->
                {
                    window.mInput.getGamepadConnectionCallback().onDisconnection(joystick);
                });
            }
        });
    }

    private static void tryToRemoveJoystickCallback()
    {
        if (mAvailableWindows.isEmpty()) {
            GLFW.glfwSetJoystickCallback(null);
        }
    }

    private static void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Holds the most recent windowed size and position.
     */
    private class RestoreState
    {
        private int mWidth;

        private int mHeight;

        private int mX;

        private int mY;

        private RestoreState(Window window)
        {
            mWidth = window.getWidth();
            mHeight = window.getHeight();
            mX = window.getX();
            mY = window.getY();
        }
    }

    /**
     * Executes canvas operations. This thread is where all windows' rendering takes place.
     */
    private static class RenderThread extends Thread
    {
        // Windows to draw
        private final List<Window> mRenderableWindows = new ArrayList<>();

        // State changes from main thread
        private final Queue<Message> mGLFWMessages = new ConcurrentLinkedQueue<>();

        private volatile boolean mContinue = true;

        @Override
        public void run()
        {
            while (mContinue) {

                // Draw
                mRenderableWindows.forEach((window) ->
                {
                    GLFW.glfwMakeContextCurrent(window.mIdGLFW);
                    window.mCanvas.draw();
                    GLFW.glfwSwapBuffers(window.mIdGLFW);
                });

                // Execute commands from main; these are typically window state changes
                Message msg;
                while ((msg = mGLFWMessages.poll()) != null) {
                    msg.execute(this);
                }
            }

            assert (mRenderableWindows.isEmpty());
            assert (mGLFWMessages.isEmpty());
        }

        private interface Message
        {
            void execute(RenderThread runnable);
        }
    }

    /**
     * Callback to be notified when a window resizes.
     */
    public interface OnSizeChangeListener
    {
        /**
         * Called when resized.
         *
         * @param width width.
         * @param height height.
         */
        void onSizeChange(int width, int height);
    }

    /**
     * Callback to be notified when a window gains or loses focus.
     */
    public interface OnFocusChangeListener
    {
        /**
         * Called when focused or unfocused.
         *
         * @param focus true if now has focus.
         */
        void onFocusChange(boolean focus);
    }

    /**
     * Callback to be notified when a window minimizes.
     */
    public interface OnMinimizeListener
    {
        /**
         * Called when minimized.
         */
        void onMinimize();
    }

    /**
     * Callback to be notified when a window maximizes.
     */
    public interface OnMaximizeListener
    {
        /**
         * Called when maximized.
         */
        void onMaximize();
    }

    /**
     * Callback to be notified when a window receives a close request.
     */
    public interface CloseCallback
    {
        /**
         * Called when a close request is made.
         *
         * @return true to cancel the request.
         */
        boolean onClose();
    }
}