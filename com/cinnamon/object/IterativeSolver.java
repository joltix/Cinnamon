package com.cinnamon.object;

import com.cinnamon.gfx.ImageComponent;
import com.cinnamon.utils.OnRemoveListener;
import com.cinnamon.utils.Shape;
import com.cinnamon.utils.Vector2F;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     <b>License note: </b>IterativeSolver uses modified source code from Erin Catto's Box2D. Specifically, the
 *     impulse computations for separating speed and bias, friction, as well as contact warmstarting are
 *     originally from the Box2D open source project. Due to this, Box2D's license has been posted following
 *     Cinnamon's license below.
 * </p>
 * <br>
 * <p>
 *     Copyright (c) 2017 Christian Ramos
 * </p>
 * <p>
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 * </p>
 * <p>
 *     The above copyright notice and this permission notice shall be included in all
 *     copies or substantial portions of the Software.
 * </p>
 * <p>
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *     SOFTWARE.
 * </p>
 * <br>
 * <p>
 *     Copyright (c) 2006-2013 Erin Catto http://www.gphysics.com
 * </p>
 * <p>
 *     This software is provided 'as-is', without any express or implied
 *     warranty.  In no event will the authors be held liable for any damages
 *     arising from the use of this software.
 * </p>
 * <p>
 *     Permission is granted to anyone to use this software for any purpose,
 *     including commercial applications, and to alter it and redistribute it
 *     freely, subject to the following restrictions:
 * </p>
 * <p>
 *     1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 *     <br>
 *     2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 *     <br>
 *     3. This notice may not be removed or altered from any source distribution.
 * </p>
 * <br>
 * <br>
 * <p>
 *     IterativeSolver uses sequential impulses from Erin Catto's Box2D.
 * </p>
 * <p>
 *     Iteratively tries to separate colliding {@link BodyComponent} in a single frame update. The number of
 *     iterations affects the accuracy of separation with higher accuracy as the number of iterations grows. More
 *     iterations are required for stacking bodies when the bodies on top are collectively higher in mass than the
 *     supporting body at the bottom.
 * </p>
 * <p>
 *     A {@link ContactGraph} is used to keep track of {@link ContactGraph.Contact}s between bodies as well as
 *     allowing groups of bodies to be put to sleep as an optimization to prevent unnecessary computations. However,
 *     <i>when</i> a Contact exists between two bodies is left up to the IterativeSolver as the graph merely stores them.
 * </p>
 * <p>
 *     One update from the IterativeSolver consists of four main steps: update spacial partitioning, detect
 *     collisions and Contacts, generate impulses for Contact separation, and integration.
 * </p>
 * <h4>Spacial partitioning</h4>
 * <p>
 *     Each body is checked in with a {@link BoundingTree} to make sure it's properly ordered in a spacial hierarchy
 *     in order to limit the number of bodies that must be checked during collision detection in the next step.
 * </p>
 * <h4>Collision and Contact detection</h4>
 * <p>
 *     Each body is queried against a {@link BoundingTree} to obtain a list of bodies whose bounding boxes overlap.
 *     Fine grain collision detection is then performed between the examined body and each in the list, either
 *     creating a Contact, removing one, or updating prior.
 * </p>
 * <h4>Impulses</h4>
 * <p>
 *     Separating and friction impulses are computed and applied to both bodies for each Contact to undo the majority
 *     of penetration from the collision as well as simulate friction along the colliding surface.
 * </p>
 * <h4>Move</h4>
 * <p>
 *     All bodies' positions are updated according to the velocities computed due to the impulses in the previous step.
 * </p>
 */
public final class IterativeSolver extends Solver
{
    /**
     * Computation constants
     */

    // Minimum speed at a collision before body's restitution is treated as 0 (no bounce)
    private static final float MIN_COLLISION_SPEED = 1f;

    // Coefficient for baumgarte stabilization when computing separation speed
    private static final float BAUMGARTE = 0.2f;

    // Leeway for penetration when computing baumgarte
    private static final float PENETRATION_SLOP = 0.01f;

    // Factor to scale down separating speed
    private static final float SEPARATION_DAMPING = 0.8f;

    // Factor to scale down friction
    private static final float FRICTION_DAMPING = 0.9f;

    // Maximum speed required for sleep eligibility
    private static final float MAX_SLEEP_THRESHOLD = 0.1f;

    // Maximum speed difference between current frame and previous for sleep eligibility
    private static final float MAX_DIFF_SLEEP_SPEED = 0.2f;

    /**
     * Colors for tinting bodies when colorize sleep is enabled
     */

    // RGB for sleeping bodies
    private static final float SLEEP_R = 0f;
    private static final float SLEEP_G = 1f;
    private static final float SLEEP_B = 0f;

    // RGB for awake bodies
    private static final float WAKE_R = 1f;
    private static final float WAKE_G = 0f;
    private static final float WAKE_B = 0f;

    // RGB for static bodies
    private static final float STATIC_R = 0f;
    private static final float STATIC_G = 0f;
    private static final float STATIC_B = 1f;

    /**
     * Run configuration
     */

    // Number of times to apply impulses for all active Contacts per update
    private final int mIterations;

