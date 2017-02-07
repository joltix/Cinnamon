package com.cinnamon.system;

import com.cinnamon.gfx.*;
import com.cinnamon.object.*;
import com.cinnamon.utils.Event;
import com.cinnamon.utils.KeyEvent;
import com.cinnamon.utils.MouseEvent;

import java.util.Map;

/**
 * <p>
 *     This class is responsible for not only controlling the game's tick
 *     rate but also providing properties and services to different parts of
 *     the game. Subclasses may, for example, retrieve systems such as
 *     {@link #getInput()} to change keyboard or mouse control bindings.
 * </p>
 *
 *
 */
public abstract class Game
{
    /**
     * <p>Constant value for enabling a property whose expected value is a
     * toggle.</p>
     */
    public static final String PROPERTY_ENABLE = "enable";

    /**
     * <p>Constant value for disabling a property whose expected value is a
     * toggle.</p>
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

    // Conversion constant # of nanosec in 1 sec
    private static final long NS_PER_SEC = 1000000000L;

    // Tick rate if not set
    private static final int DEFAULT_TICKRATE = 60;

    // Game runs on its own thread
    private Thread mThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            // Block till gfx resources are ready
            while (!getShaderFactory().isLoaded()) {
            }

            // Continue on to main process
            Game.this.run();
        }
    });


    // Frame rate and frame duration
    private int mTickRate = DEFAULT_TICKRATE;
    private long mTickSize = NS_PER_SEC / DEFAULT_TICKRATE;

    // Visible area on screen
    private final View mView;

    // Current game room
    private Room mRoom;

    // Currently selected GObject (id/version)
    private int mSelectedId;
    private int mSelectedVersion;

    // Keyboard and mouse input converted to Events
    private final Window.Input mWinInput;

    // Device input mapping to Actions
    private final InputMap mInputMap = new InputMap();

    // Drawing surface
    private final Canvas mCanvas;

    // Factories
    private final Resources mResDir;

    // Listener to be notified of a pending scene snapshot to be drawn
    private ImageFactory.OnFrameEndListener mOnFrameEndListener;

    // Game state callbacks and loop control
    private OnEndListener mOnEndListener;
    private volatile boolean mContinue = true;

    // Game metadata
    private final Map<String, String> mProperties;

    /**
     * <p>Constructor for a Game.</p>
     *
     * @param resources Resources for game object construction.
     * @param canvas drawing surface.
     * @param properties game properties such as {@link Game#TITLE} and
     * {@link Game#VSYNC}.
     */
    public Game(Resources resources, Canvas canvas, Map<String, String>
            properties)
    {
        mProperties = properties;
        mResDir = resources;

        checkBasicProperties();
        checkResources();

        mCanvas = canvas;
        mWinInput = canvas.getWindow().getInput();

        // Create fullscreen View
        mView = new View(canvas);

        mOnFrameEndListener = getImageFactory().newOnFrameEndListener();
    }

    /**
     * <p>Checks whether or not the set properties have the required demo
     * entries and throws an {@link IllegalArgumentException} if any are
     * missing or invalid. Namely: {@link Game#TITLE},
     * {@link Game#DEVELOPER}, and {@link Game#VERSION}.</p>
     */
    private void checkBasicProperties()
    {
        // Check if title, dev, and version keys even exist
        if (!mProperties.containsKey(TITLE)
                || !mProperties.containsKey(DEVELOPER)
                || !mProperties.containsKey(VERSION)) {
            throw new IllegalArgumentException("Game must have at least a " +
                    "Game.TITLE, Game.DEVELOPER, and Game.VERSION property");
        }

        // Prevent null title
        if (mProperties.get(TITLE) == null) {
            throw new IllegalArgumentException("Game may not have a null Game" +
                    ".TITLE");
        }

        // Prevent null developer
        if (mProperties.get(DEVELOPER) == null) {
            throw new IllegalArgumentException("Game may not have a null Game" +
                    ".DEVELOPER");
        }

        // Prevent null version
        if (mProperties.get(VERSION) == null) {
            throw new IllegalArgumentException("Game may not have a null Game" +
                    ".VERSION");
        }
    }

