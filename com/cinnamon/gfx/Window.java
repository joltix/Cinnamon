package com.cinnamon.gfx;

import com.cinnamon.system.ControlMap;
import com.cinnamon.system.OnEndListener;
import com.cinnamon.utils.Event;
import com.cinnamon.utils.KeyEvent;
import com.cinnamon.utils.MouseEvent;
import com.cinnamon.utils.PooledQueue;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * <p>
 *     Wrapper class for the GLFW windowing library.
 * </p>
 *
 *
 */
public class Window
{
    // Callback for Window closing
    private OnEndListener mOnEndListener;

    // Generates Events from user input
    private Input mInput;

    // Window handle, title, and status
    private long mWinId;
    private String mWinTitle;
    private volatile boolean mClosed = false;

    // Desired dimensions
    private int mWidth;
    private int mHeight;

    // Primary monitor dimensions
    private int mPrimaryWidth;
    private int mPrimaryHeight;

    // Whether or not vsync is desired
    private boolean mVsync = false;

    /**
     * <p>Constructor for a demo Window.</p>
     *
     * @param width width.
     * @param height height.
     * @param title title bar text.
     * @throws IllegalStateException if the windowing toolkit failed to
     * load.
     */
    public Window(int width, int height, String title)
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

        // Create input Event processor
        mInput = new Input();
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
     * <p>Sets the Window's resolution. If either specified dimension is
     * invalid (i.e. less than 1 or greater than the primary monitor's full
     * resolution) then the Window's will go fullscreen.</p>
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
        GLFW.glfwShowWindow(mWinId);

        // Run main proc
        while (!GLFW.glfwWindowShouldClose(mWinId)) {
            GLFW.glfwPollEvents();
            mInput.process();
        }

        // Trigger shutdown callback
        if (mOnEndListener != null) {
            mOnEndListener.onEnd();
        }

        try {
            GLFW.glfwDestroyWindow(mWinId);
        } finally {
            GLFW.glfwTerminate();
        }

