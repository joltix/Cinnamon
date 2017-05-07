package com.cinnamon.object;

import com.cinnamon.utils.PooledQueue;

import java.util.*;

/**
 * <p>
 *     ContactGraph models the relationship a {@link Contact} represents between colliding {@link BodyComponent}s.
 * </p>
 * <p>
 *     This representation is formed as an undirected graph where each node represents a BodyComponent and each
 *     link is a Contact. The graph itself is meant to encompass the whole game area with all bodies added. Having
 *     bodies as nodes and Contacts as links leads to a graph whose links are constantly added and removed in
 *     accordance with the physics being applied to the bodies.
 * </p>
 * <br>
 * <p>
 *     When two bodies collide, determined from outside the ContactGraph, the collision should be reported with
 *     {@link #addContact(BodyComponent, BodyComponent)} and the returned Contact be used for storing implementation
 *     specific contact data. Specific Contacts can be removed with
 *     {@link #removeContact(BodyComponent, BodyComponent)} or a pass on all Contacts can be done with
 *     {@link #removeInvalidContacts()}, delegating the definition of an invalid Contact to overrides of
 *     {@link #isInvalid(Contact)}.
 * </p>
 * <br>
 * <h4>Sleeping components</h4>
 * <p>
 *     ContactGraph also offers a sleep optimization (also known as deactivation) for groups of bodies through the
 *     graph's connected components. All non-static bodies are arranged according to connected components and checked
 *     for optimization eligibility. Below is an example of handling this optimization.
 * </p>
 * <br>
 * <pre>
 *     {@code
 *
 *     // Update each component's sleeping
 *     for (int component = 0, count = graph.getComponentCount(); component < count; component++) {
 *
 *         // Sleep if allowed to
 *         if (graph.isSleepEligible(component)) {
 *             graph.sleep(component);
 *
 *         } else {
 *             // Wake since shouldn't be sleeping
 *             graph.wake(component);
 *         }
 *     }
 *     }
 * </pre>
 */
@SuppressWarnings("unchecked")
public abstract class ContactGraph<E extends ContactGraph.Contact>
{
    /**
     * Depth first search for sleep/waking entire graph
     */

    // Connections to track frames instead of recursion
    private final Stack<ContactGraph.Node> mDFSTrace = new Stack<ContactGraph.Node>();

    /**
     * General structure members; obj pools, size
     */

    // Obj pool to recycle Nodes
    private final PooledQueue<ContactGraph.Node> mNodePool = new PooledQueue<ContactGraph.Node>();

    // For resetting visit flags after sleep/wake traversal
    private final List<ContactGraph.Node> mNodesInUse = new ArrayList<ContactGraph.Node>();

    // True if a link or Node has been added or removed
    private boolean mGraphChanged = false;

    // Entry points into components; index = component
    private List<ContactGraph.Node> mComponents = new ArrayList<ContactGraph.Node>();

    // Whether or not a component can sleep
    private List<Boolean> mComponentSleepablility = new ArrayList<Boolean>();

    // Number of Nodes in use
    private int mSize = 0;

    /**
     * Contacts
     */

    // Lookup for all Contacts for a specific body
    private final List<List<E>> mContactsByBody;

    // Max number of supported bodies
    private final int mCapacity;

    // Global listing for iterating over all Contacts
    private final List<E> mGlobalContacts = new ArrayList<E>();

    // Obj pool to recycle Contacts
    private final PooledQueue<E> mContactPool = new PooledQueue<E>();

    /**
     * <p>Constructs a ContactGraph with an initial capacity for {@link BodyComponent}s.</p>
     *
     * @param capacity maximum number of bodies allowed at any given time.
     */
    public ContactGraph(int capacity)
    {
        mCapacity = capacity;

        // Instantiate Contact list lookup by body
        mContactsByBody = new ArrayList<List<E>>();

        // Create null entries in list
        fillContactLookup(capacity);
    }

    /**
     * <p>Adds null elements to the Contact lookup to "open up" indices for later get operations.</p>
     *
     * @param capacity maximum number of bodies allowed at any given time.
     */
    private void fillContactLookup(int capacity)
    {
        for (int i = 0; i < capacity; i++) {
            mContactsByBody.add(null);
        }
    }

