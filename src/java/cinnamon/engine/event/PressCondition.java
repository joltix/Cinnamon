package cinnamon.engine.event;

import cinnamon.engine.utils.Table;

/**
 * <p>This is used by the input devices {@link Keyboard}, {@link Mouse}, and {@link Gamepad} to normalize the decision
 * of determining if a button has been pressed based off event histories.</p>
 *
 * @param <T> input event type.
 */
final class PressCondition<T extends InputEvent>
{
    /**
     * <p>Checks if the latest event of an ordinal is a press or release.</p>
     *
     * @param value ordinal source.
     * @param pressHistory press event history.
     * @param releaseHistory release event history.
     * @return true if the most recent press event occurs after the most recent release.
     */
    public boolean isPressed(Enum value, Table<T> pressHistory, Table<T> releaseHistory)
    {
        assert (value != null);
        assert (pressHistory != null);
        assert (releaseHistory != null);

        final int ord = value.ordinal();
        final T press = pressHistory.get(0, ord);
        final T release = releaseHistory.get(0, ord);

        return press != null && (release == null || press.getTime() > release.getTime());
    }
}
