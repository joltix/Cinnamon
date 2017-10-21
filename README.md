**NOTICE:** Major changes are currently occurring across all packages on the revamp-1 branch. Admittedly, the build on the master branch is not yet suitable for public use and so many classes are being rewritten or removed in anticipation of the first *usable* build. This includes the addition of tests and bug fixes as well as API redesigns and more comprehensive documentation.

# Cinnamon
Cinnamon is a Java framework for building 2D games and graphics-related applications in general, built on top of LWJGL. In addition to low-level APIs from LWJGL, Cinnamon provides higher-level wrappers for accelerated development and a multi-threaded application skeleton.

This project started as an educational experience in engineering interoperable systems, building a framework for others' development, and exploring the architectures and methodologies behind video games. Due to Cinnamon's experimental nature, *the project is not intended for products releasing to market.*

## Current state
Rewriting for usability and stability. All revisions are being introduced on the revamp-1 branch.

#### Available on master
* Partial physics from Box2D *
* Zoomable camera with interpolated motion
* Background thread rendering
* Key and mouse mapping with press/release specification

#### Upcoming on revamp-1
* Asynchronous file and resource loading
* Input filters for button combinations
* Multiple windows
* Support for Xbox controllers
* 3D space partitioning tree and related shapes
* JUnit 4, Mockito, and PowerMock tests

###### * see [attribution](#attribution)

## Structure
A Cinnamon game runs, at minimum, on two threads with the secondary thread devoted to rendering operations. These two threads follow a producer-consumer relationship where the main thread buffers the game state's drawable information for the secondary to poll and draw.

Both threads are tied to two classes in particular; the **Game** class belongs to the main thread while **Canvas** belongs to the secondary. Both must be extended - the Game class for game logic and Canvas for drawing the game's state.

#### The loop
One moment in game time is called a *tick*. Operations occur per tick to determine the game's state prior to rendering. These operations typically execute from the Game class as the game loops and constantly runs the simulation.

## Demo
**Note:**
These demo game rooms use an older version of Cinnamon. Updated rooms will be available shortly after revamp-1 is merged with master.

A test room setup as a platformer with a batch file for run configurations and a readme for controls is available in [artifacts](artifacts). The demo must be run through the batch file and not the jar. Currently, this demo has only been tested on Windows * .

A console is provided to see exceptions as well as submit commands to examine the game world. These commands range from spawning a game object to reading its angle of rotation; the full list of commands are available by typing "help".

This demo can be run in fullscreen mode by changing the batch file's run resolution to "0x0". Since fullscreen obscures window controls, the ESC key has been set to exit the program.

Note that unlike the [Hello World](#hello-world) below, the initialization code for an instance of Game is separated to the [DemoDriver](com/cinnamon/demo/DemoDriver.java) class in the [demo](com/cinnamon/demo) package.

###### * this project (and therefore the demo) uses native code from LWJGL and has only been tested on Windows 10 64-bit.


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