    /**
     * <p>Adds a {@link BodyComponent} to the ContactGraph.</p>
     *
     * @param body body.
     * @return false if the body was not added because it already belongs to a ContactGraph (even if it's the same graph
     * as the one calling this method).
     * @throws IllegalStateException if the body capacity has already been reached and no more bodies can be added
     * without removing.
     */
    public final boolean add(BodyComponent body)
    {
        // Ensure only dealing with expected body count
        if (size() == getCapacity()) {
            throw new IllegalStateException("BodyComponent capacity has been reached: " + getCapacity());
        }

        final Node node = body.getContactNode();

        // Failed to add since already belongs to a graph
        if (node != null) {
            return false;
        }

        // Assoc body with node and update size
        final Node newNode = getNode();
        newNode.setBody(body);
        body.setContactNode(newNode);
        mSize++;

        // Notify connected components need updating
        mGraphChanged = true;
        return true;
    }

    /**
     * <p>Removes a {@link BodyComponent} from the graph.</p>
     *
     * <p>All {@link Contact}s between the body and its neighbors are removed as well.</p>
     *
     * @param body body.
     * @return false if body does not belong to the graph.
     */
    public final boolean remove(BodyComponent body)
    {
        // Remove fails if not in graph
        if (!contains(body)) {
            return false;
        }

        // Erase Contacts and place back in pool
        final List<E> contacts = getContactListOf(body);
        clearContactList(contacts);

        // Remove body's association with Node
        final ContactGraph.Node node = body.getContactNode();
        node.setBody(null);
        body.setContactNode(null);

        // Erase inbound and outbound links used in traversal
        clearLinks(body, node);

        // Update size and notify connected components need updating
        mSize--;
        mGraphChanged = true;
        return true;
    }

    /**
     * <p>Removes all inbound and outbound links between the {@link Node} and its neighbors and places the Node back
     * in the object pool. Afterwards, the BodyComponent and its Node can no longer be reached by traversal.</p>
     *
     * @param body body.
     * @param node body's Node.
     */
    private void clearLinks(BodyComponent body, Node node)
    {
        final List<ContactGraph.Node> connections = node.getLinks();

        // Erase all inbound links
        for (int i = 0, sz = connections.size(); i < sz; i++) {
            final ContactGraph.Node neighbor = connections.get(i);
            neighbor.getLinks().remove(node);
        }

        // Erase all outbound links and place node (and its list) back in pool
        connections.clear();
        mNodesInUse.remove(node);
        mNodePool.add(node);
    }

    /**
     * <p>Checks if a {@link BodyComponent} has {@link Contact} with any other body.</p>
     *
     * @param body body.
     *
     * @return true if colliding with at least one other body.
     */
    public final boolean hasContact(BodyComponent body)
    {
        final List<E> contacts = getContactListOf(body);
        return !contacts.isEmpty();
    }

    /**
     * <p>Gets the {@link Contact} between two {@link BodyComponent}s.</p>
     *
     * <p>If there is no Contact associated with the two bodies with the first argument matching the BodyComponent
     * returned by a Contact's {@link Contact#getBodyA()}, this method returns null.</p>
     *
     * @param bodyA body A.
     * @param bodyB body B.
     * @return Contact.
     */
    public final E getContact(BodyComponent bodyA, BodyComponent bodyB)
    {
        if (!contains(bodyA) || !contains(bodyB)) {
            throw new IllegalArgumentException("Both bodies should be in the graph");
        }

        final List<E> contacts = getContactListOf(bodyA);

        // Search and return a Contact whose body B matches the arg
        for (int i = 0, sz = contacts.size(); i < sz; i++) {
            final E con = contacts.get(i);
            if (con.getBodyB() == bodyB) {
                return con;
            }
        }

        // No Contact found with matching body B
        return null;
    }