    // Gravity, defaults to Earth strength 9.8 m/s^2
    private final Vector2F mGravity = new Vector2F(0f, -9.8f);

    // True to allow placing bodies to sleep
    private static final boolean mSleepOptim = true;

    // True to tint bodies according to sleep status (green = asleep, red = awake, blue = static)
    private static final boolean mColorizeSleep = true;

    /**
     * Vectors for computing impulses when solving Contacts
     */

    // Vector for collision normal
    private final Vector2F mCollisionNormal = new Vector2F();

    // Vector for a BodyComponent's velocity
    private final Vector2F mVelocityA = new Vector2F();

    // Vector for another BodyComponent's velocity
    private final Vector2F mVelocityB = new Vector2F();

    // Vector for relative velocity (velocity A - velocity B)
    private final Vector2F mVelocityR = new Vector2F();

    // Vector for representing an impulse during integration
    private final Vector2F mImpulse = new Vector2F();

    // Vector for friction impulse
    private final Vector2F mFriction = new Vector2F();

    /**
     * Spacial partitioning trees and list for querying them
     */

    // Spatial partitioning tree for non static bodies
    private final BoundingTree mDynamicTree = new BoundingTree();

    // Spatial partitioning tree for static bodies
    private final BoundingTree mStaticTree = new BoundingTree();

    // Used for querying BoundingTrees for bounding box collisions
    private final List<BodyComponent> mCollisions = new ArrayList<BodyComponent>();

    // Stores collision data during collision detection
    private final BodyComponent.Manifold mManifold = new BodyComponent.Manifold();

    /**
     * Contacts retaining info on collisions
     */

    // Tracks SIContacts between colliding bodies
    private final ContactGraph<SIContact> mGraph = new SIContactGraph();

    /**
     * <p>Constructs an IterativeSolver.</p>
     *
     * @param factory body lookup.
     * @param timestep timestep.
     * @param iterations number of times per update to solve collisions.
     * @throws IllegalArgumentException if iterations <= 0.
     */
    public IterativeSolver(BodyFactory factory, float timestep, int iterations)
    {
        super(timestep);

        // Ensure iterations makes sense
        if (iterations <= 0) {
            throw new IllegalArgumentException("Iterations must be > 0");
        }

        // Attach listener to sync removal with graph
        factory.addOnRemoveListener(new GraphRemoveSync());

        mIterations = iterations;
    }

    @Override
    public List<BodyComponent> getCollisions(BodyFactory factory, BodyComponent body)
    {
        // Make sure all bodies are properly ordered
        ensureSpacialPartitioning(factory);

        // Query each BoundingTree for bounding box collisions
        final List<BodyComponent> dynamicCollisions = new ArrayList<BodyComponent>();
        final List<BodyComponent> staticCollisions = new ArrayList<BodyComponent>();
        mDynamicTree.getCollisions(dynamicCollisions, body);
        mStaticTree.getCollisions(staticCollisions, body);

        // Test body against all other bodies and add collisions to output list
        final List<BodyComponent> actualCollisions = new ArrayList<BodyComponent>();
        addCollisions(actualCollisions, staticCollisions, body);
        addCollisions(actualCollisions, dynamicCollisions, body);

        return actualCollisions;
    }

    /**
     * <p>Performs fine-grain collision detection between the given body and all {@link BodyComponent}s in the given
     * list and adds all colliding to an output list.</p>
     *
     * @param bodies list of bodies.
     * @param out list of colliding bodies.
     * @param body body.
     */
    private void addCollisions(List<BodyComponent> bodies, List<BodyComponent> out, BodyComponent body)
    {
        // Add each colliding body to output list
        for (BodyComponent other : out) {

            // Perform finer grain collision detection between the Shapes
            if (body.collidesWith(other, mManifold)) {
                bodies.add(other);
            }
        }
    }

    @Override
    public Vector2F getGlobalAcceleration()
    {
        return new Vector2F(mGravity);
    }

    @Override
    public void getGlobalAcceleration(Vector2F container)
    {
        container.copy(mGravity);
    }

    @Override
    public void setGlobalAcceleration(Vector2F vector)
    {
        mGravity.copy(vector);
    }

    @Override
    public void update(GObjectFactory objectFactory, BodyFactory bodyFactory)
    {
        // Make sure all bodies are properly spatially partitioned
        ensureSpacialPartitioning(bodyFactory);

        // Detect collisions and SIContacts
        searchForContacts(objectFactory, bodyFactory);

        // Remove SIContacts from previous update whose bodies are now separated
        mGraph.removeInvalidContacts();

        // Apply impulses carried over from previous update
        warmstart();

        // Attempt to separate all SIContacts
        solve();

        // Move all bodies according to their velocities
        moveAll(objectFactory, bodyFactory);
    }

    /**
     * <p>"Warms" all active {@link SIContact}s by applying the accumulated separation and friction impulses from the
     * previous {@link Solver} update.</p>
     *
     * <p>Applying the previous update's impulses helps stabilize the {@link BodyComponent}s faster.</p>
     *
     * <p>This method follows the warmstarting scheme in Box2D.</p>
     */
    private void warmstart()
    {
        // Warmstart all active SIContacts
        for (SIContact contact : mGraph.getContacts()) {

            // Apply accumulated separation and friction impulses from previous update
            contact.warm(mVelocityA, mVelocityB, mFriction);
        }
    }

