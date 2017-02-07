package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.object.BodyFactory;
import com.cinnamon.object.GObjectFactory;
import com.cinnamon.system.Game;

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
 *
 * <p>
 *     Entry point for the Cinnamon engine.
 * </p>
 */
public class DemoDriver
{
    // Window and Canvas dimensions
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    // Game property values
    private static final String TITLE = "Simple Demo";
    private static final String DEVELOPER = "Christian Ramos";
    private static final String VERSION = "0.01a";
    private static final String VSYNC = Game.PROPERTY_ENABLE;

    public static void main(String[] args)
    {
        System.setProperty("org.lwjgl.librarypath", "");


        // Setup game info
        final Map<String, String> props = new HashMap<String, String>();
        props.put(Game.TITLE, TITLE);
        props.put(Game.DEVELOPER, DEVELOPER);
        props.put(Game.VERSION, VERSION);
        props.put(Game.VSYNC, VSYNC);

        // Setup Window for Canvas drawing
        final Window window = new Window(WIDTH, HEIGHT, props.get(Game.TITLE));

        // Prepare game resource such as factories
        final Game.Resources res = new CustomResources();

        // Prepare Canvas for drawing
        final ConcurrentSceneBuffer sceneBuffer = new ConcurrentSceneBuffer();
        final Canvas canvas;
        canvas = new DemoCanvas(window, sceneBuffer, res.getShaderFactory());

        // Begin game
        new DemoGame(res, canvas, props).start();
    }

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
}
