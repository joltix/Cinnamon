package cinnamon.engine.event;

import java.util.*;

/**
 * <p>Handles the getting and setting of control mappings for input devices. This is a base class
 * for {@link ButtonHandler} and {@link AxisHandler}.</p>
 *
 * @param <ExternalType> external constant.
 * @param <InternalType> constant to work with internally.
 * @param <EventType> event to handle.
 * @param <PreferencesType> preferences for how to execute.
 * @param <ConditionType> either {@link ButtonCondition} or {@link AxisCondition}.
 */
abstract class MappingsHandler<
        ExternalType extends Enum<ExternalType>,
        InternalType extends Enum<InternalType>,
        EventType extends InputEvent,
        PreferencesType extends Preferences,
        ConditionType
        >
{

    // Sorts instructions by priority
    private static final Comparator<Executable> PRIORITY_COMPARATOR = Comparator.comparingInt((exe) ->
    {
        return exe.getPreferences().getPriority();
    });

    // Instructions sorted by priority, per constant
    private final Map<InternalType, List<Executable<EventType, PreferencesType, ConditionType>>> mExecutables;

    /**
     * <p>Constructs a {@code MappingsHandler} around the values of a {@code Class}' enums.</p>
     *
     * <p>The values of the given {@code Enum} class will become keys in the {@code Map} returned by
     * {@code getExecutables()}.</p>
     *
     * @param cls enum class.
     */
    @SuppressWarnings("unchecked")
    MappingsHandler(Class<InternalType> cls)
    {
        assert (cls != null);
        assert (cls.isEnum());

        mExecutables = new EnumMap<>(cls);

        for (final InternalType constant : cls.getEnumConstants()) {
            mExecutables.put(constant, new ArrayList<>());
        }
    }

    /**
     * <p>Saves and organizes all executables from the given map such that each associated constant contains
     * the executable in the constant's list. If a mappings' constants are wrapped, i.e. aliased by
     * {@code ButtonWrapper} or {@code AxisWrapper}, then they are unwrapped. Each constant's list of executables
     * are sorted by their priority.</p>
     *
     * @param mappings executables source.
     */
    @SuppressWarnings("unchecked")
    public final void setMappings(Map<String, ? extends InputRule<ExternalType, EventType, PreferencesType>> mappings)
    {
        assert (mappings != null);

        clearMappings();
        saveMappings(mappings);
        orderMappings();
    }

    /**
     * <p>Considers the given event as a trigger for input mapping(s).</p>
     *
     * @param event potential trigger.
     */
    public abstract void submit(EventType event);

    /**
     * <p>Tries to execute each {@code Executable} belonging to the given {@code constant}.</p>
     *
     * @param constant triggering constant.
     * @param event triggering event.
     */
    protected final void attemptToExecuteOn(Enum constant, EventType event)
    {
        assert (constant != null);
        assert (event != null);

        for (final Executable<EventType, PreferencesType, ConditionType> exe : getExecutables().get(constant)) {
            if (isReadyToExecute(exe.getCondition(), constant, exe)) {
                exe.getListener().onEvent(event);
            }
        }
    }

    /**
     * <p>Checks the {@code condition} if the given executable can be executed.</p>
     *
     * @param condition condition.
     * @param constant triggering constant.
     * @param exe instructions to execute.
     * @return true if {@code exe} can be executed.
     */
    protected abstract boolean isReadyToExecute(ConditionType condition,
                                                Enum constant,
                                                Executable<EventType, PreferencesType, ConditionType> exe);

    /**
     * <p>Creates an array of matching length with the base versions of the given constants. For constants
     * such as keyboard keys, the returned type is the same as that given and so this method should not be overridden.
     * For gamepad constants, which implement one of the the wrapper interfaces ({@code ButtonWrapper} or
     * {@code AxisWrapper}), this method should return the <i>base</i> constant i.e. the constant aliased by the
     * wrapper.</p>
     *
     * @param constants constants.
     * @return array of base constants.
     */
    protected Enum[] unwrap(List<? extends Enum> constants)
    {
        return constants.toArray(new Enum[0]);
    }

    /**
     * <p>Retrieves the condition appropriate for the given preferences.</p>
     *
     * @param preferences preferences.
     * @return condition.
     */
    protected abstract ConditionType selectConditionFrom(PreferencesType preferences);

    /**
     * <p>Retrieves the constant carried by an event e.g. {@code KeyEvent}s return {@code Key}s.</p>
     *
     * @param event event.
     * @return constant.
     */
    protected abstract InternalType extractConstantFrom(EventType event);

    /**
     * <p>Gets a map of all control mappings.</p>
     *
     * @return mappings.
     */
    protected final Map<InternalType, List<Executable<EventType, PreferencesType, ConditionType>>> getExecutables()
    {
        return mExecutables;
    }

    /**
     * <p>Removes all set mappings.</p>
     */
    private void clearMappings()
    {
        for (final List<Executable<EventType, PreferencesType, ConditionType>> exes : mExecutables.values()) {
            exes.clear();
        }
    }

    /**
     * <p>Copies all rules to the handler. These can be later retrieved with {@code getExecutables()}.</p>
     *
     * @param mappings mappings.
     */
    @SuppressWarnings("unchecked")
    private void saveMappings(Map<String, ? extends InputRule<ExternalType, EventType, PreferencesType>> mappings)
    {
        for (final InputRule<ExternalType, EventType, PreferencesType> rule : mappings.values()) {

            final ConditionType condition  = selectConditionFrom(rule.getPreferences());
            final Enum[] raw = unwrap(rule.getConstants());
            final Executable exe = new Executable(rule, raw, condition);

            // Last constant should be trigger
            mExecutables.get(raw[raw.length - 1]).add(exe);
        }
    }

    /**
     * <p>Sorts each constant's associated executables by their preference's priority.</p>
     */
    private void orderMappings()
    {
        for (final List<Executable<EventType, PreferencesType, ConditionType>> exes : mExecutables.values()) {
            exes.sort(PRIORITY_COMPARATOR);
        }
    }

    /**
     * <p>Container for a set of instructions' mapping to an event and set of required constants.</p>
     *
     * @param <EventType> triggering event.
     * @param <PreferencesType>  either {@link ButtonPreferences} or {@link MotionPreferences}.
     * @param <ConditionType> either {@link ButtonCondition} or {@link AxisCondition}.
     */
    static class Executable<EventType extends InputEvent, PreferencesType extends Preferences, ConditionType>
    {
        private final EventListener<EventType> mListener;
        private final PreferencesType mPreference;
        private final Enum[] mConstants;
        private final ConditionType mCondition;

        Executable(InputRule<?, EventType, PreferencesType> rule, Enum[] constants, ConditionType condition)
        {
            mListener = rule.getListener();
            mPreference = rule.getPreferences();
            mConstants = constants;
            mCondition = condition;
        }

        final EventListener<EventType> getListener()
        {
            return mListener;
        }

        final PreferencesType getPreferences()
        {
            return mPreference;
        }

        final Enum[] getConstants()
        {
            return mConstants;
        }

        final ConditionType getCondition()
        {
            return mCondition;
        }
    }
}
