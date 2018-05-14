package cinnamon.engine.event;

import cinnamon.engine.utils.*;
import cinnamon.engine.utils.IntMap.IntWrapper;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>An event-based representation of the user's gamepad. Updating the gamepad's press-release and motion states
 * requires updating the table of events passed to the {@code Gamepad}'s {@link Gamepad.State} during instantiation.</p>
 *
 * <p>Methods in this class that take either a {@link ButtonWrapper} or {@link AxisWrapper} are expected to be given
 * the appropriate {@code Enum} expected by the {@link PadProfile} that configured the {@code Gamepad} instance.
 * Providing these methods with a different wrapper implementation will cause an {@code Exception}.</p>
 *
 * <h3>Connection</h3>
 * <p>{@code Gamepad}s are given an unchangeable {@link Connection} upon creation which identify the owning
 * {@literal "player"}. There may be many {@code Gamepad} instances with the same {@code Connection}.</p>
 *
 * <h3>Dead Zones</h3>
 * <p>Each {@link AxisWrapper} constant is allowed a circular area which suppresses motion. This value is
 * defined as a radius radiating from the constant's resting offset and is referred to as the sensor's
 * {@literal "dead zone"}. Although these dead zones can be set for each axis-based constant, this class does not
 * enforce the motion suppression and is at the mercy of classes working directly with produced motion. By default,
 * all axes have a dead zone radius of 0.</p>
 */
public final class Gamepad implements EventSilenceable
{
    /**
     * <p>Gamepad identifying constants.</p>
     */
    public enum Connection implements IntWrapper
    {
        /**
         * <p>Gamepad 1.</p>
         */
        PAD_1(GLFW.GLFW_JOYSTICK_1),

        /**
         * <p>Gamepad 2.</p>
         */
        PAD_2(GLFW.GLFW_JOYSTICK_2),

        /**
         * <p>Gamepad 3.</p>
         */
        PAD_3(GLFW.GLFW_JOYSTICK_3),

        /**
         * <p>Gamepad 4.</p>
         */
        PAD_4(GLFW.GLFW_JOYSTICK_4);

        /**
         * <p>Number of possible gamepads.</p>
         */
        public static final int COUNT = Connection.values().length;

        // Mapping between Connections and lower level constants
        private static final IntMap<Connection> MAPPING = new SparseEnumIntMap<>(Connection.class);

        // Lower level constant
        private int mConstant;

        Connection(int constant)
        {
            mConstant = constant;
        }

        @Override
        public int toInt()
        {
            return mConstant;
        }

        /**
         * <p>Gets the {@code Connection} equivalent of a lower level constant.</p>
         *
         * @param constant lower level constant.
         * @return connection or null if unrecognized.
         */
        public static Connection from(int constant)
        {
            return MAPPING.get(constant);
        }
    }

    /**
     * <p>Gamepad button constants.</p>
     */
    public enum Button implements IntWrapper
    {
        /*
        Although current constants are merely indices, they are explicitly
        provided in case the lower level input source is changed
         */
        BUTTON_0(0),
        BUTTON_1(1),
        BUTTON_2(2),
        BUTTON_3(3),
        BUTTON_4(4),
        BUTTON_5(5),
        BUTTON_6(6),
        BUTTON_7(7),
        BUTTON_8(8),
        BUTTON_9(9),
        BUTTON_10(10),
        BUTTON_11(11),
        BUTTON_12(12),
        BUTTON_13(13),
        BUTTON_14(14),
        BUTTON_15(15),
        BUTTON_16(16),
        BUTTON_17(17);

        /**
         * <p>Number of buttons.</p>
         */
        public static final int COUNT = Button.values().length;

        // Mapping between buttons and lower level constants
        private static final Button[] MAPPING = Button.values();

        // Lower level constant
        private final int mConstant;

        Button(int constant)
        {
            mConstant = constant;
        }

        @Override
        public int toInt()
        {
            return mConstant;
        }

        /**
         * <p>Gets the {@code Button} equivalent of a lower level constant.</p>
         *
         * @param constant lower level constant.
         * @return button or null if unrecognized.
         */
        public static Button from(int constant)
        {
            if (constant < 0 || constant >= MAPPING.length) {
                return null;
            }

            return MAPPING[constant];
        }
    }

    /**
     * <p>Gamepad axis constants.</p>
     */
    public enum Axis implements IntWrapper
    {
        /*
        Although current constants are merely indices, they are explicitly
        provided in case the lower level input source is changed
        */
        AXIS_0(0),
        AXIS_1(1),
        AXIS_2(2),
        AXIS_3(3),
        AXIS_4(4),
        AXIS_5(5);

        /**
         * <p>Number of axes.</p>
         */
        public static final int COUNT = Axis.values().length;