    /**
     * <p>Creates a {@link Contact} between two {@link BodyComponent}s.</p>
     *
     * <p>The BodyComponent passed in as bodyA becomes the key in Contact lookup.</p>
     *
     * @param bodyA body A.
     * @param bodyB body B.
     * @return Contact.
     * @throws IllegalArgumentException if either body is null or does not exist in the graph.
     */
    public final E addContact(BodyComponent bodyA, BodyComponent bodyB)
    {
        // Check valid bodies
        if (bodyA == null || bodyB == null) {
            throw new IllegalArgumentException("Bodies may not be null");
        }

        // Check each body's already in the graph
        if (!contains(bodyA) || !contains(bodyB)) {
            throw new IllegalArgumentException("Bodies A and B must be in the graph to add Contact");
        }

        final ContactGraph.Node nodeA = bodyA.getContactNode();
        final ContactGraph.Node nodeB = bodyB.getContactNode();

        // Form mutual link between both nodes' bodies
        nodeA.getLinks().add(nodeB);
        nodeB.getLinks().add(nodeA);

        // Assoc bodies with a Contact
        final E contact = getContact();
        contact.setBodies(bodyA, bodyB);

        // Add Contact only to body A's list
        getContactListOf(bodyA).add(contact);

        // Add to global Contact listing
        mGlobalContacts.add(contact);

        mGraphChanged = true;
        return contact;
    }

    /**
     * <p>Removes the {@link Contact} between two {@link BodyComponent}s.</p>
     *
     * <p>The correct A-B body order must be specified in the arguments in order to match a Contact. I.e. if the
     * Contact to remove's body A is given as argument bodyB, this method will return false.</p>
     *
     * @param bodyA body A.
     * @param bodyB body B.
     * @return false if no Contact was found.
     */
    public final boolean removeContact(BodyComponent bodyA, BodyComponent bodyB)
    {
        // Make sure both bodies are in graph
        if (!contains(bodyA) || !contains(bodyB)) {
            throw new IllegalArgumentException("Bodies A and B must be in the graph");
        }

        return removeContact(bodyA, bodyB, true);
    }

    /**
     * <p>Removes a {@link Contact} between two {@link BodyComponent}s.</p>
     *
     * @param bodyA body A.
     * @param bodyB body B.
     * @param removeFromGlobal true to remove the Contact from lookup via body.
     * @return false if no Contact was found between body A and body B.
     */
    private boolean removeContact(BodyComponent bodyA, BodyComponent bodyB, boolean removeFromGlobal)
    {
        final List<E> contacts = getContactListOf(bodyA);

        // Remove Contact lookup with matching body B
        E contact = null;

        // Remove Contact whose body B matches
        for (int i = 0, sz = contacts.size(); i < sz; i++) {
            if (contacts.get(i).getBodyB() == bodyB) {

                // Clear and place back in obj pool
                final E con = contacts.remove(i);
                con.clear();
                mContactPool.add(con);
                contact = con;

                break;
            }
        }

        // Bail out if no Contact was found
        if (contact == null) {
            return false;

        } else if (removeFromGlobal) {
            // Remove Contact from global listing
            mGlobalContacts.remove(contact);
        }

        final ContactGraph.Node nodeA = bodyA.getContactNode();
        final ContactGraph.Node nodeB = bodyB.getContactNode();

        if (nodeA == null || nodeB == null) {
            System.out.printf("NULL node(s)\n");
        }

        // Remove traversal links
        nodeA.getLinks().remove(nodeB);
        nodeB.getLinks().remove(nodeA);

        // Flag structure changed; connected components are invalid
        mGraphChanged = true;
        return true;
    }

    /**
     * <p>Removes all {@link Contact}s in the graph where {@link #isInvalid(Contact)} is true.</p>
     */
    public final void removeInvalidContacts()
    {
        final Iterator<E> iter = mGlobalContacts.iterator();

        // Remove all Contacts considered invalid
        while (iter.hasNext()) {
            final E contact = iter.next();

            // Remove if invalid
            if (isInvalid(contact)) {
                iter.remove();

                // Remove all other references
                final boolean removed = removeContact(contact.getBodyA(), contact.getBodyB(), false);

                // Try removing from pov of body B if nothing was found for body A
                if (!removed) {
                    removeContact(contact.getBodyB(), contact.getBodyA(), false);
                }
            }
        }
    }

    /**
     * <p>Checks if a {@link Contact} is no longer valid.</p>
     *
     * <p>Default criteria for invalidity consists of at least one of the bodies being null.</p>
     *
     * @param contact Contact.
     * @return true if Contact should be removed.
     */
    protected boolean isInvalid(E contact)
    {
        return contact.getBodyA() == null || contact.getBodyB() == null;
    }

