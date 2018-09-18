package cinnamon.engine.gfx;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.lwjgl.opengl.GL11;

@RunWith(Suite.class)
@Suite.SuiteClasses({WindowTest.class, WindowLoopStyleTest.class, WindowMultiInstanceTest.class})
public class WindowTestSuite
{
    /**
     * Skeletal {@code Canvas}. This implementation does not explicitly render anything. As such, the output screen
     * will simply be the default color (typically black).
     */
    public static class DummyCanvas extends Canvas
    {
        public DummyCanvas()
        {
            super();
        }

        @Override
        protected void onStartUp() { }

        @Override
        protected void onShutDown() { }

        @Override
        protected void onDraw() { }

        @Override
        protected void onResize() { }
    }

    /**
     * {@code Canvas} implementation that changes the background color over time.
     */
    public static class ColorFadeCanvas extends Canvas
    {
        private final double mAdditive;

        private double mRed = Math.random();

        private double mGreen = Math.random();

        private double mBlue = Math.random();

        /**
         * Constructs a {@code ColorFadeCanvas} to step through colors by a given amount.
         *
         * @param speed normalized color change speed.
         */
        public ColorFadeCanvas(double speed)
        {
            assert (speed > 0d && speed <= 1d);

            mAdditive = 0.01d * speed;
        }

        @Override
        protected void onStartUp() { }

        @Override
        protected void onShutDown() { }

        @Override
        protected void onDraw()
        {
            mRed += mAdditive;
            mGreen += mAdditive;
            mBlue += mAdditive;

            final float r = (float) (0.5d * Math.sin(mRed) + 0.5d);
            final float g = (float) (0.5d * Math.sin(mGreen - 1.5d) + 0.5d);
            final float b = (float) (0.5d * Math.sin(mBlue - 3d) + 0.5d);

            GL11.glClearColor(r, g, b, 1f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }

        @Override
        protected void onResize() { }
    }
}
