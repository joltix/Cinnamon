package cinnamon.engine.event;

import cinnamon.engine.event.InputEvent.AxisEvent;
import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.event.Mouse.Button;
import cinnamon.engine.utils.Point;

/**
 * <p>{@code MouseEvent}s describe mouse interaction and are composed of a {@link Button} and either a button state
 * or scrolling motion (scrolling is only allowed with {@code Button#MIDDLE}). In addition, all {@code MouseEvent}s
 * carry the screen position of its action.</p>
 */
public final class MouseEvent extends InputEvent implements ButtonEvent, AxisEvent
{
    // Mouse button
    private final Button mButton;

    // True if event describes a press
    private final boolean mPressed;

    // Horizontal offset
    private final float mHorizontalScroll;

    // Vertical offset
    private final float mVerticalScroll;

    // Screen space coordinates
    private final Point mPoint = new Point();

    private final int mHash;

    /**
     * <p>Constructs a {@code MouseEvent} based off another.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param event to copy.
     * @throws NullPointerException if event is null.
     */
    public MouseEvent(long time, MouseEvent event)
    {
        super(time);

        mButton = event.getButton();
        mPressed = event.isPress();

        mHorizontalScroll = event.getHorizontal();
        mVerticalScroll = event.getVertical();

        mPoint.setX(event.getX());
        mPoint.setY(event.getY());

        mHash = event.hashCode();
    }

    /**
     * <p>Constructs a {@code MouseEvent} for either a press or release.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param position position on screen.
     * @param button button.
     * @param press true if press.
     * @throws NullPointerException if either position or button is null.
     */
    public MouseEvent(long time, Point position, Button button, boolean press)
    {
        super(time);

        checkNull(button);
        checkNull(position);

        mButton = button;
        mPressed = press;

        mHorizontalScroll = 0f;
        mVerticalScroll = 0f;

        mPoint.copy(position);

        mHash = computeHash();
    }

    /**
     * <p>Constructs a {@code MouseEvent} for a scroll. The event's {@code Button} will be {@code MIDDLE}.</p>
     *
     * @param time creation timestamp.
     * @param position position on screen.
     * @param offsets scroll offsets.
     * @throws NullPointerException if either position or offsets is null.
     */
    public MouseEvent(long time, Point position, Point offsets)
    {
        super(time);

        checkNull(offsets);
        checkNull(position);

        mButton = Button.MIDDLE;
        mPressed = false;

        mHorizontalScroll = clampOffset(offsets.getX());
        mVerticalScroll = clampOffset(offsets.getY());

        mPoint.copy(position);

        mHash = computeHash();
    }

    /**
     * <p>Gets the {@code Mouse.Button}.</p>
     *
     * @return button.
     */
    public Button getButton()
    {
        return mButton;
    }

    /**
     * <p>Checks if the event belongs to the given {@link Mouse.Button}.</p>
     *
     * @param button button.
     * @return true if the button matches.
     */
    public boolean isButton(Button button)
    {
        return button != null && mButton == button;
    }

    @Override
    public float getVertical()
    {
        return mVerticalScroll;
    }

    @Override
    public float getHorizontal()
    {
        return mHorizontalScroll;
    }

    /**
     * <p>Checks if the event describes scrolling behavior.</p>
     *
     * @return true if scrolling.
     */
    public boolean isScroll()
    {
        return mHorizontalScroll != 0f || mVerticalScroll != 0f;
    }

    /**
     * <p>Checks if the event describes a forward scroll.</p>
     *
     * @return true if scrolling forward.
     */
    public boolean isScrollForward()
    {
        return mVerticalScroll > 0f;
    }

    /**
     * <p>Checks if the event describes a backward scroll.</p>
     *
     * @return true if scrolling backward.
     */
    public boolean isScrollBackward()
    {
        return mVerticalScroll < 0f;
    }

    /**
     * <p>Checks if the event describes a left scroll.</p>
     *
     * @return true if scrolling left.
     */
    public boolean isScrollLeft()
    {
        return mHorizontalScroll > 0f;
    }

    /**
     * <p>Checks if the event describes a right scroll.</p>
     *
     * @return true if scrolling right.
     */
    public boolean isScrollRight()
    {
        return mHorizontalScroll < 0f;
    }

    /**
     * <p>Gets the x position in screen space.</p>
     *
     * @return x.
     */
    public float getX()
    {
        return mPoint.getX();
    }

    /**
     * <p>Gets the y position in screen space.</p>
     *
     * @return y.
     */
    public float getY()
    {
        return mPoint.getY();
    }

    @Override
    public boolean isPress()
    {
        return mPressed;
    }

    @Override
    public boolean isRelease()
    {
        return !mPressed && !isScroll();
    }

    @Override
    public void accept(InputEventVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public String toString()
    {
        final String format = "%s(@(%.2f,%.2f),%s,%s)";
        final String name = getClass().getSimpleName();
        final String button = getButton().toString();

        final String action;
        if (isScroll()) {
            action = String.format("scroll(%.2f,%.2f)", getHorizontal(), getVertical());
        } else {
            action = (isPress()) ? "press" : "release";
        }

        return String.format(format, name, getX(), getY(), button, action);
    }

    @Override
    public int hashCode()
    {
        return mHash;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        } else if (obj == this) {
            return true;
        }

        final MouseEvent event = (MouseEvent) obj;

        return event.getButton() == getButton() && event.isPress() == isPress() &&
                event.getHorizontal() == getHorizontal() && event.getVertical() == getVertical() &&
                event.getX() == getX() && event.getY() == getY() &&
                event.getTime() == getTime();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private float clampOffset(float offset)
    {
        return Math.max(Math.min(offset, 1f), -1f);
    }

    private int computeHash()
    {
        int hash = 17 * 31 + getButton().hashCode();
        hash = hash * 31 + Boolean.hashCode(isPress());

        hash = hash * 31 + Float.hashCode(getHorizontal());
        hash = hash * 31 + Float.hashCode(getVertical());

        hash = hash * 31 + Float.hashCode(getX());
        hash = hash * 31 + Float.hashCode(getY());

        return hash * 31 + Long.hashCode(getTime());
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
