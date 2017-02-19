package com.cinnamon.system;

import com.cinnamon.gfx.Canvas;
import com.cinnamon.object.GObject;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.object.Room;

import java.util.Set;

/**
 * <p>
 *     Provides query and control to many services and functions such as creating {@link GObject}s and checking the
 *     current framerate. Two kinds of commands are acted upon: query and action. Query commands begin with the word
 *     <i>get</i> and prints requested information to the console. On the other hand, action commands have no
 *     specific identifier. These commands perform operations such as destroying {@link GObject}s but will still
 *     print an acknowledgment of the action to the console.
 * </p>
 *
 * <p>
 *     For both query and action commands, if nothing is printed on the console in response to a command, the command
 *     was likely not recognized and may have been mistyped.
 * </p>
 *
 * <p>
 *     The following query commands are supported: shutdown, get ups, get fps, get meta, get gl, get gpu, get
 *     processors, get memory, get resolution, get obj configs, get obj count, get selected, get room, get view.
 * </p>
 *
 * <p>
 *     The following action commands are supported: create obj {@literal <config name>}, destroy selected, constrain
 *     view {@literal <true | false>}.
 * </p>
 */
public final class DebugCtrl
{
    /**
     * The following query commands retrieve and print information to the console.
     */

    // Shuts down the game
    private static final String SHUTDOWN = "shutdown";

    // Print the update per second (or tickrate)
    private static final String GET_TICKRATE = "get ups";

    // Print drawing framerate
    private static final String GET_FRAMERATE = "get fps";

    // Print game title, dev, and build
    private static final String GET_META = "get meta";

    // Print OpenGL version
    private static final String GET_GL_VERSION = "get gl";

    // Print GPU vendor and model
    private static final String GET_GPU = "get gpu";

    // Print # of processors
    private static final String GET_PROCESSORS = "get processors";

    // Print memory stats
    private static final String GET_MEMORY = "get memory";

    // Print Canvas resolution
    private static final String GET_RESOLUTION = "get resolution";

    // Print a list of available GObjectConfigs
    private static final String GET_GOBJECT_CONFIGS = "get obj configs";

    // Print number of GObjects
    private static final String GET_GOBJECT_COUNT = "get obj count";

    // Print basic info about selected GObject
    private static final String GET_SELECTED = "get selected";

    // Print basic info about Room
    private static final String GET_ROOM = "get room";

    // Print basic info about View
    private static final String GET_VIEW = "get view";

    /**
     * The following action commands affect the game and are not simply getting information.
     */

    // Creates a GObject at the View's center, command format: "create obj <config name>"
    private static final String CREATE_GOBJ = "create obj";

    // Destroys the currently selected GObject
    private static final String DESTROY_SELECTED = "destroy selected";

    // Calls view.setRoomConstrained(boolean);
    private static final String SET_VIEW_CONSTRAINT = "constrain view";

    // Threaded console input
    private final ConcurrentConsoleInput<String> mConsole = new DebugConsole();

    // Game access for resources
    private final Game mGame;

    /**
     * <p>Constructor for a DebugCtrl.</p>
     *
     * @param game Game to manipulate.
     */
    public DebugCtrl(Game game)
    {
        mGame = game;

        // Launch console input thread
        mConsole.start();
    }

    /**
     * <p>Polls the console for input. When it's received, assuming a well-formed command, the input is translated
     * to either a query or action where the former prints to the console requested information and the latter
     * executes specific operations such as creating a GObject.</p>
     */
    public void debug()
    {
        // End early if no input to processor
        if (mConsole.isEmpty()) {
            return;
        }

        final String message = mConsole.poll();
        if (isQuery(message)) {
            // Fetch and print desired info if query
            printInfo(message);
        } else {
            // Execute operations if not query
            act(message);
        }
    }

    /**
     * <p>Checks whether or not the given console input is a query for information and matches the form of a query
     * input (e.g. "get fps" or "get obj count").</p>
     *
     * @param message console input.
     * @return true if input is a query.
     */
    private boolean isQuery(String message)
    {
        return message.matches("get\\s(.*)");
    }

