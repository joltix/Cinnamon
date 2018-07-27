package cinnamon.engine.core;

import cinnamon.engine.event.ControlsSystem;
import cinnamon.engine.event.IntegratableInput;
import cinnamon.engine.gfx.Canvas;
import cinnamon.engine.gfx.Window;
import cinnamon.engine.core.Game.CoreSystem;
import cinnamon.engine.utils.Properties;

import java.util.*;

/**
 * Base class for a Cinnamon game. Games should extend this class to gain basic utilities and automate common game
 * services like input handling.
 *
 * <p>Some aspects or features can be adjusted through named values provided at construction. These properties may
 * contain modifiable user-defined named values. However, some names are reserved with an expected value type - these
 * are not modifiable through the property setters (e.g. {@code setStringProperty(String, String)}). While many of these
 * properties do not need to exist in the initial {@code Map}, four must be explicitly specified.</p>
 *
 * <h3>Required</h3>
 * <ul>
 *     <li>{@link #TITLE} : String</li>
 *     <li>{@link #DEVELOPER} : String</li>
 *     <li>{@link #BUILD} : double</li>
 *     <li>{@link #TICK_RATE} : int</li>
 * </ul>
 *
 * <h3>Optional</h3>
 * <ul>
 *     <li>{@link #VSYNC} : boolean</li>
 *     <li>{@link #WINDOW_TITLE} : String</li>
 *     <li>{@link #WINDOW_WIDTH} : int</li>
 *     <li>{@link #WINDOW_HEIGHT} : int</li>
 *     <li>{@link #WINDOW_FULLSCREEN} : boolean</li>
 *     <li>{@link #WINDOW_BORDERLESS} : boolean</li>
 *     <li>{@link #WINDOW_HIDDEN} : boolean</li>
 * </ul>
 *
 */
public abstract class Game implements Properties, WritableSystemDirectory<CoreSystem>, SystemCoordinator<CoreSystem>
{
    /**
     * Game name property.
     */
    public static final String TITLE = "title";

    /**
     * Developer name property.
     */
    public static final String DEVELOPER = "developer";

    /**
     * Game build property.
     */
    public static final String BUILD = "build";

    /**
     * Target tick rate property.
     */
    public static final String TICK_RATE = "tick_rate";

    /**
     * Vsync state property.
     */
    public static final String VSYNC = "vsync";

    /**
     * Window title property.
     */
    public static final String WINDOW_TITLE = "win_title";

    /**
     * Window width property.
     */
    public static final String WINDOW_WIDTH = "win_width";

    /**
     * Window height property.
     */
    public static final String WINDOW_HEIGHT = "win_height";

    /**
     * Window fullscreen property.
     */
    public static final String WINDOW_FULLSCREEN = "win_fullscreen";

    /**
     * Window borderless state property.
     */
    public static final String WINDOW_BORDERLESS = "win_borderless";

    /**
     * Window hidden state property.
     */
    public static final String WINDOW_HIDDEN = "win_hidden";

    /**
     * Input devices and controls mapping system.
     */
    public static final String CONTROLS_SYSTEM = "controls";

    private static final long NANOSECONDS_PER_SECOND = 1_000_000_000L;

    private static final boolean DEFAULT_WINDOW_FULLSCREEN = true;

    private static final boolean DEFAULT_WINDOW_BORDERLESS = false;

    private static final boolean DEFAULT_WINDOW_HIDDEN = false;

    private static final Map<String, Class> EXPECTED_PROPERTY_TYPES = new HashMap<>();

    // Mapping between all built-in properties and expected value's type
    static
    {
        EXPECTED_PROPERTY_TYPES.put(TITLE, String.class);
        EXPECTED_PROPERTY_TYPES.put(DEVELOPER, String.class);
        EXPECTED_PROPERTY_TYPES.put(BUILD, Double.class);
        EXPECTED_PROPERTY_TYPES.put(TICK_RATE, Integer.class);

        EXPECTED_PROPERTY_TYPES.put(WINDOW_TITLE, String.class);
        EXPECTED_PROPERTY_TYPES.put(WINDOW_WIDTH, Integer.class);
        EXPECTED_PROPERTY_TYPES.put(WINDOW_HEIGHT, Integer.class);
        EXPECTED_PROPERTY_TYPES.put(WINDOW_FULLSCREEN, Boolean.class);
        EXPECTED_PROPERTY_TYPES.put(WINDOW_BORDERLESS, Boolean.class);
        EXPECTED_PROPERTY_TYPES.put(WINDOW_HIDDEN, Boolean.class);
        EXPECTED_PROPERTY_TYPES.put(VSYNC, Boolean.class);
    }

