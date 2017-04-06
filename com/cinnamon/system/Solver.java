package com.cinnamon.system;

import com.cinnamon.object.*;
import com.cinnamon.utils.Point2F;
import com.cinnamon.utils.Vector2F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     Responsible for simulating physics of {@link BodyComponent}s and resolving their collisions.
 * </p>
 *
 * <p>
 *     Gravity can be set with {@link #setGlobalAcceleration(Vector2F)}. The default value is 9.8 meters per second
 *     per second.
 * </p>
 *
 * <p>
 *     The impulse, friction, and penetration adjustment computations were learned from Randy Paul's blog:
 *     http://www.randygaul.net/.
 * </p>
 */
public final class Solver
{
    // Maximum speed cap for all bodies (in meters per second)
    private static final float MAX_SPEED = 100f;

    // Minimum speed until body is flagged as sleeping
    private static final float SLEEP_SPEED_THRESHOLD = 1f;

    /**
     * BodyComponent lists
     */

    // BodyFactory providing BodyComponents to examine
    private final BodyFactory mFactory;

    /**
     * Physics optimizations
     */

    // Index = id of body owning state
    private SleepState[] mStates = new SleepState[500];

    // Velocity to add to each body for each update
    private final Vector2F mGlobalAcceleration = new Vector2F(0f, -9.8f);

    // Notifies when the global acceleration has been changed since last update
    private boolean mGlobalAccelChanged = false;

    // Impulses created by collisions
    private Vector2F[] mImpulses = new Vector2F[500];

    // Game's tickrate
    private final float mTickrate;

    /**
     * Collision data
     */

    // BoundingTree for bodies that can move
    private final BoundingTree mDynaTree = new BoundingTree();

    // BoundingTree for bodies that don't move
    private final BoundingTree mStatTree = new BoundingTree();

    // List of bounding box collisions
    private final List<BodyComponent> mCollisions = new ArrayList<BodyComponent>();

    // Manifold stores collision data: contact points and contact normal
    private final BodyComponent.Manifold mManifold = new BodyComponent.Manifold();

    /**
     * reactToCollision()'s vector objects; target body is the body being examined
     */

    // Collision normal for computing collision reactions for target body
    private final Vector2F mCollisionNormal0 = new Vector2F();

    // Collision normal for computing collision reactions for other body
    private final Vector2F mCollisionNormal1 = new Vector2F();

    /**
     * computeImpulse()'s vector objects for avoiding repeatedly instantiating
     */

    // Target body's velocity vector
    private final Vector2F mImpVelocity0 = new Vector2F();

    // Opposing body's velocity vector
    private final Vector2F mImpVelocity1 = new Vector2F();

    /**
     * applyFriction()'s vector objects for avoiding repeatedly instantiating
     */

    private final Vector2F mFricNormal = new Vector2F();

    private final Vector2F mFricVelocity0 = new Vector2F();

    private final Vector2F mFricVelocity1 = new Vector2F();

    /**
     * move()'s vector objects for avoiding repeatedly instantiating
     */

    private final Vector2F mMoveVelocity = new Vector2F();

    private final Vector2F mMoveAcceleration = new Vector2F();

    private final Vector2F mMovePrevAcceleration = new Vector2F();

    private final Vector2F mMoveImpulse = new Vector2F();

    /**
     * <p>Constructs a physics simulation solver.</p>
     *
     * @param factory {@link BodyComponent} lookup.
     * @param tickrate number of updates per second.
     */
    Solver(BodyFactory factory, int tickrate)
    {
        mFactory = factory;
        mTickrate = 1f / tickrate;
    }

    /**
     * <p>Gets the acceleration affecting all {@link BodyComponent}s in each update.</p>
     *
     * @return acceleration.
     */
    public Vector2F getGlobalAcceleration()
    {
        return new Vector2F(mGlobalAcceleration);
    }

    /**
     * <p>Sets an acceleration to affect all {@link BodyComponent}s in each update.</p>
     *
     * @param acceleration acceleration.
     */
    public void setGlobalAcceleration(Vector2F acceleration)
    {
        mGlobalAcceleration.copy(acceleration);
        mGlobalAccelChanged = true;
    }

    /**
     * <p>Processes physics updates for all eligible {@link BodyComponent}s.</p>
     *
     * @param factory for synchronizing new positions with a body's {@link GObject}.
     */
    void update(GObjectFactory factory)
    {
        // Perform physics computations on each body
        for (int i = 0, processed = 0, sz = mFactory.size(); processed < sz; i++) {
            final BodyComponent body = mFactory.get(i);

            // Skip unused ids
            if (body == null) {
                continue;
            }

            // Remove orphaned bodies
            if (body.isOrphan()) {
                sz--;
                mFactory.remove(body.getId());
                mDynaTree.remove(body);
                mStatTree.remove(body);
                continue;
            }

            // Count body towards factory's size
            processed++;

            // Ensure bounding box position is tracked correctly
            updateBroadphase(body);

            // No need to apply physics for static bodies
            if (body.isStatic()) {
                continue;
            }

            // Skip physics update for sleeping bodies
            final SleepState state = getState(body);
            if (!mGlobalAccelChanged && isSleeping(body, state)) {
                continue;
            }

            // Remove sleeping flag
            wake(state);

            // Compute new position from acceleration and impulses
            move(body);

            // Only examine collisions if body's collidable
            if (body.isCollidable()) {
                // Resolve collisions against dynamic bodies
                updateNarrowphase(mDynaTree, body);

                // Resolve collisions against static bodies
                updateNarrowphase(mStatTree, body);
            }

            // Sync position with GObject (and therefore other position dependent Components)
            final GObject obj = factory.get(body.getGObjectId(), body.getGObjectVersion());
            obj.moveTo(body.getX(), body.getY());
        }

        // End acceleration changed notification
        mGlobalAccelChanged = false;
    }

    /**
     * <p>Checks a {@link BodyComponent} against other nearby bodies for collisions and applies an impulse between
     * them for separation.</p>
     *
     * @param tree {@link BoundingTree} to query for collisions.
     * @param body BodyComponent to test for.
     */
    private void updateNarrowphase(BoundingTree tree, BodyComponent body)
    {
        // Get a list of possible collisions (bounding box collisions)
        mCollisions.clear();
        tree.getCollisions(mCollisions, body);

        for (int i = 0, sz = mCollisions.size(); i < sz; i++) {
            final BodyComponent otherBody = mCollisions.get(i);

            // Test and record collision info to Manifold but skip processing if no collision
            if (!body.collidesWith(otherBody, mManifold)) {
                continue;
            }

            // Separation impulses aren't needed if already heading away from other body
            if (isMovingTowards(body, otherBody)) {
                // Apply an impulse to try and separate the collision only if not already separating
                reactToCollision(body, otherBody);
            }

            // If body's velocity + impulses are low enough in magnitude, sleep
            if (shouldSleep(body)) {
                // Put body to sleep and record resting pair
                sleep(body, otherBody);
            }

            mManifold.getCollisionNormal(mCollisionNormal0);
            mCollisionNormal0.negate();

            // Directly correct positions based on penetration depth
            adjustForSeparation(body, otherBody, mCollisionNormal0);
        }
    }

    /**
     * <p>Checks if the {@link BodyComponent} should be flagged as sleeping. That is, the body must be collidable
     * and have a speed < 1 meter per second.</p>
     *
     * @param body body.
     * @return true if should be sleeping.
     */
    private boolean shouldSleep(BodyComponent body)
    {
        assert (body.isCollidable());

        return body.getSpeed() < SLEEP_SPEED_THRESHOLD;
    }

    /**
     * <p>Gets the {@link SleepState} used to record information about the moment a {@link BodyComponent} was put to
     * sleep.</p>
     *
     * @param body BodyComponent.
     * @return SleepState.
     */
    private SleepState getState(BodyComponent body)
    {
        SleepState state = mStates[body.getId()];

        // Instantiate new SleepState on first time
        if (state == null) {
            state = new SleepState();
            mStates[body.getId()] = state;
        }

        return state;
    }

    /**
     * <p>Checks if the body is sleeping. If the body has been marked as sleeping, whether or not there are impulses
     * waiting to be applied becomes the deciding factor. Any impulse, whether created by the Solver or added from
     * outside the Solver through {@link BodyComponent#addImpulse(Vector2F)}, will cause this method to return true.</p>
     *
     * @param body body.
     * @return true if body is sleeping.
     */
    private boolean isSleeping(BodyComponent body, SleepState state)
    {
        return state.mSleeping && !hasImpulse(body);
    }

    /**
     * <p>Marks the body as sleeping. </p>
     *
     * <p>If a body is asleep, its physics update is skipped unless an impulse is applied.</p>
     *
     * @param body body.
     */
    private void sleep(BodyComponent body, BodyComponent surface)
    {
        // Remove velocity and acceleration
        body.setVelocity(null);
        body.setAcceleration(null);
        body.setImpulse(null);

        // Remove system generated impulse
        clearImpulse(body);

        // Update sleep state
        final SleepState state = mStates[body.getId()];
        state.mSurface = surface;
        state.mX = body.getX();
        state.mY = body.getY();
        state.mSleeping = true;
    }

    /**
     * <p>Checks if a body has an impulse waiting to be applied. This method will check for both system created
     * impulses and those added from outside through {@link BodyComponent#addImpulse(Vector2F)}.</p>
     *
     * @param body body.
     * @return true if the body has a stored impulse.
     */
    private boolean hasImpulse(BodyComponent body)
    {
        // Check if impulse was provided from outside simulation
        if (!body.getImpulse().isZero()) {
            return true;
        }

        // Check if simulation generated an impulse
        final Vector2F impulse = mImpulses[body.getId()];
        return impulse != null && !impulse.isZero();
    }

    /**
     * <p>Removes the sleeping flag on a {@link BodyComponent}, permitting its physics updates.</p>
     *
     * @param state the body's {@link SleepState}.
     */
    private void wake(SleepState state)
    {
        state.mSurface = null;
        state.mX = -Float.MAX_VALUE;
        state.mY = -Float.MAX_VALUE;
        state.mSleeping = false;
    }

    /**
     * <p>Gets the system generated impulse for a body.</p>
     *
     * @param body body.
     * @param container vector to hold impulse values.
     */
    private void getImpulse(BodyComponent body, Vector2F container)
    {
        final Vector2F impulse = mImpulses[body.getId()];
        if (impulse == null) {
            return;
        }

        container.copy(impulse);
    }

    private void addImpulse(BodyComponent body, Vector2F impulse)
    {
        final Vector2F systemImpulse = mImpulses[body.getId()];

        // Instantiate new vector for impulse if none before
        if (systemImpulse == null) {
            mImpulses[body.getId()] = new Vector2F(impulse);
        } else {
            // Vector already exists so just add
            systemImpulse.add(impulse);
        }
    }

    private void clearImpulse(BodyComponent body)
    {
        final Vector2F impulse = mImpulses[body.getId()];
        if (impulse == null) {
            return;
        }

        impulse.set(0f, 0f);
    }

    private void reactToCollision(BodyComponent body, BodyComponent other)
    {
        mManifold.getCollisionNormal(mCollisionNormal0);

        // Scale impulse direction to strength while considering both body's masses
        final float impulse = Math.abs(computeImpulse(mCollisionNormal0, body, other));
        mCollisionNormal0.negate();
        mCollisionNormal0.multiply(impulse);
        mCollisionNormal0.divide((1f / body.getMass()) + (1f / other.getMass()));

        // Get collision normal for other body
        mCollisionNormal1.copy(mCollisionNormal0);
        mCollisionNormal1.negate();

        // Scale impulse to body's mass and apply
        mCollisionNormal0.divide(body.getMass());
        addImpulse(body, mCollisionNormal0);

        // Compute and apply friction for target body and apply
        applyFriction(mCollisionNormal0, body, other);

        // Shouldn't move static bodies
        if (other.isStatic()) {
            return;
        }

        // Scale other body's impulse according to its mass and apply
        mCollisionNormal1.divide(other.getMass());
        addImpulse(other, mCollisionNormal1);

        // Compute and apply friction based on the impulse
        applyFriction(mCollisionNormal1, other, body);
    }

    private boolean isMovingTowards(BodyComponent body, BodyComponent reference)
    {
        body.getVelocity(mCollisionNormal0);
        reference.getVelocity(mCollisionNormal1);
        mCollisionNormal0.subtract(mCollisionNormal1);

        mFricNormal.set(body.getCenterX(), body.getCenterY());
        mFricNormal.subtract(reference.getCenterX(), reference.getCenterY());

        final float dot = mCollisionNormal0.dotProduct(mFricNormal);
        return dot < 0f || Point2F.isEqual(dot, 0f);
    }

    /**
     * <p>Computes the strength of the impulse in meters per second to use to separate two {@link BodyComponent}s.</p>
     *
     * @param normal collision normal facing from body to other.
     * @param body body.
     * @param other other body.
     * @return impulse strength.
     */
    private float computeImpulse(Vector2F normal, BodyComponent body, BodyComponent other)
    {
        final float restitution = Math.min(body.getRestitution(), other.getRestitution());

        // Compute velocity with restitution
        body.getVelocity(mImpVelocity0);
        mImpVelocity0.multiply(restitution);

        // Compute other body's velocity with restitution
        other.getVelocity(mImpVelocity1);
        mImpVelocity1.multiply(restitution);


        // Get difference in velocity
        mImpVelocity0.subtract(mImpVelocity1);

        // Scale impulse direction by velocity difference
        final float projection = mImpVelocity0.dotProduct(normal);

        return -(1f + restitution) * projection;
    }

    private void applyFriction(Vector2F impulse, BodyComponent body, BodyComponent other)
    {
        // Revert impulse to -(collision normal)
        mFricNormal.copy(impulse);
        mFricNormal.normalize();

        // Get both bodies' velocities
        body.getVelocity(mImpVelocity0);
        other.getVelocity(mImpVelocity1);

        // Compute vector perpendicular to collision normal
        mImpVelocity1.subtract(mImpVelocity0);
        mFricNormal.multiply(mImpVelocity1.dotProduct(mFricNormal));
        mImpVelocity1.subtract(mFricNormal);
        mImpVelocity1.normalize();

        // Compute perpendicular vector's magnitude (friction strength)
        body.getVelocity(mFricVelocity0);
        other.getVelocity(mFricVelocity1);
        mFricVelocity0.subtract(mFricVelocity1);

        // Compute impulse strength for friction
        final float cor = Math.min(body.getRestitution(), other.getRestitution());
        float impStrength = (mFricVelocity0.dotProduct(mImpVelocity1)) * (-(1 + cor));
        impStrength /= (1f / body.getMass()) + (1f / other.getMass());
        impStrength /= body.getMass();

        // Apply impulse along friction direction
        mImpVelocity1.multiply(impStrength);

        // Compute max friction according to coloumb friction
        final float coloumbMax = impulse.magnitude() * body.getFriction();

        // Limit friction
        if (mImpVelocity1.magnitude() > coloumbMax) {
            mImpVelocity1.normalize();
            mImpVelocity1.multiply(coloumbMax);
        }

        // Compute impulse left after friction
        getImpulse(body, mImpVelocity0);
        mImpVelocity0.add(mImpVelocity1);

        addImpulse(body, mImpVelocity1);
    }

    private void adjustForSeparation(BodyComponent body, BodyComponent other, Vector2F normal)
    {
        final float depth = Math.max(mManifold.getPenetrationDepthAt(0), mManifold.getPenetrationDepthAt(1));

        // Don't adjust if penetration is too little
        if (depth < 0.01f) {
            return;
        }

        // Compute separation
        final float shift = (depth / ((1f / other.getMass()) + (1f / body.getMass()))) * 0.6f;

        // Stretch normal
        normal.multiply(shift * (1f / body.getMass()));

        // Shift body back
        body.moveBy(normal.getX(), normal.getY());
        final SleepState bodyState = mStates[body.getId()];
        bodyState.mX = body.getX();
        bodyState.mY = body.getY();

        // Shouldn't move static bodies
        if (other.isStatic()) {
            return;
        }

        // Create normal for other body
        mCollisionNormal1.copy(normal);
        mCollisionNormal1.negate();
        mCollisionNormal1.multiply(shift * (1f / other.getMass()));

        // Shift other body in opposite direction
        other.moveBy(mCollisionNormal1.getX(), mCollisionNormal1.getY());
        final SleepState otherState = mStates[other.getId()];
        otherState.mX = other.getX();
        otherState.mY = other.getY();
    }

    /**
     * <p>Updates a {@link BodyComponent}'s bounding box position in the {@link BoundingTree} and ensures static
     * bodies are separated from non-static. This allows an optimization for the narrowphase where static
     * bodies do not need a physics update as static body motion is prohibited.</p>
     *
     * @param body body.
     */
    private void updateBroadphase(BodyComponent body)
    {
        // Determine which tree the body belongs to (static bodies belong to the static tree)
        final BoundingTree prefTree;
        final BoundingTree otherTree;
        if (body.isStatic()) {
            prefTree = mStatTree;
            otherTree = mDynaTree;
        } else {
            prefTree = mDynaTree;
            otherTree = mStatTree;
        }

        // Try to add to preferred tree (and remove from other if add succeeded)
        if (prefTree.add(body)) {
            otherTree.remove(body);
        } else {
            // Failed adding means already in tree so just update position
            prefTree.update(body);
        }
    }

    /**
     * <p>Moves the BodyComponent according to set physics properties.</p>
     */
    private void move(BodyComponent body)
    {
        body.getVelocity(mMoveVelocity);

        // Add impulse given from outside the simulation
        body.getImpulse(mMoveImpulse);
        if (!mMoveImpulse.isZero()) {

            // Apply impulse to velocity then erase for next update
            mMoveVelocity.add(mMoveImpulse);
            body.setImpulse(null);
        }

        // Apply system created impulse
        getImpulse(body, mMoveImpulse);
        if (!mMoveImpulse.isZero()) {
            mMoveVelocity.add(mMoveImpulse);
            clearImpulse(body);
        }

        // Cap velocity to global max speed
        if (mMoveVelocity.magnitude() > MAX_SPEED) {
            mMoveVelocity.normalize();
            mMoveVelocity.multiply(MAX_SPEED);
        }

        // Save changes from impulses and global max
        body.setVelocity(mMoveVelocity);

        // Scale current velocity according to timestep
        mMoveVelocity.multiply(mTickrate);

        // Scale previous acceleration according to timestep then add to velocity
        body.getPreviousAcceleration(mMovePrevAcceleration);
        mMovePrevAcceleration.multiply((mTickrate * mTickrate) / 2f);
        mMoveVelocity.add(mMovePrevAcceleration);

        // Move to new position
        body.moveBy(mMoveVelocity.getX(), mMoveVelocity.getY());

        // Compute next update's velocity
        body.getAcceleration(mMoveAcceleration);
        mMoveAcceleration.add(mGlobalAcceleration);
        mMoveAcceleration.add(mMovePrevAcceleration);
        mMoveAcceleration.multiply(2f * mTickrate);

        // Save new velocity
        body.getVelocity(mMoveVelocity);
        mMoveVelocity.add(mMoveAcceleration);
        body.setVelocity(mMoveVelocity);
    }

    /**
     * <p>Gets a {@link List} of {@link BodyComponent}s whose bounding boxes collided with a given body's bounding
     * box.</p>
     *
     * @param body BodyComponent.
     * @return list of bounding box collisions.
     */
    public List<BodyComponent> getBoundingBoxCollisions(BodyComponent body)
    {
        // Get bounding box collisions with dynamic bodies
        mCollisions.clear();
        mDynaTree.getCollisions(mCollisions, body);

        // Get bounding box collisions with static bodies
        final List<BodyComponent> staticBodies = new ArrayList<BodyComponent>();
        mStatTree.getCollisions(staticBodies, body);
        staticBodies.addAll(mCollisions);

        mCollisions.clear();
        return staticBodies;
    }

    /**
     * <p>
     *     Holds information about the moment a {@link BodyComponent} enters the sleep state such as the body it
     *     collided with and its position during the event.
     * </p>
     */
    private class SleepState
    {
        private BodyComponent mSurface;
        private float mX;
        private float mY;
        private boolean mSleeping;
    }
}
