package cinnamon.engine.event;

import cinnamon.engine.utils.Table;

/**
 * <p>This class normalizes the decision of determining if a button has been pressed based off event histories.</p>
 */
final class PressChecker
{
    private PressChecker() { }

    /**
     * <p>Checks if a button's latest event is a press or release.</p>
     *
     * @param button button.
     * @param presses press history.
     * @param releases release history.
     * @return false if the most recent event describes a release or if the press history is empty.
     */
    public static <T extends InputEvent> boolean isPressed(Enum button, Table<T> presses, Table<T> releases)
    {
        final int ord = button.ordinal();
        final InputEvent press = presses.get(0, ord);
        final InputEvent release = releases.get(0, ord);

        return press != null && (release == null || press.getTime() > release.getTime());
    }
}