    // Properties that must be given by subclass
    private static final List<String> REQUIRED_PROPERTIES = new ArrayList<>();

    static
    {
        REQUIRED_PROPERTIES.add(TITLE);
        REQUIRED_PROPERTIES.add(DEVELOPER);
        REQUIRED_PROPERTIES.add(BUILD);
        REQUIRED_PROPERTIES.add(TICK_RATE);
    }

    // Properties that cannot be changed through the property setters
    private static final List<String> LOCKED_PROPERTIES = new ArrayList<>();

    static
    {
        LOCKED_PROPERTIES.add(TITLE);
        LOCKED_PROPERTIES.add(DEVELOPER);
        LOCKED_PROPERTIES.add(BUILD);
        LOCKED_PROPERTIES.add(TICK_RATE);
        LOCKED_PROPERTIES.add(WINDOW_WIDTH);
        LOCKED_PROPERTIES.add(WINDOW_HEIGHT);
        LOCKED_PROPERTIES.add(WINDOW_FULLSCREEN);
        LOCKED_PROPERTIES.add(WINDOW_BORDERLESS);
        LOCKED_PROPERTIES.add(WINDOW_HIDDEN);
    }

    private final Map<String, Object> mProperties = new HashMap<>();

    private final Domain<CoreSystem> mSystems = new Domain<>(Comparator.comparingInt(BaseSystem::getPriority));

    private final Window mWindow;

    private volatile boolean mContinue = false;

    // Number of ticks measured within the last second
    private int mMeasuredTicksPerSecond = 0;

    // Number of ticks performed since "mTicksPerSecondTime"
    private int mTicksPerSecondSoFar = 0;

    /**
     * Constructs a {@code Game}.
     *
     * <p>At a minimum, some keys are required in the map and are as follows.</p>
     * <ul>
     *     <li>{@link #TITLE}</li>
     *     <li>{@link #DEVELOPER}</li>
     *     <li>{@link #BUILD}</li>
     *     <li>{@link #TICK_RATE}</li>
     * </ul>
     *
     * @param canvas canvas.
     * @param properties properties.
     * @throws NullPointerException if canvas or properties is null.
     * @throws IllegalArgumentException if a property's value has an unexpected type.
     * @throws NoSuchElementException if a required property is either not a key in the given map or a property's
     * value is null.
     */
    protected Game(Canvas canvas, Map<String, Object> properties)
    {
        checkNotNull(canvas);
        checkNotNull(properties);
        checkRequiredPropertiesExist(properties);
        checkPropertiesHaveCorrectValueTypes(properties);

        mWindow = createWindowFromProperties(canvas, properties);

        ensurePropertiesHaveValidValues(properties);
    }

    /**
     * Begins the main game loop.
     */
    public final void start()
    {
        mContinue = true;
        run();
    }

    /**
     * Stops the main game loop.
     */
    public final void stop()
    {
        mContinue = false;
    }

