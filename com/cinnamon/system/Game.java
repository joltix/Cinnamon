package com.cinnamon.system;

import com.cinnamon.gfx.*;
import com.cinnamon.object.*;
import com.cinnamon.system.EventDispatcher.EventFilter;
import com.cinnamon.system.InputEvent.Action;
import com.cinnamon.system.MouseEvent.Button;
import com.cinnamon.utils.Point2F;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     This class is responsible for not only controlling the game's tick rate but also providing properties and
 *     services to different parts of the game. Subclasses may, for example, retrieve systems such as
 *     {@link #getControlMap()} to change keyboard or mouse control bindings.
 * </p>
 */
public abstract class Game<E extends GObject>
{
    /**
     * <p>Constant value for enabling a property whose expected value is a toggle.</p>
     */
    public static final String PROPERTY_ENABLE = "enable";

    /**
     * <p>Constant value for disabling a property whose expected value is a toggle.</p>
     */
    public static final String PROPERTY_DISABLE = "disable";

    /**
     * <p>Constant key denoting the Game's title property.</p>
     */
    public static final String TITLE = "title";

    /**
     * <p>Constant key denoting the Game's developer property.</p>
     */
    public static final String DEVELOPER = "developer";

    /**
     * <p>Constant key denoting the Game's version property.</p>
     */
    public static final String VERSION = "version";

    /**
     * <p>Constant key denoting the Game's vsync status.</p>
     */
    public static final String VSYNC = "vsync";

    /**
     * <p>Target tickrate for updates per second.</p>
     */
    public static final String TICKRATE = "tickrate";

    /**
     * <p>Number of seconds before an average per second tickrate is measured.</p>
     */
    public static final String RATE_SAMPLES = "rate_samples";

    /**
     * <p>Toggle for enabling debug actions through console input.</p>
     */
    public static final String DEBUG_MODE = "debug_mode";

    // Initial number of pixels per world unit for View scalings
    private static final float DEFAULT_VIEW_SCALE = 60f;

    // Conversion constant # of nanosec in 1 sec
    private static final long NS_PER_SEC = 1000000000L;

    // Tickrate if not set
    private static final int DEFAULT_TICKRATE = 60;

    // Tickrate sample size if not set (5 samples == 5 second measurement intervals)
    private static final int DEFAULT_TICKRATE_SAMPLES = 5;

    // Frame rate
    private int mTickRate;

    // Number of nanoseconds per frame
    private long mTickSize;

    /**
     * Debug tools
     */

    // Measures game loop
    private final RateLogger mRateLogger;

    // Performs debug commands from console input
    private DebugCtrl mDCtrl;

    /**
     * Utils
     */

    // Point surrogate to hold mouse position during getMousePosition()
    private final Point2F mMousePos = new Point2F(0f, 0f);

    /**
     * Current game area
     */

    // Visible area on screen
    private final View mView;

    // Current game room
    private Room mRoom;

    // Currently selected GObject (id-version)
    private int mSelectedId = -1;
    private int mSelectedVersion = -1;

    /**
     * Services
     */

    // Drawing surface
    private final Canvas mCanvas;

    // Keyboard and mouse input converted to Events
    private final Window.Input mWinInput;

    // Event queue for processing system wide Events
    private final EventHub mEventHub;

    // Physics and collision handler
    private final Solver mSolver;

    // Device input mapping to Actions
    private final ControlMap mControlMap;

    /**
     * Resources
     */

    // Factories
    private final Resources<E> mResDir;

    // Listener to be notified of a pending scene snapshot to be drawn
    private ImageFactory.OnFrameEndListener mOnFrameEndListener;

    // Loop control
    private volatile boolean mContinue = true;

    // Game metadata
    private final Map<String, String> mProperties = new HashMap<String, String>();

    /**
     * <p>Constructor for a Game.</p>
     *
     * @param resources Resources for game object construction.
     * @param services Services for running various game systems.
     * @param properties game properties such as {@link Game#TITLE} and {@link Game#VSYNC}.
     */
    public Game(Resources<E> resources, Services services, Canvas canvas, Map<String, String> properties)
    {
        mCanvas = canvas;
        mProperties.putAll(properties);

        // Check if minimum required meta data was provided
        checkBasicProperties(properties);

        // Check all resources were provided
        checkResources(resources);

        // Defensive copy in case given Resources instantiates new Object with each method call
        mResDir = copy(resources);

        // Compute tick details
        mTickRate = getIntegerProperty(TICKRATE, DEFAULT_TICKRATE);
        mTickSize = NS_PER_SEC / mTickRate;

        // Create logger for measuring real tickrate
        mRateLogger = new RateLogger(getIntegerProperty(RATE_SAMPLES, DEFAULT_TICKRATE_SAMPLES));

        // Only instantiate debug if debug mode was requested
        if (getBooleanProperty(DEBUG_MODE, false)) {
            mDCtrl = new DebugCtrl(this);
        }

        // Init physics and collision
        mSolver = new IterativeSolver(getBodyFactory(), 1f / (float) mTickRate, 30);

        // Use given EventHub or use default if none provided
        final boolean noService = services == null;
        final EventHub eventHub = (noService) ? null : services.getEventHub();
        mEventHub = (eventHub == null) ? new DefaultEventHub() : eventHub;

        // Use given ControlMap or use default if none provided
        final ControlMap ctrl = (noService) ? null : services.getControlMap();
        mControlMap = (ctrl == null) ? new DefaultControlMap() : ctrl;

        // Create default Window.Input if none was set
        final Window window = mCanvas.getWindow();
        mWinInput = (window.getInput() == null) ? new DefaultInput(window) : window.getInput();
        window.setInput(mWinInput);

        // Create fullscreen View
        mView = new View(this, DEFAULT_VIEW_SCALE);

        installSystemListeners();
    }

    /**
     * <p>Install listeners for various system functions such as updating drawing order at the end of a frame or
     * updating the {@link View}'s dimensions when the {@link Window}'s framebuffer resizes.</p>
     */
    private void installSystemListeners()
    {
        // Attach resize listener to update View's scaling limits with Window
        getCanvas().getWindow().addOnFramebufferResizeListener(new OnResizeListener()
        {
            @Override
            public void onResize(float oldWidth, float oldHeight, float width, float height)
            {
                // Update the View's dimensions to match Window's new framebuffer size
                final View view = getView();
                view.setWidth(width);
                view.setHeight(height);

                // Reapplying scale forces room scale limit when view is room constrained
                view.setScale(view.getScale());
            }
        });

        // Listener that updates image component drawing order at the end of each frame
        mOnFrameEndListener = getImageFactory().newOnFrameEndListener();
    }

    /**
     * <p>Checks whether or not the set properties have the required entries and throws an
     * {@link IllegalArgumentException} if any are missing or invalid.</p>
     *
     * @throws IllegalArgumentException if either {@link Game#TITLE}, {@link Game#DEVELOPER}, or {@link Game#VERSION}
     * are not keys in the given Map or any of the keys have no value.
     */
    private void checkBasicProperties(Map<String, String> properties)
    {
        // Check if title, dev, and version keys even exist
        if (!properties.containsKey(TITLE) || !properties.containsKey(DEVELOPER) || !properties.containsKey(VERSION)) {
            throw new IllegalArgumentException("Game must have at least a Game.TITLE, Game.DEVELOPER, and Game" +
                    ".VERSION property");
        }

        // Prevent null title
        if (properties.get(TITLE) == null) {
            throw new IllegalArgumentException("Game may not have a null Game.TITLE");
        }

        // Prevent null developer
        if (properties.get(DEVELOPER) == null) {
            throw new IllegalArgumentException("Game may not have a null Game.DEVELOPER");
        }

        // Prevent null version
        if (properties.get(VERSION) == null) {
            throw new IllegalArgumentException("Game may not have a null Game.VERSION");
        }
    }

    /**
     * <p>Checks whether or not any of the {@link Resources}'s methods return null. In such a case, this method
     * throws an {@link IllegalArgumentException}.</p>
     *
     * @throws IllegalArgumentException if any of the methods defined in Resources return null.
     */
    private void checkResources(Resources resources)
    {
        // Check for null GObjectFactory
        if (resources.getGObjectFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null GObjectFactory");
        }

        // Check for null ImageFactory
        if (resources.getBodyFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null BodyFactory");
        }

        // Check for null ImageFactory
        if (resources.getImageFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null ImageFactory");
        }

        // Check for null ShaderFactory
        if (resources.getShaderFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null ShaderFactory");
        }
    }

    /**
     * <p>Copies the resources returned by the methods declared in {@link Resources} to a new instance of Resources.
     * This protects against cases where the given Resources object instantiates a new resource with each call
     * to its getter methods.</p>
     *
     * @param resources Resources.
     * @return new Resources.
     */
    private Resources<E> copy(Resources<E> resources)
    {
        return new Resources<E>()
        {
            private final ShaderFactory mShaderFactory = resources.getShaderFactory();
            private final GObjectFactory<E> mGObjectFactory = resources.getGObjectFactory();
            private final BodyFactory mBodyFactory = resources.getBodyFactory();
            private final ImageFactory mImageFactory = resources.getImageFactory();

            @Override
            public ShaderFactory getShaderFactory()
            {
                return mShaderFactory;
            }

            @Override
            public GObjectFactory<E> getGObjectFactory()
            {
                return mGObjectFactory;
            }

            @Override
            public BodyFactory getBodyFactory()
            {
                return mBodyFactory;
            }

            @Override
            public ImageFactory getImageFactory()
            {
                return mImageFactory;
            }
        };
    }

    /**
     * <p>Calls on the Game to begin.</p>
     *
     * <p>This method blocks until {@link ShaderFactory} has finished loading its resources (e.g. textures and
     * shaders) and {@link ShaderFactory#isLoaded()} returns true.</p>
     */
    public final void start()
    {
        // Set vsync from property
        final Window window = mCanvas.getWindow();
        window.setVsyncEnabled(getBooleanProperty(VSYNC, true));

        // Launch rendering thread
        mCanvas.start(mRateLogger.getSampleSize());

        // Block till gfx resources are ready
        while (!getShaderFactory().isLoaded()) {
        }

        // Installs commonly used EventHandlers
        installSystemHandlers();

        run();
    }

    /**
     * <p>Reads the set properties and returns a true or false if the given property name is associated with
     * {@link #PROPERTY_ENABLE} or {@link #PROPERTY_DISABLE}, respectively. If the property is not found, this method
     * returns the given default value.</p>
     *
     * @param name property name.
     * @param defaultValue in case property was not found or was incorrectly formatted.
     * @return value.
     */
    private boolean getBooleanProperty(String name, boolean defaultValue)
    {
        final String val = mProperties.get(name);
        if (val == null) {
            return defaultValue;
        }

        return val.equals(PROPERTY_ENABLE) || !(val.equals(PROPERTY_DISABLE));
    }

    /**
     * <p>Reads the set properties and returns an int associated with the given property name. If the property is
     * not found, this method returns the given default value.</p>
     *
     * @param name property name.
     * @param defaultValue in case property was not found or was incorrectly formatted.
     * @return value.
     */
    private int getIntegerProperty(String name, int defaultValue)
    {
        final String val = mProperties.get(name);
        if (val == null) {
            return defaultValue;
        }

        // Try to convert value to int
        int num = defaultValue;
        try {
            num = Integer.valueOf(val);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return num;
    }

    /**
     * <p>Installs basic {@link EventHandler}s expected to be commonly needed such as selecting a {@link GObject}
     * instance.</p>
     */
    private void installSystemHandlers()
    {
        final EventDispatcher dispatcher = mEventHub.getDispatcher();

        // Install GObject selection via mouse
        dispatcher.addHandler(new MouseSelectHandler(), new EventFilter<MouseEvent>()
        {
            @Override
            public boolean filter(MouseEvent event)
            {
                return event.isPress() && event.isButton(Button.LEFT);
            }
        }, EventDispatcher.PRIORITY_MAX);
    }

    /**
     * <p>Launches the game's window and signals the beginning of both drawing and game state operations./p>
     */
    protected final void run()
    {
        // Install image configs
        getImageFactory().load();

        // Install body configs
        getBodyFactory().load();

        // Install GObject configs
        getGObjectFactory().load();

        // Open the window
        final Window window = mCanvas.getWindow();
        window.show();

        // Notify loop beginning
        onBegin();

        // Loop
        process();

        // Notify game shutting down
        onEnd();

        // Request the debug thread stop if was created
        if (mDCtrl != null) {
            mDCtrl.stop();
        }

        // Request Window close and notify Canvas of closing
        window.close();

        // Wait for guarantee Canvas won't use GL ops anymore
        while (!mCanvas.hasStopped()) {
        }

        // Release GLFW and GL resources
        window.destroy();

        // Ensure any leftover blocking Threads (like debug) won't keep program alive
        System.exit(0);
    }

    /**
     * <p>Signals the game to prepare shutting down.</p>
     *
     * <p>This method does not interrupt a game tick from completing but will prevent subsequent ticks from beginning
     * . {@link Game#onEnd()} is called after the current tick has completed.</p>
     */
    public final void stop()
    {
        mContinue = false;
    }

    /**
     * <p>Called when the Game has begun (after {@link #start()}) but ticks have not begun processing yet.</p>
     *
     * <p>This method should be used to perform initial setup operations such as setting the {@link View} to follow a
     * specific {@link GObject}.</p>
     */
    protected abstract void onBegin();

    /**
     * <p>This method controls the game's tick timing and state updating.</p>
     *
     * <p><b>The current loop implementation was learned from "Game Programming Patterns" by Robert Nystrom at
     * http://gameprogrammingpatterns.com/game-loop.html.</b></p>
     */
    protected final void process()
    {
        final Window window = getCanvas().getWindow();

        final long tickSize = this.getTickSize();
        long timeLastStart = System.nanoTime();
        long frameDuration = 0;

        mRateLogger.start();

        // Begin game loop
        while (mContinue && !window.isClosing()) {

            // Figure last set duration
            final long current = System.nanoTime();
            final long elapsed = current - timeLastStart;
            timeLastStart = current;
            frameDuration += elapsed;

            while (frameDuration >= tickSize) {

                // Poll for GLFW events
                window.pollEvents();

                // Process debug mode actions if available
                if (mDCtrl != null) {
                    mDCtrl.debug();
                }

                // Process input
                onUpdateBegin();

                // Deferred ops to subclass
                onUpdate();

                frameDuration -= tickSize;
                mRateLogger.log();
            }

            // Send drawing data
            onFrameEnd();
        }
    }

    /**
     * <p>Called after {@link #stop()}, this method signals the game shutting
     * down. Subclasses should perform cleanup here.</p>
     */
    protected abstract void onEnd();

    /**
     * <p>Gets the set tick rate.</p>
     *
     * @return ticks per second.
     */
    public final int getTickRate()
    {
        return mTickRate;
    }

    /**
     * <p>Gets the desired duration of a game tick in nanoseconds.</p>
     *
     * <p>This method is dependent on the tickrate submitted as a property in
     * {@link #Game(Resources, Services, Canvas, Map)} using the name {@link #TICKRATE}.</p>
     *
     * @return desired nanosecond duration.
     */
    public final long getTickSize()
    {
        return mTickSize;
    }

    /**
     * <p>Gets the most recently measured tickrate.</p>
     *
     * @return tickrate.
     */
    final int getCurrentTickrate()
    {
        return mRateLogger.getRate();
    }

    /**
     * <p>This method is executed before each call to {@link #onUpdate()} and
     * is thus performed per tick.</p>
     */
    private void onUpdateBegin()
    {
        // Take InputEvents from Window
        mWinInput.poll(mControlMap, mEventHub);

        // Propagate general events
        mEventHub.broadcast();

        // Execute commands attached to specific InputEvents
        mControlMap.fire();

        // Perform AABB collision tests
        mSolver.update(getGObjectFactory(), getBodyFactory());

        // Process View's operations over time (focusing, interpolation)
        mView.update();
    }

    /**
     * <p>Main body of a game tick. Subclasses should override this method
     * to perform per tick operations.</p>
     */
    protected abstract void onUpdate();

    /**
     * <p>Called when the game's timing is within the desired rate and there is no pressing need to update the game
     * state. All {@link ImageComponent}s visible and within the current {@link View} are snapshot and sent to the
     * {@link Canvas} for drawing.</p>
     */
    private void onFrameEnd()
    {
        final Canvas.SceneBuffer buffer = getCanvas().getSceneBuffer();

        // Notify any set listener
        if (mOnFrameEndListener != null) {
            mOnFrameEndListener.onFrameEnd();
        }

        // Snapshot all visible ImageComponents into the Scene
        final Scene scene = buffer.getWriteScene();
        scene.add(getImageFactory(), getView());

        // Send Scene to be drawn in Canvas
        buffer.flush();
    }

    /**
     * <p>Gets the GObject last selected by the mouse with a left-click.</p>
     *
     * @return GObject, or null if the GObject is no longer alive or none has
     * been selected.
     */
    protected final GObject getSelected()
    {
        // Check for initial state where id and version isn't valid (none selected)
        if (mSelectedId < 0 || mSelectedVersion < 0) {
            return null;
        }

        return getGObjectFactory().get(mSelectedId, mSelectedVersion);
    }

    /**
     * <p>Sets the selection to a specific {@link GObject} that matches a given id-version pair.</p>
     *
     * @param id id.
     * @param version version.
     */
    protected final void setSelected(int id, int version)
    {
        mSelectedId = id;
        mSelectedVersion = version;
    }

    /**
     * <p>Sets the selection to a specific {@link GObject}.</p>
     *
     * @param object GObject to select, or null to reset selection.
     */
    protected final void setSelected(GObject object)
    {
        // Reset selection to invalid id-version if given null
        if (object == null) {
            setSelected(-1, -1);
        } else {
            // Assign GObject's id-version to selection
            setSelected(object.getId(), object.getVersion());
        }
    }

    /**
     * <p>Gets the current tick's KeyEvent.</p>
     *
     * @return current KeyEvent.
     */
    protected final KeyEvent getKey()
    {
        return mControlMap.getKey();
    }

    /**
     * <p>Gets the current tick's MouseEvent.</p>
     *
     * @return current MouseEvent.
     */
    protected final MouseEvent getMouse()
    {
        return mControlMap.getMouse();
    }

    /**
     * <p>Gets the mouse's current screen coordinates.</p>
     *
     * <p>This method should only be called from the main thread.</p>
     *
     * @return mouse's (x,y).
     */
    protected final Point2F getMousePosition()
    {
        mWinInput.pollMouse(mMousePos);
        return mMousePos;
    }

    /**
     * <p>Gets the {@link ControlMap} responsible for mapping operations to the mouse and keyboard.</p>
     *
     * @return ControlMap.
     */
    protected final ControlMap getControlMap()
    {
        return mControlMap;
    }

    /**
     * <p>Gets the {@link Solver} responsible for all {@link BodyComponent}'s physics computations.</p>
     *
     * @return Solver.
     */
    protected final Solver getSolver()
    {
        return mSolver;
    }

    /**
     * <p>Gets the {@link EventHub} to propagate {@link Event}s throughout the system.</p>
     *
     * @return EventHub.
     */
    protected final EventHub getEventHub()
    {
        return mEventHub;
    }

    /**
     * <p>Gets the {@link ImageFactory} responsible for generating {@link ImageComponent}s.</p>
     *
     * @return ImageFactory.
     */
    protected final ImageFactory getImageFactory()
    {
        return mResDir.getImageFactory();
    }

    /**
     * <p>Gets the {@link Canvas} responsible for drawing {@link Scene}s.</p>
     *
     * @return Canvas.
     */
    protected final Canvas getCanvas()
    {
        return mCanvas;
    }

    /**
     * <p>Gets the {@link ShaderFactory} providing {@link ShaderProgram}s and {@link Texture}s for constructing
     * imagery.</p>
     *
     * @return ShaderFactory.
     */
    protected final ShaderFactory getShaderFactory()
    {
        return mResDir.getShaderFactory();
    }

    /**
     * <p>Gets the {@link GObjectFactory} responsible for generating {@link GObject}s.</p>
     *
     * @return GObjectFactory.
     */
    protected final GObjectFactory<E> getGObjectFactory()
    {
        return mResDir.getGObjectFactory();
    }

    /**
     * <p>Gets the {@link BodyFactory} responsible for generating {@link BodyComponent}s.</p>
     *
     * @return BodyFactory.
     */
    protected final BodyFactory getBodyFactory()
    {
        return mResDir.getBodyFactory();
    }

    /**
     * <p>Gets the current game {@link Room}.</p>
     *
     * @return Room.
     */
    protected final Room getRoom()
    {
        return mRoom;
    }

    /**
     * <p>Sets the current game {@link Room}.</p>
     *
     * @param room Room.
     */
    protected final void setRoom(Room room)
    {
        mRoom = room;
        mView.setRoom(mRoom);
    }

    /**
     * <p>Gets the {@link View} describing the visible area within the game world as it's constrained by the
     * dimensions of the {@link Canvas}'s drawing area.</p>
     *
     * @return View.
     */
    protected final View getView()
    {
        return mView;
    }

    /**
     * <p>Gets the game's title.</p>
     *
     * @return title.
     */
    public final String getTitle()
    {
        return mProperties.get(TITLE);
    }

    /**
     * <p>Gets the game's developer.</p>
     *
     * @return developer.
     */
    public final String getDeveloper()
    {
        return mProperties.get(DEVELOPER);
    }

    /**
     * <p>Gets the game's build version.</p>
     *
     * @return version.
     */
    public final String getVersion()
    {
        return mProperties.get(VERSION);
    }

    /**
     * <p>Gets the {@link Resources} given to the Game's constructor.</p>
     *
     * @return Resources.
     */
    protected final Resources<E> getResources()
    {
        return mResDir;
    }

    /**
     * <p>
     *     {@link MouseEventHandler} to be triggered on a {@link Button#LEFT} and {@link Action#RELEASE}
     *     {@link MouseEvent}. This tests the associated MouseEvent against {@link GObject}s, executing
     *     their respective {@link GObject#click(MouseEvent)} if the Event occurred on the GObject and saving the
     *     clicked GObject's instance id and version as <i>selected</i>.
     * </p>
     *
     * <p>
     *     The selected GObject may be retrieved through {@link #getSelected()}.
     * </p>
     */
    private class MouseSelectHandler implements MouseEventHandler
    {
        @Override
        public void handle(MouseEvent mouseEvent)
        {
            // Translate event coordinates to world-space
            getView().translateToWorld(mouseEvent);

            // Retrieve factories to check obj selection status
            final ImageFactory imgFactory = getImageFactory();
            final GObjectFactory<E> objFactory = getGObjectFactory();

            // Test all objects from front to back
            final int size = imgFactory.getVisibleCount();
            for (int i = size - 1; i >= 0; i--) {

                // Get img info at specific z dist from screen
                final ImageComponent rend = imgFactory.getAtDistance(i);

                // Skip orphaned Components
                if (rend.isOrphan()) {
                    continue;
                }

                // Get owning GObject
                final GObject obj = objFactory.get(rend.getGObjectId(), rend.getGObjectVersion());

                // Can't test for containment without BodyComponent
                final BodyComponent body = obj.getBodyComponent();
                if (body == null) {
                    continue;
                }

                // Only select if mouse was in bounding box and body allows
                if (body.contains(mouseEvent.getX(), mouseEvent.getY()) && body.isSelectable()) {

                    // Save new selected obj's id/version for reference
                    Game.this.setSelected(obj.getId(), obj.getVersion());

                    // Allow GObject to handle click operations
                    obj.click(mouseEvent);

                    // First hit is implied user's desired target
                    return;

                } else {
                    // No hit so set selection to nothing
                    Game.this.setSelected(null);
                }
            }
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Game instances should not be cloned.");
    }

    @Override
    public String toString()
    {
        return "[title(" + getTitle() + "),dev(" + getDeveloper() + "),build(" + getVersion() + ")]";
    }

    /**
     * <p>
     *     The Resources class provides access to various factories which produce not only
     *     {@link ComponentFactory.Component}s for assembling {@link GObject}s but also other sources for building
     *     game materials such as the {@link GObjectFactory} itself.</p>
     *
     * <p>
     *     Subclasses must provide their own implementations for all objects returned by the class' declared getters.
     *     If any of getters' subclass implementations return a null resource, {@link Game} will throw an
     *     {@link IllegalArgumentException} and will not safely execute.
     * </p>
     */
    public static abstract class Resources<E extends GObject>
    {
        /**
         * <p>Gets the {@link ShaderFactory} to use for providing shaders and textures.</p>
         *
         * @return ShaderFactory.
         */
        public abstract ShaderFactory getShaderFactory();

        /**
         * <p>Gets the {@link GObjectFactory} to use for providing game objects.</p>
         *
         * @return GObjectFactory.
         */
        public abstract GObjectFactory<E> getGObjectFactory();

        /**
         * <p>Gets the {@link BodyFactory} to use for providing collision operations.</p>
         *
         * @return BodyFactory.
         */
        public abstract BodyFactory getBodyFactory();

        /**
         * <p>Gets the {@link ImageFactory} to use for providing drawing preferences for each game object.</p>
         *
         * @return ImageFactory.
         */
        public abstract ImageFactory getImageFactory();
    }

    /**
     * <p>
     *     Services gives access to parts of the engine that control specific aspects such as user control bindings
     *     or handling and publishing {@link Event}s.
     * </p>
     *
     * <p>
     *     Unlike {@link Resources}, subclasses of Services are not required to provide implementations of each
     *     system returned by a method in Services. If a method returns <i>null</i>, a default implementation will be
     *     provided when {@link Game} begins.
     * </p>
     */
    public static abstract class Services
    {
        /**
         * <p>Gets the {@link EventHub} to use for publishing, handling, and propagating {@link Event}s throughout
         * the game.</p>
         *
         * @return EventHub.
         */
        public abstract EventHub getEventHub();

        /**
         * <p>Gets the {@link ControlMap} to use for handling user controls and key bindings.</p>
         *
         * @return ControlMap.
         */
        public abstract ControlMap getControlMap();
    }
}
