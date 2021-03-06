package com.cinnamon.system;

import com.cinnamon.utils.Point2F;
import org.lwjgl.glfw.GLFW;

/**
 * <p>
 *     MouseEvents represent {@link InputEvent}s generated by mouse interaction and are described by the
 *     {@link Button} that performed the {@link InputEvent.Action} as well as the screen coordinates of the cursor at
 *     the time of the action. Unlike {@link KeyEvent}s, MouseEvents support all Actions.
 * </p>
 *
 * <p>
 *     While the {@link Button#MIDDLE} Button supports {@link InputEvent.Action#PRESS} and
 *     {@link InputEvent.Action#RELEASE}, it is also capable of acting on {@link InputEvent.Action#SCROLL_FORWARD}
 *     and {@link InputEvent.Action#SCROLL_BACKWARD}. It is the only Button capable of doing so.
 * </p>
 */
public final class MouseEvent extends InputEvent
{
    /**
     * <p>Number of supported mouse buttons.</p>
     */
    public static final int SUPPORTED_BUTTONS = 3;

    /**
     * <p>
     *     Mouse button constants for describing {@link MouseEvent}s.
     * </p>
     */
    public enum Button
    {
        /**
         * <p>Left mouse button.</p>
         */
        LEFT,
        /**
         * <p>Right mouse button.</p>
         */
        RIGHT,
        /**
         * <p>Middle mouse button.</p>
         */
        MIDDLE
    }

    // Left, right, or middle button
    private Button mButton;

    // Screen space coordinates of event
    private final Point2F mPoint = new Point2F(0, 0);

    /**
     * <p>Constructs a MouseEvent described by a {@link Button}, the {@link Action} taken on the button, and the
     * position of
     * the {@link Event} on the screen.</p>
     *
     * @param button either {@link Button#LEFT}, {@link Button#RIGHT}, or {@link Button#MIDDLE}.
     * @param action Action such as {@link Action#PRESS}.
     * @param x x.
     * @param y y.
     */
    public MouseEvent(Button button, InputEvent.Action action, float x, float y)
    {
        super(action);
        mButton = button;
        mPoint.set(x, y);
    }

    /**
     * <p>Constructs a MouseEVent by copying another.</p>
     *
     * @param event MouseEvent to copy.
     */
    public MouseEvent(MouseEvent event)
    {
        super(event.getAction());
        mButton = event.mButton;
        mPoint.set(event.mPoint.getX(), event.mPoint.getY());
    }

    /**
     * <p>Changes the {@link Button}, {@link Action}, and (x,y) position.<p>
     *
     * <p>Timestamp is not copied but updated to the time of this method's execution.</p>
     *
     * @param button either {@link Button#LEFT}, {@link Button#RIGHT}, or {@link Button#MIDDLE}.
     * @param action either {@link Action#PRESS}, {@link Action#RELEASE},
     * {@link Action#REPEAT}, {@link Action#SCROLL_FORWARD}, or {@link Action#SCROLL_BACKWARD}.
     */
    public final void update(Button button, Action action, float x, float y)
    {
        mButton = button;
        setAction(action);
        mPoint.set(x, y);
        timestamp();
    }

    /**
     * <p>Gets the {@link Button} describing the MouseEvent.</p>
     *
     * @return either {@link Button#LEFT}, {@link Button#RIGHT}, or {@link Button#MIDDLE}.
     */
    public final Button getButton()
    {
        return mButton;
    }

    /**
     * <p>Checks whether or not the MouseEvent is described by a specified {@link Button}</p>
     *
     * @param button button constant such as {@link Button#LEFT}.
     * @return true if the button describes the MouseEvent.
     */
    public final boolean isButton(Button button)
    {
        return mButton == button;
    }

    /**
     * <p>Checks whether or not the MouseEvent is described by {@link Action#SCROLL_FORWARD}.</p>
     *
     * @return true if SCROLL_FORWARD event.
     */
    public final boolean isScrollForward()
    {
        return getAction() == Action.SCROLL_FORWARD;
    }

    /**
     * <p>Checks whether or not the MouseEvent is described by {@link Action#SCROLL_BACKWARD}.</p>
     *
     * @return true if SCROLL_BACKWARD event.
     */
    public final boolean isScrollBackward()
    {
        return getAction() == Action.SCROLL_BACKWARD;
    }

    /**
     * <p>Gets the x position in screen coordinates.</p>
     *
     * @return x.
     */
    public final float getX()
    {
        return mPoint.getX();
    }

    /**
     * <p>Gets the y position in screen coordinates.</p>
     *
     * @return y.
     */
    public final float getY()
    {
        return mPoint.getY();
    }

    @Override
    protected void handle(EventDispatcher distributor) {
        distributor.process(this);
    }

    /**
     * <p>Gets the {@link Button} form of a GLFW mouse button constant.</p>
     *
     * @param system system constant.
     * @return Button, or null if the system constant is unrecognized.
     */
    static final Button systemButtonsToButton(int system)
    {
        switch (system) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT:
                return Button.LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT:
                return Button.RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE:
                return Button.MIDDLE;
            default: return null;
        }
    }

    /**
     * <p>Gets the {@link Action} form of a GLFW mouse scroll given the vertical scrolling distance.</p>
     *
     * @param yDist distance scrolled.
     * @return either {@link Action#SCROLL_FORWARD} or {@link Action#SCROLL_BACKWARD}.
     */
    static final Action systemActionToAction(double yDist)
    {
        return (yDist < 0f) ? Action.SCROLL_BACKWARD : Action.SCROLL_FORWARD;
    }
}
