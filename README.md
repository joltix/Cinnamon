# Cinnamon
Cinnamon is a Java framework for building 2D games intended as an educational hands-on exploration of engineering a large system.

## Sounds like reinventing the wheel
The purpose of this project is to gain exposure to and a better understanding of different algorithms, data structures, and problems that arise in their interaction through application. Plus, who doesn't want to know how video games work?

## Can I use this for my own game?
For now, it's not recommended. The Cinnamon engine is currently very much incomplete, buggy, and prone to frequent changes.

## The Structure
A game made with Cinnamon is drawn with OpenGL and centers around an instance of [Game](com/cinnamon/system/Game.java) and [Canvas2D](com/cinnamon/gfx/Canvas2D.java) working across two threads. In each game update (also known as a "tick"), a snapshot of the current visible scene is pushed to be drawn in the second thread. In turn, the snapshot is drawn as soon as possible, with the canvas redrawing the same scene if no new data is available.

It should be noted that this project (and therefore the [demo](#demo)) has only been tested on Windows 10 64-bit, though more systems are planned to officially be supported.

#### Game
The main thread belongs to Game and performing game world related tasks such as processing input, collision detection, stepping objects through their physics, and the like.

#### Canvas
The second thread belongs to Canvas2D and the bulk of OpenGL calls. This helps ease the time it takes to process a game update since updating the game's state occurs in parallel to drawing (the previous state).

## Demo
A demo with a batch file for run configurations and a readme for controls is available in [artifacts](artifacts). The demo must be run through the batch file and not the jar. Currently, this demo is only available to be run on Windows.

A console is provided to see exceptions as well as submit commands to examine the game world. These commands range from spawning a game object to reading an object's rotation; a full list is shown by typing "help". This demo consists of a simple room with simplistic physics displayed in fullscreen and using the ESC key to exit.

Note that unlike the [Hello World](#hello-world) example, the initialization code for a Game instance is separated to the [DemoDriver](com/cinnamon/demo/DemoDriver.java) class in the demo.

## Hello World
In order to run, Cinnamon requires a subclass of each of the following (aside from [Game](com/cinnamon/system/Game.java)): [GObjectFactory](com/cinnamon/object/GObjectFactory.java), [BodyFactory](com/cinnamon/object/BodyFactory.java), [ImageFactory](com/cinnamon/gfx/ImageFactory.java), [ShaderFactory](com/cinnamon/gfx/ShaderFactory.java), and [Canvas2D](com/cinnamon/gfx/Canvas2D.java). Each of these factory classes provide game specific object configurations while the Canvas implementation facilitates drawing.

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
     *  <p>
     *      Directory responsible for providing factories that assemble game objects.
     *  </p>
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
