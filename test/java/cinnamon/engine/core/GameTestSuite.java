package cinnamon.engine.core;

import cinnamon.engine.gfx.Canvas;
import cinnamon.engine.gfx.Scene;
import cinnamon.engine.gfx.ShaderManager;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import java.util.HashMap;
import java.util.Map;

@RunWith(Suite.class)
@SuiteClasses({GameTest.class, GameOptionalPropertiesTest.class})
public class GameTestSuite
{
    static final String TITLE = "Game Tests";

    static final String DEVELOPER = "Christian Ramos";

    static final double BUILD = 0.1d;

    static final int TICK_RATE = 30;

    public static Map<String, Object> createMinimalProperties()
    {
        final Map<String, Object> properties = new HashMap<>();

        properties.put(Game.TITLE, TITLE);
        properties.put(Game.DEVELOPER, DEVELOPER);
        properties.put(Game.BUILD, BUILD);
        properties.put(Game.TICK_RATE, TICK_RATE);

        return properties;
    }

    /**
     * Implementation with a timed lifespan. This {@code Game} stops itself after a specified duration.
     */
    static class AutoStopGame extends Game
    {
        private final Runnable mAction;

        private final long mDuration;

        private long mStart;

        /**
         * Constructs an {@code AutoStopGame} that will stop on the first update after {@link Game#start()}.
         *
         * @param canvas canvas.
         * @param properties properties.
         */
        public AutoStopGame(Canvas canvas, Map<String, Object> properties)
        {
            super(canvas, properties);
            mDuration = 0L;
            mAction = () -> {};
        }

        /**
         * Constructs an {@code AutoStopGame} to stop after some specified duration.
         *
         * @param canvas canvas.
         * @param properties properties.
         * @param duration minimum lifespan, in ms.
         */
        public AutoStopGame(Canvas canvas, Map<String, Object> properties, long duration)
        {
            this(canvas, properties, duration, () -> {});
        }

        /**
         * Constructs an {@code AutoStopGame} to stop after some specified duration while executing a
         * {@code Runnable} once per tick.
         *
         * @param canvas canvas.
         * @param properties properties.
         * @param duration minimum lifespan, in ms.
         * @param action to execute.
         */
        public AutoStopGame(Canvas canvas, Map<String, Object> properties, long duration, Runnable action)
        {
            super(canvas, properties);
            assert (duration >= 0L);
            assert (action != null);

            mDuration = duration;
            mAction = action;
        }

        @Override
        protected void onStartUp()
        {
            mStart = System.currentTimeMillis();
        }

        @Override
        protected void onTick()
        {
            mAction.run();

            if (System.currentTimeMillis() - mStart > mDuration) {
                stop();
            }
        }

        @Override
        protected void onShutDown() { }
    }

    static class TestCanvas extends Canvas
    {
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
