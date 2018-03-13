package cinnamon.engine.event;

import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.event.Mouse.State;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Position;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MouseTest
{
    private static final int HISTORY_LENGTH = 2;

    // Misc location for position testing
    private static final float POSITION_X = 4f;
    private static final float POSITION_Y = 2f;

    private Mouse mMouse;
    private Mouse.State mState;

    private FixedQueueArray<MouseEvent> mPressHistory;
    private FixedQueueArray<MouseEvent> mReleaseHistory;

    @Before
    public void setUp()
    {
        mPressHistory = new FixedQueueArray<>(Key.COUNT, HISTORY_LENGTH);
        mReleaseHistory = new FixedQueueArray<>(Key.COUNT, HISTORY_LENGTH);

        mState = new State(mPressHistory, mReleaseHistory);
        mMouse = new Mouse(mState);
    }

    @After
    public void tearDown()
    {
        mPressHistory = null;
        mReleaseHistory = null;

        mState = null;
        mMouse = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPE()
    {
        new Mouse(null);
    }

    @Test
    public void testGetPosition()
    {
        mState.getPosition().setPosition(POSITION_X, POSITION_Y, 0f);

        final Position pos = mMouse.getPosition();
        Assert.assertEquals(POSITION_X, pos.getX(), 0f);
        Assert.assertEquals(POSITION_Y, pos.getY(), 0f);
    }

    @Test
    public void testGetHorizontalScroll()
    {
        mState.setHorizontalScrollOffset(5f);

        Assert.assertEquals(5f, mMouse.getHorizontalScroll(), 0f);
    }

    @Test
    public void testGetVerticalScroll()
    {
        mState.setVerticalScrollOffset(3f);

        Assert.assertEquals(3f, mMouse.getVerticalScroll(), 0f);
    }

    @Test
    public void testIsPressed()
    {
        final MouseEvent event = MouseEvent.createForButton(Button.MIDDLE, true, POSITION_X, POSITION_Y);
        mPressHistory.add(Button.MIDDLE.ordinal(), event);

        Assert.assertTrue(mMouse.isPressed(Button.MIDDLE));
    }

    @Test
    public void testIsPressedReturnsFalse()
    {
        final MouseEvent event = MouseEvent.createForButton(Button.MIDDLE, false, POSITION_X, POSITION_Y);
        mReleaseHistory.add(Button.MIDDLE.ordinal(), event);

        Assert.assertFalse(mMouse.isPressed(Button.MIDDLE));
    }

    @Test (expected = NullPointerException.class)
    public void testIsPressedNPE()
    {
        mMouse.isPressed(null);
    }
}