    /**
     * <p>Integrates all {@link BodyComponent}s' velocities and synchronizes their {@link GObject}s with the new
     * positions.</p>
     *
     * <p>This method also limits the speed of all bodies to {@link Solver#MAX_SPEED}.</p>
     *
     * @param objectFactory game object lookup.
     * @param bodyFactory body lookup.
     */
    private void moveAll(GObjectFactory objectFactory, BodyFactory bodyFactory)
    {
        // Move all bodies and synchronize positions with their GObjects
        for (int id = 0, i = 0, sz = bodyFactory.size(); i < sz; id++) {
            final BodyComponent body = bodyFactory.get(id);

            // Skip unused id
            if (body == null) {
                continue;
            } else {
                // Count active id towards expected body count
                i++;
            }

            // Tint body according to sleep status
            if (mSleepOptim) {
                debugColorizeSleep(objectFactory, body);
            }

            // Static and sleeping bodies shouldn't be moved
            if (!isDynamic(body)) {
                continue;
            }

            // Keep speed from getting too high
            limitSpeed(body);

            // Move body
            integrate(body);

            // Synchronize position with GObject (and therefore other Components)
            final GObject obj = objectFactory.get(body.getGObjectId());
            obj.moveToCenter(body.getCenterX(), body.getCenterY());
        }
    }

    private void debugColorizeSleep(GObjectFactory factory, BodyComponent body)
    {
        // Bail out if no coloring wanted
        if (!mColorizeSleep) {
            return;
        }

        // Get ImageComponent representing the body
        final ImageComponent img = factory.get(body.getGObjectId()).getImageComponent();

        // Color static
        if (body.isStatic()) {
            img.setTint(STATIC_R, STATIC_G, STATIC_B);

        } else if (body.isSleeping()) {
            // Color sleeping
            img.setTint(SLEEP_R, SLEEP_G, SLEEP_B);

        } else {
            // Color awake
            img.setTint(WAKE_R, WAKE_G, WAKE_B);
        }
    }

    /**
     * <p>Limits a {@link BodyComponent}'s maximum speed to {@link Solver#MAX_SPEED}.</p>
     *
     * @param body body.
     */
    private void limitSpeed(BodyComponent body)
    {
        // Cap speed to MAX_SPEED
        if (body.getSpeed() > Solver.MAX_SPEED) {
            body.getVelocity(mVelocityA);

            // Resize velocity's magnitude
            mVelocityA.normalize();
            mVelocityA.multiply(Solver.MAX_SPEED);

            // Apply
            body.setVelocity(mVelocityA);
        }
    }

    /**
     * <p>Integrates a {@link BodyComponent}'s velocities to move the body's position accordingly.</p>
     *
     * <p>This method uses a velocity verlet scheme.</p>
     *
     * @param body body to move.
     */
    private void integrate(BodyComponent body)
    {
        // Scale working copy of velocity to timestep
        body.getVelocity(mVelocityA);
        mVelocityA.multiply(getTimestep());

        // Get body's previous acceleration, scale with time, then add to working velocity
        body.getAcceleration(mVelocityB);
        mVelocityB.multiply(getTimestep() / 2f);
        mVelocityA.add(mVelocityB);

        // Move body according to scaled velocity
        body.moveBy(mVelocityA.getX(), mVelocityA.getY());

        // Compute velocity for next update (add gravity then previous accel)
        body.getAcceleration(mImpulse);
        mImpulse.add(mGravity);
        mImpulse.add(mVelocityB);
        mImpulse.multiply(getTimestep());

        // Save new velocity for next update
        body.getVelocity(mVelocityA);
        mVelocityA.add(mImpulse);
        body.updateVelocity(mVelocityA);
    }

    /**
     * <p>Attempts to solve all active {@link SIContact}s by applying separation and friction impulses iteratively,
     * terminating after {@link #mIterations}.</p>
     *
     * <p>This method follows solving velocity constraints in Box2D.</p>
     */
    private void solve()
    {
        // Sleep/wake groups of bodies if optimization enabled
        if (mSleepOptim) {
            optimizeWithSleep();
        }

        // Precompute velocity bias
        for (SIContact contact : mGraph.getContacts()) {

            // Precompute velocity bias
            contact.setSeparationBias(computeSeparationBias(contact));
        }

        // Apply friction then separation impulses iteratively
        for (int i = 0; i < mIterations; i++) {

            // Solve each Contact detected from the previous step)
            for (SIContact contact : mGraph.getContacts()) {

                // Don't try to solve sleeping SIContacts
                if (contact.isSleeping()) {
                    continue;
                }

                // Generate an impulse along collision normal for friction
                applyFrictionImpulse(contact);

                // Generate an impulse between both bodies for separation
                applySeparationImpulse(contact);
            }
        }
    }

