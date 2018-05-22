package cinnamon.engine.event;

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
 * The following kinds of {@link AxisPreferences} are tested.</p>
 * <ul>
 *     <li>Any motion</li>
 *     <li>Horizontal only</li>
 *     <li>Vertical only</li>
 *     <li>Positive horizontal</li>
 *     <li>Negative horizontal</li>
 *     <li>Positive vertical</li>
 *     <li>Negative vertical</li>
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
        setTraceMapping(trace, AxisPreferences.forMotion(0));

        // Scroll left
        fakeHistory(false, false);
        execute();

        // Scroll up
        fakeHistory(true, true);
        execute();

        // Scroll right
        fakeHistory(false, true);
        execute();

        // Scroll down
        fakeHistory(true, false);
        execute();

        Assert.assertEquals(new Integer(4), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollHorizontal()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0));

        // Scroll left
        fakeHistory(false, false);
        execute();

        // Scroll right
        fakeHistory(false, true);
        execute();

        Assert.assertEquals(new Integer(2), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollHorizontalDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0));

        // Scroll down
        fakeHistory(true, false);
        execute();

        // Scroll up
        fakeHistory(true, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollVertical()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0));

        // Scroll down
        fakeHistory(true, false);
        execute();

        // Scroll up
        fakeHistory(true, true);
        execute();

        Assert.assertEquals(new Integer(2), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollVerticalDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0));

        // Scroll left
        fakeHistory(false, false);
        execute();

        // Scroll right
        fakeHistory(false, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollLeft()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0, false));

        fakeHistory(false, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollLeftDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0, false));

        fakeHistory(false, true);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollUp()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0, true));

        fakeHistory(true, true);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollUpDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0, true));

        fakeHistory(true, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollRight()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0, true));

        fakeHistory(false, true);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollRightDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forHorizontal(0, true));

        fakeHistory(false, false);
        execute();

        Assert.assertEquals(new Integer(0), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollDown()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0, false));

        fakeHistory(true, false);
        execute();

        Assert.assertEquals(new Integer(1), trace.get());
    }

    @Test
    public void testExecuteOnEventScrollDownDoesNothing()
    {
        final AtomicReference<Integer> trace = new AtomicReference<>(0);
        setTraceMapping(trace, AxisPreferences.forVertical(0, false));

        fakeHistory(true, true);
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
    private void setTraceMapping(AtomicReference<Integer> trace, AxisPreferences preferences)
    {
        final Map<String, AxisRule<Button, MouseEvent>> mapping = new HashMap<>(1);

        mapping.put("trace", new AxisRule<>(Button.MIDDLE, (event) ->
        {
            trace.getAndUpdate((count) ->
            {
                return count + 1;
            });
        }, preferences));

        mHandler.setMappings(mapping);
    }

    /**
     * <p>Inserts scrolling {@code MouseEvent}s into the history.</p>
     *
     * @param vertical true if scrolling along the y-axis, false for x-axis.
     * @param rising true to set motion as +1, false for -1.
     */
    private void fakeHistory(boolean vertical, boolean rising)
    {
        mHistory.add(MIDDLE_ORD, NO_SCROLL_EVENT);

        final float verticalB;
        final float horizontalB;

        if (vertical) {
            verticalB = (rising) ? 1f : -1f;
            horizontalB = 0f;
        } else {
            verticalB = 0f;
            horizontalB = (rising) ? 1f : -1f;
        }

        final Point offsets = new Point(horizontalB, verticalB, 0f);
        mHistory.add(MIDDLE_ORD, new MouseEvent(System.nanoTime(), new Point(), offsets));
    }
}
