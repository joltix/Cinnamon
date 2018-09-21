package cinnamon.engine.core;

import cinnamon.engine.core.Game.Preference;
import cinnamon.engine.gfx.WindowTestSuite.DummyCanvas;
import cinnamon.engine.utils.TestUtils;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({GameConfigurationBuilderTest.class, GameTest.class})
public class GameTestSuite
{
    public static final String TITLE = "Game Tests";

    public static final String DEVELOPER = "Christian Ramos";

    public static final String VERSION = "0.001";

    public static final int BUILD = 0;

    public static final int TICK_RATE = 30;

    /**
     * Returns a {@code Configuration.Builder} set up to immediately produce a {@code Configuration}. The
     * {@code Preference.HIDDEN_WINDOW} is enabled.
     *
     * @return build-ready configuration builder.
     */
    public static Game.Configuration.Builder createMinimalConfigurationBuilder()
    {
        return new Game.Configuration.Builder()
                .withHeader(TITLE, DEVELOPER)
                .withVersion(VERSION, BUILD)
                .withTickRate(TICK_RATE)
                .withCanvas(new DummyCanvas())
                .withPreference(Preference.HIDDEN_WINDOW, true);
    }

    /**
     * Implementation with a timed lifespan. This {@code Game} stops itself after a specified duration.
     */
    static class AutoStopGame extends Game
    {
        private Runnable mAction;

        private final long mDuration;

        private long mStart;

        /**
         * Constructs an {@code AutoStopGame} that will stop on the first update after {@link Game#start()}.
         *
         * @param configuration configuration.
         * @throws NullPointerException if configuration is null.
         */
        public AutoStopGame(Configuration configuration)
        {
            super(configuration);
            mDuration = 0L;
            mAction = () -> {};
        }

        /**
         * Constructs an {@code AutoStopGame} to stop after some specified duration.
         *
         * @param configuration configuration.
         * @param duration minimum lifespan, in ms.
         * @throws NullPointerException if configuration is null.
         */
        public AutoStopGame(Configuration configuration, long duration)
        {
            this(configuration, duration, () -> {});
        }

        /**
         * Constructs an {@code AutoStopGame} to stop after some specified duration while executing a
         * {@code Runnable} once per tick.
         *
         * @param configuration configuration.
         * @param duration minimum lifespan, in ms.
         * @param tickAction to execute.
         * @throws NullPointerException if configuration is null.
         */
        public AutoStopGame(Configuration configuration, long duration, Runnable tickAction)
        {
            super(configuration);
            assert (duration >= 0L);
            assert (tickAction != null);

            mDuration = duration * TestUtils.NANO_PER_MILLI;
            mAction = tickAction;
        }

        /**
         * Sets a {@code Runnable} to run on each call to {@code onTick()}.
         *
         * @param tickAction action per tick.
         */
        public void setTickAction(Runnable tickAction)
        {
            mAction = tickAction;
        }

        @Override
        protected void onStartUp()
        {
            mStart = System.nanoTime();
        }

        @Override
        protected void onTick()
        {
            mAction.run();

            if (System.nanoTime() - mStart > mDuration) {
                stop();
            }
        }

        @Override
        protected void onShutDown() { }
    }
}