        // Mapping between axes and lower level constants
        private static final Axis[] MAPPING = Axis.values();

        // Lower level constant
        private int mConstant;

        Axis(int constant)
        {
            mConstant = constant;
        }

        @Override
        public int toInt()
        {
            return mConstant;
        }

        /**
         * <p>Gets the {@code Axis} equivalent of a lower level constant.</p>
         *
         * @param constant lower level constant.
         * @return axis or null if unrecognized.
         */
        public static Axis from(int constant)
        {
            if (constant < 0 || constant >= MAPPING.length) {
                return null;
            }

            return MAPPING[constant];
        }
    }

    // Initial dead zone
    private static final double DEFAULT_DEAD_ZONE_RADIUS = 0d;

    // Event histories and connection status
    private final State mState;

    // Expected configuration
    private final PadProfile mProfile;

    // Gamepad identifier
    private final Connection mConnection;

    // Resting values per axis
    private final Map<Axis, Float> mResting;

    // Dead zone radius is stored only with each wrapper's vertical Axis
    private final Map<Axis, Double> mDeadZones;

    // True if events should be ignored
    private boolean mMuted = false;

    /**
     * <p>Constructs a {@code Gamepad}.</p>
     *
     * @param connection connection.
     * @param profile controller specific configuration.
     * @param state state.
     * @throws NullPointerException if connection, profile, or state is null.
     */
    public Gamepad(Connection connection, PadProfile profile, State state)
    {
        checkNull(connection);
        checkNull(profile);
        checkNull(state);

        mConnection = connection;
        mProfile = profile;
        mState = state;

        mResting = mProfile.getRestingAxisValues();

        // Initialize dead zones
        mDeadZones = new EnumMap<>(Axis.class);
        for (final Axis axis : Axis.values()) {
            mDeadZones.put(axis, DEFAULT_DEAD_ZONE_RADIUS);
        }
    }

    /**
     * <p>Checks if the button is pressed.</p>
     *
     * @param button button.
     * @return true if pressed.
     * @throws NullPointerException if button is null.
     * @throws IllegalArgumentException if button's class is not the same as that used by the profile.
     */
    public boolean isPressed(ButtonWrapper button)
    {
        checkNull(button);

        // Check if button is expected
        if (button.getClass() != mProfile.getButtonClass()) {

            final String format = "Constant class does not match profile's button class, expected: %s, actual: %s";
            final String expected = mProfile.getButtonClass().getSimpleName();
            final String actual = button.getClass().getSimpleName();

            throw new IllegalArgumentException(String.format(format, expected, actual));
        }

        return PressChecker.isPressed(button.button(), mState.getPressHistory(), mState.getReleaseHistory());
    }

    /**
     * <p>Gets the horizontal and vertical motion offsets.</p>
     *
     * @param axis axis.
     * @return motion offsets.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile.
     */
    public Point getMotion(AxisWrapper axis)
    {
        checkNull(axis);
        checkAxisClass(axis);

        final Axis vertical = axis.vertical();
        final PadEvent event = mState.mMotions.get(0, vertical.ordinal());
        final Point motion = new Point();

        if (event != null) {
            motion.setX(event.getHorizontal());
            motion.setY(event.getVertical());

        } else {
            // Use resting values when no event
            final Axis horizontal = axis.horizontal();
            motion.setX((horizontal == null) ? 0f : mResting.get(horizontal));
            motion.setY(mResting.get(vertical));
        }

        return motion;
    }

    /**
     * <p>Checks if the axis' offset position is within its dead zone.</p>
     *
     * @param axis axis.
     * @return true if within dead zone.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile.
     */
    public boolean isInsideDeadZone(AxisWrapper axis)
    {
        checkNull(axis);
        checkAxisClass(axis);

        final int vertical = axis.vertical().ordinal();
        final Boolean inside = mState.getDeadZoneHistory().get(0, vertical);

        // Explicit unboxing necessary to avoid NPE
        return inside != null && inside.booleanValue();
    }

    /**
     * <p>Checks if the given offset position is inside an axis' dead zone.</p>
     *
     * @param axis axis.
     * @param x x.
     * @param y y.
     * @return true if the axis' position is within the dead zone.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile.
     */
    public boolean isInsideDeadZone(AxisWrapper axis, float x, float y)
    {
        checkNull(axis);
        checkAxisClass(axis);

        final double radius = mDeadZones.get(axis.vertical());

        return (x * x) + (y * y) <= radius * radius;
    }

    /**
     * <p>Gets an axis' dead zone radius.</p>
     *
     * @param axis axis.
     * @return dead zone radius.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile.
     */
    public double getDeadZone(AxisWrapper axis)
    {
        checkNull(axis);
        checkAxisClass(axis);

        return mDeadZones.get(axis.vertical());
    }

