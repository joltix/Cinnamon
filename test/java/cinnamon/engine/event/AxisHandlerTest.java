package cinnamon.engine.event;

import cinnamon.engine.event.MotionPreferences.Axis;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.FixedQueueArray;
import cinnamon.engine.utils.Point;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>Mouse scrolling is front-and-center in this class' tests but gamepad axes are also handled in the same manner.
 * The following kinds of {@link MotionPreferences} are tested.</p>
 * <ul>
 *     <li>any motion</li>
 *     <li>motion on x only</li>
 *     <li>motion on y only</li>
 *     <li>positive x axis</li>
 *     <li>negative x axis</li>
 *     <li>positive y axis</li>
 *     <li>negative y axis</li>
 *     <li>towards resting position</li>
 *     <li>away from resting position</li>
 *     <li>towards resting position on x only</li>
 *     <li>away from resting position on x only</li>
 *     <li>towards resting position on y only</li>
 *     <li>away from resting position on y only</li>
 * </ul>
 */
public class AxisHandlerTest
{
    private static final int MIDDLE_ORD = Button.MIDDLE.ordinal();

    // How far back each axis' history goes
    private static final int GENERATIONS = 2;

    // Inserted to history prior to a new scroll event
    private static final MouseEvent NO_SCROLL_EVENT = new MouseEvent(0L, new Point(), new Point());

    // Mouse scroll history
    private FixedQueueArray<MouseEvent> mHistory;

    private AxisHandler<Button, Button, MouseEvent> mHandler;

    @Before
    public void setUp()
    {
        mHistory = new FixedQueueArray<>(Button.COUNT, GENERATIONS);
        mHandler = new AxisHandler<Button, Button, MouseEvent>(Button.class, mHistory)
        {
            @Override
            protected Button extractConstantFrom(MouseEvent event)
            {
                return event.getButton();
            }
        };
    }

    @After
    public void tearDown()
    {
        mHandler = null;
        mHistory = null;
    }

    @Test
    public void testExecuteOnEventScrollFree()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forTranslation());

        // Scroll left
        fakeHistory(false, -1f, false);
        execute();

        // Scroll up
        fakeHistory(true, 1f, false);
        execute();

        // Scroll right
        fakeHistory(false, 1f, false);
        execute();

        // Scroll down
        fakeHistory(true, -1f, false);
        execute();

        Assert.assertEquals(new Integer(4), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollHorizontal()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forTranslation(Axis.X));

        // Scroll left
        fakeHistory(false, -1f, false);
        execute();

        // Scroll right
        fakeHistory(false, 1f, false);
        execute();

        Assert.assertEquals(new Integer(2), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollHorizontalDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forTranslation(Axis.X));

        // Scroll down
        fakeHistory(true, -1f, false);
        execute();

        // Scroll up
        fakeHistory(true, 1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollVertical()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forTranslation(Axis.Y));

        // Scroll down
        fakeHistory(true, -1f, false);
        execute();

        // Scroll up
        fakeHistory(true, 1f, false);
        execute();

        Assert.assertEquals(new Integer(2), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollVerticalDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forTranslation(Axis.Y));

        // Scroll left
        fakeHistory(false, -1f, false);
        execute();

        // Scroll right
        fakeHistory(false, 1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollLeft()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.X, false));

        fakeHistory(false, -1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollLeftDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.X, false));

        fakeHistory(false, 1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollUp()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.Y, true));

        fakeHistory(true, 1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollUpDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.Y, true));

        fakeHistory(true, -1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollRight()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.X, true));

        fakeHistory(false, 1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollRightDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.X, true));

        fakeHistory(false, -1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollDown()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.Y, false));

        fakeHistory(true, -1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollDownDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forSignedTranslation(Axis.Y, false));

        fakeHistory(true, 1f, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAway()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(true));

        fakeHistory(true, -1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAwayDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(true));

        fakeHistory(true, 0f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAwayOnX()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.X, true));

        fakeHistory(false, -1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAwayOnXDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.X, true));

        fakeHistory(false, 0f, true);
        fakeHistory(false, 0f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAwayOnY()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.Y, true));

        fakeHistory(true, -1f, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollAwayOnYDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.Y, true));

        fakeHistory(true, 0f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowards()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(false));

        fakeHistory(true, -1f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowardsDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(false));

        fakeHistory(true, 0f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowardsOnX()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.X, false));

        fakeHistory(false, -1f, true);
        fakeHistory(false, 0f, true);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowardsOnXDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.X, false));

        fakeHistory(true, -1f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowardsOnY()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.Y, false));

        fakeHistory(true, -1f, true);
        fakeHistory(true, 0f, true);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    /**
     * <p>This case does not test events with NO_SCROLL_EVENT prefixed in order to test how {@code PadEvent}s are
     * handled. Mouse scrolls are a special case where there is typically no scrolling toward a starting position.</p>
     */
    @Test
    public void testExecuteOnEventScrollTowardsOnYDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, MotionPreferences.forDirectedTranslation(Axis.Y, false));

        fakeHistory(false, 0f, true);
        fakeHistory(false, -1f, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    /**
     * <p>Calls {@link AxisHandler#submit(InputEvent)} with the most recent event in the scroll history.</p>
     */
    private void execute()
    {
        mHandler.submit(mHistory.get(0, MIDDLE_ORD));
    }

    /**
     * <p>Sets the control mapping responsible for updating the given trace. The trace's value will be incremented
     * for each qualifying execution, as determined by the given preferences.</p>
     *
     * @param trace trace.
     * @param preferences preferences.
     */
    private void setTraceMapping(AtomicReference<Integer> trace, MotionPreferences preferences)
    {
        final Map<String, AxisRule<Button, MouseEvent>> mapping = new HashMap<>(1);

        mapping.put("trace", new AxisRule<>(Button.MIDDLE, (event) ->
        {
            trace.getAndUpdate((count) ->
            {
                return count + 1;
            });
        }, preferences, 0));

        mHandler.setMappings(mapping);
    }

    /**
     * <p>Inserts scrolling {@code MouseEvent}s into the history.</p>
     *
     *  @param vertical true if scrolling along the y-axis, false for x-axis.
     * @param motion motion value.
     * @param direct false to insert the NO_SCROLL_EVENT prior to the actual event.
     */
    private void fakeHistory(boolean vertical, float motion, boolean direct)
    {
        if (!direct) {
            mHistory.add(MIDDLE_ORD, NO_SCROLL_EVENT);
        }

        final float x;
        final float y;

        if (vertical) {
            y = motion;
            x = 0f;
        } else {
            y = 0f;
            x = motion;
        }

        final Point offsets = new Point(x, y, 0f);
        mHistory.add(MIDDLE_ORD, new MouseEvent(System.nanoTime(), new Point(), offsets));
    }
}
