package cinnamon.engine.event;

/**
 * <p>Routes {@code InputEvent}s for appropriate handling.</p>
 *
 * <p>This visitor is used to identify and separate {@code InputEvent}s. Unlike {@link EventVisitor}, there is no
 * support for custom input events as {@code InputEvent} is not intended for external overrides.</p>
 * <pre>
 *     <code>
 *         InputEventVisitor visitor = new InputEventVisitor()
 *         {
 *            {@literal @Override}
 *             public void visit(KeyEvent event)
 *             {
 *                 // This is called
 *             }
 *
 *            {@literal @Override}
 *             public void visit(MouseEvent event) { }
 *
 *            {@literal @Override}
 *             public void visit(PadEvent event) { }
 *         };
 *
 *         final InputEvent event = new KeyEvent(0L, Key.KEY_ENTER, true);
 *
 *         // Have the visitor figure the type
 *         event.accept(visitor);
 *     </code>
 * </pre>
 */
public interface InputEventVisitor
{
    /**
     * <p>Called when {@link KeyEvent#accept(InputEventVisitor)} is called with the visitor.</p>
     *
     * @param event calling event.
     * @throws NullPointerException if event is null.
     */
    void visit(KeyEvent event);

    /**
     * <p>Called when {@link MouseEvent#accept(InputEventVisitor)} is called with the visitor.</p>
     *
     * @param event calling event.
     * @throws NullPointerException if event is null.
     */
    void visit(MouseEvent event);

    /**
     * <p>Called when {@link PadEvent#accept(InputEventVisitor)} is called with the visitor.</p>
     *
     * @param event calling event.
     * @throws NullPointerException if event is null.
     */
    void visit(PadEvent event);

    /**
     * <p>This interface is implemented by {@code InputEvent}s and marks them as available for distribution by an
     * {@code InputEventVisitor}.</p>
     */
    interface Visitable
    {
        /**
         * <p>Called by the given visitor.</p>
         *
         * @param visitor visitor.
         * @throws NullPointerException if visitor is null.
         */
        void accept(InputEventVisitor visitor);
    }
}
