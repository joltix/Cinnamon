package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.object.*;
import com.cinnamon.system.*;
import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.Vector2F;

import java.util.Map;
import java.util.Random;

/**
 * <p>
 *     Demo {@link Game}.
 * </p>
 *
 * <p>
 *     <b>[Current controls]</b>
 *     <br><i>Left arrow:</i> move left relative to gravity</br>
 *     <br><i>Right arrow:</i> move right relative to gravity</br>
 *     <br><i>Up arrow:</i> jump relative to gravity</br>
 *
 *     <br></br>
 *
 *     <br><i>Space:</i> fire projectile from selected object to cursor</br>
 *     <br><i>Left click:</i> select object (or deselect if nothing selectable was clicked)</br>
 *     <br><i>Right click:</i> change gravity's direction towards cursor relative from center of view</br>
 * </p>
 */
public class DemoGame extends Game
{
    // Random number gen used in adding variation when creating game objects
    private static final Random mRanGen = new Random(System.nanoTime());

    /**
     * Cloud generation controls
     */

    // Number of clouds
    private static final int CLOUD_COUNT = 25;

    // Minimum cloud width allowed
    private static final float CLOUD_MIN_WIDTH = 0.25f;

    // Maximum cloud width allowed
    private static final float CLOUD_MAX_WIDTH = 12f;

    /**
     * Grass generation controls
     */

    // Number of grass objects to make
    private static final int GRASS_COUNT = 30;

    // Minimum width allowed
    private static final float GRASS_MIN_WIDTH = 0.75f;

    // Maximum height allowed
    private static final float GRASS_MAX_WIDTH = 2.5f;

    // Minimum z layer allowed
    private static final float GRASS_MIN_Z = 0f;

    // Maximum z layer allowed
    private static final float GRASS_MAX_Z = 10;

    /**
     * Mountain generation controls
     */

    // Number of mountains to generate
    private static final int MOUNTAIN_COUNT = 6;

    // Minimum mountain width
    private static final float MOUNTAIN_MIN_WIDTH = 2.5f;

    // Maximum mountain width
    private static final float MOUNTAIN_MAX_WIDTH = 45f;

    // Minimum z allowed for mountain
    private static final int MOUNTAIN_MIN_Z = 10;

    // Maximum z allowed for mountain
    private static final int MOUNTAIN_MAX_Z = 20;

    /**
     * Player object
     */

    // Player's z drawing layer
    private static final float PLAYER_Z = 5f;

    // Player's coefficient of restitution
    private static final float PLAYER_RESTITUTION = 0.3f;

    // Player's coefficient of friction
    private static final float PLAYER_FRICTION = 0.8f;

    // Player's left/right speed in meters per sec
    private static final float mStepSize = 1f;

    // Player's jump speed in meters per second
    private static final float mJumpSize = 10f;

    /**
     * Projectile
     */

    // Projectile mass in kilograms
    private static final float PROJECTILE_MASS = 1f;

    // Projectile coefficient of friction
    private static final float PROJECTILE_FRICTION = 0.3f;

    // Projectile coefficient of restitution
    private static final float PROJECTILE_RESTITUTION = 0.8f;

    /**
     * Walls
     */

    // Wall mass in kilograms
    private static final float WALL_MASS = 1000000f;

    // Wall coefficient of friction
    private static final float WALL_FRICTION = 0.6f;

    // Wall coefficient of restitution
    private static final float WALL_RESTITUTION = 0.8f;

    /**
     * Velocity vector used in onUpdate() to rotate object images towards velocity
     */
    private final Vector2F mImgDirection = new Vector2F();