    /**
     * <p>Groups {@link BodyComponent} in the {@link ContactGraph} and places groups to sleep if they're deemed slow
     * enough and their speed has not changed too much since the last frame update. Sleeping bodies skip physics
     * computations and do not move.</p>
     */
    private void optimizeWithSleep()
    {
        // Wake/sleep components
        for (int component = 0, sz = mGraph.getComponentCount(); component < sz; component++) {

            // Put component to sleep if should be sleeping
            if (mGraph.isSleepEligible(component)) {
                mGraph.sleep(component);

            } else {
                // Wake component if shouldn't be sleeping
                mGraph.wake(component);
            }
        }
    }

    /**
     * <p>Combines a {@link BodyComponent}'s impulse set from outside the {@link Solver} through
     * {@link BodyComponent#setImpulse(Vector2F)} or {@link BodyComponent#addImpulse(Vector2F)} with the body's
     * velocity.</p>
     *
     * <p>The impulse will be cleared and {@link BodyComponent#getImpulse()} will return a zero vector.</p>
     *
     * @param body body.
     */
    private void integrateExternalImpulse(BodyComponent body)
    {
        body.getImpulse(mImpulse);

        // Bail out if no impulse
        if (mImpulse.isZero()) {
            return;
        }

        // Clear externally set impulse
        body.setImpulse(null);

        // Sum and set new velocity
        body.getVelocity(mVelocityA);
        mVelocityA.add(mImpulse);
        body.setVelocity(mVelocityA);
    }

    /**
     * <p>Computes the separation bias term to use when computing a separation impulse for a given {@link SIContact}
     * .</p>
     *
     * <p>This bias term can be thought of as a target velocity that should be aimed for when computing a separation
     * impulse.</p>
     *
     * <p>This method mostly follows Box2D's constraint initialization.</p>
     *
     * @param contact contact.
     * @return bias.
     */
    private float computeSeparationBias(SIContact contact)
    {
        // Get collision normal
        final BodyComponent.Manifold manifold = contact.getManifold();
        manifold.getCollisionNormal(mCollisionNormal);
        mCollisionNormal.negate();

        // Compute relative speed along the collision normal
        final Vector2F relativeV = getRelativeVelocity(contact.getBodyA(), contact.getBodyB());
        final float relativeSpeed = mCollisionNormal.dotProduct(relativeV);

        // Determine target velocity to aim for after bounce
        final float bias;
        if (relativeSpeed < -MIN_COLLISION_SPEED) {

            // Compute baumgarte stabilizing term
            final float penetration = manifold.getPenetrationDepthAt(0);
            final float baumgarte = (BAUMGARTE * Math.max(penetration - PENETRATION_SLOP, 0f));

            // Compute fraction of separating speed by mixing restitutions
            final float cor = contact.getBodyA().getRestitution() * contact.getBodyB().getRestitution();
            final float restitutionSpeed = relativeSpeed * -cor;

            bias = (restitutionSpeed + baumgarte) * SEPARATION_DAMPING;

        } else {
            // Shouldn't bounce anymore when slow enough
            bias = 0f;
        }

        return bias;
    }

    /**
     * <p>Applies an impulse along a vector perpendicular to the collision normal, in the opposite direction of the
     * velocity along the perpendicular in order to simulate friction.</p>
     *
     * <p>This method follows Box2D's tangent impulse computations.</p>
     *
     * @param contact SIContact.
     */
    private void applyFrictionImpulse(SIContact contact)
    {
        // Unwrap Contact
        final BodyComponent bodyA = contact.getBodyA();
        final BodyComponent bodyB = contact.getBodyB();
        final BodyComponent.Manifold manifold = contact.getManifold();

        // Get vector perpendicular to collision normal for friction's direction
        manifold.getCollisionNormal(mFriction);
        mFriction.normal(false);

        // Compute initial friction magnitude along friction's direction
        final Vector2F relativeVelocity = getRelativeVelocity(bodyA, bodyB);
        float friction = -(relativeVelocity.dotProduct(mFriction)) * contact.getInverseSystemMass() * FRICTION_DAMPING;

        // Accumulate friction impulse for warmstarting while clamping to Coloumb friction
        friction = accumulateFriction(contact, friction, bodyA.getFriction() * bodyB.getFriction());

        // Compute friction impulse distributed over mass then apply to body A
        mFriction.multiply(friction * bodyA.getInverseMass());
        addImpulse(bodyA, mVelocityA, mFriction);

        // Only movable bodies should receive a friction impulse
        if (!bodyB.isStatic()) {

            // Get friction's direction for body B
            manifold.getCollisionNormal(mFriction);
            mFriction.normal(true);

            // Scale friction down by body B's mass then apply to body B
            mFriction.multiply(friction * bodyB.getInverseMass());
            addImpulse(bodyB, mVelocityB, mFriction);
        }
    }