    /**
     * <p>Gets a {@link BodyComponent}'s list of {@link Contact}s.</p>
     *
     * @param body body.
     * @return list of Contacts.
     */
    private List<E> getContactListOf(BodyComponent body)
    {
        List<E> contacts = mContactsByBody.get(body.getId());

        // Create list for body if none
        if (contacts == null) {
            contacts = new ArrayList<E>();

            mContactsByBody.set(body.getId(), contacts);
        }

        return contacts;
    }

    /**
     * <p>Removes and places back in the pool all {@link Contact}s from a given list. The list is also sent back to
     * an object pool.</p>
     *
     * @param contacts list of Contacts.
     */
    private void clearContactList(List<E> contacts)
    {
        for (int i = 0, sz = contacts.size(); i < sz; i++) {

            // Clear Contact info and recycle to pool
            final E con = contacts.get(i);
            con.clear();
            mContactPool.add(con);

            // Remove from global list
            mGlobalContacts.remove(con);
        }

        // Clear list and set aside for reuse
        contacts.clear();
    }

    /**
     * <p>Gets a {@link Contact}.</p>
     *
     * <p>This method first attempts to reuse an old, unused Contact. If none are available, a new Contact is
     * instantiated.</p>
     *
     * @return an unused Contact.
     */
    private E getContact()
    {
        return (mContactPool.isEmpty()) ? createContact() : mContactPool.poll();
    }

    /**
     * <p>Instantiates a new {@link Contact}.</p>
     *
     * @return new Contact.
     */
    protected abstract E createContact();

    /**
     * <p>Gets the component of a {@link BodyComponent}.</p>
     *
     * @param body body.
     * @return component.
     */
    public final int getComponentOf(BodyComponent body)
    {
        // Check body's in graph
        if (!contains(body)) {
            throw new IllegalArgumentException("Body must be in graph");
        }

        // Ensure listed components are up-to-date
        updateComponents();

        return body.getContactNode().getComponent();
    }

    /**
     * <p>Gets the number of components.</p>
     *
     * @return component count.
     */
    public final int getComponentCount()
    {
        // Ensure listed components are up-to-date
        updateComponents();

        return mComponents.size();
    }

    /**
     * <p>Gets an {@link Iterable} of all {@link Contact}s.</p>
     *
     * @return all Contacts.
     */
    public final Iterable<E> getContacts()
    {
        return mGlobalContacts;
    }

    /**
     * <p>Gets the number of {@link Contact}s.</p>
     *
     * @return Contact count.
     */
    public final int getContactCount()
    {
        return mGlobalContacts.size();
    }

    /**
     * <p>Assigns all {@link Node}s a component index to differentiate those reachable from each other.</p>
     *
     * <p>If a Node A has the same component index as a Node B, then A is reachable from B and vice-versa.</p>
     */
    private void updateComponents()
    {
        // No need to traverse if structure hasn't changed
        if (!mGraphChanged) {
            return;
        }

        // Set each Node with "unassigned" component
        for (int i = 0, sz = mNodesInUse.size(); i < sz; i++) {
            mNodesInUse.get(i).setComponent(-1);
        }

        // Erase previous components' starting points, sleep eligibility, and sleep status
        mComponents.clear();
        mComponentSleepablility.clear();

        // Do a DFS on each Node, marking each as part of a component if unassigned
        for (int i = 0, cmpt = 0, sz = mNodesInUse.size(); i < sz; i++) {
            final ContactGraph.Node cursor = mNodesInUse.get(i);
            final int component = cursor.getComponent();

            // Skip those already assigned a component or static body
            if (component != -1 || cursor.getBody().isStatic()) {
                continue;
            }

            // Set as an entry point for the component
            mComponents.add(cursor);

            // Component can be put to sleep unless one node's found ineligible
            mComponentSleepablility.add(true);

            // Set component index of all Nodes reachable from cursor
            setComponentFrom(cursor, cmpt);

            // Set all Nodes' visit flags to false
            resetVisitsFrom(cursor);

            // Switch to new component index
            cmpt++;
        }
    }

