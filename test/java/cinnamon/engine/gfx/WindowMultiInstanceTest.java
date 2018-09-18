package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.ColorFadeCanvas;
import cinnamon.engine.gfx.WindowTestSuite.DummyCanvas;
import cinnamon.engine.utils.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Supplier;

public class WindowMultiInstanceTest
{
    private static final double COLOR_FADE_SPEED = 0.5d;

    // How long to hold windows open for, in milliseconds
    private static final long DURATION = 8_000L;

    // Fractional denominator for horizontal space between the grid's side and the display's edge
    private static final int HORIZONTAL_GRID_MARGIN = 12;

    // Fractional denominator for vertical space between the grid's side and the display's edge
    private static final int VERTICAL_GRID_MARGIN = 12;

    private Monitor mMonitor;

    @Before
    public void setUp()
    {
        mMonitor = Monitor.getPrimaryMonitor();
    }

    @After
    public void tearDown()
    {
        Window.terminate();
    }

    @Test
    public void testManyOpenWindows()
    {
        final Window[] windows = openWindowsInGrid(3, 3, DummyCanvas::new);

        TestUtils.loop(DURATION, () ->
        {
            Window.pollEvents();

            updateAllWindowTitlesWithFrameRates(windows);
        });
    }

    @Test
    public void testManyOpenWindowsWithRendering()
    {
        final Window[] windows = openWindowsInGrid(3, 3, () ->
        {
            return new ColorFadeCanvas(COLOR_FADE_SPEED);
        });

        TestUtils.loop(DURATION, () ->
        {
            Window.pollEvents();

            updateAllWindowTitlesWithFrameRates(windows);
        });
    }

    @Test
    public void testOpenWindows()
    {
        final Window[] windows = openWindowsInGrid(2, 1, DummyCanvas::new);

        TestUtils.loop(DURATION, () ->
        {
            Window.pollEvents();

            updateAllWindowTitlesWithFrameRates(windows);
        });
    }

    @Test
    public void testOpenWindowsWithRendering()
    {
        final Window[] windows = openWindowsInGrid(2, 1, () ->
        {
            return new ColorFadeCanvas(COLOR_FADE_SPEED);
        });

        TestUtils.loop(DURATION, () ->
        {
            Window.pollEvents();

            updateAllWindowTitlesWithFrameRates(windows);
        });
    }

    /**
     * Creates and opens a grid of windows.
     *
     * @param columns number of columns.
     * @param rows number of rows.
     * @param supplier instantiates a new canvas.
     * @return windows.
     */
    private Window[] openWindowsInGrid(int columns, int rows, Supplier<Canvas> supplier)
    {
        final int monW = mMonitor.getWidth();
        final int monH = mMonitor.getHeight();
        final int marginH = monW / HORIZONTAL_GRID_MARGIN;
        final int marginV = monW / VERTICAL_GRID_MARGIN;

        final int gridWidth = monW - (marginH * 2);
        final int gridHeight = monH - (marginV * 2);

        // Compute window width and height
        final int colWidth = gridWidth / columns;
        final int rowHeight = gridHeight / rows;

        final Window[] windows = new Window[columns * rows];

        for (int i = 0; i < windows.length; i++) {

            final Window window = new Window(supplier.get());

            final int top = window.getFrameExtents()[1];
            window.setSize(colWidth, rowHeight - top);

            // Position within grid
            final int x = i % columns * colWidth + marginH;
            final int y = i / columns * rowHeight + top + marginV;
            window.setPosition(x, y);

            windows[i] = window;
        }

        for (final Window window : windows) {
            window.open();
        }

        return windows;
    }

    private void updateAllWindowTitlesWithFrameRates(Window[] windows)
    {
        for (int i = 0; i < windows.length; i++) {
            final int rate = windows[i].getCanvas().getFrameRate();
            windows[i].setTitle(String.valueOf(rate));
        }
    }
}
