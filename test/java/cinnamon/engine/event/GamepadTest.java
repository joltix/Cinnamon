package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.*;
import cinnamon.engine.event.XboxPadProfile.Button;
import cinnamon.engine.event.XboxPadProfile.Stick;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Table;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class GamepadTest
{
    private static final ButtonWrapper WRONG_BUTTON = new ButtonWrapper()
    {
        @Override
        public Gamepad.Button toButton()
        {
            return Gamepad.Button.BUTTON_0;
        }
    };

    private static final AxisWrapper WRONG_AXIS = new AxisWrapper()
    {
        @Override
        public Axis getVertical()
        {
            return Axis.AXIS_0;
        }

        @Override
        public Axis getHorizontal()
        {
            return null;
        }
    };

    private static final int HISTORY_LENGTH = 2;

    private static final Connection CONNECTION = Connection.PAD_1;

    private FixedQueueArray<PadEvent<ButtonWrapper>>[] mButtonHistory;
    private FixedQueueArray<PadEvent<AxisWrapper>> mAxisHistory;
    private FixedQueueArray<Boolean> mDeadZoneHistory;

    private Gamepad mGamepad;
    private State mState;

    @Before
    public void setUp()
    {
        mButtonHistory = createButtonHistory();
        mAxisHistory = createAxisHistory();
        mDeadZoneHistory = createDeadZoneHistory();

        mGamepad = fakeXboxControllerConnected(mButtonHistory[0], mButtonHistory[1], mAxisHistory, mDeadZoneHistory);
    }

    @After
    public void tearDown()
    {
        mButtonHistory = null;
        mAxisHistory = null;
        mDeadZoneHistory = null;

        mGamepad = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEConnection()
    {
        final State state = mock(State.class);
        new Gamepad(null, new XboxPadProfile(), state);
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
        new Gamepad(Connection.PAD_1, new XboxPadProfile(), null);
    }

    @Test
    public void testIsPressed()
    {
        mockButtonEvent(Button.X, true);

        Assert.assertTrue(mGamepad.isPressed(XboxPadProfile.Button.X));
    }

    @Test
    public void testIsPressedReturnsFalse()
    {
        mockButtonEvent(Button.X, false);

        Assert.assertFalse(mGamepad.isPressed(XboxPadProfile.Button.Y));
    }

    @Test (expected = NullPointerException.class)
    public void testIsPressedNPE()
    {
        mGamepad.isPressed(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsPressedIAE()
    {
        mGamepad.isPressed(WRONG_BUTTON);
    }

    @Test
    public void testGetAxisPosition()
    {
        final Axis vertical = Stick.RIGHT_TRIGGER.getVertical();
        final float v = new XboxPadProfile().getRestingAxisValues().get(vertical);

        Assert.assertEquals(v, mGamepad.getAxisPosition(Stick.RIGHT_TRIGGER).getY(), 0f);
    }

    @Test
    public void testGetAxisPositionReturnsRestingPosition()
    {
        final Axis vertical = Stick.RIGHT_TRIGGER.getVertical();

        // Remove all events for the right trigger
        mAxisHistory.clear(vertical.ordinal());

        final float v = new XboxPadProfile().getRestingAxisValues().get(vertical);

        Assert.assertEquals(v, mGamepad.getAxisPosition(Stick.RIGHT_TRIGGER).getY(), 0f);
    }

    @Test (expected = NullPointerException.class)
    public void testGetAxisPositionNPE()
    {
        mGamepad.getAxisPosition(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testGetAxisPositionIAE()
    {
        mGamepad.getAxisPosition(WRONG_AXIS);
    }

    @Test
    public void testIsInsideDeadZoneCached()
    {
        mockAxisEvent(Stick.LEFT_STICK, 0f, 0f);

        Assert.assertTrue(mGamepad.isInsideDeadZone(Stick.LEFT_STICK));
    }

    @Test
    public void testIsInsideDeadZoneCachedReturnsFalse()
    {
        mockAxisEvent(Stick.LEFT_STICK, 30f, 48f);

        Assert.assertFalse(mGamepad.isInsideDeadZone(Stick.LEFT_STICK));
    }

    @Test (expected = NullPointerException.class)
    public void testIsInsideDeadZoneCachedNPE()
    {
        mGamepad.isInsideDeadZone(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsInsideDeadZoneCachedIAE()
    {
        mGamepad.isInsideDeadZone(WRONG_AXIS);
    }

    @Test
    public void testIsInsideDeadZone()
    {
        Assert.assertTrue(mGamepad.isInsideDeadZone(Stick.LEFT_STICK, 0f, 0f));
    }

    @Test
    public void testIsInsideDeadZoneReturnsFalse()
    {
        Assert.assertFalse(mGamepad.isInsideDeadZone(Stick.LEFT_STICK, 50f, 80f));
    }

    @Test (expected = NullPointerException.class)
    public void testIsInsideDeadZoneNPE()
    {
        mGamepad.isInsideDeadZone(null, 0f, 0f);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testIsInsideDeadZoneIAE()
    {
        mGamepad.isInsideDeadZone(WRONG_AXIS, 0f, 0f);
    }

    @Test
    public void testSetDeadZone()
    {
        mGamepad.setDeadZone(Stick.LEFT_STICK, 0.15d);
    }

    @Test (expected = NullPointerException.class)
    public void testSetDeadZoneNPE()
    {
        mGamepad.setDeadZone(null, 0.15d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAERadiusTooSmall()
    {
        mGamepad.setDeadZone(Stick.LEFT_STICK, -0.15d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAERadiusTooLarge()
    {
        mGamepad.setDeadZone(Stick.LEFT_STICK, -1.5d);
    }

    @Test (expected = IllegalArgumentException.class)
    public void testSetDeadZoneIAEWrongClass()
    {
        mGamepad.setDeadZone(WRONG_AXIS, 0.15d);
    }

    @Test
    public void testStateSetConnected()
    {
        mState.setConnected(false);

        Assert.assertFalse(mState.isConnected());
        Assert.assertFalse(mGamepad.isConnected());
    }

    /**
     * <p>Inserts a button-type event into the gamepad's button history.</p>
     *
     * @param button source of event.
     * @param press true if pressed.
     * @param <T> type of button.
     */
    private <T extends Enum<T> & ButtonWrapper> void mockButtonEvent(T button, boolean press)
    {
        final PadEvent<ButtonWrapper> event = PadEvent.createForButton(CONNECTION, button, press);

        mButtonHistory[(press) ? 0 : 1].add(button.toButton().ordinal(), event);
    }

    /**
     * <p>Inserts an axis-type event into the gamepad's axis history.</p>
     *
     * @param axis source of event.
     * @param horizontal horizontal position.
     * @param vertical vertical position.
     * @param <T> type of axis.
     */
    private <T extends Enum<T> & AxisWrapper> void mockAxisEvent(T axis, float horizontal, float vertical)
    {
        final PadEvent<AxisWrapper> event = PadEvent.createForAxis(CONNECTION, axis, horizontal, vertical);
        final int ord = axis.getVertical().ordinal();

        mAxisHistory.add(ord, event);
        mDeadZoneHistory.add(ord, (horizontal == 0f && vertical == 0f));
    }


    private Gamepad fakeXboxControllerConnected(Table<PadEvent<ButtonWrapper>> presses,
                                                Table<PadEvent<ButtonWrapper>> releases,
                                                Table<PadEvent<AxisWrapper>> axes, Table<Boolean> zones)
    {
        mState = State.builder()
                .pressHistory(presses)
                .releaseHistory(releases)
                .axisHistory(axes)
                .deadZoneHistory(zones)
                .build();

        mState.setConnected(true);
        return new Gamepad(Connection.PAD_1, new XboxPadProfile(), mState);
    }

    /**
     * <p>Press history is returned in the 0th index with release history in the second.</p>
     *
     * @return event histories.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<PadEvent<ButtonWrapper>>[] createButtonHistory()
    {
        final int buttonCount = Gamepad.Button.values().length;
        final FixedQueueArray<PadEvent<ButtonWrapper>>[] histories =
                (FixedQueueArray<PadEvent<ButtonWrapper>>[]) new FixedQueueArray[2];

        histories[0] = new FixedQueueArray<>(buttonCount, HISTORY_LENGTH);
        histories[1] = new FixedQueueArray<>(buttonCount, HISTORY_LENGTH);

        return histories;
    }

    private FixedQueueArray<PadEvent<AxisWrapper>> createAxisHistory()
    {
        return new FixedQueueArray<>(Axis.COUNT, HISTORY_LENGTH);
    }

    private FixedQueueArray<Boolean> createDeadZoneHistory()
    {
        return new FixedQueueArray<>(Axis.COUNT, HISTORY_LENGTH);
    }
}
