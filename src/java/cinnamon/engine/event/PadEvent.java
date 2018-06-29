package cinnamon.engine.event;

import cinnamon.engine.event.Gamepad.AxisWrapper;
import cinnamon.engine.event.Gamepad.ButtonWrapper;
import cinnamon.engine.event.Gamepad.Connection;
import cinnamon.engine.event.InputEvent.AxisEvent;
import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.utils.Point;

/**
 * <p>{@code PadEvent}s describe gamepad interaction and are composed of a wrapper constant and either a button state
 * or motion, depending on whether the wrapper is a {@link ButtonWrapper} or {@link AxisWrapper}.</p>
 */
public final class PadEvent extends InputEvent implements ButtonEvent, AxisEvent
{
    // Identifier for gamepad that created event
    private final Connection mSource;

    // Button alias
    private final ButtonWrapper mButton;

    // True if event describes a press
    private final boolean mPressed;

    // Axis alias
    private final AxisWrapper mAxis;

    // Horizontal offset
    private final float mHorizontal;

    // Vertical offset
    private final float mVertical;

    private final int mHash;

    /**
     * <p>Constructs a {@code PadEvent} based off another.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param event to copy.
     * @throws NullPointerException if event is null.
     */
    public PadEvent(long time, PadEvent event)
    {
        super(time);

        mSource = event.getSource();

        mButton = event.getButton();
        mPressed = event.isPress();

        mAxis = event.getAxis();
        mVertical = event.getVertical();
        mHorizontal = event.getHorizontal();

        mHash = event.hashCode();
    }

    /**
     * <p>Constructs a {@code PadEvent} for either a press or release.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param source originating gamepad.
     * @param button button.
     * @param press true if press.
     * @throws NullPointerException if source or button is null.
     * @throws IllegalArgumentException if {@code button.getButton()} is null.
     */
    public PadEvent(long time, Connection source, ButtonWrapper button, boolean press)
    {
        super(time);

        checkNull(source);
        checkNull(button);
        ButtonWrapper.checkWrapper(button);

        mSource = source;

        mButton = button;
        mPressed = press;

        mAxis = null;
        mHorizontal = 0f;
        mVertical = 0f;

        mHash = computeHash();
    }

    /**
     * <p>Constructs a {@code PadEvent} for motion along axes.</p>
     *
     * @param time creation timestamp.
     * @param source originating gamepad.
     * @param axis axis.
     * @param offsets motion offsets.
     * @throws NullPointerException if source, axis, or offsets is null.
     * @throws IllegalArgumentException if {@code axis.vertical()} is null.
     */
    public PadEvent(long time, Connection source, AxisWrapper axis, Point offsets)
    {
        super(time);

        checkNull(source);
        checkNull(axis);
        checkNull(offsets);
        AxisWrapper.checkWrapper(axis);

        mSource = source;

        mButton = null;
        mPressed = false;

        mAxis = axis;
        mHorizontal = offsets.getX();
        mVertical = offsets.getY();

        mHash = computeHash();
    }

    /**
     * <p>Gets the button.</p>
     *
     * @return button or null if event belongs to an axis.
     */
    public ButtonWrapper getButton()
    {
        return mButton;
    }

    /**
     * <p>Checks if the event belongs to a {@code ButtonWrapper}.</p>
     *
     * @return true if the event belongs to a button, false if an axis.
     */
    public boolean isButton()
    {
        return mButton != null;
    }

    /**
     * <p>Checks if the event belongs to the given {@code ButtonWrapper}.</p>
     *
     * @param button button.
     * @param <T> button type.
     * @return true if the button matches.
     * @throws NullPointerException if button is null.
     */
    public <T extends Enum<T> & ButtonWrapper> boolean isButton(T button)
    {
        return button != null && mButton == button;
    }

    /**
     * <p>Gets the {@code Gamepad.Axis}.</p>
     *
     * @return axis or null if event belongs to a button.
     */
    public AxisWrapper getAxis()
    {
        return mAxis;
    }

    /**
     * <p>Checks if the event belongs to the given {@code AxisWrapper}.</p>
     *
     * @param axis axis.
     * @param <K> axis type.
     * @return true if the axis matches.
     * @throws NullPointerException if axis is null.
     */
    public <K extends Enum<K> & AxisWrapper> boolean isAxis(K axis)
    {
        return axis != null && mAxis == axis;
    }

    /**
     * <p>Gets the {@code Connection} signifying the {@code Gamepad} that caused the event.</p>
     *
     * @return source connection.
     */
    public Connection getSource()
    {
        return mSource;
    }

    @Override
    public float getVertical()
    {
        return mVertical;
    }

    @Override
    public float getHorizontal()
    {
        return mHorizontal;
    }

    @Override
    public boolean isPress()
    {
        return mPressed;
    }

    @Override
    public boolean isRelease()
    {
        return mButton != null && !mPressed;
    }

    @Override
    public void accept(InputEventVisitor visitor)
    {
        visitor.visit(this);
    }

    @Override
    public String toString()
    {
        final String format = "%s(%s,%s)";
        final String name = getClass().getSimpleName();

        final String wrapper;
        final String action;

        if (isButton()) {
            wrapper = getButton().toString();
            action = (isPress()) ? "press" : "release";
        } else {
            wrapper = getAxis().toString();
            action = String.format("motion(%.2f,%.2f)", getHorizontal(), getVertical());
        }

        return String.format(format, name, wrapper, action);
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

        final PadEvent event = (PadEvent) obj;

        return event.getSource() == getSource() &&
                event.getButton() == getButton() &&
                event.isPress() == isPress() &&
                event.getAxis() == getAxis() &&
                event.getHorizontal() == getHorizontal() && event.getVertical() == getVertical() &&
                event.getTime() == getTime();
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private int computeHash()
    {
        int hash = 17 * 31 + getSource().hashCode();

        hash = hash * 31 + ((getButton() == null) ? 0 : getButton().hashCode());
        hash = hash * 31 + Boolean.hashCode(isPress());

        hash = hash * 31 + ((getAxis() == null) ? 0 : getAxis().hashCode());
        hash = hash * 31 + Float.hashCode(getVertical());
        hash = hash * 31 + Float.hashCode(getHorizontal());

        return hash * 31 + Long.hashCode(getTime());
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
