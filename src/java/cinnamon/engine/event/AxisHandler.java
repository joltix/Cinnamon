package cinnamon.engine.event;

import cinnamon.engine.event.InputEvent.AxisEvent;
import cinnamon.engine.utils.Table;

/**
 * <p>Executes instructions saved as a control map per {@code InputEvent} provided through
 * {@link #submit(InputEvent)} for axis-based motion events.</p>
 *
 * @param <ExternalType> external constant.
 * @param <InternalType> constant to work with internally.
 * @param <EventType> event to handle.
 */
abstract class AxisHandler<
        ExternalType extends Enum<ExternalType>,
        InternalType extends Enum<InternalType>,
        EventType extends InputEvent & AxisEvent
        > extends MappingsHandler<ExternalType, InternalType, EventType, MotionPreferences, AxisCondition>
{
    private final Table<EventType> mHistory;

    AxisHandler(Class<InternalType> cls, Table<EventType> history)
    {
        super(cls);

        assert (history != null);

        mHistory = history;
    }

    @Override
    public final void submit(EventType event)
    {
        assert (event != null);

        attemptToExecuteOn(extractConstantFrom(event), event);
    }

    @Override
    protected final boolean isReadyToExecute(AxisCondition condition,
                                       Enum constant,
                                       Executable<EventType, MotionPreferences, AxisCondition> exe)
    {
        return condition.validate(this, constant, exe.getPreferences());
    }

    @Override
    protected final AxisCondition selectConditionFrom(MotionPreferences preferences)
    {
        assert (preferences != null);

        return AxisCondition.from(preferences);
    }

    final Table<EventType> getHistory()
    {
        return mHistory;
    }
}