    /**
     * Checks if {@link #stop()} has been called.
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
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, String.class);

        final Object value = mProperties.get(name);

        if (value != null && value.getClass().isAssignableFrom(String.class)) {
            return (String) value;
        }
        return null;
    }

    @Override
    public final double getDoubleProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Double.class);

        final Object value = mProperties.get(name);

        if (value != null && value.getClass().isAssignableFrom(Double.class)) {
            return (Double) value;
        }
        return Double.NaN;
    }

    @Override
    public final int getIntegerProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Integer.class);

        final Object value = mProperties.get(name);

        if (value != null && value.getClass().isAssignableFrom(Integer.class)) {
            return (Integer) value;
        }
        return 0;
    }

    @Override
    public final boolean getBooleanProperty(String name)
    {
        checkNotNull(name);
        checkPropertyExists(name);
        checkPropertyValueTypeMatches(name, Boolean.class);

        final Object value = mProperties.get(name);

        if (value != null && value.getClass().isAssignableFrom(Boolean.class)) {
            return (Boolean) value;
        }
        return false;
    }

    @Override
    public final void setStringProperty(String name, String value)
    {
        checkNotNull(name);
        checkNotNull(value);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, String.class);

        mProperties.put(name, value);
    }

    @Override
    public final void setDoubleProperty(String name, double value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Double.class);

        mProperties.put(name, value);
    }

    @Override
    public final void setIntegerProperty(String name, int value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Integer.class);

        mProperties.put(name, value);
    }

    @Override
    public final void setBooleanProperty(String name, boolean value)
    {
        checkNotNull(name);
        checkPropertySettable(name);
        checkPropertyValueTypeMatches(name, Boolean.class);

        mProperties.put(name, value);
    }

    @Override
    public final boolean containsProperty(String name)
    {
        checkNotNull(name);

        return mProperties.containsKey(name);
    }

    /**
     * Gets the most recent measurement of the number of ticks per second.
     *
     * @return actual tick rate.
     */
    public final int getMeasuredTickRate()
    {
        return mMeasuredTicksPerSecond;
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

    @Override
    protected final Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private void run()
    {
        configureWindow();

        addBasicSystems();

        if (!getBooleanProperty(WINDOW_HIDDEN)) {
            mWindow.open();
        }

        onStartUp();
        mSystems.startSystems();

        loop();

        mSystems.stopSystems();
        onShutDown();

        mWindow.close();
        Window.terminate();
    }

    /**
     * Applies properties to the window.
     */
    private void configureWindow()
    {
        mWindow.setPositionCenter();
        mWindow.setDecorated(!getBooleanProperty(WINDOW_BORDERLESS));

        // Set resolution
        final int w = getIntegerProperty(WINDOW_WIDTH);
        final int h = getIntegerProperty(WINDOW_HEIGHT);
        mWindow.setSize(w, h);

        mWindow.setFullscreen(getBooleanProperty(WINDOW_FULLSCREEN));
    }

    private void addBasicSystems()
    {
        final IntegratableInput input = mWindow.getInput();

        addSystem(CONTROLS_SYSTEM, new ControlsSystem(Integer.MAX_VALUE, input, input, input));
    }

    /**
     * Checks all properties for behaviorally invalid values, makes changes if necessary, and sets aside the edited
     * copy.
     *
     * @param properties externally provided properties.
     */
    private void ensurePropertiesHaveValidValues(Map<String, Object> properties)
    {
        mProperties.putAll(properties);

        final int[] primRes = mWindow.getPrimaryDisplayResolution();
        constrainWindowPropertySize(primRes[0], true);
        constrainWindowPropertySize(primRes[1], false);

        selectWindowAestheticIfUnspecified();
    }

    /**
     * Creates a {@code Window} with a title either specified through the {@link #WINDOW_TITLE} property or, if
     * not given, matching the value of {@link #TITLE}. The final title will be reflected in the game's internal
     * properties.
     *
     * @param canvas canvas.
     * @param properties externally provided properties.
     * @return window.
     */
    private Window createWindowFromProperties(Canvas canvas, Map<String, Object> properties)
    {
        String title = (String) properties.get(WINDOW_TITLE);

        // Window title defaults to game title
        if (title == null) {
            title = (String) properties.get(TITLE);
            properties.put(WINDOW_TITLE, title);
        }

        return new Window(canvas, title);
    }

    /**
     * Changes either the {@link #WINDOW_WIDTH} or {@link #WINDOW_HEIGHT} property value to some maximum size if a
     * specified target size is either smaller than the window allows or larger than the given maximum.
     *
     * @param max maximum size.
     * @param width true if width should be constrained, false for height.
     */
    private void constrainWindowPropertySize(int max, boolean width)
    {
        final String property;
        final int min;

        if (width) {
            property = WINDOW_WIDTH;
            min = Window.MINIMUM_WIDTH;
        } else {
            property = WINDOW_HEIGHT;
            min = Window.MINIMUM_HEIGHT;
        }

        // Check if side is specified
        if (mProperties.containsKey(property)) {

            final int desired = (Integer) mProperties.get(property);

            // Take fullscreen if specified is invalid
            if (desired < min || desired > max) {
                mProperties.put(property, max);
            }

        } else {
            mProperties.put(property, max);
        }
    }

    /**
     * Applies the default values for the following window properties, if unspecified.
     *
     * <ul>
     *     <li>{@link #WINDOW_FULLSCREEN}</li>
     *     <li>{@link #WINDOW_BORDERLESS}</li>
     *     <li>{@link #WINDOW_HIDDEN}</li>
     * </ul>
     */
    private void selectWindowAestheticIfUnspecified()
    {
        if (!mProperties.containsKey(WINDOW_FULLSCREEN)) {
            mProperties.put(WINDOW_FULLSCREEN, DEFAULT_WINDOW_FULLSCREEN);
        }
        if (!mProperties.containsKey(WINDOW_BORDERLESS)) {
            mProperties.put(WINDOW_BORDERLESS, DEFAULT_WINDOW_BORDERLESS);
        }
        if (!mProperties.containsKey(WINDOW_HIDDEN)) {
            mProperties.put(WINDOW_HIDDEN, DEFAULT_WINDOW_HIDDEN);
        }
    }

    /**
     * Controls the game's tick timing.
     *
     * <p><b>The current loop implementation was learned from "Game Programming Patterns" by Robert Nystrom at
     * http://gameprogrammingpatterns.com/game-loop.html.</b></p>
     */
    private void loop()
    {
        final long tickDuration = NANOSECONDS_PER_SECOND / getIntegerProperty(TICK_RATE);
        long timeLastStart = System.nanoTime();
        long frameDuration = 0L;

        // Nanosecond timestamp of the current second when measuring tick rate
        long mTicksPerSecondTime = timeLastStart;

        // Begin game loop
        while (mContinue && !mWindow.isClosing()) {

            // Figure last set duration
            final long current = System.nanoTime();

            final long elapsed = current - timeLastStart;
            assert (elapsed >= 0);

            timeLastStart = current;
            frameDuration += elapsed;

            while (frameDuration >= tickDuration) {
                Window.pollEvents();
                mWindow.updateGamepads();

                onTick();

                mSystems.callWithSystems(CoreSystem::onTick);

                mTicksPerSecondSoFar++;
                final long time = System.nanoTime();

                // Measure ticks per second
                if (time - mTicksPerSecondTime >= NANOSECONDS_PER_SECOND) {
                    mMeasuredTicksPerSecond = mTicksPerSecondSoFar;
                    mTicksPerSecondSoFar = 0;
                    mTicksPerSecondTime = time;
                }

                frameDuration -= tickDuration;
            }
        }
    }

    /**
     * Checks whether or not the set properties have the required entries and throws a
     * {@link NoSuchElementException} if any are missing or invalid.
     *
     * <h3>Required properties</h3>
     * <ul>
     *     <li>{@link Game#TITLE}</li>
     *     <li>{@link Game#DEVELOPER}</li>
     *     <li>{@link Game#BUILD}</li>
     *     <li>{@link Game#TICK_RATE}</li>
     * </ul>
     *
     * @param properties properties.
     * @throws NoSuchElementException if a required property has no value.
     */
    private void checkRequiredPropertiesExist(Map<String, Object> properties)
    {
        final String format = "Game must be provided with a \"%s\" property";

        for (final String property : REQUIRED_PROPERTIES) {
            if (!properties.containsKey(property)) {
                throw new NoSuchElementException(String.format(format, property));
            }
        }
    }

    /**
     * Checks a map of properties if each property's values are both non-null and of the expected value type, if the
     * property is recognized.
     *
     * @param properties properties.
     * @throws NoSuchElementException if a property's value is null.
     * @throws IllegalArgumentException if a property's value type does not match its expected value type.
     */
    private void checkPropertiesHaveCorrectValueTypes(Map<String, Object> properties)
    {
        for (final String property : properties.keySet()) {

            final Object givenValue = properties.get(property);

            // All properties must have non-null value
            if (givenValue == null) {
                final String format = "Property \"%s\" has no value";
                throw new NoSuchElementException(String.format(format, property));
            }

            final Class expectedType = EXPECTED_PROPERTY_TYPES.get(property);

            // Check given value type against expected
            if (expectedType != null && expectedType != givenValue.getClass()) {
                final String format = "Property \"%s\" expects %s, actual: %s";
                final String expectedName = expectedType.getSimpleName();
                final String actualName = givenValue.getClass().getSimpleName();
                throw new IllegalArgumentException(String.format(format, property, expectedName, actualName));
            }
        }
    }

    private void checkPropertyExists(String name)
    {
        if (!mProperties.containsKey(name)) {
            final String format = "No such property named \"%s\"";
            throw new NoSuchElementException(String.format(format,name));
        }
    }

    private void checkPropertySettable(String name)
    {
        if (LOCKED_PROPERTIES.contains(name)) {
            final String format = "Property \"%s\" is not settable";
            throw new IllegalArgumentException(String.format(format, name));
        }
    }

    /**
     * Checks if a property's expected value type matches a given type.
     *
     * @param name property name.
     * @param type value type to test.
     * @throws IllegalArgumentException if a property's value type does not match its expected value type.
     */
    private void checkPropertyValueTypeMatches(String name, Class type)
    {
        final Class actualType  = mProperties.get(name).getClass();

        if (actualType != type) {
            final String format = "Property \"%s\" accepts %s values, given: %s";
            final String actualName = actualType.getSimpleName();
            final String typeName = type.getSimpleName();
            throw new IllegalArgumentException(String.format(format, name, actualName, typeName));
        }
    }

    private void checkNotNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Acts as a basis for implementing wide-reaching features.
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