    /**
     * <p>Performs a depth first search to assign a component index to all {@link Node}s reachable from a start Node
     * .</p>
     *
     * @param start starting point.
     * @param component component index.
     */
    private void setComponentFrom(Node start, int component)
    {
        mDFSTrace.add(start);

        // Traverse through all nodes reachable from start and mark them as component
        while (!mDFSTrace.isEmpty()) {
            final ContactGraph.Node cursor = mDFSTrace.pop();

            // Set component index and mark as visited
            cursor.setComponent(component);
            cursor.setVisited(true);

            // Flag entire component as not sleep eligible if at least one body is not
            if (!isEligibleForSleep(cursor.getBody())) {
                mComponentSleepablility.set(component, false);
            }

            final List<ContactGraph.Node> links = cursor.getLinks();

            // Add all unvisited neighbors to the stack to be visited
            for (int i = 0, sz = links.size(); i < sz; i++) {
                final ContactGraph.Node neighbor = links.get(i);

                // Skip unvisited and static bodies
                if (!neighbor.isVisited() && !neighbor.getBody().isStatic()) {
                    mDFSTrace.add(neighbor);
                }
            }
        }
    }

    /**
     * <p>Checks if a {@link BodyComponent can be put to sleep.</p>
     *
     * @param body body.
     * @return true if eligible for sleep.
     */
    protected abstract boolean isEligibleForSleep(BodyComponent body);

    /**
     * <p>Checks if a component is eligible for sleep.</p>
     *
     * @param component component.
     * @return true if component can be put to sleep.
     * @throws IllegalArgumentException if the component is < 0 or >= {@link #getComponentCount()}.
     */
    public final boolean isSleepEligible(int component)
    {
        checkComponent(component);

        // Make sure components are up-to-date
        updateComponents();

        return mComponentSleepablility.get(component);
    }

    /**
     * <p>Sleeps all {@link BodyComponent}s in the specified component.</p>
     *
     * @param component component.
     * @throws IllegalArgumentException if the component is < 0 or >= {@link #getComponentCount()}.</>
     */
    public final void sleep(int component)
    {
        // Check valid component
        checkComponent(component);

        // Ensure listed components are up-to-date
        updateComponents();

        // Put all bodies to sleep
        setSleepForAll(mComponents.get(component), true);
    }

    /**
     * <p>Wakes all {@link BodyComponent}s in the specified component.</p>
     *
     * @param component component.
     * @throws IllegalArgumentException if the component is < 0 or >= {@link #getComponentCount()}.</>
     */
    public final void wake(int component)
    {
        // Check valid component
        checkComponent(component);

        // Ensure listed components are up-to-date
        updateComponents();

        // Wake all bodies in component
        setSleepForAll(mComponents.get(component), false);
    }

    /**
     * <p>Throws an {@link IllegalArgumentException} if the given component is invalid.</p>
     *
     * @param component component.
     * @throws IllegalArgumentException if the component is < 0 or >= {@link #getComponentCount()}.
     */
    private void checkComponent(int component)
    {
        if (component < 0 || component >= mComponents.size()) {
            throw new IllegalArgumentException("Component must be >= 0 and < getComponentCount(): " + component);
        }
    }

    /**
     * <p>Performs a depth first search starting from a given {@link Node} and sets each visited Node's
     * {@link BodyComponent#setSleeping(boolean)} to true.</p>
     *
     * @param start DFS starting point.
     * @param sleep true to place all visited Nodes to sleep.
     */
    private void setSleepForAll(Node start, boolean sleep)
    {
        // Static bodies shouldn't connect components for sleep
        assert (!start.getBody().isStatic());

        mDFSTrace.push(start);

        // Traverse all nodes setting their bodies to sleep/wake
        while (!mDFSTrace.isEmpty()) {
            final Node cursor = mDFSTrace.pop();
            final List<ContactGraph.Node> connections = cursor.getLinks();

            // Reached leaf so skip
            if (connections.isEmpty()) {
                continue;
            }

            // Add to the stack all nodes to be visited
            for (int i = 0, sz = connections.size(); i < sz; i++) {
                final ContactGraph.Node toVisit = connections.get(i);

                // Only add unvisited to prevent cycling
                if (!toVisit.isVisited() && !toVisit.getBody().isStatic()) {
                    mDFSTrace.push(connections.get(i));
                }
            }

            // Apply sleep or wake and flag node as visited
            final BodyComponent body = cursor.getBody();
            cursor.setVisited(true);
            body.setSleeping(sleep);

            // Allow subclass ops
            onSleep(body);
        }

        // Set all visit flags back to false
        resetVisitsFrom(start);
    }

