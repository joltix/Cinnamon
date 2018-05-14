package cinnamon.engine.event;

import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.utils.FixedQueueArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class KeyboardTest
{
    // Number of events to track per key
    private static final int HISTORY_LENGTH = 2;

    private Keyboard mKeyboard;

    private FixedQueueArray<KeyEvent> mPressHistory;

    private FixedQueueArray<KeyEvent> mReleaseHistory;

    @Before
    public void setUp()
    {
        mPressHistory = new FixedQueueArray<>(Key.COUNT, HISTORY_LENGTH);
        mReleaseHistory = new FixedQueueArray<>(Key.COUNT, HISTORY_LENGTH);

        mKeyboard = new Keyboard(mPressHistory, mReleaseHistory);
    }

    @After
    public void tearDown()
    {
        mPressHistory = null;
        mReleaseHistory = null;

        mKeyboard = null;
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEPressHistory()
    {
        new Keyboard(null, mReleaseHistory);
    }

    @Test (expected = NullPointerException.class)
    public void testConstructorNPEReleaseHistory()
    {
        new Keyboard(null, mReleaseHistory);
    }

    @Test
    public void testIsPressed()
    {
        final KeyEvent event = new KeyEvent(0L, Key.KEY_SPACE, true);
        mPressHistory.add(Key.KEY_SPACE.ordinal(), event);

        Assert.assertTrue(mKeyboard.isPressed(Key.KEY_SPACE));
    }

    @Test
    public void testIsPressedReturnsFalse()
    {
        final KeyEvent event = new KeyEvent(0L, Key.KEY_SPACE, false);
        mReleaseHistory.add(Key.KEY_SPACE.ordinal(), event);

        Assert.assertFalse(mKeyboard.isPressed(Key.KEY_SPACE));
    }

    @Test
    public void testIsPressedReturnsFalseWithEmptyHistory()
    {
        Assert.assertFalse(mKeyboard.isPressed(Key.KEY_SPACE));
    }

    @Test (expected = NullPointerException.class)
    public void testIsPressedNPE()
    {
        mKeyboard.isPressed(null);
    }

    @Test
    public void testIsMutedReturnsFalse()
    {
        Assert.assertFalse(mKeyboard.isMuted());
    }

    @Test
    public void testIsMutedReturnsTrueAfterMute()
    {
        mKeyboard.mute();

        Assert.assertTrue(mKeyboard.isMuted());
    }

    @Test
    public void testIsMutedReturnsFalseAfterUnmute()
    {
        mKeyboard.unmute();

        Assert.assertFalse(mKeyboard.isMuted());
    }
}
