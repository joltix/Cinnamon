package cinnamon.engine.core;

import cinnamon.engine.event.ControlsSystem;
import cinnamon.engine.event.IntegratableInput;
import cinnamon.engine.gfx.Canvas;
import cinnamon.engine.gfx.Monitor;
import cinnamon.engine.gfx.Window;
import cinnamon.engine.core.Game.CoreSystem;
import cinnamon.engine.utils.Assets;
import cinnamon.engine.utils.Properties;
import cinnamon.engine.utils.PropertyMap;
import cinnamon.engine.utils.LoopMeasure;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Function;

/**
 * Base class for a Cinnamon game. Games should extend this class to gain basic utilities and automate common game
 * services.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *     <li>configuration</li>
 *     <li>start up</li>
 *     <li>systems loop</li>
 *     <li>shut down</li>
 * </ol>
 *
 * <p>A {@code Game} makes adjustments to built-in systems and features based on the {@link Configuration} provided
 * during creation. This is also the point where critical, unchangeable, values are set such as the game's target tick
 * rate, though not all submitted during this point are immutable.</p>
 *
 * <p>{@link #onStartUp()} and {@link #onShutDown()} callbacks surround the core game loop for set up and tear down.</p>
 *
 * <p>Major game-specific systems are given the opportunity to execute once per tick. Implementations may insert
 * their own systems with {@link #addSystem(String, CoreSystem)}.</p>
 *
 * <p>If no monitors are detected or a resolution smaller than {@link Window#MINIMUM_WIDTH} x
 * {@link Window#MINIMUM_HEIGHT} is specified, the game will run as if {@link Preference#HIDDEN_WINDOW} was set to
 * {@code true}.</p>
 *
 * <h3>Configuration</h3>
 * <p>While some configuration steps are necessary, as shown below, {@link Preference}s can optionally be specified.
 * These preferences' default behaviors are outlined in their documentation.</p>
 *
 * <pre>
 *     <code>
 *
 *         Game.Configuration config = new Game.Configuration.Builder()
 *             .withHeader("Title", "Developer")
 *             .withVersion("0.343a", 42)
 *             .withTickRate(30)
 *             .withCanvas(new CanvasImpl())
 *
 *             // Preference calls are optional
 *             .withPreference(Game.Preference.FULLSCREEN, true)
 *             .withPreference(Game.Preference.RESOLUTION_X, 1366)
 *             .withPreference(Game.Preference.RESOLUTION_Y, 768)
 *
 *             .build();
 *
 *         new GameImpl(config).start();
 *     </code>
 * </pre>
 *
 * <p>The header, version, and tick rate will be available during the game's runtime through property getters (e.g.
 * {@code getStringProperty(Game.TITLE)}).</p>
 *
 * <h3>Concurrency</h3>
 * <p>Most methods are expected to be called on the main thread. Methods not explicitly documenting safety should be
 * presumed to expect main thread execution.</p>
 */
public abstract class Game implements Properties, WritableSystemDirectory<CoreSystem>, SystemCoordinator<CoreSystem>
{
    /**
     * Configurable features to be set during a {@link Configuration}'s construction.
     *
     * <p>The default values for each {@code Preference} is as follows (expected value types are noted in each
     * {@code Preference}'s documentation).</p>
     * <ul>
     *     <li>{@link #MONITOR}: primary monitor</li>
     *     <li>{@link #VSYNC}: true</li>
     *     <li>{@link #FULLSCREEN}: true</li>
     *     <li>{@link #BORDERLESS}: false</li>
     *     <li>{@link #HIDDEN_WINDOW}: false</li>
     *     <li>{@link #RESOLUTION_X}: primary monitor's width</li>
     *     <li>{@link #RESOLUTION_Y}: primary monitor's height</li>
     *     <li>{@link #ICON}: none</li>
     * </ul>
     */
    public enum Preference
    {
        /**
         * Monitor to display on.
         *
         * <p>Monitor indices begin at 0 (the primary monitor) and increment.</p>
         *
         * <p>This preference expects an {@code int}.</p>
         */
        MONITOR(0, (value) ->
        {
            return Preference.isNotNull(value) && ((int) value) >= 0;
        }),