    /**
     * <p>This method is called during {@link #sleep(int)} each time a {@link BodyComponent} in the targeted
     * component has been set as sleeping.</p>
     *
     * @param body sleeping body.
     */
    protected abstract void onSleep(BodyComponent body);

    /**
     * <p>Performs a depth first search from a given {@link Node} and sets all reachable Nodes (including the
     * starting Node) as unvisited.</p>
     *
     * @param start starting Node.
     */
    private void resetVisitsFrom(Node start)
    {
        mDFSTrace.add(start);

        while (!mDFSTrace.isEmpty()) {
            final ContactGraph.Node cursor = mDFSTrace.pop();

            // Reset Node as unvisited
            cursor.setVisited(false);

            final List<ContactGraph.Node> links = cursor.getLinks();

            // Add all visited Neighbors to the stack
            for (int i = 0, sz = links.size(); i < sz; i++) {
                final ContactGraph.Node neighbor = links.get(i);

                if (neighbor.isVisited()) {
                    mDFSTrace.add(neighbor);
                }
            }
        }
    }

    /**
     * <p>Gets a {@link Node}.</p>
     *
     * <p>This method first attempts to reuse an old, unused Node. If none are available, a new Node is instantiated
     * . The Node being returned is also added to the list of Nodes in use.</p>
     *
     * @return an unused Node.
     */
    private Node getNode()
    {
        // Reuse old Node or create new if no old
        final Node node = (mNodePool.isEmpty()) ? new Node() : mNodePool.poll();

        // Add to list of those in use
        mNodesInUse.add(node);
        return node;
    }

    /**
     * <p>Checks if a {@link BodyComponent} is in the ContactGraph.</p>
     *
     * @param body body.
     * @return true if in graph.
     */
    public final boolean contains(BodyComponent body)
    {
        final ContactGraph.Node node = body.getContactNode();
        return node != null && node.getGraph() == this;
    }

    /**
     * <p>Checks if the ContactGraph has no {@link BodyComponent}s.</p>
     *
     * @return true if there are no bodies.
     */
    public final boolean isEmpty()
    {
        return mSize == 0;
    }

    /**
     * <p>Gets the number of {@link BodyComponent}s.</p>
     *
     * @return body count.
     */
    public final int size()
    {
        return mSize;
    }

    /**
     * <p>Gets the number of {@link BodyComponent}s the ContactGraph can support.</p>
     *
     * @return maximum number of added bodies.
     */
    public final int getCapacity()
    {
        return mCapacity;
    }

    /**
     * <p>
     *     Represents a {@link BodyComponent} in the ContactGraph.
     * </p>
     * <p>
     *     Each Node contains references to its neighbors, each of which represent a colliding BodyComponent.
     * </p>
     */
    final class Node
    {
        // Representative body
        private BodyComponent mBody;

        // Colliding bodies
        private List<ContactGraph.Node> mLinks = new ArrayList<ContactGraph.Node>();

        // Component index
        private int mComponent;

        // True if already visited during sleep/wake; reset to false afterwards
        private boolean mVisited = false;

        /**
         * <p>Gets the {@link BodyComponent}.</p>
         *
         * @return body.
         */
        private BodyComponent getBody()
        {
            return mBody;
        }

        /**
         * <p>Sets the {@link BodyComponent}.</p>
         *
         * @param body body.
         */
        private void setBody(BodyComponent body)
        {
            mBody = body;
        }

        /**
         * <p>Gets a list of neighboring Nodes.</p>
         *
         * @return neighboring Nodes.
         */
        private List<ContactGraph.Node> getLinks()
        {
            return mLinks;
        }

        /**
         * <p>Gets the {@link ContactGraph} housing the Node instance.</p>
         *
         * @return graph.
         */
        private ContactGraph getGraph()
        {
            return ContactGraph.this;
        }

        /**
         * <p>Checks if the Node has been visited during a traversal.</p>
         *
         * @return true if visited.
         */
        private boolean isVisited()
        {
            return mVisited;
        }

        /**
         * <p>Sets whether or not the Node has been visited during a traversal.</p>
         *
         * @param visited true to flag as visited.
         */
        private void setVisited(boolean visited)
        {
            mVisited = visited;
        }

        /**
         * <p>Gets the component the Node belongs to.</p>
         *
         * @return component.
         */
        private int getComponent()
        {
            return mComponent;
        }

