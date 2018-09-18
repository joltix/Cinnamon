package cinnamon.engine.gfx;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a GLFW monitor. Monitor objects are only valid until disconnected. Thus, most of a disconnected
 * {@code Monitor}'s methods will throw an exception when called. As a GLFW wrapper, Monitor.{@link #terminate()}
 * must be called when this class is no longer in use.
 *
 * <p>Monitors are viewports within a virtual screen space defined by the smallest bounding box containing all
 * monitors. As such, each {@code Monitor} has an (x,y) position along with a width and height.</p>
 *
 * <br><p><i>Warning: GLFW should not be used while this class is in use. This class makes use of {@code GLFW} to
 * maintain its state and so some of {@code GLFW}'s methods, such as {@code GLFW.glfwTerminate()} interferes with this
 * class' expectations.</i></p>
 *
 * <h3>Concurrency</h3>
 * <p>Aside from {@link #isConnected()}, all methods must be called on the main thread. This is noted in the
 * documentation for those affected.</p>
 */
public final class Monitor
{
    // Primary monitor's index into monitors list
    private static final int PRIMARY = 0;

    // All connected monitors
    private static final List<Monitor> mMonitors = new ArrayList<>();

    // True if this class' static methods have been used and Monitor.tearDown() has not yet been called
    private static boolean mInUse = false;

    // GLFW monitor handle
    private final long mHandle;
                                                                                                                                                                                                                                                                                                                                   
    // True = usable
    private boolean mConnected;

    /**
     * Constructs a {@code Monitor} whose handle is {@code 0}.
     *
     * <p>This constructor is meant for testing purposes.</p>
     */
    Monitor()
    {
        mHandle = 0L;
    }

    /**
     * Constructs a {@code Monitor} with a specified GLFW monitor handle.
     *
     * @param handle GLFW monitor.
     */
    private Monitor(long handle)
    {
        mHandle = handle;
    }

    /**
     * Gets the width.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return width.
     * @throws IllegalStateException if this monitor is no longer connected or a {@code GLFW} error occurred.
     */
    public int getWidth()
    {
        checkMonitorIsConnected();

        final GLFWVidMode mode = GLFW.glfwGetVideoMode(mHandle);
        checkVideoModeExists(mode);

        return mode.width();
    }

    /**
     * Gets the height.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return height.
     * @throws IllegalStateException if this monitor is no longer connected or a {@code GLFW} error occurred.
     */
    public int getHeight()
    {
        checkMonitorIsConnected();

        final GLFWVidMode mode = GLFW.glfwGetVideoMode(mHandle);
        checkVideoModeExists(mode);

        return mode.height();
    }

    /**
     * Gets the x position in virtual screen space.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return x.
     * @throws IllegalStateException if this monitor is no longer connected.
     */
    public int getX()
    {
        checkMonitorIsConnected();

        final int[] x = new int[1];
        GLFW.glfwGetMonitorPos(mHandle, x, null);

        return x[0];
    }

    /**
     * Gets the y position in virtual screen space.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return y.
     * @throws IllegalStateException if this monitor is no longer connected.
     */
    public int getY()
    {
        checkMonitorIsConnected();

        final int[] y = new int[1];
        GLFW.glfwGetMonitorPos(mHandle, null, y);

        return y[0];
    }

    /**
     * Gets the horizontal and vertical content scale factors.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return content scale factors.
     * @throws IllegalStateException if this monitor is no longer connected.
     */
    public float[] getContentScale()
    {
        checkMonitorIsConnected();

        final float[] scaleX = new float[2];
        final float[] scaleY = new float[1];

        GLFW.glfwGetMonitorContentScale(mHandle, scaleX, scaleY);
        scaleX[1] = scaleY[0];

        return scaleX;
    }

    /**
     * Returns {@code true} if this monitor is connected and therefore still valid.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return true if connected.
     */
    public boolean isConnected()
    {
        return mConnected;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    void connect()
    {
        mConnected =  true;
    }

    void disconnect()
    {
        mConnected = false;
    }

    long getHandle()
    {
        return mHandle;
    }

    private void checkVideoModeExists(GLFWVidMode mode)
    {
        if (mode == null) {
            throw new IllegalStateException("Unable to retrieve video mode");
        }
    }

    private void checkMonitorIsConnected()
    {
        if (!mConnected) {
            throw new IllegalStateException("Monitor is no longer connected");
        }
    }

    /**
     * Gets the currently connected primary monitor.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return primary monitor.
     */
    public static Monitor getPrimaryMonitor()
    {
        Monitor.ensureResourcesAreReady();

        return (mMonitors.isEmpty()) ? null : mMonitors.get(0);
    }

    /**
     * Gets all currently connected monitors. The first entry is the primary monitor.
     *
     * <p>This method should only be called on the main thread.</p>
     *
     * @return connected monitors.
     */
    public static Monitor[] getConnectedMonitors()
    {
        Monitor.ensureResourcesAreReady();

        final Monitor[] monitors = new Monitor[mMonitors.size()];
        mMonitors.toArray(monitors);

        return monitors;
    }

    /**
     * Disconnects all {@code Monitor}s.
     *
     * <p>If no other {@code GLFW} wrapping classes are in use, {@code GLFW} is terminated.</p>
     *
     * <p>This method should only be called on the main thread.</p>
     */
    public static void terminate()
    {
        if (Monitor.isInUse()) {
            Monitor.tearDown();
        }

        if (!Window.isInUse()) {
            GLFW.glfwTerminate();
        }
    }

    static boolean isInUse()
    {
        return mInUse;
    }

    /**
     * Disconnects all {@code Monitor}s and makes them unavailable through getters.
     */
    private static void tearDown()
    {
        GLFW.glfwSetMonitorCallback(null);

        mMonitors.forEach(Monitor::disconnect);
        mMonitors.clear();

        mInUse = false;
    }

    private static Monitor findMonitorWithGLFWHandle(long handle)
    {
        for (final Monitor monitor : mMonitors) {
            if (monitor.getHandle() == handle) {
                return monitor;
            }
        }

        return null;
    }

    private static void ensureResourcesAreReady()
    {
        if (!mInUse) {
            Window.ensureGLFWIsInitialized();
            Monitor.setMonitorCallback();
            mInUse = true;
        }

        // Assume empty list means not yet refreshed
        if (mMonitors.isEmpty()) {
            Monitor.refreshConnectedMonitors();
        }
    }

    private static void setMonitorCallback()
    {
        // Track available monitors
        GLFW.glfwSetMonitorCallback((handle, event) ->
        {
            if (event == GLFW.GLFW_DISCONNECTED) {
                final Monitor toRemove = Monitor.findMonitorWithGLFWHandle(handle);

                assert (toRemove != null);

                // Remove from availability
                toRemove.disconnect();
                mMonitors.remove(toRemove);

                if (!mMonitors.isEmpty()) {
                    ensurePrimaryMonitorIsListedFirst();
                }

            } else {
                assert (event == GLFW.GLFW_CONNECTED);

                final Monitor monitor = new Monitor(handle);
                monitor.connect();

                mMonitors.add(monitor);

                if (mMonitors.size() > 1) {
                    ensurePrimaryMonitorIsListedFirst();
                }
            }
        });
    }

    private static void refreshConnectedMonitors()
    {
        final PointerBuffer buffer = GLFW.glfwGetMonitors();

        if (buffer != null) {
            // Create Monitor objects
            for (int i = 0, limit = buffer.limit(); i < limit; i++) {

                final Monitor monitor = new Monitor(buffer.get(i));
                monitor.connect();

                mMonitors.add(monitor);
            }
        }
    }

    private static void ensurePrimaryMonitorIsListedFirst()
    {
        long handle = GLFW.glfwGetPrimaryMonitor();

        // First assume null primary is due to an error; try reading another way
        if (handle == MemoryUtil.NULL) {

            final PointerBuffer monitorHandles = GLFW.glfwGetMonitors();
            if (monitorHandles != null) {

                assert (monitorHandles.limit() > 0);

                handle = monitorHandles.get(0);
            } else {
                // If there are no monitors found at all, assume notification is
                // just delayed and each monitor subsequently hits the callback
                // as a disconnection
                return;
            }
        }

        // Make sure primary is first in list
        if (mMonitors.get(PRIMARY).getHandle() != handle) {
            final Monitor primaryMonitor = Monitor.findMonitorWithGLFWHandle(handle);

            assert (primaryMonitor != null);

            mMonitors.remove(primaryMonitor);
            mMonitors.add(PRIMARY, primaryMonitor);
        }
    }
}