        /**
         * Enables vertical synchronization.
         *
         * <p>This preference expects a {@code boolean}.</p>
         */
        VSYNC(true, Preference::isNotNull),

        /**
         * Makes the game fullscreen.
         *
         * <p>If the {@link #MONITOR} preference has been specified, the game will go fullscreen on the specified
         * monitor.</p>
         *
         * <p>This preference expects a {@code boolean}.</p>
         */
        FULLSCREEN(true, Preference::isNotNull),

        /**
         * Removes the window's frame and title bar.
         *
         * <p>This preference expects a {@code boolean}.</p>
         */
        BORDERLESS(false, Preference::isNotNull),

        /**
         * Hides the window; rendering will not begin.
         *
         * <p>This preference expects a {@code boolean}.</p>
         */
        HIDDEN_WINDOW(false, Preference::isNotNull),

        /**
         * Horizontal resolution.
         *
         * <p>This preference expects an {@code int}.</p>
         */
        RESOLUTION_X(-1, (value) ->
        {
            final Integer x = (Integer) value;
            return Preference.isNotNull(x) && x >= Window.MINIMUM_WIDTH;
        }),

        /**
         * Vertical resolution.
         *
         * <p>This preference expects an {@code int}.</p>
         */
        RESOLUTION_Y(-1, (value) ->
        {
            final Integer y = (Integer) value;
            return Preference.isNotNull(y) && y >= Window.MINIMUM_HEIGHT;
        }),

        /**
         * Path to task and title bar icon resource.
         *
         * <p>This preference expects a {@code String}.</p>
         */
        ICON("", Preference::isNotNull);

        // Returns true if user-specified value is valid for the preference
        private final Function<Object, Boolean> mValidator;

        private final Object mDefaultValue;

        Preference(Object defaultValue, Function<Object, Boolean> valueValidator)
        {
            mDefaultValue = defaultValue;
            mValidator = valueValidator;
        }

        /**
         * Returns the given value if it is valid for the preference. Otherwise, the preference's default value is
         * returned.
         *
         * @param value value.
         * @return value if valid, otherwise default value.
         */
        private Object autoCorrect(Object value)
        {
            return (mValidator.apply(value)) ? value : mDefaultValue;
        }

        private Class getType()
        {
            return mDefaultValue.getClass();
        }

        private static Map<String, Class> getExpectedTypeMapping()
        {
            final Map<String, Class> expectations = new HashMap<>();
            for (final Preference pref : Preference.values()) {
                expectations.put(pref.toString(), pref.getType());
            }
            return expectations;
        }

        private static boolean isNotNull(Object object)
        {
            return object != null;
        }
    }

    /**
     * Game name property.
     */
    public static final String TITLE = "title";

    /**
     * Developer name property.
     */
    public static final String DEVELOPER = "developer";

    /**
     * Game version property.
     */
    public static final String VERSION = "version";

    /**
     * Game build property.
     */
    public static final String BUILD = "build";

    /**
     * Target tick rate property.
     */
    public static final String TICK_RATE = "tick_rate";

    /**
     * Input devices and controls mapping system.
     */
    public static final String CONTROLS_SYSTEM = "controls";

    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    private final PropertyMap mProperties;

    private final Domain<CoreSystem> mSystems = new Domain<>(Comparator.comparingInt(BaseSystem::getPriority));

    private final Configuration mConfig;

    private Window mWindow;

    private volatile boolean mContinue = false;

    private final LoopMeasure mMeasure = new LoopMeasure(3);

    /**
     * Constructs a {@code Game}.
     *
     * <p>If the configuration requests an unusable preference value, the request is ignored.</p>
     *
     * @param configuration configuration.
     * @throws NullPointerException if configuration is null.
     */
    protected Game(Game.Configuration configuration)
    {
        checkNotNull(configuration);

        mConfig = configuration;

        final Map<String, Object> props = configuration.mProps;
        mProperties = new PropertyMap(props, Preference.getExpectedTypeMapping(), props.keySet());
    }

    /**
     * Begins the game.
     *
     * <p>{@link #onStartUp()} will be called for games to initialize resources.</p>
     *
     * @throws IllegalStateException if window creation fails.
     */
    public final void start()
    {
        mWindow = new Window(mConfig.mCanvas);
        mContinue = true;

        configureWindow();
        installBasicSystems();

        run();
    }

