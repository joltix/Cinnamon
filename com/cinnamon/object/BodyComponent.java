package com.cinnamon.object;

import com.cinnamon.system.ComponentFactory;
import com.cinnamon.utils.*;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     A BodyComponent represents the 2D collision polygon for a {@link GObject} as well as a body for the
 *     {@link Solver} to manipulate during physics computations.
 * </p>
 *
 * <p>
 *     The current collision detection algorithm is performed via SAT (Separating Axis Theorem) to accurately
 *     determine if a collision has taken place. For contact point generation, clipping is used with the
 *     separating axis of minimum translation found during the detection step.
 * </p>
 *
 * <p>
 *     Information about a collision is stored in a {@link Manifold} that is given to
 *     {@link #collidesWith(BodyComponent, Manifold)}. This includes contact points, each point's penetration depth,
 *     and a unit collision normal facing in the direction of the BodyComponent that calls collidesWith
 *     (BodyComponent, Manifold) to the BodyComponent passed in as an argument.
 * </p>
 *
 * <p>
 *     Aside from collision operations, BodyComponents also hold quantities used during physics computations in the
 *     {@link Solver}. These include (but are not limited to) mass, velocity, and coefficients of restitution and
 *     friction, the first two being measured in kilograms and meters per second, respectively.
 * </p>
 *
 * <p>
 *     Toggles are available for disabling or enabling certain features such as collision with
 *     {@link #setCollidable(boolean)}.
 * </p>
 *
 * <p>
 *     Both the SAT detection and contact point generation algorithms were originally learned from http://www.dyn4j
 *     .org/.
 * </p>
 */
public final class BodyComponent extends ComponentFactory.Component implements Positional, Rotatable
{
    /**
     * Collision testing vars to be reused for each SAT run for collidesSAT()
     */

    // Separating axis to be used during SAT
    private final Vector2F mSeparatingAxis = new Vector2F();

    // An edge's start and end points
    private final float[] mEdgePt0 = new float[2];
    private final float[] mEdgePt1 = new float[2];

    // Minimum and maximum containers for each Shape's projection
    private final Vector2F mMinMax0 = new Vector2F();
    private final Vector2F mMinMax1 = new Vector2F();

    // Separating axis with minimum depth overlap as a result of SAT, for use in contact gen
    private final Vector2F mMinDepthVec = new Vector2F();

    // Minimum projection used during SAT to find minimum dist to separate
    private float mMinDepth;

    /**
     * Contact generation vars for getContacts()
     */

    // Reference edge used during contact point generation
    private final CollidingEdge mRefEdge = new CollidingEdge();

    // Incident edge used during contact point generation
    private final CollidingEdge mIncEdge = new CollidingEdge();

    // Container vector representing an edge (one end minus the other) used in computations
    private final Vector2F mEdge = new Vector2F();

    /**
     * BodyComponent instance specific properties
     */

    // Anchor in tree that allows O(1) lookup
    private BoundingTree.Node mContainer;

    // Anchor in graph for O(1) lookup
    private ContactGraph.Node mContactNode;

    // Polygon for fine-grain collision checking
    private Shape mShape;

    // Toggle to allow selection
    private boolean mSelectable = true;

    // Toggle to disable collision reactions
    private boolean mCollidable = true;

    // True to ignore collisions with GObject's parent
    private boolean mIgnoreGObjectParent = false;

    /**
     * Physics
     */

    // Kilograms
    private float mMass = 70f;

    // Inverse mass (in kilograms)
    private float mInvMass = 1f / mMass;

    // Meters per second
    private final Vector2F mVelocity = new Vector2F();

    // Velocity from previous physics update
    private final Vector2F mOldVelocity = new Vector2F();

    // Impulse vector to be applied during each move() and cleared afterwards
    private final Vector2F mImpulse = new Vector2F();

    // Coefficient of friction
    private float mCOF = 0.5f;

    // Coefficient of restitution
    private float mCOR = 0.8f;

    // Sleep optimization flag
    private boolean mSleeping = false;

    /**
     * <p>Constructs a BodyComponent with a specific {@link Shape}. The stored Shape is a copy of the given Shape.</p>
     *
     * @param shape Shape.
     */
    public BodyComponent(Shape shape)
    {
        mShape = new Shape(shape);
    }

    /**
     * <p>Checks whether or not another BodyComponent's {@link Shape} is intersecting and, if so, populates a given
     * {@link Manifold} with information about the collision.</p>
     *
     * @param other BodyComponent.
     * @param manifold container for collision info.
     * @return true if the Shapes intersect.
     */
    public final boolean collidesWith(BodyComponent other, Manifold manifold)
    {
        // If either body isn't collidable, no collision ops can be done
        if (!isCollidable() || !other.isCollidable()) {
            return false;
        }

        // No separation axis implies no collision
        final Vector2F sepAxis = collidesSAT(other, true);
        if (sepAxis == null) {
            return false;
        }

        // Make sure separating axis' direction to point towards the other body
        if (isPointingTowards(getCenterX(), getCenterY(), sepAxis, other.getCenterX(), other.getCenterY())) {
            sepAxis.negate();
        }

        // Erase old collision data and get contact lists
        manifold.clear();
        final List<Vector2F> contacts = manifold.mPoints;
        final List<Float> depths = manifold.mDepths;

        // Compute contact points for current collision
        getContacts(contacts, depths, other, sepAxis);

        // There should be points of contact if a collision occurred
        assert (!contacts.isEmpty());

        // Compute collision normal from the reference edge chosen in contact gen
        final Vector2F collisionNormal = mRefEdge.mEnd;
        collisionNormal.subtract(mRefEdge.mBegin);
        collisionNormal.normal(true);
        collisionNormal.normalize();

        // Make sure collision normal points towards other object
        if (isPointingTowards(getCenterX(), getCenterY(), collisionNormal, other.getCenterX(), other.getCenterY())) {
            collisionNormal.negate();
        }

        // Fill manifold with collision info
        manifold.setContactNormal(collisionNormal);
        manifold.setContactPoint(0, contacts.get(0), depths.get(0));
        if (contacts.size() > 1) {
            manifold.setContactPoint(1, contacts.get(1), depths.get(1));
        }

        return true;
    }

    /**
     * <p>Checks if a position and a pseudo velocity vector is headed in the direction of another position. The
     * second position's velocity is assumed to be the zero vector.</p>
     *
     * @param x0 first position's x.
     * @param y0 first position's y.
     * @param velocity pseudo velocity.
     * @param x1 second position's x.
     * @param y1 second position's y.
     * @return true if the first position is pointing towards the second.
     */
    private boolean isPointingTowards(float x0, float y0, Vector2F velocity, float x1, float y1)
    {
        final float xDiff = x0 - x1;
        final float yDiff = y0 - y1;

        final float dot = (velocity.getX() * xDiff) + (velocity.getY() * yDiff);
        return dot > 0f;
    }

    /**
     * <p>Performs collision testing against another {@link BodyComponent}'s {@link Shape} using the Separating Axis
     * Theorem.</p>
     *
     * @param other other BodyComponent.
     * @param reverseRoles true to perform SAT from the other body's point of view as well.
     * @return the separating axis with the minimum distance needed for separation, or null if there was no collision.
     */
    private Vector2F collidesSAT(BodyComponent other, boolean reverseRoles)
    {
        final Shape otherShape = other.getShape();

        mMinDepth = Float.MAX_VALUE;

        // Find min/max projected points of first shape
        for (int i = 0, sz = mShape.getPointCount(); i < sz; i++) {

            // Get points forming an edge
            mShape.getPoint(i, mEdgePt0);
            mShape.getPoint((i + 1) % sz, mEdgePt1);

            // Compute separation axis
            mSeparatingAxis.set(mEdgePt1[0] - mEdgePt0[0], mEdgePt1[1] - mEdgePt0[1]);

            // Make it a unit vector and transform perpendicular
            mSeparatingAxis.normalize();
            mSeparatingAxis.normal(false);

            // Find the min/max projections of each shape onto the axis
            BodyComponent.findMinMaxProjections(mShape, mSeparatingAxis, mMinMax0);
            BodyComponent.findMinMaxProjections(otherShape, mSeparatingAxis, mMinMax1);

            // Bail out as soon as a separation is found (min = x, max = y)
            final boolean case0 = (mMinMax0.getX() > mMinMax1.getY() || Point2F.isEqual(mMinMax0.getX(),
                    mMinMax1.getY()));
            final boolean case1 = (mMinMax0.getY() < mMinMax1.getX() || Point2F.isEqual(mMinMax0.getY(),
                    mMinMax1.getX()));
            if (case0 || case1) {
                return null;
            }

            // Compute shape's projection overlap on current separating axis
            final float depth = Math.min(Math.abs(mMinMax0.getX() - mMinMax1.getY()), Math.abs(mMinMax1.getX() -
                    mMinMax0.getY()));

            // Track separating axis with the minimum overlap
            if (depth < mMinDepth) {
                mMinDepth = depth;
                mMinDepthVec.copy(mSeparatingAxis);
            }
        }

        // Test SAT once more but switch BodyComponent's roles
        if (reverseRoles) {

            // Bail out if a space was found from the other shape's pov
            final Vector2F otherMinVec = other.collidesSAT(this, false);
            if (otherMinVec == null) {
                return null;
            }

            // Return separating axis with min overlap between both Shapes
            return new Vector2F((mMinDepth < other.mMinDepth) ? mMinDepthVec : otherMinVec);
        }

        // Return separating axis with min overlap amongst all local axes
        return new Vector2F(mMinDepthVec);
    }

    /**
     * <p>Finds the minimum and maximum projections of a given {@link Shape} onto an axis. The min-max values are
     * stored in a {@link Vector2F} where x is minimum and y is maximum.</p>
     *
     * @param shape Shape with points to project.
     * @param axis axis to project on to.
     * @param container holds min-max values when done.
     */
    private static void findMinMaxProjections(Shape shape, Vector2F axis, Vector2F container)
    {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        // Project each point onto the vector
        for (int i = 0, sz = shape.getPointCount(); i < sz; i++) {
            // Convert point to vector for methods
            shape.getPoint(i, container);

            // Compute projection constant and track min/max
            final float constant = axis.dotProduct(container);
            if (constant < min || Point2F.isEqual(constant, min)) {
                min = constant;
            }
            if (constant > max || Point2F.isEqual(constant, max)) {
                max = constant;
            }
        }

        // Store values in vector
        container.set(min, max);
    }

    /**
     * <p>Checks whether or not an (x,y) point is found within the BodyComponent.</p>
     *
     * @param x x.
     * @param y y.
     * @return true if the point exists within.
     */
    public final boolean contains(float x, float y)
    {
        // Check if point does not exist within the bounding box
        if (!mShape.getBounds().contains(x, y)) {
            return false;
        }

        // Check if point is within shape
        return mShape.contains(x, y);
    }

    /**
     * <p>Gets the {@link Shape} used for narrow phase collision detection.</p>
     *
     * @return Shape.
     */
    public final Shape getShape()
    {
        return mShape;
    }

    /**
     * <p>Sets the {@link Shape} to use for narrow phase collision detection.</p>
     *
     * @param shape Shape.
     */
    public final void setShape(Shape shape)
    {
        mShape = new Shape(shape);

        // Ensure shape can't be modified after set
        mShape.lock();
    }
    /**
     * <p>Gets the {@link Rect2D} to use as a bounding box for broad phase collision detection.</p>
     *
     * @return bounding box.
     */
    public final Rect2D getBounds()
    {
        return mShape.getBounds();
    }

    @Override
    public final float getWidth()
    {
        return mShape.getWidth();
    }

    @Override
    public final void setWidth(float width)
    {
        mShape.setWidth(width);
    }

    @Override
    public final float getHeight()
    {
        return mShape.getHeight();
    }

    @Override
    public final void setHeight(float height)
    {
        mShape.setHeight(height);
    }

    @Override
    public float getCenterX()
    {
        return getX() + (getWidth() / 2f);
    }

    @Override
    public float getCenterY()
    {
        return getY() + (getHeight() / 2f);
    }

    @Override
    public double getRotation()
    {
        return mShape.getRotation();
    }

    @Override
    public void rotateTo(double angle)
    {
        mShape.rotateTo(angle);
    }

    @Override
    public void rotateBy(double angle)
    {
        mShape.rotateBy(angle);
    }

    /**
     * <p>Gets the coefficient of friction used when computing collision reactions.</p>
     *
     * @return coefficient of friction.
     */
    public final float getFriction()
    {
        return mCOF;
    }

    /**
     * <p>Sets the coefficient of friction.</p>
     *
     * @param coefficient coefficient.
     */
    public final void setFriction(float coefficient)
    {
        mCOF = coefficient;
    }

    /**
     * <p>Gets the coefficient of restitution used when computing collision reactions.</p>
     *
     * @return coefficient of restitution.
     */
    public final float getRestitution()
    {
        return mCOR;
    }

    /**
     * <p>Sets the coefficient of restitution.</p>
     *
     * @param coefficient coefficient.
     */
    public final void setRestitution(float coefficient)
    {
        mCOR = coefficient;
    }

    /**
     * <p>Gets the velocity.</p>
     *
     * @return velocity vector.
     */
    public final Vector2F getVelocity()
    {
        return new Vector2F(mVelocity);
    }

    /**
     * <p>Copies the velocity into a given vector.</p>
     *
     * @param container vector to copy to.
     */
    public final void getVelocity(Vector2F container)
    {
        container.copy(mVelocity);
    }

    /**
     * <p>Sets the velocity. If null is given, the current velocity is zeroed.</p>
     *
     * @param velocity velocity vector.
     */
    public final void setVelocity(Vector2F velocity)
    {
        // Clear the velocity if given null
        if (velocity == null) {
            mVelocity.set(0f, 0f);
            return;
        }

        mVelocity.copy(velocity);
    }

    /**
     * <p>Gets the velocity from before the most recent physics update.</p>
     *
     * @param container container.
     */
    final void getPreviousVelocity(Vector2F container)
    {
        container.copy(mOldVelocity);
    }

    /**
     * <p>Version of {@link #setVelocity(Vector2F)} for use in computing the new BodyComponent's new velocity during a
     * physics update. This changes the velocity returned by {@link #getPreviousVelocity(Vector2F)} to the vector
     * before this method is called.</p>
     *
     * @param velocity new velocity.
     */
    final void updateVelocity(Vector2F velocity)
    {
        mOldVelocity.copy(mVelocity);
        mVelocity.copy(velocity);
    }

    /**
     * <p>Gets the current speed in meters per second.</p>
     *
     * @return current speed.
     */
    public final float getSpeed()
    {
        return mVelocity.magnitude();
    }

    /**
     * <p>Gets the impulse vector to be applied in the next motion step.</p>
     *
     * @return impulse.
     */
    public final Vector2F getImpulse()
    {
        return new Vector2F(mImpulse);
    }

    /**
     * <p>Copies the impulse into a given vector.</p>
     *
     * @param container vector to copy to.
     */
    public final void getImpulse(Vector2F container)
    {
        container.copy(mImpulse);
    }

    /**
     * <p>Sets the impulse to be added during the next motion step.</p>
     *
     * @param impulse impulse.
     */
    public final void setImpulse(Vector2F impulse)
    {
        if (impulse == null) {
            mImpulse.set(0f, 0f);
            return;
        }

        mImpulse.copy(impulse);
    }

    /**
     * <p>Adds an impulse to be added during the next motion step.</p>
     *
     * @param impulse impulse.
     */
    public final void addImpulse(Vector2F impulse)
    {
        mImpulse.add(impulse);
    }

    /**
     * <p>Gets the acceleration.</p>
     *
     * <p>This is the difference between the current and previous velocity.</p>
     *
     * @return acceleration vector.
     */
    public final Vector2F getAcceleration()
    {
        return new Vector2F(mVelocity).subtract(mOldVelocity);
    }

    /**
     * <p>Copies the acceleration into a given vector.</p>
     *
     * <p>This is the difference between the current and previous velocity.</p>
     *
     * @param container vector to copy to.
     */
    public final void getAcceleration(Vector2F container)
    {
        container.copy(mVelocity);
        container.subtract(mOldVelocity);
    }

    /**
     * <p>Gets the mass in kilograms.</p>
     *
     * @return kilograms.
     */
    public final float getMass()
    {
        return mMass;
    }

    /**
     * <p>Sets the mass in kilograms.</p>
     *
     * @param mass kilograms.
     * @throws IllegalArgumentException if mass < 0.
     */
    final void setMass(float mass)
    {
        // Ensure no negative masses
        if (mass < 0f) {
            throw new IllegalArgumentException("Mass should be >= 0: " + mass);
        }

        mMass = mass;

        // Precompute inverse mass; store inverse infinite as 0
        mInvMass = (mass == 0f) ? 0f : 1f / mass;
    }

    /**
     * <p>Gets the inverse mass in kilograms. This value is often used in physics.</p>
     *
     * @return (1 / mass) kilograms
     */
    public final float getInverseMass()
    {
        return mInvMass;
    }

    @Override
    public final Point3F getPosition()
    {
        return mShape.getPosition();
    }

    @Override
    public final float getX()
    {
        return mShape.getX();
    }

    @Override
    public final float getY()
    {
        return mShape.getY();
    }

    @Override
    public final float getZ()
    {
        return mShape.getZ();
    }

    @Override
    public final void moveTo(float x, float y)
    {
        // Move shape along
        mShape.moveTo(x, y);
    }

    @Override
    public final void moveTo(float x, float y, float z)
    {
        // Move shape along
        mShape.moveTo(x, y, z);
    }

    @Override
    public final void moveBy(float x, float y)
    {
        moveBy(x, y, 0f);
    }

    @Override
    public final void moveBy(float x, float y, float z)
    {
        // Move shape along
        mShape.moveBy(x, y, z);
    }

    @Override
    public void moveToCenter(float x, float y)
    {
        mShape.moveTo(mShape.getX() - (mShape.getWidth() / 2f), mShape.getY() - (mShape.getHeight() / 2f));
    }

    /**
     * <p>Checks whether the BodyComponent can be selected.</p>
     *
     * @return true if selection is allowed.
     */
    public final boolean isSelectable()
    {
        return mSelectable;
    }

    /**
     * <p>Sets whether or not the BodyComponent can be selected.</p>
     *
     * @param enable true to allow selection.
     */
    public final void setSelectable(boolean enable)
    {
        mSelectable = enable;
    }

    /**
     * <p>Checks if the BodyComponent has infinite mass and is not meant to move during physics updates.</p>
     *
     * @return true if not meant to move through physics.
     */
    public final boolean isStatic()
    {
        return mMass == 0f;
    }

    /**
     * <p>Checks if the BodyComponent should process collisions with other BodyComponents.</p>
     *
     * @return true if collisions are allowed.
     */
    public final boolean isCollidable()
    {
        return mCollidable;
    }

    /**
     * <p>Sets whether or not the BodyComponent is allowed to process collisions with other BodyComponents.</p>
     *
     * <p>Using {@link #collidesWith(BodyComponent, Manifold)} with a non-collidable BodyComponent, whether
     * caller or argument, will return false.</p>
     *
     * @param enable true to allow collisions.
     */
    public final void setCollidable(boolean enable)
    {
        mCollidable = enable;
    }

    /**
     * <p>Checks if collisions with the owning {@link GObject}'s parent are ignored.</p>
     *
     * @return true if collisions with the GObject's parent are ignored.
     */
    public final boolean isIgnoreParentEnabled()
    {
        return mIgnoreGObjectParent;
    }

    /**
     * <p>Sets whether to ignore collisions with the {@link GObject}'s parent.</p>
     *
     * @param enable true to ignore collisions with the GObject's parent.
     */
    public final void setIgnoreGObjectParent(boolean enable)
    {
        mIgnoreGObjectParent = enable;
    }

    /**
     * <p>Gets the {@link BoundingTree.Node} containing the BodyComponent for fast lookup.</p>
     *
     * @return BoundingTree's Node.
     */
    BoundingTree.Node getContainer()
    {
        return mContainer;
    }

    /**
     * <p>Sets the {@link BoundingTree.Node} to represent the BodyComponent within the tree.</p>
     *
     * @param container Node.
     */
    void setContainer(BoundingTree.Node container)
    {
        mContainer = container;
    }

    /**
     * <p>Gets the {@link ContactGraph.Node} representing the BodyComponent in a {@link ContactGraph} for fast
     * lookup.</p>
     *
     * @return ContactGraph's Node.
     */
    ContactGraph.Node getContactNode()
    {
        return mContactNode;
    }

    /**
     * <p>Sets the {@link ContactGraph.Node} to represent the BodyComponent within a {@link ContactGraph}. This
     * allows for fast lookup in the graph.</p>
     *
     * @param node Node.
     */
    void setContactNode(ContactGraph.Node node)
    {
        mContactNode = node;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException("BodyComponents should only come from the BodyFactory");
    }

    /**
     * <p>Computes contact points between this BodyComponent and another. This method is step #2 of collision
     * detection from {@link #collidesWith(BodyComponent, Manifold)}.</p>
     *
     * @param contacts container for points.
     * @param depths container for points' penetration depths.
     * @param other colliding body.
     * @param sepAxis separating axis of minimum translation.
     */
    private void getContacts(List<Vector2F> contacts, List<Float> depths, BodyComponent other, Vector2F sepAxis)
    {
        // Find calling Shape's colliding edge
        findContactEdge(mRefEdge, mShape, sepAxis);

        // Find other Shape's colliding edge (flip separating axis to originate from other body)
        sepAxis.negate();
        other.findContactEdge(mIncEdge, other.mShape, sepAxis);
        sepAxis.negate();

        // Swap reference and incident edges if incident is more perpendicular against axis
        if (shouldSwap(mRefEdge, mIncEdge, sepAxis)) {
            swapRoles(mRefEdge, mIncEdge);
        }

        // Clip shapes and save contact points and penetration depths
        clipShapes(contacts, depths, mRefEdge, mIncEdge);
    }

    /**
     * <p>Swaps the vertices between the reference and incident edges.</p>
     *
     * @param reference reference edge.
     * @param incident incident edge.
     */
    private void swapRoles(CollidingEdge reference, CollidingEdge incident)
    {
        // Set aside reference's data
        final Vector2F begin = reference.mBegin;
        final Vector2F end = reference.mEnd;
        final boolean farthest = reference.mBeginFarthest;

        // Copy incident's data to reference
        reference.mBegin = incident.mBegin;
        reference.mEnd = incident.mEnd;
        reference.mBeginFarthest = incident.mBeginFarthest;

        // Set reference's old data as incident
        incident.mBegin = begin;
        incident.mEnd = end;
        incident.mBeginFarthest = farthest;
    }

    /**
     * <p>Perform clipping operations between the two edges.</p>
     *
     * @param contacts contact points.
     * @param depths penetration depths.
     * @param reference reference edge.
     * @param incident incident edge.
     */
    private void clipShapes(List<Vector2F> contacts, List<Float> depths, CollidingEdge reference, CollidingEdge incident)
    {
        // Make direction vector from reference edge
        mEdge.copy(reference.mEnd);
        mEdge.subtract(reference.mBegin);
        mEdge.normalize();

        // Clip at reference edge's first vertex
        float offset = mEdge.dotProduct(reference.mBegin);
        clip(contacts, incident.mBegin, incident.mEnd, mEdge, offset);

        assert (contacts.size() >= 2);

        // Clip at reference edge's second vertex with direction reversed
        offset = mEdge.dotProduct(reference.mEnd);
        mEdge.negate();
        clip(contacts, contacts.get(0), contacts.get(1), mEdge, -offset);
        mEdge.negate();

        // Transform ref unit vector into a normal towards the reference edge
        makeNormalTowardsReference(mEdge, reference, incident);

        // Add first contact point if within clip area (depth >= 0f is inside)
        offset = mEdge.dotProduct(reference.getFarthestVertex());
        final float depth0 = mEdge.dotProduct(contacts.get(0)) - offset;
        if (depth0 < 0f) {
            contacts.remove(0);
        } else {
            depths.add(depth0);
        }

        // Add second contact point if within clip area (depth >= 0f is inside)
        final float depth1 = mEdge.dotProduct(contacts.get(contacts.size() - 1)) - offset;
        if (depth1 < 0f) {
            contacts.remove(contacts.size() - 1);
        } else {
            depths.add(depth1);
        }
    }

    /**
     * <p>Turns the reference unit vector into a perpendicular vector facing towards the reference edge.</p>
     *
     * @param refUnit reference's unit vector.
     * @param refEdge reference edge.
     * @param incEdge incident edge.
     */
    private void makeNormalTowardsReference(Vector2F refUnit, CollidingEdge refEdge, CollidingEdge incEdge)
    {
        final Vector2F incVertex = incEdge.getFarthestVertex();

        // Compute distance from first normal (positioned from ref edge) to the inc edge's farthest vertex
        final Vector2F norm0 = refUnit.getNormal(false);
        norm0.add(refEdge.mBegin);
        final double dist0 = Math.abs(Point2F.distanceBetween(norm0.getX(), norm0.getY(), incVertex.getX(), incVertex
                .getY()));

        // Compute distance from second normal (positioned from ref edge) to the inc edge's farthest vertex
        final Vector2F norm1 = refUnit.getNormal(true);
        norm1.add(refEdge.mBegin);
        final double dist1 = Math.abs(Point2F.distanceBetween(norm1.getX(), norm1.getY(), incVertex.getX(), incVertex
                .getY()));

        if (dist0 < dist1) {
            refUnit.normal(false);
        } else {
            refUnit.normal(true);
        }
    }

    /**
     * <p>Clips a line segment formed by two points against a normal moved by an offset and adds new points to the
     * list. If a point previously added to the list is outside the new clipping area, the point is removed.</p>
     *
     * @param points container.
     * @param incBegin line's starting point.
     * @param incEnd line's ending point.
     * @param normal normal to clip against.
     * @param offset normal's offset.
     */
    private void clip(List<Vector2F> points, Vector2F incBegin, Vector2F incEnd, Vector2F normal, float offset)
    {
        // Compute dot products of each point on the edge with an offset to origin
        final float edgeDot0 = incBegin.dotProduct(normal) - offset;
        final float edgeDot1 = incEnd.dotProduct(normal) - offset;

        // Check if second point is in list
        final boolean contained0 = points.size() > 0 && points.get(0) == incBegin;
        if ((edgeDot0 > 0f || Point2F.isEqual(edgeDot0, 0f))) {

            // Save beginning point if not inside
            if (!contained0) {
                points.add(incBegin);
            }
        } else if (contained0) {
            points.remove(0);
        }

        // Check if second point is in list
        final boolean contained1 = !points.isEmpty() && points.get(0) == incEnd
                || (points.size() > 1 && points.get(1) == incEnd);
        if ((edgeDot1 > 0f || Point2F.isEqual(edgeDot1, 0f))) {

            // Save second point if not inside
            if (!contained1) {
                points.add(incEnd);
            }
        } else if (contained1) {
            points.remove(points.size() - 1);
        }

        // Negative product means a point was dropped (dropped point's dot prod was > 0)
        if ((edgeDot0 < 0f || edgeDot1 < 0f)) {

            // Make new point out of incident edge
            final Vector2F clippedPt = new Vector2F(incEnd);
            clippedPt.subtract(incBegin);

            // Compute clipped point along edge
            clippedPt.multiply(edgeDot0 / (edgeDot0 - edgeDot1));
            clippedPt.add(incBegin);

            points.add(clippedPt);
        }
    }

    /**
     * <p>Checks if the reference edge and incident edges should be swapped as the reference edge should be the edge
     * more perpendicular to the separating axis.</p>
     *
     * @param reference reference edge.
     * @param incident incident edge.
     * @param axis separating axis.
     * @return true if given incident edge should be reference.
     */
    private boolean shouldSwap(CollidingEdge reference, CollidingEdge incident, Vector2F axis)
    {
        // Create a vector from reference edge and project onto axis
        mEdge.copy(reference.mEnd);
        mEdge.subtract(reference.mBegin);
        final float refDot = Math.abs(mEdge.dotProduct(axis));

        // Create a vector from incident edge and project onto axis
        mEdge.copy(incident.mEnd);
        mEdge.subtract(incident.mBegin);
        final float incDot = Math.abs(mEdge.dotProduct(axis));

        // Should swap roles so edge less perpendicular to separating axis is "incident edge"
        return incDot < refDot;
    }

    /**
     * <p>Determines the edge of a {@link Shape} which is most likely colliding with another Shape.</p>
     *
     * @param edge container to fill with the colliding edge.
     * @param shape Shape which the edge belongs to.
     * @param sepAxis separating axis facing outwards from the Shape.
     */
    private void findContactEdge(CollidingEdge edge, Shape shape, Vector2F sepAxis)
    {
        final float[] pt = new float[2];
        final int ptCount = shape.getPointCount();

        // Find edge's first point
        int farthestIndex = 0;
        float maxProj = -Float.MAX_VALUE;
        for (int i = 0; i < ptCount; i++) {
            shape.getPoint(i, pt);

            // Save pt with largest projection onto sep axis (closest pt to other shape)
            final float proj = (pt[0] * sepAxis.getX()) + (pt[1] * sepAxis.getY());
            if (maxProj < proj) {
                maxProj = proj;
                farthestIndex = i;
            }
        }

        // Set closest pt to other shape as edge's beginning point
        shape.getPoint(farthestIndex, pt);
        final Vector2F farthestPt = new Vector2F(pt[0], pt[1]);

        // Figure edge's both possible end points (beginning point's neighbors)
        final int endPtIndex0 = (farthestIndex == 0) ? ptCount - 1 : farthestIndex - 1;
        final int endPtIndex1 = (farthestIndex == (ptCount - 1)) ? 0 : farthestIndex + 1;

        // Compute first possible end point's projection
        shape.getPoint(endPtIndex0, pt);
        final Vector2F endPt = new Vector2F(farthestPt);
        endPt.subtract(pt[0], pt[1]);
        endPt.normalize();
        final float endPtProj0 = Math.abs(endPt.dotProduct(sepAxis));

        // Compute second possible end point's projection
        shape.getPoint(endPtIndex1, pt);
        endPt.copy(farthestPt);
        endPt.subtract(pt[0], pt[1]);
        endPt.normalize();
        final float endPtProj1 = Math.abs(endPt.dotProduct(sepAxis));

        // Choose most perpendicular two vertices to use for colliding edge
        if (endPtProj0 < endPtProj1 || Point2F.isEqual(endPtProj0, endPtProj1)) {

            // Get first possible end pt again since current pt is second possible
            shape.getPoint(endPtIndex0, pt);
            endPt.set(pt[0], pt[1]);

            // Edge starts at end point and ends with farthest point
            edge.update(endPt, farthestPt, false);
        } else {

            shape.getPoint(endPtIndex1, pt);
            endPt.set(pt[0], pt[1]);

            // Edge starts at farthest point and ends with end point
            edge.update(farthestPt, endPt, true);
        }
    }

    boolean isSleeping()
    {
        return mSleeping;
    }

    void setSleeping(boolean enable)
    {
        mSleeping = enable;
    }

    /**
     * <p>Represents a colliding edge during contact point generation in
     * {@link #getContacts(List, List, BodyComponent, Vector2F)}.</p>
     */
    private class CollidingEdge
    {
        // Beginning vertex
        private Vector2F mBegin;

        // Ending vertex
        private Vector2F mEnd;

        // Whether the beginning vertex is the farthest along the separating axis
        private boolean mBeginFarthest = false;

        /**
         * <p>Sets the beginning and ending vertices.</p>
         *
         * <p>The farthest vertex is considered the vertex whose projected position is the farthest along the
         * separating axis when the axis points from the vertex's body to another.</p>
         *
         * @param begin beginning vertex.
         * @param end ending vertex.
         * @param beginIsFarthest whether beginning vertex is farthest along separating axis.
         */
        public void update(Vector2F begin, Vector2F end, boolean beginIsFarthest)
        {
            mBegin = begin;
            mEnd = end;
            mBeginFarthest = beginIsFarthest;
        }

        /**
         * <p>Gets the vertex whose projection along the separating axis is the farthest away from the
         * {@link BodyComponent}. This is either the beginning or ending vertex of the edge.</p>
         *
         * @return farthest vertex.
         */
        public Vector2F getFarthestVertex()
        {
            return (mBeginFarthest) ? mBegin : mEnd;
        }
    }

    /**
     * <p>
     *     Container for data about the collision of two {@link BodyComponent}s such as contact points and the
     *     collision normal.
     * </p>
     *
     * <p>
     *     The collision normal is a unit vector denoting direction from one BodyComponent to another. The
     *     BodyComponent the collision normal faces away from is always the "owning" body. Each contact point is
     *     two-dimensional and stored with the depth of penetration into the body.
     * </p>
     */
    public static final class Manifold
    {
        // Contact points
        private final List<Vector2F> mPoints = new ArrayList<Vector2F>();

        // Penetration depths for each contact point
        private final List<Float> mDepths = new ArrayList<Float>();

        // Contact normal
        private final Vector2F mNormal = new Vector2F();

        /**
         * <p>Gets the contact point at an index.</p>
         *
         * @param i index.
         * @return contact point.
         */
        public Vector2F getContactPointAt(int i)
        {
            return mPoints.get(i);
        }

        /**
         * <p>Sets the contact point at a specific index.</p>
         *
         * @param i index.
         * @param point contact point.
         * @param penetration penetration depth.
         */
        private void setContactPoint(int i, Vector2F point, float penetration)
        {
            mPoints.add(i, point);
            mDepths.add(i, penetration);
        }

        /**
         * <p>Gets the penetration depth of a contact point.</p>
         *
         * @param i contact point index.
         * @return penetration depth.
         */
        public float getPenetrationDepthAt(int i)
        {
            return mDepths.get(i);
        }

        /**
         * <p>Gets the number of contact points.</p>
         *
         * @return contact point count.
         */
        public int getContactPointCount()
        {
            return mPoints.size();
        }

        /**
         * <p>Copies the contact normal into a given vector.</p>
         *
         * @param container vector to hold collision normal.
         */
        public void getCollisionNormal(Vector2F container)
        {
            container.copy(mNormal);
        }

        /**
         * <p>Copies the stored contact normal to a vector. The contact normal will point in the direction from the
         * host {@link BodyComponent} to the BodyComponent it collided with.</p>
         *
         * @param normal collision normal.
         */
        public void setContactNormal(Vector2F normal)
        {
            mNormal.copy(normal);
            mNormal.normalize();
        }

        /**
         * <p>Copies the collision data from another Manifold.</p>
         *
         * @param manifold Manifold to copy.
         */
        public void copy(Manifold manifold)
        {
            // Replace points with given Manifold's
            mPoints.clear();
            mPoints.addAll(manifold.mPoints);

            // Replace penetration depths with given Manifold's
            mDepths.clear();
            mDepths.addAll(manifold.mDepths);

            // Replace collision normal
            mNormal.copy(manifold.mNormal);
        }

        /**
         * <p>Empties the Manifold of all collision information.</p>
         */
        public void clear()
        {
            mPoints.clear();
            mDepths.clear();
            mNormal.set(0f, 0f);
        }
    }
}
