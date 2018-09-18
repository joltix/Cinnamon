package cinnamon.engine.gfx;

import cinnamon.engine.gfx.WindowTestSuite.DummyCanvas;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.glfw.GLFW;

public class CanvasTest
{
    private Canvas mCanvas;

    @Before
    public void setUp()
    {
        mCanvas = new DummyCanvas();
        mCanvas.startUp();
    }

    @After
    public void tearDown()
    {
        GLFW.glfwTerminate();
        mCanvas = null;
    }

    @Test
    public void testGetWidthReturnsZeroBeforeInitialResize()
    {
        Assert.assertEquals(0, mCanvas.getWidth());
    }

    @Test
    public void testGetWidthReturnsWidthAfterResize()
    {
        mCanvas.resize(343, 1);

        Assert.assertEquals(343, mCanvas.getWidth());
    }

    @Test
    public void testGetHeightReturnsZeroBeforeInitialResize()
    {
        Assert.assertEquals(0, mCanvas.getHeight());
    }

    @Test
    public void testGetHeightReturnsHeightAfterResize()
    {
        mCanvas.resize(1, 343);

        Assert.assertEquals(343, mCanvas.getHeight());
    }
}
