package cinnamon.engine.event;

import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.event.Keyboard.Key;

/**
 * <p>{@code KeyEvent}s describe keyboard interaction and are composed of a {@link Key} and button state.</p>
 */
public final class KeyEvent extends InputEvent implements ButtonEvent
{
    // Keyboard key
    private final Key mKey;

    // True if event describes a press
    private final boolean mPressed;

    private final int mHash;

    /**
     * <p>Constructs a {@code KeyEvent} based off another.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param event to copy.
     * @throws NullPointerException if event is null.
     */
    public KeyEvent(long time, KeyEvent event)
    {
        super(time);

        mKey = event.getKey();
        mPressed = event.isPress();
        mHash = computeHash();
    }

    /**
     * <p>Constructs a {@code KeyEvent}.</p>
     *
     * @param time creation timestamp in nanoseconds.
     * @param key key.
     * @param press true if press.
     * @throws NullPointerException if key is null.
     */
    public KeyEvent(long time, Key key, boolean press)
    {
        super(time);

        if (key == null) {
            throw new NullPointerException();
        }

        mKey = key;
        mPressed = press;
        mHash = computeHash();
    }

    /**
     * <p>Gets the {@code Key}.</p>
     *
     * @return key.
     */
    public Key getKey()
    {
        return mKey;
    }

    /**
     * <p>Checks if the event belongs to the given {@link Key}.</p>
     *
     * @param key key.
     * @return true if the key matches.
     */
    public boolean isKey(Key key)
    {
        return mKey == key;
    }

    @Override
    public boolean isPress()
    {
        return mPressed;
    }

    @Override
    public boolean isRelease()
    {
        return !mPressed;
    }

    @Override
    public void accept(InputEventVisitor visitor)
    {
        visitor.visit(this);
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

        final KeyEvent event = (KeyEvent) obj;

        return event.getKey() == getKey() &&
                event.isPress() == isPress() &&
                event.getTime() == getTime();
    }

    @Override
    public String toString()
    {
        final String format = "%s(%s,%s)";
        
        final String name = getClass().getSimpleName();
        final String key = getKey().toString();
        final String pressed = (isPress()) ? "press" : "release";

        return String.format(format, name, key, pressed);
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    private int computeHash()
    {
        int hash = 17 * 31 + getKey().hashCode();
        hash = hash * 31 + Boolean.hashCode(isPress());
        return hash * 31 + Long.hashCode(getTime());
    }
}
