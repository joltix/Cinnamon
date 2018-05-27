package cinnamon.engine.event;

import cinnamon.engine.event.MotionPreferences.Axis;
import cinnamon.engine.event.InputEvent.AxisEvent;
import cinnamon.engine.utils.Table;

/**
 * <p>Offers value and history checking functions for specific kinds of translation.</p>
 */
interface AxisCondition
{
    AxisCondition MOTION = (handler, constant, preferences) ->
    {
        return true;
    };

    AxisCondition MOTION_ON_X = (handler, constant, preferences) ->
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

    AxisCondition MOTION_ON_Y = (handler, constant, preferences) ->
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

    AxisCondition POSITIVE_ON_X = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final AxisEvent current = history.get(0, constant.ordinal());

        return current.getHorizontal() >= 0f;
    };

    AxisCondition NEGATIVE_ON_X = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final AxisEvent current = history.get(0, constant.ordinal());

        return current.getHorizontal() <= 0f;
    };

    AxisCondition POSITIVE_ON_Y = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final AxisEvent current = history.get(0, constant.ordinal());

        return current.getVertical() >= 0f;
    };

    AxisCondition NEGATIVE_ON_Y = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final AxisEvent current = history.get(0, constant.ordinal());

        return current.getVertical() <= 0f;
    };

    AxisCondition INCREASING = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousH = previous.getHorizontal();
        final float previousV = previous.getVertical();
        final float currentH = current.getHorizontal();
        final float currentV = current.getVertical();

        // Compute relative distances
        final float previousDist = (previousH * previousH) + (previousV * previousV);
        final float currentDist = (currentH * currentH) + (currentV * currentV);

        return previousDist < currentDist;
    };

    AxisCondition INCREASING_ON_X = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousH = previous.getHorizontal();
        final float currentH = current.getHorizontal();

        // Compute relative distances
        return (previousH * previousH) < (currentH * currentH);
    };

    AxisCondition INCREASING_ON_Y = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousV = previous.getVertical();
        final float currentV = current.getVertical();

        // Compute relative distances
        return (previousV * previousV) < (currentV * currentV);
    };

    AxisCondition DECREASING = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousH = previous.getHorizontal();
        final float previousV = previous.getVertical();
        final float currentH = current.getHorizontal();
        final float currentV = current.getVertical();

        // Compute relative distances
        final float previousDist = (previousH * previousH) + (previousV * previousV);
        final float currentDist = (currentH * currentH) + (currentV * currentV);

        return previousDist > currentDist;
    };


    AxisCondition DECREASING_ON_X = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousH = previous.getHorizontal();
        final float currentH = current.getHorizontal();

        // Compute relative distances
        return (previousH * previousH) > (currentH * currentH);
    };

    AxisCondition DECREASING_ON_Y = (handler, constant, preferences) ->
    {
        assert (handler != null);
        assert (constant != null);
        assert (preferences != null);

        @SuppressWarnings("unchecked")
        final Table<AxisEvent> history = handler.getHistory();
        final int ord = constant.ordinal();
        final AxisEvent previous = history.get(1, ord);
        final AxisEvent current = history.get(0, ord);

        final float previousV = previous.getVertical();
        final float currentV = current.getVertical();

        // Compute relative distances
        return (previousV * previousV) > (currentV * currentV);
    };

    /**
     * <p>Checks if the condition has been met.</p>
     *
     * @param handler event history source.
     * @param constant trigger.
     * @param preferences preferences.
     * @return true if the condition is met.
     */
    boolean validate(AxisHandler handler, Enum constant, MotionPreferences preferences);

    /**
     * <p>Gets the condition appropriate for the given preferences.</p>
     *
     * @param preferences preferences.
     * @return condition.
     */
    static AxisCondition from(MotionPreferences preferences)
    {
        assert (preferences != null);

        final Axis[] axes = preferences.getAxes();
        final AxisCondition condition;

        switch (preferences.getTranslation()) {

            case SPECIFIC_AXIS:
                if (axes.length == 1) {
                    condition = (axes[0] == Axis.X) ? MOTION_ON_X : MOTION_ON_Y;
                } else {
                    condition = MOTION;
                } break;

            case POSITIVE_SIGN:
                condition = (axes[0] == Axis.X) ? POSITIVE_ON_X : POSITIVE_ON_Y; break;

            case NEGATIVE_SIGN:
                condition = (axes[0] == Axis.X) ? NEGATIVE_ON_X : NEGATIVE_ON_Y; break;

            case INCREASING_DISTANCE:
                if (axes.length == 1) {
                    condition = (axes[0] == Axis.X) ? INCREASING_ON_X : INCREASING_ON_Y;
                } else {
                    condition = INCREASING;
                } break;

            case DECREASING_DISTANCE:
                if (axes.length == 1) {
                    condition = (axes[0] == Axis.X) ? DECREASING_ON_X : DECREASING_ON_Y;
                } else {
                    condition = DECREASING;
                } break;

            default: condition = null;
        }

        return condition;
    }
}