    /**
     * <p>Creates and imparts an impulse on both {@link BodyComponent}s in opposite directions according to a
     * collision normal.</p>
     *
     * <p>This method follows Box2D's separating impulse computations.</p>
     *
     * @param contact SIContact to separate.
     */
    private void applySeparationImpulse(SIContact contact)
    {
        // Unwrap Contact details
        final BodyComponent.Manifold manifold = contact.getManifold();
        final BodyComponent bodyA = contact.getBodyA();
        final BodyComponent bodyB = contact.getBodyB();

        // Static bodies aren't checked for collisions (only against) so body A is never static
        assert (!bodyA.isStatic());

        // Compute relative velocity in regards to body A
        final Vector2F relativeV = getRelativeVelocity(bodyA, bodyB);

        // Get collision normal
        manifold.getCollisionNormal(mCollisionNormal);
        mCollisionNormal.negate();

        // Compute a portion of the impulse needed to separate the bodies
        final float totalInverseMass = contact.getInverseSystemMass();
        final float separatingSpeed = relativeV.dotProduct(mCollisionNormal);
        final float bias = contact.getBias();
        float impulse = -totalInverseMass * (separatingSpeed - bias);

        // Add impulse to accumulation and use impulse computed from accumulation
        impulse = accumulateSeparation(contact, impulse);

        // Scale down impulse for body A according to its mass
        mCollisionNormal.multiply(impulse * bodyA.getInverseMass());

        // Apply impulse to body A
        addImpulse(bodyA, mVelocityA, mCollisionNormal);

        // Only apply an impulse to body B if it's movable
        if (!bodyB.isStatic()) {

            // Distribute impulse over body B's mass
            manifold.getCollisionNormal(mCollisionNormal);
            mCollisionNormal.multiply(impulse * bodyB.getInverseMass());

            // Apply impulse to body B
            addImpulse(bodyB, mVelocityB, mCollisionNormal);
        }
    }

    /**
     * <p>Computes the relative velocity of a {@link BodyComponent} such that <i>body.velocity - other.velocity.</i></p>
     *
     * @param body body.
     * @param other other body.
     * @return relative velocity.
     */
    private Vector2F getRelativeVelocity(BodyComponent body, BodyComponent other)
    {
        body.getVelocity(mVelocityR);
        other.getVelocity(mVelocityA);
        mVelocityR.subtract(mVelocityA);
        return mVelocityR;
    }

    /**
     * <p>Adds an impulse to a {@link BodyComponent}.
     *
     * <p>Unlike {@link BodyComponent#addImpulse(Vector2F)}, this method
     * immediately adds the impulse to the body's velocity whereas addImpulse(Vector2F) stores the impulse to be
     * combined at a later time.</p>
     *
     * @param body body.
     * @param container vector for computations.
     * @param impulse impulse to add.
     */
    private void addImpulse(BodyComponent body, Vector2F container, Vector2F impulse)
    {
        // Combine velocity vectors and set new velocity
        body.getVelocity(container);
        container.add(impulse);
        body.setVelocity(container);
    }

    /**
     * <p>Adds the friction impulse to the {@link SIContact}'s accumulation while keeping the accumulation within a
     * range defined by the given friction coefficient and returns the impulse that should actually be used when
     * applying friction to a body.</p>
     *
     * <p>This method follows Box2D's impulse clamping and accumulation scheme.</p>
     *
     * @param contact SIContact.
     * @param impulse friction impulse magnitude.
     * @param coefficient friction coefficient between both surfaces.
     * @return friction impulse to use instead.
     */
    private float accumulateFriction(SIContact contact, float impulse, float coefficient)
    {
        // Compute accumulated friction impulse
        final float accumulated = contact.getAccumulatedFriction();
        final float newAccumulation = accumulated + impulse;

        // Compute fraction of separation impulse as min/max (using coefficient of friction)
        final float cofSep = contact.getAccumulatedSeparation() * coefficient * FRICTION_DAMPING;

        // Make sure sum of all friction impulses obeys Coloumb friction
        contact.setAccumulatedFriction(Math.max(Math.min(newAccumulation, cofSep), -cofSep));

        // Return new impulse to use (this differs from given impulse if accumulated's clamped)
        return contact.getAccumulatedFriction() - accumulated;
    }

    /**
     * <p>Adds the magnitude of the separation impulse to the given {@link SIContact}'s accumulation and returns
     * the impulse to replace the magnitude passed as argument.</p>
     *
     * <p>The returned separation magnitude is only different than the magnitude passed in as an argument if the
     * argument magnitude is < 0 (i.e. the separation magnitude actually applied to a body is always >= 0).</p>
     *
     * <p>This method follows Box2D's impulse clamping and accumulation scheme.</p>
     *
     * @param contact SIContact.
     * @param separation impulse magnitude.
     * @return new separation impulse magnitude
     */
    private float accumulateSeparation(SIContact contact, float separation)
    {
        final float accumulated = contact.getAccumulatedSeparation();

        // Make sure sum of all impulses is never negative
        contact.setAccumulatedSeparation(Math.max(accumulated + separation, 0f));

        // Return new impulse to use (this differs from given impulse if accumulated's clamped)
        return contact.getAccumulatedSeparation() - accumulated;
    }

