# Cinnamon
Cinnamon is a Java-based framework for building 2D games. This project is intended as an educational experience in engineering a system designed for extension as well as an exploration of the nuts and bolts that can drive together the various systems of a video game.

Due to this project's experimental nature, *Cinnamon is not intended for production use.* That said, hopefully things in this engine can help others in their own projects.

**Current status:** Major changes focusing on breaking up bloated classes and improving documentation

#### Available systems
* Partial physics from Box2D *
* Zoomable camera with interpolated motion
* Support for OpenGL on secondary thread
* Key and mouse mapping with press/release specification

#### Upcoming
* Multiple viewports and cameras
* Asynchronous resource loading
* Unit tests with JUnit 4
* UML class overview

###### * see [attribution](#attribution)

## Demo
A test room setup as a platformer with a batch file for run configurations and a readme for controls is available in [artifacts](artifacts). The demo must be run through the batch file and not the jar. Currently, this demo is only available on Windows * .

A console is provided to see exceptions as well as submit commands to examine the game world. These commands range from spawning a game object to reading its angle of rotation; the full list of commands are available by typing "help".

This demo can be run in fullscreen mode by changing the batch file's run resolution to "0x0". Since fullscreen obscures window controls, the ESC key has been set to exit the program.

Note that unlike the [Hello World](#hello-world) below, the initialization code for an instance of Game is separated to the [DemoDriver](com/cinnamon/demo/DemoDriver.java) class in the [demo](com/cinnamon/demo) package.

###### * this project (and therefore the demo) uses native code from LWJGL and has only been tested on Windows 10 64-bit.


## Hello World
In order to run, Cinnamon requires a subclass of each of the following (aside from [Game](com/cinnamon/system/Game.java)): [GObjectFactory](com/cinnamon/object/GObjectFactory.java), [BodyFactory](com/cinnamon/object/BodyFactory.java), [ImageFactory](com/cinnamon/gfx/ImageFactory.java), [ShaderFactory](com/cinnamon/gfx/ShaderFactory.java), and [Canvas2D](com/cinnamon/gfx/Canvas2D.java). While GObjectFactory constructs game objects, BodyFactory, ImageFactory, and ShaderFactory provide resources that define a game object's behavioral and visual configuration. The Canvas, on the other hand, is the end of the rendering pipeline responsible for OpenGL calls.

In the example below, implementations prefixed with "Example" are assumed to exist. Required method overrides for subclasses of Game have been omitted for clarity.

For an even more involved setup, see the [demo package](com/cinnamon/demo) where initialization is separated to a driver class.

```java
public class ExampleGame extends Game
{
    public static void main(String[] args)
    {
        // Create game info: title, name, version
        final Map<String, String> properties = createBasicProperties();

        // Create directory of factories for assembling game objects
        final Game.Resources res = new Resources();

        // Instantiate a window and Canvas to draw on
        final Window window = new Window(1920, 1080, properties.get(Game.TITLE), false);
        final Canvas canvas = new ExampleCanvas(window, new ConcurrentSceneBuffer(), res.getShaderFactory());

        // Begin the game
        new ExampleGame(res, null, canvas, properties).start();
    }

    /**
     *  <p>Creates a Map containing the game's title, author, and version.</p>
     */
    private static Map<String, String> createBasicProperties()
    {
        final Map<String, String> properties = new HashMap<String, String>();

        // Fill with basic game info
        properties.put(Game.TITLE, "Hello World");
        properties.put(Game.DEVELOPER, "John Smith");
        properties.put(Game.VERSION, "0.001a");

        return properties;
    }

    /**
     *  <p>Directory responsible for providing factories that assemble game objects.</p>
     */
    private static class Resources<ExampleGObject> extends Game.Resources
    {
        @Override
        public GObjectFactory<ExampleGObject> getGObjectFactory()
        {
            return new ExampleGObjectFactory<ExampleGObject>();
        }

        @Override
        public BodyFactory getBodyFactory()
        {
            return new ExampleBodyFactory();
        }

        @Override
        public ImageFactory getImageFactory()
        {
            return new ExampleImageFactory();
        }

        @Override
        public ShaderFactory getShaderFactory()
        {
            return new ExampleShaderFactory();
        }
    }
}
```


## The Structure
A game made with Cinnamon is drawn with OpenGL and centers around an instance of [Game](com/cinnamon/system/Game.java) and [Canvas2D](com/cinnamon/gfx/Canvas2D.java) working across two threads with the main thread responsible for the game state. In each game update (also known as a "tick"), a snapshot of the current state is pushed to be drawn in the second thread. In turn, the snapshot is drawn repeatedly until new data is received from the main thread.

#### Game
The main thread belongs to the Game class and performing world related tasks such as processing input, collision detection, stepping objects through their physics, and the like. The Game class should be extended and available methods overridden to access services such as keybindings as well as provide hooks into specific points of a game tick.

#### Canvas
The second thread belongs to Canvas2D and the bulk of OpenGL calls. This decouples the tickrate from framerate, allowing for 30hz in game logic with 60fps.


## Attribution
Cinnamon uses LWJGL for low level access to window creation, OpenGL, and the like. In addition, some of Box2D's source code has been ported and slightly modified for use in [IterativeSolver](com/cinnamon/object/IterativeSolver.java) and is documented as such in the class. Both LWJGL and Box2D's licenses can be found in separate aptly named folders at the root of this project.

## License
MIT License

Copyright (c) 2017 Christian Ramos

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
