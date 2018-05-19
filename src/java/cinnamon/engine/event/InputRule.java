package cinnamon.engine.event;

import java.util.List;

/**
 * <p>Implemented by classes carrying reaction instructions to input events.</p>
 *
 * @param <ConstantType> constants capable of triggering the rule.
 * @param <EventType> input event to act on.
 * @param <PreferencesType> execution preferences.
 */
interface InputRule<
        ConstantType extends Enum<ConstantType>,
        EventType extends InputEvent,
        PreferencesType extends Preferences
        >
{
    /**
     * <p>Gets an immutable {@code List} of the constants able to trigger the rule.</p>
     *
     * @return immutable list of constants.
     */
    List<ConstantType> getConstants();

    /**
     * <p>Gets the listener to be called when the rule is deemed executable.</p>
     *
     * @return listener.
     */
    EventListener<EventType> getListener();

    /**
     * <p>Gets the preferences determining when the rule can be executed.</p>
     *
     * @return preferences.
     */
    PreferencesType getPreferences();
}