    /**
     * <p>Shuts down the game.</p>
     */
    private KeyEventHandler mCloseAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent event)
        {
            DemoGame.this.stop();
        }
    };

    /**
     * <p>Adds an impulse going up (relative to gravity's direction).</p>
     */
    private KeyEventHandler mUpAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final Vector2F jumpVector = getSolver().getGlobalAcceleration();
            jumpVector.negate();
            jumpVector.normalize();

            // Add vertical impulse
            final BodyComponent body = obj.getBodyComponent();
            body.setImpulse(jumpVector.multiply(mJumpSize));
        }
    };

    /**
     * <p>Adds an impulse to the left (relative to gravity's direction).</p>
     */
    private KeyEventHandler mLeftAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final Vector2F leftVector = getSolver().getGlobalAcceleration().getNormal(true);
            leftVector.normalize();
            leftVector.multiply(-mStepSize);

            obj.getImageComponent().setFlipHorizontally(true);

            // Don't add impulse if not resting on a surface
            final BodyComponent body = obj.getBodyComponent();
            body.addImpulse(leftVector);
        }
    };


    /**
     * <p>Adds an impulse to the right (relative to gravity's direction).</p>
     */
    private KeyEventHandler mRightAction = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent keyEvent)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final Vector2F rightVector = getSolver().getGlobalAcceleration().getNormal(true);
            rightVector.normalize();
            rightVector.multiply(mStepSize);

            obj.getImageComponent().setFlipHorizontally(false);

            // Don't add impulse if not resting on a surface
            final BodyComponent body = obj.getBodyComponent();
            body.addImpulse(rightVector);
        }
    };

    /**
     * <p>Zooms the {@link View} in and out with scrolling.</p>
     */
    private MouseEventHandler mZoom = new MouseEventHandler()
    {
        @Override
        public void handle(MouseEvent event)
        {
            final View view = getView();
            final float scale = view.getScale();

            // Alter view's scale depending on scroll direction
            if (event.isScrollForward()) {
                view.setScale(scale + 1.5f);
            } else {
                view.setScale(scale - 1.5f);
            }
        }
    };

    /**
     * <p>Changes the direction of the {@link Solver}'s global acceleration in the direction of the cursor relative
     * to the center of the {@link View}.</p>
     */
    private MouseEventHandler mGravityHandler = new MouseEventHandler()
    {
        @Override
        public void handle(MouseEvent event)
        {
            final View view = getView();
            view.translateToWorld(event);

            // Get direction from click to View's center
            final Vector2F gravity = new Vector2F(event.getX(), event.getY());
            gravity.subtract(view.getCenterX(), view.getCenterY());

            // Scale new gravity vector's strength to 9.8 meters per second per second
            gravity.normalize();
            gravity.multiply(9.8f);

            // Set new gravity to affect all non-static game objects
            getSolver().setGlobalAcceleration(gravity);
        }
    };

    /**
     * <p>Creates a projectile at the selected {@link GObject} and applies an impulse of 30 meters per second.
     * toward the cursor.</p>
     */
    private KeyEventHandler mFireHandler = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent event)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final GObject projectile = createProjectile(getGObjectFactory(), getShaderFactory());
            projectile.moveToCenter(obj.getCenterX(), obj.getCenterY());

            projectile.getImageComponent().setOffsets(0f, 0f);

            // Apply firing velocity
            final Point2F mouse = getMousePosition();
            getView().translateToWorld(mouse);
            final Vector2F firingVector = new Vector2F(mouse.getX() - obj.getX(), mouse.getY() - obj.getY());
            firingVector.normalize();
            firingVector.multiply(30f);
            projectile.getBodyComponent().setVelocity(firingVector);
        }
    };

    public DemoGame(Resources resources, Services services, Canvas canvas, Map<String, String> properties)
    {
        super(resources, services, canvas, properties);
    }

    @Override
    protected void onBegin()
    {
        // Create Room
        final Room room = new Room(getResources(), 35f,80f, Texture.NULL, "Room 1");
        room.setBackgroundTint(0.67f, 0.86f, 0.94f);
        this.setRoom(room);

        // Fetch resources
        final GObjectFactory goFactory = getGObjectFactory();
        final ShaderFactory shaders = getShaderFactory();

        // Create game object for user control
        final GObject player = createPlayer(goFactory, shaders);

        // Zoom view in to fill room and center on player
        final View view = getView();
        view.setRoomConstrained(true);
        view.setScale(1f);
        view.moveToCenter(player);

        // Generate clouds in sky
        createClouds(goFactory, shaders);

        // Generate grass along ground
        createGrass(goFactory, shaders);

        // Create ramp in left side
        createRamp(goFactory);

        // Generate mountains in background
        createMountains(goFactory, shaders);

        // Create boundary walls around room to contain game objects
        createWalls(goFactory);

        // Attach selected object motion to arrow keys
        final ControlMap input = getControlMap();
        input.attach(KeyEvent.Key.KEY_UP, mUpAction);
        input.setMode(KeyEvent.Key.KEY_UP, true, false);
        input.attach(KeyEvent.Key.KEY_RIGHT, mRightAction);
        input.setMode(KeyEvent.Key.KEY_RIGHT, true, true);
        input.attach(KeyEvent.Key.KEY_LEFT, mLeftAction);
        input.setMode(KeyEvent.Key.KEY_LEFT, true, true);

        // Allow selected object to fire projectile
        input.attach(KeyEvent.Key.KEY_SPACE, mFireHandler);
        input.setMode(KeyEvent.Key.KEY_SPACE, true, false);

        // Allow game shutdown from ESC key
        input.attach(KeyEvent.Key.KEY_ESCAPE, mCloseAction);

        // Zoom in and out
        input.attach(MouseEvent.Button.MIDDLE, mZoom);
        input.setMode(MouseEvent.Button.MIDDLE, false, true);

        // Allow gravity changes
        input.attach(MouseEvent.Button.RIGHT, mGravityHandler);
        input.setMode(MouseEvent.Button.RIGHT, false, false);
    }

    private GObject createProjectile(GObjectFactory goFactory, ShaderFactory shaders)
    {
        final GObject bullet = getGObjectFactory().get("char");
        final ImageComponent image = bullet.getImageComponent();
        final BodyComponent body = bullet.getBodyComponent();

        // Set projectile texture
        final Texture texture = getShaderFactory().getTexture("demo_projectile.png");
        image.setTexture(texture.getId());

        // Randomize color to identify individual bullets
        final Random ranGen = new Random(System.nanoTime());
        image.setTint(ranGen.nextFloat() + 0.7f, ranGen.nextFloat() + 0.7f, ranGen.nextFloat() + 0.7f);

        // Assemble bullet
        body.setMass(PROJECTILE_MASS);
        body.setFriction(PROJECTILE_FRICTION);
        body.setRestitution(PROJECTILE_RESTITUTION);
        body.setCollidable(true);
        bullet.setImageComponent(image);
        bullet.setBodyComponent(body);
        bullet.setWidth(0.5f);
        bullet.setHeight(0.5f);

        return bullet;
    }

    /**
     * <p>Creates the initial game object under user control.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce game objects.
     * @param shaders {@link ShaderFactory} for referencing character texture.
     */
    private GObject createPlayer(GObjectFactory goFactory, ShaderFactory shaders)
    {
        // Get game object to act as player
        final GObject obj = goFactory.get("char");
        final int texture = shaders.getTexture("demo_character.png").getId();

        obj.setWidth(1.5f);
        obj.setHeight(2f);
        this.setSelected(obj);

        // Setup visual
        final ImageComponent image = obj.getImageComponent();
        image.setTexture(texture);
        image.setTint(0.8f, 1f, 1f);
        image.setOffsets(0f, -0.25f);

        // Setup collision and physics
        final BodyComponent body = obj.getBodyComponent();
        body.setCollidable(true);
        body.setStatic(false);
        body.setRestitution(PLAYER_RESTITUTION);
        body.setFriction(PLAYER_FRICTION);

        obj.moveToCenter(getRoom().getWidth() / 2f, getRoom().getHeight() - 15f);
        obj.moveBy(0f, 0f, PLAYER_Z);

        return obj;
    }

    /**
     * <p>Creates cloud game objects and scatters them about the room.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce game objects.
     * @param shaders {@link ShaderFactory} for referencing cloud texture.
     */
    private void createClouds(GObjectFactory goFactory, ShaderFactory shaders)
    {
        // Get cloud texture
        final Texture cloudTexture = shaders.getTexture("demo_cloud.png");
        final int texture = cloudTexture.getId();
        final float heightRatio = (float) cloudTexture.getHeight() / cloudTexture.getWidth();

        // Area placement limits
        final Room room = getRoom();
        final int maxX = (int) room.getWidth();
        final int minY = (int) (room.getHeight() * (1f / 6f));
        final int rangeY = (int) (room.getHeight() * (5f / 6f));

        // Generate clouds
        for (int i = 0; i < CLOUD_COUNT; i++) {
            final GObject cloud = goFactory.get("char");

            // Set cloud image
            final ImageComponent image = cloud.getImageComponent();
            image.setTexture(texture);
            image.setFlipHorizontally(mRanGen.nextBoolean());
            image.setFlipVertically(mRanGen.nextBoolean());

            // Apply dimensions
            final float width = ((CLOUD_MAX_WIDTH - CLOUD_MIN_WIDTH) * mRanGen.nextFloat()) + CLOUD_MIN_WIDTH;
            cloud.setWidth(width);
            cloud.setHeight(cloud.getWidth() * heightRatio);

            // Place somewhere in the room's top half
            cloud.moveToCenter(mRanGen.nextInt(maxX), mRanGen.nextInt(rangeY) + minY);

            // No collision or physics needed
            cloud.setBodyComponent(null);
        }
    }

    /**
     * <p>Creates an untextured black ramp in the left side of the {@link Room}.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce the ramp.
     */
    private void createRamp(GObjectFactory goFactory)
    {
        final GObject ramp = goFactory.get("char");

        // Make ramp black
        final ImageComponent image = ramp.getImageComponent();
        image.setTexture(Texture.NULL);
        image.setTint(0f, 0f, 0f);
        image.setTransparency(1f);
        image.setOffsets(0f, 0.2f);

        // Make ramp like walls (solid but not selectable)
        final BodyComponent body = ramp.getBodyComponent();
        body.setSelectable(false);
        body.setCollidable(true);
        body.setMass(WALL_MASS);
        body.setFriction(WALL_FRICTION);
        body.setRestitution(WALL_RESTITUTION);

        // Size
        ramp.setWidth(30f);
        ramp.setHeight(10f);

        // Place angled in bottom left corner
        ramp.moveTo(-10f, -7f, -Float.MAX_VALUE);
        ramp.rotateTo(-Math.PI / 8d);
        body.setStatic(true);
    }

    /**
     * <p>Creates static non-collidable grass in front of mountains and around the player.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce game objects.
     * @param shaders {@link ShaderFactory} for referencing grass texture.
     */
    private void createGrass(GObjectFactory goFactory, ShaderFactory shaders)
    {
        // Get texture and height ratio for sizing
        final Texture grassTexture = shaders.getTexture("demo_grass.png");
        final int texture = grassTexture.getId();
        final float heightRatio = (float) grassTexture.getHeight() / grassTexture.getWidth();

        // Max x possible for placement
        final int maxX = (int) getRoom().getWidth();

        for (int i = 0; i < GRASS_COUNT; i++) {
            // Randomize size while keeping texture's size ratio then randomize location on ground
            final GObject object = goFactory.get("char");
            final float width = ((GRASS_MAX_WIDTH - GRASS_MIN_WIDTH) * mRanGen.nextFloat()) + GRASS_MIN_WIDTH;
            object.setWidth(width * 1.5f);
            object.setHeight(width * heightRatio);

            // Randomize location along ground, horizontally centered to itself.
            final float z = ((GRASS_MAX_Z - GRASS_MIN_Z) * mRanGen.nextFloat()) + GRASS_MIN_Z;
            object.moveTo(mRanGen.nextInt(maxX) - (object.getWidth() / 2f), 0f, z);

            // Apply grass texture and randomize horizontal flip
            final ImageComponent image = object.getImageComponent();
            image.setTexture(texture);
            image.setFlipHorizontally(mRanGen.nextBoolean());

            // No collision or physics needed
            object.setBodyComponent(null);
        }
    }

    /**
     * <p>Creates static non-collidable mountains in the background.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce game objects.
     * @param shaders {@link ShaderFactory} for referencing mountain texture.
     */
    private void createMountains(GObjectFactory goFactory, ShaderFactory shaders)
    {
        final Texture mountainTexture = shaders.getTexture("demo_mountain.png");
        final int texture = mountainTexture.getId();
        final float heightRatio = (float) mountainTexture.getHeight() / mountainTexture.getWidth();

        final int maxX = (int) getRoom().getWidth();

        for (int i = 0; i < MOUNTAIN_COUNT; i++) {
            final GObject object = goFactory.get("char");
            final float width = ((MOUNTAIN_MAX_WIDTH - MOUNTAIN_MIN_WIDTH) * mRanGen.nextFloat()) + MOUNTAIN_MIN_WIDTH;
            object.setWidth(width);
            object.setHeight(width * heightRatio);

            // Move to location
            final float z = ((MOUNTAIN_MAX_Z - MOUNTAIN_MIN_Z) * mRanGen.nextFloat()) + MOUNTAIN_MIN_Z;
            object.moveTo(mRanGen.nextInt(maxX) - (object.getWidth() / 2f), 0f, z);

            // Setup image
            final ImageComponent image = object.getImageComponent();
            image.setTexture(texture);
            image.setFlipHorizontally(mRanGen.nextBoolean());
            image.setOffsets(0f, -1.2f);

            // No collision or physics needed
            object.setBodyComponent(null);
        }
    }

    /**
     * <p>Creates 500 unit thick collidable static walls around the {@link Room} to keep other game objects inside.
     * Each wall has an {@link ImageComponent} to draw each wall in black to hide game object's edges sticking
     * beyond the Room's edges.</p>
     *
     * @param goFactory {@link GObjectFactory} to produce game objects.
     */
    private void createWalls(GObjectFactory goFactory)
    {
        final float z = -Float.MAX_VALUE;

        // Create and place north wall
        final GObject northWall = goFactory.get("char");

        final ImageComponent northImage = northWall.getImageComponent();
        northImage.setTexture(Texture.NULL);
        northImage.setTint(0f, 0f, 0f);
        northImage.setOffsets(0f, 0f);

        final BodyComponent northBody = northWall.getBodyComponent();
        northWall.setWidth(getRoom().getWidth() + 1000f);
        northWall.setHeight(500f);
        northWall.moveTo(-500f, getRoom().getHeight(), z);
        northWall.setBodyComponent(northBody);
        northBody.setSelectable(false);
        northBody.setCollidable(true);
        northBody.setStatic(true);
        northBody.setMass(WALL_MASS);
        northBody.setRestitution(WALL_RESTITUTION);
        northBody.setFriction(WALL_FRICTION);

        // Create and place south wall
        final GObject southWall = goFactory.get("char");

        final ImageComponent southImage = southWall.getImageComponent();
        southImage.setTexture(Texture.NULL);
        southImage.setTint(0f, 0f, 0f);
        southImage.setOffsets(0f, 0f);

        final BodyComponent southBody = southWall.getBodyComponent();
        southWall.setWidth(getRoom().getWidth() + 1000f);
        southWall.setHeight(500f);
        southWall.moveTo(-500f, -500f, z);
        southWall.setBodyComponent(southBody);
        southBody.setSelectable(false);
        southBody.setCollidable(true);
        southBody.setStatic(true);
        southBody.setMass(WALL_MASS);
        southBody.setRestitution(WALL_RESTITUTION);
        southBody.setFriction(WALL_FRICTION);

        // Create and place east wall
        final GObject eastWall = goFactory.get("char");

        final ImageComponent eastImage = eastWall.getImageComponent();
        eastImage.setTexture(Texture.NULL);
        eastImage.setTint(0f, 0f, 0f);
        eastImage.setOffsets(0f, 0f);

        final BodyComponent eastBody = eastWall.getBodyComponent();
        eastWall.setWidth(500f);
        eastWall.setHeight(getRoom().getHeight() + 1000f);
        eastWall.moveTo(getRoom().getWidth(), -500f, z);
        eastWall.setBodyComponent(eastBody);
        eastBody.setSelectable(false);
        eastBody.setCollidable(true);
        eastBody.setStatic(true);
        eastBody.setMass(WALL_MASS);
        eastBody.setRestitution(WALL_RESTITUTION);
        eastBody.setFriction(WALL_FRICTION);

        // Create and place west wall
        final GObject westWall = goFactory.get("char");

        final ImageComponent westImage = westWall.getImageComponent();
        westImage.setTexture(Texture.NULL);
        westImage.setTint(0f, 0f, 0f);
        westImage.setOffsets(0f, 0f);

        final BodyComponent westBody = westWall.getBodyComponent();
        westWall.setWidth(500f);
        westWall.setHeight(getRoom().getHeight() + 1000f);
        westWall.moveTo(-500f, -500f, z);
        westWall.setBodyComponent(westBody);
        westBody.setSelectable(false);
        westBody.setCollidable(true);
        westBody.setStatic(true);
        westBody.setMass(WALL_MASS);
        westBody.setRestitution(WALL_RESTITUTION);
        westBody.setFriction(WALL_FRICTION);
    }

    @Override
    protected void onUpdate()
    {
        final GObject selected = getSelected();
        if (selected != null) {
            // Move View to center on selected GObject
            final View view = getView();
            view.moveToCenter(selected);

            flipObjectTowardsCursor(selected);
        }
    }

    /**
     * <p>Flip the given {@link GObject}'s {@link ImageComponent} horizontally to face towards the cursor.</p>
     *
     * @param object game object.
     */
    private void flipObjectTowardsCursor(GObject object)
    {
        // Convert mouse position to world coordinates
        final View view = getView();
        final Point2F position = getMousePosition();
        view.translateToWorld(position);

        // Align vector from view center to mouse position
        final float relativeX = position.getX() - object.getCenterX();
        final float relativeY = position.getY() - object.getCenterY();
        mImgDirection.set(relativeX, relativeY);

        // Flip image horizontally towards mouse
        final double angle = mImgDirection.getAngle();
        object.getImageComponent().setFlipHorizontally(angle > (Math.PI / 2d) && angle < (3d * Math.PI) / 2d);
    }

    @Override
    protected void onEnd()
    {
        getGObjectFactory().clear();
        getBodyFactory().clear();
        getImageFactory().clear();
    }
}