    /**
     * <p>Examines each non-static {@link BodyComponent} for collisions against all other BodyComponents, whether static
     * or non-static, and saves the collision as a {@link SIContact} in the {@link ContactGraph}.</p>
     *
     * <p>The process for each body consists of two phases: bounding box collision detection and finer {@link Shape}
     * based collision detection. {@link BoundingTree}s are first queried for bounding box collisions before
     * scrutinizing each such collision with the detection routine implemented in
     * {@link BodyComponent#collidesWith(BodyComponent, BodyComponent.Manifold)}. Bodies found to collide after this
     * second test obtain a {@link SIContact} or update a previously assigned one.</p>
     *
     * @param objectFactory GObject lookup.
     * @param factory body lookup.
     */
    private void searchForContacts(GObjectFactory objectFactory, BodyFactory factory)
    {
        // Examine each body for a collision with another
        for (int id = 0, i = 0, sz = factory.size(); i < sz; id++) {
            final BodyComponent body = factory.get(id);

            // Skip unused ids
            if (body == null) {
                continue;
            } else {
                // Count valid body to be processed
                i++;
            }

            // Sum external impulses added with body.addImpulse(Vector2F) to velocity
            integrateExternalImpulse(body);

            // Don't create Contacts for non-colliding and static since they likely haven't moved
            if (!body.isCollidable() || body.isStatic()) {
                continue;
            }

            // Perform collision detection against static bodies
            searchTreeForCollisions(objectFactory, mStaticTree, body);

            // Perform collision detection against dynamic bodies
            searchTreeForCollisions(objectFactory, mDynamicTree, body);
        }
    }

    /**
     * <p>Performs collision detection between the given {@link BodyComponent} and other bodies in its vicinity as
     * organized by the given {@link BoundingTree}.</p>
     *
     * <p>If two bodies are found to be colliding, a {@link SIContact} is assigned for the pair in the
     * {@link ContactGraph}.</p>
     *
     * <p>If a colliding pair of bodies is found to already have an associated Contact, the Contact's collision
     * manifold is updated instead. In the case where a body pair is found to no longer collide, the Contact is removed
     * .</p>
     *
     * <p>During a single {@link Solver} update, only one body of a Contact's body pair will process the Contact. For
     * example, if a Contact was handled when the Contact's body B was being examined, the Contact will not be
     * handled again when body A is examined, and vice-versa.</p>
     *
     * @param factory GObject lookup.
     * @param tree BoundingTree for the body.
     * @param body body to examine.
     */
    private void searchTreeForCollisions(GObjectFactory factory, BoundingTree tree, BodyComponent body)
    {
        assert (!body.isStatic());

        // Get bounding box collisions (that is, potential collisions)
        mCollisions.clear();
        tree.getCollisions(mCollisions, body);

        // Test against all others whose bounding boxes collided with the body's
        for (int x = 0, len = mCollisions.size(); x < len; x++) {
            final BodyComponent other = mCollisions.get(x);

            // Skip bodies requesting no collision
            if (!other.isCollidable() || shouldIgnore(factory, body, other)) {
                continue;
            }

            // Test bodies' actual shapes for collision
            final boolean collided = body.collidesWith(other, mManifold);

            // Get Contact between both bodies
            SIContact contact = mGraph.getContact(body, other);

            // Don't process if body B body already handled Contact
            if ((contact != null && contact.isHandled()) || mGraph.getContact(other, body) != null) {
                continue;
            }

            // Three cases to deal with
            if (collided) {

                // Case 1: prior contact and still colliding
                if (contact != null) {
                    // Update with new collision data
                    contact.setManifold(mManifold);
                    contact.handle();

                } else {
                    // Case 2: no prior contact but now colliding
                    final SIContact con = mGraph.addContact(body, other);

                    // Update with new collision data and mark as processed
                    con.setManifold(mManifold);
                    con.handle();
                }

                // Case 3: prior contact, bounding box collision, but not truly colliding
            } else if (contact != null) {
                // Remove Contact since bodies separated so no longer valid
                mGraph.removeContact(body, other);
            }
        }
    }

    /**
     * <p>Checks if collision between two bodies should be ignored because one of the bodies represents the parent
     * {@link GObject} of the other's {@link GObject} and the other
     * body's {@link BodyComponent#isIgnoreParentEnabled()} returns true. This relationship is checked for in both
     * directions where the first body argument is the parent and vice versa.</p>
     *
     * @param factory GObject lookup.
     * @param body body.
     * @param other other body.
     * @return true if one body's GObject is the parent of the other and the other wants collisions ignored.
     */
    private boolean shouldIgnore(GObjectFactory factory, BodyComponent body, BodyComponent other)
    {
        final BodyComponent ignoringBody;
        final BodyComponent bodyToIgnore;

        // Check if should care if other body belongs to parent
        if (!body.isIgnoreParentEnabled() && !other.isIgnoreParentEnabled()) {
            return false;

        } else if (body.isIgnoreParentEnabled()) {
            ignoringBody = body;
            bodyToIgnore = other;

        } else {
            ignoringBody = other;
            bodyToIgnore = body;
        }

        final GObject obj = factory.get(ignoringBody.getGObjectId());
        final int parentId = obj.getParentId();
        final int parentVer = obj.getParentVersion();

        // Match other body's id to parent
        return parentId == bodyToIgnore.getGObjectId() && parentVer == bodyToIgnore.getGObjectVersion();
    }

