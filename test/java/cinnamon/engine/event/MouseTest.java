package cinnamon.engine.event;

import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MouseTest
{
    // Number of events to track per button
    private static final int HISTORY_LENGTH = 2;

    // Write access to mouse' position and event histories
    private Mouse.State mState;

    private Mouse mMouse;

    @Before
    public void setUp()
    {
        mState = createMouseState();
        mMouse = new Mouse(mState);
    }

    @After
    public void tearDown()
    {
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
        final Point position = new Point(4f, 2f, 0f);
        mState.getPosition().copy(position);

        Assert.assertEquals(position, mMouse.getPosition());
    }

    @Test
    public void testGetPositionChangesDoNotAffectPosition()
    {
        mMouse.getPosition().setPosition(3f, 4f, 3f);

        Assert.assertEquals(new Point(0f, 0f, 0f), mState.getPosition());
    }

    @Test
    public void testGetHorizontalScroll()
    {
        fakeScroll(1f, 0f);

        Assert.assertEquals(1f, mMouse.getHorizontalScroll(), 0f);
    }

    @Test
    public void testGetHorizontalScrollReturnsZeroWithEmptyHistory()
    {
        Assert.assertEquals(0f, mMouse.getHorizontalScroll(), 0f);
    }

    @Test
    public void testGetVerticalScroll()
    {
        fakeScroll(0f, 1f);

        Assert.assertEquals(1f, mMouse.getVerticalScroll(), 0f);
    }

    @Test
    public void testGetVerticalScrollReturnsZeroWithEmptyHistory()
    {
        Assert.assertEquals(0f, mMouse.getVerticalScroll(), 0f);
    }

    @Test
    public void testIsPressed()
    {
        fakeButtonAction(Button.MIDDLE, true);

        Assert.assertTrue(mMouse.isPressed(Button.MIDDLE));
    }

    @Test
    public void testIsPressedReturnsFalse()
    {
        fakeButtonAction(Button.MIDDLE, false);

        Assert.assertFalse(mMouse.isPressed(Button.MIDDLE));
    }

    @Test
    public void testIsPressedReturnsFalseWithEmptyHistory()
    {
        Assert.assertFalse(mMouse.isPressed(Button.MIDDLE));
    }

    @Test (expected = NullPointerException.class)
    public void testIsPressedNPE()
    {
        mMouse.isPressed(null);
    }

    @Test
    public void testIsMutedReturnsFalse()
    {
        Assert.assertFalse(mMouse.isMuted());
    }

    @Test
    public void testIsMutedReturnsTrueAfterMute()
    {
        mMouse.mute();

        Assert.assertTrue(mMouse.isMuted());
    }

    @Test
    public void testIsMutedReturnsFalseAfterUnmute()
    {
        mMouse.unmute();

        Assert.assertFalse(mMouse.isMuted());
    }

    /**
     * <p>Creates a {@code Mouse.State} with empty event histories. The position will be (0,0).</p>
     *
     * @return mouse's state.
     */
    private Mouse.State createMouseState()
    {
        final int count = Key.COUNT;
        final int length = HISTORY_LENGTH;
        final FixedQueueArray<MouseEvent> presses = new FixedQueueArray<>(count, length);
        final FixedQueueArray<MouseEvent> releases = new FixedQueueArray<>(count, length);
        final FixedQueueArray<MouseEvent> scrolls = new FixedQueueArray<>(count, length);

        return Mouse.State.builder()
                .pressHistory(presses)
                .releaseHistory(releases)
                .scrollHistory(scrolls)
                .position(new Point())
                .build();
    }

    /**
     * <p>Writes a button event to the mouse' history. The event will have a timestamp of 0.</p>
     *
     * @param button button.
     * @param press true if pressed.
     */
    private void fakeButtonAction(Button button, boolean press)
    {
        final MouseEvent action = new MouseEvent(0L, new Point(), button, press);

        final FixedQueueArray<MouseEvent> history;
        history = (press) ? mState.getPressHistory() : mState.getReleaseHistory();
        history.add(button.ordinal(), action);
    }

    /**
     * <p>Writes a scroll event to the mouse' history. The event's button will be {@code Mouse.Button.MIDDLE} and the
     * timestamp will be 0.</p>
     *
     * @param horizontal horizontal scroll offset.
     * @param vertical vertical scroll offset.
     */
    private void fakeScroll(float horizontal, float vertical)
    {
        final Point offsets = new Point(horizontal, vertical, 0f);

        mState.getScrollHistory().add(0, new MouseEvent(0L, new Point(), offsets));
    }
}