        // Signal isClosing()
        mClosed = true;
    }

    /**
     * <p>Attempts to close the Window.</p>
     */
    public void close()
    {
        GLFW.glfwSetWindowShouldClose(mWinId, true);
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
        return mClosed;
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
     * <p>Checks whether or not the frame rate is being limited to match the
     * monitor's refresh rate.</p>
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
     * <p>
     *     Facilitates a connection to the {@link Window}'s keyboard
     *     and mouse input.
     * </p>
     *
     * <p>
     *     Classes wanting to access {@link Event}s from the
     *     {@link Window} must call {@link #poll(ControlMap)}
     *     continuously to remain up-to-date on newly available Events.
     * </p>
     *
     */
    public final class Input
    {
        // Initial event buffer load
        private static final int BUFFER_SIZE = 100;

        // Sync lock when pushing input to Game thread
        private Object mLock = new Object();

        // Keyboard input buffers
        private Queue<KeyEvent> mConcurrentKeyboard
                = new ArrayBlockingQueue<KeyEvent>(BUFFER_SIZE);
        private Queue<KeyEvent> mKeyBuffer
                = new ArrayBlockingQueue<KeyEvent>(BUFFER_SIZE);

        // Mouse input buffers
        private Queue<MouseEvent> mConcurrentMouse
                = new ArrayBlockingQueue<MouseEvent>(BUFFER_SIZE);
        private Queue<MouseEvent> mMouseBuffer
                = new ArrayBlockingQueue<MouseEvent>(BUFFER_SIZE);

        // Mouse' x and y position
        private double[] mMouseX = new double[1];
        private double[] mMouseY = new double[1];

        // Event object pools
        private final PooledQueue<KeyEvent> mKeyPool = new PooledQueue<>();
        private final PooledQueue<MouseEvent> mMousePool = new PooledQueue<>();

        /**
         * <p>Constructor for an Input.</p>
         */
        private Input()
        {
            initKeyboardInput();
            initMouseInput();
        }

        /**
         * <p>Attaches a GLFW keyboard callback to handle KeyEvent creation.</p>
         */
        private void initKeyboardInput()
        {
            GLFW.glfwSetKeyCallback(mWinId, new GLFWKeyCallbackI()
            {
                @Override
                public void invoke(long window, int key, int scancode, int
                        action, int mods)
                {
                    if (action == GLFW.GLFW_REPEAT) {
                        return;
                    }

                    // Create KeyEvent and set aside for game thread polling
                    final KeyEvent event = createKeyEvent(key, action);
                    if (event != null) {
                        mKeyBuffer.add(event);
                    }
                }
            });
        }

        /**
         * <p>Attaches GLFW mouse input callbacks for button clicks and
         * scroll wheels to handle MouseEvent creation.</p>
         */
        private void initMouseInput()
        {
            // Use mouse location input from bottom left origin
            final GLFWMouseButtonCallbackI mouseCllbck;

            // Define MouseEvent generation
            mouseCllbck = new GLFWMouseButtonCallbackI()
            {
                @Override
                public void invoke(long window, int button, int action,
                                   int mods)
                {
                    // Don't react to anything but PRESS and RELEASE
                    if (action != GLFW.GLFW_PRESS
                            && action != GLFW.GLFW_RELEASE) {
                        return;
                    }

                    // Update mouse location
                    GLFW.glfwGetCursorPos(mWinId, mMouseX, mMouseY);
                    final float x = (float) mMouseX[0];
                    final float y = (float) mMouseY[0];

                    // Remap GLFW's actions
                    final Event.Action actionMap;
                    if (action == GLFW.GLFW_PRESS) {
                        actionMap = MouseEvent.Action.PRESS;
                    } else {
                        actionMap = MouseEvent.Action.RELEASE;
                    }

                    // Create MouseEvent from position and action
                    final MouseEvent event = createMouseEvent(button,
                            actionMap, x, y);

                    // Store mouse event
                    if (event != null) {
                        mMouseBuffer.add(event);
                    }
                }
            };

            // Attach mouse Event generation
            GLFW.glfwSetMouseButtonCallback(mWinId, mouseCllbck);

            // Hook into mouse scroll input
            GLFW.glfwSetScrollCallback(mWinId, new GLFWScrollCallbackI()
            {
                @Override
                public void invoke(long window, double xoffset, double yoffset)
                {
                    // Query mouse position on scroll
                    GLFW.glfwGetCursorPos(mWinId, mMouseX, mMouseY);
                    final float x = (float) mMouseX[0];
                    final float y = (float) mMouseY[0];

                    // Figure direction of wheel scroll
                    final Event.Action direction;
                    if (yoffset < 0.0f) {
                        direction = MouseEvent.Action.SCROLL_BACKWARD;
                    } else {
                        direction = MouseEvent.Action.SCROLL_FORWARD;
                    }

                    // Create MouseEvent and set aside for game thread
                    final MouseEvent event = createMouseEvent(MouseEvent
                            .BUTTON_MIDDLE, direction, x, y);

                    if (event != null) {
                        mMouseBuffer.add(event);
                    }
                }
            });
        }

        /**
         * <p>Creates a {@link KeyEvent} described by a given key and action.
         * </p>
         *
         * @param key key.
         * @param action action.
         * @return KeyEvent.
         */
        private KeyEvent createKeyEvent(int key, int action)
        {
            // Convert to KeyEvent key constant
            int newKey = KeyEvent.systemDigitsToKey(key);
            newKey = (newKey == -1) ? KeyEvent.systemPunctuationToKey(key) :
                    newKey;
            newKey = (newKey == -1) ? KeyEvent.systemAuxiliaryToKey(key) :
                    newKey;
            newKey = (newKey == -1) ? KeyEvent.systemFunctionToKey(key) :
                    newKey;
            newKey = (newKey == -1) ? KeyEvent.systemAlphabetToKey(key) :
                    newKey;

            // Ignore unsupported key
            if (newKey == -1) {
                return null;
            }

            // Convert to KeyEvent action
            final Event.Action newAction;
            switch (action) {
                case GLFW.GLFW_PRESS:
                    newAction = KeyEvent.Action.PRESS;
                    break;
                case GLFW.GLFW_RELEASE:
                    newAction = KeyEvent.Action.RELEASE;
                    break;
                default: throw new IllegalArgumentException("Unrecognized " +
                        "action: " + action);
            }

            // Create KeyEvent but reuse older KeyEvent if possible
            final KeyEvent event;
            if (mKeyPool.isEmpty()) {
                event = new KeyEvent(newKey, newAction);
            } else {
                // Reuse
                event = mKeyPool.poll();
                event.update(newKey, newAction);
            }

            return event;
        }

        /**
         * <p>Generates a {@link MouseEvent} based off of GLFW's constants.</p>
         *
         * @param button GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT, or
         *               GLFW_MOUSE_BUTTON_MIDDLE.
         * @param action GLFW_PRESS or GLFW_RELEASE.
         * @param x x.
         * @param y y.
         * @return MouseEvent representing the action.
         */
        private MouseEvent createMouseEvent(int button, Event.Action action,
                                            float x, float y)
        {
            final int buttonMap;

            // Remap GLFW's buttons to internal
            switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                    buttonMap = MouseEvent.BUTTON_LEFT; break;
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                    buttonMap = MouseEvent.BUTTON_RIGHT; break;
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE:
                    buttonMap = MouseEvent.BUTTON_MIDDLE; break;
                default: throw new IllegalArgumentException("Unrecognized " +
                        "mouse button:" +
                        " " + button);
            }

            // Create MouseEvent but reuse older MouseEvent if available
            final MouseEvent event;
            if (mMousePool.isEmpty()) {
                event = new MouseEvent(buttonMap, action, x, y);
            } else {
                // Reuse
                event = mMousePool.poll();
                event.update(buttonMap, action, x, y);
            }

            return event;
        }

        /**
         * <p>Makes input {@link Event}s available for other threads to process.
         * After this method completes, the next call to
         * {@link #poll(ControlMap)} will transfer available {@link Event}s to
         * the game update thread. However, only one {@link KeyEvent} and
         * {@link MouseEvent} will be sent.</p>
         *
         * <p>This method should only be called from the main thread.</p>
         */
        protected final void process()
        {
            // No need to synchronize with game thread if no events available
            final MouseEvent mouseEvent = mMouseBuffer.poll();
            final KeyEvent keyEvent = mKeyBuffer.poll();
            if (mouseEvent == null && keyEvent == null) {
                return;
            }

            // Send most recent KeyEvent and MouseEvent
            synchronized (mLock) {
                if (mouseEvent != null) {
                    mConcurrentMouse.add(mouseEvent);
                }
                if (keyEvent != null) {
                    mConcurrentKeyboard.add(keyEvent);
                }
            }
        }

        /**
         * <p>Stores the next available {@link KeyEvent} and
         * {@link MouseEvent} in a given {@link ControlMap}.</p>
         *
         * <p>This method is thread-safe.</p>
         *
         * @param map ControlMap to set with new {@link Event}s.
         */
        public final void poll(ControlMap map)
        {
            synchronized (mLock) {

                // Transfer new Events over to game thread
                map.setEvent(mConcurrentKeyboard.poll());
                map.setEvent(mConcurrentMouse.poll());
            }
        }
    }
}