    /**
     * Stops the game.
     *
     * <p>{@link #onShutDown()} will be called for games to perform clean up.</p>
     *
     * <p>This method may be called from any thread.</p>
     */
    public final void stop()
    {
        mContinue = false;
    }

    /**
     * Checks if the game is ongoing. This method returns {@code false} once {@link #stop()} is called.
     *
     * <p>This method may be called from any thread.</p>
     *
     * @return true if the game continues.
     */
    public final boolean isRunning()
    {
        return mContinue;
    }

    @Override
    public final void pauseSystem(String name, int reason)
    {
        mSystems.pauseSystem(name, reason);
    }

    @Override
    public final void resumeSystem(String name, int reason)
    {
        mSystems.resumeSystem(name, reason);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is not allowed to be executed from the following callbacks and will throw an
     * {@code IllegalStateException} on attempts to do so.</p>
     *
     * <ul>
     *     <li>{@link CoreSystem#onTick()}</li>
     *     <li>{@link CoreSystem#onStart()}</li>
     *     <li>{@link CoreSystem#onStop()}</li>
     *     <li>{@link CoreSystem#onPause(int)}</li>
     *     <li>{@link CoreSystem#onResume(int)}</li>
     * </ul>
     *
     * @param name name.
     * @param system system.
     * @throws NullPointerException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @throws IllegalStateException if this method is called from an incompatible callback.
     */
    @Override
    public final void addSystem(String name, CoreSystem system)
    {
        mSystems.addSystem(name, system);
    }

    /**
     * {@inheritDoc}
     *
     * <p>This method is not allowed to be executed from the following callbacks and will throw an
     * {@code IllegalStateException} on attempts to do so.</p>
     *
     * <ul>
     *     <li>{@link CoreSystem#onTick()}</li>
     *     <li>{@link CoreSystem#onStart()}</li>
     *     <li>{@link CoreSystem#onStop()}</li>
     *     <li>{@link CoreSystem#onPause(int)}</li>
     *     <li>{@link CoreSystem#onResume(int)}</li>
     * </ul>
     *
     * @param name name.
     * @throws NullPointerException {@inheritDoc}
     * @throws NoSuchElementException {@inheritDoc}
     * @throws IllegalStateException if this method is called from an incompatible callback.
     */
    @Override
    public final void removeSystem(String name)
    {
        mSystems.removeSystem(name);
    }

    @Override
    public final CoreSystem getSystem(String name)
    {
        return mSystems.getSystem(name);
    }

    @Override
    public final String getStringProperty(String name)
    {
        return mProperties.getStringProperty(name);
    }

    @Override
    public final double getDoubleProperty(String name)
    {
        return mProperties.getDoubleProperty(name);
    }

    @Override
    public final int getIntegerProperty(String name)
    {
        return mProperties.getIntegerProperty(name);
    }

    @Override
    public final boolean getBooleanProperty(String name)
    {
        return mProperties.getBooleanProperty(name);
    }

    @Override
    public final void setStringProperty(String name, String value)
    {
        mProperties.setStringProperty(name, value);
    }

    @Override
    public final void setDoubleProperty(String name, double value)
    {
        mProperties.setDoubleProperty(name, value);
    }

    @Override
    public final void setIntegerProperty(String name, int value)
    {
        mProperties.setIntegerProperty(name, value);
    }

    @Override
    public final void setBooleanProperty(String name, boolean value)
    {
        mProperties.setBooleanProperty(name, value);
    }

    @Override
    public final boolean containsProperty(String name)
    {
        return mProperties.containsProperty(name);
    }

    /**
     * Gets the most recent tick rate.
     *
     * <p>This is a measurement of the actual tick rate and is not necessarily the same as the target rate specified
     * at construction.</p>
     *
     * @return measured ticks per second.
     */
    public final int getTickRate()
    {
        return mMeasure.getRate();
    }

    /**
     * Gets the average tick duration.
     *
     * @return duration in milliseconds.
     */
    public final double getTickDuration()
    {
        return mMeasure.getAverageDuration();
    }

    /**
     * Gets the shortest recent tick duration.
     *
     * @return shortest duration in milliseconds.
     */
    public final double getMinimumTickDuration()
    {
        return mMeasure.getMinimumDuration();
    }

    /**
     * Gets the longest recent tick duration.
     *
     * @return longest duration in milliseconds.
     */
    public final double getMaximumTickDuration()
    {
        return mMeasure.getMaximumDuration();
    }

    /**
     * Called once after {@link #start()} and before {@link #onTick()}.
     */
    protected abstract void onStartUp();

    /**
     * Called once per game tick prior to any system.
     */
    protected abstract void onTick();

    /**
     * Called once after {@link #stop()}.
     */
    protected abstract void onShutDown();

    /**
     * Called when the window is asked to close through the title bar's close button. The default implementation
     * stops the game.
     */
    protected void onWindowClose()
    {
        stop();
    }

    @Override
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void run()
    {
        onStartUp();

        if (!mConfig.getBoolean(Preference.HIDDEN_WINDOW)) {
            mWindow.open();
        }

        mSystems.startSystems();
        loop();
        mSystems.stopSystems();

        onShutDown();

        Window.terminate();
        Monitor.terminate();
    }

    /**
     * Configures the {@code Window} based off {@code Preference}s. After this completes, the {@code Window} just
     * needs to be opened.
     */
    private void configureWindow()
    {
        mWindow.setVsync(mConfig.getBoolean(Preference.VSYNC));
        mWindow.setDecorated(!mConfig.getBoolean(Preference.BORDERLESS));
        attemptToSetWindowIcon();

        final Monitor[] monitors = Monitor.getConnectedMonitors();
        if (monitors.length > 0) {

            // Select monitor to place on
            final int monitorIndex = mConfig.getInteger(Preference.MONITOR);
            final Monitor monitor = (monitorIndex >= monitors.length) ? monitors[0] : monitors[monitorIndex];

            final int w = mConfig.getInteger(Preference.RESOLUTION_X);
            final int h = mConfig.getInteger(Preference.RESOLUTION_Y);

            // Take monitor's resolution if desired resolution was invalid
            if (w < 0 || h < 0) {
                mWindow.setSize(monitor.getWidth(), monitor.getHeight());
            } else {
                mWindow.setSize(w, h);
            }

            // Position window
            if (mConfig.getBoolean(Preference.FULLSCREEN)) {
                mWindow.setFullscreen(monitor);
            } else {
                mWindow.setPositionCenter(monitor);
            }
        } else {
            // Choose arbitrary size and position since window won't be shown
            mWindow.setSize(Window.MINIMUM_WIDTH, Window.MINIMUM_HEIGHT);
            mWindow.setPosition(0, 0);
            mConfig.mPrefs.put(Preference.HIDDEN_WINDOW.toString(), true);
        }

        // Let game callback override close requests
        mWindow.setCloseCallback(() ->
        {
            onWindowClose();
            return true;
        });

        mWindow.setTitle(mProperties.getStringProperty(TITLE));
    }

    /**
     * Tries to set the window's icon from the path given as {@link Preference#ICON}. Any {@code Exception} thrown
     * during this attempt will be caught; in this case, no icon is set.
     */
    private void attemptToSetWindowIcon()
    {
        final String iconPath = mConfig.getString(Preference.ICON);

        try {
            final byte[] bytes = Assets.loadResource(iconPath, Assets.BYTE_ARRAY);

            final ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);;
            buffer.put(bytes);
            buffer.flip();

            // Format colors
            final int[] width = new int[1];
            final int[] height = new int[1];
            final int[] channels = new int[1];
            final ByteBuffer pulled = STBImage.stbi_load_from_memory(buffer, width, height, channels, 4);

            MemoryUtil.memFree(buffer);

            if (pulled != null) {
                mWindow.setIcon(pulled);
            }

        } catch (Exception e) { }
    }

    private void installBasicSystems()
    {
        final IntegratableInput input = mWindow.getInput();

        addSystem(CONTROLS_SYSTEM, new ControlsSystem(Integer.MAX_VALUE, input, input, input));
    }

    /**
     * Begins the main game loop.
     *
     * <p>The current loop implementation was learned from "Game Programming Patterns" by Robert Nystrom at
     * http://gameprogrammingpatterns.com/game-loop.html.</p>
     */
    private void loop()
    {
        final int desiredRate = getIntegerProperty(Game.TICK_RATE);
        final long desiredTickDuration = NANOSECONDS_PER_SECOND / desiredRate;

        long lastStartTimestamp = System.nanoTime();
        long tickDuration = 0L;

        mMeasure.markLoopBegins(lastStartTimestamp);

        while (mContinue) {

            final long now = System.nanoTime();

            // Should be monotonic
            assert (now - lastStartTimestamp >= 0);

            // Measure how long last tick took
            tickDuration += now - lastStartTimestamp;
            lastStartTimestamp = now;

            // Game steps forward so long as time is available
            while (tickDuration >= desiredTickDuration) {

                mMeasure.markIterationBegins(System.nanoTime());
                Window.pollEvents();

                // Process game
                onTick();
                mSystems.callWithSystems(CoreSystem::onTick);

                tickDuration -= desiredTickDuration;
                mMeasure.markIterationEnds(System.nanoTime());
            }
        }
    }

    private static void checkStringNotBlank(String name, String string)
    {
        if (string.trim().isEmpty()) {
            final String format = "%s cannot be empty or made of whitespace";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    private static void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Carries various initialization details for a {@code Game}'s startup such as the title, version, and
     * runtime tweaks.
     *
     * <p>{@code Configuration}s are built step-by-step with a {@code Configuration.Builder}.</p>
     */
    public static final class Configuration
    {
        // String keys are preferred here over enums to eventually allow enabling undocumented experimental features.
        // Such features can be enabled by manually entering its corresponding String key without explicitly listing
        // it as an option

        private final Map<String, Object> mProps = new HashMap<>();

        private final Map<String, Object> mPrefs = new HashMap<>();

        private Canvas mCanvas;

        private Configuration() { }

        private Configuration(Configuration configuration)
        {
            mProps.putAll(configuration.mProps);
            mPrefs.putAll(configuration.mPrefs);
            mCanvas = configuration.mCanvas;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException
        {
            throw new CloneNotSupportedException();
        }

        private String getString(Preference preference)
        {
            return (String) preference.autoCorrect(mPrefs.get(preference.toString()));
        }

        private int getInteger(Preference preference)
        {
            return (int) preference.autoCorrect(mPrefs.get(preference.toString()));
        }

        private boolean getBoolean(Preference preference)
        {
            return (boolean) preference.autoCorrect(mPrefs.get(preference.toString()));
        }

        /**
         * Progressively builds a {@link #Configuration}.
         *
         * <p>The following is the minimum needed to produce a {@code Configuration}.</p>
         * <pre>
         *     <code>
         *
         *         Configuration config = new Game.Configuration.Builder()
         *             .withHeader("Title", "Developer")
         *             .withVersion("0.343a", 42)
         *             .withTickRate(30)
         *             .withCanvas(new CanvasImpl())
         *             .build();
         *     </code>
         * </pre>
         */
        public static final class Builder
        {
            private Configuration mConfig;

            /**
             * Builds a {@code Configuration}.
             *
             * <p>Repeated calls to this method will produce instances with the same information.</p>
             *
             * @throws IllegalStateException if header, version, tick rate, or a canvas was not specified.
             */
            public Configuration build()
            {
                if (mConfig == null || !mConfig.mProps.containsKey(Game.TITLE)) {
                    throw new IllegalStateException("Title and developer were not specified");
                }
                if (!mConfig.mProps.containsKey(Game.VERSION)) {
                    throw new IllegalStateException("Version and build were not specified");
                }
                if (!mConfig.mProps.containsKey(Game.TICK_RATE)) {
                    throw new IllegalStateException("Tick rate was not specified");
                }
                if (mConfig.mCanvas == null) {
                    throw new IllegalStateException("Canvas was not specified");
                }

                return new Configuration(mConfig);
            }

            /**
             * Specifies the name and developer.
             *
             * @param title title.
             * @param developer developer.
             * @return builder.
             * @throws NullPointerException if title or developer is null.
             * @throws IllegalArgumentException if title or developer is empty or whitespace.
             */
            public Builder withHeader(String title, String developer)
            {
                checkNotNull(title);
                checkNotNull(developer);
                checkStringNotBlank("Game title", title);
                checkStringNotBlank("Game developer", developer);
                ensureConfigurationExists();

                mConfig.mProps.put(Game.TITLE, title);
                mConfig.mProps.put(Game.DEVELOPER, developer);
                return this;
            }

            /**
             * Specifies the version and build number.
             *
             * @param version outward facing version.
             * @param build internal build number.
             * @return builder.
             * @throws NullPointerException if version is null.
             * @throws IllegalArgumentException if version is empty or whitespace or build is {@literal <} 0.
             */
            public Builder withVersion(String version, int build)
            {
                checkNotNull(version);
                checkStringNotBlank("Game version", version);

                if (build < 0) {
                    throw new IllegalArgumentException("Game build number must be >= 0");
                }
                ensureConfigurationExists();

                mConfig.mProps.put(Game.VERSION, version);
                mConfig.mProps.put(Game.BUILD, build);
                return this;
            }

            /**
             * Specifies the target tick rate.
             *
             * @param rate tick rate.
             * @return builder.
             * @throws IllegalArgumentException if rate < 1.
             */
            public Builder withTickRate(int rate)
            {
                if (rate < 1) {
                    final String format = "Tick rate must be >= 1, given: %d";
                    throw new IllegalArgumentException(String.format(format, rate));
                }
                ensureConfigurationExists();

                mConfig.mProps.put(Game.TICK_RATE, rate);
                return this;
            }

            /**
             * Specifies the renderer.
             *
             * @param canvas renderer.
             * @return builder.
             * @throws NullPointerException if canvas is null.
             */
            public Builder withCanvas(Canvas canvas)
            {
                checkNotNull(canvas);
                ensureConfigurationExists();

                mConfig.mCanvas = canvas;
                return this;
            }

            /**
             * Specifies a {@code String} value for a launch preference.
             *
             * @param key key.
             * @param value value.
             * @return builder.
             * @throws NullPointerException if key or value is null.
             * @throws IllegalArgumentException if the preference does not expect a {@code String} value.
             */
            public Builder withPreference(Preference key, String value)
            {
                checkNotNull(key);
                checkNotNull(value);
                checkExpectedType(key, value);
                ensureConfigurationExists();

                mConfig.mPrefs.put(key.toString(), value);
                return this;
            }

            /**
             * Specifies an {@code int} value for a launch preference.
             *
             * @param key key.
             * @param value value.
             * @return builder.
             * @throws NullPointerException if key is null.
             * @throws IllegalArgumentException if the preference does not expect an {@code int} value.
             */
            public Builder withPreference(Preference key, int value)
            {
                checkNotNull(key);
                checkExpectedType(key, value);
                ensureConfigurationExists();

                mConfig.mPrefs.put(key.toString(), value);
                return this;
            }

            /**
             * Specifies a {@code boolean} value for a launch preference.
             *
             * @param key key.
             * @param value value.
             * @return builder.
             * @throws NullPointerException if key is null.
             * @throws IllegalArgumentException if the preference does not expect a {@code boolean} value.
             */
            public Builder withPreference(Preference key, boolean value)
            {
                checkNotNull(key);
                checkExpectedType(key, value);
                ensureConfigurationExists();

                mConfig.mPrefs.put(key.toString(), value);
                return this;
            }

            @Override
            protected Object clone() throws CloneNotSupportedException
            {
                throw new CloneNotSupportedException();
            }

            private void ensureConfigurationExists()
            {
                if (mConfig == null) {
                    mConfig = new Configuration();
                }
            }

            private void checkExpectedType(Preference key, Object value)
            {
                if (value != null && value.getClass() != key.getType()) {

                    final String format = "Preference expects a %s value but given: %s";
                    final String expName = key.getType().getSimpleName();
                    final String actName = value.getClass().getSimpleName();

                    throw new IllegalArgumentException(String.format(format, expName, actName));
                }
            }
        }
    }

    /**
     * Basis for implementing wide-reaching features.
     */
    public static abstract class CoreSystem extends BaseSystem
    {
        /**
         * Constructs a {@code CoreSystem}.
         *
         * @param priority priority.
         */
        protected CoreSystem(int priority)
        {
            super(priority);
        }

        /**
         * Called once per game tick.
         */
        protected abstract void onTick();
    }
}
