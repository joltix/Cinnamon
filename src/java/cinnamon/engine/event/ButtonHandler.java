package cinnamon.engine.event;

import cinnamon.engine.event.InputEvent.ButtonEvent;
import cinnamon.engine.utils.Table;

/**
 * <p>Executes instructions saved as a control map per {@code InputEvent} provided through
 * {@link #submit(InputEvent)} while tracking double click usage and availability for button-based events.</p>
 *
 * <h3>Double clicks</h3>
 * <p>Double click usage is tracked by whether the two most recent press events have been used in a previous double
 * click. This assumes a press event is followed by a corresponding release event i.e. a press event demarcates a
 * click. {@link #isUsableForDoubleClick(Enum)} and {@link #markAsDoubleClick(Enum)} are provided to test if a
 * double click may be performed and to record that a double click has been used, respectively.</p>
 *
 * @param <ExternalType> external constant.
 * @param <InternalType> constant to work with internally.
 * @param <EventType> event to handle.
 */
abstract class ButtonHandler<
        ExternalType extends Enum<ExternalType>,
        InternalType extends Enum<InternalType>,
        EventType extends InputEvent & ButtonEvent
        > extends MappingsHandler<ExternalType, InternalType, EventType, ButtonPreferences, ButtonCondition>
{
    // 2 presses
    private static final int DOUBLE_CLICK_LENGTH = 2;

    // Series of press events
    private final Table<EventType> mPressHistory;

    // Series of release events
    private final Table<EventType> mReleaseHistory;

    // Records of presses and releases per constant, 0 and 1 = presses, 2 and 3 = releases
    // Trues are events used by a double click
    private final boolean[] mDoubleClicked;

    ButtonHandler(Class<InternalType> cls, Table<EventType> pressHistory, Table<EventType> releaseHistory)
    {
        super(cls);

        assert (pressHistory != null);
        assert (releaseHistory != null);

        mPressHistory = pressHistory;
        mReleaseHistory = releaseHistory;
        mDoubleClicked = new boolean[cls.getEnumConstants().length * DOUBLE_CLICK_LENGTH];
    }

    @Override
    public final void submit(EventType event)
    {
        assert (event != null);

        final Enum button = extractConstantFrom(event);

        // Track presses for double clicks
        if (event.isPress()) {
            appendUnusedPressForDoubleClickUsage(button);
        }

        attemptToExecuteOn(button, event);
    }

    @Override
    protected final boolean isReadyToExecute(ButtonCondition condition,
                                       Enum constant,
                                       Executable<EventType, ButtonPreferences, ButtonCondition> exe)
    {
        return condition.validate(this, constant, exe);
    }

    @Override
    protected final ButtonCondition selectConditionFrom(ButtonPreferences preferences)
    {
        assert (preferences != null);

        return ButtonCondition.from(preferences);
    }

    /**
     * <p>Checks the given button's double click history if its two most recent press events are unused i.e. the last
     * two clicks have not already been used in a double click.</p>
     *
     * @param button button.
     * @return true if the button is allowed a double click.
     */
    final boolean isUsableForDoubleClick(Enum button)
    {
        assert (button != null);

        final int scaledOrd = button.ordinal() * DOUBLE_CLICK_LENGTH;
        return !mDoubleClicked[scaledOrd] && !mDoubleClicked[scaledOrd + 1];
    }

    /**
     * <p>Marks the most recent two press events as <i>used</i> in the given button's double click history.</p>
     *
     * @param button button.
     */
    final void markAsDoubleClick(Enum button)
    {
        assert (button != null);

        final int scaledOrd = button.ordinal() * DOUBLE_CLICK_LENGTH;
        mDoubleClicked[scaledOrd] = true;
        mDoubleClicked[scaledOrd + 1] = true;
    }

    final Table<EventType> getPressHistory()
    {
        return mPressHistory;
    }

    final Table<EventType> getReleaseHistory()
    {
        return mReleaseHistory;
    }

    /**
     * <p>Appends an unused event entry to the given button's double click history.</p>
     *
     * @param button button.
     */
    private void appendUnusedPressForDoubleClickUsage(Enum button)
    {
        assert (button != null);

        final int scaledOrd = button.ordinal() * DOUBLE_CLICK_LENGTH;
        mDoubleClicked[scaledOrd] = mDoubleClicked[scaledOrd + 1];
        mDoubleClicked[scaledOrd + 1] = false;
    }
}
