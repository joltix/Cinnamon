package cinnamon.engine.event;

/**
 * <p>Routes events for appropriate handling.</p>
 *
 * <p>User-created event subclasses must implement {@code EventVisitor.Visitable} in the following manner.</p>
 * <pre>
 *     <code>
 *        {@literal @Override}
 *         public void accept(EventVisitor visitor)
 *         {
 *             visitor.visit(this);
 *         }
 *     </code>
 * </pre>
 *
 * <p>The {@code EventVisitor} can then be used as follows.</p>
 * <pre>
 *     <code>
 *         EventVisitor visitor = new EventVisitor()
 *         {
 *            {@literal @Override}
 *             public void visit(InputEvent event) { }
 *
 *            {@literal @Override}
 *             public void visit(Event event)
 *             {
 *                 // Handle CustomEvent here
 *             }
 *         };
 *
 *         new CustomEvent().accept(visitor);
 *     </code>
 * </pre>
 */
public interface EventVisitor
{
    /**
     * <p>Called when {@link InputEvent#accept(InputEventVisitor)} is called with the visitor.</p>
     *
     * @param event calling event.
     * @throws NullPointerException if event is null.
     */
    void visit(InputEvent event);

    /**
     * <p>Called when {@link Event#accept(EventVisitor)} is called with the visitor.</p>
     *
     * @param event calling event.
     * @throws NullPointerException if event is null.
     */
    void visit(Event event);

    /**
     * <p>This interface is implemented by {@code Event}s and marks them as available for distribution by an
     * {@code EventVisitor}.</p>
     */
    interface Visitable
    {
        /**
         * <p>Called by the given visitor.</p>
         *
         * @param visitor visitor.
         * @throws NullPointerException if visitor is null.
         */
        void accept(EventVisitor visitor);
    }
}