    /**
     * <p>Requests the console cease taking input. This method only alerts the console to stop as soon as it can.
     * However, if the console is already waiting on user input when this method is called, the console may not stop
     * right away but first wait on the last input.</p>
     */
    public void stop()
    {
        mConsole.stop();
    }

    /**
     * <p>Retrieves and prints the information requested in the console input. This method assumes
     * calling {@link #isQuery(String)} on the input yields true.</p>
     *
     * @param input console input.
     */
    private void printInfo(String input)
    {
        switch (input) {
            // Print game tickrate
            case GET_TICKRATE:
                printTickrate(); break;
            // Print drawing framerate
            case GET_FRAMERATE:
                printFramerate(); break;
            // Print game title, dev, and build
            case GET_META:
                printMeta(); break;
            // Print OpenGL version
            case GET_GL_VERSION:
                printOpenGLVersion(); break;
            // Print GPU maker and model
            case GET_GPU:
                printGPU(); break;
            // Print number of available processors
            case GET_PROCESSORS:
                printProcessors(); break;
            // Print memory usage stats
            case GET_MEMORY:
                printMemory(); break;
            // Print drawing resolution
            case GET_RESOLUTION:
                printResolution(); break;
            // Print available GObject Configs
            case GET_GOBJECT_CONFIGS:
                printGObjectConfigs(); break;
            // Print number of GObjects in game
            case GET_GOBJECT_COUNT:
                printGObjectCount(); break;
            // Print basic info about the selected GObject
            case GET_SELECTED:
                printSelectedGObject(); break;
            // Print basic info about the Room
            case GET_ROOM:
                printRoom(); break;
            // Print basic info about the View
            case GET_VIEW:
                printView(); break;
            default:
                // Unrecognized get command so don't do anything
        }
    }

    /**
     * <p>Prints the most recent tickrate measurement reported by {@link Game#getCurrentTickrate()}.</p>
     */
    private void printTickrate()
    {
        final int ups = mGame.getCurrentTickrate();
        System.out.printf(format("updates per second = %s"), ups);
    }

    /**
     * <p>Prints the most recent framerate measurement reported by {@link Canvas#getCurrentFramerate()} ()}.</p>
     */
    private void printFramerate()
    {
        final int fps = mGame.getCanvas().getCurrentFramerate();
        System.out.printf(format("frames per second = %s"), fps);
    }

    /**
     * <p>Prints the {@link Game}'s title, developer, and build properties.</p>
     */
    private void printMeta()
    {
        final String title = mGame.getTitle();
        final String dev = mGame.getDeveloper();
        final String build = mGame.getVersion();
        System.out.printf(format("title(\"%s\"), dev(\"%s\"), build(\"%s\")"), title, dev, build);
    }

    /**
     * <p>Prints the OpenGL version in use.</p>
     */
    private void printOpenGLVersion()
    {
        System.out.printf(format(mGame.getCanvas().getGLVersion()));
    }

    /**
     * <p>Prints the vendor and model of the GPU in use.</p>
     */
    private void printGPU()
    {
        final Canvas canvas = mGame.getCanvas();
        final String vendor = canvas.getGPUVendor();
        final String model = canvas.getGPUModel();
        System.out.printf(format("vendor(%s), model(%s)"), vendor, model);
    }

    /**
     * <p>Prints the number of available processors reported by {@link Runtime#availableProcessors()}. Note that
     * Intel Hyper-threading may double this value.</p>
     */
    private void printProcessors()
    {
        final Runtime rt = Runtime.getRuntime();
        final int procCount = rt.availableProcessors();
        System.out.printf(format("processors(%d)"), procCount);
    }

    /**
     * <p>Prints the memory statistics reported by {@link Runtime}.</p>
     */
    private void printMemory()
    {
        final Runtime rt = Runtime.getRuntime();
        final int bytesInMB = 1000000;
        final long tot = rt.totalMemory();
        final long fre = rt.freeMemory();
        final float totalMem = (float) tot / bytesInMB;
        final float freeMem = (float) fre / bytesInMB;
        final float usedMem = (float) (tot - fre) / bytesInMB;
        final float maxMem = (float) rt.maxMemory() / bytesInMB;
        final String formatted = format("memory : total(%.2fMB), free(%.2fMB), used(%.2fMB), max(%.2fMB)");
        System.out.printf(formatted, totalMem, freeMem, usedMem, maxMem);
    }