    /**
     * <p>Iterates over all {@link BodyComponent}s removing any orphaned and making sure spatial partitioning is
     * valid for any position or size changes that have occurred since the last update.</p>
     *
     * @param factory body lookup.
     */
    private void ensureSpacialPartitioning(BodyFactory factory)
    {
        // Iterate through all bodies
        for (int id = 0, i = 0, sz = factory.size(); i < sz; id++) {
            final BodyComponent body = factory.get(id);

            // Skip unused ids
            if (body == null) {
                continue;

            } else if (body.isOrphan()) {
                // Remove orphaned body
                factory.remove(id);
                mGraph.remove(body);
                updateSpacialPartitioning(body, true);

                // Decrement expected body count
                sz--;
                continue;

            } else {
                // Bump processed counter
                i++;
            }

            if (body.isCollidable()) {
                // Ensure body's in contact graph for tracking Contacts
                mGraph.add(body);
            } else {
                mGraph.remove(body);
            }

            // Make sure bounding box hierarchy is tracking the body
            updateSpacialPartitioning(body, false);
        }
    }

    /**
     * <p>Makes sure a {@link BodyComponent} resides in the correct {@link BoundingTree} (static bodies inside static
     * BoundingTree, dynamic in dynamic) and updates the tree's hierarchy if it was disrupted by the body changing
     * positions or sizes since the last call to this method. If true is given for <i>remove</i>, the given body is
     * instead removed from both BoundingTrees.</p>
     *
     * @param body body.
     * @param remove true to remove the body from all spatial partitioning.
     */
    private void updateSpacialPartitioning(BodyComponent body, boolean remove)
    {
        // Remove from all spatial partitioning if requested
        if (remove) {
            mStaticTree.remove(body);
            mDynamicTree.remove(body);
            return;
        }

        final BoundingTree preferred;
        final BoundingTree other;

        // Decide which BoundingTree the body should be in
        if (body.isStatic()) {
            preferred = mStaticTree;
            other = mDynamicTree;
        } else {
            preferred = mDynamicTree;
            other = mStaticTree;
        }

        // Try to add body to preferred tree
        if (preferred.add(body)) {
            // Successful add implies was in other tree before now
            other.remove(body);

        } else {
            // Add failure means was already there so just update position
            preferred.update(body);
        }
    }

    /**
     * <p>Checks if a {@link BodyComponent} is movable because it's neither static nor sleeping.</p>
     *
     * @param body body.
     * @return true if neither static nor sleeping.
     */
    private boolean isDynamic(BodyComponent body)
    {
        return !body.isStatic() && !body.isSleeping();
    }

    /**
     * <p>
     *     SIContacts support, at least a portion of, the sequential impulses setup of the Box2D physics engine. This
     *     includes storing accumulated impulses for separation and friction as well as warmstarting through
     *     {@link #warm(Vector2F, Vector2F, Vector2F)} and a velocity bias term with
     *     {@link #setSeparationBias(float)}.
     * </p>
     */
    private class SIContact extends ContactGraph.Contact
    {
        // Sum of iterated separation impulses
        private float mAccuSeparation = 0f;

        // Sum of iterated friction impulses
        private float mAccuFriction = 0f;

        // Velocity bias used during solve iterations
        private float mSepBias = 0f;

        // Inverse of system's inverse mass; 1 / ((1 / ma) + (1 / mb))
        private float mInvSysMass = 0f;

        @Override
        protected void onBodiesSet(BodyComponent bodyA, BodyComponent bodyB)
        {
            mInvSysMass = 1f / (bodyA.getInverseMass() + bodyB.getInverseMass());
        }

        /**
         * <p>Gets the inverse of the body pairs' total inverse mass: 1 / ((1 / ma) + (1 / mb))</p>
         *
         * @return inverse of total inverse mass.
         */
        private float getInverseSystemMass()
        {
            return mInvSysMass;
        }

        /**
         * <p>Checks if both body A and body B are <i>effectively</i> sleeping. That is, in situations such as if body A
         * is sleeping and body B is not but body B is static, then this method still returns true.</p>
         *
         * @return true if both bodies shouldn't be moved.
         */
        private boolean isSleeping()
        {
            final BodyComponent bodyA = getBodyA();
            final BodyComponent bodyB = getBodyB();

            // Should body A not be moved?
            if ((!bodyA.isSleeping() && bodyA.isStatic()) || (bodyA.isSleeping() && !bodyA.isStatic())) {

                // Should body B not be moved?
                if ((!bodyB.isSleeping() && bodyB.isStatic()) || (bodyB.isSleeping() && !bodyB.isStatic())) {
                    return true;
                }
            }

            return false;
        }

