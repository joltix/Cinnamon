package cinnamon.engine.event;

import cinnamon.engine.event.ButtonPreferences.Style;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Set of instructions to be executed in reaction to a button. Each {@code ButtonRule} consists of an
 * {@link EventListener} to execute, {@link ButtonPreferences} to specify its requirements and details, and the
 * constants for which the reaction belongs. A {@code ButtonRule}'s components cannot be changed.</p>
 *
 * @param <K> constant type owning the reaction.
 * @param <T> event type to react to.
 */
public final class ButtonRule<K extends Enum<K>, T extends InputEvent> implements InputRule<K, T, ButtonPreferences>
{
    // Actual reaction
    private final EventListener<T> mListener;

    // Requirements
    private final ButtonPreferences mPrefs;

    // Constants
    private final K[] mButtons;

    private final int mHash;

    /**
     * <p>Constructs a {@code ButtonRule}.</p>
     *
     * @param button button.
     * @param listener listener.
     * @param preferences preferences.
     * @throws NullPointerException if button, listener, or preferences is null.
     */
    @SuppressWarnings("unchecked")
    public ButtonRule(K button, EventListener<T> listener, ButtonPreferences preferences)
    {
        checkNull(button);
        checkNull(listener);
        checkNull(preferences);

        checkCompatibility(preferences, 1);

        mListener = listener;
        mPrefs = preferences;
        mButtons = (K[]) new Enum[] {button};

        mHash = computeHash();
    }

    /**
     * <p>Constructs a {@code ButtonRule} for an array of buttons.</p>
     *
     * @param buttons buttons.
     * @param listener listener.
     * @param preferences preferences.
     * @throws NullPointerException if buttons, listener, or preferences is null.
     * @throws IllegalArgumentException if buttons has all null elements or preferences can only work with a single
     * button while more than one button is provided.
     */
    @SuppressWarnings("unchecked")
    public ButtonRule(K[] buttons, EventListener<T> listener, ButtonPreferences preferences)
    {
        checkNull(buttons);
        checkNull(listener);
        checkNull(preferences);

        int buttonCount = 0;

        // Count number of buttons
        for (final K button : buttons) {
            buttonCount += (button != null) ? 1 : 0;
        }

        if (buttonCount == 0) {
            throw new IllegalArgumentException("Buttons array is empty");
        }

        checkCompatibility(preferences, buttonCount);

        mListener = listener;
        mPrefs = preferences;
        mButtons = (K[]) new Enum[buttonCount];

        // Ensure buttons are packed
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] != null) {
                mButtons[i] = buttons[i];
            }
        }

        mHash = computeHash();
    }

    @Override
    public List<K> getConstants()
    {
        return Collections.unmodifiableList(Arrays.asList(mButtons));
    }

    @Override
    public EventListener<T> getListener()
    {
        return mListener;
    }

    @Override
    public ButtonPreferences getPreferences()
    {
        return mPrefs;
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

        final ButtonRule other = (ButtonRule) obj;

        return getListener() == other.getListener()
                && getPreferences() == other.getPreferences()
                && getConstants().equals(other.getConstants());
    }

    @Override
    protected Object clone() throws CloneNotSupportedException
    {
        throw new CloneNotSupportedException();
    }

    /**
     * <p>Throws an {@code IllegalArgumentException} if {@code preferences} are incompatible with the given number
     * of buttons.</p>
     *
     * @param preferences preferences.
     * @param count number of buttons.
     */
    private void checkCompatibility(ButtonPreferences preferences, int count)
    {
        if (preferences.getStyle() == Style.DOUBLE) {
            if (count > 1) {
                throw new IllegalArgumentException("Preferences target a double-click but more than one button is " +
                        "targeted");
            }

        } else if (preferences.getTolerance() > 0L && count == 1) {
            throw new IllegalArgumentException("Preferences intend on targeting multiple buttons but only one button " +
                    "was provided");
        }
    }

    private int computeHash()
    {
        int hash = 17 * 31 + mListener.hashCode();
        hash = 31 * hash + mPrefs.hashCode();
        return 31 * hash + Arrays.hashCode(mButtons);
    }

    private void checkNull(Object object)
    {
        if (object == null) {
            throw new NullPointerException();
        }
    }
}
