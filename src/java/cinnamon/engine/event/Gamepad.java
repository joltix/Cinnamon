package cinnamon.engine.event;

import cinnamon.engine.utils.IntMap;
import cinnamon.engine.utils.IntMap.IntWrapper;
import cinnamon.engine.utils.Point;
import cinnamon.engine.utils.SparseEnumIntMap;
import cinnamon.engine.utils.Table;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.Map;

/**
 * <p>{@code Gamepad} represents the gamepad (or joystick) input device, providing a view of connection status and
 * button and axis states. With the exception of dead zone radii, updates must be set through the
 * {@code Gamepad.State} passed to the constructor.</p>
 */
public final class Gamepad implements EventSilenceable
{
    /**
     * <p>Each {@code Connection} represents a specific {@code Gamepad} instance.</p>
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
         * <p>Gamepad count.</p>
         */
        public static final int COUNT = Connection.values().length;

        private static final IntMap<Connection> MAPPING = new SparseEnumIntMap<>(Connection.class);

        private int mJoystick;

        Connection(int glfw)
        {
            mJoystick = glfw;
        }

        @Override
        public int toInt()
        {
            return mJoystick;
        }

        /**
         * <p>Gets the {@code Connection} equivalent of a GLFW joystick constant.</p>
         *
         * @param glfw GLFW joystick.
         * @return connection, or null if unrecognized.
         */
        public static Connection from(int glfw)
        {
            return MAPPING.get(glfw);
        }
    }

    /**
     * <p>Gamepad button constants.</p>
     */
    public enum Button implements IntWrapper
    {
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
        BUTTON_13(13);

        /**
         * <p>Button count.</p>
         */
        public static final int COUNT = Button.values().length;

        private static final Button[] MAPPING = Button.values();

        private final int mIndex;

        Button(int index)
        {
            mIndex = index;
        }

        @Override
        public int toInt()
        {
            return mIndex;
        }

        /**
         * <p>Gets the {@code Button} equivalent of a GLFW joystick button constant.</p>
         *
         * @param glfw GLFW button.
         * @return button, or null if unrecognized.
         */
        public static Button from(int glfw)
        {
            if (glfw < 0 || glfw >= MAPPING.length) {
                return null;
            }

            return MAPPING[glfw];
        }
    }

    /**
     * <p>Gamepad axis constants.</p>
     */
    public enum Axis implements IntWrapper
    {
        AXIS_0(0),
        AXIS_1(1),
        AXIS_2(2),
        AXIS_3(3),
        AXIS_4(4),
        AXIS_5(5);

        /**
         * <p>Axis count.</p>
         */
        public static final int COUNT = Axis.values().length;

        private static final Axis[] MAPPING = Axis.values();

        private int mIndex;

        Axis(int glfw)
        {
            mIndex = glfw;
        }

        @Override
        public int toInt()
        {
            return mIndex;
        }

        /**
         * <p>Gets the {@code Axis} equivalent of a GLFW joystick axis constant.</p>
         *
         * @param glfw GLFW axis.
         * @return axis, or null if unrecognized.
         */
        public static Axis from(int glfw)
        {
            if (glfw < 0 || glfw >= MAPPING.length) {
                return null;
            }

            return MAPPING[glfw];
        }
    }

    private static final double DEFAULT_DEAD_ZONE_RADIUS = 0d;

    private static final PressCondition<PadEvent<ButtonWrapper>> mPressCondition = new PressCondition<>();

    private final State mState;

    private final PadProfile mProfile;
    private final Connection mConnection;

    private final Map<Axis, Float> mResting;

    // Dead zone radius is stored only with each wrapper's vertical Axis
    private final Map<Axis, Double> mDeadZones;

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

        mResting = profile.getRestingAxisValues();
        mConnection = connection;
        mProfile = profile;
        mState = state;

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

        if (button.getClass() != mProfile.getButtonClass()) {
            throw new IllegalArgumentException("Constant class does not match profile's button class, " +
                    "expected: " + mProfile.getButtonClass().getSimpleName() + ", actual: " +
                    button.getClass().getSimpleName());
        }

        return mPressCondition.isPressed(button.toButton(), mState.mPresses, mState.mReleases);
    }

    /**
     * <p>Gets an axis' position.</p>
     *
     * @param axis axis.
     * @return position.
     * @throws NullPointerException if axis is null.
     * @throws IllegalArgumentException if axis' class is not the same as that used by the profile.
     */
    public Point getAxisPosition(AxisWrapper axis)
    {
        checkNull(axis);
        checkAxisClass(axis);

        final Axis vertical = axis.getVertical();
        final int ord = vertical.ordinal();
        final PadEvent<AxisWrapper> event = mState.mAxes.get(0, ord);
        final Point pt = new Point();

        // Use resting values when no event
        if (event != null) {
            pt.setX(event.getHorizontal());
            pt.setY(event.getVertical());

        } else {
            final Axis horizontal = axis.getHorizontal();
            pt.setX((horizontal == null) ? 0f : mResting.get(horizontal));
            pt.setY(mResting.get(vertical));
        }

        return pt;
    }

    /**
     * <p>Checks if the axis' position is within its dead zone.</p>
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

        return mState.mZones.get(0, axis.getVertical().ordinal());
    }

    /**
     * <p>Checks if the given position is inside the specified axis' dead zone.</p>
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

        final float xSqr = x * x;
        final float ySqr = y * y;
        final double radius = mDeadZones.get(axis.getVertical());

        // Dead zone does not apply to triggers
        return axis.getHorizontal() != null && xSqr + ySqr <= radius * radius;
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

        if (axis.getClass() != mProfile.getAxisClass()) {
            throw new IllegalArgumentException("Constant class doesn't match profile's expected axis class");
        }
        if (radius < 0d) {
            throw new IllegalArgumentException("Dead zone must be >= 0 and <= 1, actual: " + radius);
        }

        mDeadZones.put(axis.getVertical(), radius);
    }

    /**
     * <p>Gets the {@code PadProfile}.</p>
     *
     * @return profile.
     */
    public PadProfile getProfile()
    {
        return mProfile;
    }

    /**
     * <p>Gets the {@code Connection}.</p>
     *
     * @return connection.
     */
    public Connection getConnection()
    {
        return mConnection;
    }

    /**
     * <p>Checks if the {@code Gamepad} is connected.</p>
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

    private void checkAxisClass(AxisWrapper axis)
    {
        if (axis.getClass() != mProfile.getAxisClass()) {
            throw new IllegalArgumentException("Constant class does not match profile's axis class, expected: " +
                    mProfile.getAxisClass().getSimpleName() + ", actual: " + axis.getClass().getSimpleName());
        }
    }

    private static void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * <p>Controls the {@code Gamepad}'s event histories and connection state. Along with the press/release event
     * histories for buttons, the {@code State} also wraps an axis event history and accompanying history for each
     * axis' dead zone (if applicable).</p>
     *
     * <p>{@code Gamepad.State}s are constructed through a guided builder process as follows.</p>
     * <pre>
     *     <code>
     *         State state = State.builder()
     *           .pressHistory(presses)
     *           .releaseHistory(releases)
     *           .axisHistory(axes)
     *           .deadZoneHistory(zones)
     *           .build();
     *     </code>
     * </pre>
     */
    public static class State
    {
        private final Table<PadEvent<ButtonWrapper>> mPresses;
        private final Table<PadEvent<ButtonWrapper>> mReleases;
        private final Table<PadEvent<AxisWrapper>> mAxes;
        private final Table<Boolean> mZones;

        private boolean mConnected = false;

        /**
         * <p>Constructs a {@code State}.</p>
         *
         * @param pressHistory press event history.
         * @param releaseHistory release event history.
         * @param axisHistory axis event history.
         * @param zoneHistory dead zone history.
         * @throws NullPointerException if either pressHistory, releaseHistory, axisHistory, or zoneHistory is null.
         */
        private State(Table<PadEvent<ButtonWrapper>> pressHistory, Table<PadEvent<ButtonWrapper>> releaseHistory,
                Table<PadEvent<AxisWrapper>> axisHistory, Table<Boolean> zoneHistory)
        {
            checkNull(pressHistory);
            checkNull(releaseHistory);
            checkNull(axisHistory);
            checkNull(zoneHistory);

            mPresses = pressHistory;
            mReleases = releaseHistory;
            mAxes = axisHistory;
            mZones = zoneHistory;
        }

        /**
         * <p>Checks if the {@code Gamepad} is connected.</p>
         *
         * @return connection status.
         */
        public boolean isConnected()
        {
            return mConnected;
        }

        /**
         * <p>Sets the connection status as connected.</p>
         *
         * @param connected true if connected.
         */
        public void setConnected(boolean connected)
        {
            mConnected = connected;
        }

        /**
         * <p>Begins the first step for constructing a {@code Gamepad.State}.</p>
         *
         * @return first build step.
         */
        public static PressHistoryStep builder()
        {
            return new Builder();
        }

        /**
         * <p>Guides the creation of a {@code Gamepad.State}.</p>
         */
        private static class Builder implements PressHistoryStep, ReleaseHistoryStep, AxisHistoryStep,
                DeadZoneHistoryStep, BuildStep
        {
            private Table<PadEvent<ButtonWrapper>> mPressHistory;
            private Table<PadEvent<ButtonWrapper>> mReleaseHistory;
            private Table<PadEvent<AxisWrapper>> mAxisHistory;
            private Table<Boolean> mDeadZoneHistory;

            @Override
            public ReleaseHistoryStep pressHistory(Table<PadEvent<ButtonWrapper>> history)
            {
                checkNull(history);

                mPressHistory = history;
                return this;
            }

            @Override
            public AxisHistoryStep releaseHistory(Table<PadEvent<ButtonWrapper>> history)
            {
                checkNull(history);

                mReleaseHistory = history;
                return this;
            }

            @Override
            public DeadZoneHistoryStep axisHistory(Table<PadEvent<AxisWrapper>> history)
            {
                checkNull(history);

                mAxisHistory = history;
                return this;
            }

            @Override
            public BuildStep deadZoneHistory(Table<Boolean> history)
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
            ReleaseHistoryStep pressHistory(Table<PadEvent<ButtonWrapper>> history);
        }

        public interface ReleaseHistoryStep
        {
            AxisHistoryStep releaseHistory(Table<PadEvent<ButtonWrapper>> history);
        }

        public interface AxisHistoryStep
        {
            DeadZoneHistoryStep axisHistory(Table<PadEvent<AxisWrapper>> axisHistory);
        }

        public interface DeadZoneHistoryStep
        {
            BuildStep deadZoneHistory(Table<Boolean> deadZoneHistory);
        }

        public interface BuildStep
        {
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
         * <p>Gets the raw button.</p>
         *
         * @return raw button.
         */
        Button toButton();
    }

    /**
     * <p>This interface should be implemented by custom axis enumerations to refer to {@code Gamepad}-specific axes
     * . {@code getVertical()} should never be null.</p>
     */
    public interface AxisWrapper
    {
        /**
         * <p>Gets the vertical axis.</p>
         *
         * @return vertical axis.
         */
        Axis getVertical();

        /**
         * <p>Gets the horizontal axis.</p>
         *
         * <p>If there is no horizontal axis, implementations should return null.</p>
         *
         * @return horizontal axis, or null if unavailable.
         */
        Axis getHorizontal();
    }
}