    /**
     * <p>Checks whether or not the set {@link Resources} is not null and
     * that none of its returned resources are null as well. In such a case,
     * this method throws an {@link IllegalArgumentException}.</p>
     */
    private void checkResources()
    {
        // Check for null Resources
        if (mResDir == null) {
            throw new IllegalArgumentException("Resources cannot be " +
                    "null");
        }

        // Check for null GObjectFactory
        if (mResDir.getGObjectFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null " +
                    "GObjectFactory");
        }

        // Check for null ImageFactory
        if (mResDir.getBodyFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null " +
                    "BodyFactory");
        }

        // Check for null ImageFactory
        if (mResDir.getImageFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null " +
                    "ImageFactory");
        }

        // Check for null ShaderFactory
        if (mResDir.getShaderFactory() == null) {
            throw new IllegalArgumentException("Resources cannot have a null " +
                    "ShaderFactory");
        }
    }

    /**
     * <p>Calls on the Game to begin.</p>
     *
     * <p>This method blocks until {@link ShaderFactory} has finished
     * loading its resources (e.g. textures and shaders) and
     * {@link ShaderFactory#isLoaded()} returns true.</p>
     */
    public final void start()
    {
        // Launch rendering thread
        final Canvas canvas = getCanvas();
        canvas.start();

        // Launch process thread
        mThread.start();

        // Set vsync from property
        final Window window = canvas.getWindow();
        window.setVsyncEnabled(getVsyncProperty());

        // Configure Window to notify Game when closing
        window.setOnEndListener(new OnEndListener()
        {
            @Override
            public void onEnd()
            {
                Game.this.stop();
            }
        });

        // Open the window
        window.show();
    }

    /**
     * <p>Reads the set properties and returns true if a request for
     * enabling vsync is found.</p>
     *
     * @return true if vsync was requested.
     */
    private boolean getVsyncProperty()
    {
        final String val = getProperty(VSYNC);
        return (val != null && val.equals(PROPERTY_ENABLE));
    }

    /**
     * <p>Launches the game's window and signals the beginning of both
     * drawing and game state operations./p>
     */
    protected final void run()
    {
        // Install image configs
        getImageFactory().load();

        // Install GObject configs
        getGObjectFactory().load();

        // Attach left-click mouse GObject selection hook
        final int button = MouseEvent.BUTTON_LEFT;
        mInputMap.attachSystemMouse(button, new MouseSelectAction());

        // Notify loop beginning
        onBegin();

        // Loop
        process();

        // Notify game shutting down
        onEnd();

        // Execute last callback if any
        if (mOnEndListener != null) {
            mOnEndListener.onEnd();
        }
    }

    /**
     * <p>Signals the game to prepare shutting down.</p>
     *
     * <p>This method does not interrupt a game tick from completing but will
     * prevent subsequent ticks from beginning. After the current tick has
     * completed, {@link Game#onEnd()} and any listener set by
     * {@link #setOnEndListener(OnEndListener)} are called, respectively.</p>
     */
    public final void stop()
    {
        mContinue = false;
    }

    /**
     * <p>Called when the Game has begun (after {@link #start()}) but ticks
     * have not begun processing yet.</p>
     *
     * <p>This method should be used to perform initial setup operations
     * such as setting the {@link View} to follow a specific {@link GObject}
     * .</p>
     */
    protected abstract void onBegin();

    /**
     * <p>This method controls the game's tick timing and state updating.</p>
     *
     * <p><b>The current loop implementation was learned from "Game
     * Programming Patterns" by Robert Nystrom at
     * http://gameprogrammingpatterns.com/game-loop.html.</b></p>
     */
    protected final void process()
    {
        final long tickSize = this.getTickSize();
        long timeLastStart = System.nanoTime();
        long frameDuration = 0;

        // Begin game loop
        while (mContinue) {

            // Figure last set duration
            final long current = System.nanoTime();
            final long elapsed = current - timeLastStart;
            timeLastStart = current;
            frameDuration += elapsed;

            while (frameDuration >= tickSize) {
                // Process input
                onTickBegin();

                // Deferred ops to subclass
                update();

                frameDuration -= tickSize;
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
     * <p>Sets the number of ticks per second.</p>
     *
     * @param ticksPerSecond tick rate.
     */
    protected final void setTickrate(int ticksPerSecond)
    {
        mTickRate = ticksPerSecond;
        mTickSize = NS_PER_SEC / ticksPerSecond;
    }

    /**
     * <p>Gets the desired duration of a game tick in nanoseconds.</p>
     *
     * <p>This method is dependent on the rate given to
     * {@link #setTickrate(int)}.</p>
     *
     * @return desired nanosecond duration.
     */
    public final long getTickSize()
    {
        return mTickSize;
    }

    /**
     * <p>This method is executed before each call to {@link #update()} and
     * is thus performed per tick.</p>
     */
    private void onTickBegin()
    {
        // Take input events from Window
        mWinInput.poll(mInputMap);

        // Trigger actions if events match
        mInputMap.executeActions();
    }

    /**
     * <p>Main body of a game tick. Subclasses should override this method
     * to perform per tick operations.</p>
     */
    protected abstract void update();

    /**
     * <p>Called when the game's timing is within the desired rate and there
     * is no pressing need to update the game state. All
     * {@link ImageComponent}s visible and within the current {@link View} are
     * snapshot and sent to the {@link Canvas} for drawing.</p>
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
        return getGObjectFactory().get(mSelectedId, mSelectedVersion);
    }

    /**
     * <p>Gets the current tick's KeyEvent.</p>
     *
     * @return current KeyEvent.
     */
    protected final KeyEvent getKey()
    {
        return mInputMap.getKey();
    }

    /**
     * <p>Gets the current tick's MouseEvent.</p>
     *
     * @return current MouseEvent.
     */
    protected final MouseEvent getMouse()
    {
        return mInputMap.getMouse();
    }

    /**
     * <p>Gets the {@link InputMap} responsible for mapping
     * {@link ControlMap.Action}s to {@link KeyEvent}s or {@link MouseEvent}s
     * .</p>
     *
     * @return InputMap.
     */
    protected final InputMap getInput()
    {
        return mInputMap;
    }

    /**
     * <p>Gets the {@link ImageFactory} responsible for generating
     * {@link ImageComponent}s.</p>
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
     * <p>Gets the {@link ShaderFactory} providing {@link ShaderProgram}s
     * and {@link Texture}s for constructing imagery.</p>
     *
     * @return ShaderFactory.
     */
    protected final ShaderFactory getShaderFactory()
    {
        return mCanvas.getShaderFactory();
    }

    /**
     * <p>Gets the {@link GObjectFactory} responsible for generating
     * {@link GObject}s.</p>
     *
     * @return GObjectFactory.
     */
    protected final GObjectFactory getGObjectFactory()
    {
        return mResDir.getGObjectFactory();
    }

    /**
     * <p>Gets the {@link BodyFactory} responsible for generating
     * {@link BodyComponent}s.</p>
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
     * <p>Gets the {@link View} describing the visible area within the game
     * world as it's constrained by the dimensions of the {@link Canvas}'s
     * drawing area.</p>
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
     * <p>Gets a game property associated with the following key.</p>
     *
     * @param key property key.
     * @return value.
     */
    public final String getProperty(String key)
    {
        return mProperties.get(key);
    }

    /**
     * <p>Gets the {@link Resources} given to the Game's constructor.</p>
     *
     * @return Resources.
     */
    protected final Resources getResources()
    {
        return mResDir;
    }

    /**
     * <p>Sets a callback to be triggered after {@link #stop()} has been
     * called.</p>
     *
     * @param listener OnEndListener.
     */
    public final void setOnEndListener(OnEndListener listener)
    {
        mOnEndListener = listener;
    }

    /**
     * <p>{@link InputMap.Action} to be triggered on the
     * {@link MouseEvent#BUTTON_LEFT} {@link Event.Action#RELEASE} event.
     * This tests the associated {@link MouseEvent} against affected
     * {@link GObject}s, executing their respective
     * {@link GObject#click(MouseEvent)} and saving the clicked on
     * GObject's instance id as <i>selected</i>.</p>
     *
     * <p>The id may be retrieved through {@link #getSelected()}.</p>
     */
    private class MouseSelectAction implements ControlMap.Action<MouseEvent>
    {
        @Override
        public void execute(MouseEvent mouseEvent)
        {
            // Translate event coordinates to world-space
            getView().translateToWorld(mouseEvent);

            // Retrieve factories to check obj selection status
            final ImageFactory imgFactory = getImageFactory();
            final GObjectFactory objFactory = getGObjectFactory();

            // Test all objects from front to back
            final int size = imgFactory.size();
            for (int i = size - 1; i >= 0; i--) {

                // Get img info at specific z dist from screen
                final ImageComponent rend = imgFactory.getAtDistance(i);

                // Get BodyComponent for containment testing
                final int id = rend.getGObjectId();
                final int version = rend.getGObjectVersion();
                final GObject obj = objFactory.get(id, version);

                // Skip abandoned component (owning GObject is gone)
                if (obj == null) {
                    continue;
                }

                // Can't test for containment without BodyComponent
                final BodyComponent body = obj.getBodyComponent();
                if (body == null) {
                    continue;
                }

                // Only select if mouse was in bounding box and body allows
                if (body.contains(mouseEvent.getX(), mouseEvent.getY()) &&
                        body.isSelectable()) {

                    // Save new selected obj's id/version for reference
                    mSelectedId = obj.getId();
                    mSelectedVersion = obj.getVersion();

                    // Stop mouse event broadcast if consumed
                    if (obj.click(mouseEvent)) {
                        return;
                    }
                }
            }
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("Game instances should not be " +
                "cloned.");
    }

    @Override
    public String toString()
    {
        return "[title(" + getTitle() + "),dev(" + getDeveloper() + "),build("
                + getVersion() + ")]";
    }

    /**
     * <p>The Resources class provides access to various factories which produce
     * not only {@link ComponentFactory.Component}s for assembling
     * {@link GObject}s but also other sources for building game materials
     * such as the {@link GObjectFactory} itself.</p>
     *
     * <p>Subclasses must provide their own implementations for all objects
     * returned by the class' declared getters. If any of getters' subclass
     * implementations return a null resource, {@link Game} will throw an
     * {@link IllegalArgumentException} and will not safely execute.</p>
     */
    public static abstract class Resources
    {
        /**
         * <p>Gets the {@link ShaderFactory} to use for providing shaders and
         * textures.</p>
         *
         * @return ShaderFactory.
         */
        public abstract ShaderFactory getShaderFactory();

        /**
         * <p>Gets the {@link GObjectFactory} to use for providing game
         * objects.</p>
         *
         * @return GObjectFactory.
         */
        public abstract GObjectFactory getGObjectFactory();

        /**
         * <p>Gets the {@link BodyFactory} to use for providing collision
         * operations.</p>
         *
         * @return BodyFactory.
         */
        public abstract BodyFactory getBodyFactory();

        /**
         * <p>Gets the {@link ImageFactory} to use for providing drawing
         * preferences for each game object.</p>
         *
         * @return ImageFactory.
         */
        public abstract ImageFactory getImageFactory();
    }
}