    /**
     * <p>Prints the resolution reported by {@link Canvas}.</p>
     */
    private void printResolution()
    {
        final Canvas canvas = mGame.getCanvas();
        System.out.printf(format("width(%d), height(%d)"), canvas.getWidth(), canvas.getHeight());
    }

    /**
     * <p>Prints all {@link GObjectFactory.GObjectConfig}s available to use when creating {@link GObject}s.</p>
     */
    private void printGObjectConfigs()
    {
        final StringBuilder configList = new StringBuilder();

        // Get all config names
        final GObjectFactory factory = mGame.getGObjectFactory();
        final Set<String> configNames = factory.getConfigNames();

        // Set config count as first line
        configList.append("count(");
        configList.append(configNames.size() - 1);
        configList.append(")\n");

        // Add each config as its own line
        int i = 0;
        for (String name : factory.getConfigNames()) {

            // Don't report "room" config - shouldn't be spawned through here
            if (name.equals(GObjectFactory.CONFIG_ROOM)) {
                continue;
            }

            configList.append("    [");
            configList.append(i++);
            configList.append("] \"");
            configList.append(name);
            configList.append("\"\n");
        }

        // Format and print list
        final String list = configList.substring(0, configList.length() - 1);
        System.out.printf(format(list));
    }

    /**
     * <p>Prints the number of {@link GObject}s reported by {@link GObjectFactory#size()}.</p>
     */
    private void printGObjectCount()
    {
        final GObjectFactory factory = mGame.getGObjectFactory();
        System.out.printf(format("population(%d)"), factory.size());
    }

    /**
     * <p>Prints basic information about the currently selected {@link GObject}.</p>
     */
    private void printSelectedGObject()
    {
        final GObject obj = mGame.getSelected();
        final String info;
        if (obj == null) {
            info = "no object selected";
            System.out.printf(format(info));
        } else {

            info = "id(%d), version(%d), position(%d,%d,%d), img(%s), collision(%s)";

            final int id = obj.getId();
            final int ver = obj.getVersion();
            final int x = (int) obj.getX();
            final int y = (int) obj.getY();
            final int z = (int) obj.getZ();
            final boolean hasImg = (obj.getImageComponent() != null);
            final boolean hasBod = (obj.getBodyComponent() != null);

            System.out.printf(format(info), id, ver, x, y, z, hasImg, hasBod);
        }
    }

    /**
     * <p>Prints basic information about the {@link View}.</p>
     */
    private void printView()
    {
        final View view = mGame.getView();

        final int width = (int) view.getWidth();
        final int height = (int) view.getHeight();
        final int x = (int) view.getX();
        final int y = (int) view.getY();

        System.out.printf(format("width(%d), height(%d), position(%d, %d)"), width, height, x, y);
    }

    /**
     * <p>Prints basic information about the {@link Room}.</p>
     */
    private void printRoom()
    {
        final Room room = mGame.getRoom();
        System.out.printf(format("width(%d), height(%d)"), (int) room.getWidth(), (int) room.getHeight());
    }

    /**
     * <p>Executes operations depending on the command matching the console input.</p>
     *
     * @param input console input.
     */
    private void act(String input)
    {
        // Creates a GObject based off of a GObjectConfig
        if (input.contains(CREATE_GOBJ)) {
            createGObject(input);

            // Destroys the currently selected GObject
        } else if (input.equals(DESTROY_SELECTED)) {
            destroySelected();

            // Sets whether or not the View should be room constrained
        } else if (input.contains(SET_VIEW_CONSTRAINT)) {
            setViewConstraint(input);

            // Calls for the Game to stop
        } else if (input.equals(SHUTDOWN)) {
            mGame.stop();

        } else {
            // Do nothing - message didn't have any cmd key phrases
        }
    }

