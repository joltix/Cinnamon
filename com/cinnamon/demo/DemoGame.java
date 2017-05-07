package com.cinnamon.demo;

import com.cinnamon.gfx.*;
import com.cinnamon.object.*;
import com.cinnamon.system.*;
import com.cinnamon.utils.*;

import java.util.Map;
import java.util.Random;

/**
 * <p>
 *     Demo {@link Game}.
 * </p>
 *
 * <p>
 *     <b>[Current controls]</b>
 *     <br><i>Left arrow:</i> move left</br>
 *     <br><i>Right arrow:</i> move right</br>
 *     <br><i>Up arrow:</i> jump</br>
 *
 *     <br></br>
 *
 *     <br><i>Left CTRL:</i> switch between free cam and following selected game object</br>
 *     <br><i>Space:</i> move View to zoom in on and look at the selected game object</br>
 *     <br><i>Left click:</i> select object (or deselect if nothing selectable was clicked)</br>
 *     <br><i>Right click:</i> fire projectile from selected object to cursor</br>
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
    private static final int GRASS_COUNT = 7;

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
    private static final float PLAYER_RESTITUTION = 0.2f;

    // Player's coefficient of friction
    private static final float PLAYER_FRICTION = 0.9f;

    // Player's mass
    private static final float PLAYER_MASS = 5f;

    // Player's left/right speed in meters per sec
    private static final float mStepSize = 0.35f;

    // Player's jump speed in meters per second
    private static final float mJumpSize = 6f;

    /**
     * Projectile
     */

    // Projectile mass in kilograms
    private static final float PROJECTILE_MASS = 0.00125f;

    // Projectile coefficient of friction
    private static final float PROJECTILE_FRICTION = 0.8f;

    // Projectile coefficient of restitution
    private static final float PROJECTILE_RESTITUTION = 0.6f;

    /**
     * Walls
     */

    // Wall mass in kilograms
    private static final float WALL_MASS = 0f;

    // Wall coefficient of friction
    private static final float WALL_FRICTION = 0.9f;

    // Wall coefficient of restitution
    private static final float WALL_RESTITUTION = 1f;

    // Vector for determining which direction to flip image towards
    private final Vector2F mImgDirection = new Vector2F();

    // Vector used for cutting player's velocity in sudden turns
    private final Vector2F mPlayerVelocity = new Vector2F();

    /**
     * <p>
     *     Shuts down the game.
     * </p>
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
     * <p>
     *     Adds an impulse going up (relative to gravity's direction).
     * </p>
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
     * <p>
     *     Adds an impulse to the left (relative to gravity's direction).
     * </p>
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

            final BodyComponent body = obj.getBodyComponent();
            body.getVelocity(mPlayerVelocity);

            // Cut speed down if changing directions
            if (mPlayerVelocity.getX() > 0f) {
                mPlayerVelocity.divide(1.1f);
                body.setVelocity(mPlayerVelocity);
            }

            // Apply left impulse
            body.addImpulse(leftVector);
        }
    };


    /**
     * <p>
     *     Adds an impulse to the right (relative to gravity's direction).
     * </p>
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

            final BodyComponent body = obj.getBodyComponent();
            body.getVelocity(mPlayerVelocity);

            // Cut speed down if changing directions
            if (mPlayerVelocity.getX() < 0f) {
                mPlayerVelocity.divide(1.1f);
                body.setVelocity(mPlayerVelocity);
            }

            // Apply right impulse
            body.addImpulse(rightVector);
        }
    };

    /**
     * <p>
     *     Zooms the {@link View} in and out with scrolling.
     * </p>
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
     * <p>
     *     Creates a projectile at the selected and applies an impulse of 30 meters per second towards the cursor.
     * </p>
     */
    private MouseEventHandler mFireHandler = new MouseEventHandler()
    {
        @Override
        public void handle(MouseEvent event)
        {
            final GObject obj = getSelected();
            if (obj == null) {
                return;
            }

            final GObject projectile = createProjectile();
            projectile.moveToCenter(obj.getCenterX(), obj.getCenterY());
            projectile.getImageComponent().setOffsets(0f, 0f);

            // Apply firing velocity
            final Point2F mouse = getMousePosition();
            getView().translateToWorld(mouse);
            final Vector2F firingVector = new Vector2F(mouse.getX() - obj.getCenterX(), mouse.getY() - obj.getCenterY());
            firingVector.normalize();
            firingVector.multiply(20f);

            // Add firing velocity to body and have it ignore collisions with the player
            final BodyComponent body = projectile.getBodyComponent();
            body.setVelocity(firingVector);
            body.setIgnoreGObjectParent(true);
            projectile.setParent(obj);
        }
    };

    /**
     * <p>
     *     Move towards and zoom in on the selected GObject.
     * </p>
     */
    private KeyEventHandler mLookAtPlayerHandler = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent event)
        {
            // Can't home in if nothing selected to move towards
            final GObject selected = getSelected();
            final View view = getView();

            if (selected == null || view.isFocusing()) {
                return;
            }

            final long duration = 3000L;

            // Move towards and zoom in on selected object
            view.moveToCenter(selected.getCenterX(), selected.getCenterY(), duration);
            view.setScale(view.getMaximumScale(), duration);
        }
    };

    private KeyEventHandler mViewToggle = new KeyEventHandler()
    {
        @Override
        public void handle(KeyEvent event)
        {
            final View view = getView();

            // Disable focusing if currently focusing
            if (view.isFocusing()) {
                view.setFocus(null);



            } else {
                final GObject selected = getSelected();

                // Can only focus if something selected
                if (selected != null) {
                    view.setFocus(selected);
                }
            }
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

        // Create game object for user control
        final GObject player = createPlayer();

        // Zoom view in to fill room and center on player
        final View view = getView();
        view.setFocus(player);
        view.setRoomConstrained(true);
        view.setScale(1f);

        // Enable motion by 1 meter per second by moving cursor by the edges
        view.setEdgeMotionEnabled(true);
        view.setSpeed(0.1f);

        // Generate clouds in sky
        createClouds();

        // Generate grass along ground
        createGrass();

        // Create ramp in left side
        createRamp();

        // Generate mountains in background
        createMountains();

        // Attach selected object motion to arrow keys
        final ControlMap input = getControlMap();
        input.attach(KeyEvent.Key.KEY_UP, mUpAction);
        input.setMode(KeyEvent.Key.KEY_UP, true, false);
        input.attach(KeyEvent.Key.KEY_RIGHT, mRightAction);
        input.setMode(KeyEvent.Key.KEY_RIGHT, true, true);
        input.attach(KeyEvent.Key.KEY_LEFT, mLeftAction);
        input.setMode(KeyEvent.Key.KEY_LEFT, true, true);

        // Center View on player while zooming in
        input.attach(KeyEvent.Key.KEY_SPACE, mLookAtPlayerHandler);
        input.setMode(KeyEvent.Key.KEY_SPACE, false, false);

        // Toggle free cam vs following selected GObject
        input.attach(KeyEvent.Key.KEY_LEFT_CTRL, mViewToggle);
        input.setMode(KeyEvent.Key.KEY_LEFT_CTRL, false, false);

        // Allow game shutdown from ESC key
        input.attach(KeyEvent.Key.KEY_ESCAPE, mCloseAction);

        // Zoom in and out
        input.attach(MouseEvent.Button.MIDDLE, mZoom);
        input.setMode(MouseEvent.Button.MIDDLE, false, true);

        // Allow selected object to fire projectile
        input.attach(MouseEvent.Button.RIGHT, mFireHandler);
        input.setMode(MouseEvent.Button.RIGHT, true, false);
    }

    private GObject createProjectile()
    {
        final GObject bullet = getGObjectFactory().get();
        final ImageComponent image = getImageFactory().get();
        final BodyComponent body = getBodyFactory().get(PROJECTILE_MASS);
        bullet.setBodyComponent(body);
        bullet.setImageComponent(image);

        // Set projectile texture
        final Texture texture = getShaderFactory().getTexture("demo_projectile.png");
        image.setTexture(texture.getId());

        // Randomize color to identify individual bullets
        final Random ranGen = new Random(System.nanoTime());
        image.setTint(ranGen.nextFloat() + 0.7f, ranGen.nextFloat() + 0.7f, ranGen.nextFloat() + 0.7f);

        // Assemble bullet
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
     */
    private GObject createPlayer()
    {
        // Get game object to act as player
        final GObject obj = getGObjectFactory().get();
        final BodyComponent body = getBodyFactory().get(PLAYER_MASS);
        final ImageComponent image = getImageFactory().get();
        obj.setBodyComponent(body);
        obj.setImageComponent(image);

        obj.setWidth(1.5f);
        obj.setHeight(2f);
        this.setSelected(obj);

        // Setup visual
        final int texture = getShaderFactory().getTexture("demo_character.png").getId();
        image.setTexture(texture);
        image.setTint(0.8f, 1f, 1f);
        image.setOffsets(0f, -0.25f);

        // Setup collision and physics
        body.setCollidable(true);
        body.setRestitution(PLAYER_RESTITUTION);
        body.setFriction(PLAYER_FRICTION);

        obj.moveToCenter((getRoom().getWidth() / 2f) + 5f, getRoom().getHeight() - 30f);
        obj.moveBy(0f, 0f, PLAYER_Z);

        return obj;
    }

    /**
     * <p>Creates cloud game objects and scatters them about the room.</p>
     */
    private void createClouds()
    {
        // Get cloud texture
        final Texture cloudTexture = getShaderFactory().getTexture("demo_cloud.png");
        final int texture = cloudTexture.getId();
        final float heightRatio = (float) cloudTexture.getHeight() / cloudTexture.getWidth();

        // Area placement limits
        final Room room = getRoom();
        final int maxX = (int) room.getWidth();
        final int minY = (int) (room.getHeight() * (1f / 6f));
        final int rangeY = (int) (room.getHeight() * (5f / 6f));

        // Generate clouds
        for (int i = 0; i < CLOUD_COUNT; i++) {
            final GObject cloud = getGObjectFactory().get();
            final ImageComponent image = getImageFactory().get();
            cloud.setImageComponent(image);

            // Set cloud image
            image.setTexture(texture);
            image.setFlipHorizontally(mRanGen.nextBoolean());
            image.setFlipVertically(mRanGen.nextBoolean());

            // Apply dimensions
            final float width = ((CLOUD_MAX_WIDTH - CLOUD_MIN_WIDTH) * mRanGen.nextFloat()) + CLOUD_MIN_WIDTH;
            cloud.setWidth(width);
            cloud.setHeight(cloud.getWidth() * heightRatio);

            // Place somewhere in the room's top half
            cloud.moveToCenter(mRanGen.nextInt(maxX), mRanGen.nextInt(rangeY) + minY);
        }
    }

    /**
     * <p>Creates an untextured black ramp in the left side of the {@link Room}.</p>
     */
    private void createRamp()
    {
        final GObject ramp = getGObjectFactory().get();
        final BodyComponent body = getBodyFactory().get(WALL_MASS);
        final ImageComponent image = getImageFactory().get();
        ramp.setBodyComponent(body);
        ramp.setImageComponent(image);

        // Make ramp black
        image.setTexture(Texture.NULL);
        image.setTint(0f, 0f, 0f);
        image.setTransparency(0.5f);
        image.setOffsets(0f, 0.2f);

        // Make ramp like walls (solid but not selectable)
        body.setSelectable(false);
        body.setCollidable(true);
        body.setFriction(WALL_FRICTION);
        body.setRestitution(WALL_RESTITUTION);

        // Size
        ramp.setWidth(30f);
        ramp.setHeight(10f);

        // Place angled in bottom left corner
        ramp.moveTo(-10f, -7f, -Float.MAX_VALUE);
        ramp.rotateTo(-Math.PI / 8d);
    }

    /**
     * <p>Creates static non-collidable grass in front of mountains and around the player.</p>
     */
    private void createGrass()
    {
        // Get texture and height ratio for sizing
        final Texture grassTexture = getShaderFactory().getTexture("demo_grass.png");
        final int texture = grassTexture.getId();
        final float heightRatio = (float) grassTexture.getHeight() / grassTexture.getWidth();

        // Max x possible for placement
        final int maxX = (int) getRoom().getWidth();

        for (int i = 0; i < GRASS_COUNT; i++) {
            // Randomize size while keeping texture's size ratio then randomize location on ground
            final GObject object = getGObjectFactory().get("char");
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
     */
    private void createMountains()
    {
        final Texture mountainTexture = getShaderFactory().getTexture("demo_mountain.png");
        final int texture = mountainTexture.getId();
        final float heightRatio = (float) mountainTexture.getHeight() / mountainTexture.getWidth();

        final int maxX = (int) getRoom().getWidth();

        for (int i = 0; i < MOUNTAIN_COUNT; i++) {
            final GObject object = getGObjectFactory().get("char");
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

    @Override
    protected void onUpdate()
    {
        final View view = getView();
        final GObject selected = getSelected();
        if (selected != null) {
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