        /**
         * <p>Sets the component.</p>
         *
         * @param component component.
         */
        private void setComponent(int component)
        {
            mComponent = component;
        }
    }

    /**
     * <p>
     *     Represents contact between two {@link BodyComponent}s.
     * </p>
     * <p>
     *     A Contact carries not only the colliding bodies but also the {@link BodyComponent.Manifold} describing the
     *     collision. A flag is provided through {@link #isHandled()} and its related methods to mark whether or not a
     *     Contact has been processed during a frame.
     * </p>
     * <p>
     *     The persistence of a Contact, i.e. number of frames it has existed through, can be retrieved with
     *     {@link #getPersistence()} and requires {@link #clearHandle()} ()} to be called once at the end of every
     *     frame. Calling handle more than once in a frame or not calling it at all will result in inaccurate
     *     persistence. Furthermore, clearing a Contact's handle flag at the end of a frame or beginning of the next
     *     provides a clearer representation of a Contact's frame persistence as the frame is not counted until it is
     *     considered complete.
     * </p>
     */
    public static abstract class Contact
    {
        // Collision data
        private final BodyComponent.Manifold mManifold = new BodyComponent.Manifold();

        // Body A
        private BodyComponent mBodyA;

        // Body B
        private BodyComponent mBodyB;

        // Number of frames since
        private int mPersistence = 0;

        // True if processed during current update
        private boolean mHandled = false;

        /**
         * <p>Gets the body pair's collision data.</p>
         *
         * @return collision data.
         */
        public final BodyComponent.Manifold getManifold()
        {
            return mManifold;
        }

        /**
         * <p>Sets the collision data between the Contact's body pair.</p>
         *
         * @param manifold collision data.
         */
        public final void setManifold(BodyComponent.Manifold manifold)
        {
            mManifold.copy(manifold);
        }

        /**
         * <p>Gets {@link BodyComponent} A.</p>
         *
         * @return body A.
         */
        public final BodyComponent getBodyA()
        {
            return mBodyA;
        }

        /**
         * <p>Gets {@link BodyComponent} B.</p>
         *
         * @return body B.
         */
        public final BodyComponent getBodyB()
        {
            return mBodyB;
        }

        /**
         * <p>Sets {@link BodyComponent}s A and B.</p>
         *
         * @param bodyA body A.
         * @param bodyB body B.
         */
        public final void setBodies(BodyComponent bodyA, BodyComponent bodyB)
        {
            mBodyA = bodyA;
            mBodyB = bodyB;

            // Let subclasses initialize their data
            onBodiesSet(mBodyA, mBodyB);
        }

        /**
         * <p>This method is called after {@link #setBodies(BodyComponent, BodyComponent)} and allows subclasses to
         * perform one-time operations when the Contact's body pair has been assigned.</p>
         *
         * @param bodyA body A.
         * @param bodyB body B.
         */
        protected abstract void onBodiesSet(BodyComponent bodyA, BodyComponent bodyB);

        /**
         * <p>Checks if {@link #handle()} has been called.</p>
         *
         * @return true if handled.
         */
        public final boolean isHandled()
        {
            return mHandled;
        }

        /**
         * <p>Marks the Contact as handled.</p>
         *
         * <p>This method should be called once per frame.</p>
         */
        public final void handle()
        {
            mHandled = true;
        }

        /**
         * <p>Removes the handled flag.</p>
         *
         * <p>This method should be called once per frame.</p>
         */
        public final void clearHandle()
        {
            mHandled = false;

            // Count update
            mPersistence++;
        }

        /**
         * <p>Gets the number of frames in which the Contact has been valid.</p>
         *
         * @return number of frames since first use.
         */
        public final int getPersistence()
        {
            return mPersistence;
        }

        /**
         * <p>Erases all contact data including the handle flag and frame persistence.</p>
         */
        public final void clear()
        {
            // Clear body pair
            mBodyA = null;
            mBodyB = null;

            // Clear collision data
            mManifold.clear();

            // Reset handle and frame count
            mHandled = false;
            mPersistence = 0;

            // Allow subclasses to clear their data
            onClear();
        }

        /**
         * <p>This method is called after {@link #clear()} and allows subclasses to remove their data.</p>
         */
        protected abstract void onClear();
    }
}