        /**
         * <p>Applies the accumulated separation and friction impulses from the previous frame to both A and B
         * {@link BodyComponent}s.</p>
         *
         * @param container0 vector to use during computations.
         * @param container1 vector to use during computations.
         * @param container2 vector to use during computations.
         */
        private void warm(Vector2F container0, Vector2F container1, Vector2F container2)
        {
            // Reset handle flag for upcoming processing
            this.clearHandle();

            final BodyComponent bodyA = getBodyA();
            final BodyComponent bodyB = getBodyB();

            final boolean dynA = isDynamic(bodyA);
            final boolean dynB = isDynamic(bodyB);

            // Fill vectors with impulses
            getImpulsesForBodyB(container1, container2);

            // Case 1: both bodies are dynamic so apply impulses to both
            if (dynA && dynB) {

                // Impulses for body A
                container1.negate();
                warm(bodyA, container0, container1, container2);

                // Impulses for body B
                getImpulsesForBodyB(container1, container2);
                warm(bodyB, container0, container1, container2);

            } else if (dynA) {
                // Case 2: only body A can be moved
                container1.negate();
                warm(bodyA, container0, container1, container2);

            } else if (dynB) {
                // Case 3: only body B can be moved
                warm(bodyB, container0, container1, container2);
            }
        }

        /**
         * <p>Fills two {@link Vector2F}s with the accumulated separation and friction impulses for body A.</p>
         *
         * @param separation separation impulse.
         * @param friction friction impulse.
         */
        private void getImpulsesForBodyB(Vector2F separation, Vector2F friction)
        {
            // Get separation impulse from accumulated
            getManifold().getCollisionNormal(separation);
            separation.multiply(mAccuSeparation);

            // Get friction impulse from accumulated
            mManifold.getCollisionNormal(friction);
            friction.normal(false);
            friction.multiply(mAccuFriction);
        }

        /**
         * <p>Applies the accumulated impulse to a {@link BodyComponent}.</p>
         *
         * @param body body.
         * @param velocity vector to hold velocity.
         * @param separation impulse vector.
         * @param friction impulse vector.
         */
        private void warm(BodyComponent body, Vector2F velocity, Vector2F separation, Vector2F friction)
        {
            // Distribute over body
            separation.multiply(body.getInverseMass());
            friction.multiply(body.getInverseMass());

            // Apply impulses
            body.getVelocity(velocity);
            velocity.add(separation);
            velocity.add(friction);
            body.setVelocity(velocity);
        }

        /**
         * <p>Gets the accumulated impulse between the body pair.</p>
         *
         * @return accumulated impulse.
         */
        private float getAccumulatedSeparation()
        {
            return mAccuSeparation;
        }

        /**
         * <p>Sets the accumulated impulse between the body pair.</p>
         *
         * @param impulse accumulated impulse.
         */
        private void setAccumulatedSeparation(float impulse)
        {
            mAccuSeparation = impulse;
        }

        /**
         * <p>Gets the accumulated friction impulse.</p>
         *
         * @return accumulated friction.
         */
        private float getAccumulatedFriction()
        {
            return mAccuFriction;
        }

        /**
         * <p>Sets the accumulated friction impulse.</p>
         *
         * @param friction accumulated friction.
         */
        private void setAccumulatedFriction(float friction)
        {
            mAccuFriction = friction;
        }

        /**
         * <p>Gets the velocity bias term.</p>
         *
         * @return velocity bias.
         */
        private float getBias()
        {
            return mSepBias;
        }

        /**
         * <p>Sets the bias term to use when solving for the separation velocity.</p>
         *
         * @param bias separation bias.
         */
        private void setSeparationBias(float bias)
        {
            mSepBias = bias;
        }

        @Override
        protected void onClear()
        {
            mAccuSeparation = 0f;
            mAccuFriction = 0f;
            mSepBias = 0f;
        }
    }

    /**
     * <p>
     *     {@link ContactGraph} using {@link SIContact} adjusted for the sequential impulses setup in the
     *     {@link IterativeSolver}.
     * </p>
     */
    private class SIContactGraph extends ContactGraph<SIContact>
    {
        // Maximum number of supported bodies
        private static final int CAPACITY = 500;

        /**
         * <p>Constructs an SIContactGraph.</p>
         */
        public SIContactGraph()
        {
            super(CAPACITY);
        }

        @Override
        protected SIContact createContact()
        {
            return new SIContact();
        }

        @Override
        protected boolean isInvalid(SIContact contact)
        {
            // Invalid if either body is missing or Contact's unhandled by the time this is called
            return super.isInvalid(contact) || !contact.isHandled();
        }

        @Override
        protected boolean isEligibleForSleep(BodyComponent body)
        {
            // Get frame's speed
            body.getPreviousVelocity(mVelocityB);
            final float prevSpeed = mVelocityB.magnitude();

            final float speed = body.getSpeed();

            // Eligible only if speed difference across frames is below threshold
            return speed <= MAX_SLEEP_THRESHOLD && Math.abs(speed - prevSpeed) < MAX_DIFF_SLEEP_SPEED;
        }

        @Override
        protected void onSleep(BodyComponent body)
        {
        }
    }

    /**
     * <p>
     *     Removes a {@link BodyComponent} from the {@link ContactGraph} when the {@link BodyFactory} removes it.
     * </p>
     */
    private class GraphRemoveSync implements OnRemoveListener<BodyComponent>
    {
        @Override
        public void onRemove(BodyComponent object)
        {
            mGraph.remove(object);
        }
    }
}
