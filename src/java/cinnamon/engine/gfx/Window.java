package cinnamon.engine.gfx;

import cinnamon.engine.event.Gamepad;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.IntegratableInput;
import cinnamon.engine.event.IntegratableInput.GamepadConnectionCallback;
import cinnamon.engine.event.IntegratableInput.GamepadUpdateCallback;
import cinnamon.engine.event.IntegratableInput.MouseButtonCallback;
import cinnamon.engine.event.IntegratableInput.MouseScrollCallback;
import cinnamon.engine.utils.Size;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>This class wraps a GLFW window for OpenGL operations on a separate thread and integration with {@code Input}
 * for producing {@code InputEvents}. By default, a {@code Window} opens using the minimum allowed size of
 * {@link #MINIMUM_WIDTH} x {@link #MINIMUM_HEIGHT}, is decorated, not resizable, and with vsync enabled.</p>
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
 * reopened whereas a destroyed one has had its resources released and whose instance is no longer usable.</p>
 *
 * <p>In relation to GLFW's life cycle, instantiating a {@code Window} calls {@code GLFW.glfwInit()} and
 * destroying the last calls {@code GLFW.glfwTerminate()}. In order to ensure all resources are properly released,
 * GLFW's termination method is wrapped by {@link Window#terminate()}, which also allows all instances to complete
 * their life cycles without the need for direct references.</p>
 *
 * <p>Continuously calling {@code Window.pollEvents()} is required to maintain the {@code Window's} visibility and
 * ancillary operations. This method is the same as {@code GLFW.glfwPollEvents()} but with {@code InputEvent}
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
 *         while (isProcessing()) {
 *          // Process window and input events
 *          Window.pollEvents();
 *
 *          // Do work
 *          ...
 *         }
 *
 *         window.destroy();
 *     </code>
 * </pre>
 *
 * <b>Input devices</b>
 * <p>Each {@code Window} produces an {@code Input} for generating events from the keyboard, mouse, and gamepads as
 * well as allowing read-access to the devices' event histories. The input update rate is tied to the rate of calling
 * {@code Window.pollEvents()}. It should be noted that calling this method at a low rate can result in missed
 * gamepad data since the hardware can be interacted with at moments in-between polls.</p><br>
 *
 * <b>Multiple windows</b>
 * <p>Handling more than one window is done by instantiating the needed number and calling {@code Window.pollEvents()}
 * once per loop. While each instance's state can be controlled by their respective methods,
 * {@link Window#terminate()} will destroy all instances.</p>
 *
 * <p><b>note</b><i> An exception access violation has been observed to occasionally occur when using multiple
 * windows. This note will be updated as the problem is investigated.</i>
 * </p><br>
 *
 * <b>Concurrency</b>
 * <p>The constructor and most methods should only be called on the main thread. This is noted in the documentation
 * for those affected. All callbacks are notified on the main thread.</p>
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

    // Tracks all instantiated Windows for automatic GLFW termination
    private static final List<Window> mWindows = new ArrayList<>();

    // Window size listeners
    private final List<OnSizeChangeListener> mOnSizeChangeListeners = new ArrayList<>();

    // Pixel area listeners
    private final List<OnSizeChangeListener> mOnFramebufferOnSizeChangeListeners = new ArrayList<>();

    private final List<OnFocusChangeListener> mOnFocusChangeListeners = new ArrayList<>();

    private final GamepadUpdateCallback mGamepadUpdateCallback;

    private Thread mRenderThread;

    private final Canvas mCanvas;

    private final IntegratableInput mInput;

    // Window's handle in GLFW
    private final long mIdGLFW;

    // Class specific identifier based on instantiation order
    private final int mId;

    // Width in screen coords
    private int mPrimaryWidth;

    // Width in screen coords
    private int mPrimaryHeight;

    private final Object mFramebufferSizeLock = new Object();

    // Width in pixels
    private int mFramebufferWidth = 0;

    // Height in pixels
    private int mFramebufferHeight = 0;

    private String mWinTitle;

    private boolean mHasResized = false;

    private final double[] mMousePosX = new double[1];

    private final double[] mMousePosY = new double[1];

    /**
     * The following toggles reflect GLFW's corresponding window states and allow thread-safe state viewing without
     * the need for native calls (GLFW's methods).
     */

    private volatile boolean mVsync = true;

    private volatile boolean mDestroyed = false;

    private volatile boolean mFocused = true;

    private volatile boolean mMinimized = false;

    private volatile boolean mMaximized = false;

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

        mId = mWindows.size();
        mWindows.add(this);

        mCanvas = canvas;
        mWinTitle = title;

        // Must be read prior to creating Window for correct resolution
        readPrimaryDisplayResolution();

        mIdGLFW = createWindow();
        mInput = new IntegratableInput();

        createGamepads();

        mGamepadUpdateCallback = mInput.getGamepadUpdateCallback();
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
            GLFW.glfwMakeContextCurrent(mIdGLFW);
            GL.createCapabilities();

            mCanvas.initialize();
            render();
            mCanvas.terminate();

            GLFW.glfwMakeContextCurrent(MemoryUtil.NULL);
        });

        // Enable Input to produce events
        setInputCallbacks();

        GLFW.glfwShowWindow(mIdGLFW);
        mRenderThread.start();
    }

    /**
     * <p>Checks if the window is going to close.</p>
     *
     * @return true if closing.
     */
    public boolean isClosing()
    {
        return GLFW.glfwWindowShouldClose(mIdGLFW);
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

        GLFW.glfwHideWindow(mIdGLFW);
        GLFW.glfwSetWindowShouldClose(mIdGLFW, true);

        if (mRenderThread != null) {
            try {
                mRenderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * <p>Reads the button and axis states of all connected gamepads and produces representative input events. This
     * method does nothing if the {@code Window} does not have focus.</p>
     */
    public void updateGamepads()
    {
        if (!isFocused()) {

            for (final Connection connection : Gamepad.Connection.values()) {

                final Gamepad pad = mInput.getGamepad(connection);
                if (pad == null || pad.isMuted()) {
                    continue;
                }

                final ByteBuffer buttons = GLFW.glfwGetJoystickButtons(connection.toInt());
                final FloatBuffer axes = GLFW.glfwGetJoystickAxes(connection.toInt());

                mGamepadUpdateCallback.onButtonsUpdate(connection, buttons);
                mGamepadUpdateCallback.onAxesUpdate(connection, axes);
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
        GLFW.glfwGetWindowSize(mIdGLFW, width, null);
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
        GLFW.glfwGetWindowSize(mIdGLFW, null, height);
        return height[0];
    }

    /**
     * <p>Sets the width of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width.
     */
    public void setWidth(int width)
    {
        width = (width < MINIMUM_WIDTH) ? MINIMUM_WIDTH : width;
        GLFW.glfwSetWindowSize(mIdGLFW, width, getHeight());
    }

    /**
     * <p>Sets the height of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param height height.
     */
    public void setHeight(int height)
    {
        height = (height < MINIMUM_HEIGHT) ? MINIMUM_HEIGHT : height;
        GLFW.glfwSetWindowSize(mIdGLFW, getWidth(), height);
    }

    /**
     * <p>Sets the width and height of the drawable area in screen coordinates.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param width width.
     * @param height height.
     */
    public void setSize(int width, int height)
    {
        width = (width < MINIMUM_WIDTH) ? MINIMUM_WIDTH : width;
        height = (height < MINIMUM_HEIGHT) ? MINIMUM_HEIGHT : height;

        GLFW.glfwSetWindowSize(mIdGLFW, width, height);
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
        GLFW.glfwGetWindowSize(mIdGLFW, width, height);

        final int[] x = new int[1];
        final int[] y = new int[1];
        GLFW.glfwGetWindowPos(mIdGLFW, x, y);

        GLFW.glfwSetWindowMonitor(mIdGLFW, monitor, x[0], y[0], width[0], height[0], GLFW.GLFW_DONT_CARE);
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
        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_RESIZABLE) == GLFW.GLFW_TRUE;
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
        GLFW.glfwSetWindowAttrib(mIdGLFW, GLFW.GLFW_RESIZABLE, (resizable) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
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
        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_DECORATED) == GLFW.GLFW_TRUE;
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
        GLFW.glfwSetWindowAttrib(mIdGLFW, GLFW.GLFW_DECORATED, (decorated) ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
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

        GLFW.glfwSetWindowTitle(mIdGLFW, title);
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

        GLFW.glfwMakeContextCurrent(mIdGLFW);
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

        GLFW.glfwFocusWindow(mIdGLFW);
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
        GLFW.glfwIconifyWindow(mIdGLFW);
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
        GLFW.glfwMaximizeWindow(mIdGLFW);
    }

    /**
     * <p>Restores the video mode from before the window was minimized.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public void restore()
    {
        GLFW.glfwRestoreWindow(mIdGLFW);
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

        GLFW.glfwSetWindowIcon(mIdGLFW, buffer);
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
        return GLFW.glfwGetWindowAttrib(mIdGLFW, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE;
    }

    /**
     * <p>Gets the point of creation for the window's input events.</p>
     *
     * @return input creator.
     */
    public IntegratableInput getInput()
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
        GLFW.glfwGetWindowPos(mIdGLFW, x, null);
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
        GLFW.glfwGetWindowPos(mIdGLFW, null, y);
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
        GLFW.glfwGetWindowSize(mIdGLFW, width, height);

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

        GLFW.glfwSetWindowPos(mIdGLFW, x, y);
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
        GLFW.glfwGetWindowSize(mIdGLFW, width, height);

        // Compute main display's center
        final int x =  (mPrimaryWidth / 2) - (width[0] / 2);
        final int y = (mPrimaryHeight / 2) - (height[0] / 2);

        GLFW.glfwSetWindowPos(mIdGLFW, x, y);
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
     * <p>Gets the window's unique identifier denoting its place in the creation order of multiple windows.</p>
     *
     * <p>This id does not correspond to GLFW's window handle.</p>
     *
     * @return id.
     */
    public int getId()
    {
        return mId;
    }

    /**
     * <p>Adds an {@link OnSizeChangeListener} to be notified of changes to the window's screen size. These
     * dimensions are measured in screen coordinates. For dealing with pixel-based methods such as GL operations, see
     * {@link #addOnFramebufferSizeChangeListener(OnSizeChangeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void addOnSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNull(listener);

        mOnSizeChangeListeners.add(listener);
    }

    /**
     * <p>Removes an {@code OnSizeChangeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void removeOnSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNull(listener);

        mOnSizeChangeListeners.remove(listener);
    }

    /**
     * <p>Adds an {@link OnSizeChangeListener} to be notified of changes to the window's framebuffer size. These
     * dimensions are measured in pixels. For dealing with screen coordinate-based methods such as moving the window
     * on the display, see {@link #addOnSizeChangeListener(OnSizeChangeListener)}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void addOnFramebufferSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNull(listener);

        mOnFramebufferOnSizeChangeListeners.add(listener);
    }

    /**
     * <p>Removes an {@code OnSizeChangeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void removeOnFramebufferOnSizeChangeListener(OnSizeChangeListener listener)
    {
        checkNull(listener);

        mOnFramebufferOnSizeChangeListeners.remove(listener);
    }

    /**
     * <p>Adds an {@code OnFocusChangeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void addOnFocusChangeListener(OnFocusChangeListener listener)
    {
        checkNull(listener);

        mOnFocusChangeListeners.add(listener);
    }

    /**
     * <p>Removes an {@code OnFocusChangeListener}.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @param listener listener.
     * @throws NullPointerException if listener is null.
     */
    public void removeOnFocusChangeListener(OnFocusChangeListener listener)
    {
        checkNull(listener);

        mOnFocusChangeListeners.remove(listener);
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
        GLFW.glfwDestroyWindow(mIdGLFW);
        removeInputCallbacks();
        removeStateCallbacks();

        final boolean removed = mWindows.remove(this);
        assert (removed);

        // Release resources if destroying last window
        if (mWindows.isEmpty()) {
            GLFW.glfwTerminate();
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Sets the GLFW callbacks regarding the window's state such as whether it's maximized or has focus.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     */
    private void setStateCallbacks()
    {
        GLFW.glfwSetWindowIconifyCallback(mIdGLFW, (handle, minimized) ->
        {
            mMinimized = minimized;
        });
        GLFW.glfwSetWindowFocusCallback(mIdGLFW, (handle, focused) ->
        {
            mFocused = focused;

            if (focused) {
                setInputCallbacks();
            } else {
                removeInputCallbacks();
            }

            notifyFocusListeners(focused);
        });
        GLFW.glfwSetWindowMaximizeCallback(mIdGLFW, (handle, maximized) ->
        {
            mMaximized = maximized;
        });
        GLFW.glfwSetWindowCloseCallback(mIdGLFW, (handle) ->
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
        GLFW.glfwSetWindowIconifyCallback(mIdGLFW, null);
        GLFW.glfwSetWindowFocusCallback(mIdGLFW, null);
        GLFW.glfwSetWindowMaximizeCallback(mIdGLFW, null);
        GLFW.glfwSetWindowCloseCallback(mIdGLFW, null);
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
     *     <li>text input</li>
     *     <li>gamepad connection</li>
     * </ul>
     */
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

        // No need to keep setting for all windows
        if (mWindows.size() == 1) {

            final GamepadConnectionCallback connCallback = mInput.getGamepadConnectionCallback();
            GLFW.glfwSetJoystickCallback((joystick, event) ->
            {
                final String name = GLFW.glfwGetJoystickName(joystick);
                connCallback.onConnectionUpdate(joystick, event, name);
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
        GLFW.glfwSetMouseButtonCallback(mIdGLFW, null);
        GLFW.glfwSetScrollCallback(mIdGLFW, null);
        GLFW.glfwSetKeyCallback(mIdGLFW, null);

        // Only remove if no windows are available
        if (mWindows.size() == 1) {
            GLFW.glfwSetJoystickCallback(null);
        }
    }

    /**
     * <p>Loops continuously to render in the window until GLFW signals its close.</p>
     */
    private void render()
    {
        while (!GLFW.glfwWindowShouldClose(mIdGLFW)) {

            // Sync Canvas' size with framebuffer
            if (mHasResized) {
                synchronized (mFramebufferSizeLock) {
                    mCanvas.resizeTo(mFramebufferWidth, mFramebufferHeight);
                    mHasResized = false;
                }
            }

            mCanvas.consumeResourceRequests();
            mCanvas.draw();

            GLFW.glfwSwapBuffers(mIdGLFW);
        }
    }

    /**
     * <p>Checks each {@code Connection} for a present joystick and notifies the {@link IntegratableInput} of its
     * presence.</p>
     */
    private void createGamepads()
    {
        final GamepadConnectionCallback callback = mInput.getGamepadConnectionCallback();

        for (final Connection connection : Connection.values()) {
            final int joystick = connection.toInt();

            if (GLFW.glfwJoystickPresent(joystick)) {
                final String name = GLFW.glfwGetJoystickName(joystick);
                callback.onConnectionUpdate(joystick, GLFW.GLFW_CONNECTED, name);
            }
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
        GLFW.glfwGetFramebufferSize(mIdGLFW, frameWidth, frameHeight);

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
        GLFW.glfwSetWindowSizeCallback(mIdGLFW, (window, width, height) ->
        {
            for (final OnSizeChangeListener listener : mOnSizeChangeListeners) {
                listener.onSizeChange(width, height);
            }
        });

        GLFW.glfwSetFramebufferSizeCallback(mIdGLFW, (window, width, height) ->
        {
            synchronized (mFramebufferSizeLock) {
                mFramebufferWidth = width;
                mFramebufferHeight = height;
                mHasResized = true;
            }

            for (final OnSizeChangeListener callback : mOnFramebufferOnSizeChangeListeners) {
                callback.onSizeChange(width, height);
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

    private void notifyFocusListeners(boolean focus)
    {
        for (final OnFocusChangeListener listener : mOnFocusChangeListeners) {
            listener.onFocusChange(focus);
        }
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
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
    }

    /**
     * <p>Destroys all {@code Window}s and releases resources.</p>
     *
     * <p>This method is equivalent to calling {@code GLFW.glfwTerminate()} and should only be called on the
     * main thread.</p>
     */
    public static void terminate()
    {
        for (final Window window : mWindows) {
            window.destroy();
        }
    }

    /**
     * <p>Notified whenever the size changes.</p>
     */
    public interface OnSizeChangeListener
    {
        /**
         * <p>Called whenever the {@code Window}'s size changes.</p>
         *
         * @param width width.
         * @param height height.
         */
        void onSizeChange(int width, int height);
    }

    /**
     * <p>Notified when focus is gained or lost.</p>
     */
    public interface OnFocusChangeListener
    {
        /**
         * <p>Called whenever the {@code Window} gains or loses focus.</p>
         *
         * @param focus true if now has focus.
         */
        void onFocusChange(boolean focus);
    }
}