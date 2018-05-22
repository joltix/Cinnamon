package cinnamon.engine.event;

import cinnamon.engine.event.InputEvent.AxisEvent;
import cinnamon.engine.utils.Table;

/**
 * <p>Offers history checking algorithms for various kinds of axis motion.</p>
 */
interface AxisCondition
{
    AxisCondition ALL_MOTION = (handler, constant, preferences) ->
    {
        return true;
    };

    AxisCondition HORIZONTAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        return current.getHorizontal() != past.getHorizontal();
    };

    AxisCondition VERTICAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        return current.getVertical() != past.getVertical();
    };

    AxisCondition RISING_HORIZONTAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        if (preferences.isPositive()) {
            return current.getHorizontal() > past.getHorizontal();
        } else {
            return current.getHorizontal() < past.getHorizontal();
        }
    };

    AxisCondition FALLING_HORIZONTAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        if (preferences.isPositive()) {
            return current.getHorizontal() > past.getHorizontal();
        } else {
            return current.getHorizontal() < past.getHorizontal();
        }
    };

    AxisCondition RISING_VERTICAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        if (preferences.isPositive()) {
            return current.getVertical() > past.getVertical();
        } else {
            return current.getVertical() < past.getVertical();
        }
    };

    AxisCondition FALLING_VERTICAL = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent current = history.get(0, ord);
        final AxisEvent past = history.get(1, ord);

        if (preferences.isPositive()) {
            return current.getVertical() > past.getVertical();
        } else {
            return current.getVertical() < past.getVertical();
        }
    };


    /**
     * <p>Checks if the condition has been met.</p>
     *
     * @param handler event history source.
     * @param constant trigger.
     * @param preferences preferences.
     * @return true if the condition is met.
     */
    boolean validate(AxisHandler handler, Enum constant, AxisPreferences preferences);

    /**
     * <p>Gets the condition appropriate for the given preferences.</p>
     *
     * @param preferences preferences.
     * @return condition.
     */
    static AxisCondition from(AxisPreferences preferences)
    {
        assert (preferences != null);

        final AxisCondition condition;
        switch (preferences.getStyle()) {
            case FREE:
                condition = ALL_MOTION; break;

            case HORIZONTAL:
                if (preferences.isDiscriminating()) {
                    condition = (preferences.isPositive()) ? RISING_HORIZONTAL : FALLING_HORIZONTAL;
                } else {
                    condition = HORIZONTAL;
                } break;

            case VERTICAL:
                if (preferences.isDiscriminating()) {
                    condition = (preferences.isPositive()) ? RISING_VERTICAL : FALLING_VERTICAL;
                } else {
                    condition = VERTICAL;
                } break;

            default: condition = null;
        }

        return condition;
    }
}
