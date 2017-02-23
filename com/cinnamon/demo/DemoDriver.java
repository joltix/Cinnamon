package com.cinnamon.demo;

import com.cinnamon.gfx.Canvas;
import com.cinnamon.gfx.ConcurrentSceneBuffer;
import com.cinnamon.gfx.ImageFactory;
import com.cinnamon.gfx.ShaderFactory;
import com.cinnamon.object.BodyFactory;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.system.ControlMap;
import com.cinnamon.system.EventHub;
import com.cinnamon.system.Game;
import com.cinnamon.system.Window;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     The Cinnamon engine uses LWJGL. The following is LWJGL's license.
 * </p>
 *
 * <p>
 *     Copyright (c) 2012-present Lightweight Java Game Library
 *     All rights reserved.
 *
 *     Redistribution and use in source and binary forms, with or without
 *     modification, are permitted provided that the following conditions are
 *     met:
 *
 *     - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *     - Neither the name Lightweight Java Game Library nor the names of
 *     its contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 *     THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *     "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *     TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *     PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *     OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *     SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *     LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *     DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *     THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *     (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *     OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *</p>
 * <br><br>
 * <p>
 *     Entry point for the Cinnamon engine.
 * </p>
 *
 * <p>
 *     Arguments must be in one of two formats, either specifying an explicit resolution <i>{@literal <#x#> <true|false
 *     debug> <tickrate> <true|false vsync>}</i> or the subset <i>{@literal <true|false debug> <tickrate> <true|false
 *     vsync>}</i> where the latter format will cause the {@link Window} to launch in fullscreen.
 * </p>
 *
 * <p>
 *     The Window will only be resizable when using the explicit resolution format with {@literal <i><debug></i>} as
 *     true. The current minimum supported Window size is {@link Window#MINIMUM_WIDTH} x {@link Window#MINIMUM_HEIGHT}.
 *     If a size smaller than the minimum or larger than the display supports is given, the Window will default to
 *     fullscreen and act as if the fullscreen resolution format was used.
 * </p>
 */
public class DemoDriver
{
    // Game property values
    private static final String TITLE = "Simple Demo";
    private static final String DEVELOPER = "Christian Ramos";
    private static final String VERSION = "0.01a";
    private static final String TICK_SAMPLES = "3";

    // Argument count for explicit resolution format
    private static final int EXPLICIT_RES_ARGS = 4;

    // Argument count for fullscreen arg format
    private static final int FULL_RES_ARGS = 3;

    // Argument format for explicit resolution
    private static final String ARG_FORMAT = "<#x#> <true|false debug> <tickrate> <true|false vsync>";

    public static void main(String[] args)
    {
        // Check argument count mismatch
        if (args.length != FULL_RES_ARGS && args.length != EXPLICIT_RES_ARGS) {
            throw new IllegalArgumentException("CL arguments must be in one of two formats: " + ARG_FORMAT + " where " +
                    "the resolution argument \"<#x#>\" is optional");
        }

        // Translate arguments to properties to be fed to Game
        final Map<String, String> props = buildPropertiesFromArgs(args);

        // Read resolution from arguments if explicit length
        final int[] resolution;
        if (args.length == EXPLICIT_RES_ARGS) {
            resolution = getResolution(0, args);
        } else {
            // Dimensions higher than display can handle will force fullscreen
            resolution = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE};
        }

        // Check if debug was enabled from args
        final boolean debugEnabled = props.get(Game.DEBUG_MODE).equals(Game.PROPERTY_ENABLE);

        // Setup Window for Canvas drawing
        final Window window = new Window(resolution[0], resolution[1], props.get(Game.TITLE), debugEnabled);

        // Prepare game resource such as factories
        final Game.Resources res = new CustomResources();

        // Prepare Canvas for drawing
        final ConcurrentSceneBuffer sceneBuffer = new ConcurrentSceneBuffer();
        final Canvas canvas = new DemoCanvas(window, sceneBuffer, res.getShaderFactory());

        // Begin game
        new DemoGame(res, new CustomServices(), canvas, props).start();
    }

    /**
     * <p>Builds a {@link Map} of properties to submit to {@link Game}'s constructor based off of the given
     * arguments.</p>
     *
     * @param args cl arguments.
     * @return properties.
     * @throws IllegalArgumentException if either the debug, tickrate, or vsync arguments' values are invalid (e.g.
     * tickrate's value is a negative floating point).
     */
    private static Map<String, String> buildPropertiesFromArgs(String[] args)
    {
        // Setup game info
        final Map<String, String> props = new HashMap<String, String>();
        props.put(Game.TITLE, TITLE);
        props.put(Game.DEVELOPER, DEVELOPER);
        props.put(Game.VERSION, VERSION);
        props.put(Game.RATE_SAMPLES, TICK_SAMPLES);

        // Determine index of debug mode arg based on whether format provides explicit resolution
        final int debugArg = (args.length == EXPLICIT_RES_ARGS) ? 1 : 0;

        // Read and apply arg for debug
        final String debug = getBooleanArg(debugArg, args);
        if (debug == null) {
            throw new IllegalArgumentException("Debug argument must be either \"true\" or \"false\"");
        }
        props.put(Game.DEBUG_MODE, getBooleanArg(debugArg, args));

        // Apply desired tickrate
        final String tickrate = getIntArg(debugArg + 1, args);
        if (tickrate == null || Integer.parseInt(tickrate) <= 0) {
            throw new IllegalArgumentException("Tickrate argument must be an int > 0");
        }
        props.put(Game.TICKRATE, getIntArg(debugArg + 1, args));

        // Apply vsync arg
        final String vsync = getBooleanArg(debugArg + 2, args);
        if (vsync == null) {
            throw new IllegalArgumentException("Vsync argument must be either \"true\" or \"false\"");
        }
        props.put(Game.VSYNC, getBooleanArg(debugArg + 2, args));
        return props;
    }

    /**
     * <p>Returns either {@link Game#PROPERTY_ENABLE} or {@link Game#PROPERTY_DISABLE} depending on whether or not
     * the argument specified by the given index is "true" or "false", respectively.</p>
     *
     * @param index argument index.
     * @param args arguments.
     * @return either PROPERTY_ENABLE or PROPERTY_DISABLE.
     */
    private static String getBooleanArg(int index, String[] args)
    {
        final String argVal = args[index];
        final boolean argTrue = argVal.equals("true");
        final boolean argFalse = argVal.equals("false");
        final String translation;

        // Check if argument if a proper "true" | "false"
        if (argTrue) {
            translation = Game.PROPERTY_ENABLE;
        } else if (argFalse) {
            translation = Game.PROPERTY_DISABLE;
        } else {
            throw new IllegalArgumentException("Argument[" + index + "] must be \"true\" or \"false\":" + args[index]);
        }

        return translation;
    }

    /**
     * <p>Checks whether or not the argument specified by the given index is an int. If the argument is not an int,
     * this method returns null.</p>
     *
     * @param index argument index.
     * @param args arguments.
     * @return argument, or null if it is not a valid int.
     */
    private static String getIntArg(int index, String[] args)
    {
        try {
            final int val = Integer.valueOf(args[index]);
            return String.valueOf(val);
        } catch (NumberFormatException e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <p>Gets the width and height dimensions of an argument formatted as a resolution (e.g. "1920x1080").</p>
     *
     * @param index argument index.
     * @param args arguments.
     * @return two element array holding width and height, in that order, or null if the dimensions are not ints.
     */
    private static int[] getResolution(int index, String[] args)
    {
        final String argVal = args[index];

        // Check if valid formatting
        if (!argVal.matches("[0-9]+x[0-9]+")) {
            throw new IllegalArgumentException("Resolution argument must be formatted like so: 1920x1080");
        }

        // Split resolution by the 'x' and convert each dimension to an int
        final String[] resTokens = argVal.split("x");
        try {
            // Try to convert tokens to dimensions
            final int width = Integer.valueOf(resTokens[0]);
            final int height = Integer.valueOf(resTokens[1]);

            return new int[] {width, height};

        } catch (NumberFormatException e) {
            // Should never occur given the regex format check at the beginning
            e.printStackTrace();
            return null;
        }
    }

    /**
     * <p>
     *     Provides the {@link Game} with a directory of customized resources for constructing game objects.
     * </p>
     */
    private static class CustomResources extends Game.Resources
    {
        private ShaderFactory mShaderFactory = new DemoShaderFactory();
        private ImageFactory mImgFactory = new DemoImageFactory(mShaderFactory);
        private GObjectFactory mGObjectFactory = new DemoGObjectFactory(this);
        private BodyFactory mBodyFactory = new DemoBodyFactory();

        @Override
        public ShaderFactory getShaderFactory()
        {
            return mShaderFactory;
        }

        @Override
        public ImageFactory getImageFactory()
        {
            return mImgFactory;
        }

        @Override
        public GObjectFactory getGObjectFactory()
        {
            return mGObjectFactory;
        }

        @Override
        public BodyFactory getBodyFactory()
        {
            return mBodyFactory;
        }
    }

    /**
     * <p>
     *     Returns null for each method returning a service. This tells {@link Game} to provide its own
     *     implementations.
     * </p>
     */
    private static class CustomServices extends Game.Services
    {
        @Override
        public EventHub getEventHub()
        {
            return null;
        }

        @Override
        public ControlMap getControlMap()
        {
            return null;
        }
    }
}
