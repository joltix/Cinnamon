package cinnamon.engine.event;

/**
 * <p>{@code InputEvent}s describe interactions with the keyboard, mouse, and gamepad(s). These events generally
 * represent an action in the style of a button press or motion along some normalized axes.</p>
 *
 * <p><b>note</b> This class is not intended to be subclassed outside the framework.</p>
 */
public abstract class InputEvent extends Event implements InputEventVisitor.Visitable
{
    /**
     * <p>Constructs an {@code InputEvent}.</p>
     *
     * @param time creation timestamp.
     */
    protected InputEvent(long time)
    {
        super(time);
    }

    @Override
    public void accept(EventVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * <p>Implemented by {@code InputEvent}s that can represent a button's binary action.</p>
     *
     * <h3>Release detection</h3>
     * <p>Although an inverted {@link #isPress()} implies a release in the context of a button-only event,
     * {@link #isRelease()} is provided for those also implementing {@link AxisEvent}. For these hybrids, the
     * user only needs to use the given method when looking for a release. Otherwise, the user must also check for the
     * existence of offsets along both axes.</p>
     */
    interface ButtonEvent
    {
        /**
         * <p>Checks if the event describes a press.</p>
         *
         * @return true if a press.
         */
        boolean isPress();

        /**
         * <p>Checks if the event describes a release.</p>
         *
         * @return true if a release.
         */
        boolean isRelease();
    }

    /**
     * <p>Implemented by {@code InputEvent}s that can represent motion along the x and y axes.</p>
     */
    interface AxisEvent
    {
        /**
         * <p>Gets the offset along the horizontal axis. This offset is between -1 and 1, inclusive.</p>
         *
         * @return horizontal offset.
         */
        float getHorizontal();

        /**
         * <p>Gets the offset along the vertical axis. This offset is between -1 and 1, inclusive.</p>
         *
         * @return vertical offset.
         */
        float getVertical();
    }
}
