package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.*;
import cinnamon.engine.event.XB1.Stick;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class GamepadTest
{
    /**
     * <p>This enum is passed to {@code Gamepad}'s methods in order to trigger an {@code IllegalArgumentException}.</p>
     */
    private enum WRONG_BUTTON implements ButtonWrapper
    {
        BUTTON_0;

        @Override
        public Button button()
        {
            return Button.BUTTON_0;
        }
    }

    /**
     * <p>This enum is passed to {@code Gamepad}'s methods in order to trigger an {@code IllegalArgumentException}.</p>
     */
    private enum WRONG_AXIS implements AxisWrapper
    {
        AXIS_0;

        @Override
        public Axis horizontal()
        {
            return null;
        }

        @Override
        public Axis vertical()
        {
            return Axis.AXIS_0;
        }
    }

    // Number of events to track
    private static final int HISTORY_LENGTH = 2;

    // Gamepad identifier
    private static final Connection CONNECTION = Connection.PAD_1;

    // Write access to gamepad's event histories
    private Gamepad.State mState;

    private Gamepad mGamepad;

    @Before
    public void setUp()
    {
        mState = createGamepadState();
        mGamepad = fakeXboxControllerConnected();
    }

    @After
    public void tearDown()
    {
        mState = null;
        mGamepad = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEConnection()
    {
        final State state = mock(State.class);
        new Gamepad(null, XB1.GAMEPAD_PROFILE, state);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEProfile()
    {
        final State state = mock(State.class);
        new Gamepad(Connection.PAD_1, null, state);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEState()
    {
        new Gamepad(Connection.PAD_1, XB1.GAMEPAD_PROFILE, null);
    }

    @Test
    public void testIsPressed()
    {
        fakeButtonEvent(XB1.Button.X, true);

        Assert.assertTrue(mGamepad.isPressed(XB1.Button.X));
    }

    @Test
    public void testIsPressedReturnsFalse()
    {
        fakeButtonEvent(XB1.Button.X, false);

        Assert.assertFalse(mGamepad.isPressed(XB1.Button.Y));
    }

    @Test
    public void testIsPressedReturnsFalseWithEmptyHistory()
    {
        Assert.assertFalse(mGamepad.isPressed(XB1.Button.Y));
    }

    @Test (expected = NullPointerException.class)
    public void testIsPressedNPE()
    {
        mGamepad.isPressed(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsPressedIAEWrongClass()
    {
        mGamepad.isPressed(WRONG_BUTTON.BUTTON_0);
    }

    @Test
    public void testGetMotion()
    {
        final float falseMotion = 0.5f;
        fakeAxisEvent(Stick.RIGHT_TRIGGER, 0f, falseMotion);

        final float motion = mGamepad.getMotion(Stick.RIGHT_TRIGGER).getY();
        Assert.assertEquals(falseMotion, motion, 0f);
    }

    @Test
    public void testGetMotionReturnsRestingPosition()
    {
        final Axis trigger = XB1.Stick.RIGHT_TRIGGER.vertical();
        final float resting = XB1.GAMEPAD_PROFILE.getRestingAxisValues().get(trigger);

        final float motion = mGamepad.getMotion(Stick.RIGHT_TRIGGER).getY();
        Assert.assertEquals(resting, motion, 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testGetMotionNPE()
    {
        mGamepad.getMotion(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetMotionIAEWrongClass()
    {
        mGamepad.getMotion(WRONG_AXIS.AXIS_0);
    }

    @Test
    public void testIsInsideDeadZoneCached()
    {
        fakeAxisEvent(XB1.Stick.LEFT_STICK, 0f, 0f);

        Assert.assertTrue(mGamepad.isInsideDeadZone(XB1.Stick.LEFT_STICK));
    }

    @Test
    public void testIsInsideDeadZoneCachedReturnsFalse()
    {
        fakeAxisEvent(XB1.Stick.LEFT_STICK, 30f, 48f);

        Assert.assertFalse(mGamepad.isInsideDeadZone(XB1.Stick.LEFT_STICK));
    }

    @Test
    public void testIsInsideDeadZoneCachedReturnsFalseWithEmptyHistory()
    {
        Assert.assertFalse(mGamepad.isInsideDeadZone(XB1.Stick.LEFT_STICK));
    }

    @Test (expected = NullPointerException.class)
    public void testIsInsideDeadZoneCachedNPE()
    {
        mGamepad.isInsideDeadZone(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsInsideDeadZoneCachedIAEWrongClass()
    {
        mGamepad.isInsideDeadZone(WRONG_AXIS.AXIS_0);
    }

    @Test
    public void testIsInsideDeadZone()
    {
        Assert.assertTrue(mGamepad.isInsideDeadZone(XB1.Stick.LEFT_STICK, 0f, 0f));
    }

    @Test
    public void testIsInsideDeadZoneReturnsFalse()
    {
        Assert.assertFalse(mGamepad.isInsideDeadZone(XB1.Stick.LEFT_STICK, 50f, 80f));
    }

    @Test (expected = NullPointerException.class)
    public void testIsInsideDeadZoneNPE()
    {
        mGamepad.isInsideDeadZone(null, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsInsideDeadZoneIAEWrongClass()
    {
        mGamepad.isInsideDeadZone(WRONG_AXIS.AXIS_0, 0f, 0f);
    }

    @Test
    public void testGetDeadZone()
    {
        final double zoneRadius = mGamepad.getDeadZone(Stick.RIGHT_TRIGGER);
        Assert.assertEquals(0d, zoneRadius, 0d);
    }

    @Test (expected = NullPointerException.class)
    public void testGetDeadZoneNPE()
    {
        mGamepad.getDeadZone(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetDeadZoneIAEWrongClass()
    {
        mGamepad.getDeadZone(WRONG_AXIS.AXIS_0);
    }

    @Test
    public void testSetDeadZone()
    {
        mGamepad.setDeadZone(XB1.Stick.LEFT_STICK, 0.15d);
    }

    @Test (expected = NullPointerException.class)
    public void testSetDeadZoneNPE()
    {
        mGamepad.setDeadZone(null, 0.15d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAERadiusTooSmall()
    {
        mGamepad.setDeadZone(XB1.Stick.LEFT_STICK, -0.15d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAERadiusTooLarge()
    {
        mGamepad.setDeadZone(XB1.Stick.LEFT_STICK, -1.5d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAEWrongClass()
    {
        mGamepad.setDeadZone(WRONG_AXIS.AXIS_0, 0.15d);
    }

    @Test
    public void testIsMutedReturnsFalse()
    {
        Assert.assertFalse(mGamepad.isMuted());
    }

    @Test
    public void testIsMutedReturnsTrueAfterMute()
    {
        mGamepad.mute();

        Assert.assertTrue(mGamepad.isMuted());
    }

    @Test
    public void testIsMutedReturnsFalseAfterUnmute()
    {
        mGamepad.unmute();

        Assert.assertFalse(mGamepad.isMuted());
    }

    /**
     * <p>Inserts a button-type event into the gamepad's button history.</p>
     *
     * @param button source of event.
     * @param press true if pressed.
     * @param <T> type of button.
     */
    private <T extends Enum<T> & ButtonWrapper> void fakeButtonEvent(T button, boolean press)
    {
        final PadEvent event = new PadEvent(0L, CONNECTION, button, press);

        final FixedQueueArray<PadEvent> history;
        history = (press) ? mState.getPressHistory() : mState.getReleaseHistory();
        history.add(button.button().ordinal(), event);
    }

    /**
     * <p>Attempts to insert a motion event into the gamepad's histories. If the motion is within the dead zone
     * assigned to the given axis, the event is not written to the motion history. Whether or not the motion is
     * ignored, it will be written to the dead zone history as appropriate.</p>
     *
     * @param axis source of event.
     * @param horizontal horizontal offset.
     * @param vertical vertical offset.
     * @param <T> type of axis.
     */
    private <T extends Enum<T> & AxisWrapper> void fakeAxisEvent(T axis, float horizontal, float vertical)
    {

        final double radius = mGamepad.getDeadZone(axis);
        final boolean suppressed = isInsideRadius(horizontal, vertical, radius);
        final int ord = axis.vertical().ordinal();

        if (!suppressed) {
            final Point position = new Point(horizontal, vertical, 0f);
            mState.getMotionHistory().add(ord, new PadEvent(0L, CONNECTION, axis, position));
        }

        mState.getDeadZoneHistory().add(ord, suppressed);
    }

    private boolean isInsideRadius(float h, float v, double r)
    {
        return (h * h) + (v * v) <= r * r;
    }

    private Gamepad fakeXboxControllerConnected()
    {
        mState.setConnected(true);
        return new Gamepad(CONNECTION, XB1.GAMEPAD_PROFILE, mState);
    }

    /**
     * <p>Creates a {@code Gamepad.State} with empty event histories.</p>
     *
     * @return state.
     */
    private Gamepad.State createGamepadState()
    {
        final int c = XB1.Button.COUNT;
        final int l = HISTORY_LENGTH;

        final FixedQueueArray<PadEvent> presses = new FixedQueueArray<>(c, l);
        final FixedQueueArray<PadEvent> releases = new FixedQueueArray<>(c, l);
        final FixedQueueArray<PadEvent> motions = new FixedQueueArray<>(c, l);
        final FixedQueueArray<Boolean> zones = new FixedQueueArray<>(c, l);

        return Gamepad.State.builder()
                .pressHistory(presses)
                .releaseHistory(releases)
                .motionHistory(motions)
                .deadZoneHistory(zones)
                .build();
    }
}
