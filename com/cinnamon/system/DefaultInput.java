package com.cinnamon.system;

import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.PooledQueue;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * <p>
 *     Default implementation for {@link Window.Input} using GLFW callbacks to gather user input and generate
 *     {@link InputEvent}s.
 * </p>
 */
public final class DefaultInput extends Window.Input
{
    // Initial event buffer load
    private static final int BUFFER_SIZE = 100;

    // Keyboard input buffers
    private Queue<KeyEvent> mKeyBuffer = new ArrayBlockingQueue<KeyEvent>(BUFFER_SIZE);

    // Mouse input buffers
    private Queue<MouseEvent> mMouseBuffer = new ArrayBlockingQueue<MouseEvent>(BUFFER_SIZE);

    // Mouse' x and y position
    private double[] mMouseX = new double[1];
    private double[] mMouseY = new double[1];

    // Event object pools
    private final PooledQueue<KeyEvent> mKeyPool = new PooledQueue<>();
    private final PooledQueue<MouseEvent> mMousePool = new PooledQueue<>();

    /**
     * <p>Constructor for a DefaultInput.</p>
     *
     * @param window Window to bind to.
     */
    public DefaultInput(Window window)
    {
        super(window);
    }

    /**
     * <p>Creates a {@link KeyEvent} described by a given key and action.
     * </p>
     *
     * @param key key.
     * @param action action.
     * @return KeyEvent.
     */
    private KeyEvent createKeyEvent(KeyEvent.Key key, InputEvent.Action action)
    {
        // Create KeyEvent but reuse older KeyEvent if possible
        final KeyEvent event;
        if (mKeyPool.isEmpty()) {
            event = new KeyEvent(key, action);
        } else {
            // Reuse KeyEvent if available
            event = mKeyPool.poll();
            event.update(key, action);
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
    private MouseEvent createMouseEvent(MouseEvent.Button button, InputEvent.Action action, float x, float y)
    {
        // Create brand new MouseEvent if necessary
        final MouseEvent event;
        if (mMousePool.isEmpty()) {
            event = new MouseEvent(button, action, x, y);
        } else {
            // Reuse a MouseEvent
            event = mMousePool.poll();
            event.update(button, action, x, y);
        }

        return event;
    }

    /**
     * <p>Stores the next available {@link KeyEvent} and
     * {@link MouseEvent} in a given {@link ControlMap}.</p>
     *
     * <p>This method is thread-safe.</p>
     *
     * @param controls ControlMap for connecting {@link InputEvent}s to user controls.
     * @param hub DefaultEventHub for exposing Events to handlers.
     */
    @Override
    public final void poll(ControlMap controls, EventHub hub)
    {
        // Pull most recent KeyEvent
        if (!mKeyBuffer.isEmpty()) {
            final KeyEvent keyEvent = mKeyBuffer.poll();
            controls.addEvent(keyEvent);
            hub.add(keyEvent);
        }

        // Add more recent MouseEvent to Game systems
        if (!mMouseBuffer.isEmpty()) {
            final MouseEvent mouseEvent = mMouseBuffer.poll();
            controls.addEvent(mouseEvent);
            hub.add(mouseEvent);
        }
    }

    @Override
    public void pollMouse(Point2F position)
    {
        GLFW.glfwGetCursorPos(getId(), mMouseX, mMouseY);
        position.set((float) mMouseX[0], (float) mMouseY[0]);
    }

    @Override
    void bind()
    {
        final long windowId = this.getId();

        // Attach input hooks to GLFW's window
        GLFW.glfwSetKeyCallback(windowId, new KeyboardHook());
        GLFW.glfwSetMouseButtonCallback(windowId, new MouseButtonHook());
        GLFW.glfwSetScrollCallback(windowId, new MouseScrollHook());
    }

    @Override
    void unbind()
    {
        final long windowId = this.getId();

        // Detach input hooks
        GLFW.glfwSetKeyCallback(windowId, null);
        GLFW.glfwSetMouseButtonCallback(windowId, null);
        GLFW.glfwSetScrollCallback(windowId, null);
    }

    /**
     * <p>Callback for generating {@link KeyEvent}s from key actions reported by GLFW.</p>
     */
    private class KeyboardHook extends GLFWKeyCallback
    {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods)
        {
            // Ignore REPEATs
            if (action == GLFW.GLFW_REPEAT) {
                return;
            }

            // Translate GLFW key to KeyEvent.Key
            KeyEvent.Key keyMapped = KeyEvent.systemAlphabetToKey(key);
            keyMapped = (keyMapped == null) ? KeyEvent.systemAuxiliaryToKey(key) : keyMapped;
            keyMapped = (keyMapped == null) ? KeyEvent.systemDigitsToKey(key) : keyMapped;
            keyMapped = (keyMapped == null) ? KeyEvent.systemFunctionToKey(key) : keyMapped;
            keyMapped = (keyMapped == null) ? KeyEvent.systemPunctuationToKey(key) : keyMapped;

            // Translate GLFW action to InputEvent.Action
            final InputEvent.Action actionMapped = InputEvent.systemActionToAction(action);

            if (keyMapped == null) {
                throw new IllegalStateException("Unsupported keyboard key: " + key);
            }

            // Create KeyEvent and set aside for game thread polling
            final KeyEvent event = createKeyEvent(keyMapped, actionMapped);
            if (event != null) {
                mKeyBuffer.add(event);
            }
        }
    }

    /**
     * <p>Callback for generating {@link MouseEvent}s from button actions reported by GLFW.</p>
     */
    private class MouseButtonHook extends GLFWMouseButtonCallback
    {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            // Ignore GLFW's REPEAT events
            if (action == GLFW.GLFW_REPEAT) {
                return;
            }

            // Update mouse location
            GLFW.glfwGetCursorPos(getId(), mMouseX, mMouseY);
            final float x = (float) mMouseX[0];
            final float y = (float) mMouseY[0];

            // Remap GLFW button constant to MouseEvent.Button
            final MouseEvent.Button buttonMapped = MouseEvent.systemButtonsToButton(button);

            // Remap GLFW action constant to InputEvent.Action
            final InputEvent.Action actionMapped = InputEvent.systemActionToAction(action);

            // Create MouseEvent to be stored for later
            final MouseEvent event = createMouseEvent(buttonMapped, actionMapped, x, y);

            // Store MouseEvent for later
            mMouseBuffer.add(event);
        }
    }

    /**
     * <p>
     *     Callback for generating {@link MouseEvent}s from scrolling actions reported by GLFW.
     * </p>
     */
    private class MouseScrollHook extends GLFWScrollCallback
    {
        @Override
        public void invoke(long window, double xoffset, double yoffset) {
            // Query mouse position on scroll
            GLFW.glfwGetCursorPos(getId(), mMouseX, mMouseY);
            final float x = (float) mMouseX[0];
            final float y = (float) mMouseY[0];

            // Decide direction of wheel scroll
            final InputEvent.Action direction = MouseEvent.systemActionToAction(yoffset);

            // Create MouseEvent and set aside for game thread
            final MouseEvent event = createMouseEvent(MouseEvent.Button.MIDDLE, direction, x, y);

            // Store MouseEvent for later
            mMouseBuffer.add(event);
        }
    }
}