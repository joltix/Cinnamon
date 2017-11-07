package cinnamon.engine.gfx;

import cinnamon.engine.event.Input;
import cinnamon.engine.utils.Size;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>This class wraps a GLFW window for OpenGL operations on a separate thread, multi-window management, and
 * integration with {@code Input} for producing {@code InputEvents}. By default, a {@code Window} opens using the
 * minimum allowed size of {@link #MINIMUM_WIDTH} x {@link #MINIMUM_HEIGHT}, is decorated, not resizable, and with
 * vsync enabled.</p>
 *
 * <p><i>Warning: Raw direct GLFW calls should not be used alongside this class as methods such as
 * {@code GLFW.glfwTerminate()} interferes with the expected state of a {@code Window}.</i></p><br>
 *
 * <b>State</b>
 * <p>A {@code Window} can be open, closed, or destroyed. Upon instantiation, it begins in the closed
 * state and transitions to the open state with {@code open()}. Likewise, a {@code Window} closes with
 * {@code close()} but can also enter this state through {@code destroy()}.</p>
 *
 * <p>The difference between a closed and destroyed {@code Window} is that a closed {@code Window} can still be
 * reopened whereas a destroyed one has had its resources released and whose instance is no longer usable. When the
 * last {@code Window} is destroyed, GLFW is terminated.</p>
 *
 * <p>Continuously calling {@code Window.pollEvents()} is required to maintain the {@code Window's} visibility and
 * and ancillary operations. This method is the same as {@code GLFW.glfwPollEvents()} but with {@code InputEvent}
 * processing. The following is an outline of managing a {@code Window} from instantiation to application
 * termination.</p>
 *
 * <pre>
 *     <code>
 *
 *         final Window window =...
 *
 *         // Configure before visible
 *         window.setSize(2560, 1440);
 *         window.setPositionCenter();
 *
 *         window.open();
 *
 *         while (isProcessing) {
 *          // Process window and input events
 *          Window.pollEvents();
 *
 *          // Do work
 *          ...
 *         }
 *
 *         window.destroy()
 *     </code>
 * </pre>
 *
 * <b>Input devices</b>
 * <p>Keyboard and mouse input depends on the {@code Window} in focus at the time of the event while gamepad input is
 * shared amongst all instances. The input's state update rate is tied to the rate of calling
 * {@code Window.pollEvents()}. It should be noted that calling this method at a low rate can result in missed gamepad
 * data since the hardware can be interacted with at moments in-between polls.</p><br>
 *
 * <b>Multiple windows</b>
 * <p>Handling more than one window is done by instantiating the needed number and calling {@code Window.pollEvents()}
 * once per loop. All created {@code Windows} can be opened, closed, and destroyed en mass through the methods
 * {@code Window.openAll()}, {@code Window.closeAll()}, and {@code Window.destroyAll()}. The total number of created
 * windows can be retrieved with {@code Window.getWindowCount()}.</p>
 *
 * <p><i>Note: An exception access violation has been observed to occasionally occur with 12 or more windows.
 * </i></p><br>
 *
 * <b>Concurrency</b>
 * <p>The constructor and most methods should only be called on the main thread. This is noted in the documentation
 * for affected methods. All callbacks are notified on the main thread.</p>
 */
public final class Window
{
    /**
     * <p>Minimum supported width.</p>
     */
    public static final int MINIMUM_WIDTH = 320;

    /**
     * <p>Minimum supported height.</p>
     */
    public static final int MINIMUM_HEIGHT = 240;

    private final List<OnResizeListener> mOnResizeListeners = new ArrayList<>();
    private final List<OnResizeListener> mOnFramebufferResizeListeners = new ArrayList<>();

    // All instantiated Windows for multi-window management
    private static final List<Window> mWindows = new ArrayList<>();

    private Thread mRenderThread;
    private final Canvas mCanvas;
    private final Input mInput;

    // Window's handle
    private final long mId;

    // Primary monitor dimensions in screen coords
    private int mPrimaryWidth;
    private int mPrimaryHeight;

    // Framebuffer size in pixels
    private final Object mFramebufferSizeLock = new Object();
    private int mFramebufferWidth = 0;
    private int mFramebufferHeight = 0;

    private String mWinTitle;
    private volatile boolean mVsync = true;
    private volatile boolean mDestroyed = false;
    private volatile boolean mFocused = true;
    private volatile boolean mMinimized = false;
    private volatile boolean mMaximized = false;

    private boolean mHasResized = false;

    /**
     * <p>Constructs a {@code Window} with a given title and a specified drawing surface.</p>
     *
     * <p>This constructor should only be called on the main thread.</p>
     *
     * @param canvas drawing operations.
     * @param title title bar text.
     * @throws NullPointerException if either canvas or title is null.
     * @throws IllegalStateException if system resources failed to initialize a window.
     */
    public Window(Canvas canvas, String title)
    {
        checkNull(canvas);
        checkNull(title);

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("GLFW initialization failed");
        }

        mWindows.add(this);
        mCanvas = canvas;
        mWinTitle = title;

        // Must be read prior to creating Window for correct resolution
        readPrimaryDisplayResolution();

        mId = createWindow();
        mInput = new Input(mId);

        setInputCallbacks();
        setStateCallbacks();
        setSizeCallbacks();

        readFrameBufferSize();
    }

    /**
     * <p>Checks if the window can no longer be opened.</p>
     *
     * @return true if unusable.
     */
    public boolean isDestroyed()
    {
        return mDestroyed;
    }

    /**
     * <p>Shows the window and begins processing {@code InputEvents}. If the window is already open, this method
     * does nothing.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @throws IllegalStateException if window has already been destroyed.
     */
    public void open()
    {
        if (isDestroyed()) {
            throw new IllegalStateException("Cannot reopen a destroyed Window");
        }
        if (isOpen()) {
            return;
        }

        // Render on separate thread
        mRenderThread = new Thread(() -> {
            GLFW.glfwMakeContextCurrent(mId);
            GL.createCapabilities();

            mCanvas.initialize();
            render();
            mCanvas.terminate();

            GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);
        });

        // Enable Input to produce events
        setInputCallbacks();

        GLFW.glfwShowWindow(mId);
        mRenderThread.start();
    }

    /**
     * <p>Checks if the window is going to close.</p>
     *
     * @return true if closing.
     */
    public boolean isClosing()
    {
        return GLFW.glfwWindowShouldClose(mId);
    }

    /**
     * <p>Closes the window and signals the render thread to halt drawing operations. This method will block until the
     * render thread ceases.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    public void close()
    {
        if (!isOpen()) {
            return;
        }

        GLFW.glfwHideWindow(mId);
        removeInputCallbacks();

        GLFW.glfwSetWindowShouldClose(mId, true);

        if (mRenderThread != null) {
            try {
                mRenderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>Gets the width of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * @return width.
     */
    public int getWidth()
    {
        final int[] width = new int[1];
        GLFW.glfwGetWindowSize(mId, width, null);
        return width[0];
    }

    /**
     * <p>Gets the height of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * @return height.
     */
    public int getHeight()
    {
        final int[] height = new int[1];
        GLFW.glfwGetWindowSize(mId, null, height);
        return height[0];
    }

    /**
     * <p>Sets the width of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width.
     * @throws IllegalArgumentException if width is {@literal <}= 0.
     */
    public void setWidth(int width)
    {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be >= 0");
        }

        GLFW.glfwSetWindowSize(mId, width, getHeight());
    }

    /**
     * <p>Sets the height of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param height height.
     * @throws IllegalArgumentException if height is {@literal <}= 0.
     */
    public void setHeight(int height)
    {
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be >= 0");
        }

        GLFW.glfwSetWindowSize(mId, getWidth(), height);
    }

    /**
     * <p>Sets the width and height of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width.
     * @param height height.
     * @throws IllegalArgumentException if either width or height is {@literal <}= 0.
     */
    public void setSize(int width, int height)
    {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be >= 0");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Height must be >= 0");
        }

        GLFW.glfwSetWindowSize(mId, width, height);
    }

    /**
     * <p>Gets the width of the window's drawable area in pixels.</p>
     *
     * <p>On some systems, this method will return a value different than {@link #getWidth()}. This method should be
     * used when dealing specifically with pixels (i.e. drawing operations). For screen coordinates, see
     * {@code getWidth()}.</p>
     *
     * @return width in pixels.
     */
    public int getFramebufferWidth()
    {
        synchronized (mFramebufferSizeLock) {
            return mFramebufferWidth;
        }
    }

    /**
     * <p>Gets the height of the window's drawable area in pixels.</p>
     *
     * <p>On some systems, this method will return a value different than {@link #getHeight()}. This method should
     * be used when dealing specifically with pixels (i.e. drawing operations). For screen coordinates, see
     * {@code getHeight()}.</p>
     *
     * @return height in pixels.
     */
    public int getFramebufferHeight()
    {
        synchronized (mFramebufferSizeLock) {
            return mFramebufferHeight;
        }
    }

    /**
     * <p>Sets whether or not the window should be fullscreen.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param enable true to go fullscreen.
     * @throws IllegalStateException if enable is true and no primary monitor was found.
     */
    public void setFullscreen(boolean enable)
    {
        final long monitor;
        if (!enable) {
            monitor = MemoryUtil.NULL;

        } else if ((monitor = GLFW.glfwGetPrimaryMonitor()) == MemoryUtil.NULL) {
            throw new IllegalStateException("No primary monitor was found");
        }

        final int[] width = new int[1];
        final int[] height = new int[1];
        GLFW.glfwGetWindowSize(mId, width, height);

        final int[] x = new int[1];
        final int[] y = new int[1];
        GLFW.glfwGetWindowPos(mId, x, y);

        GLFW.glfwSetWindowMonitor(mId, monitor, x[0], y[0], width[0], height[0], GLFW.GLFW_DONT_CARE);
    }

    /**
     * <p>Checks if the window is resizable.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if can be resized.
     */
    public boolean isResizable()
    {
        return GLFW.glfwGetWindowAttrib(mId, GLFW.GLFW_RESIZABLE) == GLFW.GLFW_TRUE;
    }

    /**
     * <p>Sets whether the window can be resized by dragging the cursor along its boundary. This method does not
     * affect programmatically changing the size through {@code setSize(float, float)} or the like.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param resizable true to allow resizing by cursor.
     */
    public void setResizable(boolean resizable)
    {
        GLFW.glfwSetWindowAttrib(mId, GLFW.GLFW_RESIZABLE, (resizable) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    /**
     * <p>Checks if a title bar is shown.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if title bar is visible.
     */
    public boolean isDecorated()
    {
        return GLFW.glfwGetWindowAttrib(mId, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
    }

    /**
     * <p>Sets whether a title bar should be shown.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param decorated true to show a title bar.
     */
    public void setDecorated(boolean decorated)
    {
        GLFW.glfwSetWindowAttrib(mId, GLFW.GLFW_DECORATED, (decorated) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    }

    /**
     * <p>Gets the title bar's text.</p>
     *
     * @return title bar text.
     */
    public String getTitle()
    {
        return mWinTitle;
    }

    /**
     * <p>Sets the title bar's text.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param title title bar text.
     * @throws NullPointerException if title is null.
     */
    public void setTitle(String title)
    {
        checkNull(title);

        GLFW.glfwSetWindowTitle(mId, title);
        mWinTitle = title;
    }

    /**
     * <p>Checks whether or not the frame rate is being limited to match the monitor's refresh rate.</p>
     *
     * @return true if vsync is enabled.
     */
    public boolean isVsyncEnabled()
    {
        return mVsync;
    }

    /**
     * <p>Limits the frame rate to match the monitor's.</p>
     *
     * @param enable true to limit fps.
     */
    public void setVsync(boolean enable)
    {
        mVsync = enable;
        final long actual = GLFW.glfwGetCurrentContext();

        GLFW.glfwMakeContextCurrent(mId);
        GLFW.glfwSwapInterval((mVsync) ? 1 : 0);

        // Restore in case was different
        GLFW.glfwMakeContextCurrent(actual);
    }

    /**
     * <p>Checks if the window has focus.</p>
     *
     * @return true if focused.
     */
    public boolean isFocused()
    {
        return mFocused;
    }

    /**
     * <p>Places the window above all others and gives it input focus. If the window is not open or minimized, this
     * method does nothing.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void focus()
    {
        // GLFW docs state should not be closed or minimized
        if (!isOpen() || isMinimized()) {
            return;
        }

        GLFW.glfwFocusWindow(mId);
    }

    /**
     * <p>Checks if the window is minimized.</p>
     *
     * @return true if minimized.
     */
    public boolean isMinimized()
    {
        return mMinimized;
    }

    /**
     * <p>Minimizes the window.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void minimize()
    {
        GLFW.glfwIconifyWindow(mId);
    }

    /**
     * <p>Checks if the window is maximized.</p>
     *
     * @return true if maximized.
     */
    public boolean isMaximized()
    {
        return mMaximized;
    }

    /**
     * <p>Maximizes the window. If the window is fullscreen, this method does nothing.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void maximize()
    {
        GLFW.glfwMaximizeWindow(mId);
    }

    /**
     * <p>Restores the video mode from before the window was minimized.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void restore()
    {
        GLFW.glfwRestoreWindow(mId);
    }

    /**
     * <p>Sets the icon. If more than one icon is given, GLFW will choose one with the most appropriate size for the
     * system.</p>
     *
     * <p>All icons must be square with a side that is a positive multiple of 16, inclusive (sizes are expected along
     * the vein of 16x16, 32x32, and 48x48). Colors are expected to be 32-bit RGBA.</p>
     *
     * <p>On Windows 10, the set icon is shown in the top left of the title bar and as the application icon in the task
     * bar. This method does nothing on macOS.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param icons at varying scales.
     * @throws NullPointerException if icons is null or an index within icons is null.
     * @throws IllegalArgumentException if an icon's buffer has {@literal <} 1024 bytes remaining or if the remaining
     * amount of bytes are not representative of a square size of a positive multiple of 16.
     */
    public void setIcon(ByteBuffer...icons)
    {
        checkNull(icons);

        final GLFWImage.Buffer buffer = GLFWImage.malloc(icons.length);
        for (int i = 0; i < icons.length; i++) {
            final ByteBuffer icon = icons[i];
            checkNull(icon);

            // Work back square resolution size
            final int totalSize = icon.remaining() / 4;
            final int size = (int) Math.sqrt(totalSize);

            if (icon.remaining() < 1024) {
                throw new IllegalArgumentException("Icon has invalid bytes remaining: " + icon.remaining());
            } else if (size % 16 != 0) {
                throw new IllegalArgumentException("Icon size must be a positive multiple of 16, actual: " + size);
            }

            buffer.position(i);
            buffer.pixels(icons[i]);
            buffer.width(size);
            buffer.height(size);
        }

        GLFW.glfwSetWindowIcon(mId, buffer);
    }

    /**
     * <p>Checks if the window is open.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if visible.
     */
    public boolean isOpen()
    {
        return GLFW.glfwGetWindowAttrib(mId, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE;
    }

    /**
     * <p>Gets the {@code Input} responsible for producing the window's {@code InputEvents}.</p>
     *
     * @return input processor.
     */
    public Input getInput()
    {
        return mInput;
    }

    /**
     * <p>Gets the x position on screen.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * @return x in screen coordinates.
     */
    public int getX()
    {
        final int[] x = new int[1];
        GLFW.glfwGetWindowPos(mId, x, null);
        return x[0];
    }

    /**
     * <p>Gets the y position on screen.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * @return y in screen coordinates.
     */
    public int getY()
    {
        final int[] y = new int[1];
        GLFW.glfwGetWindowPos(mId, null, y);
        return y[0];
    }

    /**
     * <p>Sets the window's position on the primary display. The given (x,y) position will be clamped to keep the
     * window within the primary display's boundaries.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param x x.
     * @param y y.
     */
    public void setPosition(int x, int y)
    {
        final int[] width = new int[1];
        final int[] height = new int[1];
        GLFW.glfwGetWindowSize(mId, width, height);

        // Keep x on screen
        if (x < 0) {
            x = 0;
        } else if ((x + width[0]) > mPrimaryWidth) {
            x = mPrimaryWidth - width[0];
        }

        // Keep y on screen
        if (y < 0) {
            y = 0;
        } else if ((y + height[0]) > mPrimaryHeight) {
            y = mPrimaryHeight - height[0];
        }

        GLFW.glfwSetWindowPos(mId, x, y);
    }

    /**
     * <p>Centers the window on the primary display.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void setPositionCenter()
    {
        final int width[] = new int[1];
        final int height[] = new int[1];
        GLFW.glfwGetWindowSize(mId, width, height);

        // Compute main display's center
        final int x =  (mPrimaryWidth / 2) - (width[0] / 2);
        final int y = (mPrimaryHeight / 2) - (height[0] / 2);

        GLFW.glfwSetWindowPos(mId, x, y);
    }

    /**
     * <p>Gets the resolution of the primary display. The returned {@code Size's} depth will be 0.</p>
     *
     * @return primary display size.
     */
    public Size getPrimaryDisplayResolution()
    {
        return new Size()
        {
            @Override
            public float getWidth()
            {
                return mPrimaryWidth;
            }

            @Override
            public float getHeight()
            {
                return mPrimaryHeight;
            }

            @Override
            public float getDepth()
            {
                return 0f;
            }
        };
    }

    /**
     * <p>Adds an {@link OnResizeListener} to be notified of changes to the window's screen size. These
     * dimensions are measured in screen coordinates. For dealing with pixel-based methods such as GL operations, see
     * {@link #addOnFramebufferResizeListener(OnResizeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void addOnResizeListener(OnResizeListener listener)
    {
        mOnResizeListeners.add(listener);
    }

    /**
     * <p>Removes an {@code OnResizeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     */
    public void removeOnResizeListener(OnResizeListener listener)
    {
        mOnResizeListeners.remove(listener);
    }

    /**
     * <p>Sets an {@link OnResizeListener} to be notified of changes to the window's framebuffer size. These
     * dimensions are measured in pixels. For dealing with screen coordinate-based methods such as moving the window
     * on the display, see {@link #addOnResizeListener(OnResizeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void addOnFramebufferResizeListener(OnResizeListener listener)
    {
        checkNull(listener);
        mOnFramebufferResizeListeners.add(listener);
    }

    /**
     * <p>Removes an {@code OnResizeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     */
    public void removeOnFramebufferResizeListener(OnResizeListener listener)
    {
        mOnFramebufferResizeListeners.remove(listener);
    }

    /**
     * <p>Marks the window as no longer usable. Once this method executes, the window cannot be reopened.</p>
     *
     * <p>If the window is open when this method is called, it is automatically closed. If the destroyed window was
     * the last {@code Window} instance, GLFW is terminated.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void destroy()
    {
        if (isDestroyed()) {
            return;

        } else if (isOpen()) {
            // Stop rendering first
            close();
        }

        mDestroyed = true;
        GLFW.glfwDestroyWindow(mId);
        removeStateCallbacks();

        final boolean removed = mWindows.remove(this);
        assert (removed);

        // Release resources if destroying last window
        if (mWindows.isEmpty()) {
            GLFW.glfwTerminate();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Since much of the {@code Window's} state relies on methods which require execution on the main thread, this
     * method should also only be called on the main thread.</p>
     *
     * @return hash code.
     */
    @Override
    public int hashCode()
    {
        int hash = 17 * 31 + mWinTitle.hashCode();
        hash = 31 * hash + Integer.hashCode(getWidth());
        hash = 31 * hash + Integer.hashCode(getHeight());
        hash = 31 * hash + Integer.hashCode(getX());
        return 31 * hash + Integer.hashCode(getY());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Since much of the {@code Window's} state relies on methods which require execution on the main thread, this
     * method should also only be called on the main thread.</p>
     *
     * @param obj the {@code Window} with which to compare.
     * @return true if the given object is a {@code Window} and both have the same title, size, position, open/close,
     * and minimize state.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != Window.class) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final Window window = (Window) obj;
        final boolean sizeMatches = getWidth() == window.getWidth() && getHeight() == window.getHeight();
        final boolean stateMatches = isOpen() == window.isOpen() && isMinimized() == window.isMinimized();
        final boolean positionMatches = getX() == window.getX() && getY() == window.getY();

        return getTitle().equals(window.getTitle()) && sizeMatches && stateMatches && positionMatches;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Processes windowing events and should be called continuously to trigger the window's callbacks. This method
     * wraps {@link GLFW#glfwPollEvents()}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void pollEvents()
    {
        GLFW.glfwPollEvents();
        Input.updateGamepads();
    }

    /**
     * <p>Opens all openable {@code Windows}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void openAll()
    {
        for (final Window window : mWindows) {
            window.open();
        }
    }

    /**
     * <p>Closes all closeable {@code Windows}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void closeAll()
    {
        for (final Window window : mWindows) {
            window.close();
        }
    }

    /**
     * <p>Destroys all {@code Windows}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void destroyAll()
    {
        final Object[] objs = mWindows.toArray();
        for (int i = 0; i < objs.length; i++) {
            ((Window) objs[i]).destroy();
        }
    }

    /**
     * <p>Gets the number of {@code Windows} yet to be destroyed.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return number of openable windows (including those already opened).
     */
    public static int getWindowCount()
    {
        return mWindows.size();
    }

    /**
     * <p>Sets the GLFW callbacks regarding the window's state such as whether it's maximized or has focus.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    private void setStateCallbacks()
    {
        GLFW.glfwSetWindowIconifyCallback(mId, (handle, minimized) ->
        {
            mMinimized = minimized;
        });
        GLFW.glfwSetWindowFocusCallback(mId, (handle, focused) ->
        {
            mFocused = focused;
        });
        GLFW.glfwSetWindowMaximizeCallback(mId, (handle, maximized) ->
        {
            mMaximized = maximized;
        });
        GLFW.glfwSetWindowCloseCallback(mId, (handle) ->
        {
            // Allow Window to sync state to GLFW directed close cmd
            close();
        });
    }

    /**
     * <p>Removes GLFW callbacks regarding the window's state such as whether it's maximized or has focus.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    private void removeStateCallbacks()
    {
        GLFW.glfwSetWindowIconifyCallback(mId, null);
        GLFW.glfwSetWindowFocusCallback(mId, null);
        GLFW.glfwSetWindowMaximizeCallback(mId, null);
        GLFW.glfwSetWindowCloseCallback(mId, null);
    }

    /**
     * <p>Sets the following input related callbacks for GLFW.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * <ul>
     *     <li>mouse button</li>
     *     <li>mouse scroll</li>
     *     <li>keyboard key</li>
     *     <li>joystick connection</li>
     * </ul>
     */
    private void setInputCallbacks()
    {
        GLFW.glfwSetMouseButtonCallback(mId, (handle, button, action, mods) ->
        {
            mInput.getMouseButtonCallback().invoke(handle, button, action, mods);
        });

        GLFW.glfwSetScrollCallback(mId, (handle, xOffset, yOffset) ->
        {
            mInput.getMouseScrollCallback().invoke(handle, xOffset, yOffset);
        });

        GLFW.glfwSetKeyCallback(mId, (handle, key, scanCode, action, mods) ->
        {
            mInput.getKeyboardCallback().invoke(handle, key, scanCode, action, mods);
        });

        // No need to keep setting for all windows
        if (mWindows.isEmpty()) {
            GLFW.glfwSetJoystickCallback((joystick, event) ->
            {
                Input.getGamepadConnectionCallback().invoke(joystick, event);
            });
        }
    }

    /**
     * <p>Removes the following input related callbacks from GLFW.</p>
     *
     * <ul>
     *     <li>mouse button</li>
     *     <li>mouse scroll</li>
     *     <li>keyboard key</li>
     *     <li>joystick connection</li>
     * </ul>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    private void removeInputCallbacks()
    {
        GLFW.glfwSetMouseButtonCallback(mId, null);
        GLFW.glfwSetScrollCallback(mId, null);
        GLFW.glfwSetKeyCallback(mId, null);

        // Only remove if no windows are available
        if (mWindows.isEmpty()) {
            GLFW.glfwSetJoystickCallback(null);
        }
    }

    /**
     * <p>Loops continuously to render in the window until GLFW signals its close.</p>
     */
    private void render()
    {
        while (!GLFW.glfwWindowShouldClose(mId)) {

            // Sync Canvas' size with framebuffer
            if (mHasResized) {
                synchronized (mFramebufferSizeLock) {
                    mCanvas.resizeTo(mFramebufferWidth, mFramebufferHeight);
                    mHasResized = false;
                }
            }

            mCanvas.consumeResourceRequests();
            mCanvas.draw();

            GLFW.glfwSwapBuffers(mId);
        }
    }

    /**
     * <p>Updates the {@code Window} with the frame buffer's current size from GLFW.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    private void readFrameBufferSize()
    {
        // Read framebuffer size
        final IntBuffer frameWidth = BufferUtils.createIntBuffer(1);
        final IntBuffer frameHeight = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetFramebufferSize(mId, frameWidth, frameHeight);

        frameWidth.clear();
        frameHeight.clear();

        synchronized (mFramebufferSizeLock) {
            mFramebufferWidth = frameWidth.get(0);
            mFramebufferHeight = frameHeight.get(0);
        }
    }

    /**
     * <p>Sets the callbacks for changes in window and framebuffer sizes. These callbacks update the {@code Window}
     * with new sizes and pass the notification along to any externally set listeners.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    private void setSizeCallbacks()
    {
        GLFW.glfwSetWindowSizeCallback(mId, (window, width, height) ->
        {
            for (final OnResizeListener listener : mOnResizeListeners) {
                listener.onResize(width, height);
            }
        });

        GLFW.glfwSetFramebufferSizeCallback(mId, (window, width, height) ->
        {
            synchronized (mFramebufferSizeLock) {
                mFramebufferWidth = width;
                mFramebufferHeight = height;
                mHasResized = true;
            }

            for (final OnResizeListener callback : mOnFramebufferResizeListeners) {
                callback.onResize(width, height);
            }
        });
    }

    /**
     * <p>Queries GLFW for the user's primary monitor dimensions.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    private void readPrimaryDisplayResolution()
    {
        final GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        mPrimaryWidth = mode.width();
        mPrimaryHeight = mode.height();
    }

    /**
     * <p>Generates GLFW's window and assigns it an id.</p>
     *
     * <p>This method should only be called on the main thread.</p>
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

    /**
     * <p>Throws a {@code NullPointerException} if the given object is null.</p>
     *
     * @param object to check.
     * @throws NullPointerException if object is null.
     */
    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Notified by {@code Window} whenever its size changes.</p>
     */
    public interface OnResizeListener
    {
        /**
         * <p>This method is called whenever the {@code Window}'s size changes.</p>
         *
         * @param width width.
         * @param height height.
         */
        void onResize(int width, int height);
    }
}