    /**
     * <p>Creates a {@link GObject} based off of a specified {@link GObjectFactory.GObjectConfig}.</p>
     *
     * @param input console input.
     */
    private void createGObject(String input)
    {
        final String[] tokens = partition(input);

        // Check if at least correct # of tokens
        if (tokens.length != 3) {
            return;
        }

        // Retrieve GObjectConfig
        final String configName = tokens[2];
        final GObjectFactory factory = mGame.getGObjectFactory();
        final GObjectFactory.GObjectConfig config = factory.getConfig(configName);

        // Don't allow GObjectFactory.CONFIG_ROOM to be used here
        if (config == null || configName.equals(GObjectFactory.CONFIG_ROOM)) {
            System.out.printf(format("no config named \"%s\""), configName);
        } else {

            final View view = mGame.getView();
            final GObject obj = factory.getGObject(configName);

            // Compute center of View
            final float centerX = (view.getX() + (view.getWidth()) / 2) - (obj.getX() + (obj.getWidth()) / 2);
            final float centerY = (view.getY() + (view.getHeight()) / 2) - (obj.getY() + (obj.getHeight()) / 2);

            // Move obj to View's center and notify on console
            obj.moveTo(centerX, centerY);
            System.out.printf(format("object created at (%d, %d)"), (int) centerX, (int) centerY);
        }
    }

    /**
     * <p>Destroys the currently selected {@link GObject}.</p>
     */
    private void destroySelected()
    {
        final GObject obj = mGame.getSelected();

        // Can't destroy if nothing's selected
        if (obj == null) {
            System.out.printf(format("no object selected"));
            return;
        }

        // Remove GObject from game
        final int id = obj.getId();
        final int ver = obj.getVersion();
        mGame.getGObjectFactory().remove(id, ver);

        // Reset selection
        mGame.setSelected(null);
        System.out.printf(format("(id(%d), version(%d) destroyed"), id, ver);
    }

    /**
     * <p>Sets whether or not the {@link View} should be room constrained (i.e. whether or not
     * {@link View#isRoomConstrained()}) should return true.</p>
     *
     * @param input console input.
     */
    private void setViewConstraint(String input)
    {
        final String[] tokens = partition(input);

        // Check if at least correct # of tokens
        if (tokens.length != 3) {
            return;
        }

        // Ignore command if boolean value wasn't entered correctly
        if (!tokens[2].equals("true") && !tokens[2].equals("false")) {
            return;
        }

        // Apply constraint toggle
        final boolean enable = Boolean.valueOf(tokens[2]);
        mGame.getView().setRoomConstrained(enable);
    }

    /**
     * <p>Separates the console input by whitespace and returns each word as an element of an array.</p>
     *
     * @param input console input.
     * @return input tokens.
     */
    private static String[] partition(String input)
    {
        return input.split("\\s+");
    }

    /**
     * <p>Returns a {@link String} containing the given input as a substring. The returned String is formatted with
     * symbols and or spacing to stand out in the console as separate from input text.</p>
     *
     * @param message message to print on console.
     * @return formatted message.
     */
    private static String format(String message)
    {
        return ">   [" + message + "]\n";
    }

    /**
     * <p>
     *     {@link ConcurrentConsoleInput} setup for debugging purposes. Enter {@link #STOP_CMD} to stop the console.
     *     All input is case-sensitive.
     * </p>
     */
    private class DebugConsole extends ConcurrentConsoleInput<String>
    {
        // Stop command shuts down console input when entered
        private static final String STOP_CMD = "stop debug";

        // Intro text provides stop command and game shutdown reminder
        private static final String INTRO_MSG = "[Debug console enabled. To stop debug mode, enter \"" + STOP_CMD +
                "\". To stop the game, enter \"" + SHUTDOWN + "\"]";

        // Closing text to notify console will stop acting on input
        private static final String OUTRO_MSG = "[Debug console now disabled]";

        /**
         * <p>Constructor for a DebugConsole.</p>
         */
        public DebugConsole()
        {
            super(INTRO_MSG, OUTRO_MSG);
        }

        @Override
        protected String onNextLine(String line)
        {
            // Ignore Strings of whitespace
            return ((line = line.trim()).isEmpty()) ? null : line;
        }

        @Override
        protected boolean isStopRequest(String line)
        {
            // Accept both debug stop command and game shutdown command
            return line.equals(STOP_CMD) || line.equals(SHUTDOWN);
        }

        @Override
        protected String createMessage(String line)
        {
            // Deal with raw Strings
            return line;
        }
    }
}
