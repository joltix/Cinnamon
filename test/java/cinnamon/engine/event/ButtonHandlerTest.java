package cinnamon.engine.event;

import cinnamon.engine.event.Keyboard.Key;
import cinnamon.engine.utils.FixedQueueArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>These cases test the success and failure of the following forms.</p>
 * <ul>
 *     <li>Press</li>
 *     <li>Multi press</li>
 *     <li>Release</li>
 *     <li>Multi release</li>
 *     <li>Double click</li>
 * </ul>
 */
public class ButtonHandlerTest
{
    /**
     * All time constants below are measured in milliseconds.
     */

    // Wait after default history; allow tests to disqualify default events
    private static final long HISTORY_SEPARATION_DURATION = 300L;

    // How far back each key's history goes
    private static final int GENERATIONS = 2;

    // Max time allowed between target events
    private static final long TOLERANCE = 75L;

    // Max time between a press and its release
    private static final long DURATION = 75L;

    // Keyboard history; [0] = press history, [1] = release history
    private FixedQueueArray<KeyEvent>[] mHistory;

    private ButtonHandler<Key, Key, KeyEvent> mHandler;

    @Before
    public void setUp()
    {
        mHistory = createDefaultKeyboardHistory();

        mHandler = new ButtonHandler<Key, Key, KeyEvent>(Key.class, mHistory[0], mHistory[1])
        {
            @Override
            protected Key extractConstantFrom(KeyEvent event)
            {
                return event.getKey();
            }
        };

        waitFor(HISTORY_SEPARATION_DURATION);
    }

    @After
    public void tearDown()
    {
        mHistory = null;
        mHandler = null;
    }

    @Test
    public void testExecuteOnEventPress()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forPress(100);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        execute(Key.KEY_SPACE, true);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventPressDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forPress(100);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiPress()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiPress(100, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_SPACE, true);
        fakeHistory(Key.KEY_ENTER, true);
        execute(Key.KEY_ENTER, true);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiPressDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiPress(100, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_SPACE, true);
        fakeHistory(Key.KEY_ENTER, false);
        execute(Key.KEY_ENTER, true);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventRelease()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forRelease(100);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventReleaseDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forRelease(100);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        execute(Key.KEY_SPACE, false);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventReleaseWithDurationConstraint()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forRelease(100, DURATION);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventReleaseWithDurationConstraintDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forRelease(100, DURATION);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        waitFor(DURATION * 2L);
        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiRelease()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiRelease(100, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_SPACE, false);
        fakeHistory(Key.KEY_ENTER, false);
        execute(Key.KEY_ENTER, false);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiReleaseDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiRelease(100, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_SPACE, true);
        fakeHistory(Key.KEY_ENTER, false);
        execute(Key.KEY_ENTER, true);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiReleaseWithDurationConstraint()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiRelease(100, DURATION, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_SPACE, true);
        fakeHistory(Key.KEY_ENTER, true);
        fakeHistory(Key.KEY_SPACE, false);
        fakeHistory(Key.KEY_ENTER, false);
        execute(Key.KEY_SPACE, false);
        execute(Key.KEY_ENTER, false);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventMultiReleaseWithDurationConstraintDoesNothing()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forMultiRelease(100, DURATION, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE, Key.KEY_ENTER});

        fakeHistory(Key.KEY_ENTER, true);
        waitFor(DURATION * 2L);
        fakeHistory(Key.KEY_ENTER, false);
        execute(Key.KEY_ENTER, true);

        Assert.assertFalse(trace.get());
    }

    @Test
    public void testExecuteOnEventDoubleClick()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forDoubleClick(100, DURATION, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        execute(Key.KEY_SPACE, true);

        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        fakeHistory(Key.KEY_SPACE, true);
        execute(Key.KEY_SPACE, true);

        fakeHistory(Key.KEY_SPACE, false);
        execute(Key.KEY_SPACE, false);

        Assert.assertTrue(trace.get());
    }

    @Test
    public void testExecuteOnEventDoubleClickDoesNothingClicksFarApart()
    {
        final AtomicReference<Boolean> trace = new AtomicReference<>(false);
        final ButtonPreferences o = ButtonPreferences.forDoubleClick(0, DURATION, TOLERANCE);
        setTraceMapping(trace, o, new Key[] {Key.KEY_SPACE});

        fakeHistory(Key.KEY_SPACE, true);
        execute(Key.KEY_SPACE, true);

        Assert.assertFalse(trace.get());
    }

    /**
     * <p>Blocks for at least the given amount of time.</p>
     *
     * @param duration time to wait in milliseconds.
     */
    private void waitFor(long duration)
    {
        final long durInNano = duration * 1_000_000L;
        final long start = System.nanoTime();

        //noinspection StatementWithEmptyBody
        while ((System.nanoTime() - start) <= durInNano) {
        }
    }

    /**
     * <p>Sets a control mapping such that the given {@code AtomicReference<Boolean>} will be set to {@code true}
     * should the instructions be executed.</p>
     *
     * @param trace trace.
     * @param preferences preferences.
     * @param keys required constants.
     */
    private void setTraceMapping(AtomicReference<Boolean> trace, ButtonPreferences preferences, Key[] keys)
    {
        final Map<String, ButtonRule<Key, KeyEvent>> mapping = new HashMap<>(1);

        mapping.put("trace", new ButtonRule<>(keys, (event) ->
        {
            trace.set(true);
        }, preferences));

        mHandler.setMappings(mapping);
    }

    /**
     * <p>Writes a {@code KeyEvent} to the event history.</p>
     *
     * @param key key.
     * @param press true to create press event, false for release event.
     */
    private void fakeHistory(Key key, boolean press)
    {
        (((press)) ? mHistory[0] : mHistory[1]).add(key.ordinal(), new KeyEvent(System.nanoTime(), key, press));
    }

    /**
     * <p>Calls {@link ButtonHandler#submit(InputEvent)} with a new {@code KeyEvent}.</p>
     *
     * @param key key.
     * @param press true to create press event, false for release event.
     */
    private void execute(Key key, boolean press)
    {
        mHandler.submit(new KeyEvent(System.nanoTime(), key, press));
    }

    /**
     * <p>Creates a press and release history for {@code KeyEvent}s with records for two clicks (four events) for each
     * {@code Key}.</p>
     *
     * @return keyboard history.
     */
    @SuppressWarnings("unchecked")
    private FixedQueueArray<KeyEvent>[] createDefaultKeyboardHistory()
    {
        final FixedQueueArray<KeyEvent>[] histories = (FixedQueueArray<KeyEvent>[]) new FixedQueueArray[2];
        histories[0] = new FixedQueueArray<>(Key.COUNT, GENERATIONS);
        histories[1] = new FixedQueueArray<>(Key.COUNT, GENERATIONS);

        final long time = System.nanoTime();

        for (final Key constant : Key.class.getEnumConstants()) {
            final int ord = constant.ordinal();
            histories[0].add(ord, new KeyEvent(time, constant, true));
            histories[1].add(ord, new KeyEvent(time, constant, false));
            histories[0].add(ord, new KeyEvent(time, constant, true));
            histories[1].add(ord, new KeyEvent(time, constant, false));
        }

        return histories;
    }
}
