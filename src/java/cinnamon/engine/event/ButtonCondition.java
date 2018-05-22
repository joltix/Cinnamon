package cinnamon.engine.event;

import cinnamon.engine.event.MappingsHandler.Executable;
import cinnamon.engine.utils.Table;

/**
 * <p>Offers the history checking algorithms for buttons pressing, releasing, and double clicking.</p>
 */
interface ButtonCondition
{
    ButtonCondition PRESS_CONDITION = (handler, constant, exe) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (exe != null);

        final Enum[] buttons = exe.getConstants();

        @SuppressWarnings("unchecked")
        final Table<InputEvent> pressHistory = handler.getPressHistory();
        @SuppressWarnings("unchecked")
        final Table<InputEvent> releaseHistory = handler.getReleaseHistory();

        boolean ordered = true;
        long prereqButtonTime = -Long.MAX_VALUE;

        long earliest = Long.MAX_VALUE;
        long latest = -Long.MAX_VALUE;

        // Measure latest and earliest press events among the button(s)
        for (final Enum button : buttons) {

            final int ord = button.ordinal();
            final InputEvent press = pressHistory.get(0, ord);
            final InputEvent release = releaseHistory.get(0, ord);
            final long pressTime = press.getTime();

            // Presses should come after latest releases
            ordered = pressTime >= release.getTime() && ordered;

            // Honor press sequence
            ordered = ordered && prereqButtonTime <= pressTime;

            earliest = (pressTime < earliest) ? pressTime : earliest;
            latest = (pressTime > latest) ? pressTime : latest;
            prereqButtonTime = pressTime;
        }

        // Presses must be close enough together
        return ordered && latest - earliest <= exe.getPreferences().getTolerance();
    };

    ButtonCondition RELEASE_CONDITION = (handler, constant, exe) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (exe != null);

        final ButtonPreferences preferences = exe.getPreferences();
        final Enum[] buttons = exe.getConstants();

        @SuppressWarnings("unchecked")
        final Table<InputEvent> pressHistory = handler.getPressHistory();
        @SuppressWarnings("unchecked")
        final Table<InputEvent> releaseHistory = handler.getReleaseHistory();

        boolean ordered = true;
        long lastPressTime = -Long.MAX_VALUE;

        long earliest = Long.MAX_VALUE;
        long latest = -Long.MAX_VALUE;

        // Measure latest and earliest release events among the button(s)
        for (final Enum button : buttons) {

            final int ord = button.ordinal();
            final InputEvent press = pressHistory.get(0, ord);
            final InputEvent release = releaseHistory.get(0, ord);
            final long pressTime = press.getTime();
            final long releaseTime = release.getTime();

            // Releases should come after presses
            ordered = releaseTime >= pressTime && ordered;

            // Click must be fast enough
            ordered = ordered && releaseTime - pressTime <= preferences.getClickDuration();

            earliest = (releaseTime < earliest) ? releaseTime : earliest;
            latest = (releaseTime > latest) ? releaseTime : latest;
            lastPressTime = (pressTime > lastPressTime) ? pressTime : lastPressTime;
        }

        // Releases must be close enough together
        return ordered && latest - earliest <= preferences.getTolerance();
    };

    ButtonCondition DOUBLE_CLICK_CONDITION = (handler, constant, exe) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (exe != null);

        // Make sure previous press wasn't used in another double click
        if (!handler.isUsableForDoubleClick(constant)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        final Table<InputEvent> pressHistory = handler.getPressHistory();
        @SuppressWarnings("unchecked")
        final Table<InputEvent> releaseHistory = handler.getReleaseHistory();

        final int ord = constant.ordinal();
        final long pressTime = pressHistory.get(0, ord).getTime();
        final long prevPressTime = pressHistory.get(1, ord).getTime();
        final long releaseTime = releaseHistory.get(0, ord).getTime();

        // First click should occur before most recent press and latest event can't be release
        if (!(prevPressTime <= releaseTime && releaseTime <= pressTime)) {
            return false;
        }

        final ButtonPreferences prefs = exe.getPreferences();

        // Click should be within duration constraint
        boolean satisfied = releaseTime - prevPressTime <= prefs.getClickDuration();

        // Presses should be close enough together
        satisfied = satisfied && (pressTime - prevPressTime) <= prefs.getTolerance();

        if (satisfied) {
            handler.markAsDoubleClick(constant);
        }

        return satisfied;
    };

    /**
     * <p>Checks if the condition has been met.</p>
     *
     * @param handler event history source.
     * @param constant trigger.
     * @param exe instructions to execute.
     * @return true if the condition is met.
     */
    boolean validate(ButtonHandler handler, Enum constant, Executable<?, ButtonPreferences, ButtonCondition> exe);

    /**
     * <p>Gets the condition appropriate for the given preferences.</p>
     *
     * @param preferences preferences.
     * @return condition.
     */
    static ButtonCondition from(ButtonPreferences preferences)
    {
        assert (preferences != null);

        final ButtonCondition condition;
        switch (preferences.getStyle()) {
            case PRESS:
                condition = PRESS_CONDITION; break;

            case RELEASE:
                condition = RELEASE_CONDITION; break;

            case DOUBLE:
                condition = DOUBLE_CLICK_CONDITION; break;

            default: condition = null;
        }

        return condition;
    }
}