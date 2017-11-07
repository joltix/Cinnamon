package cinnamon.engine.gfx;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({WindowTest.class, WindowMultiInstanceTest.class})
public class WindowTestSuite
{
    /**
     * <p>Holds all {@code Windows} open for the given number of milliseconds and executes a {@code Runnable} once
     * after a specified delay elapses.</p>
     *
     * @param oneShot to execute, or null if just holding window open.
     * @param executionDelay delay until execution in ms.
     * @param openDuration how long to hold open in ms.
     */
    static void keepOpenAndExecute(Runnable oneShot, long executionDelay, long openDuration)
    {
        if (executionDelay >= openDuration) {
            throw new IllegalArgumentException("Execution delay must be < length of time to keep Window open");
        }

        final long start = System.currentTimeMillis();
        long soFar;
        boolean executed = false;

        while ((soFar = System.currentTimeMillis() - start) <= openDuration) {
            Window.pollEvents();

            if (oneShot != null && soFar >= executionDelay && !executed) {
                executed = true;
                oneShot.run();
            }
        }

        // In case delay is so little the loop didn't execute
        if (oneShot != null && !executed) {
            oneShot.run();
        }
    }

    /**
     * <p>Holds all {@code Windows} open for the given number of milliseconds and executes a {@code Runnable} repeatedly
     * after a specified delay.</p>
     *
     * @param oneShot to execute.
     * @param executionDelay delay until execution in ms.
     * @param openDuration how long to hold open in ms.
     * @throws NullPointerException if oneShot is null.
     */
    static void keepOpenAndExecuteRepeatedly(Runnable oneShot, long executionDelay, long openDuration)
    {
        if (oneShot == null) {
            throw new NullPointerException();
        }
        if (executionDelay >= openDuration) {
            throw new IllegalArgumentException("Execution delay must be < length of time to keep Window open");
        }

        final long start = System.currentTimeMillis();
        long soFar;
        boolean executed = false;

        while ((soFar = System.currentTimeMillis() - start) <= openDuration) {
            Window.pollEvents();

            if (soFar >= executionDelay) {
                executed = true;
                oneShot.run();
            }
        }

        // In case delay is so little the loop didn't execute
        if (!executed) {
            oneShot.run();
        }
    }

    /**
     * <p>Skeletal {@code Canvas} for use with {@code Window} tests. This implementation does not explicitly render
     * anything. As such, the output screen will simply be the default color (typically black).</p>
     */
    static class MockCanvas extends Canvas
    {
        public MockCanvas() { }

        @Override
        protected void onInitialize() { }

        @Override
        protected void onTerminate() { }

        @Override
        protected void draw(Scene scene, ShaderManager shaders) { }

        @Override
        protected void onResize() { }

        @Override
        protected float[] createProjectionMatrix()
        {
            return new float[0];
        }

        @Override
        protected Scene createScene()
        {
            return null;
        }
    }
}