    /**
     * <p>Sets a dead zone radius.</p>
     *
     * @param axis axis.
     * @param radius dead zone radius.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile, radius is
     * {@literal <} 0, or radius is {@literal >} 1.
     */
    public void setDeadZone(AxisWrapper axis, double radius)
    {
        checkNull(axis);
        checkAxisClass(axis);

        if (radius < 0d) {
            final String format = "Dead zone radius must be >= 0 and <= 1, actual: %f";
            throw new IllegalArgumentException(String.format(format, radius));
        }

        mDeadZones.put(axis.vertical(), radius);
    }

    /**
     * <p>Gets the profile describing the the gamepad's configuration.</p>
     *
     * @return profile.
     */
    public PadProfile getProfile()
    {
        return mProfile;
    }

    /**
     * <p>Gets the gamepad's identifier.</p>
     *
     * @return connection.
     */
    public Connection getConnection()
    {
        return mConnection;
    }

    /**
     * <p>Checks if the gamepad is connected.</p>
     *
     * @return true if connected.
     */
    public boolean isConnected()
    {
        return mState.isConnected();
    }

    @Override
    public boolean isMuted()
    {
        return mMuted;
    }

    @Override
    public void mute()
    {
        mMuted = true;
    }

    @Override
    public void unmute()
    {
        mMuted = false;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Throws an {@code IllegalArgumentException} if the given wrapper's class name does not match that which the
     * profile expects.</p>
     *
     * @param axis axis.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis class does not match the profile.
     */
    private void checkAxisClass(AxisWrapper axis)
    {
        if (axis.getClass() != mProfile.getAxisClass()) {

            final String format = "Constant class does not match profile's axis class, expected: %s, actual: %s";
            final String expected = mProfile.getAxisClass().getSimpleName();
            final String actual = axis.getClass().getSimpleName();

            throw new IllegalArgumentException(String.format(format, expected, actual));
        }
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>{@code Gamepad.State} allows changing the {@code Gamepad}'s connection status and updating its press,
     * release, and motion events.</p>
     *
     * <p>{@code Gamepad.State}s are constructed with a guided builder.</p>
     * <pre>
     *     <code>
     *         Gamepad.State state = Gamepad.State.builder()
     *             .pressHistory(presses)
     *             .releaseHistory(releases)
     *             .motionHistory(motions)
     *             .deadZoneHistory(zones)
     *             .build();
     *     </code>
     * </pre>
     */
    public static class State
    {
        // Press event history
        private final FixedQueueArray<PadEvent> mPresses;

        // Release event history
        private final FixedQueueArray<PadEvent> mReleases;

        // Axis motion event history
        private final FixedQueueArray<PadEvent> mMotions;

        // Dead zone presence history
        private final FixedQueueArray<Boolean> mZones;

        // True if gamepad state is still valid
        private boolean mConnected = false;

        private State(FixedQueueArray<PadEvent> pressHistory, FixedQueueArray<PadEvent> releaseHistory,
                      FixedQueueArray<PadEvent> axisHistory, FixedQueueArray<Boolean> zoneHistory)
        {
            mPresses = pressHistory;
            mReleases = releaseHistory;
            mMotions = axisHistory;
            mZones = zoneHistory;
        }

        /**
         * <p>Gets the editable history for press events.</p>
         *
         * @return editable press history.
         */
        public FixedQueueArray<PadEvent> getPressHistory()
        {
            return mPresses;
        }

        /**
         * <p>Gets the editable history for release events.</p>
         *
         * @return editable release history.
         */
        public FixedQueueArray<PadEvent> getReleaseHistory()
        {
            return mReleases;
        }

        /**
         * <p>Gets the editable history for motion events.</p>
         *
         * @return editable motion history.
         */
        public FixedQueueArray<PadEvent> getMotionHistory()
        {
            return mMotions;
        }

        /**
         * <p>Gets the editable history for dead zone presence.</p>
         *
         * @return editable dead zone history.
         */
        public FixedQueueArray<Boolean> getDeadZoneHistory()
        {
            return mZones;
        }

        /**
         * <p>Checks if the {@code Gamepad} is connected.</p>
         *
         * @return true if connected.
         */
        public boolean isConnected()
        {
            return mConnected;
        }

        /**
         * <p>Sets the connection status.</p>
         *
         * @param connected true if connected.
         */
        public void setConnected(boolean connected)
        {
            mConnected = connected;
        }

        /**
         * <p>Begins the process to build a {@code Gamepad.State}.</p>
         *
         * @return first build step.
         */
        public static PressHistoryStep builder()
        {
            return new StepBuilder();
        }

        /**
         * <p>Guides the creation of a {@code Gamepad.State}.</p>
         */
        private static class StepBuilder implements PressHistoryStep, ReleaseHistoryStep, MotionHistoryStep,
                DeadZoneHistoryStep, BuildStep
        {
            private FixedQueueArray<PadEvent> mPressHistory;
            private FixedQueueArray<PadEvent> mReleaseHistory;
            private FixedQueueArray<PadEvent> mAxisHistory;
            private FixedQueueArray<Boolean> mDeadZoneHistory;

            @Override
            public ReleaseHistoryStep pressHistory(FixedQueueArray<PadEvent> history)
            {
                checkNull(history);

                mPressHistory = history;
                return this;
            }

            @Override
            public MotionHistoryStep releaseHistory(FixedQueueArray<PadEvent> history)
            {
                checkNull(history);

                mReleaseHistory = history;
                return this;
            }

            @Override
            public DeadZoneHistoryStep motionHistory(FixedQueueArray<PadEvent> history)
            {
                checkNull(history);

                mAxisHistory = history;
                return this;
            }

            @Override
            public BuildStep deadZoneHistory(FixedQueueArray<Boolean> history)
            {
                checkNull(history);

                mDeadZoneHistory = history;
                return this;
            }

            @Override
            public State build()
            {
                return new State(mPressHistory, mReleaseHistory, mAxisHistory, mDeadZoneHistory);
            }
        }

        public interface PressHistoryStep
        {
            /**
             * <p>Sets the gamepad's button press event history.</p>
             *
             * @param history press history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            ReleaseHistoryStep pressHistory(FixedQueueArray<PadEvent> history);
        }

        public interface ReleaseHistoryStep
        {
            /**
             * <p>Sets the gamepad's button release event history.</p>
             *
             * @param history release history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            MotionHistoryStep releaseHistory(FixedQueueArray<PadEvent> history);
        }

        public interface MotionHistoryStep
        {
            /**
             * <p>Sets the gamepad's axis motion history.</p>
             *
             * @param history motion history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            DeadZoneHistoryStep motionHistory(FixedQueueArray<PadEvent> history);
        }

        public interface DeadZoneHistoryStep
        {
            /**
             * <p>Sets the gamepad's dead zone history.</p>
             *
             * @param history dead zone history.
             * @return next step.
             * @throws NullPointerException if history is null.
             */
            BuildStep deadZoneHistory(FixedQueueArray<Boolean> history);
        }

        public interface BuildStep
        {
            /**
             * <p>Creates the {@code Gamepad.State}. The state will not be connected.</p>
             *
             * @return gamepad state.
             */
            State build();
        }
    }

    /**
     * <p>This interface should be implemented by custom button enumerations to refer to {@code Gamepad}-specific
     * buttons.</p>
     */
    public interface ButtonWrapper
    {
        /**
         * <p>Gets the underlying {@code Gamepad.Button}.</p>
         *
         * @return button.
         */
        Button button();

        /**
         * <p>Checks if the {@code ButtonWrapper} returns the expected kind of values.</p>
         *
         * @param wrapper wrapper.
         * @throws NullPointerException if wrapper is null.
         * @throws IllegalArgumentException if {@code wrapper.button()} is null.
         */
        static void checkWrapper(ButtonWrapper wrapper)
        {
            if (wrapper.button() == null) {

                final String format = "%s \"%s\" should return a non-null %s";
                final String wrapperName = ButtonWrapper.class.getSimpleName();
                final String actualName = wrapper.getClass().getSimpleName();
                final String button = Button.class.getSimpleName();

                throw new IllegalArgumentException(String.format(format, wrapperName, actualName, button));
            }
        }
    }

    /**
     * <p>This interface should be implemented by custom axis enumerations to refer to {@code Gamepad}-specific axes
     * . {@code vertical()} should never be null.</p>
     */
    public interface AxisWrapper
    {
        /**
         * <p>Gets the horizontal axis.</p>
         *
         * <p>If there is no horizontal axis, implementations should return null.</p>
         *
         * @return horizontal axis, or null if unavailable.
         */
        Axis horizontal();

        /**
         * <p>Gets the vertical axis.</p>
         *
         * @return vertical axis.
         */
        Axis vertical();

        /**
         * <p>Checks if the {@code AxisWrapper} returns the expected kind of values.</p>
         *
         * @param wrapper wrapper.
         * @throws NullPointerException if wrapper is null.
         * @throws IllegalArgumentException if {@code wrapper.vertical()} is null.
         */
        static void checkWrapper(AxisWrapper wrapper)
        {
            if (wrapper.vertical() == null) {

                final String format = "%s \"%s\" should return a non-null vertical %s";
                final String wrapperName = AxisWrapper.class.getSimpleName();
                final String actualName = wrapper.getClass().getSimpleName();
                final String axis = Axis.class.getSimpleName();

                throw new IllegalArgumentException(String.format(format, wrapperName, actualName, axis));
            }
        }
    }
}
